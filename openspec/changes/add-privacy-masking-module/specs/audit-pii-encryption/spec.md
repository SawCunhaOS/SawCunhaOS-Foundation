## ADDED Requirements

### Requirement: Cifra em repouso reversível da PII na trilha

O sistema SHALL cifrar em repouso, de forma reversível, os campos declarados em
`auditEncryptFields` antes de serem persistidos nas colunas JSONB `entityOld`/
`entityNew` da trilha de auditoria, preservando o valor para investigação posterior.

#### Scenario: Campos declarados são cifrados na persistência

- **WHEN** `auditEncryptFields` inclui `cpf` e um evento de auditoria contém `cpf`
- **THEN** o valor de `cpf` é gravado cifrado com prefixo `enc:vN:`

#### Scenario: Decrypt recupera o valor original

- **WHEN** um registro cifrado é lido com a chave correta
- **THEN** o valor original é recuperado (round-trip reversível)

#### Scenario: Opt-in por campo preserva comportamento atual

- **WHEN** `auditEncryptFields` está vazio
- **THEN** a trilha grava os valores como hoje (sem cifra), sem quebrar trilhas existentes

### Requirement: Gestão de chave plugável e versionada

O sistema SHALL obter a chave de cifra via SPI `ScosCryptoKeyProvider` (implementação
default Jasypt com secret externalizado, plugável para Vault/KMS), gravando um `keyId`
por registro. A rotação de chave MUST gerar chave nova apenas para registros novos; o
histórico NÃO é re-cifrado e é decifrado pelo seu próprio `keyId`.

#### Scenario: keyId é gravado por registro

- **WHEN** um campo é cifrado
- **THEN** o `keyId` correspondente é persistido junto ao valor cifrado

#### Scenario: Histórico decifrado pelo keyId antigo após rotação

- **WHEN** a chave é rotacionada e um registro antigo é lido
- **THEN** ele é decifrado usando o `keyId` com que foi gravado

#### Scenario: Provider sobrescrevível pelo app

- **WHEN** o app fornece seu próprio `ScosCryptoKeyProvider` (ex.: Vault/KMS)
- **THEN** ele é usado no lugar do default Jasypt

### Requirement: Cifra fora da thread de request

O sistema SHALL executar a cifra e a persistência da trilha fora da thread de request,
no executor assíncrono de auditoria (`@Async`), para não impactar o tempo de resposta.

#### Scenario: Request não espera a cifra

- **WHEN** um commit JPA dispara o registro de auditoria
- **THEN** a cifra e a persistência ocorrem na thread assíncrona, não na de request
