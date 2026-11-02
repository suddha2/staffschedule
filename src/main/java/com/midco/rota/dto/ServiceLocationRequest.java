package com.midco.rota.dto;

/**
 * Add or edit a service on the Manage Services screen. {@code location} is the rota name;
 * {@code parentId} is the service this one sits under (null for a top-level service);
 * {@code mobizioId} is the People Planner id, if known. {@code region} is used on create only.
 */
public record ServiceLocationRequest(String region, String name, String location, Long parentId, String mobizioId,
		Boolean active) {
}
