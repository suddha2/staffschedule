package com.midco.rota.dto;

public class PublishResultDTO {

	private Long rotaId;
	private String service;            // null when publishing all services for the rota
	private int unallocatedCount;
	private boolean broadcastSent;
	private String message;
	private String fcmMessageId;       // null when broadcast not sent
	private long totalPublishCount;    // total publishes for this rota+service, including this one

	public PublishResultDTO() {
	}

	public PublishResultDTO(Long rotaId, String service, int unallocatedCount, boolean broadcastSent, String message) {
		this.rotaId = rotaId;
		this.service = service;
		this.unallocatedCount = unallocatedCount;
		this.broadcastSent = broadcastSent;
		this.message = message;
	}

	public PublishResultDTO(Long rotaId, String service, int unallocatedCount, boolean broadcastSent, String message,
			String fcmMessageId, long totalPublishCount) {
		this.rotaId = rotaId;
		this.service = service;
		this.unallocatedCount = unallocatedCount;
		this.broadcastSent = broadcastSent;
		this.message = message;
		this.fcmMessageId = fcmMessageId;
		this.totalPublishCount = totalPublishCount;
	}

	public String getFcmMessageId() {
		return fcmMessageId;
	}

	public void setFcmMessageId(String fcmMessageId) {
		this.fcmMessageId = fcmMessageId;
	}

	public long getTotalPublishCount() {
		return totalPublishCount;
	}

	public void setTotalPublishCount(long totalPublishCount) {
		this.totalPublishCount = totalPublishCount;
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
