package com.tamachihua_erp.api.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tamachihua_erp.api.security.CustomUserDetailsService;
import com.tamachihua_erp.api.security.SecurityErrorWriter;
import lombok.RequiredArgsConstructor;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final SecurityErrorWriter securityErrorWriter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // Clave criptográfica para firmar y validar JWT
    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.jwt.secret}") String secret
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "La clave JWT debe tener al menos 256 bits"
            );
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtSecretKey)
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // Convierte el claim roles en autoridades de Spring Security
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }

    // Configuración CORS para el panel web y la aplicación móvil
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:8100", // app (Ionic + angular)
                "http://localhost:4200", // app (ng serve / pruebas e2e)
                "http://localhost:8081", // web (Vue)
                "http://localhost",      // app compilada (Capacitor Android)
                "https://localhost",     // app compilada (Capacitor iOS)
                "capacitor://localhost", // app compilada (Capacitor iOS)
                "ionic://localhost"      // app compilada (Ionic)
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Client-Interface"
        ));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                // Habilitar CORS
                .cors(Customizer.withDefaults())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())

                .authorizeHttpRequests(auth -> auth

                        // Login público
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        ).permitAll()

                        // Información del usuario autenticado
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/me"
                        ).authenticated()

                        // Rutas de la aplicación móvil (HU-17).
                        // Solo los roles que pueden usar la app.
                        .requestMatchers("/api/app/**").hasAnyRole(
                                "ADMIN",
                                "WAREHOUSE",
                                "SELLER"
                        )

                        // Precios de productos: solo ADMIN (HU-08)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products/*/presentations/*/prices"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/products/*/presentations/*/prices"
                        ).hasRole("ADMIN")

                        // Último costo pagado: ADMIN y PURCHASING (contrato 7)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products/*/last-cost"
                        ).hasAnyRole("ADMIN", "PURCHASING")

                        // Usuarios: únicamente ADMIN (HU-02)
                        .requestMatchers(
                                "/api/users",
                                "/api/users/**"
                        ).hasRole("ADMIN")

                        // Proveedores: ADMIN y PURCHASING (HU-09)
                        .requestMatchers(
                                "/api/suppliers",
                                "/api/suppliers/**"
                        ).hasAnyRole("ADMIN", "PURCHASING")

                        // Lotes: consulta para ADMIN, WAREHOUSE y PURCHASING
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/lots",
                                "/api/lots/**"
                        ).hasAnyRole("ADMIN", "WAREHOUSE", "PURCHASING")

                        // Lotes: registrar y corregir cantidad para ADMIN y WAREHOUSE
                        .requestMatchers(
                                "/api/lots",
                                "/api/lots/**"
                        ).hasAnyRole("ADMIN", "WAREHOUSE")

                        // Dashboard: resumen y lotes por vencer (ADMIN, WAREHOUSE)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/dashboard/summary",
                                "/api/dashboard/expiring-lots"
                        ).hasAnyRole("ADMIN", "WAREHOUSE")

                        // Dashboard: bajo stock también para PURCHASING
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/dashboard/low-stock"
                        ).hasAnyRole("ADMIN", "WAREHOUSE", "PURCHASING")

                        // Productos: lectura para ADMIN, WAREHOUSE y PURCHASING (HU-06, HU-07)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products",
                                "/api/products/**"
                        ).hasAnyRole("ADMIN", "WAREHOUSE", "PURCHASING")

                        // Productos y presentaciones: escritura ADMIN y WAREHOUSE (D-06)
                        .requestMatchers(
                                "/api/products",
                                "/api/products/**"
                        ).hasAnyRole("ADMIN", "WAREHOUSE")

                        // Roles: únicamente ADMIN
                        .requestMatchers(
                                "/api/roles",
                                "/api/roles/**"
                        ).hasRole("ADMIN")

                        // Catálogos: lectura para ADMIN, WAREHOUSE y PURCHASING
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/categories",
                                "/api/categories/**",
                                "/api/measure-units",
                                "/api/measure-units/**",
                                "/api/presentations",
                                "/api/presentations/**",
                                "/api/brands",
                                "/api/brands/**",
                                "/api/models",
                                "/api/models/**",
                                "/api/product-types",
                                "/api/product-types/**"
                        ).hasAnyRole("ADMIN", "WAREHOUSE", "PURCHASING")

                        // Catálogos: cualquier otro metodo requiere ADMIN
                        .requestMatchers(
                                "/api/categories",
                                "/api/categories/**",
                                "/api/measure-units",
                                "/api/measure-units/**",
                                "/api/presentations",
                                "/api/presentations/**",
                                "/api/brands",
                                "/api/brands/**",
                                "/api/models",
                                "/api/models/**",
                                "/api/product-types",
                                "/api/product-types/**"
                        ).hasRole("ADMIN")

                        // Cualquier otro endpoint requiere autenticación
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        // Un token alterado o expirado lo rechaza este manejador,
                        // Corrección, ya se manda los datos
                        .authenticationEntryPoint((request, response, exception) ->
                                securityErrorWriter.write(
                                        request,
                                        response,
                                        401,
                                        "UNAUTHORIZED",
                                        "Autenticación requerida o token inválido"
                                )
                        )
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                .exceptionHandling(ex -> ex

                        .authenticationEntryPoint((request, response, exception) ->
                                securityErrorWriter.write(
                                        request,
                                        response,
                                        401,
                                        "UNAUTHORIZED",
                                        "Autenticación requerida o token inválido"
                                )
                        )

                        .accessDeniedHandler((request, response, exception) ->
                                securityErrorWriter.write(
                                        request,
                                        response,
                                        403,
                                        "FORBIDDEN",
                                        "No tienes permisos para realizar esta operación"
                                )
                        )
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}