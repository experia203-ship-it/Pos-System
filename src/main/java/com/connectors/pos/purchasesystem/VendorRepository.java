package com.connectors.pos.purchasesystem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRepository extends JpaRepository<Vendor,Long> {


    Page<Vendor> findAll(Pageable pageable);

    @Query("select v from Vendor v where lower(v.name)  like lower(concat('%',:keyword,'%'))")
    Page<Vendor> filterByKeyword(Pageable pageable , @Param("keyword") String keyword);
}
