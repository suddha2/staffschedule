package com.midco.rota.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * People Planner Data Engine API connection + leave-sync settings, read from
 * {@code application-*.properties} under the {@code peopleplanner.*} prefix.
 * Nothing is hardcoded, so URL and credentials differ per environment.
 *
 * <p>Disabled by default ({@code peopleplanner.enabled=false}); the daily leave
 * sync no-ops until it is switched on and pointed at a base URL.
 */
@Component
@ConfigurationProperties(prefix = "peopleplanner")
public class PeoplePlannerProperties {

	/** Master switch. When false the sync job does nothing. */
	private boolean enabled = false;

	/** Data Engine API base URL, e.g. https://dataengine.example.com. */
	private String baseUrl;

	/** API key / bearer token. Sent as the Authorization header value. */
	private String apiKey;

	/** Path (appended to baseUrl) of the leave / unavailability endpoint. */
	private String leavePath = "/api/employee-unavailability";

	/** Cron for the daily sync. Default: 03:30 every day. */
	private String syncCron = "0 30 3 * * *";

	/** How many days back / forward to request each run. */
	private int lookbackDays = 30;
	private int lookaheadDays = 120;

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

	public String getSyncCron() {
		return syncCron;
	}

	public void setSyncCron(String syncCron) {
		this.syncCron = syncCron;
	}

	public int getLookbackDays() {
		return lookbackDays;
	}

	public void setLookbackDays(int lookbackDays) {
		this.lookbackDays = lookbackDays;
	}

	public int getLookaheadDays() {
		return lookaheadDays;
	}

	public void setLookaheadDays(int lookaheadDays) {
		this.lookaheadDays = lookaheadDays;
	}
}
