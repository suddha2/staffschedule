package com.midco.rota.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.midco.rota.model.ShiftTypeDef;

public interface ShiftTypeDefRepository extends JpaRepository<ShiftTypeDef, String> {

    List<ShiftTypeDef> findByActiveTrueOrderByDisplayNameAsc();
}
