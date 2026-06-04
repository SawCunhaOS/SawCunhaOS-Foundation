## Why

O módulo `audit` do `SawCunhaOS-Foundation` captura e persiste eventos C/U/D, mas não fecha o processo de accountability exigido pela LGPD Art. 6 X / 37, GDPR A5(2) e equivalentes: a trilha não é consultável, não está à prova de adulteração, não cobre leituras de PII, perde eventos em silêncio sob falha e degrada sob carga moderada-alta (~100–1000 ev/s) por usar `saveAndFlush` individual por evento.

## What Changes

- **Nova pipeline de persistência**: `ConcurrentLinkedQueue` + `ScosAuditBatchConsumer` com drain por size/tempo substitui `saveAndFlush` individual → 1 round-trip por lote de até 100 eventos
- **Durabilidade**: substituição do `catch` silencioso por retry com backoff + tabela DLQ `SFA_AUDIT_DLQ`; `@Scheduled` reprocessa a DLQ
- **Consulta da trilha**: novas queries paginadas em `ScosAuditLogRepository` (por entidade, usuário, período, `xRequestId`) + use-case `ScosAuditQueryService`
- **Imutabilidade / tamper-evidence**: coluna de hash encadeado em `ScosAuditLog` (Liquibase); `ScosAuditIntegrityService.verifyChain()`; grant append-only (INSERT/SELECT) na tabela
- **Cobertura de leitura**: `@Auditable` passa a suportar `ActionType.READ`; aspecto/AOP emite evento SELECT; ponto de registro manual para JPQL/bulk
- **Retenção / expurgo**: job TTL configurável (`scos.audit.retention.ttl-days`) + tombstone que preserva a cadeia de hash ao expurgar
- **Métricas**: `audit.queue.depth`, `audit.batch.size`, `audit.events.dlq` expostos via Micrometer
- **Testes de integração obrigatórios**: Testcontainers + PostgreSQL cobrindo carga, retry, DLQ, backpressure, hash-chain (incluindo detecção de adulteração), consulta paginada, READ, retenção/tombstone e métricas
- **README do módulo `audit`**: guia completo de uso em projeto (configuração mínima, `@Auditable`, consulta, grants, tuning de performance)
- **BREAKING**: `ScosAuditLog` ganha coluna `HASH_CHAIN` → migração Liquibase obrigatória; grants UPDATE/DELETE devem ser revogados no banco do consumidor

## Capabilities

### New Capabilities

- `audit-batch-pipeline`: Pipeline de persistência assíncrona com fila em memória, `ScosAuditBatchConsumer`, `saveAll` em lote, retry com backoff e DLQ (`SFA_AUDIT_DLQ`)
- `audit-query`: Queries paginadas da trilha por entidade/usuário/período/`xRequestId`; use-case `ScosAuditQueryService`
- `audit-immutability`: Hash encadeado por entidade em `ScosAuditLog`; `ScosAuditIntegrityService`; política append-only no datasource
- `audit-read-coverage`: Emissão de `ActionType.READ` via aspecto AOP em métodos anotados com `@Auditable`; ponto de registro manual para operações fora do Hibernate
- `audit-retention`: Job TTL/purge configurável com tombstone que preserva integridade da cadeia de hash
- `audit-observability`: Métricas Micrometer da pipeline (`audit.queue.depth`, `audit.batch.size`, `audit.events.dlq`)

### Modified Capabilities

<!-- Nenhuma capability existente de audit tem spec registrada — este change cria todas do zero -->

## Impact

**Módulo afetado**: `scos-foundation-audit`

**Classes modificadas**:
- `ScosAuditLog` — nova coluna `HASH_CHAIN`
- `ScosAuditServiceBean` — enfileira evento em vez de chamar `saveLog` diretamente
- `ScosAuditLogService` — adiciona `saveAll` em lote; remove `saveAndFlush`
- `ScosAuditLogRepository` — novas queries paginadas
- `ScosAuditLogProperties` — novos prefixos `scos.audit.performance.*`, `scos.audit.durability.*`, `scos.audit.immutability.*`, `scos.audit.retention.*`
- `ThreadsScosAuditConfiguration` — adiciona `ScosAuditBatchConsumer` como virtual thread dedicada

**Classes novas**:
- `ScosAuditBatchConsumer`, `ScosAuditQueryService` / `ScosAuditQueryServiceBean`, `ScosAuditIntegrityService` / `ScosAuditIntegrityServiceBean`, `ScosAuditRetentionJob`, `ScosAuditDlqLog` + `ScosAuditDlqRepository`, `ScosAuditReadAspect`

**Migrations Liquibase**: coluna `HASH_CHAIN` em `SFA_LOG_AUDIT`; nova tabela `SFA_AUDIT_DLQ`

**Dependências novas**: nenhuma (Micrometer já está no classpath do Spring Boot; Testcontainers apenas em `test`)

**Breaking para consumidores**: migração Liquibase obrigatória; grants UPDATE/DELETE a revogar
