package com.midco.rota.dto;

import java.util.List;

/**
 * A name on carers' lists that matches no rota location or that a carer lists twice, or a house
 * name that can't go on a list. {@code suggestion} is the likely right name in this region, if
 * there is one; {@code action} is the fix to offer first.
 */
public record ServiceNameToFix(String name, String problem, String suggestion, String action, List<String> carerServices,
		List<String> carerRestrictions) {
}
