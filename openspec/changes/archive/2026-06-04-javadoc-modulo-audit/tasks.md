## 1. Interfaces de especificação

- [x] 1.1 Adicionar Javadoc de classe e de método em `ScosAuditService` (interface + `saveAuditLog` + `recordRead`)
- [x] 1.2 Adicionar Javadoc de classe e de método em `ScosAuditQueryService` (interface + `findByEntity` + `findByUser` + `findByPeriod` + `findByXRequestId`)
- [x] 1.3 Adicionar Javadoc de classe e de método em `ScosAuditIntegrityService` (interface + `verifyChain`)

## 2. Entidades de domínio

- [x] 2.1 Adicionar Javadoc de campo em `ScosAuditLog` — todos os 11 campos (`id`, `originSystem`, `actionType`, `idEntity`, `entity`, `entityOld`, `entityNew`, `user`, `executionDate`, `ipAddress`, `xRequestId`, `hashChain`, `eventOrder`)
- [x] 2.2 Adicionar Javadoc de campo em `ScosAuditDlqLog` — todos os 6 campos (`id`, `payload`, `error`, `retryCount`, `version`, `createdAt`)

## 3. Enum ActionType

- [x] 3.1 Adicionar Javadoc em `ActionType.SELECT` referenciando `ScosAuditReadAspect` e `ScosAuditService#recordRead`
- [x] 3.2 Adicionar Javadoc em `ActionType.INSERT`, `UPDATE`, `DELETE` referenciando o listener Hibernate
- [x] 3.3 Adicionar Javadoc em `ActionType.TOMBSTONE` explicando o papel na preservação da hash-chain durante retenção

## 4. Classes de properties

- [x] 4.1 Adicionar Javadoc de campo em `ScosAuditPerformanceProperties` (`queueCapacity`, `batchSize`, `flushIntervalMs`) com defaults e unidades
- [x] 4.2 Adicionar Javadoc de campo em `ScosAuditDurabilityProperties` (`retryMax`, `dlqEnabled`) com defaults e comportamento ao limite
- [x] 4.3 Adicionar Javadoc de campo em `ScosAuditRetentionProperties` (`enabled`, `ttlDays`) — `ttlDays` SHALL indicar obrigatoriedade quando `enabled = true`
- [x] 4.4 Adicionar Javadoc de campo em `ScosAuditImmutabilityProperties` (`hashChain`) indicando opt-in e algoritmo SHA-256
- [x] 4.5 Adicionar Javadoc de campo em `ScosAuditLogProperties` (`system`, `enable`, `enableLiquibase`) com defaults reais
