package com.midco.rota.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.midco.rota.model.Region;

public interface RegionRepository extends JpaRepository<Region, String> {

    /** The operating regions the solver uses (distinct staffrota_region), for dropdowns. */
    @Query("select distinct r.staffrotaRegion from Region r "
            + "where r.staffrotaRegion is not null and r.active = true order by r.staffrotaRegion")
    List<String> findOperatingRegions();
}
