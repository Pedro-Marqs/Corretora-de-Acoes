package com.projeto.gestao.config;

import java.util.List;

import com.projeto.gestao.api.exception.ApiErrorCode;
import com.projeto.gestao.api.exception.SecurityErrorResponseWriter;
import com.projeto.gestao.config.JwtSecurityProperties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableConfigurationProperties({JwtSecurityProperties.class, AdminJwtSecurityProperties.class})
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http, SecurityErrorResponseWriter errorWriter,
            CsrfTokenRepository csrfTokenRepository, AdminJwtSecurityProperties adminProperties,
            @Qualifier("adminJwtDecoder") org.springframework.beans.factory.ObjectProvider<JwtDecoder>
                    adminJwtDecoderProvider,
            org.springframework.beans.factory.ObjectProvider<
                    AdminJwtSecurityConfiguration.AdminJwtAuthenticationConverter>
                    adminJwtAuthenticationConverterProvider) throws Exception {
        http
                .securityMatcher("/api/admin/**")
                .cors(cors -> { })
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers(request -> hasBearerToken(request)))
                .securityContext(context -> context
                        .securityContextRepository(new NullSecurityContextRepository())
                        .requireExplicitSave(false))
                .sessionManagement(session -> session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable());

        http.authorizeHttpRequests(authorize -> {
            if (adminProperties.enabled()) {
                authorize
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/logout").permitAll()
                        .anyRequest().hasRole("ADMIN");
            } else {
                authorize.anyRequest().denyAll();
            }
        });
        if (adminProperties.enabled()) {
            JwtDecoder decoder = adminJwtDecoderProvider.getIfAvailable();
            var converter = adminJwtAuthenticationConverterProvider.getIfAvailable();
            if (decoder == null || converter == null) {
                throw new IllegalStateException("Admin JWT authentication is not configured");
            }
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .authenticationEntryPoint((request, response, exception) ->
                            errorWriter.write(response, ApiErrorCode.AUTHENTICATION_ERROR))
                    .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(converter)));
        }
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) ->
                        errorWriter.write(response, ApiErrorCode.AUTHENTICATION_ERROR))
                .accessDeniedHandler((request, response, exception) ->
                        errorWriter.write(response, ApiErrorCode.AUTHORIZATION_ERROR)));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, SecurityErrorResponseWriter errorWriter,
            CsrfTokenRepository csrfTokenRepository, JwtSecurityProperties jwtProperties,
            SecurityContextRepository securityContextRepository,
            @Qualifier("jwtDecoder") org.springframework.beans.factory.ObjectProvider<JwtDecoder>
                    jwtDecoderProvider,
            org.springframework.beans.factory.ObjectProvider<JwtSecurityConfiguration
                    .InvestorJwtAuthenticationConverter>
                    jwtAuthenticationConverterProvider) throws Exception {
        http
                .cors(cors -> { })
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers(request -> jwtProperties.enabled()
                                && hasBearerToken(request)))
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true))
                .sessionManagement(session -> session.sessionCreationPolicy(
                        jwtProperties.enabled() && !jwtProperties.sessionCompatibility()
                                ? SessionCreationPolicy.STATELESS : SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.disable())
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/accounts").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/reactivation").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/reactivation/check").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                errorWriter.write(response, ApiErrorCode.AUTHENTICATION_ERROR))
                        .accessDeniedHandler((request, response, exception) ->
                                errorWriter.write(response, ApiErrorCode.AUTHORIZATION_ERROR)));
        if (jwtProperties.enabled()) {
            JwtDecoder decoder = jwtDecoderProvider.getIfAvailable();
            Converter<Jwt, AbstractAuthenticationToken> converter = jwtAuthenticationConverterProvider
                    .getIfAvailable();
            if (decoder == null || converter == null) {
                throw new IllegalStateException("JWT authentication converter is not configured");
            }
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .authenticationEntryPoint((request, response, exception) ->
                            errorWriter.write(response, ApiErrorCode.AUTHENTICATION_ERROR))
                    .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(converter)));
        }
        return http.build();
    }

    private boolean hasBearerToken(jakarta.servlet.http.HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && authorization.startsWith("Bearer ");
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository(
            @Value("${app.security.session-cookie-secure:true}") boolean secure) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookieCustomizer(cookie -> cookie
                .httpOnly(false)
                .secure(secure)
                .sameSite("Lax")
                .path("/"));
        return repository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE,
                HttpHeaders.AUTHORIZATION, "X-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean(name = "corsFilter")
    CorsFilter corsFilter(
            @Qualifier("corsConfigurationSource") CorsConfigurationSource configurationSource,
            SecurityErrorResponseWriter errorWriter) {
        CorsFilter filter = new CorsFilter(configurationSource);
        filter.setCorsProcessor(new ApiCorsProcessor(errorWriter));
        return filter;
    }

    @Bean
    FilterRegistrationBean<CorsFilter> disableContainerCorsFilter(CorsFilter corsFilter) {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(corsFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    DefaultCookieSerializer cookieSerializer(
            @Value("${app.security.session-cookie-secure:true}") boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(secure);
        return serializer;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new PreHashingBCryptPasswordEncoder();
    }

    @Bean
    SecurityContextRepository securityContextRepository(JwtSecurityProperties jwtProperties) {
        if (jwtProperties.enabled() && !jwtProperties.sessionCompatibility()) {
            return new org.springframework.security.web.context.NullSecurityContextRepository();
        }
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }
}
