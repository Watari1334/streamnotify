package com.shin.streamnotify.config;

import com.shin.streamnotify.user.CustomOidcUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/webhooks/**")
                )
                .addFilterAfter(csrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/webhooks/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)
                        )
                );

        return http.build();
    }

    private OncePerRequestFilter csrfCookieFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
                if (csrfToken != null) {
                    csrfToken.getToken();
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}