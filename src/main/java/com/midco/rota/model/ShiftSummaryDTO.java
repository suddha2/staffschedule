package com.midco.rota.model;

import java.math.BigDecimal;

public class ShiftSummaryDTO {
	public int count = 0;
	public BigDecimal hours = BigDecimal.ZERO;

	public void add(BigDecimal hours2) {
		this.count++;
		this.hours = this.hours.add(hours2);
	}
}
