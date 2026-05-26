package com.midco.rota.repository;

import com.midco.rota.model.PinnedTemplateAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PinnedTemplateAssignmentRepository extends JpaRepository<PinnedTemplateAssignment, Long> {

    List<PinnedTemplateAssignment> findByShiftTemplateId(Long shiftTemplateId);

    List<PinnedTemplateAssignment> findByEmployeeId(Long employeeId);

    void deleteByShiftTemplateId(Long shiftTemplateId);

    boolean existsByShiftTemplateIdAndEmployeeId(Long shiftTemplateId, Long employeeId);

    /**
     * Bulk-delete all pins for any of the given template ids. Used by
     * ScheduleVersionService.createPinsFromAllAssignments to implement
     * replace-on-save: every save first wipes pins for the templates this rota
     * touches, then re-inserts the current (template, employee) pairs. This keeps
     * pinned_template_assignment a mirror of the latest saved truth rather than an
     * append-only log.
     *
     * flushAutomatically + clearAutomatically ensure subsequent inserts in the
     * same transaction don't collide with rows still cached in the persistence
     * context or buffered pre-flush.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PinnedTemplateAssignment p WHERE p.shiftTemplateId IN :templateIds")
    void deleteByShiftTemplateIdIn(@Param("templateIds") Collection<Long> templateIds);
}