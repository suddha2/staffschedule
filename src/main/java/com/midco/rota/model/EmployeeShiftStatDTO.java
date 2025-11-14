package com.midco.rota.model;

import java.util.List;

import com.midco.rota.util.ContractType;
import com.midco.rota.util.RateCode;

public class EmployeeShiftStatDTO {
	public String name;
	public String region; // added to support rate computation
	public RateCode rateCode;
	public ContractType contractType;
	public List<WeeklyShiftStatDTO> weeklyStats;

	public EmployeeShiftStatDTO(String name, ContractType contractType, String region, RateCode rateCode,
			List<WeeklyShiftStatDTO> weeklyStats) {
		super();
		this.name = name;
		this.contractType = contractType;
		this.region = region;
		this.rateCode = rateCode;
		this.weeklyStats = weeklyStats;
	}

}
