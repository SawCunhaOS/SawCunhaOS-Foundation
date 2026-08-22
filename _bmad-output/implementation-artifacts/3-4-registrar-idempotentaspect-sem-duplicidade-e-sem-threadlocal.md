# Story 3.4: Registrar `IdempotentAspect` sem duplicidade e sem `ThreadLocal`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero que o aspecto não duplique bean quando há mais de um ponto de registro, e não use `ThreadLocal` para `MessageDigest`,
Para evitar NPE latente sob carga com virtual threads.

## Acceptance Criteria

1. **Given** mais de um ponto de registro do aspecto no contexto Spring, **When** `IdempotentAspect` é registrado com `@ConditionalOnMissingBean`, **Then** apenas um bean existe.
2. **And** o uso de `ThreadLocal` para `MessageDigest` é removido.
3. **And** um teste cobre o cenário de "duplo `IdempotentAspect`".

## Tasks / Subtasks

- [ ] Task 1: Corrigir a duplicidade de bean em `ScosJdempotentConfig` (AC: #1)
  - [ ] **Bug real confirmado**: `ScosJdempotentConfig.java` declara dois métodos `@Bean` que retornam `IdempotentAspect` — `getIdempotentAspectOnErrorConditionalCallback()` (linha 46, `@ConditionalOnBean(ErrorConditionalCallback.class)`) e `getIdempotentAspect()` (linha 51, sem nenhuma condicional). Quando a aplicação consumidora registra um bean `ErrorConditionalCallback`, **ambos** os métodos são elegíveis e o Spring cria dois beans `IdempotentAspect` no contexto — nenhum dos dois tem `@ConditionalOnMissingBean`
  - [ ] Adicionar `@ConditionalOnMissingBean(IdempotentAspect.class)` a `getIdempotentAspect()` (o bean "default", sem callback de erro), garantindo que ele só é criado se o bean com callback não tiver sido criado primeiro
  - [ ] Confirmar a ordem de avaliação das condições do Spring (`@ConditionalOnBean` de um método é avaliado antes do `@ConditionalOnMissingBean` do outro na mesma fase de configuração) — se a ordem de declaração dos métodos na classe importar para o resultado, documentar isso explicitamente nas Completion Notes
- [ ] Task 2: Remover o `ThreadLocal<MessageDigest>` (AC: #2)
  - [ ] Em `IdempotentAspect.java`, linhas 89-107, o `ThreadLocal<MessageDigest>` existe para evitar recriar o `MessageDigest` (que não é thread-safe) a cada chamada — mas retenção de estado por thread é um padrão problemático sob **virtual threads** (Java 25/Spring Boot 4.1, conforme ADD-6): cada requisição pode rodar numa virtual thread nova, então o `ThreadLocal` deixa de ter benefício de reuso e ainda mantém referências que podem crescer sem bound conforme threads são criadas e descartadas
  - [ ] Substituir por instanciação direta de `MessageDigest.getInstance(CryptographyAlgorithm.SHA256.value())` a cada chamada de `execute()` (custo de criação de `MessageDigest` é baixo comparado ao I/O de Redis já presente no fluxo) — **não** introduzir um pool de `MessageDigest` nem outro mecanismo de cache: seria abstração não pedida pelo AC, que só pede a remoção do `ThreadLocal`
  - [ ] O `ThreadLocal<StringBuilder> stringBuilders` (linhas 73-86) **não está no escopo desta story** — o AC #2 cita especificamente `MessageDigest`; não remover o `StringBuilder` sem confirmação, para não misturar duas mudanças de comportamento não relacionadas no mesmo commit (NFR2)
- [ ] Task 3: Teste de duplo `IdempotentAspect` (AC: #3)
  - [ ] Escrever um teste de contexto Spring que registra um bean `ErrorConditionalCallback` e carrega `ScosJdempotentConfig`, então confirma via `applicationContext.getBeansOfType(IdempotentAspect.class)` que **exatamente um** bean existe
  - [ ] Este teste deve **falhar contra o código atual** (hoje produz dois beans) e passar após a correção — reproduz o disaster antes do fix, no espírito do NFR5 mesmo não sendo formalmente exigido por este epic

## Dev Notes

- Ambos os bugs (duplicidade de bean e uso de `ThreadLocal`) são **confirmados por leitura direta** do código-fonte, não hipotéticos: `ScosJdempotentConfig.java` (linhas 40-53) e `IdempotentAspect.java` (linhas 89-107).
- `@ConditionalOnMissingBean(IdempotentAspect.class)` é o mecanismo padrão do Spring Boot para este exato problema — não é necessário nenhum código customizado de deduplicação.
- **NFR2**: as duas correções (duplicidade de bean, remoção de `ThreadLocal`) são comportamentos distintos — considerar 2 commits separados se a granularidade do time exigir rastreabilidade fina, mas ambos cabem nesta única story porque os dois ACs (#1 e #2) pertencem ao mesmo FR29.

### Project Structure Notes

- Arquivos modificados: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/configuration/ScosJdempotentConfig.java`, `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java`.

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/configuration/ScosJdempotentConfig.java]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java#L89-L107]
- [Source: _bmad-output/planning-artifacts/epics.md#story-34-registrar-idempotentaspect-sem-duplicidade-e-sem-threadlocal]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
