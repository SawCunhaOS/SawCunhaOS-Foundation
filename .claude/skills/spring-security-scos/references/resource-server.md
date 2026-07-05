# Resource server, claim mapping, SCOS integration & review

Spring Security 7 / Spring Boot 4.0.x. Stateless JWT resource server is the
default for a SCOS API behind an IdP.

## Dependencies

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<!-- spring-security-oauth2-jose comes transitively and provides JWT decode/verify -->
```

## Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OAUTH_ISSUER_URI}          # e.g. https://idp.example.com/realms/scos
          # jwk-set-uri: ${OAUTH_JWKS_URI}         # alternative if issuer discovery isn't used
          audiences: ${OAUTH_AUDIENCE:}            # validate the aud claim
```

## SecurityFilterChain (lambda DSL)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity                       // enables @PreAuthorize on services
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())                              // stateless API
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())                        // deny by default
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(scosJwtConverter())))
            .build();
    }

    private JwtAuthenticationConverter scosJwtConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // map roles from a custom claim (e.g. Keycloak realm_access.roles) to ROLE_*
            var roles = Optional.ofNullable(jwt.getClaimAsMap("realm_access"))
                .map(m -> (List<String>) m.get("roles")).orElse(List.of());
            return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        });
        return converter;
    }
}
```

Without a converter, Spring maps `scope`/`scp` claims to `SCOPE_*` authorities
automatically. Add the converter only when your roles live elsewhere in the
token.

## Method security

```java
@Service("CancelOrderService")
@RequiredArgsConstructor
public class CancelOrderServiceBean implements CancelOrderService {

    @Override
    @PreAuthorize("hasRole('ORDER_MANAGER')")     // or hasAuthority('SCOPE_orders:write')
    public void cancel(OrderId id) { /* ... */ }
}
```

Put `@PreAuthorize` on the application service (where the operation lives), not on
controllers. Express the rule in domain/permission terms.

## SCOS integration

- **Current user:** read the principal through the foundation's
  `ScosUserAuthentication` rather than calling `SecurityContextHolder` directly —
  it's the house abstraction and keeps services testable.
- **Permissions/features:** model authorization with `ScosPermission` /
  `ScosFeature` where the app uses a permission/feature scheme beyond plain roles.
- **Errors:** throw `ScosSecurityException` for authorization failures; the
  foundation's global `ExceptionsHandler` renders it as the standard
  `ExceptionResponse`. Don't write a bespoke 403 handler.
- **Secrets:** encrypt sensitive properties with Jasypt (in the BOM/foundation)
  instead of committing plaintext; inject the password via env.

## CORS (only if browsers call the API directly)

Configure a `CorsConfigurationSource` with explicit allowed origins/methods.
Never combine `allowedOrigins("*")` with `allowCredentials(true)` — that's both
invalid and a leak. Prefer a narrow allowlist from config.

## Review checklist (impact order)

### 1. Auth bypass / open endpoints (highest impact)
- Is the default deny (`anyRequest().authenticated()`), with public paths
  permitted on purpose? Any stray `permitAll()`/`anyRequest().permitAll()`?
- Are Actuator/management endpoints exposed without auth beyond health?
- Is the filter chain actually applied (a `SecurityFilterChain` bean present), or
  relying on defaults that may not match intent?

### 2. Token validation strength
- Is `issuer-uri`/`jwk-set-uri` configured so signatures are verified against the
  JWKS? Is `aud` (audience) validated, not just signature + expiry?
- Are tokens treated as stateless (no server session) with CSRF off accordingly?

### 3. Secret handling
- Are issuer URIs, client secrets, and keys externalized (env) / Jasypt-encrypted,
  not in committed config?

### 4. Authorization placement & model
- Is authz at the service boundary (`@PreAuthorize`) or scattered in controllers?
- Are roles/scopes mapped correctly from the actual token claims?
- Is the current user read via `ScosUserAuthentication`, not raw
  `SecurityContextHolder`?

### 5. CORS/CSRF
- Is CORS a narrow allowlist (no `*` + credentials)?
- Is CSRF handled correctly for the app type (off for stateless token APIs, on for
  session/browser flows)?

### Output format for a review
1. **Health summary** — one paragraph: can an unauthenticated/under-privileged
   caller reach something they shouldn't?
2. **Findings** — impact-ordered, each with why + a precise, concrete fix.
3. **What's solid** — 1–3 things worth keeping.
