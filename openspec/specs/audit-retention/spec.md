# audit-retention Specification

## Purpose
TBD - created by archiving change audit-conformidade-lgpd. Update Purpose after archive.
## Requirements
### Requirement: Job TTL/purge configurável
O `ScosAuditRetentionJob` SHALL executar via `@Scheduled` e deletar registros de `SFA_LOG_AUDIT` com `EXECUTION_DATE < NOW() - ttl-days`. A retenção SHALL ser opt-in via `scos.audit.retention.enabled: true`.

#### Scenario: Registros expirados são purgados
- **WHEN** o job executa e existem registros com `executionDate` anterior a `now() - ttl-days`
- **THEN** esses registros são deletados de `SFA_LOG_AUDIT`

#### Scenario: Registros dentro do TTL são preservados
- **WHEN** o job executa
- **THEN** registros com `executionDate >= now() - ttl-days` não são tocados

#### Scenario: Retenção desativada — job não executa
- **WHEN** `scos.audit.retention.enabled: false`
- **THEN** o `ScosAuditRetentionJob` não é instanciado e nenhum purge ocorre

### Requirement: Tombstone preserva integridade da cadeia de hash
Antes de deletar um registro, o `ScosAuditRetentionJob` SHALL inserir um tombstone: `ScosAuditLog` com `actionType = TOMBSTONE`, `entityOld = NULL`, `entityNew = NULL`, `HASH_CHAIN = SHA-256("TOMBSTONE" + hash_do_registro_a_ser_deletado)`.

#### Scenario: Tombstone inserido antes da deleção
- **WHEN** o job deleta um registro com `HASH_CHAIN` não-nulo
- **THEN** um tombstone é inserido com `actionType=TOMBSTONE` e hash encadeado correto; o registro original é então deletado

#### Scenario: verifyChain permanece verdadeiro após expurgo com tombstone
- **WHEN** registros expirados foram purgados com tombstones e `verifyChain` é chamado
- **THEN** retorna `true` — o tombstone mantém a cadeia verificável

#### Scenario: Registro sem HASH_CHAIN é deletado sem tombstone
- **WHEN** o registro a ser deletado tem `HASH_CHAIN = NULL` (pré-migração)
- **THEN** é deletado diretamente sem inserir tombstone

### Requirement: TTL configurável pelo consumidor
O consumidor SHALL definir o prazo de retenção via `scos.audit.retention.ttl-days`. Não há valor default — a property é obrigatória quando `retention.enabled: true`.

#### Scenario: ttl-days ausente com retention habilitado
- **WHEN** `scos.audit.retention.enabled: true` e `scos.audit.retention.ttl-days` não está definido
- **THEN** a aplicação falha no startup com `BeanCreationException` e mensagem `"scos.audit.retention.ttl-days is required when retention is enabled"`

#### Scenario: ttl-days configurado — job usa o valor
- **WHEN** `scos.audit.retention.ttl-days: 1825` (5 anos)
- **THEN** registros com mais de 1825 dias são candidatos ao purge

