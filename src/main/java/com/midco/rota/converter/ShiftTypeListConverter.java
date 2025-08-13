package com.midco.rota.converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.midco.rota.util.ShiftType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ShiftTypeListConverter implements AttributeConverter<List<ShiftType>, String> {

    @Override
    public String convertToDatabaseColumn(List<ShiftType> types) {
        return (types != null && !types.isEmpty())
                ? types.stream()
                       .map(ShiftType::name)
                       .collect(Collectors.joining(","))
                : "";
    }

    @Override
    public List<ShiftType> convertToEntityAttribute(String joined) {
        return (joined != null && !joined.isBlank())
                ? Arrays.stream(joined.split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .map(ShiftType::valueOf)
                        .collect(Collectors.toList())
                : List.of();
    }
}