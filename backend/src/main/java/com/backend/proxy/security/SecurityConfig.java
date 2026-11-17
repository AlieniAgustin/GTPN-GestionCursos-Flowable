package com.backend.proxy.security;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_URLS = {
        "/api/auth/login",
        "/health",
        // Sin esto, cualquier excepción se reenvía a /error, que al estar protegido
        // devuelve 403 y tapa el ApiErrorResponse del GlobalExceptionHandler.
        "/error"
    };

    /**
     * Sección pública del sitio (enunciado 4.1.1): portada, alta de solicitudes,
     * login y estáticos.
     */
    private static final String[] PUBLIC_PAGES = {
        "/",
        "/solicitudes",
        "/solicitudes/enviada",
        "/login",
        "/health",
        "/error",
        "/css/**",
        "/js/**"
    };

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilterRegistration(ApiKeyFilter filter) {
        FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain flowableSecurity(
            HttpSecurity http,
            ApiKeyFilter apiKeyFilter
    ) throws Exception {
        return http
            .securityMatcher("/api/flowable/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /**
     * Sesión estándar de Spring Security: {@code POST /api/auth/login} valida contra el
     * IDM de Flowable y guarda el SecurityContext en la HttpSession, así que el resto de
     * las llamadas solo necesitan la cookie JSESSIONID.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain applicationSecurity(
            HttpSecurity http
    ) throws Exception {
        return http
            .securityMatcher("/api/**")
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .securityContext(context -> context
                .securityContextRepository(new HttpSessionSecurityContextRepository())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_URLS).permitAll()
                .anyRequest().authenticated()
            )
            // Para una API, "no estás autenticado" es 401, no 403
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .build();
    }

    /**
     * Sitio web institucional (enunciado 4.1). A diferencia de la API, acá un usuario
     * sin sesión no recibe un 401 sino que va a parar al formulario de login.
     *
     * <p>La sección pública —portada y alta de solicitudes— queda abierta: según el
     * enunciado (4.1.1) cualquier empresa puede pedir una capacitación sin tener cuenta.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain websiteSecurity(
            HttpSecurity http,
            SiteAuthenticationProvider siteAuthenticationProvider
    ) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authenticationProvider(siteAuthenticationProvider)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .securityContext(context -> context
                .securityContextRepository(new HttpSessionSecurityContextRepository())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PAGES).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/panel", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?sesion=cerrada")
                .permitAll()
            )
            .build();
    }

    /**
     * Hashea las contrasenas de las cuentas de empresa que crea
     * "Habilitar acceso de la empresa" (enunciado 4.1.2). El personal de la institucion
     * no pasa por aca: se valida contra el IDM de Flowable (enunciado 3.3).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://localhost:5173", "*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
