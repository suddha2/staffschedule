package com.midco.rota.dto;

import java.util.List;

/** What a bulk update will change, or did change, for the user to check. */
public record ServiceBulkPreview(int templates, List<String> carerServices, List<String> carerRestrictions,
		int services, int patterns, int publishLogs, int history, List<String> warnings) {
}
