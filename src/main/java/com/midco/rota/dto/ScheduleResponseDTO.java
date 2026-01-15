package com.midco.rota.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;

public class ScheduleResponseDTO {
	private Long rotaId;
	private LocalDate startDate;
	private LocalDate endDate;
	private List<ShiftAssignmentDTO> shiftAssignmentList;
	private List<EmployeeDTO> employeeList;

	// Getters and setters
	public Long getRotaId() {
		return rotaId;
	}

	public void setRotaId(Long rotaId) {
		this.rotaId = rotaId;
	}

//    public LocalDate getStartDate() { return startDate; }
//    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
//    
//    public LocalDate getEndDate() { return endDate; }
//    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

	public List<ShiftAssignmentDTO> getShiftAssignmentList() {
		return shiftAssignmentList;
	}

	public void setShiftAssignmentList(List<ShiftAssignmentDTO> list) {
		this.shiftAssignmentList = list;
	}

	public List<EmployeeDTO> getEmployeeList() {
		return employeeList;
	}

	public void setEmployeeList(List<EmployeeDTO> list) {
		this.employeeList = list;
	}

	// ✅ Converter from Entity
	public static ScheduleResponseDTO fromRota(Rota rota) {
		ScheduleResponseDTO dto = new ScheduleResponseDTO();
		dto.setRotaId(rota.getId());

		// Convert assignments
		dto.setShiftAssignmentList(rota.getShiftAssignmentList().stream().map(ShiftAssignmentDTO::fromEntity)
				.collect(Collectors.toList()));

		// Convert employees
		dto.setEmployeeList(rota.getEmployeeList().stream().map(EmployeeDTO::fromEntity).collect(Collectors.toList()));

		return dto;
	}

	public static ScheduleResponseDTO fromRotaAndFullEmpList(Rota rota, List<Employee> fullEmpList) {
		ScheduleResponseDTO dto = new ScheduleResponseDTO();
		dto.setRotaId(rota.getId());

		// Convert assignments
		dto.setShiftAssignmentList(rota.getShiftAssignmentList().stream().map(ShiftAssignmentDTO::fromEntity)
				.collect(Collectors.toList()));

		// Convert employees
		dto.setEmployeeList(fullEmpList.stream().map(EmployeeDTO::fromEntity).collect(Collectors.toList()));

		return dto;
	}
}