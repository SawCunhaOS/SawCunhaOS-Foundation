## ADDED Requirements

### Requirement: Hash encadeado por entidade em ScosAuditLog
O `ScosAuditLog` SHALL incluir coluna `HASH_CHAIN VARCHAR(64)` calculada pelo `ScosAuditBatchConsumer` antes do `saveAll`. O hash SHALL ser `SHA-256(entity + idEntity + actionType + entityNew + entityOld + executionDate + hash_anterior)` onde `hash_anterior` é `"GENESIS"` para o primeiro registro de cada `(entity, idEntity)`.

#### Scenario: Primeiro registro de uma entidade recebe hash GENESIS
- **WHEN** o primeiro evento de `(entity="PESSOA", idEntity="4711")` é persistido
- **THEN** `HASH_CHAIN = SHA-256("PESSOA4711INSERT<entityNew><executionDate>GENESIS")`

#### Scenario: Registro subsequente encadeia no hash anterior
- **WHEN** um segundo evento de `(entity="PESSOA", idEntity="4711")` é persistido
- **THEN** `HASH_CHAIN = SHA-256(payload_atual + hash_do_registro_anterior)`

#### Scenario: Coluna HASH_CHAIN é nullable para registros pré-migração
- **WHEN** a migration é aplicada em banco com registros existentes
- **THEN** os registros antigos têm `HASH_CHAIN = NULL`; a cadeia inicia a partir do primeiro registro pós-migração

### Requirement: Verificação de integridade da cadeia
O `ScosAuditIntegrityService` SHALL expor `verifyChain(String entity, String idEntity): boolean` que recalcula e compara todos os hashes da cadeia para a entidade informada.

#### Scenario: Cadeia íntegra retorna true
- **WHEN** `auditIntegrityService.verifyChain("Pessoa", "4711")` é chamado e nenhum registro foi adulterado
- **THEN** retorna `true`

#### Scenario: Registro adulterado detectado
- **WHEN** um registro da trilha tem seu `entityNew` alterado diretamente no banco
- **THEN** `verifyChain` retorna `false`

#### Scenario: Registro removido da cadeia detectado
- **WHEN** um registro intermediário é deletado diretamente do banco (sem tombstone)
- **THEN** `verifyChain` retorna `false` ao detectar hash quebrado

#### Scenario: Cadeia sem registros com hash retorna true (sem histórico verificável)
- **WHEN** todos os registros de `(entity, idEntity)` têm `HASH_CHAIN = NULL` (pré-migração)
- **THEN** retorna `true` com aviso de log que cadeia não verificável

### Requirement: Hash-chain é opt-in por configuração
O hash encadeado SHALL ser ativado via `scos.audit.immutability.hash-chain: true`. Quando desativado, `HASH_CHAIN` é persistido como `NULL` e `verifyChain` lança `UnsupportedOperationException`.

#### Scenario: Hash desativado — campo NULL persiste sem erro
- **WHEN** `scos.audit.immutability.hash-chain: false`
- **THEN** `HASH_CHAIN` é `NULL` em todos os registros novos; pipeline funciona normalmente

#### Scenario: verifyChain com hash desativado
- **WHEN** `scos.audit.immutability.hash-chain: false` e `verifyChain` é chamado
- **THEN** lança `UnsupportedOperationException` com mensagem `"hash-chain not enabled"`

### Requirement: Política append-only documentada no README
O README do módulo `audit` SHALL documentar os grants SQL necessários para tornar `SFA_LOG_AUDIT` append-only, incluindo exemplo de `REVOKE UPDATE, DELETE ON SFA_LOG_AUDIT FROM <role>`.

#### Scenario: Consumidor aplica grants e tenta UPDATE
- **WHEN** os grants são aplicados conforme README e a aplicação tenta UPDATE em `SFA_LOG_AUDIT`
- **THEN** o banco rejeita a operação com erro de permissão
