package com.midco.rota.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.TopicManagementResponse;
import com.midco.rota.model.EmployeeDevice;
import com.midco.rota.repository.EmployeeDeviceRepository;

@Service
public class FcmPushNotificationService {

	private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationService.class);

	@Value("${firebase.fcm.employees-topic:employees}")
	private String employeesTopic;

	private final EmployeeDeviceRepository deviceRepository;

	public FcmPushNotificationService(EmployeeDeviceRepository deviceRepository) {
		this.deviceRepository = deviceRepository;
	}

	private boolean firebaseReady() {
		if (FirebaseApp.getApps().isEmpty()) {
			log.warn("FirebaseApp is not initialized — skipping push");
			return false;
		}
		return true;
	}

	public void broadcastToEmployeesTopic(String title, String body, Map<String, String> data) {
		if (!firebaseReady()) {
			return;
		}
		Map<String, String> payload = data == null ? new HashMap<>() : new HashMap<>(data);
		try {
			Message message = Message.builder()
					.setTopic(employeesTopic)
					.setNotification(Notification.builder().setTitle(title).setBody(body).build())
					.putAllData(stringifyAll(payload))
					.build();
			String response = FirebaseMessaging.getInstance().send(message);
			log.info("FCM topic broadcast sent to '{}': {}", employeesTopic, response);
		} catch (FirebaseMessagingException e) {
			log.error("FCM topic broadcast failed (topic={}): {}", employeesTopic, e.getMessage());
		}
	}

	public void sendToEmployee(Integer employeeId, String title, String body, Map<String, String> data) {
		if (!firebaseReady()) {
			return;
		}
		List<EmployeeDevice> devices = deviceRepository.findByEmployeeIdAndActiveTrue(employeeId);
		if (devices.isEmpty()) {
			log.info("No active devices for employee {} — skipping push", employeeId);
			return;
		}
		List<String> tokens = devices.stream().map(EmployeeDevice::getFcmToken).toList();
		sendToTokens(tokens, title, body, data);
	}

	public void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
		if (!firebaseReady() || tokens == null || tokens.isEmpty()) {
			return;
		}
		Map<String, String> payload = data == null ? new HashMap<>() : new HashMap<>(data);
		try {
			MulticastMessage message = MulticastMessage.builder()
					.addAllTokens(tokens)
					.setNotification(Notification.builder().setTitle(title).setBody(body).build())
					.putAllData(stringifyAll(payload))
					.build();
			var response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
			log.info("FCM multicast: {} success, {} failure (out of {})",
					response.getSuccessCount(), response.getFailureCount(), tokens.size());
			deactivateStaleTokens(tokens, response.getResponses());
		} catch (FirebaseMessagingException e) {
			log.error("FCM multicast failed: {}", e.getMessage());
		}
	}

	private void deactivateStaleTokens(List<String> tokens, List<SendResponse> responses) {
		for (int i = 0; i < responses.size(); i++) {
			SendResponse r = responses.get(i);
			if (r.isSuccessful() || r.getException() == null) {
				continue;
			}
			String code = r.getException().getMessagingErrorCode() == null
					? ""
					: r.getException().getMessagingErrorCode().name();
			if ("UNREGISTERED".equals(code) || "INVALID_ARGUMENT".equals(code)) {
				String token = tokens.get(i);
				deviceRepository.findByFcmToken(token).ifPresent(d -> {
					d.setActive(false);
					d.setLastSeenAt(LocalDateTime.now());
					deviceRepository.save(d);
					log.info("Deactivated stale FCM token for employee {} (code={})", d.getEmployee().getId(), code);
				});
			}
		}
	}

	public void subscribeToEmployeesTopic(String token) {
		if (!firebaseReady() || token == null || token.isBlank()) {
			return;
		}
		try {
			TopicManagementResponse resp = FirebaseMessaging.getInstance()
					.subscribeToTopic(Collections.singletonList(token), employeesTopic);
			log.info("Subscribed token to '{}': {} success, {} failure",
					employeesTopic, resp.getSuccessCount(), resp.getFailureCount());
		} catch (FirebaseMessagingException e) {
			log.error("Failed to subscribe token to topic '{}': {}", employeesTopic, e.getMessage());
		}
	}

	private Map<String, String> stringifyAll(Map<String, String> data) {
		Map<String, String> out = new HashMap<>();
		if (data != null) {
			data.forEach((k, v) -> out.put(k, v == null ? "" : v));
		}
		return out;
	}
}
