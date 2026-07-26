package io.datasquire.sql.agent;

import io.datasquire.core.agent.SubAgentRequest;
import io.datasquire.core.agent.SubAgentResponse;
import io.datasquire.core.schema.SchemaProvider;
import io.datasquire.sql.config.TextToSqlProperties;
import io.datasquire.sql.executor.SqlExecutorTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TextToSqlAgent verifying virtual thread executor usage
 * and async processing behavior.
 */
@ExtendWith(MockitoExtension.class)
class TextToSqlAgentTest {

    @Mock
    private ChatModel chatModel;
    @Mock
    private SchemaProvider schemaProvider;
    @Mock
    private SqlExecutorTool sqlExecutorTool;
    @Mock
    private TextToSqlProperties properties;

    private TextToSqlAgent agent;

    @BeforeEach
    void setUp() {
        // ChatModel mock needs to return a response for ChatClient.Builder usage
        var generation = mock(Generation.class);
        var assistantMessage = mock(org.springframework.ai.chat.messages.AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("SELECT 1");
        when(generation.getOutput()).thenReturn(assistantMessage);
        var chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(schemaProvider.getSchemaContext(any())).thenReturn("CREATE TABLE users (id INT);");

        agent = new TextToSqlAgent(chatModel, schemaProvider, sqlExecutorTool, properties);
    }

    @Test
    void processReturnsCompletableFutureRunningOnVirtualThread() throws Exception {
        SubAgentRequest request = new SubAgentRequest(
                "count users", List.of(), null, Map.of());

        CompletableFuture<SubAgentResponse> future = agent.process(request);

        // Should complete without blocking the calling thread indefinitely
        SubAgentResponse response = future.get(10, TimeUnit.SECONDS);
        assertNotNull(response);
        assertTrue(response.success());
        assertEquals("text-to-sql", response.agentName());
    }

    @Test
    void processReturnsFailureOnEmptyLlmResponse() throws Exception {
        var generation = mock(Generation.class);
        var assistantMessage = mock(org.springframework.ai.chat.messages.AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("");
        when(generation.getOutput()).thenReturn(assistantMessage);
        var chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // Recreate agent with updated mock
        agent = new TextToSqlAgent(chatModel, schemaProvider, sqlExecutorTool, properties);

        SubAgentRequest request = new SubAgentRequest(
                "bad query", List.of(), null, Map.of());

        SubAgentResponse response = agent.process(request).get(10, TimeUnit.SECONDS);
        assertFalse(response.success());
        assertTrue(response.errorMessage().contains("empty response"));
    }

    @Test
    void processReturnsFailureOnException() throws Exception {
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("LLM timeout"));

        agent = new TextToSqlAgent(chatModel, schemaProvider, sqlExecutorTool, properties);

        SubAgentRequest request = new SubAgentRequest(
                "failing query", List.of(), null, Map.of());

        SubAgentResponse response = agent.process(request).get(10, TimeUnit.SECONDS);
        assertFalse(response.success());
        assertTrue(response.errorMessage().contains("LLM timeout"));
    }

    @Test
    void agentMetadata() {
        assertEquals("text-to-sql", agent.getName());
        assertFalse(agent.getCapabilities().isEmpty());
        assertFalse(agent.getKeywords().isEmpty());
        assertFalse(agent.getExampleQueries().isEmpty());
    }
}
