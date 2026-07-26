package io.datasquire.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the DataSquire orchestrator.
 */
@ConfigurationProperties(prefix = "datasquire.orchestrator")
public class OrchestratorProperties {

    private int maxIterations = 5;
    private double qualityThreshold = 0.7;
    private int timeoutSeconds = 60;
    private int subagentTimeoutSeconds = 30;
    private String executionMode = "parallel";
    private String mode = "react";

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public double getQualityThreshold() {
        return qualityThreshold;
    }

    public void setQualityThreshold(double qualityThreshold) {
        this.qualityThreshold = qualityThreshold;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getSubagentTimeoutSeconds() {
        return subagentTimeoutSeconds;
    }

    public void setSubagentTimeoutSeconds(int subagentTimeoutSeconds) {
        this.subagentTimeoutSeconds = subagentTimeoutSeconds;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
