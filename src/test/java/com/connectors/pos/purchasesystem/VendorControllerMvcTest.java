package com.connectors.pos.purchasesystem;

import com.connectors.pos.purchasesystem.purchasedtos.VendorCreateDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class VendorControllerMvcTest {
    private final VendorService service = mock(VendorService.class);
    private final VendorRepository repository = mock(VendorRepository.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new VendorController(service, repository))
            .setCustomArgumentResolvers(new PageableResolver()).build();

    @Test
    void vendorListAndSearchUsePagedServiceResults() throws Exception {
        Page<Vendor> page = new PageImpl<>(List.of(Vendor.builder().id(7L).name("Parts Co").build()));
        when(service.viewAllVendors(any())).thenReturn(page);
        when(service.filterByName(any(), org.mockito.ArgumentMatchers.eq("Parts"))).thenReturn(page);

        mvc.perform(get("/suppliers").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("vendors"))
                .andExpect(model().attribute("vendors", page));
        mvc.perform(get("/suppliers/search").param("name", "Parts"))
                .andExpect(status().isOk())
                .andExpect(view().name("vendors :: vendor-fragment "));

        verify(service).viewAllVendors(PageRequest.of(0, 10));
        verify(service).filterByName(any(), org.mockito.ArgumentMatchers.eq("Parts"));
    }

    @Test
    void createFormAndCreatePostExposeSupplierModelAndFragment() throws Exception {
        Page<Vendor> page = Page.empty();
        when(service.viewAllVendors(any())).thenReturn(page);
        when(service.createVendor(any())).thenReturn(Vendor.builder().id(3L).name("Parts Co").build());

        mvc.perform(get("/suppliers/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/supplier-form :: supplier-pop-up"))
                .andExpect(model().attributeExists("supplierCreate", "suppliers"));
        mvc.perform(post("/suppliers").param("name", "Parts Co").param("location", "Cairo")
                        .param("phoneNumber", "01000000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("purchase-cart :: supplier-fragment"))
                .andExpect(model().attribute("suppliers", page));

        verify(service).createVendor(new VendorCreateDto(null, "Parts Co", "Cairo", "01000000000", null));
    }

    @Test
    void updatePopupLoadsVendorAndDeleteRefreshesVendorFragment() throws Exception {
        Vendor vendor = Vendor.builder().id(9L).name("Parts Co").location("Cairo")
                .mobile("01000000000").landline("0222222222").build();
        when(repository.findById(9L)).thenReturn(Optional.of(vendor));
        when(service.viewAllVendors(any())).thenReturn(Page.empty());

        mvc.perform(get("/suppliers/update/9"))
                .andExpect(status().isOk())
                .andExpect(view().name("vendor-update-popup :: supplier-pop-up3 "))
                .andExpect(model().attributeExists("supplierUpdate"));
        mvc.perform(delete("/suppliers/9"))
                .andExpect(status().isOk())
                .andExpect(view().name("vendors :: vendor-fragment"))
                .andExpect(model().attributeExists("vendors"));

        verify(service).deleteVendorById(9L);
    }

    private static class PageableResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return Pageable.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                      NativeWebRequest request, WebDataBinderFactory binderFactory) {
            int page = Integer.parseInt(request.getParameter("page") == null ? "0" : request.getParameter("page"));
            int size = Integer.parseInt(request.getParameter("size") == null
                    ? String.valueOf(parameter.getParameterAnnotation(PageableDefault.class).size())
                    : request.getParameter("size"));
            return PageRequest.of(page, size);
        }
    }
}
