package com.midco.rota.dto;

public class PublishResultDTO {

	private Long rotaId;
	private String service;            // null when publishing all services for the rota
	private int unallocatedCount;
	private boolean broadcastSent;
	private String message;

	public PublishResultDTO() {
	}

	public PublishResultDTO(Long rotaId, String service, int unallocatedCount, boolean broadcastSent, String message) {
		this.rotaId = rotaId;
		this.service = service;
		this.unallocatedCount = unallocatedCount;
		this.broadcastSent = broadcastSent;
		this.message = message;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public Long getRotaId() {
		return rotaId;
	}

	public void setRotaId(Long rotaId) {
		this.rotaId = rotaId;
	}

	public int getUnallocatedCount() {
		return unallocatedCount;
	}

	public void setUnallocatedCount(int unallocatedCount) {
		this.unallocatedCount = unallocatedCount;
	}

	public boolean isBroadcastSent() {
		return broadcastSent;
	}

	public void setBroadcastSent(boolean broadcastSent) {
		this.broadcastSent = broadcastSent;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
