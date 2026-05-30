---
name: scos-jdempotent-config
description: >
  Configurar o módulo scos-foundation-jdempotent num sistema consumidor SCOS — idempotência de requisições
  via Redis (scos.jdempotent.*) e anotações @JdempotentResource/@JdempotentId/@JdempotentRequestPayload.
  Use ao proteger endpoints contra reprocessamento duplicado ou ao configurar o Redis da idempotência.
---

# Configuração — `scos-foundation-jdempotent`

Idempotência de requisições: a mesma chave não reprocessa: a resposta anterior é devolvida. Estado em Redis.

## 1. Dependência

```xml
<dependency>
  <groupId>br.com.sawcunhaos</groupId>
  <artifactId>scos-foundation-jdempotent</artifactId>
</dependency>
```

Auto-configura por `AutoConfiguration.imports`.

## 2. Ativação + Redis

Ligado por `scos.jdempotent.enabled` (`matchIfMissing=true`). Exige Redis.

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
scos:
  jdempotent:
    enabled: true
    cache:
      redis:
        expirationTimeHour: 24
        dialTimeoutSecond: 5
        readTimeoutSecond: 5
        writeTimeoutSecond: 5
        maxRetryCount: 3
        persistReqRes: true     # guarda payload de request/response
```

## 3. Uso (anotações em `utils.annotation.jdempotent`)

```java
@ScosRequestPOST(uri = "/companies", httpCode = 201)
@JdempotentResource(cachePrefix = "create-company", ttl = 24, ttlTimeUnit = TimeUnit.HOURS)
public ScosResponseDTO<CompanyDTO> create(
        @JdempotentRequestPayload @RequestBody CreateCompanyRequest request,
        @JdempotentId @RequestHeader("Idempotency-Key") String key) {
    // ...
}
```

- `@JdempotentResource` — marca o método idempotente.
- `@JdempotentId` — fonte da chave de idempotência (header/param).
- `@JdempotentRequestPayload` — payload considerado na chave.
- `@JdempotentProperty` / `@JdempotentIgnore` — incluir/excluir campos do hash da chave.

## Pegadinhas

- Sem Redis acessível a config falha no startup.
- `scos.jdempotent.enabled=false` desliga o módulo (default ligado por `matchIfMissing`).
- A chave de idempotência deve ser estável por requisição lógica — header `Idempotency-Key` é o padrão.
