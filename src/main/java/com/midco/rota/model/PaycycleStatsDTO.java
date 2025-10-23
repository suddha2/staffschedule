package com.midco.rota.model;

import java.util.ArrayList;
import java.util.List;

public class PaycycleStatsDTO {
	public String region;
	public String period;
	public String periodId;
	public List<ServiceStatsDTO> services = new ArrayList<>();
}
