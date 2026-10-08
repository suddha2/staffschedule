package com.midco.rota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Service-location master (table {@code service_location}, migration V021).
 * Loaded from the People Planner / Mobizio export. {@code serviceId} is the
 * stable property id (= cf_db {@code property_identifier}); {@code externalId}
 * is the PP external id ({@code regionCode-serviceId}); {@code name} is the
 * canonical PP/Mobizio service name; {@code staffrotaRegion} is the operating
 * region used for dropdown filtering (may be NULL for non-operated regions).
 */
@Entity
@Table(name = "service_location")
public class ServiceLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 40, nullable = false)
    private String externalId;

    @Column(name = "region_code", length = 10, nullable = false)
    private String regionCode;

    @Column(name = "region_name", length = 100, nullable = false)
    private String regionName;

    @Column(name = "staffrota_region", length = 30)
    private String staffrotaRegion;

    @Column(name = "service_id", length = 20, nullable = false)
    private String serviceId;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "mobizio_id", length = 40)
    private String mobizioId;

    /** The staffrota location string this service maps to (bridge, V022); null until reconciled. */
    @Column(name = "staffrota_location", length = 80)
    private String staffrotaLocation;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getRegionCode() { return regionCode; }
    public String getRegionName() { return regionName; }
    public String getStaffrotaRegion() { return staffrotaRegion; }
    public String getServiceId() { return serviceId; }
    public String getName() { return name; }
    public String getMobizioId() { return mobizioId; }
    public String getStaffrotaLocation() { return staffrotaLocation; }
    public boolean isActive() { return active; }
}
