package io.datasquire.orchestrator.router;

import io.datasquire.core.agent.SubAgent;
import io.datasquire.core.routing.QueryRouter;
import io.datasquire.core.routing.RoutingDecision;
import io.datasquire.orchestrator.config.OrchestratorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Routes queries to sub-agents using keyword scoring with LLM fallback.
 * First attempts keyword overlap matching; if confidence is too low and a ChatModel
 * is available, delegates classification to the LLM.
 */
@Component
public class KeywordAndLlmRouter implements QueryRouter {

    private static final Logger log = LoggerFactory.getLogger(KeywordAndLlmRouter.class);
    private static final Pattern JSON_AGENTS_PATTERN = Pattern.compile(
            "\"agents\"\\s*:\\s*\\[([^]]*)]");
    private static final Pattern REASONING_PATTERN = Pattern.compile(
            "\"reasoning\"\\s*:\\s*\"([^\"]+)\"");

    private final ChatModel chatModel;
    private final OrchestratorProperties properties;

    public KeywordAndLlmRouter(@Nullable ChatModel chatModel,
                               OrchestratorProperties properties) {
        this.chatModel = chatModel;
        this.properties = properties;
    }

    @Override
    public RoutingDecision route(String query, List<SubAgent> agents) {
        if (agents.isEmpty()) {
            return new RoutingDecision(List.of(), 0.0, "No agents available", false);
        }

        // Score agents by keyword overlap
        Set<String> queryWords = tokenize(query);
        Map<SubAgent, Double> scores = new LinkedHashMap<>();

        for (SubAgent agent : agents) {
            double score = calculateKeywordScore(queryWords, agent.getKeywords());
            if (score > 0) {
                scores.put(agent, score);
            }
        }

        // Find best keyword match
        Optional<Map.Entry<SubAgent, Double>> bestMatch = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (bestMatch.isPresent() && bestMatch.get().getValue() >= getConfidenceThreshold()) {
            SubAgent selected = bestMatch.get().getKey();
            double confidence = bestMatch.get().getValue();
            log.debug("Keyword match for '{}': agent='{}', confidence={}",
                    query, selected.getName(), confidence);

            // Include all agents above threshold
            List<String> matchedAgents = scores.entrySet().stream()
                    .filter(e -> e.getValue() >= getConfidenceThreshold())
                    .sorted(Map.Entry.<SubAgent, Double>comparingByValue().reversed())
                    .map(e -> e.getKey().getName())
                    .toList();

            return new RoutingDecision(matchedAgents, confidence,
                    "Keyword match", false);
        }

        // LLM fallback
        if (chatModel != null) {
            log.debug("No confident keyword match for '{}', falling back to LLM", query);
            return routeWithLlm(query, agents);
        }

        // No LLM available - return best keyword match even if below threshold
        if (bestMatch.isPresent()) {
            return new RoutingDecision(
                    List.of(bestMatch.get().getKey().getName()),
                    bestMatch.get().getValue(),
                    "Best keyword match (below threshold, no LLM available)",
                    true);
        }

        return new RoutingDecision(List.of(), 0.0,
                "No matching agents found and no LLM available", true);
    }

    @Override
    public double getConfidenceThreshold() {
        return properties.getQualityThreshold();
    }

    private RoutingDecision routeWithLlm(String query, List<SubAgent> agents) {
        String agentDescriptions = agents.stream()
                .map(a -> "- " + a.getName() + ": " + a.getDescription())
                .collect(Collectors.joining("\n"));

        String prompt = """
                Given these available agents:
                %s
                
                Which agent(s) should handle this query: "%s"?
                
                Return your answer as JSON: {"agents": ["agent_name1"], "reasoning": "explanation"}
                Only return the JSON, no other text.
                """.formatted(agentDescriptions, query);

        try {
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String content = response.getResult().getOutput().getText();
            return parseLlmRoutingResponse(content, agents);
        } catch (Exception e) {
            log.error("LLM routing failed for query '{}': {}", query, e.getMessage());
            return new RoutingDecision(List.of(), 0.0,
                    "LLM routing failed: " + e.getMessage(), true);
        }
    }

    private RoutingDecision parseLlmRoutingResponse(String llmResponse, List<SubAgent> agents) {
        Set<String> validNames = agents.stream()
                .map(SubAgent::getName)
                .collect(Collectors.toSet());

        List<String> selectedAgents = new ArrayList<>();
        String reasoning = "LLM classification";

        // Extract agents array
        Matcher agentsMatcher = JSON_AGENTS_PATTERN.matcher(llmResponse);
        if (agentsMatcher.find()) {
            String agentsJson = agentsMatcher.group(1);
            // Parse quoted strings from the array
            Pattern quotedString = Pattern.compile("\"([^\"]+)\"");
            Matcher stringMatcher = quotedString.matcher(agentsJson);
            while (stringMatcher.find()) {
                String name = stringMatcher.group(1);
                if (validNames.contains(name)) {
                    selectedAgents.add(name);
                }
            }
        }

        // Extract reasoning
        Matcher reasoningMatcher = REASONING_PATTERN.matcher(llmResponse);
        if (reasoningMatcher.find()) {
            reasoning = reasoningMatcher.group(1);
        }

        double confidence = selectedAgents.isEmpty() ? 0.0 : 0.8;
        return new RoutingDecision(selectedAgents, confidence, reasoning, false);
    }

    private double calculateKeywordScore(Set<String> queryWords, Set<String> agentKeywords) {
        if (queryWords.isEmpty()) {
            return 0.0;
        }
        Set<String> normalizedKeywords = agentKeywords.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        long matchCount = queryWords.stream()
                .filter(normalizedKeywords::contains)
                .count();

        return (double) matchCount / queryWords.size();
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 1)
                .collect(Collectors.toSet());
    }
}
