package com.midco.rota;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.midco.rota.model.RateCard;
import com.midco.rota.repository.RateRepository;

@Component
public class RateTableProvider {
	private static Map<String, BigDecimal> RATE_MAP = Map.of();

	private final RateRepository repo;

	public RateTableProvider(RateRepository repo) {
		this.repo = repo;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		List<RateCard> rates = repo.findAllRates();
		RATE_MAP = rates.stream().collect(
				Collectors.toMap(r -> key(r.getRegion(), r.getRateType(), r.getRateCode().name()), RateCard::getRate));
	}

	private String key(String region, String rateType, String level) {
		return region + "_" + rateType + "_" + level;
	}

	public static BigDecimal getAmount(String region, String rateType, String level) {
		return RATE_MAP.get(region + "_" + rateType + "_" + level);
	}

	public static Map<String, BigDecimal> getAllRates() {
		return RATE_MAP;
	}

	public static List<String> getAllRegions() {
		return RATE_MAP.keySet().stream().map(key -> key.split("_")[0]) // Extract region from "region_rateType_level"
				.distinct().sorted().collect(Collectors.toList());
	}

}
