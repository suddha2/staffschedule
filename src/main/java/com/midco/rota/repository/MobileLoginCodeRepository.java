package com.midco.rota.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.MobileLoginCode;

@Repository
public interface MobileLoginCodeRepository extends JpaRepository<MobileLoginCode, Long> {

	/** Most recent un-consumed code for an email — the one a verify attempt checks. */
	Optional<MobileLoginCode> findFirstByEmailAndConsumedFalseOrderByCreatedAtDesc(String email);

	/** How many codes have been requested for an email since the given time (rate-limiting). */
	long countByEmailAndCreatedAtAfter(String email, LocalDateTime since);

	/** Remove every outstanding login code for an email (on deactivation / delete). */
	long deleteByEmailIgnoreCase(String email);
}
