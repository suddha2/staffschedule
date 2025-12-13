package com.midco.rota.controller;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<ShiftTemplate>> getAllShiftTemplates(
            @RequestParam(required = false) Boolean active) {
        
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
        return shiftTemplateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST - Create new shift template
     */
    @PostMapping
    public ResponseEntity<ShiftTemplate> createShiftTemplate(@RequestBody ShiftTemplate shiftTemplate) {
        try {
            // Ensure id is null for new entity
            shiftTemplate.setId(null);
            
            // Set active to true if not specified
            if (!shiftTemplate.isActive()) {
                shiftTemplate.setActive(true);
            }
            
            ShiftTemplate savedTemplate = shiftTemplateRepository.save(shiftTemplate);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTemplate);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT - Update existing shift template
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShiftTemplate> updateShiftTemplate(
            @PathVariable Integer id,
            @RequestBody ShiftTemplate shiftTemplateDetails) {
        
        return shiftTemplateRepository.findById(id)
                .map(template -> {
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
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE - Delete shift template
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShiftTemplate(@PathVariable Integer id) {
        return shiftTemplateRepository.findById(id)
                .map(template -> {
                    shiftTemplateRepository.delete(template);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH - Toggle active status
     */
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ShiftTemplate> toggleActive(@PathVariable Integer id) {
        return shiftTemplateRepository.findById(id)
                .map(template -> {
                    template.setActive(!template.isActive());
                    ShiftTemplate updated = shiftTemplateRepository.save(template);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== Query Operations ==========

    /**
     * GET shift templates by region
     */
    @GetMapping("/by-region/{region}")
    public ResponseEntity<List<ShiftTemplate>> getShiftTemplatesByRegion(
            @PathVariable String region,
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
    public ResponseEntity<List<ShiftTemplate>> getShiftTemplatesByLocation(
            @PathVariable String location,
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
    public ResponseEntity<List<ShiftTemplate>> getShiftTemplatesByDay(
            @PathVariable DayOfWeek dayOfWeek) {
        
        List<ShiftTemplate> templates = shiftTemplateRepository.findByDayOfWeek(dayOfWeek);
        return ResponseEntity.ok(templates);
    }

    /**
     * GET shift templates by shift type
     */
    @GetMapping("/by-shift-type/{shiftType}")
    public ResponseEntity<List<ShiftTemplate>> getShiftTemplatesByShiftType(
            @PathVariable ShiftType shiftType) {
        
        List<ShiftTemplate> templates = shiftTemplateRepository.findByShiftType(shiftType);
        return ResponseEntity.ok(templates);
    }

    /**
     * GET all distinct regions
     */
    @GetMapping("/regions")
    public ResponseEntity<List<String>> getAllRegions() {
        List<String> regions = shiftTemplateRepository.findAllRegion();
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
}