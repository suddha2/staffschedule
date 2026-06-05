package com.midco.rota.controller;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.RateTableProvider;
import com.midco.rota.dto.BulkUpdateRequest;
import com.midco.rota.dto.ShiftTemplateDTO;
import com.midco.rota.dto.ShiftTemplateRequest;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.ShiftTemplateRepository;
import com.midco.rota.util.ShiftType;

@RestController
@RequestMapping("/api/shift-templates")
public class ShiftTemplateController {

	private final ShiftTemplateRepository shiftTemplateRepository;

	public ShiftTemplateController(ShiftTemplateRepository shiftTemplateRepository) {
		this.shiftTemplateRepository = shiftTemplateRepository;
	}

	// ========== CRUD Operations ==========

	/**
	 * GET all shift templates
	 */
	@GetMapping
	public ResponseEntity<List<ShiftTemplate>> getAllShiftTemplates(@RequestParam(required = false) Boolean active) {

		List<ShiftTemplate> templates;
		if (active != null && active) {
			templates = shiftTemplateRepository.findByActiveTrue();
		} else {
			templates = shiftTemplateRepository.findAll();
		}
		return ResponseEntity.ok(templates);
	}

	/**
	 * GET shift template by ID
	 */
	@GetMapping("/{id}")
	public ResponseEntity<ShiftTemplate> getShiftTemplateById(@PathVariable Integer id) {
		return shiftTemplateRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	/**
	 * POST - Create new shift template
	 */
	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PostMapping
	public ResponseEntity<?> createShiftTemplate(@RequestBody ShiftTemplateRequest request) {
		try {
			// Ensure id is null for new entity
			request.setId(null);

			// Set active to true if not specified
			if (!request.isActive()) {
				request.setActive(true);
			}

			// Dedup check up-front, BEFORE any insert, so a clash on day N doesn't
			// leave templates for days 1..N-1 already persisted. Strict equality
			// on the natural key including break window — same time with a
			// different break is the legitimate 2-carer pattern and is allowed.
			List<Map<String, Object>> conflicts = new ArrayList<>();
			for (DayOfWeek day : request.getDaysOfWeek()) {
				// Native query: enum params come in as their stringified name.
				List<ShiftTemplate> clashes = shiftTemplateRepository.findActiveDuplicates(
						request.getRegion(), request.getLocation(),
						day.name(),
						request.getShiftType() != null ? request.getShiftType().name() : null,
						request.getStartTime(), request.getEndTime(),
						request.getBreakStart(), request.getBreakEnd(),
						null);
				if (!clashes.isEmpty()) {
					Map<String, Object> entry = new HashMap<>();
					entry.put("dayOfWeek", day.name());
					entry.put("conflictingTemplateId", clashes.get(0).getId());
					conflicts.add(entry);
				}
			}
			if (!conflicts.isEmpty()) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
						"error", "Active shift template(s) with the same slot and times already exist. " +
								"Edit those instead, or change a time / break to differentiate.",
						"conflicts", conflicts));
			}

			List<ShiftTemplate> created = new ArrayList<>();

			for (DayOfWeek day : request.getDaysOfWeek()) {
				ShiftTemplate template = new ShiftTemplate();
				template.setDayOfWeek(day);
				template.setStartTime(request.getStartTime());
				template.setEndTime(request.getEndTime());
				template.setShiftType(request.getShiftType());
				template.setRegion(request.getRegion());
				template.setActive(request.isActive());
				template.setBreakEnd(request.getBreakEnd());
				template.setBreakStart(request.getBreakStart());
				template.setEmpCount(request.getEmpCount());
				template.setGender(request.getGender());
				template.setLocation(request.getLocation());
				template.setRequiredGender(request.getRequiredGender());
				template.setRequiredSkills(request.getRequiredSkills());
				template.setShiftType(request.getShiftType());
				template.setTotalHours(request.getTotalHours());
				template.setPriority(request.getPriority());

				created.add(template);
			}
			created = shiftTemplateRepository.saveAll(created);

			return ResponseEntity.status(HttpStatus.CREATED).body(created);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * PUT - Update existing shift template
	 */
	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PutMapping("/{id}")
	public ResponseEntity<?> updateShiftTemplate(@PathVariable Integer id,
			@RequestBody ShiftTemplate shiftTemplateDetails) {

		return shiftTemplateRepository.findById(id).map(template -> {
			// Dedup check BEFORE mutating: would the saved tuple clash with
			// another active template? Excludes this same row from the check.
			// Strict natural-key equality (region, location, day, type, times,
			// breaks) — same time with a different break is the legitimate
			// 2-carer pattern and is allowed through.
			List<ShiftTemplate> clashes = shiftTemplateRepository.findActiveDuplicates(
					shiftTemplateDetails.getRegion(), shiftTemplateDetails.getLocation(),
					shiftTemplateDetails.getDayOfWeek() != null ? shiftTemplateDetails.getDayOfWeek().name() : null,
					shiftTemplateDetails.getShiftType() != null ? shiftTemplateDetails.getShiftType().name() : null,
					shiftTemplateDetails.getStartTime(), shiftTemplateDetails.getEndTime(),
					shiftTemplateDetails.getBreakStart(), shiftTemplateDetails.getBreakEnd(),
					id);
			if (!clashes.isEmpty()) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
						"error", "An active shift template with the same slot and times already exists. " +
								"Edit that template instead, or change a time / break to differentiate.",
						"conflictingTemplateId", clashes.get(0).getId()));
			}

			// Update all fields
			template.setLocation(shiftTemplateDetails.getLocation());
			template.setRegion(shiftTemplateDetails.getRegion());
			template.setShiftType(shiftTemplateDetails.getShiftType());
			template.setDayOfWeek(shiftTemplateDetails.getDayOfWeek());
			template.setStartTime(shiftTemplateDetails.getStartTime());
			template.setEndTime(shiftTemplateDetails.getEndTime());
			template.setBreakStart(shiftTemplateDetails.getBreakStart());
			template.setBreakEnd(shiftTemplateDetails.getBreakEnd());
			template.setTotalHours(shiftTemplateDetails.getTotalHours());
			template.setRequiredGender(shiftTemplateDetails.getRequiredGender());
			template.setRequiredSkills(shiftTemplateDetails.getRequiredSkills());
			template.setEmpCount(shiftTemplateDetails.getEmpCount());
			template.setPriority(shiftTemplateDetails.getPriority());
			template.setActive(shiftTemplateDetails.isActive());

			ShiftTemplate updatedTemplate = shiftTemplateRepository.save(template);
			return ResponseEntity.ok(updatedTemplate);
		}).orElse(ResponseEntity.notFound().build());
	}

	/**
	 * DELETE - Delete shift template
	 */
	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteShiftTemplate(@PathVariable Integer id) {
		return shiftTemplateRepository.findById(id).map(template -> {
			shiftTemplateRepository.delete(template);
			return ResponseEntity.noContent().<Void>build();
		}).orElse(ResponseEntity.notFound().build());
	}

	/**
	 * PATCH - Toggle active status
	 */
	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PatchMapping("/{id}/toggle-active")
	public ResponseEntity<ShiftTemplate> toggleActive(@PathVariable Integer id) {
		return shiftTemplateRepository.findById(id).map(template -> {
			template.setActive(!template.isActive());
			ShiftTemplate updated = shiftTemplateRepository.save(template);
			return ResponseEntity.ok(updated);
		}).orElse(ResponseEntity.notFound().build());
	}

	// ========== Query Operations ==========

	/**
	 * GET shift templates by region
	 */
	@GetMapping("/by-region/{region}")
	public ResponseEntity<List<ShiftTemplate>> getShiftTemplatesByRegion(@PathVariable String region,
			@RequestParam(required = false, defaultValue = "true") Boolean active) {

		List<ShiftTemplate> templates;
		if (active) {
			templates = shiftTemplateRepository.findByRegionAndActiveTrue(region);
		} else {
			templates = shiftTemplateRepository.findByRegion(region);
		}
		return ResponseEntity.ok(templates);
	}

	/**
	 * GET shift templates by location (service)
	 */
	@GetMapping("/by-location/{location}")
	public ResponseEntity<List<ShiftTemplate>> getShiftTemplatesByLocation(@PathVariable String location,
			@RequestParam(required = false, defaultValue = "true") Boolean active) {

		List<ShiftTemplate> templates;
		if (active) {
			templates = shiftTemplateRepository.findByLocationAndActiveTrue(location);
		} else {
			templates = shiftTemplateRepository.findByLocation(location);
		}
		return ResponseEntity.ok(templates);
	}

	/**
	 * GET shift templates by day of week
	 */
	@GetMapping("/by-day/{dayOfWeek}")
	public ResponseEntity<List<ShiftTemplate>> getShiftTemplatesByDay(@PathVariable DayOfWeek dayOfWeek) {

		List<ShiftTemplate> templates = shiftTemplateRepository.findByDayOfWeek(dayOfWeek);
		return ResponseEntity.ok(templates);
	}

	/**
	 * GET shift templates by shift type
	 */
	@GetMapping("/by-shift-type/{shiftType}")
	public ResponseEntity<List<ShiftTemplate>> getShiftTemplatesByShiftType(@PathVariable ShiftType shiftType) {

		List<ShiftTemplate> templates = shiftTemplateRepository.findByShiftType(shiftType);
		return ResponseEntity.ok(templates);
	}

	/**
	 * GET all distinct regions
	 */
	@GetMapping("/regions")
	public ResponseEntity<List<String>> getAllRegions() {
		List<String> regions = RateTableProvider.getAllRegions();
		return ResponseEntity.ok(regions);
	}

	/**
	 * GET all locations for a region
	 */
	@GetMapping("/regions/{region}/locations")
	public ResponseEntity<List<String>> getLocationsByRegion(@PathVariable String region) {
		List<String> locations = shiftTemplateRepository.findLocationsByRegion(region);
		return ResponseEntity.ok(locations);
	}

	/**
	 * GET templates ordered by priority
	 */
	@GetMapping("/by-priority")
	public ResponseEntity<List<ShiftTemplate>> getShiftTemplatesByPriority(
			@RequestParam(required = false) String region) {

		List<ShiftTemplate> templates;
		if (region != null && !region.isEmpty()) {
			templates = shiftTemplateRepository.findByRegionAndActiveTrueOrderByPriorityAsc(region);
		} else {
			templates = shiftTemplateRepository.findByActiveTrueOrderByPriorityAsc();
		}
		return ResponseEntity.ok(templates);
	}

	/**
	 * Bulk-propagate non-key field changes across every active template that
	 * shares (region, location, shiftType). Used by the admin form's "Bulk Edit
	 * Mode" — e.g. shift the daily break window from 13:00 to 14:00 for all 7
	 * days of a location's DAY templates in one call.
	 *
	 * <p>Cohort selection is the <i>existing</i> shift type — bulk update never
	 * changes type (the FE locks that dropdown in bulk mode). Each field in the
	 * {@code updates} block is null-safe: a null means "leave this field alone",
	 * a non-null value is applied to every matched template.
	 */
	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PutMapping("/bulk-update")
	public ResponseEntity<?> bulkUpdate(@RequestBody BulkUpdateRequest request) {
		if (request == null || request.getLocation() == null || request.getShiftType() == null
				|| request.getRegion() == null) {
			return ResponseEntity.badRequest().body(Map.of(
					"error", "location, shiftType and region are required"));
		}
		BulkUpdateRequest.TemplateFieldUpdates u = request.getUpdates();
		if (u == null) {
			return ResponseEntity.badRequest().body(Map.of(
					"error", "updates block is required"));
		}

		List<ShiftTemplate> templates = shiftTemplateRepository.findByLocationAndShiftTypeAndRegion(
				request.getLocation(), request.getShiftType(), request.getRegion());

		// Only ACTIVE rows participate — inactive templates are operationally
		// dead and we don't want a bulk-update to silently revive them.
		templates = templates.stream().filter(ShiftTemplate::isActive).collect(Collectors.toList());

		// Apply each field only when the request supplied a non-null value. The
		// old implementation called the setter unconditionally with whatever
		// Jackson had bound — which, because the FE nested everything under
		// `updates`, meant every field was null/default and the entire cohort
		// was silently deactivated and time-cleared. Null-guarding makes the
		// semantics explicit and protects partial-payload callers.
		templates.forEach(template -> {
			if (u.getStartTime() != null)      template.setStartTime(u.getStartTime());
			if (u.getEndTime() != null)        template.setEndTime(u.getEndTime());
			if (u.getBreakStart() != null)     template.setBreakStart(u.getBreakStart());
			if (u.getBreakEnd() != null)       template.setBreakEnd(u.getBreakEnd());
			if (u.getTotalHours() != null)     template.setTotalHours(u.getTotalHours());
			if (u.getRequiredGender() != null) template.setRequiredGender(u.getRequiredGender());
			if (u.getRequiredSkills() != null) template.setRequiredSkills(u.getRequiredSkills());
			if (u.getEmpCount() != null)       template.setEmpCount(u.getEmpCount());
			if (u.getPriority() != null)       template.setPriority(u.getPriority());
			if (u.getActive() != null)         template.setActive(u.getActive());
		});

		shiftTemplateRepository.saveAll(templates);

		return ResponseEntity.ok(Map.of(
				"updated", templates.size(),
				"message", templates.size() + " templates updated"));
	}

	@GetMapping("/match")
	public ResponseEntity<List<ShiftTemplateDTO>> getMatchingTemplates(@RequestParam String location,
			@RequestParam String shiftType, @RequestParam String region) {

		List<ShiftTemplate> templates = shiftTemplateRepository.findByLocationAndShiftTypeAndRegion(location,
				ShiftType.valueOf(shiftType), region);

		// Convert to DTOs
		List<ShiftTemplateDTO> dtos = templates.stream().map(this::convertToDTO)
				.sorted(Comparator.comparing(ShiftTemplateDTO::getDayOfWeek)) // Sort by day
				.collect(Collectors.toList());

		return ResponseEntity.ok(dtos);
	}

	private ShiftTemplateDTO convertToDTO(ShiftTemplate template) {
		ShiftTemplateDTO dto = new ShiftTemplateDTO();
		dto.setId(template.getId());
		dto.setLocation(template.getLocation());
		dto.setRegion(template.getRegion());
		dto.setShiftType(template.getShiftType());
		dto.setDayOfWeek(template.getDayOfWeek());
		dto.setStartTime(template.getStartTime());
		dto.setEndTime(template.getEndTime());
		dto.setBreakStart(template.getBreakStart());
		dto.setBreakEnd(template.getBreakEnd());
		dto.setTotalHours(template.getTotalHours());
		dto.setRequiredGender(template.getRequiredGender());
		dto.setRequiredSkills(template.getRequiredSkills());
		dto.setEmpCount(template.getEmpCount());
		dto.setPriority(template.getPriority());
		dto.setActive(template.isActive());
		return dto;
	}
}