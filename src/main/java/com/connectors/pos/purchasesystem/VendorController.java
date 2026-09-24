package com.connectors.pos.purchasesystem;

import com.connectors.pos.purchasesystem.purchasedtos.VendorCreateDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorServo; // Assuming you have a service or repository for vendors
    private final VendorRepository vendorRepo;

    @GetMapping
    public String viewSuppliersPage(@PageableDefault Pageable pageable, Model model) {

        Page<Vendor> allV = vendorServo.viewAllVendors(pageable);
        model.addAttribute("vendors", allV);

        return "vendors";
    }

    @GetMapping("/create")
    public String viewCreateSupplierPage(@PageableDefault Pageable pageable, Model model) {
        VendorCreateDto supplier = new VendorCreateDto(null,null, null, null, null); // Adjust according to your DTO
        model.addAttribute("supplierCreate", supplier);

        Page<Vendor> suppliers = vendorServo.viewAllVendors(pageable);
        model.addAttribute("suppliers", suppliers);

        return "fragments/supplier-form :: supplier-pop-up";
    }

    @PostMapping
    public String createSupplier(@PageableDefault Pageable pageable,
                                 @ModelAttribute("supplierCreate") VendorCreateDto supplierCreate,
                                 Model model,
                                 HttpServletResponse response) {

        vendorServo.createVendor(supplierCreate);
        Page<Vendor> suppliers = vendorServo.viewAllVendors(pageable);
        model.addAttribute("suppliers", suppliers);

        return "purchase-cart :: supplier-fragment";
    }

    @GetMapping("/search")

    public String searchVendors(@PageableDefault Pageable pageable, Model model, @RequestParam(name = "name") String name) {

        Page<Vendor> allV = vendorServo.filterByName(pageable, name);

        model.addAttribute("vendors", allV);

        return "vendors :: vendor-fragment ";
    }

    @GetMapping("/create2")
    public String viewCreateSupplierPage2(@PageableDefault Pageable pageable, Model model) {
        VendorCreateDto supplier = new VendorCreateDto(null,null, null, null, null); // Adjust according to your DTO
        model.addAttribute("supplierCreate", supplier);

        Page<Vendor> suppliers = vendorServo.viewAllVendors(pageable);
        model.addAttribute("vendors", suppliers);

        return "vendor-create-vendorpage :: supplier-pop-up2";
    }


    @PostMapping("/create-vendor")
    public String createSupplier2(@PageableDefault Pageable pageable,
                                  @Valid @ModelAttribute("supplierCreate") VendorCreateDto supplierCreate, BindingResult result,
                                  Model model,
                                  HttpServletResponse response) {
        if (result.hasErrors()) {
            response.setHeader("HX-Retarget", "#new-sup-form");
            response.setHeader("HX-Reswap", "outerHTML");

            return "fragments/supplier-form :: supplier-pop-up";
        }

        vendorServo.createVendor(supplierCreate);
        Page<Vendor> allV = vendorServo.viewAllVendors(pageable);
        model.addAttribute("vendors", allV);
        response.setHeader("HX-Trigger", "close-modal");
        return "vendors :: vendor-fragment";
    }


    @GetMapping("/update/{id}")
    public String viewUpdatePopUp(@PathVariable("id") Long id,Model model){
      Vendor vendor = vendorRepo.findById(id).orElseThrow(()-> new EntityNotFoundException("no vendor exists with that id "));
        VendorCreateDto dto = new VendorCreateDto(vendor.getId(),vendor.getName(),vendor.getLocation(),vendor.getMobile(),vendor.getLandline());

      model.addAttribute("supplierUpdate",dto);

      return "vendor-update-popup :: supplier-pop-up3 ";

    }

    @PostMapping("/update")
    public String updateVendorById(@PageableDefault Pageable pageable ,@Valid@ModelAttribute("supplierUpdate") VendorCreateDto dto, Model model, @RequestParam("id") Long id,HttpServletResponse response) {
vendorServo.updateById(id,dto);
 response.setHeader("HX-Trigger","close-modal");
   Page<Vendor> suppliers =  vendorServo.viewAllVendors(pageable);
        model.addAttribute("vendors", suppliers);
        return "vendors :: vendor-fragment";
    }

    @DeleteMapping("/{id}")

    public String deleteVendorById(@PathVariable("id") Long id , Model model,@PageableDefault Pageable pageable){

        vendorServo.deleteVendorById(id);

        Page<Vendor> vendors = vendorServo.viewAllVendors( pageable);
        model.addAttribute("vendors",vendors);


        return "vendors :: vendor-fragment";

    }

}