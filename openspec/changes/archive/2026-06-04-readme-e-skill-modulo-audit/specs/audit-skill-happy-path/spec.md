## ADDED Requirements

### Requirement: Skill cobre auditoria de leituras de PII
A skill `scos-audit-config` SHALL conter uma seção (4) descrevendo como auditar leituras de dados sensíveis via `@Auditable(action = AuditAction.READ)` em métodos de serviço, e como usar `ScosAuditService#recordRead` para operações JPQL/bulk.

#### Scenario: Seção mostra exemplo de @Auditable em método de serviço
- **WHEN** Claude consulta a skill para configurar auditoria de leitura
- **THEN** a skill contém exemplo de método anotado com `@Auditable(action = AuditAction.READ, entity = "...", idEntitySpEL = "...")` e explica o SpEL para extrair o ID

#### Scenario: Seção mostra recordRead para queries bulk
- **WHEN** Claude consulta a skill para operações JPQL
- **THEN** a skill contém exemplo de `auditService.recordRead("SFA_PEDIDO", id.toString())` com injeção de `ScosAuditService`

### Requirement: Skill cobre consulta paginada da trilha
A skill `scos-audit-config` SHALL conter uma seção (5) com exemplos dos 4 métodos de `ScosAuditQueryService`: `findByEntity`, `findByUser`, `findByPeriod` e `findByXRequestId`.

#### Scenario: Seção mostra exemplo de cada método de query
- **WHEN** Claude consulta a skill para implementar busca na trilha
- **THEN** a skill contém exemplos com código Java de `findByEntity`, `findByUser`, `findByPeriod` e `findByXRequestId`, com `PageRequest` e `Sort`

#### Scenario: Seção referencia o README para detalhes
- **WHEN** Claude consulta a skill
- **THEN** a seção de query referencia `audit/README.md` para exemplos adicionais

### Requirement: Skill cobre hash-chain opt-in
A skill `scos-audit-config` SHALL conter uma seção (6) com a property de ativação e exemplo de uso de `ScosAuditIntegrityService#verifyChain`.

#### Scenario: Seção mostra property de ativação e exemplo de verificação
- **WHEN** Claude consulta a skill para configurar hash-chain
- **THEN** a skill contém `scos.audit.immutability.hash-chain: true` e exemplo Java de `integrityService.verifyChain("SFA_PEDIDO", id)` com interpretação do retorno `false`

### Requirement: Skill cobre retenção automática
A skill `scos-audit-config` SHALL conter uma seção (7) com as properties de retenção e a obrigatoriedade de `ttl-days` quando `enabled=true`.

#### Scenario: Seção mostra configuração mínima de retenção
- **WHEN** Claude consulta a skill para configurar retenção
- **THEN** a skill contém exemplo YAML com `enabled: true`, `ttl-days` obrigatório e `cron` padrão, e alerta que `ttl-days` é obrigatório quando `enabled=true`

### Requirement: Skill cobre monitoramento da DLQ
A skill `scos-audit-config` SHALL conter uma seção (8) com a métrica `audit.events.dlq`, os grants SQL recomendados e a property `dlq-enabled`.

#### Scenario: Seção mostra métrica e grants SQL
- **WHEN** Claude consulta a skill para configurar monitoramento da DLQ
- **THEN** a skill contém a métrica `audit.events.dlq` (Counter Micrometer), o SQL de grants `REVOKE UPDATE, DELETE` / `GRANT INSERT, SELECT` e a property `scos.audit.durability.dlq-enabled`

### Requirement: Skill contém tabela de referência completa de properties
A skill `scos-audit-config` SHALL conter uma seção (10) com tabela de referência de todas as properties `scos.audit.*` e `spring.datasource.audit.*`, incluindo: property, tipo, default e descrição. Os grupos cobertos SHALL ser: raiz (`enabled`, `system`), `performance`, `durability`, `immutability`, `retention` e `liquibase`.

#### Scenario: Tabela cobre todos os grupos de properties
- **WHEN** Claude consulta a tabela de properties na skill
- **THEN** a tabela contém todas as properties dos grupos: raiz, performance, durability, immutability, retention, liquibase, e referencia `spring.datasource.audit.*`

#### Scenario: Tabela inclui defaults e tipos
- **WHEN** Claude verifica um default de property na skill
- **THEN** cada linha da tabela contém tipo Java, valor default e descrição resumida

### Requirement: Skill expande Pegadinhas com casos de DLQ e retenção
A skill `scos-audit-config` SHALL ter a seção Pegadinhas atualizada com: DLQ desabilitado causa perda silenciosa de eventos, e `ttl-days` é obrigatório quando `retention.enabled=true`.

#### Scenario: Pegadinha cobre DLQ desabilitado
- **WHEN** Claude consulta as Pegadinhas da skill
- **THEN** há item descrevendo que `dlq-enabled=false` faz eventos serem descartados silenciosamente quando todas as tentativas falham

#### Scenario: Pegadinha cobre retenção sem ttl-days
- **WHEN** Claude consulta as Pegadinhas da skill
- **THEN** há item descrevendo que `retention.enabled=true` sem `ttl-days` lança `IllegalStateException` na inicialização
