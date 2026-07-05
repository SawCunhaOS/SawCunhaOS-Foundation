## Context

`SawCunhaOS-Foundation` é uma biblioteca multi-módulo (Maven) para Spring Boot
4.0.x / Spring Framework 7 / Java 25. O masking de PII vive em `utils/lgpd`, é
consumido pelos filtros de log de `utils`, não auto-configura (ao contrário de
`audit`/`jdempotent`, que já trazem `@AutoConfiguration` + `AutoConfiguration.imports`)
e tem gargalos no hot path (`Pattern` recompilado, lookup O(n), reparse de JSON).

Esta change segue à risca o documento de ideia
`etc/doc/ideia/privacy-masking-alto-desempenho.md`. As decisões abaixo já foram
fechadas nesse documento (seção "Decisões fechadas", itens 1–12) e são reproduzidas
aqui com o seu racional.

Restrições de plataforma e padrão SCOS que condicionam o design:

- Java 25, Spring Boot 4, Maven multi-módulo; ordem de módulos no parent importa
  (build topológico).
- Padrão SCOS: cabeçalho de licença Apache 2.0 em todo arquivo, pacote
  `br.com.sawcunhaos.foundation.*`, auto-config por `AutoConfiguration.imports`,
  `@ConditionalOnMissingBean` em beans públicos, `@ConfigurationProperties("scos.*")`.
- Padrão de qualidade exigido pelo solicitante: **código documentado para leitura e
  manutenção futura** (Javadoc nas APIs públicas, nomes intencionais, comentário no
  porquê das decisões não óbvias) — tratado como Non-Goal de estilo obrigatório, não
  opcional.

## Goals / Non-Goals

**Goals**

- Extrair `scos-foundation-privacy` na camada base sem ciclo com `utils`.
- `MaskingEngine` imutável/stateless/singleton: 1 fonte → 3 consumidores (HTTP, log, audit).
- Regras lidas de YML (`privacy-masking.yml`), externo → classpath, sem banco.
- Núcleo portável: `MaskingEngine.fromYaml(...)` funciona **sem Spring**.
- Estratégias de máscara como enum fechado, com fail-safe no load.
- Builtins por país como dados embarcados, ligáveis por pack.
- Auto-config: carregar ao importar; tudo sobrescrevível.
- Cifra em repouso da PII no audit (reversível, chave versionada).
- Alto desempenho multithread verificável por JMH (gate de CI).
- Suíte de testes (unit, property, integração da lib, concorrência, JMH) como entrega.
- Todo código com Javadoc/documentação e aderente ao padrão SCOS.

**Non-Goals**

- Governança (base legal, consentimento, direitos do titular, ROPA, incidente, DPIA).
- Ajustes da trilha que não usam classes de `privacy` (consulta, imutabilidade,
  retenção, cobertura de leitura, durabilidade) — ver `audit-conformidade-lgpd.md`.
- Hot-reload do YML em runtime (load 1× no startup; sem `WatchService` nesta versão).
- Auto-config própria de `utils` para os filtros (melhoria adjacente, pode ficar fora).
- Re-cifra de histórico de auditoria existente.

## Decisions

### D1. Módulo `privacy` na camada base, sem dependência de `utils`

`privacy` usa Gson próprio (o masking só opera sobre `JsonElement`; adapters de data
desnecessários). Evita o ciclo `utils ↔ privacy`, pois os filtros de log de `utils`
consomem o masking. `audit` e `utils` passam a depender de `privacy`.
**Alternativa rejeitada:** deixar o masking em `utils` e `privacy` depender de
`utils` por `GsonUtils` → criaria ciclo.

### D2. Regras via YML dedicado, resolução externo → classpath

Fonte: `scos.privacy.masking.config-path` (ou env `SCOS_PRIVACY_MASKING_CONFIG`) →
fallback `privacy-masking.yml` no classpath → ausência → builtins + WARN.
Config como **dado**, não código: manutenção sem recompilar, sem banco.
**Alternativa rejeitada:** bind só via `application.yml`/`@ConfigurationProperties` →
amarra ao Spring, quebra uso fora do SCOS. **Alternativa rejeitada:** banco → exige
infra, contraria o requisito.

### D3. Núcleo portável (SnakeYAML, sem Spring)

`PrivacyConfigLoader` (SnakeYAML) → `PrivacyConfig` (POJO) → `MaskingEngine.build()`.
Factory estática `MaskingEngine.fromYaml(Path/InputStream)` roda em app Spring, Java
puro, batch, lambda. A pasta `spring/` é wrapper que só localiza o YML e expõe beans.
SnakeYAML declarado explícito no módulo (vem transitivo no Boot, mas o uso standalone
exige).

### D4. SPI vira override opcional; precedência builtins → YML → SPI

YML é a fonte principal; `DataMaskingValues` continua para regras programáticas que
**somam** ao YML (injetada via `ObjectProvider`, opcional). Tudo achatado 1× num
snapshot imutável no build. **Alternativa rejeitada:** remover a SPI → perde regra
dinâmica em runtime. **Alternativa rejeitada:** SPI e YML em pé de igualdade sem
ordem → conflito de precedência ambíguo.

### D5. Estratégias de máscara como enum fechado, fail-safe no load

`fixed` (default), `partial`, `email`, `hash` (HMAC, pseudonim.), `encrypt`
(reversível via `ScosFieldCipher`), `redact`. Validação no load: `strategy`
desconhecida / `keep-*` negativo / `mask-char` >1 char → erro no startup; `partial`
com `keep-first+keep-last >= len` → mascara tudo (nunca vaza valor curto);
`hash`/`encrypt` sem chave em `strict` → erro, senão degrada para `fixed` + WARN.
`log-patterns` aceitam só `fixed`/`partial`/`hash` (não `email`/`encrypt`/`redact`).

### D6. Builtins como dados embarcados por pack

`privacy-builtins/<pack>.yml` no classpath do módulo (`generic`/`br`/`us`/`eu`/`uk`/`in`).
Ligar país = novo arquivo, sem recompilar lógica. App liga packs e desliga itens.
`credit-card`/`cpf`/`cnpj` validam Luhn/DV após pré-screen para reduzir falso-positivo.

### D7. Auto-config carrega ao importar; tudo `@ConditionalOnMissingBean`

`ScosPrivacyAutoConfiguration` listada em
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
Converter Logback `%mask` registrado **programaticamente** no `LoggerContext` (via
listener no startup), sem o app editar `logback.xml`; opt-out documentado.
`@ConfigurationProperties("scos.privacy")` + metadata de IDE.

### D8. Desempenho do hot path

`Pattern` pré-compilado em `DataMask`; lookup key-based `Map<String,DataMask>` O(1);
texto livre via Aho-Corasick (literais, O(n)) + `Pattern[]` só após pré-screen casar;
fast-path retorna a mesma `String` quando não há gatilho (zero alocação); `Matcher`
por thread (`ThreadLocal`); walk JSON in-place; cap de tamanho configurável; proteção
ReDoS (rejeita backtracking catastrófico no build). Offload assíncrono: `AsyncAppender`
para log, `@Async ScosAuditLogAsyncExecutor` para cifra+persist do audit.

### D9. Cifra em repouso da PII no audit

`auditEncryptFields` cifrados antes do `toJson` (hook em
`ScosAuditServiceBean.createJsonObject()`) ou via `AttributeConverter` JPA. Chave via
SPI `ScosCryptoKeyProvider` (Jasypt default, plugável Vault/KMS), versionada por
registro (`keyId` no JSONB), histórico não re-cifrado, prefixo `enc:vN:`. Opt-in por
campo (`auditEncryptFields` vazio = comportamento atual).

### D10. Padrão de código e documentação (requisito do solicitante)

Todo arquivo: cabeçalho de licença Apache 2.0, pacote `br.com.sawcunhaos.foundation.privacy.*`.
APIs públicas com **Javadoc** (contrato, thread-safety, params, exemplo quando útil);
comentário explicando o **porquê** de decisões não óbvias (ex.: fast-path, fail-safe);
nomes intencionais. Beans públicos `@ConditionalOnMissingBean`. Critério de revisão:
um novo dev entende e mantém sem ler o motor inteiro.

## Risks / Trade-offs

- **Move de pacote `utils.lgpd` → `privacy`** quebra imports diretos → mitigado por
  fachadas `@Deprecated` no pacote antigo por 1 release, delegando ao novo.
- **Ordem de módulos no parent**: `privacy` deve vir antes de `utils` no build →
  ajustar `<modules>` do parent; risco de falha de build se esquecido.
- **Cifra na coluna JSONB** torna leitura direta no banco ilegível → opt-in por campo;
  documentar `enc:vN:`; combinar com controle de acesso à tabela.
- **Converter Logback programático** mexe em infra do app → respeitar `logback.xml`
  existente, documentar opt-out (`logback-privacy.xml` via `<include>`).
- **Builtins com falso-positivo** (regex amplos) → validação Luhn/DV + pré-screen;
  packs desligáveis por item.
- **Alvos de JMH dependem da máquina** → baseline versionado em `etc/perf/baseline.json`,
  gate por regressão relativa, não por número absoluto.
- **SnakeYAML explícito** adiciona dependência → necessária para o uso standalone; já
  é transitiva no Boot, sem conflito esperado.

## Migration Plan

1. Criar módulo `privacy` e mover `utils/lgpd` → `privacy` com fachadas `@Deprecated`
   no pacote antigo (1 release de transição).
2. Ajustar `pom.xml` parent (`<module>privacy</module>` antes de `utils`) e deps de
   `utils` e `audit` para `privacy`; declarar SnakeYAML explícito.
3. Entregar `MaskingEngine` + `PrivacyConfig` + `PrivacyConfigLoader` + `fromYaml`.
4. Auto-config + `.imports` + `ScosPrivacyProperties` + metadata; converter Logback.
5. Estratégias (enum + validação) e builtins por país (`privacy-builtins/<pack>.yml`).
6. Cifra do audit (`ScosFieldCipher`/`ScosCryptoKeyProvider`); opt-in por campo.
7. `HashUtils`: HMAC ou documentar como checksum.
8. Suíte de testes (unit, property, integração, concorrência) + JMH `src/jmh` + baseline.
9. CHANGELOG, `CLAUDE.md`/skills, README (YML + standalone).

Rollback: módulo é aditivo e a cifra é opt-in; reverter = remover dep de `privacy` em
`utils`/`audit` e restaurar `utils/lgpd` (mantido como fachada deprecated no período).

## Open Questions

- **% exato do gate de regressão JMH** (placeholder "X%" no doc) — definir com o time
  ao versionar o primeiro baseline.
- **Auto-config própria de `utils`** para os filtros entra neste PR ou depois? (doc
  marca como adjacente; default: depois).
- **Estratégia default por builtin** (ex.: `partial` keep-last N por documento) — ajustar
  por país conforme convenção local ao escrever cada `privacy-builtins/<pack>.yml`.
