## Context

O módulo `audit` do `scos-foundation` captura eventos C/U/D via `ScosHibernateAuditListener` e persiste em datasource isolado. O caminho atual é: evento → `@Async` (virtual thread pool-30) → `saveAndFlush` individual. Não há batching, não há tratamento de erro além de `log.error`, não há consulta útil, não há integridade verificável, não há cobertura de READ e não há retenção.

O change fecha todas essas lacunas sem adicionar dependências externas (sem broker, sem outbox transacional no datasource de negócio).

## Goals / Non-Goals

**Goals:**
- Pipeline assíncrona com fila em memória + batch consumer + `saveAll` em lote
- Durabilidade por retry + DLQ em tabela auxiliar no datasource audit
- Consulta paginada da trilha por entidade, usuário, período e `xRequestId`
- Imutabilidade via hash encadeado (SHA-256) + política append-only no DB
- Cobertura de `ActionType.READ` via Spring AOP sobre `@Auditable`
- Retenção/expurgo configurável com tombstone preservando cadeia
- Métricas Micrometer da pipeline

**Non-Goals:**
- Cifra de PII na trilha (coberto em `add-privacy-masking-module` §5)
- Outbox transacional no datasource de negócio (crash de JVM = sistema fora de uso; aceito)
- Broker de mensagens externo (Kafka, RabbitMQ, SQS)
- Storage WORM (opcional para o consumidor; fora do escopo da biblioteca)
- Endpoint REST para consulta (biblioteca expõe use-case; app implementa o controller)

## Decisions

### D1 — Fila em memória (não outbox transacional)

`ConcurrentLinkedQueue<ScosAuditLog>` em vez de gravar em tabela outbox no mesmo TX do negócio.

**Alternativa**: outbox no datasource de negócio — zero perda mesmo em crash de JVM, mas requer que o consumidor configure um segundo datasource audit-aware e polui o schema de negócio.

**Escolha**: fila em memória. Crash de JVM implica sistema fora de uso; não há eventos novos. A durabilidade cobre falhas transientes do banco (retry + DLQ). Tradeoff aceito explicitamente pelo time.

### D2 — Drain do consumer: size OU tempo

`ScosAuditBatchConsumer` drena a fila quando `batch-size` for atingido **ou** `flush-interval-ms` expirar, o que vier primeiro. Implementado como loop em virtual thread dedicada com `poll(timeout)`.

**Por que não `@Scheduled`**: o scheduler tem overhead de agendamento e não reage imediatamente ao tamanho da fila. Loop com `poll` responde ao burst sem delay desnecessário.

### D3 — Hash encadeado calculado no consumer (não no listener)

O hash `H(payload + hash_N-1)` é calculado dentro do `ScosAuditBatchConsumer`, antes do `saveAll`. Por ser single-consumer, a ordem dos registros no lote é determinística — não há race condition na cadeia.

**Alternativa**: calcular no listener Hibernate (multi-thread) — requereria lock explícito por entidade, overhead elevado.

**Algoritmo**: SHA-256 (disponível no JDK, sem dependência extra). Input do hash: `entity + idEntity + actionType + entityNew + entityOld + executionDate + hash_anterior` serializado como string UTF-8.

**Cadeia por entidade, não global**: `H(N) = SHA256(payload_N + H(N-1))` onde `H(0) = "GENESIS"` para o primeiro registro de cada `(entity, idEntity)`. Isso permite verificação de integridade por entidade sem precisar varrer toda a tabela.

### D4 — DLQ como tabela no datasource audit

`SFA_AUDIT_DLQ` no mesmo datasource isolado do audit, com colunas: `id UUID`, `payload JSONB`, `error TEXT`, `retry_count INT`, `created_at TIMESTAMPTZ`.

**Alternativa**: tabela no datasource de negócio, ou broker externo. Tabela no negócio polui o schema; broker externo adiciona dependência de infraestrutura.

**Reprocessamento**: `@Scheduled` com batch limit (max 50 por execução) para evitar burst ao reprocessar DLQ grande.

### D5 — AOP para READ com `@Auditable`

`ScosAuditReadAspect` intercepta métodos anotados com `@Auditable` e emite `ActionType.READ` usando o `ScosAuditService` existente. A anotação `@Auditable` já existe em `utils`; o aspecto verifica se o retorno contém uma entidade auditável.

**Alternativa**: novo listener Hibernate para `PostLoadEvent` — disparado em todo carregamento, não só acessos a PII. Muito ruidoso.

**Registro manual para JPQL/bulk**: método `ScosAuditService.recordRead(String entity, String idEntity)` exposto na spec para que o consumidor chame explicitamente após `@Modifying`.

### D6 — Retenção com tombstone

O job `ScosAuditRetentionJob` (`@Scheduled`) deleta registros com `executionDate < now() - ttl-days` e insere um tombstone `ScosAuditLog` com `actionType = TOMBSTONE`, `entityNew = null`, `entityOld = null` e `hash = H("TOMBSTONE" + hash_ultimo_registro)`. Isso mantém a cadeia verificável mesmo após expurgo.

**Alternativa**: soft-delete com flag `deleted=true` — a tabela cresce indefinidamente; não atende LGPD Art. 16 (eliminação real).

### D7 — Métricas com Micrometer

`MeterRegistry` injetado no `ScosAuditBatchConsumer` via `ObjectProvider<MeterRegistry>` (opcional — se Micrometer não estiver no classpath, sem-op). Três métricas:
- `audit.queue.depth` (Gauge): tamanho atual da fila
- `audit.batch.size` (DistributionSummary): tamanho de cada lote persistido
- `audit.events.dlq` (Counter): eventos enviados à DLQ

## Risks / Trade-offs

- **Janela de perda em crash de JVM** → Eventos na `ConcurrentLinkedQueue` no momento do crash são perdidos. Mitigação: aceito — crash = sistema fora de uso. Para zero-loss real, o consumidor pode implementar outbox próprio.
- **Cadeia de hash começa em branco para registros pré-migração** → `HASH_CHAIN` é `nullable`; registros anteriores ficam sem hash. `verifyChain` só verifica a partir do primeiro registro com hash não-nulo. Documentar no CHANGELOG.
- **DLQ pode crescer se o banco de audit ficar fora por longo período** → `@Scheduled` de reprocessamento tem batch limit; monitorar via `audit.events.dlq`. Alertar o consumidor para configurar alerta nessa métrica.
- **Performance do `verifyChain` em entidades com histórico longo** → verificação percorre todos os registros da entidade em ordem. Para entidades com milhares de eventos, pode ser lento. Mitigação: índice em `(entity, id_entity, execution_date)`.
- **`@Auditable` para READ pode ser ruidoso** → Métodos chamados em loop emitem muitos eventos SELECT. Mitigação: documentar que `@Auditable(ActionType.READ)` deve ser usado apenas em pontos de acesso a PII, não em toda query.

## Migration Plan

1. Aplicar migration Liquibase: `ADD COLUMN HASH_CHAIN VARCHAR(64) NULL` em `SFA_LOG_AUDIT`; criar tabela `SFA_AUDIT_DLQ`.
2. Revogar `UPDATE` e `DELETE` no role da aplicação sobre `SFA_LOG_AUDIT` (append-only).
3. Atualizar `application.yml` do consumidor com `scos.audit.performance.*` e `scos.audit.durability.*` (defaults não-breaking se omitido).
4. **Rollback**: coluna `HASH_CHAIN` pode ser dropada sem perda de dados de negócio; tabela `SFA_AUDIT_DLQ` pode ser dropada se vazia; hash-chain e retention são opt-in por flag.

## Open Questions

- **Índices adicionais em `SFA_LOG_AUDIT`**: as novas queries (`findByEntityAndIdEntity`, `findByUser`, `findByExecutionDateBetween`) precisam de índices. Incluir na migration ou documentar como recomendação para o consumidor?
  → Incluir na migration como `CREATE INDEX IF NOT EXISTS` — biblioteca deve entregar performático por padrão.
