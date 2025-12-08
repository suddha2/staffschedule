package com.midco.rota.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.midco.rota.model.ShiftAssignment;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {
}