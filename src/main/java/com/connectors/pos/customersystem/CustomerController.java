package com.connectors.pos.customersystem;

import com.connectors.pos.customersystem.customerdtos.CustomerCreateDto;
import com.connectors.pos.customersystem.customerdtos.CustomerUpdateDto;
import com.connectors.pos.customersystem.customerdtos.CustomerViewDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerServo;



    @GetMapping("/create2")
    public String viewCreateCustomerPage2(Model model){

        CustomerCreateDto customer = new CustomerCreateDto(null,


                null,null,null);

        model.addAttribute("customerCreate",customer);

        List<Customer> customers = customerServo.viewAllCustomers();

        model.addAttribute("customers",customers);

        return "customer-form2 :: customer-pop-up";
    }
    @GetMapping("/create")

    public String viewCreateCustomerPage(Model model){

        CustomerCreateDto customer = new CustomerCreateDto(null,


                null,null,null);

        model.addAttribute("customerCreate",customer);

        List<Customer> customers = customerServo.viewAllCustomers();

        model.addAttribute("customers",customers);

        return "fragments/customer-form :: customer-pop-up";
    }
    @PostMapping("/new")
public String createCustomerCustPage( @Valid@ModelAttribute("customerCreate") CustomerCreateDto customerCreate,BindingResult bindResult, Model model, HttpServletResponse response,
                                      @PageableDefault(page = 0,size = 10,sort = "id",direction = Sort.Direction.ASC) Pageable pageable){
      if(bindResult.hasErrors()){

      }
        customerServo.createCustomer(customerCreate);
        Page<CustomerViewDto> customers = customerServo.getAllCustomers(pageable);

        model.addAttribute("custs",customers);
response.setHeader("HX-Trigger","close-modal");
        return "customers :: customers";

}
    @PostMapping

    public String createCustomer(@ModelAttribute("customerCreate") CustomerCreateDto customerCreate , Model model,
                                 HttpServletResponse response){


        customerServo.createCustomer(customerCreate);
        List<Customer> customers =customerServo.viewAllCustomers();
        model.addAttribute("customers",customers);


        return "fragments/cart :: customer-fragment";


    }

    @GetMapping

    public String viewCustomerPage(Model model ,@PageableDefault(page = 0,size = 10,sort = "id" , direction= Sort.Direction.ASC) Pageable pageable)
    {

        Page<CustomerViewDto> customers = customerServo.getAllCustomers(pageable);

        model.addAttribute("custs",customers);

        return "customers";
    }

@GetMapping("/search")

    public String findByCustomerName(@RequestParam(name = "name") String name, Model model , @PageableDefault(page = 0,size = 10,sort = "id" , direction= Sort.Direction.ASC) Pageable pageable){

        Page<CustomerViewDto> result = customerServo.findCustomerByName(name,pageable);

        model.addAttribute("custs",result);

        return "customers :: customers";
}

@GetMapping("/search2")
public String findByCustomerNameForStats(@RequestParam(name = "name") String name,Model model,
                                         @PageableDefault(page = 0,size = 10,sort = "id" , direction= Sort.Direction.ASC) Pageable pageable
                                         ){
        Page<CustomerViewDto> res = customerServo.findCustomerByName(name,pageable);

        model.addAttribute("custs",res);

        return "customers2 :: customers2";

}
@GetMapping("/part-search")

    public String findCustomerByAKeyWord(@RequestParam(name = "keyword") String keyword, Model model , @PageableDefault(page = 0,size = 10,sort = "id" , direction= Sort.Direction.ASC) Pageable pageable){

        Page<CustomerViewDto> result = customerServo.findCustomerByKeyword(keyword,pageable);

        model.addAttribute("custs",result);

        return "customers :: customers";

}
@PatchMapping("/{id}")
    public String updateCustomerById(@PathVariable Long id , Model model , @Valid @ModelAttribute CustomerUpdateDto update, BindingResult bindingResult,HttpServletResponse response,
                                     @PageableDefault(page = 0,size = 10,sort = "id" , direction= Sort.Direction.ASC) Pageable pageable                    ){

        if(bindingResult.hasErrors()){

            response.setHeader("HX-Retarget","#modal-container-cust");
            response.setHeader("HX-Reswap","innerHTML");

    return "customer-update :: customer-update-fragment";
        }

customerServo.updateCustomerInformation(id,update);

        Page<CustomerViewDto> allCustomers = customerServo.getAllCustomers(pageable);
        model.addAttribute("custs",allCustomers);
        response.setHeader("Hx-Trigger","pop-customer-container");

      return "customers";
}


@GetMapping("/update/{id}")
public String viewCustomerUpdatePopUp(@PathVariable Long id,Model model){


CustomerViewDto customer = customerServo.findByCustomerId(id);

CustomerUpdateDto update = new CustomerUpdateDto(
customer.name(),customer.location(),customer.shippingCompany(),customer.phoneNumber());


model.addAttribute("customer",update);

model.addAttribute("customerId",customer.id());



return "Customer-update :: customer-update-fragment";
}

@DeleteMapping("/{id}")

    public String deleteCustomerFromList(@PathVariable Long id ,Model model,@PageableDefault Pageable pageable){


        customerServo.deleteCustomerById(id);

     Page<CustomerViewDto> result =  customerServo.getAllCustomers(pageable);

    model.addAttribute("custs",result);

    return "customers :: customers";
}



}
