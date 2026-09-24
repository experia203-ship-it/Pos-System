package com.connectors.pos.customersystem.customerdtos;

import com.connectors.pos.customersystem.Customer;
import com.connectors.pos.customersystem.CustomerPhone;
import org.mapstruct.*;

import java.util.Set;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CustomerMapper {

    @Mapping(target="phoneNumber" ,source="customerPhones" ,qualifiedByName ="mapPhoneNum" )
 CustomerViewDto toResponse(Customer customer);





    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target="customerPhones" ,ignore=true)
@Mapping(target="id" , ignore=true)
    @Mapping(target = "active", ignore = true)

    void  updateCustomerFromDto(CustomerUpdateDto dto ,@MappingTarget Customer customer);



    @Named("mapPhoneNum")
            default String mapPhoneNum(Set<CustomerPhone> phones){

        if(phones==null || phones.isEmpty()){return null;}

        return phones.stream().filter(CustomerPhone ::isPrimary)
                .map(CustomerPhone::getPhoneNumber)
                .findFirst()
                .orElseGet(()->phones.iterator().next().getPhoneNumber());
    }


}
