package com.midco.rota.integration;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client for the People Planner Data Engine API. All connection details
 * come from {@link PeoplePlannerProperties} (the {@code peopleplanner.*} app
 * properties), so nothing is hardcoded.
 *
 * <p>The {@link RestClient} is built lazily from the properties on first use, so
 * the app starts fine even when the integration is disabled or unconfigured.
 */
@Component
public class DataEngineClient {

	private static final Logger logger = LoggerFactory.getLogger(DataEngineClient.class);

	private final PeoplePlannerProperties props;
	private volatile RestClient restClient;

	public DataEngineClient(PeoplePlannerProperties props) {
		this.props = props;
	}

	/**
	 * Fetch leave / unavailability records overlapping [from, to]. Returns an
	 * empty list (never throws) when the integration is off, unconfigured, or the
	 * call fails — the caller treats "no data" and "couldn't reach PP" the same:
	 * leave nothing changed rather than wipe good local data.
	 */
	public List<PpLeaveRecord> fetchLeave(LocalDate from, LocalDate to) {
		if (!props.isEnabled() || props.getBaseUrl() == null || props.getBaseUrl().isBlank()) {
			logger.debug("People Planner sync disabled or no base URL; skipping leave fetch");
			return List.of();
		}
		try {
			PpLeaveRecord[] body = client()
					.get()
					.uri(uriBuilder -> uriBuilder.path(props.getLeavePath())
							.queryParam("from", from)
							.queryParam("to", to)
							.build())
					.retrieve()
					.body(PpLeaveRecord[].class);
			return body == null ? List.of() : List.of(body);
		} catch (Exception e) {
			logger.warn("People Planner leave fetch failed ({} to {}): {}", from, to, e.toString());
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
