package com.midco.rota.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.midco.rota.model.DeferredSolveRequest;

public interface DeferredSolveRequestRepository extends JpaRepository<DeferredSolveRequest, Long> {

	Optional<DeferredSolveRequest>  findFirstByCompletedFalse();
}
