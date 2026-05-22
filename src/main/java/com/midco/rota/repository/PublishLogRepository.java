package com.midco.rota.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.PublishLog;

@Repository
public interface PublishLogRepository extends JpaRepository<PublishLog, Long> {

	// `service` may legitimately be null (global publish) — the query treats a null
	// parameter as "match the NULL-service rows". Spring Data's derived methods can't
	// express this without two separate methods, hence the explicit JPQL.

	@Query("SELECT COUNT(p) FROM PublishLog p WHERE p.rotaId = :rotaId "
			+ "AND ((:service IS NULL AND p.service IS NULL) OR p.service = :service)")
	long countForRotaAndService(@Param("rotaId") Long rotaId, @Param("service") String service);

	@Query("SELECT p FROM PublishLog p WHERE p.rotaId = :rotaId "
			+ "AND ((:service IS NULL AND p.service IS NULL) OR p.service = :service) "
			+ "ORDER BY p.publishedAt DESC")
	java.util.List<PublishLog> findLatestForRotaAndService(@Param("rotaId") Long rotaId,
			@Param("service") String service,
			org.springframework.data.domain.Pageable pageable);

	default Optional<PublishLog> findMostRecent(Long rotaId, String service) {
		return findLatestForRotaAndService(rotaId, service,
				org.springframework.data.domain.PageRequest.of(0, 1)).stream().findFirst();
	}

	long countByRotaId(Long rotaId);

	/**
	 * Most recent publish time that covers a given service in a rota — i.e. the
	 * latest of that service's own publishes and any rota-wide ("all services")
	 * publish. Drives the response-window clock for a shift. Null if never published.
	 */
	@Query("SELECT MAX(p.publishedAt) FROM PublishLog p WHERE p.rotaId = :rotaId "
			+ "AND ((:service IS NULL AND p.service IS NULL) OR p.service IS NULL OR p.service = :service)")
	LocalDateTime findLatestPublishedAt(@Param("rotaId") Long rotaId, @Param("service") String service);
}
