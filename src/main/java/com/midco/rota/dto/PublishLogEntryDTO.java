package com.midco.rota.dto;

import java.time.LocalDateTime;

import com.midco.rota.model.PublishLog;

/** One row of the publish audit trail, surfaced to the admin UI. */
public class PublishLogEntryDTO {

	private Long id;
	private LocalDateTime publishedAt;
	private String publishedBy;
	private String service;
	private int unallocatedCount;
	private boolean broadcastSent;
	private String fcmMessageId;
	private String notificationTitle;
	private String notificationBody;

	public PublishLogEntryDTO() {
	}

	public static PublishLogEntryDTO fromEntity(PublishLog p) {
		PublishLogEntryDTO dto = new PublishLogEntryDTO();
		dto.id = p.getId();
		dto.publishedAt = p.getPublishedAt();
		dto.publishedBy = p.getPublishedBy();
		dto.service = p.getService();
		dto.unallocatedCount = p.getUnallocatedCount();
		dto.broadcastSent = p.isBroadcastSent();
		dto.fcmMessageId = p.getFcmMessageId();
		dto.notificationTitle = p.getNotificationTitle();
		dto.notificationBody = p.getNotificationBody();
		return dto;
	}

	public Long getId() {
		return id;
	}

	public LocalDateTime getPublishedAt() {
		return publishedAt;
	}

	public String getPublishedBy() {
		return publishedBy;
	}

	public String getService() {
		return service;
	}

	public int getUnallocatedCount() {
		return unallocatedCount;
	}

	public boolean isBroadcastSent() {
		return broadcastSent;
	}

	public String getFcmMessageId() {
		return fcmMessageId;
	}

	public String getNotificationTitle() {
		return notificationTitle;
	}

	public String getNotificationBody() {
		return notificationBody;
	}
}
