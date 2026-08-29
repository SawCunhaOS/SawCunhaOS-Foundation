# scos-foundation-audit

Módulo de trilha de auditoria para serviços SCOS Spring Boot. Captura C/U/D via Hibernate listener,
persiste em datasource isolado com pipeline de batching, hash-chain para tamper-evidence, consulta
paginada e retenção configurável.

---

## Arquitetura interna (v1.2.0)

```
Produção de eventos
───────────────────
 @Auditable entity       @Auditable(READ) method / auditService.recordRead()
        │                                │
        ▼                                ▼
ScosHibernateAuditListener       ScosAuditReadAspect
  (PostInsert/Update/Delete)
        │                                │
        └──────────────┬─────────────────┘
                       ▼
               ScosAuditServiceBean
               (thread: ScosAuditLogAsyncExecutor)
                       │
                       ▼
               ScosAuditQueue
               (LinkedBlockingQueue — capacity: queue-capacity)
                       │  fila cheia → DLQ direto
                       ▼
               ScosAuditBatchConsumer
               (scheduled: flush-interval-ms)
               ├── event-order monotônico
               ├── truncar executionDate → micros
               ├── [opt] hash-chain SHA-256 por (entity, idEntity)
               └── persistWithRetry (retry-max tentativas, backoff exp.)
                       │
           ┌───────────┴──────────────┐
           ▼                          ▼
    SFA_LOG_AUDIT               SFA_AUDIT_DLQ
  (datasource audit)       (reprocessado por ScosAuditDlqJob)

Consulta / integridade
──────────────────────
 ScosAuditQueryService    ScosAuditIntegrityService
        │                          │
        ▼                          ▼
  SFA_LOG_AUDIT           verificação da cadeia
  (read-only queries)     hash SHA-256
```

---

## Dependência

```xml
<dependency>
    <groupId>br.com.sawcunhaos</groupId>
    <artifactId>scos-foundation-audit</artifactId>
</dependency>
```

---

## Configuração mínima (`application.yml`)

```yaml
scos:
  audit:
    enabled: true
    system: MEU_SISTEMA          # identifica o sistema de origem nos logs

spring:
  datasource:
    audit:
      url: jdbc:postgresql://localhost:5432/audit_db
      username: audit_user
      password: secret
      hikari:
        maximum-pool-size: 10
        minimum-idle: 2
        connection-timeout: 5000
        idle-timeout: 150000
        max-lifetime: 300000
        pool-name: AuditPool
```

---

## Marcando entidades como auditáveis

```java
import br.com.sawcunhaos.foundation.audit.api.Auditable;

@Entity
@Table(name = "SFA_PEDIDO")
@Auditable  // captura INSERT, UPDATE, DELETE automaticamente via Hibernate
public class Pedido {
    @Id
    private UUID id;
    // ...
}
```

Isso é suficiente. Toda operação C/U/D na entidade será capturada em `SFA_LOG_AUDIT`.

---

## Auditando leituras de PII (`@Auditable` em métodos)

```java
import br.com.sawcunhaos.foundation.audit.api.AuditAction;
import br.com.sawcunhaos.foundation.audit.api.Auditable;

@Service
public class PedidoService {

    // Emite ActionType.SELECT após retorno bem-sucedido
    @Auditable(action = AuditAction.READ, entity = "SFA_PEDIDO", idEntitySpEL = "#id.toString()")
    public Pedido buscarPedido(UUID id) {
        return pedidoRepository.findById(id).orElseThrow();
    }
}
```

Para operações JPQL/bulk que escapam do Hibernate:

```java
@Autowired
private ScosAuditService auditService;

public void processarBulk(String pedidoId) {
    // ... operação bulk ...
    auditService.recordRead("SFA_PEDIDO", pedidoId);
}
```

---

## Consultando a trilha

```java
@Autowired
private ScosAuditQueryService auditQueryService;

// Quem alterou o Pedido "abc-123" e quando?
Page<ScosAuditLog> trilha = auditQueryService.findByEntity(
    "SFA_PEDIDO", "abc-123", PageRequest.of(0, 50, Sort.by("executionDate").descending())
);

// Todos os eventos de um usuário
Page<ScosAuditLog> eventosPorUsuario = auditQueryService.findByUser("joao.silva", PageRequest.of(0, 20));

// Eventos em um período
Page<ScosAuditLog> periodo = auditQueryService.findByPeriod(
    LocalDateTime.now().minusDays(7), LocalDateTime.now(), PageRequest.of(0, 100)
);

// Evento de uma requisição específica
Optional<ScosAuditLog> evento = auditQueryService.findByXRequestId("req-uuid-xyz");
```

---

## Verificação de integridade (hash-chain)

```yaml
scos:
  audit:
    immutability:
      hash-chain: true    # default: false (opt-in)
```

```java
@Autowired
private ScosAuditIntegrityService integrityService;

boolean integra = integrityService.verifyChain("SFA_PEDIDO", "abc-123");
// false = adulteração ou remoção de registro detectada
```

---

## Serialização JSON (Jackson) e política de nulos

Desde a 1.2.0, todos os pontos de serialização/desserialização JSON do módulo usam Jackson
(`tools.jackson`), não mais Gson — `GsonUtils` foi removido do `utils`.

**Política de nulos**: os payloads de auditoria sempre incluem campos nulos explicitamente,
igual ao comportamento anterior (a instância `GsonUtils` usava `serializeNulls()`):

- `ScosAuditLog` está anotado com `@JsonInclude(JsonInclude.Include.ALWAYS)` — usado na íntegra
  ao serializar o payload da DLQ (`ScosAuditBatchConsumer.routeToDlq`).
- O snapshot do estado da entidade (`entityOld`/`entityNew`, um `Map<String,Object>` serializado em
  `ScosAuditServiceBean.createJsonObject`) depende do default do Jackson para `Map`, que já inclui
  valores nulos sem configuração adicional.

Por que isso importa: um campo ausente e um campo `null` explícito têm significados diferentes numa
trilha de auditoria — omitir viraria uma lacuna silenciosa na reconstrução do estado.

**Mudança de formato de wire**: datas (`LocalDateTime`/`OffsetDateTime`) agora são serializadas pelo
suporte nativo do Jackson a `java.time` (`jackson-datatype-jsr310`), não mais pelos 3 adapters Gson
customizados removidos nesta versão — ver CHANGELOG.

---

## Grants append-only (recomendado)

Para impedir que a aplicação modifique ou delete registros de auditoria:

```sql
-- Aplicar no banco de dados de audit
REVOKE UPDATE, DELETE ON SFA_LOG_AUDIT FROM <your_app_role>;
GRANT INSERT, SELECT ON SFA_LOG_AUDIT TO <your_app_role>;
GRANT INSERT, SELECT, UPDATE, DELETE ON SFA_AUDIT_DLQ TO <your_app_role>;
```

---

## Tuning de performance (`scos.audit.performance.*`)

| Property | Default | Descrição |
|---|---|---|
| `queue-capacity` | `10000` | Capacidade máxima da fila em memória |
| `batch-size` | `100` | Eventos por `saveAll` |
| `flush-interval-ms` | `500` | Drena mesmo sem atingir `batch-size` |

Para carga alta (>1000 ev/s), aumentar `queue-capacity` e reduzir `flush-interval-ms`:

```yaml
scos:
  audit:
    performance:
      queue-capacity: 50000
      batch-size: 200
      flush-interval-ms: 200
```

---

## Durabilidade (`scos.audit.durability.*`)

```yaml
scos:
  audit:
    durability:
      retry-max: 3              # tentativas antes de ir para DLQ
      dlq-enabled: true         # false = eventos perdidos em silêncio (não recomendado)
      dlq-reprocess-interval-ms: 60000   # frequência de reprocessamento da DLQ
```

Monitorar a métrica `audit.events.dlq` (Counter Micrometer). Alertar quando > 0.

---

## Retenção (`scos.audit.retention.*`)

```yaml
scos:
  audit:
    retention:
      enabled: true
      ttl-days: 1825    # 5 anos — obrigatório quando enabled=true
      cron: "0 0 2 * * *"   # default: 02:00 diariamente
```

Registros expirados são deletados; um tombstone (`ActionType.TOMBSTONE`) é inserido para preservar
a cadeia de hash.

---

## Métricas Micrometer

| Métrica | Tipo | Descrição |
|---|---|---|
| `audit.queue.depth` | Gauge | Eventos aguardando na fila |
| `audit.batch.size` | DistributionSummary | Tamanho de cada lote persistido |
| `audit.events.dlq` | Counter | Eventos enviados à DLQ |

---

## Troubleshooting

### Módulo não inicia / beans não sobem

`scos.audit.enabled=true` é obrigatório e não tem `matchIfMissing`. Sem essa property (ou com `false`)
nenhum bean do módulo é registrado — nenhum erro explícito, apenas ausência dos beans.

```yaml
scos:
  audit:
    enabled: true   # obrigatório
```

### Eventos não aparecem na trilha

A entidade precisa de `@Auditable`. Sem a anotação o listener Hibernate ignora a operação.

```java
@Entity
@Table(name = "SFA_PEDIDO")
@Auditable  // ← obrigatório
public class Pedido { ... }
```

Para leituras: verificar se o método está anotado com `@Auditable(action = AuditAction.READ)` ou
se `auditService.recordRead()` está sendo chamado explicitamente.

### Fila cheia — eventos indo para DLQ sem falha de persistência

Sintoma: `audit.queue.depth` atingindo o limite, `audit.events.dlq` incrementando sem erro de banco.

Causa: volume de eventos supera a capacidade da fila em memória.

```yaml
scos:
  audit:
    performance:
      queue-capacity: 50000      # aumentar (default: 10000)
      flush-interval-ms: 200     # reduzir para drenar mais rápido (default: 500)
```

### DLQ acumulando

Sintoma: métrica `audit.events.dlq` crescendo — alertar quando > 0.

Diagnóstico:
1. Verificar conectividade com o banco de auditoria (`spring.datasource.audit.url`)
2. Verificar grants — o role da aplicação precisa de `INSERT, SELECT` em `SFA_LOG_AUDIT`
3. Verificar que `scos.audit.durability.dlq-enabled=true` (default); com `false` eventos são
   descartados silenciosamente após `retry-max` tentativas

---

## Migration Liquibase

O módulo gerencia suas próprias migrations via `db/changelog/db_audit.changelog-master.yaml`.
O datasource de audit é completamente isolado do datasource de negócio.

```yaml
scos:
  audit:
    liquibase:
      enabled: true
      change-log: classpath:db/changelog/db_audit.changelog-master.yaml
      default-schema: public
```
