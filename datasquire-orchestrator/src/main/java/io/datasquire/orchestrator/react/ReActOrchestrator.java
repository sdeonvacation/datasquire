package io.datasquire.orchestrator.react;

import io.datasquire.core.agent.RequestContext;
import io.datasquire.core.agent.SubAgent;
import io.datasquire.core.agent.SubAgentRequest;
import io.datasquire.core.agent.SubAgentResponse;
import io.datasquire.core.exception.OrchestrationException;
import io.datasquire.core.filter.FilterResult;
import io.datasquire.core.filter.ResponseFilter;
import io.datasquire.core.routing.RoutingDecision;
import io.datasquire.core.scope.ScopeEnforcer;
import io.datasquire.core.scope.ScopeResult;
import io.datasquire.core.session.ConversationMessage;
import io.datasquire.core.session.SessionState;
import io.datasquire.orchestrator.config.OrchestratorProperties;
import io.datasquire.orchestrator.consolidator.ResponseConsolidator;
import io.datasquire.orchestrator.executor.SubAgentExecutor;
import io.datasquire.orchestrator.registry.SubAgentRegistry;
import io.datasquire.core.routing.QueryRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ReAct (Reason-Act-Observe) orchestrator that iteratively refines answers
 * by routing queries to sub-agents and assessing response quality.
 */
@Component
public class ReActOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ReActOrchestrator.class);

    private static final String REASON_PROMPT = """
            You are an orchestration planner. Given the user's question and observations so far,
            determine what additional information is needed.
            
            User question: %s
            
            Previous observations:
            %s
            
            What specific information do we still need? Be concise.
            If we have enough information to answer completely, respond with "COMPLETE".
            """;

    private static final String QUALITY_ASSESSMENT_PROMPT = """
            Rate the quality and completeness of this answer on a scale of 0.0 to 1.0.
            
            User question: %s
            
            Current answer based on observations:
            %s
            
            Return ONLY a decimal number between 0.0 and 1.0 representing the quality score.
            1.0 means the answer is complete and fully addresses the question.
            """;

    private final SubAgentRegistry registry;
    private final SubAgentExecutor executor;
    private final ResponseConsolidator consolidator;
    private final QueryRouter router;
    private final ChatModel chatModel;
    private final OrchestratorProperties properties;
    private final ScopeEnforcer scopeEnforcer;
    private final List<ResponseFilter> responseFilters;

    public ReActOrchestrator(SubAgentRegistry registry,
                             SubAgentExecutor executor,
                             ResponseConsolidator consolidator,
                             QueryRouter router,
                             ChatModel chatModel,
                             OrchestratorProperties properties,
                             Optional<ScopeEnforcer> scopeEnforcer,
                             List<ResponseFilter> responseFilters) {
        this.registry = registry;
        this.executor = executor;
        this.consolidator = consolidator;
        this.router = router;
        this.chatModel = chatModel;
        this.properties = properties;
        this.scopeEnforcer = scopeEnforcer.orElse(null);
        this.responseFilters = responseFilters.stream()
                .sorted(Comparator.comparingInt(ResponseFilter::getOrder))
                .toList();
    }

    /**
     * Orchestrates query processing through the ReAct loop.
     *
     * @param query   the user's natural language query
     * @param session current session state for context
     * @param context request context (tenant, user, attributes)
     * @return the orchestration result with response and metadata
     */
    public OrchestratorResult orchestrate(String query, SessionState session, RequestContext context) {
        // Step 1: Check scope
        if (scopeEnforcer != null) {
            ScopeResult scopeResult = scopeEnforcer.isInScope(query, context);
            if (!scopeResult.inScope()) {
                return buildRejectionResult(scopeResult.rejectionMessage());
            }
        }

        // Step 2: Route query to target agents
        List<SubAgent> healthyAgents = registry.getHealthyAgents();
        if (healthyAgents.isEmpty()) {
            throw new OrchestrationException("No healthy agents available");
        }

        RoutingDecision routing = router.route(query, healthyAgents);
        List<SubAgent> targetAgents = resolveAgents(routing.targetAgentNames());

        if (targetAgents.isEmpty()) {
            throw new OrchestrationException(
                    "Routing found no suitable agents for query: " + query);
        }

        if ("single-shot".equalsIgnoreCase(properties.getMode())) {
            return executeSingleShot(query, session, context, routing, targetAgents);
        }

        // Step 3: ReAct loop
        List<SubAgentResponse> allObservations = new ArrayList<>();
        Set<String> agentsUsed = new LinkedHashSet<>();
        int iterations = 0;
        double qualityScore = 0.0;

        for (int i = 0; i < properties.getMaxIterations(); i++) {
            iterations = i + 1;
            log.debug("ReAct iteration {} for query: '{}'", iterations, query);

            // REASON: Determine what info is still needed
            String plan = reason(query, allObservations);
            if ("COMPLETE".equalsIgnoreCase(plan.trim())) {
                log.debug("LLM determined answer is complete at iteration {}", iterations);
                break;
            }

            // ACT: Execute sub-agents
            SubAgentRequest request = buildRequest(query, session, context);
            List<SubAgentResponse> responses = executor.execute(targetAgents, request);

            // OBSERVE: Collect responses
            allObservations.addAll(responses);
            responses.stream()
                    .filter(SubAgentResponse::success)
                    .map(SubAgentResponse::agentName)
                    .forEach(agentsUsed::add);

            // DECIDE: Assess quality
            qualityScore = assessQuality(query, allObservations);
            if (qualityScore >= properties.getQualityThreshold()) {
                log.debug("Quality threshold met ({} >= {}) at iteration {}",
                        qualityScore, properties.getQualityThreshold(), iterations);
                break;
            }

            if (iterations >= properties.getMaxIterations()) {
                log.warn("Max iterations ({}) reached without meeting quality threshold",
                        properties.getMaxIterations());
            }
        }

        // Step 4: Consolidate responses
        String consolidatedResponse = consolidator.consolidate(allObservations, query);

        // Step 5: Apply response filters
        String filteredResponse = applyFilters(consolidatedResponse, context);

        // Step 6: Build result
        Map<String, Object> metadata = Map.of(
                "routingConfidence", routing.confidence(),
                "routingReasoning", routing.reasoning(),
                "executionMode", properties.getExecutionMode()
        );

        return new OrchestratorResult(
                filteredResponse,
                iterations,
                List.copyOf(agentsUsed),
                qualityScore,
                metadata
        );
    }

    /**
     * Single-shot execution: one agent call, no ReAct loop, no quality scoring.
     */
    private OrchestratorResult executeSingleShot(String query, SessionState session,
                                                  RequestContext context, RoutingDecision routing,
                                                  List<SubAgent> targetAgents) {
        log.debug("Single-shot mode for query: '{}'", query);

        SubAgentRequest request = buildRequest(query, session, context);
        // Use only the top-ranked agent for single-shot
        SubAgent primaryAgent = targetAgents.get(0);
        List<SubAgentResponse> responses = executor.execute(List.of(primaryAgent), request);

        SubAgentResponse response = responses.isEmpty()
                ? SubAgentResponse.failure(primaryAgent.getName(), "No response from agent")
                : responses.get(0);

        String responseText = response.success() ? response.response() : response.errorMessage();
        String filteredResponse = applyFilters(responseText, context);

        List<String> agentsUsed = response.success()
                ? List.of(response.agentName())
                : List.of();

        Map<String, Object> metadata = Map.of(
                "routingConfidence", routing.confidence(),
                "routingReasoning", routing.reasoning(),
                "executionMode", properties.getExecutionMode(),
                "mode", "single-shot"
        );

        return new OrchestratorResult(filteredResponse, 1, agentsUsed, 1.0, metadata);
    }

    private String reason(String query, List<SubAgentResponse> observations) {
        if (observations.isEmpty()) {
            return "Initial execution needed";
        }

        String observationsSummary = observations.stream()
                .filter(SubAgentResponse::success)
                .map(r -> "[" + r.agentName() + "]: " + r.response())
                .collect(Collectors.joining("\n"));

        String prompt = REASON_PROMPT.formatted(query, observationsSummary);

        try {
            ChatResponse response = chatModel.call(new Prompt(prompt));
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.warn("Reasoning step failed: {}", e.getMessage());
            return "COMPLETE"; // Fail-safe: stop iterating on reasoning failure
        }
    }

    private double assessQuality(String query, List<SubAgentResponse> observations) {
        String observationsSummary = observations.stream()
                .filter(SubAgentResponse::success)
                .map(SubAgentResponse::response)
                .collect(Collectors.joining("\n\n"));

        if (observationsSummary.isBlank()) {
            return 0.0;
        }

        String prompt = QUALITY_ASSESSMENT_PROMPT.formatted(query, observationsSummary);

        try {
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String text = response.getResult().getOutput().getText().trim();
            return parseQualityScore(text);
        } catch (Exception e) {
            log.warn("Quality assessment failed: {}", e.getMessage());
            return 0.5; // Default to middle score on failure
        }
    }

    private static final Pattern QUALITY_SCORE_PATTERN = Pattern.compile("(\\d+\\.\\d+|[01])");

    // Package-private for testing
    static double parseQualityScore(String text) {
        if (text == null || text.isBlank()) return 0.5;
        Matcher matcher = QUALITY_SCORE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                double score = Double.parseDouble(matcher.group(1));
                return Math.max(0.0, Math.min(1.0, score)); // clamp to [0,1]
            } catch (NumberFormatException e) {
                return 0.5;
            }
        }
        return 0.5;
    }

    private String applyFilters(String response, RequestContext context) {
        String current = response;
        for (ResponseFilter filter : responseFilters) {
            FilterResult result = filter.filter(current, context);
            if (result.blocked()) {
                return "Response blocked: " + result.blockReason();
            }
            current = result.content();
        }
        return current;
    }

    private SubAgentRequest buildRequest(String query, SessionState session, RequestContext context) {
        List<ConversationMessage> history = session != null ? session.history() : List.of();
        return new SubAgentRequest(query, history, context, Map.of());
    }

    private List<SubAgent> resolveAgents(List<String> agentNames) {
        return agentNames.stream()
                .map(registry::getAgent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(SubAgent::isHealthy)
                .toList();
    }

    private OrchestratorResult buildRejectionResult(String rejectionMessage) {
        return new OrchestratorResult(
                rejectionMessage,
                0,
                List.of(),
                0.0,
                Map.of("rejected", true)
        );
    }
}
