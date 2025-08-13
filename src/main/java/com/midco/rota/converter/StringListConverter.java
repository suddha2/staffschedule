package com.midco.rota.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> list) {
    	return (list != null && !list.isEmpty()) ? String.join(",", list) : "";
    }

    @Override
    public List<String> convertToEntityAttribute(String joined) {
    	return (joined != null && !joined.isEmpty()) ?
                Arrays.stream(joined.split(","))
                      .map(String::trim)
                      .collect(Collectors.toList())
                : List.of();
    }
}