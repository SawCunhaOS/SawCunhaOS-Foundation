# Ideia: Trilha de Auditoria — Completar o Processo de Accountability (LGPD)

## Contexto

O `SawCunhaOS-Foundation` é uma **biblioteca/framework**. A trilha de auditoria
(módulo `audit`) é a salvaguarda técnica que sustenta a **prestação de contas** —
LGPD Art. 6 X / Art. 37, GDPR A5(2), PIPL A55, PIPEDA 4.1. Hoje o módulo entrega
**captura + armazenamento**, mas **não o processo completo**.

Esta ideia trata os ajustes da trilha que **não dependem de classes do módulo
privacy**: consulta, imutabilidade/tamper-evidence, cobertura de leitura,
durabilidade e retenção. A **cifra em repouso da PII** da trilha (que usa
`MaskingEngine` / `ScosCryptoKeyProvider` / `auditEncryptFields`) vive no documento
irmão [`privacy-masking-alto-desempenho.md`](./privacy-masking-alto-desempenho.md)
§5, porque depende de classes do módulo privacy.

> **Regra de corte:** ajuste de audit que usa classe lgpd/privacy → doc privacy.
> Caso contrário → este doc.

---

## Diagnóstico: a trilha cobre todo o processo?

**Não.** O módulo `audit` entrega **captura + armazenamento**, não o **processo
completo** de auditoria/accountability. Mapeando as etapas:

| Etapa do processo | Provido? | Evidência / lacuna | Depende de privacy? |
|---|---|---|---|
| Captura de mudança (C/U/D) | ✅ | `ScosHibernateAuditListener` (INSERT/UPDATE/DELETE) | — |
| Identificação ator / IP / correlação | ✅ | `user`, `ipAddress`, `xRequestId`, `executionDate` | — |
| Persistência isolada | ✅ | datasource dedicado + Liquibase + `@Async` | — |
| Captura de **leitura/acesso** (READ) | ❌ | `ActionType.SELECT` existe no enum mas **nunca é emitido** — listener só C/U/D; acesso a PII não é logado | não |
| Captura **fora do Hibernate** (JPQL/SQL bulk) | ❌ | só eventos de entidade Hibernate | não |
| **Consulta / relatório** da trilha | ❌ | `ScosAuditLogRepository` só tem `findByActionType` (inútil p/ investigação) — sem query por entidade/usuário/período/`requestId`, sem use-case/endpoint | não |
| **Imutabilidade / tamper-evidence** | ❌ | tabela comum, editável/deletável; sem hash-chain/append-only/WORM | não |
| Proteção de **PII** na trilha | ❌ | `entityOld`/`entityNew` em JSONB cru | **sim → doc privacy §5** |
| **Retenção / expurgo** | ❌ | sem TTL/purge (LGPD Art. 15/16) | não |
| **Garantia de não-perda** | ⚠️ | `saveAuditLog` engole exceção e só loga; `@Async` fire-and-forget → perda silenciosa, sem retry/DLQ | não |
| **Desempenho de persistência** | ❌ | `saveAndFlush` individual por evento; sem batching → N round-trips sob burst; `ThreadPoolTaskScheduler` pool-30 não resolve gargalo de I/O | não |

**Veredito:** para a trilha sustentar prestação de contas (LGPD Art. 6 X / 37,
GDPR A5(2), PIPL A55), faltam **consulta**, **imutabilidade**, **retenção**,
**cobertura de leitura**, **durabilidade** e **desempenho de persistência**. Hoje
o `✅` da accountability é **parcial** — a trilha existe, mas não é consultável,
não está à prova de adulteração e não aguenta carga moderada-alta sem degradar.

### Estrutura atual do módulo `audit`

```
audit/
├─ ScosHibernateAuditListener      PostInsert/Update/Delete → captura C/U/D
├─ domain/entity/ScosAuditLog      id, originSystem, actionType, idEntity,
│                                  entity, entityOld(JSONB), entityNew(JSONB),
│                                  user, executionDate, ipAddress, xRequestId
├─ domain/entity/ActionType        SELECT, UPDATE, INSERT, DELETE  (SELECT nunca emitido)
├─ domain/repository/...Repository só findByActionType  (inútil p/ investigação)
├─ service/ScosAuditServiceBean    @Async; createJsonObject() (linha 150)
├─ service/ScosAuditLogService     @Transactional saveAndFlush
└─ configuration/                  @AutoConfiguration + AutoConfiguration.imports (já existe)
```

---

## Validação contra as leis

| Lei | Exigência de trilha/accountability | Status atual | Resta |
|---|---|---|---|
| LGPD (BR) | Art. 6 X prestação de contas · 37 ROPA · 15/16 retenção | ⚠️ Parcial | consulta, imutabilidade, retenção, READ, durabilidade |
| GDPR (UE) | A5(2) accountability · A5(e) limitação de armazenamento | ⚠️ Parcial | idem |
| CCPA/CPRA (CA) | accountability · §1798.105 deletion | ⚠️ Parcial | consulta + retenção |
| PIPL (CN) | A55 auditoria · A19 retenção | ⚠️ Parcial | consulta + imutabilidade + retenção |
| UK GDPR | A5(2) · A5(e) | ⚠️ Parcial | idem GDPR |
| DPDP (IN) | s8(5) registro · s8(7) retenção | ⚠️ Parcial | consulta + retenção |
| POPIA (ZA) | s17 documentação · s14 retenção | ⚠️ Parcial | consulta + retenção |
| PIPEDA (CA) | 4.1 accountability · 4.5 retenção | ⚠️ Parcial | consulta + retenção |

Legenda: ✅ atende · ⚠️ parcial/risco · ❌ ausente mas cabível a framework.

A **proteção de PII** na trilha (Art. 46 / A32) — cifra em repouso — é coberta no
doc [`privacy-masking-alto-desempenho.md`](./privacy-masking-alto-desempenho.md) §5.

---

## Proposta de Solução

A trilha hoje captura e armazena, mas não fecha o processo. Os ajustes abaixo
**não dependem** do módulo privacy.

### 1. Consulta da trilha

`ScosAuditLogRepository` ganha queries por `entity` + `idEntity`, `user`, faixa de
`executionDate` e `xRequestId`; expor um use-case de leitura **paginado**. Sem isso
a accountability não é demonstrável.

```java
Page<ScosAuditLog> findByEntityAndIdEntity(String entity, String idEntity, Pageable p);
Page<ScosAuditLog> findByUser(String user, Pageable p);
Page<ScosAuditLog> findByExecutionDateBetween(OffsetDateTime ini, OffsetDateTime fim, Pageable p);
Optional<ScosAuditLog> findByXRequestId(String xRequestId);
```

> Quando os campos JSONB estiverem cifrados (doc privacy §5), o use-case decifra
> **sob demanda** conforme permissão — o `ScosFieldCipher` vem do módulo privacy.

### 2. Imutabilidade / tamper-evidence

Tornar a trilha **append-only**:

- Negar UPDATE/DELETE na tabela (grant só de INSERT/SELECT ao papel da app).
- Encadear hash: `hash(registro N) = H(payload + hash N-1)` — detecta adulteração
  ou remoção de registro intermediário.
- Opção de **WORM** no storage onde disponível.

### 3. Cobertura de leitura e fora do Hibernate

- Adicionar emissão de `ActionType.READ` para acessos a PII (anotação/aspecto no
  método de serviço/repos) — hoje `SELECT` existe no enum mas nunca é emitido.
- Ponto de registro manual para mudanças via JPQL/SQL bulk / `@Modifying` que
  escapam do listener Hibernate.

### 4. Durabilidade (não perder evento em silêncio)

`saveAuditLog` não pode perder evento sem rastro:

- Retry com backoff na persistência (integrado ao `ScosAuditBatchConsumer` — §6).
- Tabela **DLQ** (`SFA_AUDIT_DLQ`) no mesmo datasource isolado do audit: batch que
  falha após retries persiste na DLQ; `@Scheduled` reprocessa a DLQ.
- Métrica de "auditoria perdida" exposta: `audit.events.dlq` (observabilidade).
- Substituir o atual `catch (Exception) { log }` por caminho de fallback → DLQ.

> **Tolerância a crash de JVM:** eventos na fila em memória no momento do crash
> são perdidos. Decisão aceita — se a JVM cai, o sistema não está em uso e não há
> eventos novos entrando. A durabilidade aqui cobre **falhas transientes do banco**,
> não crash de processo.

### 5. Retenção / expurgo

- Job de TTL/purge configurável por política (LGPD Art. 15/16, GDPR A5(e)).
- Preservar o encadeamento de hash ao expurgar (marca de **tombstone** no lugar do
  registro removido, mantendo a cadeia verificável).

### 6. Desempenho de persistência (batching)

`saveAndFlush` individual por evento = N round-trips sob carga. Substituir por
pipeline com fila em memória e consumer que drena em lote:

- Evento entra em `ConcurrentLinkedQueue<ScosAuditLog>` via `@Async` — não bloqueia
  o thread Hibernate nem o request de negócio.
- `ScosAuditBatchConsumer` (virtual thread dedicada) drena por **tamanho** (N=100)
  **ou** por **intervalo** (T=500ms), o que vier primeiro.
- `saveAll(lote)` substitui `saveAndFlush` individual → 1 round-trip por lote.
- Se fila atingir capacidade máxima (`queue-capacity`), evento vai direto para DLQ
  (backpressure sem bloquear o negócio).
- Métricas expostas: `audit.queue.depth`, `audit.batch.size`, `audit.events.dlq`.

```
Hibernate Event
    │ @Async (virtual thread — não bloqueia negócio)
    ▼
ConcurrentLinkedQueue<ScosAuditLog>  (capacidade: 10000)
    │
    ▼  ScosAuditBatchConsumer (virtual thread dedicada)
    │  drain: size=100 OU 500ms
    ▼
saveAll(lote) → audit DB             (1 round-trip para N eventos)
    │
    ├── ok
    └── falha → retry 3× backoff
                    └── falha → INSERT SFA_AUDIT_DLQ
                                @Scheduled reprocessa DLQ
```

**Ganho esperado (carga moderada-alta, ~100–1000 ev/s):**
- Hoje: 100 eventos = 100 round-trips DB
- Após: 100 eventos = 1 round-trip DB (batch_size=100)

### 7. Testes de integração (obrigatório)

Todos os novos comportamentos devem ser cobertos por testes de integração com banco
real (Testcontainers + PostgreSQL). Testes unitários isolados **não substituem** —
batching, retry, DLQ e hash-chain só são verificáveis com I/O real.

| Cenário | O que verificar |
|---|---|
| **Carga / batching** | Burst de ≥500 eventos simultâneos → `ScosAuditBatchConsumer` drena em lotes; thread de negócio não bloqueia; todos os eventos persistem |
| **Retry** | Derrubar conexão audit DB durante persistência → retry com backoff; evento persiste após DB voltar |
| **DLQ** | Exaurir `retry-max` (DB permanece fora) → INSERT em `SFA_AUDIT_DLQ`; `@Scheduled` reprocessa e move para `SFA_LOG_AUDIT` |
| **Backpressure** | Encher fila até `queue-capacity` → excedente vai para DLQ; thread de negócio não bloqueia |
| **Hash-chain** | Sequência C/U/D → encadeamento correto; adulterar registro diretamente no DB → `auditIntegrityService.verifyChain` retorna `false` |
| **Consulta paginada** | Inserir trilha variada → queries por entidade/usuário/período/`xRequestId` retornam correto com paginação |
| **Cobertura READ** | Método anotado com `@Auditable` → `ActionType.SELECT` emitido e persistido na trilha |
| **Retenção / tombstone** | Registros com `executionDate` expirado → job TTL purga; tombstone inserido; cadeia hash permanece verificável |
| **Métricas** | Burst de eventos → `audit.queue.depth` e `audit.batch.size` expostos com valores coerentes |

> Usar Testcontainers + `@ServiceConnection` (PostgreSQL) para o datasource de audit.
> Testes de carga devem usar threads virtuais / `CompletableFuture` para simular
> burst realista sem overhead de plataforma de load.

---

## Exemplo de uso

### Configuração (`application.yml`)

```yaml
scos:
  audit:
    enabled: true
    diff-only: false            # UPDATE grava só campos alterados quando true
    immutability:
      hash-chain: true          # encadeamento de hash p/ tamper-evidence
    retention:
      enabled: true
      ttl-days: 1825            # política do controlador (ex.: 5 anos)
    durability:
      retry-max: 3
      dlq-enabled: true         # tabela SFA_AUDIT_DLQ como fallback
    performance:
      queue-capacity: 10000     # buffer máx em memória (carga moderada-alta)
      batch-size: 100           # eventos por saveAll
      flush-interval-ms: 500    # drena mesmo se batch-size não atingido
```

### Consulta de trilha (investigação)

```java
// "quem alterou a Pessoa 4711 e quando?"
Page<ScosAuditLog> trilha =
    auditQueryService.byEntity("Pessoa", "4711", PageRequest.of(0, 50));
```

### Verificação de integridade (hash-chain)

```java
// detecta adulteração/remoção de registro intermediário
boolean integra = auditIntegrityService.verifyChain("Pessoa", "4711");
```

---

## Decisões fechadas

1. **Snapshot do audit:** mantém snapshot completo; minimiza por **cifra seletiva**
   (`auditEncryptFields` — doc privacy §5), não por supressão; flag
   `scos.audit.diff-only` para gravar só campos alterados em UPDATE.
2. **Imutabilidade:** append-only (grant INSERT/SELECT) + hash-chain;
   WORM opcional onde o storage suportar.
3. **Durabilidade:** retry + DLQ (tabela `SFA_AUDIT_DLQ`); nunca `catch` silencioso.
4. **Retenção:** TTL configurável pela app (controlador define o prazo);
   tombstone preserva a cadeia ao expurgar.
5. **Desempenho:** fila em memória (`ConcurrentLinkedQueue`) + `ScosAuditBatchConsumer`
   com drain por tamanho/tempo; `saveAll` substitui `saveAndFlush` individual;
   backpressure via `queue-capacity`; crash de JVM aceito como janela de perda
   (sistema fora de uso nesse cenário).

---

## Impacto e Riscos

### Breaking changes

- `ScosAuditLog` ganha coluna de **hash encadeado** → migração Liquibase; trilhas
  antigas não têm cadeia (início da cadeia a partir da migração).
- Negar UPDATE/DELETE na tabela exige ajuste de **grants** no banco do consumidor.
- `audit` passa a depender de `scos-foundation-privacy` **somente** para a cifra
  (doc privacy §5) — os ajustes deste doc não adicionam essa dependência.

### Mitigação

- Hash-chain e retenção são **opt-in** por flag (`hash-chain`, `retention.enabled`)
  → comportamento atual preservado se desligado.
- Migração Liquibase versionada; documentar grants necessários no README do módulo.
- DLQ/outbox como tabela auxiliar no mesmo datasource isolado do audit.

### Não é breaking para

- Apps que não ativam `hash-chain`/`retention` (trilha continua como hoje).
- Quem já consome só a captura C/U/D automática.

---

## Validação: conformidade com convenções SCOS e mercado

| Aspecto | Status |
|---|---|
| Trilha append-only + hash-chain (tamper-evidence) | ✅ Prática de mercado (audit log integrity) |
| Consulta paginada por entidade/usuário/período | ✅ Requisito p/ accountability demonstrável |
| Retenção/TTL com tombstone preservando cadeia | ✅ LGPD Art. 15/16 · GDPR A5(e) |
| Cobertura de leitura (READ) + fora do Hibernate | ✅ Fecha lacuna de captura |
| Durabilidade com retry + DLQ (`SFA_AUDIT_DLQ`) | ✅ Garante não-perda em falha transiente |
| `@Async` mantém request rápido | ✅ Já existe; só falta garantia de entrega |
| Batching (`saveAll` por lote) + fila em memória | ✅ Throughput 100–1000 ev/s sem degradar negócio |
| Métricas de fila (`audit.queue.depth`, `audit.events.dlq`) | ✅ Observabilidade da pipeline de auditoria |
| Testes de integração com banco real (Testcontainers) | ✅ **Obrigatório** — cobre carga, retry, DLQ, hash-chain, consulta, READ, retenção |
| Governança (ROPA, base legal, incidente) fora de escopo | ✅ Correto p/ uma biblioteca |

### Resumo: o que a implementação deve fazer além do código

1. Estender `ScosAuditLogRepository` + use-case de consulta paginado.
2. Adicionar coluna de hash encadeado (Liquibase) + serviço de verificação de cadeia.
3. Emitir `ActionType.READ` (aspecto) + ponto de registro manual para JPQL/bulk.
4. Substituir `catch` silencioso por retry + DLQ (`SFA_AUDIT_DLQ`); expor métrica de perda.
5. Job de retenção/purge configurável com tombstone.
6. Implementar `ScosAuditBatchConsumer`: fila `ConcurrentLinkedQueue` + drain por
   size/tempo + `saveAll` em lote + backpressure via `queue-capacity`; expor métricas
   `audit.queue.depth`, `audit.batch.size`, `audit.events.dlq`.
7. Documentar grants (negar UPDATE/DELETE), flags `scos.audit.*` e tuning de
   performance no CHANGELOG.
8. **Atualizar README do módulo `audit`** com guia completo de uso em projeto:
   configuração mínima (`application.yml`), uso de `@Auditable`, consulta de trilha
   via use-case, grants necessários no banco e tuning de performance
   (`scos.audit.performance.*`).
9. **Testes de integração obrigatórios** (Testcontainers + PostgreSQL): carga/batching
   (≥500 eventos burst), retry, DLQ, backpressure, hash-chain (incluindo detecção de
   adulteração), consulta paginada, READ coverage, retenção/tombstone e métricas.

---

## Referências

- [LGPD — Lei 13.709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)
- [GDPR — Regulation (EU) 2016/679](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- [Spring Boot — Creating Your Own Auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)
- [NIST SP 800-92 — Guide to Computer Security Log Management](https://csrc.nist.gov/pubs/sp/800/92/final)
- [Documento irmão — Privacy e Masking de Alto Desempenho](./privacy-masking-alto-desempenho.md)
