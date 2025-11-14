package com.midco.rota.model;

import java.math.BigDecimal;

import com.midco.rota.util.RateCode;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "rate_card")
@NamedQuery(name = "RateCard.findAllRates", query = "SELECT r FROM RateCard r")
public class RateCard {
	@Id
	private Long id;
	private String region;
	private String rateType;
	private BigDecimal rate;
	@Enumerated(EnumType.STRING)
	private RateCode level;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getRateType() {
		return rateType;
	}

	public void setRateType(String rateType) {
		this.rateType = rateType;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public RateCode getRateCode() {
		return level;
	}

	public void setRateCode(RateCode rateCode) {
		this.level = rateCode;
	}

}
