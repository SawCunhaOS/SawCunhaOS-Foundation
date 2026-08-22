# Story 3.2: Migrar hashing de MD5 para SHA-256

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero hashing criptograficamente mais forte,
Para reduzir risco de colisão de hex.

## Acceptance Criteria

1. **Given** o hashing atual via MD5 e `Integer.toHexString`, **When** a migração é concluída, **Then** o hashing usa SHA-256 via enum `CryptographyAlgorithm` e a formatação hex usa `HexFormat.of().formatHex()`.
2. **And** um teste de colisão de hex (payloads escolhidos para produzir hashes diferentes) cobre a nova implementação.

## Tasks / Subtasks

- [ ] Task 1: Trocar o algoritmo de hash em `IdempotentAspect` (AC: #1)
  - [ ] Em `IdempotentAspect.java`, o `ThreadLocal<MessageDigest>` (linhas 89-107) hoje chama `MessageDigest.getInstance(CryptographyAlgorithm.MD5.value())` — trocar para `CryptographyAlgorithm.SHA256.value()`
  - [ ] O enum `CryptographyAlgorithm` (em `jdempotent/core/constant/CryptographyAlgorithm.java`) **já contém** o valor `SHA256("SHA-256")` — não é necessário criar o enum, só usar o valor que já existe
- [ ] Task 2: Trocar a formatação hex em `DefaultKeyGenerator` (AC: #1)
  - [ ] Em `DefaultKeyGenerator.generateIdempotentKey()` (linha 60-62), o loop `for (byte b : digest) { builder.append(Integer.toHexString(0xFF & b)); }` produz hex **sem zero-padding** (ex.: byte `0x0F` vira `"f"`, não `"0f"` — risco real de colisão textual entre bytes diferentes que gera exatamente a ambiguidade que a Story pretende eliminar)
  - [ ] Substituir por `HexFormat.of().formatHex(digest)`, que já produz a string hex completa com zero-padding em uma chamada — elimina o loop manual inteiro
- [ ] Task 3: Achado — propriedade morta relacionada a este hashing (AC: #1, não bloqueante para esta story)
  - [ ] `ConfigUtility.java` (`jdempotent/core/config/ConfigUtility.java`) declara `@Value("${scos.jdempotent.cryptography.algorithm:md5}") private String algorithm;` — esse campo **não tem getter e não é lido em nenhum lugar do código** (`DefaultKeyGenerator` ignora essa propriedade e usa o algoritmo hardcoded via `CryptographyAlgorithm`). Não resolver aqui — documentar como candidato a "propriedade morta" para a Story 3.14 (`@ConfigurationProperties`), que trata explicitamente da remoção de propriedades mortas
- [ ] Task 4: Teste de colisão de hex (AC: #2)
  - [ ] Escrever teste com dois payloads que, sob a formatação antiga (`Integer.toHexString` sem padding), produziriam prefixos de hex ambíguos, e confirmar que a nova formatação (`HexFormat`) produz hashes SHA-256 completos e distintos

## Dev Notes

- **Confirmado por leitura direta**: `IdempotentAspect.java` linha 94 usa `CryptographyAlgorithm.MD5.value()`; `DefaultKeyGenerator.java` linha 61 usa `Integer.toHexString(0xFF & b)` sem `String.format("%02x", ...)` nem equivalente — o padding realmente falta.
- O enum `CryptographyAlgorithm` já é mais rico do que o epics.md sugere: além de `MD5`/`SHA256`, também tem `SHA1`. Não introduzir seleção dinâmica de algoritmo nesta story (isso seria escopo especulativo não pedido) — trocar direto para `SHA256` hardcoded, mantendo o mesmo padrão de uso atual (um único algoritmo fixo no código).
- O `ThreadLocal<MessageDigest>` em si (uso de `ThreadLocal` para armazenar o `MessageDigest`) é tratado pela Story 3.4 (FR29), não por esta — não remover o `ThreadLocal` aqui, só trocar o algoritmo dentro dele.
- **NFR4**: esta story não altera a assinatura de `generateIdempotentKey` nem introduz configuração nova — troca mecânica de algoritmo e formatação.

### Project Structure Notes

- Arquivos modificados: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java`, `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/generator/DefaultKeyGenerator.java`.
- Nenhum arquivo novo é criado (o enum `CryptographyAlgorithm` já existe com o valor necessário).

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java#L89-L107]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/generator/DefaultKeyGenerator.java#L46-L65]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/constant/CryptographyAlgorithm.java]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/config/ConfigUtility.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-32-migrar-hashing-de-md5-para-sha-256]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
