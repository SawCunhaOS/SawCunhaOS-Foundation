---
name: scos-privacy-config
description: >
  Configurar o módulo scos-foundation-privacy num sistema consumidor SCOS — masking de PII por regras YAML,
  builtins por país, estratégias (fixed/partial/email/hash/encrypt/redact), converter Logback %mask/%maskmdc,
  cifra em repouso do audit e uso standalone sem Spring. Use ao mascarar PII em logs/HTTP/auditoria ou ao
  ajustar privacy-masking.yml. Auto-configura ao estar no classpath.
---

# Configuração — `scos-foundation-privacy`

Motor único de masking de PII. Uma fonte de regras alimenta três superfícies: filtro de log HTTP, converter
Logback `%mask`, e cifra de campos do audit. Núcleo **portável** (`MaskingEngine.fromYaml`, sem Spring).

## 1. Dependência

```xml
<dependency>
  <groupId>br.com.sawcunhaos</groupId>
  <artifactId>scos-foundation-privacy</artifactId>
</dependency>
```

Vem transitivo via `scos-foundation-utils`. Auto-configura por `AutoConfiguration.imports` — **não** precisa
`@ComponentScan`. Ligado por padrão (`scos.privacy.enabled` tem `matchIfMissing=true`).

## 2. Flags (`application.yml`)

```yaml
scos:
  privacy:
    enabled: true                                 # default true
    strict: false                                 # prod: erro se hash/encrypt sem chave
    max-payload-kb: 64                            # trunca antes de mascarar
    masking:
      config-path: /etc/scos/privacy-masking.yml  # vazio -> classpath
    log:
      register-converter: true                    # registra %mask/%maskmdc no startup
    crypto:
      secret: ${SCOS_PRIVACY_CRYPTO_SECRET}       # via env/Vault/KMS
```

## 3. Regras (`privacy-masking.yml`)

Resolução: caminho externo → classpath `privacy-masking.yml` → builtins + WARN.

```yaml
scos:
  privacy:
    masking:
      builtins:
        enabled: [generic, br]          # packs por país/região
        disabled: [br.titulo-eleitor]
      headers:
        - key: authorization
          strategy: fixed
          value: "***"
      body:
        - key: cpf
          strategy: partial
          keep-first: 0
          keep-last: 2
        - key: email
          strategy: email
      log-patterns:                     # texto livre (só fixed/partial/hash)
        - regex: '(?<![A-Za-z0-9])(\d{14}|[A-Za-z0-9]{8}\d{6})(?![A-Za-z0-9])'
          strategy: partial
          keep-first: 3
          keep-last: 2
      audit-encrypt-fields:             # cifra em repouso (opt-in)
        - cpf
        - email
```

Estratégias: `fixed`, `partial`, `email`, `hash` (HMAC), `encrypt` (`enc:vN:`), `redact`. Builtins:
`generic`/`br`/`us`/`eu`/`uk`/`in` (cpf/cnpj validam dígito verificador).

## 4. Masking de log (Logback)

Dois converters: `%mask(%msg)` (texto livre) e `%maskmdc{chave}` (um valor de MDC). Em `logback-spring.xml`
declare as `<conversionRule>` (logback inicia antes do Spring) e use `%mask` no pattern:

```xml
<conversionRule conversionWord="mask"
                converterClass="br.com.sawcunhaos.foundation.privacy.logback.ScosMaskingConverter"/>
<conversionRule conversionWord="maskmdc"
                converterClass="br.com.sawcunhaos.foundation.privacy.logback.ScosMaskingMdcConverter"/>
<pattern>%d %-5level %logger - %mask(%msg)%n</pattern>
```

Detalhes completos no `privacy/README.md` (AsyncAppender, Logstash/JSON, opt-out).

## 5. Uso standalone (sem Spring)

```java
MaskingEngine engine = MaskingEngine.fromYaml(Path.of("/etc/scos/privacy-masking.yml"));
engine.maskText("cpf=12345678901");   // -> cpf=*********01
engine.maskStructured("cpf", "12345678901");
```

## 6. Override (SPI / chave)

Tudo `@ConditionalOnMissingBean`. Some regras programáticas implementando `DataMaskingValues`
(precedência builtins → YAML → SPI). Troque a chave por Vault/KMS com um `ScosCryptoKeyProvider` próprio.

## Pegadinhas

- **`%mask(%msg)` é obrigatório no pattern** — o `%msg` default não mascara.
- **Regex de `log-patterns` não pode ser ancorada** `^...$`: o token aparece no meio da linha.
- Builtins `cpf`/`cnpj` validam DV → ignoram números **falsos**; para mascarar dados de teste use uma regra
  `log-patterns` sem validador.
- `LogstashEncoder` (JSON) não passa pelo pattern → `message` não é mascarado por `%mask` (ver README).
