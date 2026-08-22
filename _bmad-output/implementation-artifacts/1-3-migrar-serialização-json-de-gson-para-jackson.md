# Story 1.3: Migrar serialização JSON de Gson para Jackson

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero que toda a base use Jackson em vez de Gson,
Para eliminar duplicidade de serializadores antes da decomposição modular.

## Acceptance Criteria

1. **Given** `GsonUtils` e os 3 adapters de `java.time` hoje em uso, **When** a migração é concluída, **Then** `GsonUtils` e os adapters são removidos.
2. **And** `JsonMasker` (módulo `privacy`) e os 4 pontos de uso em `audit` passam a usar Jackson.
3. **And** a política de nulos é preservada via `@JsonInclude` documentado.
4. **And** revisão manual confirma que `JsonMasker` continua um único loop O(n).
5. **And** antes da migração, é verificado se existe hash-chain persistida em ambiente piloto no `audit` (OQ-4 do PRD) — a migração muda o hash calculado sobre o JSON serializado, invalidando cadeias existentes se não forem removidas/reprocessadas antes.
6. **And** o inventário de classes com serialização customizada (`TypeAdapter` Gson próprio) é levantado além dos 3 adapters de `java.time` já conhecidos — incluindo os value objects `Cpf`/`Cnpj`/`Email`/`TaxIdentifier` (ainda em `utils` neste ponto, migram para `validation` só na Story 1.9) — e cada um migrado tem teste confirmando que não serializa como `{}` vazio (Jackson exige getter/anotação onde Gson refletia campo privado direto).
7. **And** um teste de determinismo garante que a serialização usada pelo hash-chain do `audit` produz o mesmo resultado entre execuções (ordem de campos, formatação de número) — não só no corte pontual do OQ-4, mas como invariante contínua.
8. **And** o caminho de travessia genérica de JSON do `JsonMasker` (`Map<String,Object>`) é testado quanto a tipos numéricos (`Integer`/`Long`/`BigDecimal` do Jackson vs. `Double`/`LazilyParsedNumber` do Gson).
9. **And** os 4 pontos de uso em `audit` têm seu tratamento de exceção revisado — `catch` de `JsonSyntaxException` (Gson, unchecked) é substituído pelo equivalente Jackson (`JsonProcessingException`, checked), preservando o comportamento de fallback/retry existente.
10. **And** a mudança de formato de data (Jackson `jackson-datatype-jsr310` vs. os 3 adapters customizados removidos) é registrada no CHANGELOG como mudança de formato de wire.

## Tasks / Subtasks

- [ ] Task 0 — pré-condição bloqueante (AC: #5): Verificar se existe hash-chain persistida em ambiente piloto do `audit`
  - [ ] Consultar quem mantém o ambiente piloto (OQ-4 do PRD) antes de prosseguir; se existir cadeia persistida, ela precisa ser removida/reprocessada **antes** desta migração — não depois, porque o Jackson muda a serialização e portanto o hash calculado sobre ela
  - [ ] Só prosseguir com as tasks abaixo após essa confirmação
- [ ] Task 1: Remover `GsonUtils` e os adapters de `java.time` (AC: #1)
  - [ ] Remover `utils/src/main/java/.../utils/GsonUtils.java`
  - [ ] Remover `utils/src/main/java/.../adapter/LocalDateAdapter.java`, `LocalDateTimeAdapter.java`, `LocalTimeAdapter.java`
  - [ ] Remover os testes correspondentes: `adapter/LocalDateAdapterTest.java`, `LocalDateTimeAdapterTest.java`, `LocalTimeAdapterTest.java`
  - [ ] Não migrar a lógica dos adapters — Jackson trata `java.time` nativamente via `jackson-datatype-jsr310`, já registrado no `ScosJacksonConfig` do projeto
- [ ] Task 2: Migrar `JsonMasker` (`privacy`) para Jackson (AC: #2, #4, #8)
  - [ ] Reescrever `privacy/src/main/java/br/com/sawcunhaos/foundation/privacy/core/JsonMasker.java` trocando `new Gson()`/`JsonElement`/`JsonObject` por `JsonNode`/`ObjectNode` do Jackson
  - [ ] Preservar a característica de **um único loop O(n)** sobre a estrutura — não introduzir passo de conversão intermediário nem segunda travessia
  - [ ] Adicionar teste cobrindo tipos numéricos: confirmar que o mascaramento trata corretamente `Integer`/`Long`/`BigDecimal` do Jackson (o Gson usava `Double`/`LazilyParsedNumber` — nenhuma checagem de tipo pode depender do comportamento antigo)
- [ ] Task 3: Migrar os 4 pontos de uso em `audit` (AC: #2, #9)
  - [ ] `audit/src/main/java/.../service/ScosAuditHashService.java` — trocar `JsonParser`/`JsonObject` (Gson) por Jackson
  - [ ] `audit/src/main/java/.../service/ScosAuditServiceBean.java` — trocar `toJson(stateMap)` por serialização Jackson
  - [ ] `audit/src/main/java/.../service/ScosAuditBatchConsumer.java` — trocar `toJson` do evento para DLQ por Jackson
  - [ ] `audit/src/main/java/.../service/ScosAuditDlqJob.java` — trocar `fromJson` por desserialização Jackson (lê o formato gravado pelo item acima)
  - [ ] Em cada um dos 4, trocar `catch (JsonSyntaxException ...)` (unchecked) pelo `catch` de `JsonProcessingException`/`tools.jackson.core.JacksonException` (checked) equivalente, preservando o comportamento de fallback/retry hoje existente — não silenciar nem mudar o efeito colateral do catch, só o tipo capturado
- [ ] Task 4: Política de nulos explícita (AC: #3)
  - [ ] Definir `@JsonInclude` explicitamente no `ObjectMapper`/DTOs usados pelo payload de auditoria (Gson omite nulos por padrão; Jackson inclui por padrão — comportamento muda se não for fixado)
  - [ ] Documentar a política escolhida no README do módulo `audit`
- [ ] Task 5: Inventário de serialização customizada além dos adapters conhecidos (AC: #6)
  - [ ] Levantar se `Cpf`/`Cnpj`/`Email`/`TaxIdentifier` (ainda em `utils/src/main/java/.../valueobjects/`) têm `TypeAdapter` Gson próprio; se sim, migrar e adicionar teste confirmando que não serializam como `{}` vazio (getter/anotação Jackson explícita onde o Gson refletia campo privado direto)
- [ ] Task 6: Teste de determinismo do hash-chain (AC: #7)
  - [ ] Escrever teste que serializa o mesmo objeto múltiplas vezes e confirma resultado byte-idêntico (ordem de campos, formatação de número) — invariante contínua, não só checagem pontual da migração
- [ ] Task 7: Limpeza de dependência e CHANGELOG (AC: #1, #10)
  - [ ] Remover `gson` de `utils/pom.xml` e de `privacy/pom.xml`
  - [ ] Registrar no CHANGELOG a mudança de formato de data como mudança de **formato de wire**, não só detalhe interno do hash-chain

## Dev Notes

- **Sequenciamento obrigatório (NFR1)**: esta story só pode rodar depois que o hash-chain do `audit` for confirmado como não-persistido em piloto (Task 0) — o plano de origem é explícito: "remover o hash-chain primeiro, migrar o serializador depois; na ordem inversa, você quebra uma coisa que ia ser removida de qualquer jeito". Se o hash-chain ainda estiver ativo e não puder ser removido antes, **parar e escalar** — não prosseguir com a migração silenciosamente.
- Fonte primária desta story (contexto e armadilhas já mapeados, não re-investigar): `etc/doc/plano/plano-decomposicao-utils.md`, Seção 6.1 "Remoção do Gson (decidido)".
- Único Jackson no repo: `tools.jackson.*` (Jackson 3) — não há Jackson 2 para conciliar, o único conflito de stack é Gson × Jackson.
- 5 pontos de uso do Gson fora do `utils`: `privacy/core/JsonMasker.java` (Gson próprio, por design, para não depender de `utils` — ao migrar, a razão original de isolamento desaparece, mas o módulo continua sem depender de `utils`, só passa a usar Jackson diretamente); `audit/service/ScosAuditHashService.java`; `ScosAuditServiceBean.java`; `ScosAuditBatchConsumer.java`; `ScosAuditDlqJob.java`.
- **NFR5 não se aplica aqui** (é só para bugs do Épico 2) — mas o espírito de "teste antes do fix" vale para os dois pontos de risco reais desta story: determinismo do hash e serialização de tipo numérico, cobertos nas Tasks 6 e 2.

### Project Structure Notes

- Arquivos removidos: `GsonUtils.java`, `LocalDateAdapter.java`, `LocalDateTimeAdapter.java`, `LocalTimeAdapter.java` e seus 3 testes, todos em `utils/src/main/java/br/com/sawcunhaos/foundation/utils/`.
- Arquivos modificados: `privacy/src/main/java/br/com/sawcunhaos/foundation/privacy/core/JsonMasker.java`, os 4 arquivos em `audit/src/main/java/br/com/sawcunhaos/foundation/audit/service/`.
- Dependência `gson` removida de `utils/pom.xml` e `privacy/pom.xml`.
- Esta story roda **antes** da criação de qualquer módulo novo (Fase 1.5 do plano de origem, entre a limpeza da Story 1.2 e a devolução das anotações da Story 1.5) — apaga classes e remove dependência, então a decomposição posterior move menos coisa.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#61-remoção-do-gson-decidido]
- [Source: privacy/src/main/java/br/com/sawcunhaos/foundation/privacy/core/JsonMasker.java]
- [Source: audit/src/main/java/br/com/sawcunhaos/foundation/audit/service/ScosAuditHashService.java]
- [Source: audit/src/main/java/br/com/sawcunhaos/foundation/audit/service/ScosAuditServiceBean.java]
- [Source: audit/src/main/java/br/com/sawcunhaos/foundation/audit/service/ScosAuditBatchConsumer.java]
- [Source: audit/src/main/java/br/com/sawcunhaos/foundation/audit/service/ScosAuditDlqJob.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-13-migrar-serialização-json-de-gson-para-jackson]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
