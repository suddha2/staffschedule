package com.midco.rota.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PeopleHR API connection + leave-sync settings, read from
 * {@code application-*.properties} under the {@code peoplehr.*} prefix. Mirrors
 * {@link PeoplePlannerProperties}; disabled by default until a base URL and key
 * are set.
 */
@Component
@ConfigurationProperties(prefix = "peoplehr")
public class PeopleHrProperties {

	private boolean enabled = false;

	/** PeopleHR API base URL. */
	private String baseUrl;

	/** API key. Sent as the Authorization header value (and/or in the request body per PeopleHR's contract). */
	private String apiKey;

	/** Path (appended to baseUrl) of the leave / holiday endpoint. */
	private String leavePath = "/Holiday";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getLeavePath() {
		return leavePath;
	}

	public void setLeavePath(String leavePath) {
		this.leavePath = leavePath;
	}
}
