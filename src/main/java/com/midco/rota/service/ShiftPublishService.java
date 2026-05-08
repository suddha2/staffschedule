package com.midco.rota.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.midco.rota.dto.PublishResultDTO;
import com.midco.rota.model.Rota;
import com.midco.rota.repository.RotaRepository;

@Service
public class ShiftPublishService {

	private static final Logger log = LoggerFactory.getLogger(ShiftPublishService.class);

	private final RotaRepository rotaRepository;
	private final FcmPushNotificationService fcm;

	public ShiftPublishService(RotaRepository rotaRepository, FcmPushNotificationService fcm) {
		this.rotaRepository = rotaRepository;
		this.fcm = fcm;
	}

	public PublishResultDTO publishUnallocatedShifts(Long rotaId) {
		return publishUnallocatedShifts(rotaId, null);
	}

	public PublishResultDTO publishUnallocatedShifts(Long rotaId, String service) {
		Rota rota = rotaRepository.findById(rotaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota not found: " + rotaId));

		long unallocated = rota.getShiftAssignmentList() == null ? 0
				: rota.getShiftAssignmentList().stream()
						.filter(sa -> sa.getEmployee() == null)
						.filter(sa -> matchesService(sa, service))
						.count();

		String scope = service == null ? "all services" : service;
		if (unallocated == 0) {
			log.info("publishUnallocatedShifts: rota {} ({}) has no unallocated shifts; skipping broadcast",
					rotaId, scope);
			return new PublishResultDTO(rotaId, service, 0, false,
					service == null ? "No unallocated shifts to publish"
							: "No unallocated shifts at " + service + " to publish");
		}

		Map<String, String> data = new HashMap<>();
		data.put("rotaId", String.valueOf(rotaId));
		data.put("unallocatedCount", String.valueOf(unallocated));
		data.put("type", "UNALLOCATED_SHIFTS_PUBLISHED");
		if (service != null) {
			data.put("service", service);
		}

		String title = service == null ? "New shifts available"
				: "New shifts available at " + service;
		String body = unallocated + " unallocated shift" + (unallocated == 1 ? "" : "s")
				+ (service == null ? " open for request" : " at " + service);
		fcm.broadcastToEmployeesTopic(title, body, data);

		log.info("publishUnallocatedShifts: rota {} ({}) broadcast {} unallocated shifts",
				rotaId, scope, unallocated);
		return new PublishResultDTO(rotaId, service, (int) unallocated, true,
				"Broadcast sent to employees topic for " + unallocated
						+ " unallocated shift(s)" + (service == null ? "" : " at " + service));
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
