package com.connectors.pos.customersystem;

import com.connectors.pos.customersystem.customerdtos.CustomerCreateDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CustomerService {

    private final CustomerRepository customerRepo;


    @Transactional

    public Customer createCustomer(CustomerCreateDto create){

        Customer customer = Customer.builder()
                .name(create.name())
                .shippingCompany(create.shippingCompany())
                .location(create.location())
                .build();

                return customerRepo.save(customer);
    }

    @Transactional(readOnly = true)

    public List<Customer> viewAllCustomers(){


        return customerRepo.findAll();
    }


    @Transactional(readOnly = true)

    public Customer findByCustomerId(Long id){

        return customerRepo.findById(id).
                orElseThrow(()-> new EntityNotFoundException(""));
    }
}
