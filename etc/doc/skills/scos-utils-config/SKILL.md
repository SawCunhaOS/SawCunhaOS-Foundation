---
name: scos-utils-config
description: >
  Configurar o módulo scos-foundation-utils num sistema consumidor SCOS — dependência,
  @ComponentScan obrigatório, filtros de log HTTP (server.filter.*), cache Redis (scos.cache.*),
  validadores BR, normalização de strings e DTOs/anotações @Scos*. Use ao iniciar um novo serviço
  SCOS ou ao ligar/ajustar log de requisição, cache ou validação. Base obrigatória dos demais módulos.
---

# Configuração — `scos-foundation-utils`

Módulo **base** da foundation. Traz anotações `@Scos*`, DTOs, validadores, filtros de log HTTP,
cache Redis, normalização de strings, `GsonUtils` e (transitivamente) o módulo `privacy`.

## 1. Dependência

```xml
<dependency>
  <groupId>br.com.sawcunhaos</groupId>
  <artifactId>scos-foundation-utils</artifactId>
</dependency>
```

> Puxa `scos-foundation-privacy` transitivamente (masking de PII consumido pelos filtros de log).

## 2. Ativação — `@ComponentScan` obrigatório

`utils`/`exception`/`security` sobem por **component scan**, não por `AutoConfiguration.imports`. O app
consumidor precisa varrer o pacote da foundation:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"br.com.sawcunhaos"})
public class MeuSistemaApplication {
    public static void main(String[] args) { SpringApplication.run(MeuSistemaApplication.class, args); }
}
```

## 3. Filtros de log HTTP

`LoggingInitialFilter` (`@Order(0)`) e `LoggingFinalFilter` (`@Order(100)`) logam requisição/resposta com
MDC (`X-Request-ID`, `IS_IP`) e **mascaram PII** via `privacy` antes de gravar. Logam só para URIs que contêm
`getURI()` (= `context-path` + `/api`).

```yaml
server:
  servlet:
    context-path: /            # getURI() = /api
  filter:
    show-request-body: false   # corpo da requisição no log (mascarado)
    show-request-headers: false
    show-response-body: false
```

PII no corpo/headers é mascarada pelas regras do `privacy` (ver skill `scos-privacy-config`).

## 4. Cache Redis (`scos.cache`)

Gated por `spring.cache.enabled=true`; exige `spring.data.redis.*`.

```yaml
spring:
  cache:
    enabled: true
  data:
    redis:
      host: localhost
      port: 6379
scos:
  cache:
    redis-time-to-live: 3600       # segundos (default 3600)
    key-prefix: meusistema         # opcional
    enable-compression: false
    compression-threshold: 1024    # bytes
    caches:
      - name: empresas
        ttl: 600
```

Liga no método via `@ScosRequestGET(nameCache = "empresas", keyGenerator = ...)`.

## 5. Validadores BR e utilitários

- Validação Jakarta em DTOs/VOs: `@CPF`, `@CNPJ`, `@TaxIdentifier`, `@ZipCode` — preferir sobre regex.
- `@NormalizeStrings` + `StringProcessingAspect` (trim/upper/lower por campo).
- DTOs de resposta: `ScosResponseDTO`, `ScosPaginatedDTO`, `ScosPaginationFilterDTO`.
- Web: `@ScosController`, `@ScosRequestGET/POST/PUT/DELETE`.
- `GsonUtils`, adapters de data, `LocaleService` (i18n).
- Anotações de `audit` (`@Auditable`) e `jdempotent` (`@JdempotentResource`, …) vivem aqui.

## Pegadinhas

- Sem `@ComponentScan("br.com.sawcunhaos")` os filtros/handlers **não sobem**.
- Cache exige `spring.cache.enabled=true` **e** Redis acessível; senão a config recua.
- O masking dos filtros depende das regras do `privacy-masking.yml` (ver `scos-privacy-config`).
