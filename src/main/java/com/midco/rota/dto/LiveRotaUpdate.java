package com.midco.rota.dto;

import java.util.List;

/**
 * Compact envelope streamed to {@code /topic/rota/{rotaId}} on each new best
 * solution from a live solver session (P2). Deliberately small — just the slot
 * → employee mapping plus score/status — so it can be pushed at ~1 Hz without
 * flooding the socket. The frontend live grid (P4) applies {@link Slot} entries
 * by {@code assignmentId}.
 */
public record LiveRotaUpdate(
		Long rotaId,
		String status,
		String score,
		int assigned,
		int total,
		List<Slot> slots) {

	public record Slot(Long assignmentId, Integer employeeId, String employeeName, boolean pinned) {
	}
}
