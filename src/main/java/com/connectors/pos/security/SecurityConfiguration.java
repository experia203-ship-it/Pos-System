package com.connectors.pos.security;

import com.connectors.pos.license.LicenseFilter;
import com.connectors.pos.license.LicenseService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.beans.factory.annotation.Value;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetails;
    private final ObjectProvider<LicenseService> licenseServiceProvider;

    @Value("${app.security.cookie-secure:false}")
    private boolean secureCookies;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokens.setCookiePath("/");
        csrfTokens.setCookieCustomizer(cookie -> cookie.sameSite("Lax").secure(secureCookies));

        HttpSecurity configured = http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                )
                .sessionManagement(cust -> cust.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/auth/login", "/auth/register", "/error",
                                "/css/**", "/js/**", "/images/**", "/license/**").permitAll()
                        .requestMatchers("/auth/logout").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new MustChangePasswordFilter(), JwtFilter.class);

        // Only present in the installed app (app.license.enabled=true): block everything until activated.
        licenseServiceProvider.ifAvailable(service ->
                configured.addFilterBefore(new LicenseFilter(service), JwtFilter.class));

        return configured.build();
    }

@Bean
    public PasswordEncoder getPasswordEncoder(){

    return new BCryptPasswordEncoder();

}

@Bean
    public AuthenticationManager getAuthManager(AuthenticationConfiguration config) throws Exception{

    return config.getAuthenticationManager();
}

@Bean

    public AuthenticationProvider getAuthProvider(){

    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetails);

    provider.setPasswordEncoder(getPasswordEncoder());


    return provider;
}
    }
