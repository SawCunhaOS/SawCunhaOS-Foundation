## ADDED Requirements

### Requirement: README contém diagrama ASCII da arquitetura interna
O README do módulo `audit` SHALL conter um diagrama ASCII do fluxo completo de produção e consulta de eventos, posicionado logo após o parágrafo de introdução. O diagrama SHALL ser legível em terminal de 80 colunas e SHALL incluir nota de versão (`v1.2.0`) para rastreabilidade.

#### Scenario: Diagrama cobre caminho de produção de eventos
- **WHEN** leitor consulta o diagrama no README
- **THEN** o diagrama mostra o fluxo: entidade `@Auditable` / método `@Auditable(READ)` → listener Hibernate / aspecto → `ScosAuditServiceBean` (thread async) → `ScosAuditQueue` → `ScosAuditBatchConsumer` (scheduled) → `SFA_LOG_AUDIT` / `SFA_AUDIT_DLQ`

#### Scenario: Diagrama mostra desvio de fila cheia
- **WHEN** leitor consulta o diagrama no README
- **THEN** o diagrama indica explicitamente que quando a fila está cheia, o evento é roteado diretamente para a DLQ

#### Scenario: Diagrama cobre caminho de consulta e integridade
- **WHEN** leitor consulta o diagrama no README
- **THEN** o diagrama mostra `ScosAuditQueryService` e `ScosAuditIntegrityService` operando sobre `SFA_LOG_AUDIT`

#### Scenario: Diagrama referencia versão
- **WHEN** leitor consulta o diagrama no README
- **THEN** o diagrama contém referência à versão `v1.2.0` para indicar quando foi introduzido

### Requirement: README contém seção Troubleshooting com os 4 casos principais
O README SHALL conter uma seção **Troubleshooting** posicionada após a seção de Métricas, cobrindo os 4 casos que exigem ação do consumidor. Cada caso SHALL apresentar sintoma, causa e ação corretiva.

#### Scenario: Troubleshooting cobre módulo não inicia
- **WHEN** leitor consulta a seção Troubleshooting
- **THEN** há um caso descrevendo que `scos.audit.enabled=true` é obrigatório e que sua ausência impede os beans de subirem

#### Scenario: Troubleshooting cobre eventos não aparecem na trilha
- **WHEN** leitor consulta a seção Troubleshooting
- **THEN** há um caso descrevendo que a entidade precisa da anotação `@Auditable` e que sem ela nenhum evento é capturado

#### Scenario: Troubleshooting cobre fila cheia
- **WHEN** leitor consulta a seção Troubleshooting
- **THEN** há um caso descrevendo a métrica `audit.queue.depth`, o sintoma (eventos indo para DLQ sem falha de persistência), e a ação (aumentar `scos.audit.performance.queue-capacity` ou reduzir `flush-interval-ms`)

#### Scenario: Troubleshooting cobre DLQ acumulando
- **WHEN** leitor consulta a seção Troubleshooting
- **THEN** há um caso descrevendo a métrica `audit.events.dlq`, o sintoma, e as ações: verificar conectividade com o banco de auditoria, verificar grants, e habilitar `scos.audit.durability.dlq-enabled=true`
