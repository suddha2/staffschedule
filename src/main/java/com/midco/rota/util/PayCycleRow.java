package com.midco.rota.util;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class PayCycleRow {
	private Long id;
	private String name;
	private String location;
	private LocalDate startDate;
	private LocalDate endDate;
	private boolean hasSolveRequest;
	private String solevReqStatus;
	private Integer employeeCount;
	private Integer locationCount;
	private Integer shiftCount;
	private Long rotaId;
	private Map<ShiftType,Integer> shiftStats;
	private Map<String,Integer> shiftAssignmentStats;

	private PayCycleRow(Builder builder) {
		this.id = builder.id;
		this.name = builder.name;
		this.startDate = builder.startDate;
		this.endDate = builder.endDate;
		this.hasSolveRequest = builder.hasSolveRequest;
		this.employeeCount = builder.employeeCount;
		this.locationCount = builder.locationCount;
		this.shiftCount = builder.shiftCount;
		this.shiftStats = builder.shiftStats;
		this.shiftAssignmentStats=builder.shiftAssignmentStats;
		this.location=builder.location;
		this.rotaId=builder.rotaId;
		this.solevReqStatus=builder.solevReqStatus;
	}
	
	public static Builder builder() {
        return new Builder();
    }

	public static class Builder {
		private Long id;
		private String name;
		private String location;
		private LocalDate startDate;
		private LocalDate endDate;
		private boolean hasSolveRequest;
		private String solevReqStatus;
		private Integer employeeCount;
		private Integer locationCount;
		private Integer shiftCount;
		private Long rotaId;
		private Map<ShiftType,Integer > shiftStats = new HashMap<>();
		private Map<String,Integer > shiftAssignmentStats = new HashMap<>();

		public Builder withCore(Long id, String name, LocalDate start, LocalDate end,String location) {
			this.id = id;
			this.name = name;
			this.startDate = start;
			this.endDate = end;
			this.location=location;
			return this;
		}

		public Builder withSolveRequest(boolean flag) {
			this.hasSolveRequest = flag;
			return this;
		}public Builder withSolevReqStatus(String status) {
			this.solevReqStatus = status;
			return this;
		}

		public Builder withStats(int empCount, int locCount,int shiftCount, Long rotaId, Map<ShiftType,Integer> shiftStats) {
			this.employeeCount = empCount;
			this.locationCount = locCount;
			this.shiftCount=shiftCount;
			this.shiftStats = shiftStats;
			this.rotaId=rotaId;
			return this;
		}
		public Builder withShiftAssignmentStats(Map<String,Integer> shiftAssignmentStats) {
			this.shiftAssignmentStats = shiftAssignmentStats;
			return this;
		}
		public PayCycleRow build() {
			return new PayCycleRow(this);
		}

		public static Builder builder() {
			return new Builder();
		}
	}

	public Long getId() {
		return id;
	}
	public Long getRotaId() {
		return rotaId;
	}
	public String getName() {
		return name;
	}
	public String getLocation() {
		return location;
	}
	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public boolean isHasSolveRequest() {
		return hasSolveRequest;
	}

	public Integer getEmployeeCount() {
		return employeeCount;
	}

	public Integer getLocationCount() {
		return locationCount;
	}
	public Integer getShiftCount() {
		return shiftCount;
	}

	public Map<ShiftType,Integer> getShiftStats() {
		return shiftStats;
	}
	public Map<String,Integer> getShiftAssignmentStats() {
		return shiftAssignmentStats;
	}
	public String getSolevReqStatus() {
		return solevReqStatus;
	}
}
