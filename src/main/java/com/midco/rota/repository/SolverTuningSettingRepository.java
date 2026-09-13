package com.midco.rota.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.SolverTuningSetting;

/** Numeric thresholds the constraint streams compare against, keyed by setting key. */
@Repository
public interface SolverTuningSettingRepository extends JpaRepository<SolverTuningSetting, String> {
}
