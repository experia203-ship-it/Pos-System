package com.connectors.pos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetails;


    @Bean
public SecurityFilterChain filterChain(HttpSecurity http){


    return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(cust->cust.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth->auth.requestMatchers("/","/auth/**" ,"/pos/print/**","/error").permitAll()
                    .anyRequest().authenticated())
            //.formLogin(form->form.loginPage("/auth/login").permitAll())
             .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();




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

