# privacy-log-integration Specification

## Purpose
TBD - created by archiving change add-privacy-masking-module. Update Purpose after archive.
## Requirements
### Requirement: Masking do log da aplicação via converter Logback

O sistema SHALL prover um converter Logback `%mask` que aplica `maskText` sobre a
mensagem renderizada, capturando PII em texto livre (ex.: `log.info("cpf {}", cpf)`),
reutilizando a mesma fonte de regras do filtro HTTP e da cifra do audit.

#### Scenario: PII em texto livre é mascarada no log

- **WHEN** a aplicação emite `log.info("Pessoa criada cpf={}", "12345678901")` com pattern de CPF ativo
- **THEN** a linha registrada sai mascarada (ex.: `cpf=***`)

#### Scenario: Valores de MDC conhecidos são mascarados por chave

- **WHEN** um valor de MDC corresponde a uma chave de regra key-based
- **THEN** ele é mascarado via `maskStructured`

### Requirement: Registro do converter sem editar logback.xml do app

O sistema SHALL registrar a `conversionRule` `%mask` programaticamente no
`LoggerContext` durante o startup, sem exigir que o app altere seu `logback.xml`. Um
opt-out documentado MUST estar disponível.

#### Scenario: Converter ativo sem alteração do app

- **WHEN** o módulo é importado e o app não edita `logback.xml`
- **THEN** o converter `%mask` fica disponível para uso

#### Scenario: Opt-out documentado

- **WHEN** o app deseja desativar o registro programático
- **THEN** existe forma documentada de optar por não registrar o converter

### Requirement: Offload assíncrono do masking de log

O sistema SHALL permitir que o masking de log ocorra fora da thread de request via
`AsyncAppender` com fila limitada, sem descartar eventos `WARN`/`ERROR`.

#### Scenario: Masking fora da thread de request

- **WHEN** o `AsyncAppender` está configurado para o appender com `%mask`
- **THEN** o masking é executado na thread de logging, não na de request

#### Scenario: Níveis críticos não são descartados

- **WHEN** a fila do `AsyncAppender` está sob pressão
- **THEN** eventos `WARN`/`ERROR` não são descartados (`discardingThreshold=0`)

