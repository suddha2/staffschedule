package com.midco.rota.integration;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.midco.rota.util.AvailabilitySource;

/**
 * PeopleHR API leave feed. Same contract as {@link DataEngineClient} but for the
 * HR system: driven by {@link PeopleHrProperties}, safe no-op when disabled or on
 * failure, tagged {@code HR_API}.
 */
@Component
public class PeopleHrClient implements LeaveSource {

	private static final Logger logger = LoggerFactory.getLogger(PeopleHrClient.class);

	private final PeopleHrProperties props;
	private volatile RestClient restClient;

	public PeopleHrClient(PeopleHrProperties props) {
		this.props = props;
	}

	@Override
	public AvailabilitySource source() {
		return AvailabilitySource.HR_API;
	}

	@Override
	public boolean isEnabled() {
		return props.isEnabled() && props.getBaseUrl() != null && !props.getBaseUrl().isBlank();
	}

	@Override
	public List<LeaveRecord> fetch(LocalDate from, LocalDate to) {
		if (!isEnabled()) {
			logger.debug("PeopleHR sync disabled or no base URL; skipping leave fetch");
			return List.of();
		}
		try {
			PeopleHrLeaveRecord[] body = client()
					.get()
					.uri(uriBuilder -> uriBuilder.path(props.getLeavePath())
							.queryParam("from", from)
							.queryParam("to", to)
							.build())
					.retrieve()
					.body(PeopleHrLeaveRecord[].class);
			if (body == null) {
				return List.of();
			}
			return java.util.Arrays.stream(body)
					.map(r -> new LeaveRecord(r.getSourceEmployeeId(), r.getEmployeeEmail(), r.getStartDate(), r.getEndDate(),
							r.getType(), r.getExternalRef(), r.getReason(), r.isCancelled()))
					.toList();
		} catch (Exception e) {
			logger.warn("PeopleHR leave fetch failed ({} to {}): {}", from, to, e.toString());
			return List.of();
		}
	}

	private RestClient client() {
		RestClient c = restClient;
		if (c == null) {
			synchronized (this) {
				c = restClient;
				if (c == null) {
					RestClient.Builder b = RestClient.builder().baseUrl(props.getBaseUrl());
					if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
						b = b.defaultHeader(HttpHeaders.AUTHORIZATION, props.getApiKey());
					}
					c = b.build();
					restClient = c;
				}
			}
		}
		return c;
	}
}
