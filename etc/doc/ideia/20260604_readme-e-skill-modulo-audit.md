# README com diagrama e skill happy-path do módulo audit

**Data**: 2026-06-04  
**Status**: 🔄 Em Análise  
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `readme-e-skill-modulo-audit`
- **Resumo em uma frase**: Aprimorar o README do módulo `audit` com diagrama ASCII da arquitetura interna e seção de troubleshooting, e expandir a skill `scos-audit-config` para cobrir o happy-path completo de configuração.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade (documentação externa de consumo do módulo)
- [x] README e skill são tratados juntos pois servem ao mesmo consumidor no mesmo momento de uso
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
O README atual cobre configuração, uso e tuning, mas não mostra a arquitetura interna — o consumidor não entende por que existe uma fila em memória, o que acontece quando ela está cheia, ou como o DLQ se comporta. A skill `scos-audit-config` cobre apenas `@Auditable` em entidades e cifra PII, mas não guia o Claude ao configurar funcionalidades que o consumidor frequentemente ativa: query service, hash-chain, retenção e DLQ.

### Objetivo
Após essa melhoria: (1) o README contém um diagrama ASCII do fluxo interno completo; (2) há uma seção de troubleshooting com os erros mais comuns; (3) a skill guia o Claude no happy-path completo — da dependência até as features ativas — com referência ao README para detalhes avançados.

### Fora de Escopo
- Javadoc no código — cobre [[javadoc-modulo-audit]].
- Criação de documentação de arquitetura (ADR) — não requerido.
- Mudanças no código-fonte do módulo.

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: README inclui diagrama ASCII do fluxo completo (Hibernate/AOP → Queue → BatchConsumer → DB/DLQ).
- [ ] **RF-02**: README inclui seção **Troubleshooting** com os casos: módulo não inicia, eventos não aparecem na trilha, fila cheia, DLQ com eventos acumulados.
- [ ] **RF-03**: Skill `scos-audit-config` atualizada para cobrir: dependência, enable + datasource, `@Auditable` em entidade, query service (`ScosAuditQueryService`), hash-chain, retenção, DLQ monitoring — em sequência de adoção natural.
- [ ] **RF-04**: Skill mantém o happy-path (não expande para todos os detalhes de tuning) e referencia o README para configuração avançada.
- [ ] **RF-05**: Skill inclui tabela de referência completa de todas as properties `scos.audit.*` com: namespace, property, tipo, default e descrição — cobrindo os grupos `performance`, `durability`, `retention`, `immutability`, `liquibase` e as raízes `enabled`/`system`.

### Não-Funcionais
- [ ] **RNF-01**: Diagrama ASCII legível em terminal de 80 colunas.
- [ ] **RNF-02**: Troubleshooting prioriza problemas que exigem ação do consumidor (não bugs internos do módulo).
- [ ] **RNF-03**: Skill segue o formato existente das outras skills SCOS (`scos-privacy-config`, `scos-security-config`).

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
audit/
└── README.md                                     — modificação (diagrama + troubleshooting)

etc/doc/skills/scos-audit-config/
└── SKILL.md                                      — modificação (happy-path expandido)
```

### Diagrama ASCII a incluir no README

```
Produção de eventos
───────────────────
 @Auditable entity          @Auditable(READ) method / auditService.recordRead()
        │                                  │
        ▼                                  ▼
ScosHibernateAuditListener         ScosAuditReadAspect
  (PostInsert/Update/Delete)
        │                                  │
        └─────────────────┬────────────────┘
                          ▼
                  ScosAuditServiceBean
                  (thread: ScosAuditLogAsyncExecutor)
                          │
                          ▼
                  ScosAuditQueue
                  (LinkedBlockingQueue — capacity: queue-capacity)
                          │  fila cheia → DLQ direto
                          ▼
                  ScosAuditBatchConsumer
                  (scheduled: flush-interval-ms)
                  ├── event-order monotônico
                  ├── truncar executionDate → micros
                  ├── [opt] hash-chain SHA-256 por (entity, idEntity)
                  └── persistWithRetry (retry-max tentativas, backoff exp.)
                          │
              ┌───────────┴──────────────┐
              ▼                          ▼
       SFA_LOG_AUDIT               SFA_AUDIT_DLQ
    (datasource audit)          (reprocessado por ScosAuditDlqJob)

Consulta / integridade
──────────────────────
 ScosAuditQueryService   ScosAuditIntegrityService
        │                        │
        ▼                        ▼
  SFA_LOG_AUDIT            verificação da
  (read-only queries)      hash-chain SHA-256
```

### Estrutura da skill expandida

```
## 1. Dependência
## 2. Ativação + datasource dedicado          ← já existe
## 3. Auditar uma entidade (@Auditable)        ← já existe
## 4. Auditar leituras de PII                  ← NOVO
## 5. Consultar a trilha (QueryService)        ← NOVO
## 6. Hash-chain (imutabilidade opt-in)        ← NOVO
## 7. Retenção automática                      ← NOVO
## 8. Monitorar DLQ                            ← NOVO
## 9. Cifra em repouso de PII (privacy)        ← já existe
## 10. Referência de properties               ← NOVO
## Pegadinhas                                  ← já existe, expandir
```

### Tabela de referência de properties (seção 10 da skill)

```
scos.audit.*
┌─────────────────────────────────────────┬─────────┬───────────┬──────────────────────────────┐
│ Property                                │ Tipo    │ Default   │ Descrição                    │
├─────────────────────────────────────────┼─────────┼───────────┼──────────────────────────────┤
│ scos.audit.enabled                      │ boolean │ false     │ Ativa o módulo               │
│ scos.audit.system                       │ String  │ SFA_AUDIT │ Nome do sistema na trilha    │
├─────────────────────────────────────────┼─────────┼───────────┼──────────────────────────────┤
│ scos.audit.performance.queue-capacity   │ int     │ 10000     │ Capacidade da fila memória   │
│ scos.audit.performance.batch-size       │ int     │ 100       │ Eventos por saveAll          │
│ scos.audit.performance.flush-interval-ms│ long    │ 500       │ Intervalo de flush (ms)      │
├─────────────────────────────────────────┼─────────┼───────────┼──────────────────────────────┤
│ scos.audit.durability.retry-max         │ int     │ 3         │ Tentativas antes da DLQ      │
│ scos.audit.durability.dlq-enabled       │ boolean │ true      │ Habilita a DLQ               │
│ scos.audit.durability.dlq-reprocess-    │ long    │ 60000     │ Intervalo reprocessamento DLQ│
│   interval-ms                           │         │           │                              │
├─────────────────────────────────────────┼─────────┼───────────┼──────────────────────────────┤
│ scos.audit.immutability.hash-chain      │ boolean │ false     │ Ativa hash-chain SHA-256     │
├─────────────────────────────────────────┼─────────┼───────────┼──────────────────────────────┤
│ scos.audit.retention.enabled            │ boolean │ false     │ Ativa retenção automática    │
│ scos.audit.retention.ttl-days           │ Integer │ —         │ TTL em dias (obrig. se on)   │
│ scos.audit.retention.cron               │ String  │ 0 0 2 * * │ Cron de execução             │
├─────────────────────────────────────────┼─────────┼───────────┼──────────────────────────────┤
│ scos.audit.liquibase.enabled            │ boolean │ true      │ Roda migrations do módulo    │
│ scos.audit.liquibase.change-log         │ String  │ (interno) │ Path do changelog            │
│ scos.audit.liquibase.default-schema     │ String  │ public    │ Schema alvo                  │
└─────────────────────────────────────────┴─────────┴───────────┴──────────────────────────────┘

spring.datasource.audit.*  — datasource isolado (mesmas chaves do DataSource padrão Spring Boot)
  url, username, password, driver-class-name, hikari.*
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Escopo skill | Happy-path + referência ao README | Skill completa com todos os detalhes | Skill completa ficaria maior que o README; duplicação de manutenção |
| Tabela de properties | Na skill (seção 10) | Apenas no README | Claude consulta a skill ao configurar; tabela rápida evita busca no README |
| Troubleshooting | Seção no README | Documento separado | Consumidor busca no README primeiro |
| Diagrama | ASCII no README | Diagrama PlantUML/Mermaid | ASCII funciona em qualquer viewer; sem dependência de renderer |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos Modificados
- `audit/README.md` — adicionar seção de diagrama (após intro) + seção Troubleshooting (após Métricas)
- `etc/doc/skills/scos-audit-config/SKILL.md` — expandir seções 4–8 + atualizar Pegadinhas

### Tarefas
- [ ] **T-01**: Adicionar diagrama ASCII no README (logo após o parágrafo de introdução)
- [ ] **T-02**: Adicionar seção **Troubleshooting** no README com os 4 casos: módulo não inicia, eventos não aparecem, fila cheia, DLQ acumulando
- [ ] **T-03**: Adicionar seção "Auditando leituras de PII" na skill (seção 4)
- [ ] **T-04**: Adicionar seção "Consultar a trilha" na skill (seção 5) com exemplos de `ScosAuditQueryService`
- [ ] **T-05**: Adicionar seção "Hash-chain" na skill (seção 6) com property + `ScosAuditIntegrityService`
- [ ] **T-06**: Adicionar seção "Retenção automática" na skill (seção 7) com property + cron
- [ ] **T-07**: Adicionar seção "Monitorar DLQ" na skill (seção 8) com métrica `audit.events.dlq` e grants SQL
- [ ] **T-08**: Atualizar seção Pegadinhas da skill com casos de DLQ desabilitado e retenção sem `ttl-days`
- [ ] **T-09**: Adicionar seção "Referência de properties" na skill (seção 10) com tabela completa de todos os grupos `scos.audit.*` + `spring.datasource.audit.*`

### Riscos e Edge Cases
1. Diagrama ASCII no README pode ficar desatualizado se a arquitetura interna mudar — adicionar nota "Diagrama referente a v1.2.0" para rastrear.
2. Skill expandida não deve substituir o README; garantir que cada seção nova da skill referencie `audit/README.md` para detalhes de tuning.

---

## 📎 Referências
- [[javadoc-modulo-audit]] — ideia complementar (Javadoc no código)
- `audit/README.md` — estado atual
- `etc/doc/skills/scos-audit-config/SKILL.md` — estado atual da skill

---
