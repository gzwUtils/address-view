package kd.address.view.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class ApiSecurityConfig {
    @Value("${portal.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository tokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokens.setHeaderName("X-CSRF-TOKEN");
        tokens.setCookiePath("/");
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.csrfTokenRepository(tokens)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterBefore(originFilter(), org.springframework.security.web.csrf.CsrfFilter.class);
        return http.build();
    }

    private OncePerRequestFilter originFilter() {
        Set<String> allowed = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(value -> !value.isBlank()).collect(Collectors.toSet());
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String method = request.getMethod();
                boolean write = !"GET".equals(method) && !"HEAD".equals(method) && !"OPTIONS".equals(method);
                if (write && request.getRequestURI().startsWith("/api/")) {
                    String origin = request.getHeader("Origin");
                    if (origin == null || !allowed.contains(origin)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origin not allowed");
                        return;
                    }
                }
                chain.doFilter(request, response);
            }
        };
    }
}
