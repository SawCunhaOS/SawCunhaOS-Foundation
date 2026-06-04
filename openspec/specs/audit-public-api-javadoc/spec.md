# audit-public-api-javadoc Specification

## Purpose
TBD - created by archiving change javadoc-modulo-audit. Update Purpose after archive.
## Requirements
### Requirement: Interfaces de especificação possuem Javadoc completo
Cada interface pública do pacote `specification` (`ScosAuditService`, `ScosAuditQueryService`, `ScosAuditIntegrityService`) SHALL ter um comentário Javadoc de classe descrevendo o contrato, e cada método público SHALL ter Javadoc com `@param` para cada parâmetro e `@return` para métodos não-`void`. Todas as interfaces SHALL ter a tag `@since 1.2.0`.

#### Scenario: Hover em interface exibe descrição do contrato
- **WHEN** desenvolvedor posiciona cursor sobre `ScosAuditQueryService` no IDE
- **THEN** tooltip exibe descrição do propósito da interface e a tag `@since 1.2.0`

#### Scenario: Hover em método exibe parâmetros e retorno
- **WHEN** desenvolvedor posiciona cursor sobre `ScosAuditQueryService#findByEntity`
- **THEN** tooltip exibe descrição do método, descrição de cada `@param` e descrição do `@return`

#### Scenario: Métodos void não têm @return
- **WHEN** `ScosAuditService#saveAuditLog` é documentado
- **THEN** o Javadoc contém `@param` para cada parâmetro e não contém tag `@return`

### Requirement: Entidades de domínio possuem Javadoc de campo
Cada campo declarado nas entidades `ScosAuditLog` e `ScosAuditDlqLog` SHALL ter um comentário Javadoc de campo descrevendo a semântica do dado armazenado. Campos opcionais SHALL indicar a condição de `null` quando relevante.

#### Scenario: Campo entityOld documenta condição de null
- **WHEN** desenvolvedor lê o Javadoc do campo `entityOld` em `ScosAuditLog`
- **THEN** a documentação indica que o campo é `null` em operações INSERT

#### Scenario: Campo entityNew documenta condição de null
- **WHEN** desenvolvedor lê o Javadoc do campo `entityNew` em `ScosAuditLog`
- **THEN** a documentação indica que o campo é `null` em operações DELETE

#### Scenario: Campos de ScosAuditDlqLog têm Javadoc
- **WHEN** desenvolvedor abre `ScosAuditDlqLog` no IDE
- **THEN** todos os 6 campos (`id`, `payload`, `error`, `retryCount`, `version`, `createdAt`) exibem Javadoc no hover

### Requirement: Enum ActionType documenta cada valor
Cada constante do enum `ActionType` SHALL ter um comentário Javadoc explicando o contexto de geração — qual operação ou mecanismo produz aquele valor.

#### Scenario: Valor SELECT documenta origem via aspecto
- **WHEN** desenvolvedor lê o Javadoc de `ActionType.SELECT`
- **THEN** a documentação referencia `ScosAuditReadAspect` e/ou `ScosAuditService#recordRead` como mecanismos geradores

#### Scenario: Valor TOMBSTONE documenta propósito na hash-chain
- **WHEN** desenvolvedor lê o Javadoc de `ActionType.TOMBSTONE`
- **THEN** a documentação explica que é inserido pelo job de retenção para preservar a cadeia de hash ao deletar registros expirados

#### Scenario: Valores INSERT, UPDATE, DELETE documentam origem Hibernate
- **WHEN** desenvolvedor lê o Javadoc de `ActionType.INSERT`, `UPDATE` ou `DELETE`
- **THEN** a documentação referencia o listener Hibernate como mecanismo gerador

### Requirement: Classes de properties possuem Javadoc de campo
Cada campo declarado nas classes `ScosAuditPerformanceProperties`, `ScosAuditDurabilityProperties`, `ScosAuditRetentionProperties`, `ScosAuditImmutabilityProperties` e `ScosAuditLogProperties` SHALL ter Javadoc indicando o propósito do campo e seu valor default.

#### Scenario: Campo retryMax documenta valor default
- **WHEN** desenvolvedor lê o Javadoc de `ScosAuditDurabilityProperties#retryMax`
- **THEN** a documentação indica o valor default (`3`) e o comportamento ao atingir o limite (roteamento à DLQ)

#### Scenario: Campo ttlDays documenta obrigatoriedade condicional
- **WHEN** desenvolvedor lê o Javadoc de `ScosAuditRetentionProperties#ttlDays`
- **THEN** a documentação indica que é obrigatório quando `enabled = true`

#### Scenario: Campo hashChain documenta opção opt-in
- **WHEN** desenvolvedor lê o Javadoc de `ScosAuditImmutabilityProperties#hashChain`
- **THEN** a documentação indica que o recurso é opt-in (default `false`) e descreve o algoritmo usado (SHA-256)

### Requirement: Javadoc segue padrão Oracle/OpenJDK
Todo Javadoc adicionado SHALL seguir o padrão Oracle/OpenJDK: primeira frase é o resumo; parágrafos adicionais usam `<p>`; literais de código usam `{@code}`; referências a outros tipos usam `{@link}`; tags `@author` e `@version` são omitidas; tags `@throws` são omitidas.

#### Scenario: Javadoc de interface não contém @author
- **WHEN** código-fonte de qualquer interface do pacote `specification` é inspecionado
- **THEN** nenhum comentário Javadoc contém a tag `@author`

#### Scenario: Literais de property usam {@code}
- **WHEN** um Javadoc referencia uma property como `scos.audit.enabled`
- **THEN** o literal é envolvido em `{@code scos.audit.enabled}`

#### Scenario: Referências cruzadas entre tipos usam {@link}
- **WHEN** o Javadoc de `ActionType.SELECT` referencia `ScosAuditService`
- **THEN** a referência usa `{@link ScosAuditService#recordRead}` (não texto simples)

