package com.midco.rota.util;

public enum ContractType {
	PERMANENT(1), ZERO_HOURS(0);

	private final int priority;

	private ContractType(int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}
}
