## 1. README — Diagrama ASCII

- [x] 1.1 Inserir diagrama ASCII do fluxo de produção de eventos no README, após o parágrafo de introdução (Hibernate/AOP → Queue → BatchConsumer → SFA_LOG_AUDIT / SFA_AUDIT_DLQ), incluindo nota `v1.2.0`
- [x] 1.2 Inserir diagrama ASCII do fluxo de consulta e integridade no README, como continuação do bloco de diagrama (`ScosAuditQueryService` / `ScosAuditIntegrityService` → `SFA_LOG_AUDIT`)

## 2. README — Troubleshooting

- [x] 2.1 Adicionar seção **Troubleshooting** no README após a seção de Métricas, com caso: módulo não inicia (`scos.audit.enabled` ausente ou `false`)
- [x] 2.2 Adicionar caso: eventos não aparecem na trilha (entidade sem `@Auditable`)
- [x] 2.3 Adicionar caso: fila cheia (métrica `audit.queue.depth`, ação: aumentar `queue-capacity` ou reduzir `flush-interval-ms`)
- [x] 2.4 Adicionar caso: DLQ acumulando (métrica `audit.events.dlq`, ação: verificar banco, grants, `dlq-enabled`)

## 3. Skill — Seções novas (4–8)

- [x] 3.1 Adicionar seção `## 4. Auditar leituras de PII` na skill com exemplo de `@Auditable(action = AuditAction.READ)` e `auditService.recordRead()`
- [x] 3.2 Adicionar seção `## 5. Consultar a trilha` na skill com exemplos dos 4 métodos de `ScosAuditQueryService` e referência ao README
- [x] 3.3 Adicionar seção `## 6. Hash-chain (imutabilidade opt-in)` na skill com property `scos.audit.immutability.hash-chain: true` e exemplo de `verifyChain`
- [x] 3.4 Adicionar seção `## 7. Retenção automática` na skill com properties `enabled`, `ttl-days` (obrigatório) e `cron`
- [x] 3.5 Adicionar seção `## 8. Monitorar DLQ` na skill com métrica `audit.events.dlq`, grants SQL e property `dlq-enabled`

## 4. Skill — Cifra e reorganização

- [x] 4.1 Renumerar seção existente de cifra PII para `## 9. Cifra em repouso de PII (privacy)` (era seção 4)

## 5. Skill — Referência de properties e Pegadinhas

- [x] 5.1 Adicionar seção `## 10. Referência de properties` na skill com tabela completa de todos os grupos `scos.audit.*` (raiz, performance, durability, immutability, retention, liquibase) + nota sobre `spring.datasource.audit.*`
- [x] 5.2 Atualizar seção **Pegadinhas** da skill com: `dlq-enabled=false` descarta eventos silenciosamente
- [x] 5.3 Atualizar seção **Pegadinhas** da skill com: `retention.enabled=true` sem `ttl-days` lança `IllegalStateException` na inicialização
