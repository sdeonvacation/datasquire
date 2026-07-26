package io.datasquire.starter.controller;

import java.util.List;

/**
 * DTO describing a registered agent and its capabilities.
 *
 * @param name         agent identifier
 * @param description  human-readable purpose
 * @param capabilities list of capability names
 */
public record AgentInfoDto(String name, String description, List<String> capabilities) {
}
