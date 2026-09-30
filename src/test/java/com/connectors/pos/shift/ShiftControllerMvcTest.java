package com.connectors.pos.shift;

import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.users.Users;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ShiftControllerMvcTest {
    private final ShiftService service = mock(ShiftService.class);
    private final UserPrincipal principal = new UserPrincipal(Users.builder().id(21L).email("cashier@example.com").build());
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ShiftController(service))
            .setCustomArgumentResolvers(new PrincipalResolver()).build();

    @Test
    void resetWidgetClearsActiveShift() throws Exception {
        mvc.perform(get("/shift/reset-widget"))
                .andExpect(status().isOk())
                .andExpect(view().name("pos :: shift-widget"))
                .andExpect(model().attribute("activeShift", (Object) null));
    }

    @Test
    void openShiftUsesAuthenticatedUserAndReturnsSessionInWidget() throws Exception {
        ShiftSession opened = ShiftSession.builder().id(5L).startingFloat(new BigDecimal("30.00")).build();
        when(service.openShift(21L, new BigDecimal("30.00"))).thenReturn(opened);

        mvc.perform(post("/shift/open").param("startingFloat", "30.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("pos :: shift-widget"))
                .andExpect(model().attribute("activeShift", opened))
                .andExpect(model().attribute("sessionId", 5L));
    }

    @Test
    void closeShiftLoadsCurrentShiftAndReturnsClosedSummary() throws Exception {
        ShiftSession active = ShiftSession.builder().id(6L).build();
        ShiftSession closed = ShiftSession.builder().id(6L).countedCash(new BigDecimal("42.00")).build();
        when(service.getActiveShift(21L)).thenReturn(active);
        when(service.closeShift(6L, new BigDecimal("42.00"))).thenReturn(closed);

        mvc.perform(post("/shift/close").param("actualCount", "42.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("shift :: shift-summary"))
                .andExpect(model().attribute("closedShift", closed));

        verify(service).closeShift(6L, new BigDecimal("42.00"));
    }

    @Test
    void cashEventAddsEventAndRefreshesActiveShiftWidget() throws Exception {
        ShiftSession active = ShiftSession.builder().id(8L).build();
        when(service.getActiveShift(21L)).thenReturn(active);

        mvc.perform(post("/shift/event").param("eventType", "PAY_OUT").param("amount", "8.50")
                        .param("reason", "Change"))
                .andExpect(status().isOk())
                .andExpect(view().name("pos :: shift-widget"))
                .andExpect(model().attribute("activeShift", active));

        verify(service).addCashEvent(8L, EventType.PAY_OUT, new BigDecimal("8.50"), "Change");
    }

    private class PrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(
                    org.springframework.security.core.annotation.AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                      NativeWebRequest request, WebDataBinderFactory binderFactory) {
            return principal;
        }
    }
}
