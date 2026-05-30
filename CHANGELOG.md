# Changelog

All notable changes to SCOS Foundation are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/) and this project adheres to semantic versioning.

## [Unreleased]

### Added — `scos-foundation-privacy` module

- New base-layer module **`scos-foundation-privacy`** providing a high-performance, multithread-safe
  `MaskingEngine` (immutable, lock-free, pre-compiled patterns, O(1) key lookup, single-pass Aho-Corasick,
  in-place JSON walk, zero-allocation fast-path, ReDoS rejection at build time).
- **YAML-driven rules** (`privacy-masking.yml`): masking fields and strategies are configuration data — no
  database, no recompile. Source resolution: external `scos.privacy.masking.config-path`
  (or env `SCOS_PRIVACY_MASKING_CONFIG`) → classpath fallback → builtins + WARN.
- **Portable core**: `MaskingEngine.fromYaml(Path|InputStream)` runs outside the SCOS layer, without Spring.
- **Masking strategies** as a closed enum: `fixed`, `partial`, `email`, `hash` (keyed HMAC), `encrypt`
  (reversible), `redact`; validated at startup with a `partial` fail-safe.
- **Builtins by country/region** as embedded data packs: `generic`, `br`, `us`, `eu`, `uk`, `in`; enable by
  list, disable individual items; check-digit validation for `cpf`/`cnpj`/`credit-card`.
- **Auto-configuration** via `AutoConfiguration.imports` — loads on import, every bean
  `@ConditionalOnMissingBean`; optional `DataMaskingValues` SPI added on top of the YAML
  (precedence builtins → YAML → SPI).
- **Application-log masking**: Logback `%mask` converter registered programmatically (no `logback.xml`
  edit); opt out via `scos.privacy.log.register-converter=false`.
- **Audit at-rest encryption**: `ScosFieldCipher` (AES-256/GCM) with `enc:vN:` tokens that embed the key id;
  `ScosCryptoKeyProvider` SPI (default externalized secret; plug Vault/KMS). Key is versioned per record;
  history is not re-encrypted.

### Changed

- **BREAKING** (planned): masking moves from package `br.com.sawcunhaos.foundation.utils.lgpd` to
  `br.com.sawcunhaos.foundation.privacy`. Deprecated facades in the old package will delegate for one
  release. `utils` and `audit` gain a dependency on `scos-foundation-privacy`; the parent POM lists
  `privacy` before `utils`.
- Recommended way to declare masking rules changes from implementing `DataMaskingValues` in code to the
  `privacy-masking.yml` file; the SPI remains as an optional override.

### Notes

- Audit JSONB columns may contain encrypted values (`enc:vN:`) for fields opted in via
  `audit-encrypt-fields`; reading them directly in the database requires decryption. The feature is opt-in
  per field — an empty list keeps current behaviour.
