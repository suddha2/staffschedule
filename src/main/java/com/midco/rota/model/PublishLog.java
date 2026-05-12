package com.midco.rota.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "publish_log")
public class PublishLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "rota_id", nullable = false)
	private Long rotaId;

	/** Null when this was a global "publish all services" action. */
	@Column(name = "service", length = 255)
	private String service;

	@Column(name = "published_by", length = 100)
	private String publishedBy;

	@Column(name = "published_at", nullable = false)
	private LocalDateTime publishedAt = LocalDateTime.now();

	@Column(name = "unallocated_count", nullable = false)
	private int unallocatedCount;

	@Column(name = "broadcast_sent", nullable = false)
	private boolean broadcastSent;

	@Column(name = "fcm_message_id", length = 255)
	private String fcmMessageId;

	@Column(name = "notification_title", length = 255)
	private String notificationTitle;

	@Column(name = "notification_body", length = 500)
	private String notificationBody;

	public PublishLog() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getPublishedBy() {
		return publishedBy;
	}

	public void setPublishedBy(String publishedBy) {
		this.publishedBy = publishedBy;
	}

	public LocalDateTime getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(LocalDateTime publishedAt) {
		this.publishedAt = publishedAt;
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

	public String getFcmMessageId() {
		return fcmMessageId;
	}

	public void setFcmMessageId(String fcmMessageId) {
		this.fcmMessageId = fcmMessageId;
	}

	public String getNotificationTitle() {
		return notificationTitle;
	}

	public void setNotificationTitle(String notificationTitle) {
		this.notificationTitle = notificationTitle;
	}

	public String getNotificationBody() {
		return notificationBody;
	}

	public void setNotificationBody(String notificationBody) {
		this.notificationBody = notificationBody;
	}
}
