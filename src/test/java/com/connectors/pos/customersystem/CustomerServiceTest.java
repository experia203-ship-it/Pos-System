package com.connectors.pos.customersystem;

import com.connectors.pos.customersystem.customerdtos.CustomerCreateDto;
import com.connectors.pos.customersystem.customerdtos.CustomerMapper;
import com.connectors.pos.customersystem.customerdtos.CustomerUpdateDto;
import com.connectors.pos.customersystem.customerdtos.CustomerViewDto;
import com.connectors.pos.exceptions.CustomerNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepo;

    @Spy
    private CustomerMapper customerMapper = Mappers.getMapper(CustomerMapper.class);

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createsCustomerAndAssociatesEachPhone() {
        when(customerRepo.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer saved = customerService.createCustomer(
                new CustomerCreateDto("Ava Chen", "North street", "Parcel Co", List.of("111", "222")));

        assertThat(saved.getName()).isEqualTo("Ava Chen");
        assertThat(saved.getLocation()).isEqualTo("North street");
        assertThat(saved.getShippingCompany()).isEqualTo("Parcel Co");
        assertThat(saved.getCustomerPhones()).extracting(CustomerPhone::getPhoneNumber)
                .containsExactlyInAnyOrder("111", "222");
        assertThat(saved.getCustomerPhones()).allSatisfy(phone -> {
            assertThat(phone.getType()).isEqualTo(Type.MOBILE_PHONE);
            assertThat(phone.getCustomer()).isSameAs(saved);
        });
        verify(customerRepo).save(any(Customer.class));
    }

    @Test
    void allowsCustomerWithoutOptionalLocationOrShippingCompany() {
        when(customerRepo.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = customerService.createCustomer(new CustomerCreateDto("Ava Chen", null, null, null));

        assertThat(customer.getLocation()).isEmpty();
        assertThat(customer.getShippingCompany()).isEmpty();
        assertThat(customer.getCustomerPhones()).isEmpty();
    }

    @Test
    void updatesCustomerDetailsAndPrimaryPhone() {
        Customer customer = Customer.builder().id(7L).name("Old name").location("Old location")
                .shippingCompany("Old shipper").build();
        customer.addCustomerPhone(CustomerPhone.builder().phoneNumber("555").isPrimary(true).type(Type.MOBILE_PHONE).build());
        when(customerRepo.findById(7L)).thenReturn(Optional.of(customer));

        CustomerViewDto updated = customerService.updateCustomerInformation(7L,
                new CustomerUpdateDto("New name", "New location", "New shipper", "999"));

        assertThat(updated.name()).isEqualTo("New name");
        assertThat(updated.location()).isEqualTo("New location");
        assertThat(updated.shippingCompany()).isEqualTo("New shipper");
        assertThat(updated.phoneNumber()).isEqualTo("999");
        assertThat(customer.getCustomerPhones()).hasSize(1);
        assertThat(customer.getCustomerPhones().iterator().next().getPhoneNumber()).isEqualTo("999");
    }

    @Test
    void addsPrimaryPhoneWhenUpdatingCustomerWithoutPhones() {
        Customer customer = Customer.builder().id(8L).name("Sam").location("Here").shippingCompany("Ship").build();
        when(customerRepo.findById(8L)).thenReturn(Optional.of(customer));

        CustomerViewDto updated = customerService.updateCustomerInformation(8L,
                new CustomerUpdateDto("Sam", null, null, "123"));

        assertThat(updated.phoneNumber()).isEqualTo("123");
        assertThat(customer.getCustomerPhones()).singleElement().satisfies(phone -> {
            assertThat(phone.isPrimary()).isTrue();
            assertThat(phone.getPhoneNumber()).isEqualTo("123");
            assertThat(phone.getCustomer()).isSameAs(customer);
        });
    }

    @Test
    void pagesAndMapsCustomersAndSearchesByKeyword() {
        Customer customer = Customer.builder().id(3L).name("Mina Saleh").location("West")
                .shippingCompany("Courier").build();
        customer.addCustomerPhone(CustomerPhone.builder().phoneNumber("777").isPrimary(true).type(Type.MOBILE_PHONE).build());
        PageRequest pageable = PageRequest.of(0, 5);
        when(customerRepo.findByPartialNameMatch("mina", pageable))
                .thenReturn(new PageImpl<>(List.of(customer), pageable, 1));

        var results = customerService.findCustomerByKeyword("mina", pageable);

        assertThat(results.getTotalElements()).isEqualTo(1);
        assertThat(results.getContent()).containsExactly(new CustomerViewDto(3L, "Mina Saleh", "West", "Courier", "777"));
        verify(customerRepo).findByPartialNameMatch("mina", pageable);
    }

    @Test
    void throwsWhenCustomerDoesNotExistAndSoftDeletesExistingCustomer() {
        when(customerRepo.findById(404L)).thenReturn(Optional.empty());
        assertThrows(CustomerNotFoundException.class, () -> customerService.findByCustomerId(404L));

        Customer customer = Customer.builder().id(9L).name("Lee").location("East")
                .shippingCompany("Ship").build();
        when(customerRepo.findById(9L)).thenReturn(Optional.of(customer));

        customerService.deleteCustomerById(9L);

        assertThat(customer.isActive()).isFalse();
        verify(customerRepo, never()).delete(any());
    }

    @Test
    void throwsCustomerNotFoundWhenDeletingMissingCustomer() {
        when(customerRepo.findById(404L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.deleteCustomerById(404L));
    }
}
