package com.midco.rota.dto;

import java.time.LocalDateTime;

public class PublishHistoryDTO {

	private Long rotaId;
	/** Null for the "publish all services" history. */
	private String service;
	private long count;
	private LocalDateTime lastPublishedAt;
	private String lastPublishedBy;
	private Boolean lastBroadcastSent;
	private Integer lastUnallocatedCount;
	private String lastFcmMessageId;

	public PublishHistoryDTO() {
	}

	public Long getRotaId() {
		return rotaId;
	}

	public void setRotaId(Long rotaId) {
		this.rotaId = rotaId;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public LocalDateTime getLastPublishedAt() {
		return lastPublishedAt;
	}

	public void setLastPublishedAt(LocalDateTime lastPublishedAt) {
		this.lastPublishedAt = lastPublishedAt;
	}

	public String getLastPublishedBy() {
		return lastPublishedBy;
	}

	public void setLastPublishedBy(String lastPublishedBy) {
		this.lastPublishedBy = lastPublishedBy;
	}

	public Boolean getLastBroadcastSent() {
		return lastBroadcastSent;
	}

	public void setLastBroadcastSent(Boolean lastBroadcastSent) {
		this.lastBroadcastSent = lastBroadcastSent;
	}

	public Integer getLastUnallocatedCount() {
		return lastUnallocatedCount;
	}

	public void setLastUnallocatedCount(Integer lastUnallocatedCount) {
		this.lastUnallocatedCount = lastUnallocatedCount;
	}

	public String getLastFcmMessageId() {
		return lastFcmMessageId;
	}

	public void setLastFcmMessageId(String lastFcmMessageId) {
		this.lastFcmMessageId = lastFcmMessageId;
	}
}
