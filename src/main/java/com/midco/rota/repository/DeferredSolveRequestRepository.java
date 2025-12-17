package com.midco.rota.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.midco.rota.model.DeferredSolveRequest;

public interface DeferredSolveRequestRepository extends JpaRepository<DeferredSolveRequest, Long> {

	Optional<DeferredSolveRequest> findFirstByCompletedFalse();

	List<DeferredSolveRequest> findTop5ByOrderByCreatedAtDesc();

	List<DeferredSolveRequest> findByRegion(String location);

	DeferredSolveRequest findByRotaId(Long id);

	Optional<DeferredSolveRequest>  findByStartDateAndEndDateAndRegionAndCompleted(
		    LocalDate startDate, 
		    LocalDate endDate, 
		    String region,
		    boolean status
		);
}
