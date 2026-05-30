## Why

O masking de PII hoje vive em `utils/lgpd`, só protege o log HTTP (2 filtros), não
tem auto-configuração (sobe só por `@ComponentScan` do app, ao contrário de
`audit`/`jdempotent`), recompila `Pattern` por chamada e faz reparse de JSON por
nível — caminho quente, subótimo sob carga. Há três superfícies de PII (log HTTP,
log da aplicação, trilha de auditoria) e só uma protegida, sem fonte única de
regras. Falta cifra em repouso da PII na trilha (`entityOld`/`entityNew` em JSONB
cru) e a pseudonimização atual (`HashUtils` SHA-256 sem salt) é inadequada à LGPD
Art. 13.

Frente a oito leis (LGPD foco, GDPR, CCPA/CPRA, PIPL, UK GDPR, DPDP, POPIA,
PIPEDA), a parcela técnica de masking/cripto precisa ser elevada ao teto comum das
leis. Como `SawCunhaOS-Foundation` é biblioteca, entrega só salvaguardas técnicas; a
governança fica no app consumidor.

As regras (campos + máscaras) devem ser **lidas de um arquivo YML** — sem banco, sem
recompilar, mantidas por texto versionável — e o motor deve ser **utilizável fora da
camada SCOS** (núcleo lê o YML sozinho, sem depender de Spring), com **alto
desempenho multithread** e **carregamento automático** ao ser importado.

Esta proposta segue **à risca** o documento de ideia
`etc/doc/ideia/privacy-masking-alto-desempenho.md`. Todo código entregue deve ser
**escrito e documentado para leitura e manutenção futura** (Javadoc nas APIs
públicas, nomes intencionais, comentários no porquê das decisões não óbvias) e
**seguir o padrão de desenvolvimento SCOS** (cabeçalho de licença Apache 2.0,
pacote `br.com.sawcunhaos.foundation.*`, auto-config via `AutoConfiguration.imports`,
`@ConditionalOnMissingBean` para override, `@ConfigurationProperties("scos.*")`).

## What Changes

- **Novo módulo `scos-foundation-privacy`** na camada base (sem dependência de
  `utils`, Gson próprio) — move `utils/lgpd` → `privacy`, com fachadas `@Deprecated`
  no pacote antigo por 1 release.
- **`MaskingEngine` unificado** (imutável, stateless, singleton) — 1 fonte de regras
  alimenta 3 consumidores: filtro HTTP, converter Logback `%mask`, cifra do audit.
- **Regras via YML** (`privacy-masking.yml`): resolução caminho externo → fallback
  classpath; `PrivacyConfig` (POJO) + `PrivacyConfigLoader` (SnakeYAML); factory
  portável `MaskingEngine.fromYaml(...)` **sem Spring**.
- **Estratégias de máscara** como enum fechado: `fixed`, `partial`, `email`, `hash`,
  `encrypt`, `redact`, com validação fail-safe no load.
- **Builtins por país/região** (`generic`/`br`/`us`/`eu`/`uk`/`in`) como dados
  embarcados, ligáveis/desligáveis por item.
- **Auto-configuração** via `AutoConfiguration.imports` (carrega ao importar, sem
  `@ComponentScan`); tudo `@ConditionalOnMissingBean`. SPI `DataMaskingValues` vira
  **override opcional** que soma ao YML (precedência builtins → YML → SPI).
- **Masking de log híbrido**: converter Logback `%mask` (text-based) + MDC
  (key-based), registrado programaticamente sem editar `logback.xml` do app.
- **Cifra em repouso da PII no audit**: `auditEncryptFields` cifrados via
  `ScosFieldCipher`/`ScosCryptoKeyProvider` (Jasypt default, plugável Vault/KMS),
  chave versionada por registro (`keyId`).
- **`HashUtils`**: migrar para HMAC quando for pseudonimização, ou documentar que é
  só checksum.
- **Performance**: `Pattern` pré-compilado, lookup O(1), walk JSON in-place,
  Aho-Corasick para literais, fast-path zero-alocação, offload assíncrono, proteção
  ReDoS.
- **Camada de testes** como parte da entrega: unit, property-based (jqwik),
  integração da lib (`ApplicationContextRunner` + `@SpringBootTest` + standalone sem
  Spring), concorrência, e JMH com baseline como gate de CI.

## Capabilities

### New Capabilities

- `privacy-masking`: motor de masking de alto desempenho (estratégias, fonte YML
  portável, builtins por país, fast-path multithread, proteção ReDoS) — núcleo
  utilizável fora da camada SCOS.
- `privacy-autoconfiguration`: carregamento automático do módulo ao ser importado
  (auto-config Spring Boot, `@ConditionalOnMissingBean`, SPI override opcional).
- `privacy-log-integration`: masking do log da aplicação via converter Logback
  `%mask` híbrido (texto livre + MDC) sem editar `logback.xml` do app.
- `audit-pii-encryption`: cifra em repouso reversível da PII na trilha de auditoria
  (campos opt-in, chave versionada por registro, prefixo `enc:vN:`).

### Modified Capabilities

<!-- Nenhuma: openspec/specs/ não contém specs existentes cujos requisitos mudem.
     O masking atual em utils/lgpd não está formalizado como spec OpenSpec. -->

## Impact

- **Código afetado:** novo módulo `privacy/`; `utils` passa a depender de `privacy`
  (filtros de log consomem os beans auto-configurados); `audit` depende de `privacy`
  para a cifra; `pom.xml` parent ganha `<module>privacy</module>` antes de `utils`.
- **Breaking changes:** move de pacote `utils.lgpd` → `foundation.privacy` (mitigado
  por fachadas `@Deprecated`); `audit`/`utils` ganham dependência nova; SnakeYAML
  declarado explícito no `privacy`; coluna JSONB de auditoria passa a conter valores
  cifrados (`enc:vN:`) para os campos opt-in em `auditEncryptFields`.
- **Não-breaking para:** apps que não referenciam `utils.lgpd` direto; trilhas sem
  `auditEncryptFields`; quem só consome os filtros de log existentes.
- **Documentação/processo:** CHANGELOG (move de pacote, YML, `strategy`, builtins,
  `enc:vN:`, flags `scos.privacy.*`); `CLAUDE.md`/skills SCOS citando o novo módulo;
  README com exemplo de YML e uso standalone; baseline de performance em
  `etc/perf/baseline.json`.
- **Fora de escopo:** governança (base legal, consentimento, direitos do titular,
  ROPA, incidente, DPIA) — responsabilidade do app; ajustes da trilha que não usam
  classes de `privacy` (consulta, imutabilidade, retenção, cobertura de leitura,
  durabilidade) — tratados em `etc/doc/ideia/audit-conformidade-lgpd.md`; auto-config
  própria de `utils` para os filtros (melhoria adjacente).
