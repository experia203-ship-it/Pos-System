package com.connectors.pos.purchasesystem;

import com.connectors.pos.purchasesystem.purchasedtos.VendorCreateDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class VendorService {

    private final VendorRepository vendorRepo;

    @Transactional
    public Vendor createVendor(VendorCreateDto create) {
        Vendor vendor = Vendor.builder()
                .name(create.name())
                .location(create.location())
                .mobile(create.phoneNumber())
                .landline(create.landline())
                .build();
        return vendorRepo.save(vendor);
    }

    @Transactional(readOnly = true)
    public Page<Vendor> viewAllVendors(Pageable pageable) {
        return vendorRepo.findAll(pageable);
    }

    @Transactional
    public void deleteVendorById(Long id){

        if(id==null){

            throw new RuntimeException("vendor id must be provided");
        }
        Vendor vendor = vendorRepo.findById(id)
                .orElseThrow(()->new EntityNotFoundException("vendor doesn't exist with that id"));


        vendor.setActive(false);
    }

    @Transactional(readOnly = true)

    public Page<Vendor> filterByName(Pageable pageable , String keyword){


        return vendorRepo.filterByKeyword(pageable,keyword);
    }

    @Transactional

    public Vendor updateById(Long id ,VendorCreateDto update){
        if(id==null){
            throw new RuntimeException("vendor id must be provided");
        }

        Vendor vendor = vendorRepo.findById(id)
                .orElseThrow(()-> new EntityNotFoundException("no vendor was found with that id"));

        if(update.name()!=null&&!update.name().equals(vendor.getName())){vendor.setName(update.name());}
        if(update.location()!=null&& !update.location().equals(vendor.getLocation())){vendor.setLocation(update.location());}
        if(update.phoneNumber()!=null&&update.phoneNumber().equals(vendor.getMobile())){vendor.setMobile(update.phoneNumber());}
       if(update.landline()!=null&&!update.landline().equals(vendor.getLandline())){vendor.setLandline(update.landline());}

       return vendor;
    }


}