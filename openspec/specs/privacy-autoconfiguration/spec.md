# privacy-autoconfiguration Specification

## Purpose
TBD - created by archiving change add-privacy-masking-module. Update Purpose after archive.
## Requirements
### Requirement: Carregamento automático ao importar o módulo

O sistema SHALL registrar seus beans (`MaskingEngine`, `DataMaskingService`,
componentes de sanitização, provedores de cripto) por auto-configuração Spring Boot
listada em `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`,
de modo que subam apenas por estar no classpath, sem exigir `@ComponentScan` do app.

#### Scenario: Beans sobem só por classpath

- **WHEN** o módulo `privacy` está no classpath e o app não declara `@ComponentScan` para o pacote
- **THEN** os beans de masking são criados pela auto-configuração

#### Scenario: Desabilitar via propriedade

- **WHEN** `scos.privacy.enabled=false`
- **THEN** a auto-configuração não registra os beans de masking

### Requirement: Beans sobrescrevíveis pelo consumidor

Todo bean público da auto-configuração MUST ser anotado com `@ConditionalOnMissingBean`,
permitindo que o app forneça sua própria implementação sem editar a foundation.

#### Scenario: App define o próprio bean

- **WHEN** o app declara seu próprio `MaskingEngine` (ou `ScosCryptoKeyProvider`)
- **THEN** o bean da foundation recua e o do app é usado

### Requirement: SPI de override opcional

O sistema SHALL aceitar implementações opcionais de `DataMaskingValues` fornecidas
pelo app (injetadas via `ObjectProvider`), somando suas regras ao YML segundo a
precedência builtins → YML → SPI, sem exigir que o app implemente a interface.

#### Scenario: Ausência de SPI usa só YML e builtins

- **WHEN** o app não fornece nenhum `DataMaskingValues`
- **THEN** o engine é construído apenas com builtins e YML, sem erro

#### Scenario: SPI presente é somada

- **WHEN** o app fornece um ou mais `DataMaskingValues`
- **THEN** suas regras são combinadas ao YML respeitando a precedência

### Requirement: Propriedades de configuração tipadas

O sistema SHALL expor `@ConfigurationProperties(prefix = "scos.privacy")` com, no
mínimo, `enabled`, `strict`, `max-payload-kb`, `masking.config-path`,
`masking.default-patterns` e `masking.builtins`, acompanhadas de metadata de IDE.
`scos.privacy.enabled` SHALL ter default `true`.

#### Scenario: Default do enabled

- **WHEN** nenhuma propriedade `scos.privacy.enabled` é definida
- **THEN** o módulo é considerado habilitado (default `true`)

#### Scenario: config-path externo é respeitado

- **WHEN** `scos.privacy.masking.config-path=/etc/scos/privacy-masking.yml` é definido
- **THEN** o loader resolve o YML a partir desse caminho externo

