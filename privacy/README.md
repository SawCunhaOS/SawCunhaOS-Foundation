# SCOS Foundation Privacy

High-performance, multithread-safe **data masking** for SCOS projects, with rules driven by a YAML file,
country/region builtins, pseudonymization and reversible at-rest encryption.

The core is **usable outside the SCOS layer** (no Spring required) through `MaskingEngine.fromYaml(...)`.

> One source of rules feeds three PII surfaces: the HTTP logging filter, the Logback `%mask` converter, and
> the audit-trail field cipher.

---

## Why a dedicated module

`privacy` sits at the base layer with **no dependency on `utils`** (it carries its own Gson). `utils` and
`audit` depend on `privacy`. This avoids the `utils ↔ privacy` cycle that would appear if the masking lived
in `utils` (whose logging filters consume it).

---

## Loading on import (Spring)

The module auto-configures itself — the same pattern as `audit`/`jdempotent`. Just having
`scos-foundation-privacy` on the classpath registers the beans (`MaskingEngine`, `DataMaskingService`,
sanitizers, crypto), with **no `@ComponentScan`** required. Every bean is `@ConditionalOnMissingBean`, so an
application can override any piece.

```xml
<dependency>
  <groupId>br.com.sawcunhaos</groupId>
  <artifactId>scos-foundation-privacy</artifactId>
</dependency>
```

---

## Masking rules (`privacy-masking.yml`)

Rules are **data, not code**: edit the YAML, no recompile, no database. Resolution precedence:

1. external path — `scos.privacy.masking.config-path` (or env `SCOS_PRIVACY_MASKING_CONFIG`);
2. classpath fallback — `privacy-masking.yml`;
3. neither — builtins only + a WARN at startup.

```yaml
scos:
  privacy:
    masking:
      builtins:
        enabled: [generic, br]          # country/region packs
        disabled: [br.titulo-eleitor]   # turn a single item off
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
      log-patterns:
        - literal: "secret"
          strategy: fixed
          value: "***"
        - regex: '\d{11}'
          strategy: partial
          keep-last: 2
      audit-encrypt-fields:
        - cpf
        - email
```

### Strategies

| `strategy` | Reversible | Params | Example (`12345678901`) |
|---|---|---|---|
| `fixed` (default) | no | `value` (default `***`) | `***` |
| `partial` | no | `keep-first`, `keep-last`, `mask-char`, `preserve-length` | `*********01` |
| `email` | no | `mask-local`, `mask-domain` | `a***@***.com` |
| `hash` | no (pseudonym.) | keyed HMAC-SHA256 | `h:7f3a…` |
| `encrypt` | yes | field cipher (`enc:vN:`) | `enc:vv1:9af1…` |
| `redact` | no | — | removed |

Fail-safe: `partial` masks the whole value when `keep-first + keep-last >= length`. Invalid `strategy`,
negative `keep-*`, multi-char `mask-char` and ReDoS-prone regexes all fail at startup, never in production.
`log-patterns` accept only `fixed`/`partial`/`hash`.

### Builtins by country/region

`generic` (email, credit-card with Luhn, ipv4/6, jwt/bearer), `br` (cpf, cnpj, cep, tel-br, rg, cnh, pis,
titulo-eleitor), `us`, `eu`, `uk`, `in`. Packs are embedded data (`privacy-builtins/<pack>.yml`); adding a
country is a new file. `cpf`/`cnpj`/`credit-card` validate the check digit after a cheap pre-screen.

---

## Which rule fires where

Each YAML section maps to a different engine method, matched differently and consumed at a different moment:

| Rule (`privacy-masking.yml`) | Engine method | Matches by | Consumer / moment |
|---|---|---|---|
| `headers` | `maskHeader(name, value)` | header **name** | HTTP log filter, when logging the request **headers** |
| `body` | `maskStructured(key, value)` | JSON field **name** | HTTP log filter, when logging the request/response **body**; also `%maskmdc{key}` |
| `log-patterns` (+ builtins) | `maskText(message)` | **value** (literal/regex) | Logback `%mask(%msg)` and any free-text log line |
| `audit-encrypt-fields` | `ScosFieldCipher.encrypt` | field **name** | Audit trail, before persisting the JSONB snapshot (on the `@Async` thread) |

> **Name vs value.** `headers` / `body` / `audit-encrypt-fields` match by the **field/header name** (regardless
> of the value); `log-patterns` and builtins match the **value** by literal/regex inside free text.

### 1) `headers` — request header in the HTTP log

Fired by the HTTP logging filter while rendering the request headers. Gated by `server.filter.show-request-headers`.

```yaml
# privacy-masking.yml
headers:
  - key: authorization
    strategy: fixed
    value: "***"
```
```yaml
# application.yml — the filter must actually log the headers
server:
  filter:
    show-request-headers: true
```
```
# log:  Header Name -> authorization -- ***
```

### 2) `body` — JSON field in the HTTP body log (matched by field name)

Fired by `JsonMasker` (single-pass walk) when the filter logs the request/response body. Gated by
`server.filter.show-request-body` / `show-response-body`. **If those are `false`, `body` rules stay dormant.**

```yaml
# privacy-masking.yml
body:
  - key: taxIdentifier        # matches the JSON field NAME, any value
    strategy: partial
    keep-first: 3
    keep-last: 2
```
```yaml
# application.yml
server:
  filter:
    show-request-body: true
    show-response-body: true
```
```jsonc
// request body                         // logged (masked)
{ "taxIdentifier": "RTHSGCAN000163" }   -> { "taxIdentifier": "RTH*********63" }
```

The same `body` rules also back `%maskmdc{taxIdentifier}` in Logback (it calls `maskStructured`), and a direct
call to `DataMaskingService.applyDataMaskValueBody("taxIdentifier", value)`.

### 3) `log-patterns` — free text in the application log (matched by value)

Fired by `maskText`, i.e. the Logback `%mask(%msg)` converter — anywhere PII appears in a message.

```yaml
# privacy-masking.yml
log-patterns:
  - regex: '(?<![A-Za-z0-9])(\d{14}|[A-Za-z0-9]{8}\d{6})(?![A-Za-z0-9])'
    strategy: partial
    keep-first: 3
    keep-last: 2
```
```java
log.info("Validating CNPJ uniqueness: {}", taxId);
// log:  Validating CNPJ uniqueness: RTH*********63
```

### 4) `audit-encrypt-fields` — at rest in the audit trail (matched by field name)

Fired in `ScosAuditServiceBean.createJsonObject()` before the snapshot is persisted; the listed fields are
encrypted (`enc:vN:`). Requires `scos.privacy.crypto.secret`. Empty list = current plaintext behaviour.

```yaml
# privacy-masking.yml
audit-encrypt-fields:
  - taxIdentifier
```
```jsonc
// stored entityNew (JSONB)
{ "taxIdentifier": "enc:v1:9af1c2…", "name": "Empresa Válida" }
```

---

## Behaviour flags (`application.yml`)

```yaml
scos:
  privacy:
    enabled: true                                 # loads on import
    strict: false                                 # prod: error if hash/encrypt has no key
    max-payload-kb: 64                            # truncate before masking, bounds worst case
    masking:
      config-path: /etc/scos/privacy-masking.yml  # empty -> classpath
      default-patterns: true
    log:
      register-converter: true                    # %mask registered programmatically (see below)
    crypto:
      secret: ${SCOS_PRIVACY_CRYPTO_SECRET}       # prefer env / Vault / KMS
```

---

## Application log masking (`%mask` / `%maskmdc`)

Two Logback conversion rules let the engine mask the application's own log lines (the HTTP filter is a
separate surface that already masks request/response body and headers):

| Word | Pattern usage | Masks | Engine method |
|---|---|---|---|
| `mask` | `%mask(%msg)` | free text — wraps a sub-pattern | `maskText` |
| `maskmdc` | `%maskmdc{key}` | a single MDC value by key | `maskStructured` |

`ScosMaskingConverter` is a **composite** converter: it wraps the rendered child pattern (`%msg`), so use the
parenthesised form `%mask(%msg)` — `%mask` alone renders nothing. At log time the converter reads the shared
engine; during early startup (before the engine bean exists) it returns the text unchanged (fail-open).

### Step 1 — make the words available

The rules are registered **programmatically** at startup (`scos.privacy.log.register-converter=true`, the
default). But Logback in a Spring Boot app initialises **before** the Spring context, so when you keep your
pattern in a `logback-spring.xml`, declare the `<conversionRule>`s in the XML too — then they resolve at
parse time regardless of bean timing:

```xml
<conversionRule conversionWord="mask"
                converterClass="br.com.sawcunhaos.foundation.privacy.logback.ScosMaskingConverter"/>
<conversionRule conversionWord="maskmdc"
                converterClass="br.com.sawcunhaos.foundation.privacy.logback.ScosMaskingMdcConverter"/>
```

### Step 2 — reference `%mask` in the pattern

> **The masking only happens where the pattern uses `%mask`.** The default Spring Boot pattern (`%msg`)
> does **not** mask — you must change it to `%mask(%msg)`.

Full `logback-spring.xml` example:

```xml
<configuration>

    <conversionRule conversionWord="mask"
                    converterClass="br.com.sawcunhaos.foundation.privacy.logback.ScosMaskingConverter"/>
    <conversionRule conversionWord="maskmdc"
                    converterClass="br.com.sawcunhaos.foundation.privacy.logback.ScosMaskingMdcConverter"/>

    <appender name="Console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- %mask(%msg) masks the message; %maskmdc{cpf} masks one MDC key -->
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %mask(%msg) cpf=%maskmdc{cpf}%n</pattern>
        </encoder>
    </appender>

    <!-- Lowest request latency: wrap in AsyncAppender (bounded queue; never drop WARN/ERROR) -->
    <appender name="Async" class="ch.qos.logback.classic.AsyncAppender">
        <discardingThreshold>0</discardingThreshold>
        <queueSize>4096</queueSize>
        <appender-ref ref="Console"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="Async"/>
    </root>
</configuration>
```

```java
log.info("Validating CNPJ uniqueness: {}", taxId); // -> Validating CNPJ uniqueness: RTH*********63
```

With a `log-patterns` rule (`partial`, `keep-first: 3`, `keep-last: 2`) matching the CNPJ.

### Gotcha — `log-patterns` regex must not be anchored

`maskText` masks tokens **anywhere** in the message. A regex anchored with `^…$` only matches when the whole
message *is* the token, so it never fires on `"… cnpj=RTHSGCAN000163"`. Write it unanchored, using
lookarounds to avoid clipping a longer run:

```yaml
log-patterns:
  - regex: '(?<![A-Za-z0-9])(\d{14}|[A-Za-z0-9]{8}\d{6})(?![A-Za-z0-9])'
    strategy: partial
    keep-first: 3
    keep-last: 2
```

Builtin `cpf`/`cnpj` rules also run a **check-digit** validation, so they skip invalid (e.g. test/fake)
numbers — use an explicit `log-patterns` rule (no validator) when you need those masked too.

### Opt-out

Set `scos.privacy.log.register-converter=false` to skip the programmatic registration and wire the rules
yourself (e.g. a `logback-privacy.xml` included via `<include>`).

### JSON encoders (Logstash) — not covered by `%mask`

`net.logstash.logback.encoder.LogstashEncoder` serializes the `message` field directly, **bypassing the
pattern**, so `%mask` does not reach it. To mask the JSON message, emit it through a pattern provider:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <fieldNames><message>[ignore]</message></fieldNames>           <!-- disable default message -->
    <provider class="net.logstash.logback.composite.loggingevent.LoggingEventPatternJsonProvider">
        <pattern>{"message":"%mask(%msg)"}</pattern>
    </provider>
</encoder>
```

Likewise a bulk `%mdc` / `<mdc>` dump renders MDC raw; mask specific keys with `%maskmdc{key}` instead.

---

## Use outside the SCOS layer (no Spring)

```java
MaskingEngine engine = MaskingEngine.fromYaml(Path.of("/etc/scos/privacy-masking.yml"));
String safeLine = engine.maskText("Pessoa criada cpf=12345678901"); // -> cpf=***
String safeField = engine.maskStructured("cpf", "12345678901");      // -> *********01
```

---

## Audit at-rest encryption

Fields listed in `audit-encrypt-fields` are encrypted before persistence with `ScosFieldCipher`
(AES-256/GCM), producing self-describing `enc:vN:` tokens that embed the key id. Keys come from
`ScosCryptoKeyProvider` (default reads an externalized secret; plug Vault/KMS by providing your own bean).
Rotation affects only new records — history is decrypted by its stored key id and never re-encrypted.

---

## Performance

Immutable, lock-free engine built once at startup and shared across threads. Pre-compiled `Pattern`s, O(1)
key lookup, single-pass Aho-Corasick for literals, in-place JSON walk, a zero-allocation fast-path for lines
without PII, and ReDoS rejection at build time. JMH benchmarks live in `src/jmh` and run under `-Pperf`,
gated against `etc/perf/baseline.json`.
