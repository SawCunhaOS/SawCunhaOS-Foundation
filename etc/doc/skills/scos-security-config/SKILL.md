---
name: scos-security-config
description: >
  Configurar o módulo scos-foundation-security num sistema consumidor SCOS — resource server OAuth2/JWT
  (Keycloak), datasource dedicado de segurança (scos.security.datasource.*), cache de login/permissão
  (scos.security.cache.*), CORS (cors-security.*), usuário corrente (ScosUserAuthentication) e permissões.
  Use ao proteger endpoints, validar JWT ou mapear roles/permissões.
---

# Configuração — `scos-foundation-security`

Autenticação/autorização: resource server OAuth2 com JWT (Keycloak), permissões em banco dedicado, cache de
login/permissão, CORS e usuário corrente (`ScosUserAuthentication`).

## 1. Dependência

```xml
<dependency>
  <groupId>br.com.sawcunhaos</groupId>
  <artifactId>scos-foundation-security</artifactId>
</dependency>
```

## 2. Ativação

As classes `@AutoConfiguration`/`@Configuration` do módulo sobem pelo **component scan** (não há
`AutoConfiguration.imports`). Exige `@ComponentScan(basePackages = {"br.com.sawcunhaos"})` no app
(ver `scos-utils-config`).

## 3. Resource server JWT (Keycloak)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.exemplo.com/realms/meu-realm
```

`JwtAuthConverter` extrai o principal de `preferred_username` e mapeia authorities.

## 4. Datasource dedicado de segurança

Permissões/login ficam em datasource **próprio** (`scos.security.datasource`):

```yaml
scos:
  security:
    datasource:
      url: jdbc:postgresql://localhost:5432/security
      username: security
      password: ${SECURITY_DB_PASSWORD}
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 10
```

## 5. Cache de login/permissão

```yaml
scos:
  security:
    cache:
      maximum-size-logins: 1000
      maximum-size-permission: 5000
      expire-after-write-login: 2        # minutos
      expire-after-access-login: 1
      expire-after-write-permission: 10
```

## 6. CORS (`cors-security.*`)

```yaml
cors-security:
  allowOrigin: "https://app.exemplo.com"        # CSV; default *
  allowMethods: "GET,POST,DELETE,PUT,OPTIONS"
  allowHeaders: "*"
  allowCredentials: "true"
  maxAge: "1800"
```

## 7. Uso

```java
@ScosRequestGET(uri = "/companies")
@PreAuthorize("hasAuthority('COMPANY_READ')")
public ScosResponseDTO<List<CompanyDTO>> list() {
    ScosUserAuthentication user = AuthenticationUtils.currentUser();   // usuário corrente
    // ...
}
```

`ScosPermission`/`ScosFeature` modelam permissões; `ScosCorsFilter` e `AuthorizationRequiredFilter` cuidam
de CORS e checagem de acesso.

## Pegadinhas

- Sem `@ComponentScan("br.com.sawcunhaos")` os beans de segurança **não sobem** (módulo não tem `.imports`).
- Datasource de segurança é `scos.security.datasource.*` (separado do principal e do de audit).
- CORS usa prefixo `cors-security.*` (via `@Value`), **não** `scos.security.cors`.
- `issuer-uri` precisa ser alcançável no startup (o resource server valida a metadata do realm).
