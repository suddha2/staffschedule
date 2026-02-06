package com.midco.rota.service;

import com.midco.rota.model.PinnedTemplateAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.PinnedTemplateAssignmentRepository;
import com.midco.rota.repository.ShiftTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PinCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(PinCleanupService.class);

    private final PinnedTemplateAssignmentRepository pinnedTemplateAssignmentRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;

    public PinCleanupService(
            PinnedTemplateAssignmentRepository pinnedTemplateAssignmentRepository,
            ShiftTemplateRepository shiftTemplateRepository
    ) {
        this.pinnedTemplateAssignmentRepository = pinnedTemplateAssignmentRepository;
        this.shiftTemplateRepository = shiftTemplateRepository;
    }

    /**
     * Runs daily at 2 AM to cleanup orphaned pins
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOrphanedPins() {
        logger.info("Starting cleanup of orphaned pinned template assignments");

        List<PinnedTemplateAssignment> allPins = pinnedTemplateAssignmentRepository.findAll();
        int deletedCount = 0;

        for (PinnedTemplateAssignment pin : allPins) {
            ShiftTemplate template = shiftTemplateRepository.findById(pin.getShiftTemplateId().intValue())
                    .orElse(null);

            // Delete if template is missing or inactive
            if (template == null || !template.isActive()) {
                pinnedTemplateAssignmentRepository.delete(pin);
                deletedCount++;
                
                logger.debug("Deleted orphaned pin: templateId={}, employeeId={}, reason={}",
                        pin.getShiftTemplateId(),
                        pin.getEmployeeId(),
                        template == null ? "template_deleted" : "template_inactive");
            }
        }

        if (deletedCount > 0) {
            logger.info("Cleanup complete: deleted {} orphaned pins", deletedCount);
        } else {
            logger.info("Cleanup complete: no orphaned pins found");
        }
    }
}