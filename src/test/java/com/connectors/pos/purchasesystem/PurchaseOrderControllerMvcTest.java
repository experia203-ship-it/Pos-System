package com.connectors.pos.purchasesystem;

import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.purchasesystem.purchasedtos.CreatePurchaseOrderDto;
import com.connectors.pos.purchasesystem.purchasedtos.CreatePurchaseOrderItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class PurchaseOrderControllerMvcTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final VendorRepository vendors = mock(VendorRepository.class);
    private final PurchaseOrderService service = mock(PurchaseOrderService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new PurchaseOrderController(products, vendors, service)).build();

    @Test
    void clearCartBuildsEmptyCartModel() throws Exception {
        when(vendors.findAll()).thenReturn(List.of());
        when(products.findAllByIds(List.of())).thenReturn(List.of());

        mvc.perform(delete("/purchase/clear"))
                .andExpect(status().isOk())
                .andExpect(view().name("purchase-cart :: cart-zone"))
                .andExpect(model().attribute("grandTotal", BigDecimal.ZERO))
                .andExpect(model().attribute("cartItems", List.of()))
                .andExpect(model().attribute("remaining", BigDecimal.ZERO));
    }

    @Test
    void addCustomItemCalculatesTotalsAndCarriesSupplierSelection() throws Exception {
        when(vendors.findAll()).thenReturn(List.of());
        when(products.findAllByIds(List.of())).thenReturn(List.of());

        mvc.perform(post("/purchase/custom").param("discount", "2.50").param("paid", "1.00")
                        .param("supplierId", "15").param("orderNumber", "PO-15")
                        .param("itemsList[0].quantity", "1").param("itemsList[0].subDiscount", "0")
                        .param("itemsList[0].customName", "Freight")
                        .param("itemsList[0].customPurchasePrice", "10.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("purchase-cart :: cart-zone"))
                .andExpect(model().attribute("grandTotal", new BigDecimal("7.50")))
                .andExpect(model().attribute("remaining", new BigDecimal("6.50")))
                .andExpect(model().attribute("currentSupplierId", 15L))
                .andExpect(model().attribute("orderNumber", "PO-15"));
    }

    @Test
    void checkoutValidationFailureRetargetsErrorFragmentWithoutCreatingOrder() throws Exception {
        when(vendors.findAll()).thenReturn(List.of());

        mvc.perform(post("/purchase/check-out").param("discount", "").param("paid", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/auth-messages :: exceptions-response"))
                .andExpect(model().attribute("errorMessage", "Invalid purchase order data provided."))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("HX-Retarget", "#list-error"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("HX-Reswap", "innerHTML"));

        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    void validCheckoutDelegatesAndReturnsClearedCart() throws Exception {
        when(vendors.findAll()).thenReturn(List.of());
        when(products.findAllByIds(List.of())).thenReturn(List.of());

        mvc.perform(post("/purchase/check-out").param("discount", "0").param("paid", "0")
                        .param("itemsList[0].quantity", "1")
                        .param("itemsList[0].subDiscount", "0")
                        .param("itemsList[0].customName", "Freight")
                        .param("itemsList[0].customPurchasePrice", "10.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("purchase-cart :: cart-zone"))
                .andExpect(model().attribute("grandTotal", BigDecimal.ZERO));

        verify(service).createPurchaseOrder(any(CreatePurchaseOrderDto.class));
    }
}
