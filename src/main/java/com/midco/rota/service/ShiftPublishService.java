package com.midco.rota.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.midco.rota.dto.PublishHistoryDTO;
import com.midco.rota.dto.PublishLogEntryDTO;
import com.midco.rota.dto.PublishResultDTO;
import com.midco.rota.model.PublishLog;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.repository.PublishLogRepository;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.repository.ShiftAssignmentRepository;

@Service
public class ShiftPublishService {

	private static final Logger log = LoggerFactory.getLogger(ShiftPublishService.class);

	private final RotaRepository rotaRepository;
	private final FcmPushNotificationService fcm;
	private final PublishLogRepository publishLogRepository;
	private final ShiftAssignmentRepository shiftAssignmentRepository;

	public ShiftPublishService(RotaRepository rotaRepository, FcmPushNotificationService fcm,
			PublishLogRepository publishLogRepository,
			ShiftAssignmentRepository shiftAssignmentRepository) {
		this.rotaRepository = rotaRepository;
		this.fcm = fcm;
		this.publishLogRepository = publishLogRepository;
		this.shiftAssignmentRepository = shiftAssignmentRepository;
	}

	public PublishResultDTO publishUnallocatedShifts(Long rotaId, String publishedBy) {
		return publishUnallocatedShifts(rotaId, null, publishedBy);
	}

	public PublishResultDTO publishUnallocatedShifts(Long rotaId, String service, String publishedBy) {
		Rota rota = rotaRepository.findById(rotaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota not found: " + rotaId));

		List<ShiftAssignment> matching = rota.getShiftAssignmentList() == null ? List.of()
				: rota.getShiftAssignmentList().stream()
						.filter(sa -> sa.getEmployee() == null)
						.filter(sa -> matchesService(sa, service))
						.collect(Collectors.toList());
		long unallocated = matching.size();

		// Re-publishing re-advertises these slots: clear any withhold so they
		// reappear in Available and their response window starts fresh from this
		// publish.
		List<ShiftAssignment> toReset = matching.stream()
				.filter(ShiftAssignment::isWithheld)
				.collect(Collectors.toList());
		if (!toReset.isEmpty()) {
			toReset.forEach(sa -> sa.setWithheld(false));
			shiftAssignmentRepository.saveAll(toReset);
		}

		String scope = service == null ? "all services" : service;

		// Build the message regardless of count — we still log the attempt either way.
		String title = service == null ? "New shifts available" : "New shifts available at " + service;
		String body = unallocated + " unallocated shift" + (unallocated == 1 ? "" : "s")
				+ (service == null ? " open for request" : " at " + service);

		Optional<String> fcmMessageId = Optional.empty();
		String resultMessage;
		boolean broadcastSent = false;

		if (unallocated == 0) {
			log.info("publishUnallocatedShifts: rota {} ({}) has no unallocated shifts; skipping broadcast",
					rotaId, scope);
			resultMessage = service == null ? "No unallocated shifts to publish"
					: "No unallocated shifts at " + service + " to publish";
		} else {
			Map<String, String> data = new HashMap<>();
			data.put("rotaId", String.valueOf(rotaId));
			data.put("unallocatedCount", String.valueOf(unallocated));
			data.put("type", "UNALLOCATED_SHIFTS_PUBLISHED");
			if (service != null) {
				data.put("service", service);
			}

			fcmMessageId = fcm.broadcastToEmployeesTopic(title, body, data);
			broadcastSent = fcmMessageId.isPresent();

			log.info("publishUnallocatedShifts: rota {} ({}) attempt broadcast {} unallocated shifts; sent={} id={}",
					rotaId, scope, unallocated, broadcastSent, fcmMessageId.orElse("<none>"));

			if (broadcastSent) {
				resultMessage = "Broadcast sent to employees topic for " + unallocated
						+ " unallocated shift(s)" + (service == null ? "" : " at " + service);
			} else {
				resultMessage = "Publish recorded but FCM broadcast did not go out (Firebase unavailable or rejected the message)";
			}
		}

		// Audit-log every publish attempt — including zero-unallocated no-ops and FCM failures.
		PublishLog entry = new PublishLog();
		entry.setRotaId(rotaId);
		entry.setService(service);
		entry.setPublishedBy(publishedBy);
		entry.setUnallocatedCount((int) unallocated);
		entry.setBroadcastSent(broadcastSent);
		entry.setFcmMessageId(fcmMessageId.orElse(null));
		entry.setNotificationTitle(unallocated > 0 ? title : null);
		entry.setNotificationBody(unallocated > 0 ? body : null);
		publishLogRepository.save(entry);

		long totalCount = publishLogRepository.countForRotaAndService(rotaId, service);

		return new PublishResultDTO(rotaId, service, (int) unallocated, broadcastSent, resultMessage,
				fcmMessageId.orElse(null), totalCount);
	}

	public PublishHistoryDTO getPublishHistory(Long rotaId, String service) {
		long count = publishLogRepository.countForRotaAndService(rotaId, service);
		PublishHistoryDTO dto = new PublishHistoryDTO();
		dto.setRotaId(rotaId);
		dto.setService(service);
		dto.setCount(count);

		publishLogRepository.findMostRecent(rotaId, service).ifPresent(latest -> {
			dto.setLastPublishedAt(latest.getPublishedAt());
			dto.setLastPublishedBy(latest.getPublishedBy());
			dto.setLastBroadcastSent(latest.isBroadcastSent());
			dto.setLastUnallocatedCount(latest.getUnallocatedCount());
			dto.setLastFcmMessageId(latest.getFcmMessageId());
		});
		return dto;
	}

	/** Most recent publish events (capped at 50) for the audit-trail drawer. */
	public List<PublishLogEntryDTO> getPublishLog(Long rotaId, String service) {
		return publishLogRepository
				.findLatestForRotaAndService(rotaId, service, PageRequest.of(0, 50))
				.stream()
				.map(PublishLogEntryDTO::fromEntity)
				.toList();
	}

	private static boolean matchesService(com.midco.rota.model.ShiftAssignment sa, String service) {
		if (service == null) {
			return true;
		}
		return sa.getShift() != null
				&& sa.getShift().getShiftTemplate() != null
				&& service.equals(sa.getShift().getShiftTemplate().getLocation());
	}
}
