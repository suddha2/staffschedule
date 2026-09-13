package com.midco.rota.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.ConstraintSetting;

/** Per-constraint weights and hard/soft severity, keyed by constraint name. */
@Repository
public interface ConstraintSettingRepository extends JpaRepository<ConstraintSetting, String> {
}
