package io.datasquire.starter.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryRequest validation constraints (H4 fix).
 */
class QueryRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validRequest_passesValidation() {
        QueryRequest request = new QueryRequest("How many users?", "session-1", Map.of());
        Set<ConstraintViolation<QueryRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankQuery_failsNotBlank() {
        QueryRequest request = new QueryRequest("", "session-1", Map.of());
        Set<ConstraintViolation<QueryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("blank")));
    }

    @Test
    void queryExceeding10000chars_failsSize() {
        String longQuery = "x".repeat(10001);
        QueryRequest request = new QueryRequest(longQuery, "session-1", Map.of());
        Set<ConstraintViolation<QueryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("10,000")));
    }

    @Test
    void queryExactly10000chars_passes() {
        String maxQuery = "x".repeat(10000);
        QueryRequest request = new QueryRequest(maxQuery, "session-1", Map.of());
        Set<ConstraintViolation<QueryRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void sessionIdExceeding100chars_failsSize() {
        String longSessionId = "s".repeat(101);
        QueryRequest request = new QueryRequest("valid query", longSessionId, Map.of());
        Set<ConstraintViolation<QueryRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("100")));
    }

    @Test
    void sessionIdExactly100chars_passes() {
        String maxSessionId = "s".repeat(100);
        QueryRequest request = new QueryRequest("valid query", maxSessionId, Map.of());
        Set<ConstraintViolation<QueryRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void nullSessionId_passes() {
        QueryRequest request = new QueryRequest("valid query", null, Map.of());
        Set<ConstraintViolation<QueryRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void nullOptions_defaultsToEmptyMap() {
        QueryRequest request = new QueryRequest("valid query", null, null);
        assertNotNull(request.options());
        assertTrue(request.options().isEmpty());
    }
}
