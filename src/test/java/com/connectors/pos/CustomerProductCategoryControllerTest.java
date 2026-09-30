package com.connectors.pos;

import com.connectors.pos.customersystem.CustomerService;
import com.connectors.pos.customersystem.CustomerController;
import com.connectors.pos.customersystem.customerdtos.CustomerViewDto;
import com.connectors.pos.products.CategoryController;
import com.connectors.pos.products.CategoryRepository;
import com.connectors.pos.products.CategoryService;
import com.connectors.pos.products.ProductController;
import com.connectors.pos.products.ProductService;
import com.connectors.pos.products.categorydtos.CategoryCreateDto;
import com.connectors.pos.products.categorydtos.CategoryResponseDto;
import com.connectors.pos.products.productdtos.ProductResponseDto;
import com.connectors.pos.security.JwtFilter;
import com.connectors.pos.security.JwtService;
import com.connectors.pos.settings.Settings;
import com.connectors.pos.settings.SettingsRepository;
import com.connectors.pos.settings.SettingsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {CustomerController.class, ProductController.class, CategoryController.class})
class CustomerProductCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;
    @MockitoBean
    private ProductService productService;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private CategoryRepository categoryRepository;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private SettingsRepository settingsRepository;
    @MockitoBean
    private SettingsService settingsService;
    @MockitoBean
    private CacheManager cacheManager;

    @BeforeEach
    void allowTestRequestsThroughFilters() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
        when(settingsRepository.findById(1)).thenReturn(java.util.Optional.of(new Settings()));
    }

    @Test
    @WithMockUser
    void customerSearchRouteReturnsTheCustomerFragment() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<CustomerViewDto> customers = new PageImpl<>(List.of(
                new CustomerViewDto(1L, "Ava Chen", "North", "Parcel Co", "111")), pageable, 1);
        when(customerService.findCustomerByName(eq("Ava"), any())).thenReturn(customers);

        mockMvc.perform(get("/customers/search").param("name", "Ava"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers :: customers"))
                .andExpect(model().attribute("custs", customers));

        verify(customerService).findCustomerByName(eq("Ava"), any());
    }

    @Test
    @WithMockUser
    void customerCreateSignalsModalCloseOnlyAfterSuccessfulValidation() throws Exception {
        when(customerService.viewAllCustomers()).thenReturn(List.of());

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Ava Chen")
                        .param("location", "North")
                        .param("shippingCompany", "Parcel Co")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Trigger", "close-modal"));

        verify(customerService, times(1)).createCustomer(any());
        verify(customerService).viewAllCustomers();

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Retarget", "#customer-form-error"))
                .andExpect(header().doesNotExist("HX-Trigger"));

    }

    @Test
    @WithMockUser
    void productSearchRouteReturnsPartialTableForHtmxRequests() throws Exception {
        Page<ProductResponseDto> products = Page.empty(PageRequest.of(0, 10));
        when(productService.filterByKeyword(eq("filter"), any())).thenReturn(products);

        mockMvc.perform(get("/products/search")
                        .param("keyword", "filter")
                        .header("Hx-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("products :: table-wrapper"))
                .andExpect(model().attribute("allProducts", products));

        verify(productService).filterByKeyword(eq("filter"), any());
    }

    @Test
    @WithMockUser
    void categoryCreationRouteValidatesAndReturnsUpdatedCategoryList() throws Exception {
        CategoryResponseDto created = new CategoryResponseDto(5L, "Filters", "Vehicle filters");
        when(categoryService.createCategory(new CategoryCreateDto("Filters", "Vehicle filters"))).thenReturn(created);
        when(categoryService.viewAllCategories()).thenReturn(List.of(created));

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Filters")
                        .param("description", "Vehicle filters")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/auth-messages :: category-success"))
                .andExpect(model().attribute("categories", List.of(created)));

        verify(categoryService).createCategory(new CategoryCreateDto("Filters", "Vehicle filters"));
        verify(categoryService).viewAllCategories();
    }
}
