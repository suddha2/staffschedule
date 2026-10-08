package com.midco.rota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Region master (table {@code region}, migration V021). Loaded from the People
 * Planner region-code mapping. {@code code}/{@code name} are the PP values;
 * {@code staffrotaRegion} is the operating region the solver uses (NULL = a PP
 * region staffrota does not operate, e.g. London/Luton). Several PP regions can
 * map to one staffrota region (Gloucestershire + Oxfordshire -> GLOUCOXF).
 */
@Entity
@Table(name = "region")
public class Region {

    @Id
    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "staffrota_region", length = 30)
    private String staffrotaRegion;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStaffrotaRegion() { return staffrotaRegion; }
    public void setStaffrotaRegion(String staffrotaRegion) { this.staffrotaRegion = staffrotaRegion; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
