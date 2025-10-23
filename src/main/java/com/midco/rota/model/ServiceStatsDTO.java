package com.midco.rota.model;

import java.util.ArrayList;
import java.util.List;

public class ServiceStatsDTO {
	public String location;
	public int shiftCount;
    public List<WeekStatsDTO> weeks = new ArrayList<>();
}
