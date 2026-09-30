package com.connectors.pos.security;

import com.connectors.pos.users.AuthController;
import com.connectors.pos.users.UserService;
import com.connectors.pos.settings.SettingsService;
import com.connectors.pos.products.CategoryRepository;
import com.connectors.pos.products.CategoryService;
import com.connectors.pos.products.ProductController;
import com.connectors.pos.products.ProductService;
import com.connectors.pos.users.UserRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({AuthController.class, ProductController.class})
@Import({SecurityConfiguration.class, RegistrationPolicy.class})
class AuthSecurityMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private SettingsService settingsService;
    @MockitoBean
    private CacheManager cacheManager;
    @MockitoBean
    private ProductService productService;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private CategoryRepository categoryRepository;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void cashierIsBlockedFromRegistrationWhenUsersAlreadyExist() throws Exception {
        when(userRepository.count()).thenReturn(1L);

        mvc.perform(get("/auth/register").with(user("cashier").roles("USER")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/auth/register").with(user("cashier").roles("USER")).with(csrf().asHeader())
                        .param("name", "Next").param("email", "next@example.test")
                        .param("password", "Pass1234!"))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any());
    }

    @Test
    void administratorMayRegisterAnotherAccount() throws Exception {
        when(userRepository.count()).thenReturn(1L);

        mvc.perform(get("/auth/register").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        mvc.perform(post("/auth/register").with(user("admin").roles("ADMIN")).with(csrf().asHeader())
                        .param("name", "Next").param("email", "next@example.test")
                        .param("password", "Pass1234!"))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/auth/login"));

        verify(userService).createUser(any());
    }

    @Test
    void cashierCanSearchPosProductsButCannotManageProducts() throws Exception {
        mvc.perform(get("/products/search-pos").with(user("cashier").roles("USER")))
                .andExpect(status().isOk());

        mvc.perform(get("/products/create").with(user("cashier").roles("USER")))
                .andExpect(status().isForbidden());

        when(categoryService.viewAllCategories()).thenReturn(List.of());
        mvc.perform(get("/products/create").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void loginAndRegistrationPostsRequireCsrfTokens() throws Exception {
        mvc.perform(post("/auth/login").param("email", "person@example.test").param("password", "Pass1234!"))
                .andExpect(status().isForbidden());

        when(userRepository.count()).thenReturn(0L);
        MvcResult page = mvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpServletResponse response = page.getResponse();
        Cookie csrfCookie = response.getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        String csrfToken = metaContent(response.getContentAsString(), "_csrf");
        String csrfHeader = metaContent(response.getContentAsString(), "_csrf_header");

        mvc.perform(post("/auth/register").cookie(csrfCookie).header(csrfHeader, csrfToken)
                        .param("name", "First").param("email", "first@example.test")
                        .param("password", "Pass1234!"))
                .andExpect(status().isOk());

        verify(userService).createUser(any());
    }

    private String metaContent(String html, String metaName) {
        Matcher matcher = Pattern.compile("<meta name=\"" + Pattern.quote(metaName)
                + "\" content=\"([^\"]+)\">").matcher(html);
        if (!matcher.find()) {
            throw new AssertionError("Missing " + metaName + " CSRF meta tag");
        }
        return matcher.group(1);
    }
}
