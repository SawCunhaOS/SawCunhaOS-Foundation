## 1. Infraestrutura e Migração

- [x] 1.1 Criar migration Liquibase: `ADD COLUMN HASH_CHAIN VARCHAR(64) NULL` em `SFA_LOG_AUDIT`
- [x] 1.2 Criar migration Liquibase: nova tabela `SFA_AUDIT_DLQ` (id UUID, payload JSONB, error TEXT, retry_count INT, created_at TIMESTAMPTZ)
- [x] 1.3 Criar migration Liquibase: índices `idx_audit_entity_id`, `idx_audit_user`, `idx_audit_date`, `idx_audit_xrequest` em `SFA_LOG_AUDIT`
- [x] 1.4 Adicionar coluna `hashChain` em `ScosAuditLog` com mapeamento JPA (`@Column(name = "HASH_CHAIN")`)
- [x] 1.5 Criar entidade `ScosAuditDlqLog` + `ScosAuditDlqRepository` (JpaRepository)

## 2. Pipeline de Batching (audit-batch-pipeline)

- [x] 2.1 Criar `ScosAuditQueue`: wrapper de `ConcurrentLinkedQueue<ScosAuditLog>` com capacidade máxima configurável; expor `offer()` que roteia para DLQ se cheio
- [x] 2.2 Refatorar `ScosAuditServiceBean`: substituir chamada direta a `saveLog` por `scosAuditQueue.offer(log)` mantendo o `@Async`
- [x] 2.3 Criar `ScosAuditBatchConsumer`: virtual thread dedicada; loop `poll(flush-interval-ms)`; drain por `batch-size` OU timeout
- [x] 2.4 Implementar `saveAll(lote)` em `ScosAuditLogService` usando `@Transactional("ScosAuditLogTransactionManager")`; remover `saveAndFlush`
- [x] 2.5 Implementar retry com backoff exponencial no consumer (até `retry-max` tentativas) antes de enviar lote para DLQ
- [x] 2.6 Implementar `ScosAuditDlqJob`: `@Scheduled` que reprocessa `SFA_AUDIT_DLQ` em batches de max 50 por execução
- [x] 2.7 Adicionar `ScosAuditPerformanceProperties` e `ScosAuditDurabilityProperties` em `ScosAuditLogProperties`
- [x] 2.8 Registrar `ScosAuditBatchConsumer` e `ScosAuditQueue` na `ScosAuditConfiguration`

## 3. Imutabilidade e Hash-Chain (audit-immutability)

- [x] 3.1 Criar `ScosAuditHashService`: calcula SHA-256 do payload; busca último hash de `(entity, idEntity)` no repositório; retorna `"GENESIS"` se primeiro registro
- [x] 3.2 Integrar `ScosAuditHashService` no `ScosAuditBatchConsumer`: calcular hash antes de cada `saveAll` quando `hash-chain: true`
- [x] 3.3 Criar `ScosAuditImmutabilityProperties` com flag `hash-chain` (default `false`, opt-in)
- [x] 3.4 Criar `ScosAuditIntegrityService` + `ScosAuditIntegrityServiceBean`: método `verifyChain(entity, idEntity)` que recalcula e compara hashes
- [x] 3.5 `verifyChain` retorna `true` (sem log de erro) para entidades com todos os registros `HASH_CHAIN=NULL`; lança `UnsupportedOperationException` quando `hash-chain: false`

## 4. Consulta da Trilha (audit-query)

- [x] 4.1 Adicionar queries paginadas em `ScosAuditLogRepository`: `findByEntityAndIdEntity`, `findByUser`, `findByExecutionDateBetween`, `findByXRequestId`
- [x] 4.2 Criar especificação `ScosAuditQueryService` (interface) com assinaturas `findByEntity`, `findByUser`, `findByPeriod`, `findByXRequestId`
- [x] 4.3 Implementar `ScosAuditQueryServiceBean` delegando ao repositório; validar `ini <= fim` em `findByPeriod`

## 5. Cobertura de Leitura (audit-read-coverage)

- [x] 5.1 Estender anotação `@Auditable` em `scos-foundation-utils` para aceitar `ActionType action()` com default `INSERT`
- [x] 5.2 Criar `ScosAuditReadAspect`: `@Around` em métodos `@Auditable(action=READ)`; emitir evento após retorno sem exceção; extrair `entity`/`idEntity` via SpEL ou reflexão `@Id`
- [x] 5.3 Adicionar método `recordRead(String entity, String idEntity)` em `ScosAuditService` (especificação) e `ScosAuditServiceBean` (implementação)
- [x] 5.4 `recordRead` usa `"SYSTEM"` como user quando `ScosUserAuthentication` retorna null

## 6. Retenção e Expurgo (audit-retention)

- [x] 6.1 Criar `ScosAuditRetentionProperties` com `enabled` (default `false`) e `ttl-days` (obrigatório quando enabled=true; validar no `@PostConstruct`)
- [x] 6.2 Criar `ScosAuditRetentionJob`: `@Scheduled`; deleta registros expirados em batches; `@ConditionalOnProperty("scos.audit.retention.enabled", havingValue="true")`
- [x] 6.3 Antes de deletar registro com `HASH_CHAIN` não-nulo, inserir tombstone: `ScosAuditLog(actionType=TOMBSTONE, hash=SHA-256("TOMBSTONE"+hash_anterior))`
- [x] 6.4 Adicionar `ActionType.TOMBSTONE` ao enum `ActionType`

## 7. Observabilidade (audit-observability)

- [x] 7.1 Injetar `ObjectProvider<MeterRegistry>` no `ScosAuditBatchConsumer`
- [x] 7.2 Registrar `Gauge` `audit.queue.depth` apontando para `ScosAuditQueue.size()`
- [x] 7.3 Registrar `DistributionSummary` `audit.batch.size`; registrar observação a cada `saveAll`
- [x] 7.4 Registrar `Counter` `audit.events.dlq`; incrementar a cada evento enviado à DLQ

## 8. Testes de Integração (obrigatório)

- [x] 8.1 `AuditBatchPipelineIntegrationTest`: burst de ≥500 eventos via threads virtuais; verificar que todos persistem em `SFA_LOG_AUDIT`; medir que thread de negócio não bloqueou
- [x] 8.2 `AuditRetryIntegrationTest`: derrubar conexão do datasource audit durante persistência; verificar retry com backoff; verificar persistência após DB voltar
- [x] 8.3 `AuditDlqIntegrationTest`: configurar `retry-max=1` e manter DB fora; verificar INSERT em `SFA_AUDIT_DLQ`; restaurar DB; disparar job; verificar migração para `SFA_LOG_AUDIT`
- [x] 8.4 `AuditBackpressureIntegrationTest`: configurar `queue-capacity=10`; disparar 20 eventos; verificar que excedente vai para DLQ sem bloquear
- [x] 8.5 `AuditHashChainIntegrationTest`: sequência C/U/D; `verifyChain` retorna `true`; adulterar registro no DB; `verifyChain` retorna `false`; deletar registro intermediário; `verifyChain` retorna `false`
- [x] 8.6 `AuditQueryIntegrationTest`: inserir trilha variada; verificar queries paginadas por entidade/usuário/período/`xRequestId`; verificar `IllegalArgumentException` para período inválido
- [x] 8.7 `AuditReadCoverageIntegrationTest`: método anotado com `@Auditable(READ)` → verificar `ActionType.SELECT` na trilha; método sem anotação → sem evento; `recordRead` manual → evento SELECT
- [x] 8.8 `AuditRetentionIntegrationTest`: inserir registros com `executionDate` expirado; executar job; verificar tombstone; `verifyChain` retorna `true`; verificar que registros dentro do TTL foram preservados
- [x] 8.9 `AuditMetricsIntegrationTest`: burst de eventos; verificar `audit.queue.depth` > 0 durante burst; verificar `audit.batch.size` registra observações; verificar `audit.events.dlq` incrementa em cenário de falha

## 9. Documentação

- [x] 9.1 Atualizar README do módulo `audit`: configuração mínima (`application.yml`), uso de `@Auditable`, uso de `@Auditable(READ)`, consulta via `ScosAuditQueryService`, grants append-only, tuning `scos.audit.performance.*`
- [x] 9.2 Atualizar CHANGELOG com breaking changes: coluna `HASH_CHAIN` (migration obrigatória), grants a revogar, `ActionType.TOMBSTONE` adicionado ao enum
