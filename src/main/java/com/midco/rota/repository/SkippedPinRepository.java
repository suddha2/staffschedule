package com.midco.rota.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.midco.rota.model.SkippedPin;

public interface SkippedPinRepository extends JpaRepository<SkippedPin, Long> {

	List<SkippedPin> findByRotaIdOrderByShiftDateAscEmployeeNameAsc(Long rotaId);
}
