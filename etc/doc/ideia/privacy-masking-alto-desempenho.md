# Ideia: Módulo Privacy e Masking de Alto Desempenho

## Contexto

O `SawCunhaOS-Foundation` é uma **biblioteca/framework**, não uma aplicação. Por
isso só pode entregar **salvaguardas técnicas** de proteção de dados (o
equivalente ao Art. 46 da LGPD — "medidas de segurança"). A **governança** de
privacidade — base legal, consentimento, atendimento aos direitos do titular,
registro de operações (ROPA), retenção, encarregado (DPO) — pertence à
**aplicação consumidora**, que conhece o contexto de tratamento.

Esta ideia trata a parcela de **masking / pseudonimização / cifra** frente a oito
leis — **LGPD (Brasil — foco)**, GDPR (UE), CCPA/CPRA (Califórnia), PIPL (China),
UK GDPR, DPDP Act (Índia), POPIA (África do Sul), PIPEDA (Canadá) — e propõe quatro
evoluções: extrair um módulo `scos-foundation-privacy`, dar a ele um motor de
masking de **alto desempenho multithread**, fazê-lo **carregar automaticamente** ao
ser importado, e tornar as regras (campos + máscaras) **configuráveis por um arquivo
YML** — sem banco, sem recompilar e **utilizável fora da camada SCOS** (o core lê o
YML sozinho, sem depender de Spring).

> **Documento irmão:** os ajustes da trilha de auditoria que **não** dependem de
> classes do módulo privacy (consulta, imutabilidade, cobertura de leitura,
> durabilidade, retenção) vivem em
> [`audit-conformidade-lgpd.md`](./audit-conformidade-lgpd.md). A **cifra em
> repouso da PII** da trilha fica **aqui** porque usa o motor de masking e a SPI
> de cripto deste módulo.

---

## Diagnóstico: O que está faltando

### Estrutura atual

```
LGPD-relevante no foundation
│
├─ utils/lgpd/            MASCARAMENTO (apenas em LOG)
│   ├─ DataMaskingService          substitui valor por newValue OU regex
│   ├─ SanitizationBodyComponent   percorre JSON do body recursivamente
│   ├─ SanitizationHeadersComponent
│   ├─ DataMaskingValues (SPI)     app declara chaves/regex a mascarar
│   ├─ DataMask (record: key, newValue, isRegex, regex)
│   └─ aplicado em LoggingInitialFilter / LoggingFinalFilter
│
├─ audit/                 TRILHA DE AUDITORIA (consome masking p/ cifra)
│   └─ ver audit-conformidade-lgpd.md
│
├─ security/              AUTHN/AUTHZ (Keycloak JWT, permissions, CORS)
│
└─ utils/
    ├─ valueobjects/      Cpf, Cnpj, Email, TaxIdentifier (dado pessoal tipado)
    └─ HashUtils          SHA-256 sem salt
```

### Gaps identificados

| Item | Status atual | Observação |
|---|---|---|
| Masking de PII na trilha de auditoria | ❌ Ausente | `entityOld`/`entityNew` gravam JSONB **cru** — CPF/e-mail em texto puro |
| Cifra em repouso de PII | ❌ Ausente | Só Bouncycastle p/ JWT; nada cifra campo sensível |
| Pseudonimização | ⚠️ Inadequada | `HashUtils` = SHA-256 **sem salt** → reversível por rainbow table |
| Masking no log da aplicação | ❌ Ausente | Masking só nos 2 filtros HTTP; `log.info("cpf {}", x)` vaza |
| Default seguro de masking | ⚠️ Opt-in silencioso | `DataMaskingValues` vazio → loga tudo sem aviso |
| Anonimização | ❌ Ausente | Sem utilitário de generalização/supressão |
| Desempenho do masking | ⚠️ Subótimo | `Pattern` recompilado por chamada, lookup O(n), reparse de JSON |
| Auto-configuração do masking | ❌ Ausente | `audit`/`jdempotent` já auto-configuram; `utils`/`lgpd` **não** — masking depende de `@ComponentScan` do app |

### Inconsistências adicionais

- **Masking acoplado a `utils`**: vive em `utils/lgpd`, mas os próprios filtros de
  log de `utils` o consomem — extração para módulo próprio gera ciclo se mal feita.
- **Três superfícies de PII, uma só protegida**: HTTP log (protegido), log da
  aplicação (não), trilha de auditoria (não) — sem fonte única de regras.
- **`value.replaceAll(regex, ...)`** em `DataMaskingService` recompila o `Pattern`
  a cada chamada — caminho quente sob carga.
- **Auto-config inconsistente**: `audit` e `jdempotent` já trazem
  `@AutoConfiguration` + `AutoConfiguration.imports`; `utils` (onde vive o masking)
  **não** — depende de `@ComponentScan` do app, ao contrário do resto.

---

## Validação contra as leis

### Mapeamento LGPD detalhado (artigo → código)

| Art. LGPD | Exigência | Status no foundation | Evidência / lacuna |
|---|---|---|---|
| Art. 6, III (minimização) | Limitar dados ao necessário | ⚠️ Parcial | Masking em log ok; audit grava entidade inteira |
| Art. 6, VII (segurança) | Medidas técnicas | ⚠️ Parcial | AuthN/Z + masking de log; falta cifra em repouso |
| Art. 13 (pseudonimização) | Pseudonimizar quando possível | ❌ | `HashUtils` SHA-256 sem salt — inadequado |
| Art. 12 (anonimização) | Dado anonimizado sai da LGPD | ❌ | Sem utilitário de anonimização |
| Art. 46 (segurança) | Proteger contra acesso/vazamento | ⚠️ Parcial | Falta cifra em repouso de PII |
| Art. 18 (direitos do titular) | Acesso, correção, exclusão, portabilidade | ⛔ Fora de escopo | Responsabilidade do app |
| Art. 7/8 (base legal / consentimento) | Registrar base legal | ⛔ Fora de escopo | Responsabilidade do app |

Legenda: ✅ atende · ⚠️ parcial/risco · ❌ ausente mas cabível a framework · ⛔ governança (fora do escopo).

> Os artigos de **prestação de contas / trilha** (Art. 6 X, Art. 37) e
> **retenção** (Art. 15/16) são tratados em
> [`audit-conformidade-lgpd.md`](./audit-conformidade-lgpd.md).

### Tabela comparativa — 8 leis (capacidades técnicas de masking/cripto)

"—" = a lei não tem exigência técnica direta equivalente.

| Capacidade técnica | Foundation | LGPD | GDPR | CCPA/CPRA | PIPL | UK GDPR | DPDP | POPIA | PIPEDA |
|---|---|---|---|---|---|---|---|---|---|
| Mascaramento em log | ✅ | Art.46 | A32 | §1798.81.5 | A51 | A32 | s8 | s19 | 4.7 |
| Controle de acesso | ✅ | Art.46 | A32 | §1798.81.5 | A51 | A32 | s8 | s19 | 4.7 |
| Pseudonimização | ❌ fraca | Art.13 | A4/A25 | — | — | A25 | — | — | — |
| Cifra em repouso (PII) | ❌ | Art.46 | A32 | §1798.150 | A51 | A32 | s8 | s19 | 4.7 |
| Anonimização | ❌ | Art.12 | rec.26 | §1798.140(a) | A4 | rec.26 | s3 | s6 | — |

### Veredito: a parcela de masking atende todas as leis?

**Resposta direta:** atende **integralmente a parcela técnica de masking/cripto**
exigida pelas 8 leis — *após concluir o backlog*. Nenhuma lei é satisfeita 100% só
por um framework: todas exigem **governança** (base legal, direitos do titular,
ROPA, incidente, DPIA), que pertence ao controlador/app. Conformidade plena =
**foundation (técnico) + app (governança)**.

| Lei | Obrigação técnica-chave (masking/cripto) | Coberta (pós-backlog) | Resta ao app |
|---|---|---|---|
| LGPD (BR) | Art. 46 segurança · 13 pseudonim. · 12 anonim. · 6 III minimização | ✅ | Art. 18 direitos · 7/8 base legal · 37 ROPA · 48 incidente |
| GDPR (UE) | A32 segurança · A25 privacy-by-design · A5 minimização | ✅ | A12-22 direitos · A30 ROPA · A33/34 incidente · A35 DPIA |
| CCPA/CPRA (CA) | §1798.81.5 safeguards · §1798.150 cifra razoável | ✅ | §1798.100+ direitos · opt-out de venda |
| PIPL (CN) | A51 segurança · A4 anonim. | ✅ | A44-47 direitos · A38-39 transferência |
| UK GDPR | A32 segurança · A25 · A5 | ✅ | A12-22 direitos · A30 ROPA · A33/34 incidente |
| DPDP (IN) | s8 segurança razoável | ✅ (técnico) | s11-14 direitos · consent manager |
| POPIA (ZA) | s19 segurança | ✅ | s23-25 direitos · Information Officer |
| PIPEDA (CA) | 4.7 safeguards | ✅ | 4.9 acesso · 4.1 accountability |

**Conclusão:** com o backlog (cifra em repouso, `MaskingEngine`, masking de log
híbrido, pseudonimização corrigida), o foundation entrega o **teto técnico** de
masking/cripto comum às 8 leis. O que falta para "estar em conformidade" é
processo/governança do app — por design, fora do escopo de uma biblioteca.

---

## Proposta de Solução

### 1. Extrair o módulo `scos-foundation-privacy`

Novo módulo na **camada mais baixa**, sem dependência de `utils`. Nome
`scos-foundation-privacy` (escopo multi-lei; a pasta atual `lgpd` é o foco).
Pacote `br.com.sawcunhaos.foundation.privacy`.

**Quebra de ciclo:** os filtros de log (`LoggingInitialFilter`/`LoggingFinalFilter`)
vivem em `utils` e usam o masking. Se `privacy` dependesse de `utils` (por
`GsonUtils`) → **ciclo `utils ↔ privacy`**. Logo `privacy` **não** depende de
`utils`: usa Gson próprio (o masking só opera sobre `JsonElement`; adapters de
data são desnecessários).

```
scos-foundation-privacy   (base: gson próprio, crypto, logback provided)
   ▲          ▲                 ▲
 utils      audit          converter Logback
(filtros)  (cifra JSONB)   (app — %mask)
```

`audit` passa a depender de `privacy` diretamente (módulo pequeno) em vez de puxar
`utils` inteiro só para mascarar/cifrar.

```
privacy/
├─ MaskingEngine            (core stateless: maskStructured + maskText)
│   └─ MaskingEngine.fromYaml(InputStream/Path)  ← factory puro-java, ZERO Spring
├─ config/PrivacyConfig     (POJO mapeado do YML: headers, body, logPatterns, auditEncryptFields)
├─ config/PrivacyConfigLoader   (SnakeYAML: resolve externo→classpath, valida, merge SPI)
├─ DataMaskingService       (fachada — compat)
├─ SanitizationBodyComponent / SanitizationHeadersComponent
├─ model/DataMask           (+ Pattern pré-compilado)
├─ specification/DataMaskingValues   (SPI OPCIONAL: override/adição programática ao YML)
├─ crypto/ScosCryptoKeyProvider (SPI) + impl Jasypt default
├─ crypto/ScosFieldCipher    (cifra seletiva enc:vN: — usada pelo audit)
├─ logback/ScosMaskingConverter      (%mask)
└─ spring/ScosPrivacyAutoConfiguration + ScosPrivacyProperties  (wrapper Spring, opcional)
```

> **Camadas:** o núcleo (`MaskingEngine` + `PrivacyConfigLoader` + SnakeYAML) **não
> depende de Spring** → roda em app Spring, Java puro, batch, lambda. A pasta
> `spring/` é só um wrapper que localiza o YML e expõe os beans; quem usa fora do
> SCOS chama `MaskingEngine.fromYaml(...)` direto.

### 2. `MaskingEngine` unificado — 1 fonte, 3 consumidores

Uma SPI alimenta os três pontos de exposição de PII (HTTP, log, audit):

```
                 DataMaskingValues (SPI) — estendida
                          │
        ┌─────────────────┼─────────────────────┐
        ▼                 ▼                       ▼
  Filtro HTTP        Logback converter      Audit (cifra)
  body/header        %mask(%msg) + MDC      coluna JSONB
  [existe]           [NOVO — híbrido]       [NOVO — cifra repouso]
        │                 │                       │
        └────────► MaskingEngine (core, stateless) ◄──┘
                  - maskStructured(key, value)   ← key-based
                  - maskText(message)            ← pattern-based
```

`MaskingEngine` é imutável e montado 1× no startup. `DataMaskingService` vira
fachada fina sobre ele. **A fonte principal das regras é o YML** (seção 2.1); a SPI
`DataMaskingValues` continua existindo, mas **opcional**, só para regras
programáticas/dinâmicas que **somam** ao YML:

```java
// OPCIONAL — só se o app precisar de regras em código além do YML
public interface DataMaskingValues {
    default Set<DataMask> headersValue()       { return Set.of(); }
    default Set<DataMask> bodyValue()          { return Set.of(); }
    default Set<DataMask> logPatterns()        { return Set.of(); } // regex texto livre
    default Set<String>   auditEncryptFields() { return Set.of(); } // campos a cifrar
}
```

Precedência no build do engine: **builtins** (se ligados) → **YML** → **SPI**
(override programático). Tudo achatado 1× num snapshot imutável.

### 2.1 Configuração via YML — fonte única, portável (requisito)

As regras (campos + máscaras) são lidas de um **arquivo YML dedicado**, não de
banco e não de código. Objetivo: manutenção por texto versionável e uso **fora da
camada SCOS** (sem Spring).

**Resolução da fonte (precedência):**

```
1. caminho externo configurável   (ops edita sem rebuild)
      scos.privacy.masking.config-path=/etc/scos/privacy-masking.yml
      ou env SCOS_PRIVACY_MASKING_CONFIG
2. fallback: classpath            privacy-masking.yml  (embarcado no jar)
3. ausência das duas              → builtins (se ligados) + WARN no startup
```

**Schema do `privacy-masking.yml`:**

```yaml
scos:
  privacy:
    masking:
      headers:                      # key-based, O(1)
        - key: authorization
          value: "***"
      body:                         # key-based, JSON
        - key: cpf
          value: "***"
        - key: email
          strategy: partial         # ex.: a***@***.com
      log-patterns:                 # text-based (Aho-Corasick p/ literal, Pattern p/ regex)
        - literal: "secret"
          value: "***"
        - regex: '\d{11}'
          value: "***"
      audit-encrypt-fields:         # campos cifrados na trilha (doc privacy §5)
        - cpf
        - email
```

Um arquivo alimenta as **4 superfícies** (header / body / log / audit).

**Loader portável (puro-Java, zero Spring):**

```java
// fora do SCOS / app não-Spring:
MaskingEngine engine = MaskingEngine.fromYaml(Path.of("/etc/scos/privacy-masking.yml"));
String safe = engine.maskText("cpf=12345678901");

// PrivacyConfigLoader usa SnakeYAML → PrivacyConfig (POJO) → MaskingEngine.build(...)
```

- Dependência nova: **SnakeYAML** (já vem transitivo no Spring Boot; declarar
  explícito no módulo `privacy` para o uso standalone).
- **Validação no load:** chaves desconhecidas, regex ReDoS-perigoso e `strategy`
  inválida → falha no startup, não em produção (alinha com §4).
- **Load 1× no startup** → snapshot imutável; mudança no YML exige restart
  (decisão fechada). Sem `WatchService` nesta versão.

#### 2.1.1 Estratégias de máscara (`strategy`)

Cada regra key-based (`headers`/`body`) tem uma `strategy`. Enum **fechado** —
valor desconhecido falha no startup. `fixed` é o default quando só há `value`.

| `strategy` | Reversível | Params | Exemplo (`12345678901`) |
|---|---|---|---|
| `fixed` | não | `value` (default `"***"`) | `***` |
| `partial` | não | `keep-first`, `keep-last`, `mask-char` (`*`), `preserve-length` (`true`) | `123******01` |
| `email` | não | `mask-local` (`true`), `mask-domain` (`true`) | `a***@***.com` |
| `hash` | não (pseudonim.) | `algo` (HMAC-SHA256), chave via `ScosCryptoKeyProvider` | `h:7f3a…` (estável p/ correlação) |
| `encrypt` | sim | usa `ScosFieldCipher` (doc §5) | `enc:v3:9af1…` |
| `redact` | não | — | campo removido do JSON/saída |

```yaml
body:
  - key: cpf
    strategy: partial
    keep-first: 3
    keep-last: 2
    mask-char: "*"
    preserve-length: true        # false → "123**01" (comprimento fixo, não vaza tamanho)
  - key: email
    strategy: email              # mask-local/mask-domain default true
  - key: password
    strategy: fixed              # value default "***"
  - key: clientId
    strategy: hash               # pseudonimização estável (mesmo input → mesmo hash)
  - key: internalToken
    strategy: redact             # some da saída
```

**Regras de validação / fail-safe (no load):**

- `partial` com `keep-first + keep-last >= len(value)` → **mascara tudo** (nunca
  revela valor curto). `keep-*` ausentes default `0`.
- `hash`/`encrypt` exigem `ScosCryptoKeyProvider` configurado; sem chave em modo
  `strict` → erro no startup, senão degrada para `fixed` + WARN.
- `strategy` inválida, `keep-*` negativo, `mask-char` com >1 char → erro no startup.
- `log-patterns` aceitam `strategy` `fixed`/`partial`/`hash` sobre o **grupo casado**
  (não `email`/`encrypt`/`redact`, que são key-based).

#### 2.1.2 Builtins por país / região (packs)

Escopo multi-lei → os patterns embutidos vêm em **packs nomeados**, ativados por
lista (`scos.privacy.masking.builtins`). Cada item de pack tem `strategy` default
sensata; o app pode **desligar** itens específicos.

| Pack | Patterns (strategy default) |
|---|---|
| `generic` | `email` (email), `credit-card` (partial keep-last 4, valida Luhn), `ipv4`/`ipv6` (fixed), `jwt`/`bearer` (fixed) |
| `br` | `cpf`, `cnpj`, `cep`, `tel-br`, `rg`, `cnh`, `pis`, `titulo-eleitor` (partial) |
| `us` | `ssn`, `ein`, `zip`, `phone-us` (partial) |
| `eu` | `iban`, `vat` (partial keep-last 4) |
| `uk` | `nino`, `nhs` (partial) |
| `in` | `aadhaar`, `pan` (partial) |

```yaml
scos:
  privacy:
    masking:
      builtins:
        enabled: [generic, br]          # liga packs
        disabled: [br.titulo-eleitor]   # desliga item pontual
```

- Packs são **dados embarcados** (`privacy-builtins/<pack>.yml` no classpath do
  módulo), não código — adicionar país = novo arquivo, sem recompilar lógica.
- Builtins entram **antes** do YML do app na precedência (builtins → YML → SPI):
  o app sobrepõe qualquer builtin por chave.
- `credit-card`/`cpf`/`cnpj` validam dígito (Luhn / DV) **após** o pré-screen → menos
  falso-positivo no hot path.

### 3. Auto-configuração — carregar ao importar (requisito)

**O padrão já existe na foundation** — `audit` e `jdempotent` trazem
`@AutoConfiguration` + `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
(ex.: `audit/.../ScosAuditConfiguration` anotada `@AutoConfiguration` e listada no
`.imports`). **A exceção é `utils`** (onde vive o masking): não tem `.imports`, então
hoje os beans de masking só sobem se o app escanear `br.com.sawcunhaos.foundation`.

O módulo `privacy` deve **seguir o mesmo padrão de `audit`/`jdempotent`**: subir os
beans **só por estar no classpath**, sem `@ComponentScan` no consumidor.

Mecanismo (Spring Boot 4): classe `@AutoConfiguration` declarada no arquivo de
imports, descoberta automaticamente.

```java
@AutoConfiguration
@EnableConfigurationProperties(ScosPrivacyProperties.class)
@ConditionalOnProperty(prefix = "scos.privacy", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
public class ScosPrivacyAutoConfiguration {

    @Bean @ConditionalOnMissingBean
    PrivacyConfig privacyConfig(ScosPrivacyProperties props) {
        // resolve externo → classpath → builtins; valida; SnakeYAML
        return PrivacyConfigLoader.load(props.getMasking().getConfigPath());
    }

    @Bean @ConditionalOnMissingBean
    MaskingEngine maskingEngine(PrivacyConfig yml,
                                ObjectProvider<DataMaskingValues> spiOverride,  // opcional
                                ScosPrivacyProperties props) {
        // precedência: builtins → YML → SPI; snapshot imutável 1×
        return MaskingEngine.build(yml, spiOverride.stream().toList(), props);
    }

    @Bean @ConditionalOnMissingBean
    DataMaskingService dataMaskingService(MaskingEngine engine) {
        return new DataMaskingService(engine);
    }

    @Bean @ConditionalOnMissingBean SanitizationBodyComponent sanitizationBody(...) { ... }
    @Bean @ConditionalOnMissingBean SanitizationHeadersComponent sanitizationHeaders(...) { ... }
    @Bean @ConditionalOnMissingBean ScosCryptoKeyProvider cryptoKeyProvider(...) { ... } // Jasypt default
    @Bean @ConditionalOnMissingBean ScosFieldCipher fieldCipher(ScosCryptoKeyProvider kp) { ... }
}
```

```
src/main/resources/META-INF/spring/
└─ org.springframework.boot.autoconfigure.AutoConfiguration.imports
     br.com.sawcunhaos.foundation.privacy.config.ScosPrivacyAutoConfiguration
```

Pontos de design:

- **`@ConditionalOnMissingBean` em tudo** → o app sobrescreve qualquer peça (sua
  própria `DataMaskingValues`, um `ScosCryptoKeyProvider` Vault/KMS) sem tocar na
  foundation.
- **`@ConfigurationProperties("scos.privacy")`** + `additional-spring-configuration-metadata.json`
  para autocomplete no IDE. `scos.privacy.enabled` default `true`.
- **Converter Logback sem editar `logback.xml` do app**: registrar a
  `conversionRule` `%mask` **programaticamente** no `LoggerContext` via um listener
  no startup (`ch.qos.logback.classic.LoggerContext#putObject`/`LoggerContextListener`
  acionado pela auto-config). Assim o masking de log também "carrega ao importar".
  Alternativa documentada: incluir `logback-privacy.xml` via `<include>`.
- **Pré-requisito p/ os filtros de `utils`**: como `utils` passa a depender de
  `privacy`, os beans de masking vêm do `privacy` (auto-configurado). `utils`
  continua sem `.imports` próprio neste escopo; criar a auto-config de `utils` para
  os filtros é melhoria adjacente (fecha a única exceção ao padrão já usado em
  `audit`/`jdempotent`), mas pode ficar fora deste PR.

### 4. Performance e concorrência (alto desempenho multithread)

Meta: masking de alto desempenho sob carga multithread, tempo de resposta baixo,
zero contenção. O motor é caminho quente (todo request, toda linha de log) →
otimização é requisito, não detalhe.

**Correções dos gargalos atuais:**

| Gargalo atual | Correção | Ganho |
|---|---|---|
| `value.replaceAll(regex)` compila Pattern por chamada | Pré-compilar `Pattern` no startup, guardar em `DataMask` | elimina compile no hot path |
| `bodyValue().stream().filter().findFirst()` O(n)/campo | `Map<String,DataMask>` (chave lower) montado 1× → O(1) | linear→constante por campo |
| `applyDataMaskValueBody` faz `toString()` + `toJsonTree()` | mascarar sobre o `JsonPrimitive` direto, preservar tipo | sem dupla serialização |
| `processBody` faz `toString()`+`fromJson()` por nível | walk único e **in-place** da árvore `JsonObject` | remove parse/serialize ≈quadrático |
| máscara de log na thread de logging | `AsyncAppender` + fast-path | tira custo da thread de request |

**Contrato de thread-safety:**

- `MaskingEngine` **imutável** após o build (campos `final`, coleções
  `unmodifiable`). **Singleton compartilhado** por todas as threads, **sem
  sincronização** e **sem estado por-request** → zero contenção, escala linear com
  cores.
- Config carregada 1× no startup. Hot-reload opcional = **copy-on-write**: troca
  atômica de referência `volatile` para um novo snapshot imutável; leitores
  permanecem lock-free.

**Estruturas de lookup (montadas 1× no build):**

- **Key-based** (body, headers, MDC, audit): `Map<String,DataMask>` imutável, chave
  lower → **O(1)** por campo.
- **Text-based** (log livre): separar **literais** de **regex**. Literais →
  autômato **Aho-Corasick** (1 passada **O(n)** para N agulhas). Regex reais →
  `Pattern[]` pré-compilado, aplicado **só** após o pré-screen casar.

**Hot path de baixa/zero alocação:**

- **Pré-screen barato:** varredura única de chars buscando gatilho (dígito, `@`).
  Sem gatilho → retorna a **mesma referência** `String` (zero cópia, zero match).
  Cobre o caso dominante (linhas sem PII).
- Substituição: `Matcher` reusado por thread (`ThreadLocal<Matcher>`) +
  `appendReplacement` em `StringBuilder` dimensionado — evita `replaceAll`.
- JSON: parse **1×** (streaming `JsonReader` ou árvore), walk recursivo **in-place**
  mutando primitives → **O(n)** no payload.

**Latência limitada / proteção ReDoS:**

- Patterns validados no build: **proibir backtracking catastrófico** (sem
  quantificadores aninhados; usar possessivos `*+`/`++` e âncoras). Rejeitar pattern
  perigoso no startup, não em produção.
- **Cap de tamanho:** body/mensagem acima de N KB (configurável) → truncar com
  marcador antes de mascarar → limita o pior caso.

**Offload assíncrono (não pesar no tempo de resposta):**

```
request thread ──► filtro HTTP ──► maskStructured (síncrono, O(1)/campo)
   log.info(...) ──► AsyncAppender (fila limitada) ──► maskText em thread de log
   commit JPA   ──► @Async ScosAuditLogAsyncExecutor ──► cifra + persist (fora do request)
```

- **Log:** `AsyncAppender` com fila limitada (ex.: 8192), `discardingThreshold=0`
  (nunca descarta `WARN`/`ERROR`); `neverBlock` configurável.
- **Audit:** `@Async` em `ScosAuditLogAsyncExecutor` (já existe) — tunar pool + fila;
  cifra roda nessa thread, **nunca** no request. (Durabilidade da trilha é tratada
  em [`audit-conformidade-lgpd.md`](./audit-conformidade-lgpd.md).)

**Verificação (gate de CI):**

- **JMH** por operação: `maskText` (com/sem PII), `maskStructured`, walk JSON.
- Alvos sugeridos: `maskText` p99 **< ~5µs** linha sem PII (fast-path); **< ~50µs**
  com substituição; throughput escala linear até saturar cores.
- Teste de concorrência (N threads): ausência de contenção, p99 estável. Bench de
  regressão como gate de merge.

### 5. Cifra em repouso da PII na trilha de auditoria

> Fica **neste** documento porque usa classes do módulo privacy
> (`MaskingEngine`, `ScosCryptoKeyProvider`, `ScosFieldCipher`,
> `auditEncryptFields()`). Os demais ajustes da trilha estão em
> [`audit-conformidade-lgpd.md`](./audit-conformidade-lgpd.md).

`entityOld`/`entityNew` ficam **cifrados em repouso** (reversível) — preserva valor
para investigação e protege leitura no banco.

- Hook em `ScosAuditServiceBean.createJsonObject()` (linha 150): cifrar os campos de
  `auditEncryptFields()` antes do `toJson`, ou `AttributeConverter` JPA cifrando a
  coluna JSONB.
- **Gestão de chave** via SPI `ScosCryptoKeyProvider`, default Jasypt com secret
  externalizado, plugável Vault/KMS. Chave **versionada por registro** (`keyId`
  gravado junto ao JSONB); rotação gera chave nova só para registros novos, decrypt
  via `keyId`. **Histórico não é re-cifrado.**
- Não reduz exposição a quem tem a chave → combinar com controle de acesso à tabela;
  para campos não-recuperáveis, masking irreversível.

### 6. Integração com o log da aplicação (híbrido)

- Converter Logback `%mask(%msg)` aplica `maskText` sobre a mensagem renderizada →
  pega PII em texto livre (`log.info("cpf {}", cpf)`).
- Valores MDC conhecidos passam por `maskStructured` (key-based).
- Mesma `DataMaskingValues` reusada → 1 config para filtro + log + audit.
- Registro do converter via auto-config (seção 3), sem o app editar `logback.xml`.

### 7. Corrigir/clarificar `HashUtils`

- Se o uso for **pseudonimização**: migrar para HMAC com chave secreta (ou salt por
  registro). SHA-256 puro não qualifica (LGPD Art. 13 / GDPR Art. 4(5)).
- Se for só checksum/idempotência: documentar que **não** é mecanismo de privacidade.

---

## Exemplo de uso

### 1. Regras de masking (`privacy-masking.yml` — fonte principal)

```yaml
scos:
  privacy:
    masking:
      headers:
        - key: authorization
          value: "***"
      body:
        - key: cpf
          value: "***"
        - key: email
          strategy: partial
      log-patterns:
        - literal: "secret"
          value: "***"
        - regex: '\d{11}'
          value: "***"
      audit-encrypt-fields:
        - cpf
        - email
        - cardNumber
```

### 2. Comportamento do módulo (`application.yml` — só flags, Spring)

```yaml
scos:
  privacy:
    enabled: true                                 # sobe ao importar o módulo
    strict: false                                 # prod: erro se body-log sem máscara
    max-payload-kb: 64                            # cap de tamanho do masking
    masking:
      config-path: /etc/scos/privacy-masking.yml  # externo; vazio → classpath
      default-patterns: true                      # builtins CPF/CNPJ/e-mail/cartão/tel BR
```

### 3. Uso fora do SCOS (Java puro, sem Spring)

```java
MaskingEngine engine = MaskingEngine.fromYaml(Path.of("/etc/scos/privacy-masking.yml"));
String log = engine.maskText("Pessoa criada cpf=12345678901");   // → cpf=***
```

### 4. Override programático opcional (SPI — só se precisar de regra dinâmica)

```java
@Component   // soma ao YML; YML continua a fonte principal
public class MeuMaskingDinamico implements DataMaskingValues {
    @Override public Set<DataMask> bodyValue() {
        return Set.of(DataMask.builder().key("tokenInterno").isRegex(false).newValue("***").build());
    }
}
```

### Trilha de auditoria — cifra seletiva (antes / depois)

```json
// Antes (PII em claro na coluna JSONB)
{ "cpf": "12345678901", "email": "ana@x.com", "status": "ACTIVE" }

// Depois (cifra seletiva por auditEncryptFields, com keyId)
{ "cpf": "enc:v3:9af1...", "email": "enc:v3:b2c0...", "status": "ACTIVE" }
```

### Log da aplicação — antes / depois

```
// Antes
INFO  Pessoa criada cpf=12345678901 email=ana@x.com

// Depois (converter %mask híbrido)
INFO  Pessoa criada cpf=*** email=***
```

---

## Decisões fechadas

1. **Módulo:** Opção A — `scos-foundation-privacy` na camada base, sem dep de
   `utils` (Gson próprio).
2. **Chave da cifra do audit:** SPI `ScosCryptoKeyProvider`, default Jasypt;
   chave **versionada por registro**; histórico não re-cifrado.
3. **`logPatterns` default:** conjunto-base ativável por
   `scos.privacy.masking.default-patterns=true` (CPF, CNPJ, e-mail, cartão Luhn, tel BR).
4. **Forçar vs opt-in (log):** **fail-safe opt-in** + **WARN no startup** quando
   body-log ativo sem máscaras; `scos.privacy.strict=true` eleva a erro em prod.
5. **Auto-config:** módulo carrega via `AutoConfiguration.imports`; tudo
   `@ConditionalOnMissingBean` para permitir override.
6. **Fonte das regras:** **arquivo YML dedicado** (`privacy-masking.yml`), não banco
   e não código. Resolução: caminho externo configurável → fallback classpath.
7. **Portabilidade:** core lê o YML via **SnakeYAML, sem Spring**
   (`MaskingEngine.fromYaml`) → usável fora da camada SCOS; wrapper Spring opcional.
8. **SPI vs YML:** YML é a fonte principal; `DataMaskingValues` (SPI) vira **override
   opcional** que soma ao YML. Precedência: builtins → YML → SPI.
9. **Reload:** **load 1× no startup** (snapshot imutável); mudança no YML exige
   restart. Sem `WatchService` nesta versão.
10. **Estratégias:** enum fechado `fixed`/`partial`/`email`/`hash`/`encrypt`/`redact`;
    `partial` fail-safe (mascara tudo se `keep-first+keep-last >= len`).
11. **Builtins:** packs por país/região (`generic`/`br`/`us`/`eu`/`uk`/`in`) como
    **dados** embarcados, ligáveis/desligáveis por item; entram antes do YML do app.
12. **Testes:** suíte é parte da entrega — unit + property (jqwik) + integração da lib
    (`ApplicationContextRunner` + `@SpringBootTest` + standalone sem Spring) +
    concorrência + JMH como gate de CI.

---

## Camada de testes (lógica + desempenho, integrada à lib)

A suíte é **parte da entrega** — sem ela o requisito de desempenho e o fail-safe de
PII não são verificáveis. Pirâmide do módulo `privacy`:

```
        ┌─ JMH bench (gate CI) ──────── hot path: p99 + throughput + alocação
        ├─ Concorrência (N threads) ─── zero contenção, correção sob paralelismo
   ┌────┴─ Integração da lib ────────── auto-config só por classpath (sem @ComponentScan)
   │                                     + end-to-end (log / HTTP / audit)
   │                                     + standalone SEM Spring (fromYaml)
   ├─ Property-based (jqwik) ─────────── invariante: saída nunca contém PII original
   └─ Unit ──────────────────────────── cada strategy, builtins, parse YML, precedência, ReDoS
```

### 1. Unit (lógica)

- **Cada `strategy`**: `fixed`/`partial`/`email`/`hash`/`encrypt`/`redact` com tabela
  de casos (incl. fail-safe `keep-first+keep-last >= len` → mascara tudo).
- **Parse YML**: schema válido, chave desconhecida → erro, `strategy` inválida → erro,
  `keep-*` negativo → erro.
- **Precedência** builtins → YML → SPI: app sobrepõe builtin por chave; SPI soma ao YML.
- **Builtins por país**: cada pattern casa positivos e rejeita negativos; Luhn/DV em
  `credit-card`/`cpf`/`cnpj`.
- **Rejeição ReDoS**: pattern com backtracking catastrófico → falha no build.

### 2. Property-based (jqwik) — invariante de segurança

- Gera PII aleatória (CPF/e-mail/cartão), injeta em texto/JSON → **assert a saída
  não contém o valor original** (a propriedade que mais importa: não vazar).
- Idempotência: `mask(mask(x)) == mask(x)`.
- `hash` estável: mesma entrada → mesma saída; entradas diferentes → saídas diferentes.

### 3. Integração da lib (o "teste integrado" pedido)

Idiomático Spring Boot — `ApplicationContextRunner` para fatias de auto-config +
`@SpringBootTest` para end-to-end:

- **Carrega só por classpath**: `ApplicationContextRunner` com o módulo no classpath
  → beans (`MaskingEngine`, `DataMaskingService`, converter) sobem **sem**
  `@ComponentScan` (prova o requisito "carregar ao importar").
- **`@ConditionalOnMissingBean`**: app define o próprio bean → o da lib recua.
- **Resolução de fonte**: YML em `config-path` externo **sobrepõe** o do classpath;
  ausência das duas → builtins + WARN.
- **Log end-to-end**: `ListAppender`/`OutputCapture` — emite `log.info("cpf {}",x)`
  e **assert a linha saiu mascarada** (converter `%mask` ativo sem editar `logback.xml`).
- **HTTP end-to-end**: `MockMvc` → request com PII no body/header → **assert log do
  filtro mascarado**.
- **Audit round-trip**: cifra `auditEncryptFields` no persist → **decrypt recupera o
  valor** (reversível) com `keyId` versionado.
- **Standalone SEM Spring**: teste que chama `MaskingEngine.fromYaml(path)` num `main`
  puro (sem contexto Spring) → prova portabilidade "fora da camada SCOS".

### 4. Concorrência

- N threads (`= cores`) compartilham 1 `MaskingEngine`, mascaram payloads distintos
  em paralelo → **resultado idêntico ao sequencial** (sem corrupção de estado).
- Detector de contenção: sem `synchronized`/lock no hot path; throughput escala ~linear.

### 5. Desempenho (JMH — gate de CI)

- Benchmarks: `maskText` (com/sem PII), `maskStructured`, walk JSON, `fromYaml` (cold start).
- **Alvos** (sugeridos, ajustar à máquina-base): `maskText` p99 **< ~5µs** linha sem
  PII (fast-path) · **< ~50µs** com substituição · alocação **≈0** no fast-path.
- **Gate**: regressão > X% vs baseline versionado → **falha o merge**. Baseline
  guardado em `etc/perf/baseline.json`.

### Organização

- Unit/property/integração em `privacy/src/test/java` (`spring-boot-starter-test`,
  `jqwik`).
- JMH em **source set isolado** (`src/jmh`) p/ não pesar no build normal; roda em job
  de CI dedicado (perfil `-Pperf`).
- Fixtures de YML (`test-masking-*.yml`) + fixtures de PII por país p/ os builtins.

---

## Impacto e Riscos

### Breaking changes

- Pacote `br.com.sawcunhaos.foundation.utils.lgpd` → `...foundation.privacy`.
  Imports em consumidores que referenciam o pacote antigo quebram.
- `audit` e `utils` ganham nova dependência (`scos-foundation-privacy`).
- Módulo `privacy` declara **SnakeYAML** explícito (uso standalone fora do Spring).
- Regras de masking migram de código (`@Component implements DataMaskingValues`)
  para o **YML** `privacy-masking.yml`. Apps que só tinham a SPI continuam
  funcionando (SPI vira override), mas o caminho recomendado passa a ser o YML.
- Coluna JSONB de auditoria passa a conter valores cifrados (`enc:v…`) para os
  campos em `auditEncryptFields` — leitura direta no banco exige decrypt.

### Mitigação

- Manter **classes-fachada `@Deprecated`** no pacote antigo por 1 release, delegando
  ao novo (transição suave).
- `pom.xml` parent: declarar `<module>privacy</module>` **antes** de `utils`.
- Cifra é **opt-in por campo** (`auditEncryptFields` vazio = comportamento atual) →
  não quebra trilhas existentes; migração de histórico é decisão do app.
- Documentar no CHANGELOG o move de pacote e o prefixo `enc:vN:` da cifra.

### Não é breaking para

- Apps que não referenciam o pacote `utils.lgpd` diretamente (só usam via filtros).
- Trilhas de auditoria sem `auditEncryptFields` declarado.
- Quem só consome os filtros de log existentes (continuam funcionando).

---

## Validação: conformidade com convenções SCOS e mercado

### Pontos de atenção

- **Auto-config já é padrão em `audit`/`jdempotent`**: `privacy` apenas o segue.
  A pendência real é `utils` (masking/filtros) ser a exceção sem `.imports` —
  alinhar com o time se a auto-config de `utils` entra neste PR ou depois.
- **`@ConditionalOnMissingBean` obrigatório**: sem isso, a auto-config conflita com
  beans do app. Toda peça exposta deve ser sobrescrevível.
- **Logback é infra do app**: registrar `conversionRule` programaticamente respeita
  o `logback.xml` do consumidor; documentar o opt-out.
- **Nome do módulo `privacy` vs pasta `lgpd`**: escopo multi-lei justifica o nome
  neutro; documentar a relação para não confundir o time.

### O que está alinhado com mercado e SCOS

| Aspecto | Status |
|---|---|
| Masking/anonimização como medida técnica (Art. 46 / A32) | ✅ Padrão das 8 leis |
| Cifra em repouso reversível com gestão de chave | ✅ Prática de mercado (envelope encryption) |
| Pseudonimização via HMAC (não SHA puro) | ✅ Alinhado a LGPD Art. 13 / GDPR A4(5) |
| Fonte única de regras (1 SPI → log/HTTP/audit) | ✅ Reduz divergência e configuração duplicada |
| Auto-configuração via `AutoConfiguration.imports` | ✅ Idiomático Spring Boot 4 (starter pattern) |
| `@ConditionalOnMissingBean` para override | ✅ Convenção de starters |
| Aho-Corasick + Pattern pré-compilado | ✅ Técnica consolidada p/ multi-pattern em hot path |
| Offload assíncrono (AsyncAppender / `@Async`) | ✅ Mantém tempo de resposta baixo |
| Bench JMH como gate de CI | ✅ Garante o requisito de desempenho |
| Regras em YML versionável (config como dado, não código) | ✅ Manutenção fácil, sem banco, sem rebuild |
| Core sem Spring (`fromYaml`) — usável fora do SCOS | ✅ Biblioteca portável, baixo acoplamento |
| Estratégias de máscara como enum + builtins por país | ✅ Cobre multi-lei sem código por consumidor |
| Property-based garante invariante "não vaza PII" | ✅ Testa o que mais importa, não só exemplos |
| Integração via `ApplicationContextRunner` | ✅ Forma idiomática de testar auto-config Spring Boot |
| JMH como gate de regressão de performance | ✅ Requisito de desempenho fica verificável |
| Governança fora de escopo (app responsável) | ✅ Correto p/ uma biblioteca |

### Resumo: o que a implementação deve fazer além do código

1. Criar o módulo `privacy` e mover `utils/lgpd` → `privacy` (com fachadas `@Deprecated`).
2. Implementar `PrivacyConfig` (POJO) + `PrivacyConfigLoader` (SnakeYAML, resolução
   externo→classpath, validação) + `MaskingEngine.fromYaml(...)` (factory sem Spring).
3. Adicionar `AutoConfiguration.imports` + `ScosPrivacyAutoConfiguration` +
   `ScosPrivacyProperties` (`masking.config-path`) + metadata de IDE.
4. Embarcar `privacy-masking.yml` default no classpath + exemplo no README.
5. Ajustar `pom.xml` parent (ordem de módulos) e deps de `utils` e `audit`; declarar
   SnakeYAML explícito no `privacy`.
6. Atualizar `CLAUDE.md`/skills SCOS citando o novo módulo, o YML e o uso standalone.
7. Implementar as `strategy` (enum + validação fail-safe) e os packs de builtins por
   país (`privacy-builtins/<pack>.yml`).
8. Documentar no CHANGELOG: move de pacote, YML de regras, `strategy`, builtins,
   prefixo `enc:vN:`, flags `scos.privacy.*`.
9. Suíte de testes: unit + property (jqwik) + integração da lib
   (`ApplicationContextRunner` + `@SpringBootTest` + standalone sem Spring) +
   concorrência; JMH em `src/jmh` com baseline versionado como gate de CI.

---

## Referências

- [LGPD — Lei 13.709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)
- [GDPR — Regulation (EU) 2016/679](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- [Spring Boot — Creating Your Own Auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)
- [Aho-Corasick — multi-pattern string matching](https://dl.acm.org/doi/10.1145/360825.360855)
- [OWASP — Regular expression Denial of Service (ReDoS)](https://owasp.org/www-community/attacks/Regular_expression_Denial_of_Service_-_ReDoS)
- [Logback — Layouts e conversionRule](https://logback.qos.ch/manual/layouts.html)
- [SnakeYAML — YAML parser para Java](https://bitbucket.org/snakeyaml/snakeyaml/src/master/)
- [Spring Boot — Test Autoconfiguration (`ApplicationContextRunner`)](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html#features.developing-auto-configuration.testing)
- [jqwik — Property-based testing for Java](https://jqwik.net/)
- [JMH — Java Microbenchmark Harness](https://github.com/openjdk/jmh)
