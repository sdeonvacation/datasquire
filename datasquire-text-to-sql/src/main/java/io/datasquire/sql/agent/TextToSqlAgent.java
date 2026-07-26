package io.datasquire.sql.agent;

import io.datasquire.core.agent.AgentCapability;
import io.datasquire.core.agent.SubAgent;
import io.datasquire.core.agent.SubAgentRequest;
import io.datasquire.core.agent.SubAgentResponse;
import io.datasquire.core.schema.SchemaProvider;
import io.datasquire.sql.config.TextToSqlProperties;
import io.datasquire.sql.executor.SqlExecutorTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sub-agent that generates and executes SQL queries using natural language.
 * Uses RAG-augmented schema context to inform SQL generation and Spring AI's
 * ChatClient with tool callbacks for iterative query execution.
 */
public class TextToSqlAgent implements SubAgent {

    private static final Logger log = LoggerFactory.getLogger(TextToSqlAgent.class);
    private static final String PROMPT_RESOURCE = "prompts/text-to-sql-system.md";
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final ChatClient chatClient;
    private final SchemaProvider schemaProvider;
    private final TextToSqlProperties properties;
    private final String systemPromptTemplate;

    public TextToSqlAgent(ChatModel chatModel,
                          SchemaProvider schemaProvider,
                          SqlExecutorTool sqlExecutorTool,
                          TextToSqlProperties properties) {
        this.schemaProvider = schemaProvider;
        this.properties = properties;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultTools(sqlExecutorTool)
                .build();
        this.systemPromptTemplate = loadPromptTemplate();
    }

    @Override
    public String getName() {
        return "text-to-sql";
    }

    @Override
    public String getDescription() {
        return "Generates and executes SQL queries against your database using natural language";
    }

    @Override
    public Set<AgentCapability> getCapabilities() {
        return Set.of(
                new AgentCapability("aggregation",
                        "Performs aggregation queries (COUNT, SUM, AVG, GROUP BY)",
                        Set.of("count", "sum", "average", "total", "group by", "aggregate")),
                new AgentCapability("filtering",
                        "Filters and searches data with conditions",
                        Set.of("filter", "where", "find", "search", "match", "between")),
                new AgentCapability("joins",
                        "Joins data across multiple tables",
                        Set.of("join", "combine", "relate", "relationship", "across tables")),
                new AgentCapability("schema-exploration",
                        "Explores database schema and table structures",
                        Set.of("schema", "tables", "columns", "structure", "describe"))
        );
    }

    @Override
    public Set<String> getKeywords() {
        return Set.of(
                "sql", "query", "database", "table", "select",
                "count", "sum", "average", "group", "filter",
                "join", "data", "rows", "columns", "records",
                "report", "statistics", "metrics", "aggregate"
        );
    }

    @Override
    public List<String> getExampleQueries() {
        return List.of(
                "How many orders were placed last month?",
                "Show me the top 10 customers by revenue",
                "What tables are in the database?",
                "Find all users who signed up in 2024"
        );
    }

    @Override
    public CompletableFuture<SubAgentResponse> process(SubAgentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String schemaContext = schemaProvider.getSchemaContext(request.query());
                String systemPrompt = systemPromptTemplate.replace("{schemaContext}", schemaContext);

                log.debug("Processing text-to-sql request: '{}'", request.query());

                String response = chatClient.prompt()
                        .system(systemPrompt)
                        .user(request.query())
                        .call()
                        .content();

                if (response == null || response.isBlank()) {
                    return SubAgentResponse.failure(getName(),
                            "LLM returned an empty response");
                }

                return SubAgentResponse.success(getName(), response, Set.of());
            } catch (Exception e) {
                log.error("Text-to-SQL processing failed for query: '{}'",
                        request.query(), e);
                return SubAgentResponse.failure(getName(),
                        "SQL agent error: " + e.getMessage());
            }
        }, VIRTUAL_EXECUTOR);
    }

    private String loadPromptTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource(PROMPT_RESOURCE);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to load prompt template from classpath, using fallback", e);
            return """
                    You are a SQL generation agent. Answer the user's question by querying the database.
                    Use the executeSql tool to run read-only SQL queries.
                    
                    Schema:
                    {schemaContext}
                    
                    Write clear SQL and explain your results.
                    """;
        }
    }
}
