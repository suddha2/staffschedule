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

    /**
     * Delete a single (template, week-of-period, employee) pin. Used by the
     * UNPIN change-type handler in ScheduleVersionService: when an admin
     * clicks "Unpin" on a Floating-employee row, each pinned assignment for
     * that employee in the current rota gets the matching template-pin
     * removed so the next solve is free to move them. The flush+clear
     * annotation matters because UNPIN runs after createPinsFromAllAssignments
     * has just re-inserted the same row in the same transaction — without
     * the flush the delete would no-op against an unflushed buffered insert.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PinnedTemplateAssignment p " +
            "WHERE p.shiftTemplateId = :templateId " +
            "  AND p.weekOfPeriod  = :weekOfPeriod " +
            "  AND p.employeeId    = :employeeId")
    void deleteByTemplateWeekEmployee(@Param("templateId") Long templateId,
                                      @Param("weekOfPeriod") Short weekOfPeriod,
                                      @Param("employeeId") Long employeeId);
}