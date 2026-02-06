package com.midco.rota.repository;

import com.midco.rota.model.PinnedTemplateAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PinnedTemplateAssignmentRepository extends JpaRepository<PinnedTemplateAssignment, Long> {

    List<PinnedTemplateAssignment> findByShiftTemplateId(Long shiftTemplateId);

    List<PinnedTemplateAssignment> findByEmployeeId(Long employeeId);

    void deleteByShiftTemplateId(Long shiftTemplateId);

    boolean existsByShiftTemplateIdAndEmployeeId(Long shiftTemplateId, Long employeeId);
}