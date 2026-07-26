package io.datasquire.core.exception;

/**
 * Thrown when an agent loop exceeds its configured maximum iteration count
 * without producing a complete result.
 */
public class MaxIterationsExceededException extends OrchestrationException {

    private final int maxIterations;

    /**
     * @param maxIterations the iteration limit that was exceeded
     */
    public MaxIterationsExceededException(int maxIterations) {
        super("MAX_ITERATIONS_EXCEEDED",
                "Agent loop exceeded maximum iterations: " + maxIterations);
        this.maxIterations = maxIterations;
    }

    /**
     * @return the maximum iteration limit that was exceeded
     */
    public int getMaxIterations() {
        return maxIterations;
    }
}
