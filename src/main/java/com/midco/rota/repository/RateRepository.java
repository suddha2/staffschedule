package com.midco.rota.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.midco.rota.model.RateCard;


public interface RateRepository extends JpaRepository<RateCard,Long>{
    List<RateCard> findAllRates();
	
}
