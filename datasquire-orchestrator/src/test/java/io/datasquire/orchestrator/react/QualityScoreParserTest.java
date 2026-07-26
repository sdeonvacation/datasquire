package io.datasquire.orchestrator.react;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReActOrchestrator.parseQualityScore (H6 fix).
 * Verifies the parser correctly extracts the first decimal from LLM responses
 * and does not concatenate multiple numbers.
 */
class QualityScoreParserTest {

    @ParameterizedTest(name = "parses \"{0}\" as {1}")
    @CsvSource({
            "0.9, 0.9",
            "0.85, 0.85",
            "1.0, 1.0",
            "0.0, 0.0",
            "0.5, 0.5"
    })
    void parsesSimpleDecimal(String input, double expected) {
        assertEquals(expected, ReActOrchestrator.parseQualityScore(input), 0.001);
    }

    @Test
    void parsesQualityWithSurroundingText() {
        assertEquals(0.9, ReActOrchestrator.parseQualityScore("Quality: 0.9"), 0.001);
    }

    @Test
    void handlesMultipleDecimals_extractsFirst() {
        // H6 bug: "Quality: 0.9 out of 1.0" previously produced "0.91.0"
        assertEquals(0.9, ReActOrchestrator.parseQualityScore("Quality: 0.9 out of 1.0"), 0.001);
    }

    @Test
    void handlesScoreWithExplanation() {
        assertEquals(0.75, ReActOrchestrator.parseQualityScore(
                "I would rate this 0.75 because it covers the main points"), 0.001);
    }

    @Test
    void clampsScoreAboveOne() {
        assertEquals(1.0, ReActOrchestrator.parseQualityScore("9.5"), 0.001);
    }

    @Test
    void parsesIntegerAsScore() {
        assertEquals(1.0, ReActOrchestrator.parseQualityScore("1"), 0.001);
    }

    @Test
    void returnsDefaultForNoNumbers() {
        assertEquals(0.5, ReActOrchestrator.parseQualityScore("no score here"), 0.001);
    }

    @Test
    void returnsDefaultForEmptyString() {
        assertEquals(0.5, ReActOrchestrator.parseQualityScore(""), 0.001);
    }

    @Test
    void handlesWhitespaceOnly() {
        assertEquals(0.5, ReActOrchestrator.parseQualityScore("   "), 0.001);
    }

    @Test
    void parsesDecimalFromSlashNotation() {
        assertEquals(0.8, ReActOrchestrator.parseQualityScore("Score: 0.8/1.0"), 0.001);
    }

    @Test
    void handlesNewlinesAndExtraText() {
        assertEquals(0.65, ReActOrchestrator.parseQualityScore(
                "Based on my analysis:\n0.65\nThe answer partially addresses the question."), 0.001);
    }

    @Test
    void zeroIntegerClampsToZero() {
        assertEquals(0.0, ReActOrchestrator.parseQualityScore("0"), 0.001);
    }

    @Test
    void returnsDefaultForNull() {
        assertEquals(0.5, ReActOrchestrator.parseQualityScore(null), 0.001);
    }

    @Test
    void ignoresLargeIntegersNotZeroOrOne() {
        // Pattern only matches [01] for bare integers, not "5" or "10"
        assertEquals(0.5, ReActOrchestrator.parseQualityScore("score is five"), 0.001);
    }
}
