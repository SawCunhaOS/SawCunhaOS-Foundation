---
name: spring-security-scos
description: >
  Add and review authentication and authorization in SCOS Spring Boot services
  with Spring Security 7 (Spring Boot 4). Use this skill whenever the user works
  on security: securing REST endpoints, OAuth2/OIDC resource server, JWT
  validation, method security / @PreAuthorize, role and scope mapping, CORS/CSRF
  for APIs, the current user, permission checks, or encrypting secrets — even if
  they don't name Spring Security. Targets the current stable Spring Boot 4.0.x /
  Spring Security 7 / Java 25, and integrates the SCOS foundation's security
  pieces (ScosUserAuthentication, ScosPermission, ScosFeature, ScosSecurityException,
  Jasypt). Follows the SCOS conventions (controllers under /api, errors via the
  foundation ExceptionsHandler).
---

# Spring Security for SCOS (Security 7 / Spring Boot 4.0.x)

This skill secures and reviews SCOS services. Default posture for a SCOS API
behind an identity provider: a **stateless OAuth2 resource server** validating
JWT bearer tokens, with method-level authorization and the foundation's user/
permission abstractions on top. Spring Security 7 (shipped with Boot 4) uses the
component-based `SecurityFilterChain` bean with the lambda DSL.

## First step: decide the mode

- Adding/securing endpoints, wiring authn/authz → **Generate**.
- Reviewing an existing security setup for holes → **Review**.

Read `references/resource-server.md` (filter chain, JWT, role/scope mapping,
method security, SCOS integration) before producing substantial output.

## Generate

1. Add `spring-boot-starter-oauth2-resource-server` (JWT support comes via
   `spring-security-oauth2-jose`). Versions from the SCOS BOM.
2. Point the service at the identity provider with
   `spring.security.oauth2.resourceserver.jwt.issuer-uri` (or `jwk-set-uri`),
   externalized via env var. The default `NimbusJwtDecoder` fetches the JWKS and
   validates signature, `exp`, `nbf`, `iss`.
3. Define a `SecurityFilterChain` bean (lambda DSL):
   - Stateless session, CSRF disabled (token-based API, no browser session).
   - `authorizeHttpRequests` — permit Actuator health/`/v3/api-docs` as policy
     dictates; everything else authenticated.
   - `oauth2ResourceServer(o -> o.jwt(...))`.
   See the example in `references/resource-server.md`.
4. Map token claims to authorities. Spring maps `scope`/`scp` to `SCOPE_*` by
   default; if roles live in a custom claim (e.g. Keycloak `realm_access.roles`),
   supply a `JwtAuthenticationConverter`.
5. Authorize at the method level with `@EnableMethodSecurity` + `@PreAuthorize`
   on application services, expressing intent in SCOS permission terms.
6. Integrate the foundation: read the current principal via `ScosUserAuthentication`
   instead of touching `SecurityContextHolder` directly; express permission/feature
   checks via `ScosPermission`/`ScosFeature`; throw `ScosSecurityException` for
   authorization failures so the foundation's `ExceptionsHandler` renders the
   standard error. Encrypt sensitive properties with Jasypt rather than committing
   plaintext.

## Review

Assess against `references/resource-server.md`'s checklist. The findings that
recur and bite hardest: endpoints unintentionally left open (a permissive
`anyRequest().permitAll()` or a missing matcher), tokens validated without
checking `iss`/audience, secrets in plaintext config, CORS opened to `*` with
credentials, and authorization done in controllers instead of at the service
boundary. Lead with any actual auth bypass.

Frame as a one-paragraph health summary, then impact-ordered findings (auth
bypass / open endpoints > weak token validation > secret handling > CORS/CSRF >
where authz lives), each with a concrete fix. Be precise — security review
findings must be specific, not vague "consider hardening" notes.

## Principle

Authentication proves who; authorization decides what they may do. Keep the
filter chain explicit (deny by default, permit the few public paths on purpose),
validate tokens fully (signature + issuer + audience + expiry), and put authz
decisions where the business operation lives — the application service — not
scattered in controllers.
