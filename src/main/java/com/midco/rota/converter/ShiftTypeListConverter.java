package com.midco.rota.converter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.midco.rota.util.ShiftType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ShiftTypeListConverter implements AttributeConverter<List<ShiftType>, String> {

	private static final Map<String, ShiftType> aliasMap = Map.of("FLOAT", ShiftType.FLOATING, "FLOATS", ShiftType.FLOATING,"FLOATING",
			ShiftType.FLOATING, "NIGHTS", ShiftType.WAKING_NIGHT, "WAKING_NIGHT", ShiftType.WAKING_NIGHT, "LONGDAY",
			ShiftType.LONG_DAY, "LONG_DAY", ShiftType.LONG_DAY);

	@Override
	public String convertToDatabaseColumn(List<ShiftType> types) {
		return (types != null && !types.isEmpty())
				? types.stream().map(ShiftType::name).collect(Collectors.joining(","))
				: "";
	}

	@Override
	public List<ShiftType> convertToEntityAttribute(String joined) {
	
		return (joined != null && !joined.isBlank()) ? Arrays.stream(joined.split(",")).map(String::trim)
				.map(String::toUpperCase).map(s -> aliasMap.containsKey(s) ? aliasMap.get(s) : ShiftType.valueOf(s))
				.collect(Collectors.toList()) : List.of();
	}

}