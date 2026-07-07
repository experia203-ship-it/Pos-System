package com.connectors.pos.ordersystem;

import com.connectors.pos.customersystem.CustomerRepository;
import com.connectors.pos.customersystem.CustomerService;
import com.connectors.pos.ordersystem.orderdtos.OrderCreateDto;
import com.connectors.pos.ordersystem.orderdtos.OrderItemCreateDto;
import com.connectors.pos.ordersystem.orderdtos.OrderItemResponseDto;
import com.connectors.pos.ordersystem.orderdtos.OrderResponseDto;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.security.JwtFilter;
import com.connectors.pos.security.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
   private MockMvc mockMvc;
@MockitoBean
    private JwtFilter filter;
    @MockitoBean
    private JwtService servo;
    @MockitoBean
    private OrderService orderServo;
    @MockitoBean
    private CustomerRepository customerRepo;
    @MockitoBean
    private  ProductRepository productRepo;
    @MockitoBean
    private  CustomerService customerServo;


    @BeforeEach
    void setUpFilter() throws Exception {
        // This tells the mocked JwtFilter to continue the filter chain,
        // allowing the request to actually reach your OrderController.
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(filter).doFilter(any(), any(), any());
    }



    @Test
    @WithMockUser(username = "cashier_01",roles = "USER")
    void testCheckOutWithSuccess() throws Exception {




OrderResponseDto response = new OrderResponseDto(1L,"cashier_01"
, BigDecimal.TEN,BigDecimal.TWO,BigDecimal.TWO,BigDecimal.valueOf(9.00),
        LocalDateTime.now(),1L,1L,
        "customer",new ArrayList<>(),"1456");

when(orderServo.createOrder(any())).thenReturn(response);

mockMvc.perform(post("/pos/check-out")
        .param("customerId","1")
        .param("paid","10.00")
        .param("discount","1.00")
                .param("itemsList[0].productId", "99")
                .param("itemsList[0].quantity", "2")
                .param("itemsList[0].price", "100.00")
                .param("itemsList[0].subDiscount","22.00")
                .with(csrf()))

        .andExpect(status().isOk())
        .andExpect(view().name("fragments/cart ::cart"))
        .andExpect(header().string("HX-Trigger","{\"orderCompleted\": {\"orderId\": 1}}"));
    }

    @Test
    @WithMockUser(username="cashier_01")
    void checkOrderOut_Failure_Retarget() throws Exception {

        mockMvc.perform(post("/pos/check-out")
                .param("paid","-50.00")
                                .with(csrf())
                )
                .andExpect(view().name("fragments/cart :: cart"))
                .andExpect(header().string("HX-Retarget","#cart-zone"));
    }

}