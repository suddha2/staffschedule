package com.midco.rota.model;

import java.math.BigDecimal;

import com.midco.rota.util.ShiftType;

public class ShiftTypeStatsDTO {
	 	public ShiftType shiftType;
	    public BigDecimal totalHours = BigDecimal.ZERO;
	    public BigDecimal allocatedHours = BigDecimal.ZERO;
	    public BigDecimal unallocatedHours = BigDecimal.ZERO;
	    public int shiftCount = 0;
	    public int allocationCount = 0;

	    public void addAllocated(BigDecimal hours) {
	        allocatedHours = allocatedHours.add(hours);
	    }

	    public void addTotal(BigDecimal hours) {
	        totalHours = totalHours.add(hours);
	        unallocatedHours = totalHours.subtract(allocatedHours);
	    }
}
