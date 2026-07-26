package io.datasquire.starter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

/**
 * Captures operational metrics for DataSquire via Micrometer.
 */
public class DataSquireMetrics {

    private final Timer queryDuration;
    private final Counter queryCount;
    private final MeterRegistry registry;

    public DataSquireMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.queryDuration = Timer.builder("datasquire.query.duration")
                .description("Time taken to process queries")
                .register(registry);
        this.queryCount = Counter.builder("datasquire.query.count")
                .description("Total number of queries processed")
                .register(registry);
    }

    public void recordQueryLatency(Duration duration) {
        queryDuration.record(duration);
    }

    public void recordAgentCall(String agentName, boolean success) {
        Counter.builder("datasquire.agent.calls")
                .tag("agent", agentName)
                .tag("success", String.valueOf(success))
                .description("Sub-agent invocation count")
                .register(registry)
                .increment();
    }

    public void incrementQueryCount() {
        queryCount.increment();
    }

    public void recordIterationCount(int iterations) {
        registry.summary("datasquire.query.iterations",
                "description", "ReAct iterations per query").record(iterations);
    }
}
