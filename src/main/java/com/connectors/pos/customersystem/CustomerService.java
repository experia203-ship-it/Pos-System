package com.connectors.pos.customersystem;

import com.connectors.pos.customersystem.customerdtos.CustomerCreateDto;
import com.connectors.pos.customersystem.customerdtos.CustomerMapper;
import com.connectors.pos.customersystem.customerdtos.CustomerUpdateDto;
import com.connectors.pos.customersystem.customerdtos.CustomerViewDto;
import com.connectors.pos.exceptions.CustomerNotFoundException;
import com.connectors.pos.i18n.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CustomerService {

    private final CustomerRepository customerRepo;

    private final CustomerMapper customerMapper;

    @Transactional

    public Customer createCustomer(CustomerCreateDto create) {

        Customer customer = Customer.builder()
                .name(create.name())
                .shippingCompany(create.shippingCompany() == null ? "" : create.shippingCompany())
                .location(create.location() == null ? "" : create.location())
                .build();

        if (create.phoneNumbers() != null) {
            for (String number : create.phoneNumbers()) {
                customer.addCustomerPhone(CustomerPhone.builder().phoneNumber(number)
                        .type(Type.MOBILE_PHONE)
                        .build());

            }
        }

        return customerRepo.save(customer);
    }

    @Transactional(readOnly = true)

    public List<Customer> viewAllCustomers() {


        return customerRepo.findAll();
    }


    @Transactional(readOnly = true)

    public CustomerViewDto findByCustomerId(Long id) {

        Customer customer = customerRepo.findById(id).
                orElseThrow(() -> new CustomerNotFoundException(Messages.get("error.customer.notFound")));

        return customerMapper.toResponse(customer);
    }


    @Transactional(readOnly = true)

    public Page<CustomerViewDto> getAllCustomers(Pageable pageable) {

        Page<Customer> res = customerRepo.findAll(pageable);

        return res.map(customerMapper::toResponse);


    }


    @Transactional(readOnly = true)
    public Page<CustomerViewDto> findCustomerByName(String name, Pageable pageable) {

        Page<Customer> result = customerRepo.findByPartialNameMatch(name, pageable);

        return result.map(customerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CustomerViewDto> findCustomerByKeyword(String keyword, Pageable pageable) {

        Page<Customer> result = customerRepo.findByPartialNameMatch(keyword, pageable);

        return result.map(customerMapper::toResponse);
    }

    @Transactional

    public CustomerViewDto updateCustomerInformation(Long customerId, CustomerUpdateDto update) {

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(Messages.get("error.customer.notFoundWithId", customerId)));

        customerMapper.updateCustomerFromDto(update, customer);

        if (update.phoneNumber() != null) {
            updatePhoneNumber(customer, update.phoneNumber());
        }
        return customerMapper.toResponse(customer);
    }


    private void updatePhoneNumber(Customer customer, String newNumber) {


        Optional<CustomerPhone> primary = customer.getCustomerPhones().stream()
                .filter(CustomerPhone::isPrimary)
                .findFirst();

        if (primary.isPresent()) {

            primary.get().setPhoneNumber(newNumber);
        } else {
            CustomerPhone newPhone = CustomerPhone.builder().
                    phoneNumber(newNumber)
                    .customer(customer)
                    .isPrimary(true)
                    .type(Type.MOBILE_PHONE)
                    .build();
            customer.addCustomerPhone(newPhone);

        }
    }

    @Transactional
    public void deleteCustomerById(Long id) {
        if (id == null) {
            throw new CustomerNotFoundException(Messages.get("error.customer.idRequired"));
        }

        Customer customer = customerRepo.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(Messages.get("error.customer.notExistWithId")));

        customer.setActive(false);
    }

}
