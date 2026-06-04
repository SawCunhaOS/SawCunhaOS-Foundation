---
name: scos-audit-config
description: >
  Configurar o módulo scos-foundation-audit num sistema consumidor SCOS — trilha de auditoria via
  @Auditable + listener Hibernate, datasource dedicado (spring.datasource.audit.*), Liquibase próprio,
  executor @Async, consulta paginada (ScosAuditQueryService), hash-chain SHA-256, retenção automática,
  monitoramento de DLQ e cifra em repouso de PII (auditEncryptFields via privacy). Use ao auditar
  entidades, leituras de PII, consultar a trilha, configurar integridade ou retenção.
---

# Configuração — `scos-foundation-audit`

Registra INSERT/UPDATE/DELETE de entidades anotadas em uma trilha (`ScosAuditLog`, JSONB
`entityOld`/`entityNew`) via evento Hibernate, em **datasource separado** e thread `@Async`.

## 1. Dependência

```xml
<dependency>
  <groupId>br.com.sawcunhaos</groupId>
  <artifactId>scos-foundation-audit</artifactId>
</dependency>
```

Depende de `utils` e `privacy`. Auto-configura por `AutoConfiguration.imports`.

## 2. Ativação + datasource dedicado

Ligado por `scos.audit.enabled=true`. A trilha usa um datasource **próprio** (`spring.datasource.audit`),
separado do datasource da aplicação.

```yaml
scos:
  audit:
    enabled: true
    system: MEU_SISTEMA            # preenche originSystem na trilha
    liquibase:
      enabled: true
      change-log: classpath:/db/changelog/db.audit.changelog-master.yaml
spring:
  datasource:
    audit:
      url: jdbc:postgresql://localhost:5432/audit
      username: audit
      password: ${AUDIT_DB_PASSWORD}
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 10
```

`scos.audit.liquibase.*` estende as chaves base de Liquibase (`change-log`, `default-schema`,
`database-change-log-table`, `contexts`, `labels`, …).

## 3. Auditar uma entidade

```java
@Entity
@Table(name = "SFA_COMPANY")
@Auditable
public class Company { /* ... */ }
```

Cada persistência gera um `ScosAuditLog` (actionType, entity, idEntity, entityOld/New, user, originSystem,
ipAddress, xRequestId). A escrita ocorre na thread `ScosAuditLogAsyncExecutor` (`@Async`).

## 4. Auditar leituras de PII

Operações de leitura não são capturadas pelo listener Hibernate. Duas formas:

**Aspecto automático** — anote o método de serviço:

```java
@Auditable(action = AuditAction.READ, entity = "SFA_PEDIDO", idEntitySpEL = "#id.toString()")
public Pedido buscarPedido(UUID id) {
    return pedidoRepository.findById(id).orElseThrow();
}
```

`idEntitySpEL` é uma expressão SpEL resolvida contra os parâmetros do método. O evento é emitido
após retorno bem-sucedido (`ActionType.SELECT`).

**Chamada direta** — para queries JPQL/bulk que escapam do Hibernate:

```java
@Autowired
private ScosAuditService auditService;

public void processarBulk(UUID pedidoId) {
    // ... operação bulk ...
    auditService.recordRead("SFA_PEDIDO", pedidoId.toString());
}
```

Ver `audit/README.md` para detalhes sobre SpEL e casos de uso.

## 5. Consultar a trilha

Injete `ScosAuditQueryService` — opera no datasource de auditoria, não interfere no datasource principal.

```java
@Autowired
private ScosAuditQueryService auditQueryService;

// Trilha de um registro específico
Page<ScosAuditLog> trilha = auditQueryService.findByEntity(
    "SFA_PEDIDO", "abc-123",
    PageRequest.of(0, 50, Sort.by("executionDate").descending())
);

// Todos os eventos de um usuário
Page<ScosAuditLog> porUsuario = auditQueryService.findByUser(
    "joao.silva", PageRequest.of(0, 20)
);

// Eventos em um período
Page<ScosAuditLog> periodo = auditQueryService.findByPeriod(
    LocalDateTime.now().minusDays(7), LocalDateTime.now(),
    PageRequest.of(0, 100)
);

// Evento de uma requisição específica
Optional<ScosAuditLog> evento = auditQueryService.findByXRequestId("req-uuid-xyz");
```

Ver `audit/README.md` para exemplos adicionais e campos de `ScosAuditLog`.

## 6. Hash-chain (imutabilidade opt-in)

Ativa verificação de tamper-evidence via SHA-256 encadeado por `(entity, idEntity)`:

```yaml
scos:
  audit:
    immutability:
      hash-chain: true    # default: false
```

```java
@Autowired
private ScosAuditIntegrityService integrityService;

boolean integra = integrityService.verifyChain("SFA_PEDIDO", "abc-123");
// false = adulteração ou remoção de registro detectada
```

## 7. Retenção automática

```yaml
scos:
  audit:
    retention:
      enabled: true
      ttl-days: 1825          # obrigatório quando enabled=true (ex: 5 anos)
      cron: "0 0 2 * * *"    # default: 02:00 diariamente
```

Registros expirados são deletados; um `ActionType.TOMBSTONE` é inserido para preservar a hash-chain.

**Atenção**: `ttl-days` é obrigatório quando `enabled=true` — ausência lança `IllegalStateException` na inicialização.

## 8. Monitorar DLQ

Eventos vão para a DLQ quando `retry-max` tentativas de persistência falham:

```yaml
scos:
  audit:
    durability:
      retry-max: 3          # default: 3
      dlq-enabled: true     # default: true — false = eventos descartados silenciosamente
      dlq-reprocess-interval-ms: 60000   # default: 60 s
```

Alertar quando a métrica `audit.events.dlq` (Counter Micrometer) for > 0.

Grants recomendados para append-only no banco de auditoria:

```sql
REVOKE UPDATE, DELETE ON SFA_LOG_AUDIT FROM <your_app_role>;
GRANT INSERT, SELECT ON SFA_LOG_AUDIT TO <your_app_role>;
GRANT INSERT, SELECT, UPDATE, DELETE ON SFA_AUDIT_DLQ TO <your_app_role>;
```

## 9. Cifra em repouso de PII (opt-in, via `privacy`)

Campos listados em `audit-encrypt-fields` (no `privacy-masking.yml`) são cifrados antes de gravar o JSONB,
gerando tokens `enc:vN:`. Exige a chave do `privacy`:

```yaml
scos:
  privacy:
    crypto:
      secret: ${SCOS_PRIVACY_CRYPTO_SECRET}
    masking:
      # privacy-masking.yml
      # audit-encrypt-fields: [cpf, email]
```

Lista vazia = comportamento atual (texto em claro). Rotação: chave nova só afeta registros novos; histórico
é decifrado pelo `keyId` embutido no token. Ver `scos-privacy-config`.

## 10. Referência de properties

| Property | Tipo | Default | Descrição |
|---|---|---|---|
| `scos.audit.enabled` | boolean | `false` | Ativa o módulo (obrigatório) |
| `scos.audit.system` | String | `SFA_AUDIT` | Nome do sistema na trilha |
| `scos.audit.performance.queue-capacity` | int | `10000` | Capacidade da fila em memória |
| `scos.audit.performance.batch-size` | int | `100` | Eventos por `saveAll` |
| `scos.audit.performance.flush-interval-ms` | long | `500` | Intervalo de flush (ms) |
| `scos.audit.durability.retry-max` | int | `3` | Tentativas antes da DLQ |
| `scos.audit.durability.dlq-enabled` | boolean | `true` | Habilita a DLQ |
| `scos.audit.durability.dlq-reprocess-interval-ms` | long | `60000` | Intervalo de reprocessamento da DLQ (ms) |
| `scos.audit.immutability.hash-chain` | boolean | `false` | Ativa hash-chain SHA-256 (opt-in) |
| `scos.audit.retention.enabled` | boolean | `false` | Ativa retenção automática |
| `scos.audit.retention.ttl-days` | Integer | — | TTL em dias (obrigatório quando `enabled=true`) |
| `scos.audit.retention.cron` | String | `0 0 2 * * *` | Cron de execução da retenção |
| `scos.audit.liquibase.enabled` | boolean | `true` | Roda migrations do módulo |
| `scos.audit.liquibase.change-log` | String | (interno) | Path do changelog |
| `scos.audit.liquibase.default-schema` | String | `public` | Schema alvo |

`spring.datasource.audit.*` — datasource isolado; aceita as mesmas chaves do datasource padrão Spring Boot
(`url`, `username`, `password`, `driver-class-name`, `hikari.*`).

## Pegadinhas

- `scos.audit.enabled=true` é **obrigatório** (sem `matchIfMissing`); sem isso os beans não sobem.
- O datasource é `spring.datasource.audit.*` (separado) — não reaproveita o datasource principal.
- A entidade precisa de `@Auditable`; sem a anotação não é auditada.
- `dlq-enabled=false` faz eventos serem **descartados silenciosamente** quando todas as tentativas falham — não recomendado em produção.
- `retention.enabled=true` sem `ttl-days` lança `IllegalStateException` na inicialização da aplicação.
- Cifra só atua se houver `scos.privacy.crypto.secret` **e** `audit-encrypt-fields` não vazio.
