package no.carlham.workouttracker.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        // Hand 401/403 errors to ApiExceptionHandler so they become ProblemDetail JSON like all other errors
        AuthenticationEntryPoint authenticationEntryPoint =
                (request, response, exception) -> exceptionResolver.resolveException(request, response, null, exception);
        AccessDeniedHandler accessDeniedHandler =
                (request, response, exception) -> exceptionResolver.resolveException(request, response, null, exception);

        http
            // CSRF attacks rely on the browser sending credentials automatically. Access tokens are sent
            // by our own code in a header, and the refresh cookie is SameSite=Strict, so CSRF doesn't apply.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                .requestMatchers("/error").permitAll()
                // API documentation. Set springdoc.api-docs.enabled=false to turn it off in a deployment.
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/muscle-groups").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .bearerTokenResolver(bearerTokenResolver())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @Bean
    JwtEncoder jwtEncoder(AuthProperties properties) {
        return NimbusJwtEncoder.withSecretKey(secretKey(properties)).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Stores hashes as "{bcrypt}...", so the algorithm can be upgraded later without breaking old passwords
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // Reads the access token from the Authorization header, except on the public auth endpoints.
    // Spring would otherwise reject an invalid token before checking permitAll, so a frontend that sends
    // its expired token along with POST /api/auth/refresh would get a 401 and could never refresh.
    private static BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
        return request -> request.getRequestURI().startsWith("/api/auth/")
                ? null
                : headerResolver.resolve(request);
    }

    // Turns the token's "role" claim into a Spring authority, e.g. "ADMIN" -> "ROLE_ADMIN",
    // which is what hasRole("ADMIN") checks for
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(JwtClaims.ROLE);
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    private static SecretKey secretKey(AuthProperties properties) {
        return new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
