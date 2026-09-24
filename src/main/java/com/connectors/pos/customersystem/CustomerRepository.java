package com.connectors.pos.customersystem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer,Long> {


     Page<Customer> findCustomerByName(String name, Pageable pageable);



     Page<Customer> findCustomerByNameContainingIgnoreCase(String keyword,Pageable pageable);



     @Query("select c from Customer c where lower(c.name) like lower(concat('%',:keyword,'%'))")
     Page<Customer> findByPartialNameMatch(@Param("keyword") String keyword,Pageable pageable);

}
