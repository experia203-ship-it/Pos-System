package com.connectors.pos.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Locale;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class GlobalExceptionHandlerMvcTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailureController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setViewResolvers(new TestViewResolver())
            .build();

    @Test
    void businessRuleResponseUsesBadRequestFragmentAndPreservesMessage() throws Exception {
        mvc.perform(get("/failure/business"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("fragments/auth-messages :: exceptions-response"))
                .andExpect(model().attribute("errorMessage", "Business constraint failed"));
    }

    @Test
    void customerRequiredResponseUsesHtmxTargetAndSuccessfulStatus() throws Exception {
        mvc.perform(get("/failure/customer"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/auth-messages :: exceptions-response"))
                .andExpect(model().attribute("errorMessage", "Customer required"))
                .andExpect(header().string("HX-Retarget", "#customer-error"))
                .andExpect(header().string("HX-Reswap", "innerHTML"));
    }

    @Test
    void notFoundErrorsAreSwappedForHtmxAndUseNotFoundForRegularRequests() throws Exception {
        mvc.perform(get("/failure/not-found").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/auth-messages :: exceptions-response"))
                .andExpect(model().attribute("errorMessage", "Customer was not found"));

        mvc.perform(get("/failure/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("fragments/auth-messages :: exceptions-response"));
    }

    @Controller
    static class FailureController {
        @GetMapping("/failure/business")
        ModelAndView businessFailure() {
            throw new BusinessRuleException("Business constraint failed");
        }

        @GetMapping("/failure/customer")
        ModelAndView customerFailure() {
            throw new CustomerMustBeProvidedException("Customer required");
        }

        @GetMapping("/failure/not-found")
        ModelAndView missingCustomer() {
            throw new CustomerNotFoundException("Customer was not found");
        }
    }

    static class TestViewResolver implements ViewResolver {
        @Override
        public View resolveViewName(String viewName, Locale locale) {
            return new View() {
                @Override
                public String getContentType() {
                    return "text/html";
                }

                @Override
                public void render(Map<String, ?> model, jakarta.servlet.http.HttpServletRequest request,
                                   jakarta.servlet.http.HttpServletResponse response) {
                    response.setContentType(getContentType());
                }
            };
        }
    }
}
