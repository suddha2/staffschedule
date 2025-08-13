package com.midco.rota.converter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DayOfWeekListConverter implements AttributeConverter<List<DayOfWeek>, String> {

	@Override
	public String convertToDatabaseColumn(List<DayOfWeek> days) {
		return (days != null && !days.isEmpty()) ? days.stream().map(DayOfWeek::name).collect(Collectors.joining(","))
				: "";
	}

	@Override
	public List<DayOfWeek> convertToEntityAttribute(String joined) {
		return (joined != null && !joined.isBlank()) ? Arrays.stream(joined.split(",")).map(String::trim)
				.map(String::toUpperCase).map(DayOfWeek::valueOf).collect(Collectors.toList()) : List.of();
	}
}
