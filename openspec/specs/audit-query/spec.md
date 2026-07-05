# audit-query Specification

## Purpose
TBD - created by archiving change audit-conformidade-lgpd. Update Purpose after archive.
## Requirements
### Requirement: Isolamento multi-tenant por sistema de origem
Toda consulta exposta pelo `ScosAuditQueryService` SHALL filtrar automaticamente por `originSystem = scos.audit.system` (`ScosAuditLogProperties.getSystem()`). Cada sistema consumidor SHALL ver **somente** os registros gravados sob o seu próprio `scos.audit.system`, mesmo compartilhando o datasource de auditoria.

#### Scenario: Consulta retorna apenas registros do próprio sistema
- **WHEN** existem registros com `originSystem="SCOS_AUDIT"` e `originSystem="OUTRO_SISTEMA"` para a mesma entidade/ID e `scos.audit.system=SCOS_AUDIT`
- **THEN** `findByEntity`/`findByUser`/`findByPeriod`/`findByXRequestId` retornam apenas os registros com `originSystem="SCOS_AUDIT"`

### Requirement: Consulta paginada da trilha por entidade
O `ScosAuditLogRepository` SHALL expor query paginada por `entity` + `idEntity` + `originSystem`. O use-case `ScosAuditQueryService` SHALL encapsular a chamada, aplicar o filtro de sistema e retornar `Page<ScosAuditLog>`.

#### Scenario: Consulta por entidade e ID retorna página com resultados
- **WHEN** `auditQueryService.findByEntity("Pessoa", "4711", PageRequest.of(0, 50))` é chamado
- **THEN** retorna `Page<ScosAuditLog>` contendo apenas registros com `entity="PESSOA"`, `idEntity="4711"` e `originSystem` do sistema corrente

#### Scenario: Consulta por entidade sem registros retorna página vazia
- **WHEN** não existem registros para a entidade/ID informados
- **THEN** retorna `Page.empty()` sem lançar exceção

### Requirement: Consulta paginada por usuário
O repositório SHALL expor query paginada por `user`; o use-case SHALL encapsular e retornar `Page<ScosAuditLog>`.

#### Scenario: Consulta por usuário retorna todos os eventos do ator
- **WHEN** `auditQueryService.findByUser("joao.silva", PageRequest.of(0, 20))` é chamado
- **THEN** retorna `Page<ScosAuditLog>` com todos os registros onde `user="joao.silva"`

### Requirement: Consulta paginada por período
O repositório SHALL expor query por `executionDate` entre `ini` e `fim` (`OffsetDateTime`); o use-case SHALL encapsular.

#### Scenario: Consulta por período retorna registros dentro do intervalo
- **WHEN** `auditQueryService.findByPeriod(ini, fim, pageable)` é chamado com intervalo válido
- **THEN** retorna apenas registros com `executionDate >= ini AND executionDate <= fim`

#### Scenario: Período inválido (ini > fim) lança exceção
- **WHEN** `ini` é posterior a `fim`
- **THEN** o use-case lança `IllegalArgumentException` com mensagem descritiva

### Requirement: Consulta por xRequestId
O repositório SHALL expor `findByXRequestId(String xRequestId)` retornando `Optional<ScosAuditLog>`; o use-case SHALL encapsular.

#### Scenario: xRequestId existente retorna o registro
- **WHEN** `auditQueryService.findByXRequestId("req-uuid-123")` é chamado e o registro existe
- **THEN** retorna `Optional` com o `ScosAuditLog` correspondente

#### Scenario: xRequestId inexistente retorna Optional vazio
- **WHEN** nenhum registro tem o `xRequestId` informado
- **THEN** retorna `Optional.empty()` sem lançar exceção

### Requirement: Índices de suporte às queries
A migration Liquibase SHALL criar os índices abaixo em `SFA_LOG_AUDIT` para garantir performance das queries:

| Índice | Colunas |
|---|---|
| `idx_audit_entity_id` | `(ENTITY, ID_ENTITY)` |
| `idx_audit_user` | `(LOGGED_USER)` |
| `idx_audit_date` | `(EXECUTION_DATE)` |
| `idx_audit_xrequest` | `(X_REQUEST_ID)` |

#### Scenario: Índices criados pela migration sem erro
- **WHEN** a migration Liquibase é executada em banco limpo ou existente
- **THEN** os índices são criados com `CREATE INDEX IF NOT EXISTS` sem falha

