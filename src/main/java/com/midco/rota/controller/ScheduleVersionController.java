package com.midco.rota.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.dto.RollbackRequest;
import com.midco.rota.dto.SaveScheduleRequest;
import com.midco.rota.dto.VersionComparisonDTO;
import com.midco.rota.dto.VersionDetailDTO;
import com.midco.rota.dto.VersionHistoryDTO;
import com.midco.rota.service.ScheduleVersionService;


/**
 * REST API for Schedule Versioning
 */
@RestController
@RequestMapping("/api/schedules")

public class ScheduleVersionController {

	@Autowired
	private  ScheduleVersionService versionService;

	/**
	 * Get version history for a rota GET /api/schedules/{rotaId}/versions
	 */
	@GetMapping("/{rotaId}/versions")

	public ResponseEntity<VersionHistoryDTO> getVersionHistory(@PathVariable Long rotaId) {

		try {
			VersionHistoryDTO history = versionService.getVersionHistory(rotaId);
			return ResponseEntity.ok(history);
		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get current version for a rota GET /api/schedules/{rotaId}/versions/current
	 */
	@GetMapping("/{rotaId}/versions/current")

	public ResponseEntity<VersionDetailDTO> getCurrentVersion(@PathVariable Long rotaId,
			@RequestParam(required = false, defaultValue = "false") boolean highlightChanges) {

		try {
			VersionHistoryDTO history = versionService.getVersionHistory(rotaId);
			if (history.getCurrentVersion() == null) {
				return ResponseEntity.notFound().build();
			}

			VersionDetailDTO detail = versionService.getVersionDetail(history.getCurrentVersion().getVersionId(),
					highlightChanges);
			return ResponseEntity.ok(detail);
		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get specific version details GET /api/schedules/{rotaId}/versions/{versionId}
	 */
	@GetMapping("/{rotaId}/versions/{versionId}")

	public ResponseEntity<VersionDetailDTO> getVersionDetail(@PathVariable Long rotaId, @PathVariable Long versionId,
			@RequestParam(required = false, defaultValue = "false") boolean highlightChanges) {

		try {
			VersionDetailDTO detail = versionService.getVersionDetail(versionId, highlightChanges);
			return ResponseEntity.ok(detail);
		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Create new version (save current state) POST /api/schedules/{rotaId}/versions
	 */
	@PostMapping("/{rotaId}/versions")

	public ResponseEntity<VersionDetailDTO> createVersion(@PathVariable Long rotaId,
			@RequestBody SaveScheduleRequest request, Authentication authentication) {

		// Set rota ID from path
		request.setRotaId(rotaId);

		// Set username from authentication
		if (authentication != null) {
			request.setUsername(authentication.getName());
		}

		try {
			VersionDetailDTO version = versionService.createVersion(request);
			return ResponseEntity.status(HttpStatus.CREATED).body(version);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Compare two versions GET
	 * /api/schedules/{rotaId}/versions/compare?versionA=X&versionB=Y
	 */
	@GetMapping("/{rotaId}/versions/compare")

	public ResponseEntity<VersionComparisonDTO> compareVersions(@PathVariable Long rotaId, @RequestParam Long versionA,
			@RequestParam Long versionB) {

		try {
			VersionComparisonDTO comparison = versionService.compareVersions(versionA, versionB);
			return ResponseEntity.ok(comparison);
		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Rollback to a previous version POST
	 * /api/schedules/{rotaId}/versions/{versionId}/rollback
	 */
	@PostMapping("/{rotaId}/versions/{versionId}/rollback")

	public ResponseEntity<VersionDetailDTO> rollbackToVersion(@PathVariable Long rotaId, @PathVariable Long versionId,
			@RequestBody(required = false) RollbackRequest request, Authentication authentication) {

		// Create request if not provided
		if (request == null) {
			request = new RollbackRequest();
		}

		request.setTargetVersionId(versionId);

		if (authentication != null) {
			request.setUsername(authentication.getName());
		}

		try {
			VersionDetailDTO result = versionService.rollbackToVersion(request);
			return ResponseEntity.ok(result);
		} catch (Exception e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
