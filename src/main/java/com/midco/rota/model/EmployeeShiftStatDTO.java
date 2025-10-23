package com.midco.rota.model;

import java.util.List;

import com.midco.rota.util.ContractType;

public class EmployeeShiftStatDTO {
	public String name;
	public ContractType contractType;
	public List<WeeklyShiftStatDTO> weeklyStats;

	public EmployeeShiftStatDTO(String name, ContractType contractType, List<WeeklyShiftStatDTO> weeklyStats) {
		super();
		this.name = name;
		this.contractType = contractType;
		this.weeklyStats = weeklyStats;
	}

}
