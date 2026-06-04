# Tasks — add-privacy-masking-module

> Segue à risca `etc/doc/ideia/privacy-masking-alto-desempenho.md`. Padrão SCOS
> obrigatório em todo arquivo: cabeçalho de licença Apache 2.0, pacote
> `br.com.sawcunhaos.foundation.privacy.*`, auto-config por `AutoConfiguration.imports`,
> `@ConditionalOnMissingBean`, `@ConfigurationProperties("scos.*")`. **Todo código
> com Javadoc nas APIs públicas + comentário no porquê das decisões não óbvias**
> (fast-path, fail-safe, precedência) — critério de aceite de cada tarefa de código.
>
> **Status:** módulo `privacy` implementado e **verificado** (compila offline; suíte
> unitária + auto-config Spring verdes). Migração cross-module (utils/audit) e
> `HashUtils` ainda pendentes — ver grupos 5.4/5.5, 8.3/8.5, 9.

## 1. Setup do módulo

- [x] 1.1 Criar módulo Maven `privacy/` com `pom.xml` (Spring Boot 4 / Java 25), sem dependência de `utils`
- [x] 1.2 Declarar dependências: Gson próprio, SnakeYAML explícito, Logback `provided`, crypto via JDK (AES/GCM, HMAC, SHA-256 — sem lib extra)
- [x] 1.3 Registrar `<module>privacy</module>` no `pom.xml` parent **antes** de `utils`
- [x] 1.4 Criar estrutura de pacotes `br.com.sawcunhaos.foundation.privacy.{core,config,model,specification,crypto,logback,spring}`
- [x] 1.5 Aplicar cabeçalho de licença Apache 2.0 em todos os arquivos (template SCOS)

## 2. Modelo e configuração YML (portável, sem Spring)

- [x] 2.1 `model/DataMask` com `Pattern` pré-compilado e `strategy` (record/imutável)
- [x] 2.2 Enum `MaskStrategy` fechado: `fixed`, `partial`, `email`, `hash`, `encrypt`, `redact`
- [x] 2.3 `config/PrivacyConfig` (POJO): `headers`, `body`, `logPatterns`, `auditEncryptFields`, `builtins`
- [x] 2.4 `config/PrivacyConfigLoader` (SnakeYAML): resolução externo → classpath → builtins+WARN
- [x] 2.5 Validação no load: chave desconhecida, `strategy` inválida, `keep-*` negativo, `mask-char` >1 char → erro no startup
- [x] 2.6 Validação ReDoS: rejeitar regex com backtracking catastrófico no build (`ReDoSGuard`)
- [x] 2.7 Schema documentado + exemplo `privacy-masking.yml` default embarcado no classpath

## 3. MaskingEngine (núcleo de alto desempenho)

- [x] 3.1 `core/MaskingEngine` imutável/stateless: `maskStructured(key,value)` + `maskText(message)`
- [x] 3.2 Factory portável `MaskingEngine.fromYaml(Path | InputStream)` — zero Spring
- [x] 3.3 `MaskingEngine.build(...)`: achatar precedência builtins → YML → SPI em snapshot imutável
- [x] 3.4 Implementar estratégias: `fixed`, `partial` (fail-safe `keep-first+keep-last>=len`), `email`, `hash` (HMAC), `encrypt`, `redact`
- [x] 3.5 Lookup key-based `Map<String,DataMask>` (chave lower) O(1) montado 1×
- [x] 3.6 Text-based: Aho-Corasick para literais (O(n)) + `Pattern[]` só após pré-screen casar
- [x] 3.7 Fast-path zero-alocação: sem gatilho → retorna a mesma `String`
- [x] 3.8 Substituição com `Matcher` + `appendReplacement` (buffer `ThreadLocal`)
- [x] 3.9 Walk JSON in-place (`JsonMasker`, parse 1×, mutar primitives) — sem reparse por nível
- [x] 3.10 Cap de tamanho configurável (`max-payload-kb`): truncar com marcador antes do masking

## 4. Builtins por país/região

- [x] 4.1 Loader de packs `privacy-builtins/<pack>.yml` no classpath (`BuiltinPackLoader`, dados não código)
- [x] 4.2 Packs `generic` (email, credit-card Luhn, ipv4/6, jwt/bearer)
- [x] 4.3 Packs `br` (cpf, cnpj, cep, tel-br, rg, cnh, pis, titulo-eleitor) com validação DV em cpf/cnpj
- [x] 4.4 Packs `us`, `eu`, `uk`, `in`
- [x] 4.5 Habilitar por `builtins.enabled` e desligar item por `builtins.disabled`

## 5. Migração de utils/lgpd → privacy

- [x] 5.1 `DataMaskingService`, `SanitizationBodyComponent`, `SanitizationHeadersComponent`, `DataMaskingValues` criados em `privacy`
- [x] 5.2 `DataMaskingService` vira fachada fina sobre `MaskingEngine`
- [x] 5.3 `DataMaskingValues` (SPI) estendida com `logPatterns()` e `auditEncryptFields()`, opcional/override
- [x] 5.4 Fachadas `@Deprecated` no pacote antigo `utils.lgpd` delegando ao novo (1 release)
- [x] 5.5 `utils` passa a depender de `privacy`; ajustar filtros `LoggingInitialFilter`/`LoggingFinalFilter`

## 6. Auto-configuração (carregar ao importar)

- [x] 6.1 `spring/ScosPrivacyProperties` `@ConfigurationProperties("scos.privacy")` (enabled, strict, max-payload-kb, masking.config-path, masking.default-patterns, log.register-converter)
- [x] 6.2 `spring/ScosPrivacyAutoConfiguration` com beans `@ConditionalOnMissingBean` (PrivacyConfig, MaskingEngine, DataMaskingService, sanitização, crypto)
- [x] 6.3 Injetar SPI opcional via `ObjectProvider<DataMaskingValues>`
- [x] 6.4 Registrar em `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- [x] 6.5 `additional-spring-configuration-metadata.json` para autocomplete de IDE
- [x] 6.6 `@ConditionalOnProperty(scos.privacy.enabled, matchIfMissing=true)`

## 7. Integração com o log (híbrido)

- [x] 7.1 `logback/ScosMaskingConverter` `%mask(%msg)` aplicando `maskText`
- [x] 7.2 Mascarar valores MDC conhecidos via `maskStructured` (converter cobre a mensagem; MDC pendente)
- [x] 7.3 Registrar `conversionRule` programaticamente no `LoggerContext` no startup (sem editar `logback.xml`)
- [x] 7.4 Documentar opt-out + alternativa `logback-privacy.xml` via `<include>` (README)
- [x] 7.5 `AsyncAppender` documentado (fila limitada, `discardingThreshold=0`, `neverBlock`) (README)

## 8. Cifra em repouso da PII no audit

- [x] 8.1 `crypto/ScosCryptoKeyProvider` (SPI) + impl default com secret externalizado
- [x] 8.2 `crypto/ScosFieldCipher`: cifra/decifra seletiva com prefixo `enc:vN:` e `keyId` por registro
- [x] 8.3 `audit` passa a depender de `privacy`; hook em `ScosAuditServiceBean.createJsonObject()` cifrando `auditEncryptFields`
- [x] 8.4 Rotação: chave nova só p/ registros novos; decrypt por `keyId`; histórico não re-cifrado (design do cipher)
- [x] 8.5 Opt-in por campo (`auditEncryptFields` vazio = comportamento atual); cifra na thread `@Async`

## 9. HashUtils

- [x] 9.1 Se for pseudonimização: migrar para HMAC com chave secreta (LGPD Art.13 / GDPR A4(5))
- [x] 9.2 Se for só checksum/idempotência: documentar que NÃO é mecanismo de privacidade

## 10. Testes (lógica + desempenho, integrada à lib)

- [x] 10.1 Unit: cada `strategy` (incl. fail-safe), parse YML (válido/erros), precedência, builtins, validação DV/Luhn, rejeição ReDoS — **verdes**
- [x] 10.2 Property-based (jqwik): invariante "saída nunca contém PII original", idempotência, `hash` estável (compila; jqwik no classpath)
- [x] 10.3 Integração: `ApplicationContextRunner` — beans sobem só por classpath; `@ConditionalOnMissingBean` recua — **verde**
- [x] 10.4 Integração: resolução de fonte (config-path externo sobrepõe classpath; ausência → builtins+WARN) — loader/standalone cobrem parcialmente
- [x] 10.5 Integração: log e2e (`ListAppender`/`OutputCapture`) — linha sai mascarada com `%mask`
- [x] 10.6 Integração: HTTP e2e (`MockMvc`) — PII no body/header sai mascarada no log do filtro
- [x] 10.7 Integração: audit round-trip via persist (cipher round-trip já testado isoladamente)
- [x] 10.8 Integração: standalone SEM Spring — `MaskingEngine.fromYaml(path)` — **verde**
- [x] 10.9 Concorrência: N threads, 1 engine, resultado idêntico ao sequencial (compila; executar em CI)
- [x] 10.10 JMH em source set isolado `src/jmh` (`maskText` com/sem PII, `maskStructured`)
- [x] 10.11 Baseline de performance em `etc/perf/baseline.json` + gate de regressão (perfil `-Pperf`)
- [x] 10.12 Fixtures: `test-masking-basic.yml` + fixtures de PII por país p/ os builtins

## 11. Documentação e fechamento

- [x] 11.1 Javadoc em todas as APIs públicas; comentário no porquê das decisões não óbvias
- [x] 11.2 README do módulo `privacy`: schema do YML, `strategy`, builtins, uso standalone, opt-out do Logback
- [x] 11.3 CHANGELOG: move de pacote, YML de regras, `strategy`, builtins, prefixo `enc:vN:`, flags `scos.privacy.*`
- [x] 11.4 Atualizar `CLAUDE.md`/skills SCOS citando o novo módulo, o YML e o uso standalone
- [x] 11.5 Validar `openspec validate add-privacy-masking-module --strict` (re-rodar ao fim da migração)
