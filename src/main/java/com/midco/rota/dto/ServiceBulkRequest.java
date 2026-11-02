package com.midco.rota.dto;

/**
 * A bulk update of one service name in a region. {@code action} is RENAME, MERGE or REMOVE;
 * {@code newName} is unused for REMOVE. MERGE onto the same name clears a name listed twice.
 */
public record ServiceBulkRequest(String region, String action, String name, String newName) {
}
