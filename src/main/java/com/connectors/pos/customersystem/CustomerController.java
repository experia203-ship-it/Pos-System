package com.connectors.pos.customersystem;

import com.connectors.pos.customersystem.customerdtos.CustomerCreateDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerServo;

    @GetMapping("/create")

    public String viewCreateCustomerPage(Model model){

        CustomerCreateDto customer = new CustomerCreateDto(null,


                null,null);

        model.addAttribute("customerCreate",customer);

        List<Customer> customers = customerServo.viewAllCustomers();

        model.addAttribute("customers",customers);

        return "fragments/customer-form :: customer-pop-up";
    }

    @PostMapping

    public String createCustomer(@ModelAttribute("customerCreate") CustomerCreateDto customerCreate , Model model,
                                 HttpServletResponse response){


        customerServo.createCustomer(customerCreate);
        List<Customer> customers =customerServo.viewAllCustomers();
        model.addAttribute("customers",customers);


        return "fragments/cart :: customer-fragment";


    }
}
