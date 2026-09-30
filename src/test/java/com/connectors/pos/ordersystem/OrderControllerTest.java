package com.connectors.pos.ordersystem;

import com.connectors.pos.customersystem.CustomerRepository;
import com.connectors.pos.customersystem.CustomerService;
import com.connectors.pos.ordersystem.orderdtos.OrderCreateDto;
import com.connectors.pos.ordersystem.orderdtos.OrderItemCreateDto;
import com.connectors.pos.ordersystem.orderdtos.OrderItemResponseDto;
import com.connectors.pos.ordersystem.orderdtos.OrderResponseDto;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.security.JwtFilter;
import com.connectors.pos.security.JwtService;
import com.connectors.pos.settings.PosStyle;
import com.connectors.pos.settings.PrintSize;
import com.connectors.pos.settings.Settings;
import com.connectors.pos.settings.SettingsRepository;
import com.connectors.pos.settings.SettingsGlobalInjector;
import com.connectors.pos.settings.SettingsService;
import com.connectors.pos.settings.Theme;
import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import com.connectors.pos.shift.ShiftService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @MockitoBean
    private SettingsRepository settingsRepository;
    @MockitoBean
    private SettingsService settingsService;
    @MockitoBean
    private SettingsGlobalInjector settingsGlobalInjector;
    @MockitoBean
    private ShiftService shiftService;
    @MockitoBean
    private OrderRepository orderRepository;
    @MockitoBean
    private CacheManager cacheManager;


    @BeforeEach
    void setUpFilter() throws Exception {
        SettingsResponseDto settingsResponse = new SettingsResponseDto(
                "Demo", null, null, null, Theme.SYSTEM_DEFAULT, PrintSize.A4,
                "EGP", PosStyle.HORIZONTAL, null, true);
        when(settingsService.getSettings()).thenReturn(settingsResponse);
        when(settingsGlobalInjector.getSettings()).thenReturn(settingsResponse);
        when(settingsRepository.findById(1)).thenReturn(Optional.of(new Settings()));

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
                .andExpect(view().name("fragments/auth-messages :: exceptions-response"))
                .andExpect(header().string("HX-Retarget","#list-error"));
    }

    @Test
    @WithMockUser(username = "cashier_01", roles = "USER")
    void cartPreviewAddsConfiguredTaxAfterDiscountAndPayment() throws Exception {
        SettingsResponseDto taxedSettings = new SettingsResponseDto(
                "Demo", null, null, null, Theme.SYSTEM_DEFAULT, PrintSize.A4,
                "EGP", PosStyle.HORIZONTAL, null, false, new BigDecimal("10.00"));
        when(settingsGlobalInjector.getSettings()).thenReturn(taxedSettings);
        Products product = Products.builder().id(99L).name("Rotor")
                .sellingPrice(new BigDecimal("10.00")).purchasePrice(new BigDecimal("6.00"))
                .stock(3L).build();
        when(productRepo.findAllById(any())).thenReturn(List.of(product));

        mockMvc.perform(post("/pos/cart/update")
                        .param("discount", "1.00")
                        .param("paid", "12.00")
                        .param("itemsList[0].productId", "99")
                        .param("itemsList[0].quantity", "1")
                        .param("itemsList[0].subDiscount", "0.00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/cart ::cart"))
                .andExpect(model().attribute("taxableSubtotal", new BigDecimal("9.00")))
                .andExpect(model().attribute("taxAmount", new BigDecimal("0.90")))
                .andExpect(model().attribute("grandTotal", new BigDecimal("9.90")))
                .andExpect(model().attribute("amountApplied", new BigDecimal("9.90")))
                .andExpect(model().attribute("cashChange", new BigDecimal("2.10")))
                .andExpect(model().attribute("remaining", new BigDecimal("0.00")));
    }

    @Test
    @WithMockUser(username = "cashier_01", roles = "USER")
    void customerSummaryDateFilterWithoutSelectedCustomerShowsPrompt() throws Exception {
        mockMvc.perform(get("/pos/CustomerOrderSummary"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/order-sum-res-fragment :: select-customer"));

    }

    @Test
    @WithMockUser(username = "cashier_01", roles = "USER")
    void invalidCustomerSummaryDateRangeReturnsLocalizedErrorInsteadOfFailing() throws Exception {
        mockMvc.perform(get("/pos/CustomerOrderSummary/7")
                        .param("dateRange", "2026-04-10 to not-a-date"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/order-sum-res-fragment :: summary-error"));
        verifyNoInteractions(orderServo);
    }

}