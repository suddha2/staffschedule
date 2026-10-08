package com.midco.rota.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.midco.rota.model.ServiceLocation;

public interface ServiceLocationRepository extends JpaRepository<ServiceLocation, Long> {

    List<ServiceLocation> findByStaffrotaRegionAndActiveTrueOrderByNameAsc(String staffrotaRegion);

    /** Dropdown values for a region: the bridged staffrota string where known, else the canonical name. */
    @Query("select coalesce(s.staffrotaLocation, s.name) from ServiceLocation s "
            + "where s.staffrotaRegion = :region and s.active = true "
            + "order by coalesce(s.staffrotaLocation, s.name)")
    List<String> findServiceNamesByStaffrotaRegion(String region);
}
