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

**Veredito:** para a trilha sustentar prestação de contas (LGPD Art. 6 X / 37,
GDPR A5(2), PIPL A55), faltam **consulta**, **imutabilidade**, **retenção**,
**cobertura de leitura** e **durabilidade**. Hoje o `✅` da accountability é
**parcial** — a trilha existe, mas não é consultável nem à prova de adulteração.

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

- Retry com backoff na persistência.
- Fila de fallback (**DLQ/outbox**) quando a persistência falha.
- Métrica de "auditoria perdida" exposta (observabilidade).
- `@Async` mantém o request rápido, **mas com garantia de entrega** — substituir o
  atual `catch (Exception) { log }` por caminho de fallback.

### 5. Retenção / expurgo

- Job de TTL/purge configurável por política (LGPD Art. 15/16, GDPR A5(e)).
- Preservar o encadeamento de hash ao expurgar (marca de **tombstone** no lugar do
  registro removido, mantendo a cadeia verificável).

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
      dlq: true                 # fila de fallback se persistência falhar
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
3. **Durabilidade:** retry + DLQ/outbox; nunca `catch` silencioso.
4. **Retenção:** TTL configurável pela app (controlador define o prazo);
   tombstone preserva a cadeia ao expurgar.

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
| Durabilidade com retry + DLQ/outbox | ✅ Garante não-perda do evento |
| `@Async` mantém request rápido | ✅ Já existe; só falta garantia de entrega |
| Governança (ROPA, base legal, incidente) fora de escopo | ✅ Correto p/ uma biblioteca |

### Resumo: o que a implementação deve fazer além do código

1. Estender `ScosAuditLogRepository` + use-case de consulta paginado.
2. Adicionar coluna de hash encadeado (Liquibase) + serviço de verificação de cadeia.
3. Emitir `ActionType.READ` (aspecto) + ponto de registro manual para JPQL/bulk.
4. Substituir `catch` silencioso por retry + DLQ/outbox; expor métrica de perda.
5. Job de retenção/purge configurável com tombstone.
6. Documentar grants (negar UPDATE/DELETE) e flags `scos.audit.*` no CHANGELOG.

---

## Referências

- [LGPD — Lei 13.709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)
- [GDPR — Regulation (EU) 2016/679](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- [Spring Boot — Creating Your Own Auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)
- [NIST SP 800-92 — Guide to Computer Security Log Management](https://csrc.nist.gov/pubs/sp/800/92/final)
- [Documento irmão — Privacy e Masking de Alto Desempenho](./privacy-masking-alto-desempenho.md)
