package com.midco.rota.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.Rota;

@Repository
public interface RotaRepository extends JpaRepository<Rota, Integer> {

	
	
}
