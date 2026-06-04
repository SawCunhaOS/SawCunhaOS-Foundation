# audit-read-coverage Specification

## Purpose
TBD - created by archiving change audit-conformidade-lgpd. Update Purpose after archive.
## Requirements
### Requirement: @Auditable habilita captura de READ por classe (opt-in)
A anotação `@Auditable` em entidades SHALL aceitar o atributo `auditRead` (default `false`). Quando `@Auditable(auditRead = true)` for declarado na classe da entidade, o `ScosHibernateAuditListener` SHALL emitir `ActionType.SELECT` em cada `PostLoadEvent` (carregamento) dessa entidade. Quando `auditRead = false` (default), nenhum evento de leitura é emitido — comportamento atual preservado.

#### Scenario: Entidade com auditRead=true emite SELECT no carregamento
- **WHEN** uma entidade anotada com `@Auditable(auditRead = true)` é carregada via Hibernate (PostLoad)
- **THEN** um `ScosAuditLog` com `actionType = SELECT` é enfileirado para essa entidade

#### Scenario: Entidade com auditRead=false não emite SELECT
- **WHEN** uma entidade anotada apenas com `@Auditable` (auditRead default `false`) é carregada
- **THEN** nenhum evento SELECT é emitido pelo PostLoad

### Requirement: @Auditable suporta ActionType.READ via AOP em métodos
O `ScosAuditReadAspect` SHALL interceptar métodos anotados com `@Auditable` que declarem `AuditAction.READ` e emitir um evento `ScosAuditLog` com `actionType = SELECT` após a execução bem-sucedida do método. A anotação `@Auditable` existente em `scos-foundation-utils` SHALL ser estendida para aceitar o tipo de ação.

#### Scenario: Método anotado com READ emite evento SELECT
- **WHEN** um método anotado com `@Auditable(ActionType.READ)` é chamado e retorna sem exceção
- **THEN** um `ScosAuditLog` com `actionType = SELECT`, `entity`, `idEntity` e dados do ator é enfileirado na pipeline

#### Scenario: Método com READ lança exceção — evento NÃO é emitido
- **WHEN** um método anotado com `@Auditable(ActionType.READ)` lança exceção
- **THEN** nenhum evento SELECT é emitido (acesso não consumado)

#### Scenario: Método sem @Auditable não é interceptado
- **WHEN** um método de serviço é chamado e não possui a anotação `@Auditable`
- **THEN** nenhum evento de audit é emitido pelo aspecto

### Requirement: Extração de entity e idEntity via @Auditable
A anotação `@Auditable` SHALL permitir declarar `entity` e `idEntity` — ou o aspecto SHALL extrair o ID do retorno do método (se retornar uma entidade `@Auditable`).

#### Scenario: entity e idEntity declarados na anotação
- **WHEN** `@Auditable(action = ActionType.READ, entity = "Pessoa", idEntitySpEL = "#id")` e o método recebe `Long id`
- **THEN** o evento é emitido com `entity="PESSOA"` e `idEntity=id.toString()`

#### Scenario: idEntity extraído do retorno quando não declarado
- **WHEN** o método retorna objeto com `@Id` anotado e `idEntity` não está declarado na anotação
- **THEN** o aspecto usa reflexão para extrair o valor do `@Id` como `idEntity`

### Requirement: Registro manual para operações fora do Hibernate
O `ScosAuditService` SHALL expor `recordRead(String entity, String idEntity)` para que o consumidor registre manualmente acessos via JPQL/`@Modifying`/SQL bulk que escapam do listener Hibernate e do AOP.

#### Scenario: Registro manual enfileira evento SELECT
- **WHEN** `scosAuditService.recordRead("Contrato", "999")` é chamado diretamente
- **THEN** um `ScosAuditLog` com `actionType = SELECT`, `entity="CONTRATO"`, `idEntity="999"` e dados do ator é enfileirado

#### Scenario: Registro manual sem usuário autenticado usa "SYSTEM"
- **WHEN** `recordRead` é chamado fora de um contexto autenticado
- **THEN** `user` no log é definido como `"SYSTEM"`

