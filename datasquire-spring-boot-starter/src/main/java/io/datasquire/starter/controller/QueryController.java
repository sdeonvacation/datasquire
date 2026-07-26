package io.datasquire.starter.controller;

import io.datasquire.core.agent.RequestContext;
import io.datasquire.core.session.ConversationMessage;
import io.datasquire.core.session.Role;
import io.datasquire.core.session.SessionState;
import io.datasquire.core.session.SessionStore;
import io.datasquire.orchestrator.react.OrchestratorResult;
import io.datasquire.orchestrator.react.ReActOrchestrator;
import io.datasquire.orchestrator.registry.SubAgentRegistry;
import io.datasquire.starter.config.DataSquireProperties;
import io.datasquire.starter.metrics.DataSquireMetrics;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST controller exposing DataSquire query, session, and agent endpoints via SSE streaming.
 */
@RestController
@RequestMapping("${datasquire.controller.base-path:/api}")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);
    private static final long SSE_TIMEOUT_MS = 120_000L;

    @Nullable
    private final ReActOrchestrator orchestrator;
    private final SessionStore sessionStore;
    private final SubAgentRegistry subAgentRegistry;
    private final DataSquireProperties properties;
    @Nullable
    private final DataSquireMetrics metrics;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public QueryController(@Nullable ReActOrchestrator orchestrator, SessionStore sessionStore,
                           SubAgentRegistry subAgentRegistry, DataSquireProperties properties,
                           @Nullable DataSquireMetrics metrics) {
        this.orchestrator = orchestrator;
        this.sessionStore = sessionStore;
        this.subAgentRegistry = subAgentRegistry;
        this.properties = properties;
        this.metrics = metrics;
    }

    @PostMapping(value = "/query", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter query(@Valid @RequestBody QueryRequest request,
                            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
                            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {
        var emitter = new SseEmitter(SSE_TIMEOUT_MS);
        if (orchestrator == null) {
            executor.submit(() -> sendErrorEvent(emitter, new IllegalStateException(
                    "Query endpoint unavailable: orchestrator not configured")));
            return emitter;
        }
        executor.submit(() -> processQuery(emitter, request, tenantId, userId));
        return emitter;
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<SessionState> getSession(
            @PathVariable String id,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String scopedKey = tenantId + ":" + id;
        return sessionStore.get(scopedKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String id,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        String scopedKey = tenantId + ":" + id;
        sessionStore.delete(scopedKey);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/agents")
    public List<AgentInfoDto> listAgents() {
        return subAgentRegistry.getHealthyAgents().stream()
                .map(agent -> new AgentInfoDto(
                        agent.getName(), agent.getDescription(),
                        agent.getCapabilities().stream().map(c -> c.name()).toList()))
                .toList();
    }

    private void processQuery(SseEmitter emitter, QueryRequest request, String tenantId, String userId) {
        Instant start = Instant.now();
        try {
            if (metrics != null) {
                metrics.incrementQueryCount();
            }
            sendEvent(emitter, "progress", Map.of("status", "resolving_session"));

            String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                    ? request.sessionId() : UUID.randomUUID().toString();
            String scopedKey = tenantId + ":" + sessionId;
            SessionState session = sessionStore.get(scopedKey)
                    .orElseGet(() -> new SessionState(scopedKey, List.of(), Map.of(), Instant.now()));

            sendEvent(emitter, "progress", Map.of("status", "orchestrating", "sessionId", scopedKey));

            RequestContext context = new RequestContext(tenantId, userId, request.options());
            OrchestratorResult result = orchestrator.orchestrate(request.query(), session, context);

            sendEvent(emitter, "data", Map.of("response", result.response()));
            sendEvent(emitter, "done", Map.of(
                    "sessionId", scopedKey, "iterations", result.iterations(),
                    "agentsUsed", result.agentsUsed(), "qualityScore", result.qualityScore()));

            SessionState updated = session
                    .withMessage(new ConversationMessage(Role.USER, request.query(), Instant.now()))
                    .withMessage(new ConversationMessage(Role.ASSISTANT, result.response(), Instant.now()))
                    .trimHistory(properties.getSession().getMaxHistorySize());
            sessionStore.save(updated);

            if (metrics != null) {
                metrics.recordQueryLatency(Duration.between(start, Instant.now()));
                metrics.recordIterationCount(result.iterations());
                result.agentsUsed().forEach(agent -> metrics.recordAgentCall(agent, true));
            }
            emitter.complete();
        } catch (Exception e) {
            log.error("Query processing failed: {}", e.getMessage(), e);
            sendErrorEvent(emitter, e);
            if (metrics != null) {
                metrics.recordQueryLatency(Duration.between(start, Instant.now()));
            }
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e) {
            log.debug("Failed to send SSE event '{}': {}", eventName, e.getMessage());
        }
    }

    private void sendErrorEvent(SseEmitter emitter, Exception ex) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Unknown error")));
            emitter.complete();
        } catch (Exception e) {
            log.debug("Failed to send SSE error event: {}", e.getMessage());
            emitter.completeWithError(ex);
        }
    }
}
