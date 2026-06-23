package com.midco.rota.model;

import java.util.Map;

import com.midco.rota.util.ContractType;
import com.midco.rota.util.ShiftType;

/**
 * Employee plus their per-shift-type totals across a paycycle, in the same
 * shape the FE's `summarizedEmpList` consumes. Used by the floating-employee
 * panel's "Other Regions" tab.
 */
public class EmployeeWithSummaryDTO {

	private Integer id;
	private String firstName;
	private String lastName;
	private ContractType contractType;
	private String preferredRegion;
	private Map<ShiftType, ShiftSummaryDTO> shiftTypeSummary;

	public EmployeeWithSummaryDTO(Integer id, String firstName, String lastName, ContractType contractType,
			String preferredRegion, Map<ShiftType, ShiftSummaryDTO> shiftTypeSummary) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.contractType = contractType;
		this.preferredRegion = preferredRegion;
		this.shiftTypeSummary = shiftTypeSummary;
	}

	public Integer getId() { return id; }
	public String getFirstName() { return firstName; }
	public String getLastName() { return lastName; }
	public ContractType getContractType() { return contractType; }
	public String getPreferredRegion() { return preferredRegion; }
	public Map<ShiftType, ShiftSummaryDTO> getShiftTypeSummary() { return shiftTypeSummary; }
}
