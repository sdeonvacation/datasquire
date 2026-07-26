package io.datasquire.orchestrator.config;

import io.datasquire.core.agent.SubAgent;
import io.datasquire.core.filter.ResponseFilter;
import io.datasquire.core.routing.QueryRouter;
import io.datasquire.core.scope.ScopeEnforcer;
import io.datasquire.orchestrator.consolidator.ResponseConsolidator;
import io.datasquire.orchestrator.executor.SubAgentExecutor;
import io.datasquire.orchestrator.react.ReActOrchestrator;
import io.datasquire.orchestrator.registry.SubAgentRegistry;
import io.datasquire.orchestrator.router.KeywordAndLlmRouter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

/**
 * Auto-configuration for the DataSquire orchestrator module.
 * Wires up all orchestrator components when a ChatModel is available.
 * Each bean is conditional on missing to allow override via component scanning.
 */
@Configuration
@EnableConfigurationProperties(OrchestratorProperties.class)
@ConditionalOnBean(ChatModel.class)
public class OrchestratorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SubAgentRegistry subAgentRegistry(List<SubAgent> agents) {
        return new SubAgentRegistry(agents);
    }

    @Bean
    @ConditionalOnMissingBean
    public SubAgentExecutor subAgentExecutor(OrchestratorProperties properties) {
        return new SubAgentExecutor(properties);
    }

    @Bean
    @ConditionalOnMissingBean(QueryRouter.class)
    public KeywordAndLlmRouter keywordAndLlmRouter(Optional<ChatModel> chatModel,
                                                   OrchestratorProperties properties) {
        return new KeywordAndLlmRouter(chatModel.orElse(null), properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResponseConsolidator responseConsolidator(ChatModel chatModel) {
        return new ResponseConsolidator(chatModel);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReActOrchestrator reActOrchestrator(SubAgentRegistry registry,
                                              SubAgentExecutor executor,
                                              ResponseConsolidator consolidator,
                                              QueryRouter router,
                                              ChatModel chatModel,
                                              OrchestratorProperties properties,
                                              Optional<ScopeEnforcer> scopeEnforcer,
                                              List<ResponseFilter> responseFilters) {
        return new ReActOrchestrator(registry, executor, consolidator, router,
                chatModel, properties, scopeEnforcer, responseFilters);
    }
}
