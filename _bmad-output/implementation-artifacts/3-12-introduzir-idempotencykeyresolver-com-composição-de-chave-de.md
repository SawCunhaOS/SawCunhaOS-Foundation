# Story 3.12: Introduzir `IdempotencyKeyResolver` com composição de chave declarativa

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor definindo minha chave de idempotência,
Eu quero selecionar explicitamente os campos via `@JdempotentProperty`, com serialização canônica,
Para ter uma chave determinística independente da ordem dos campos.

## Acceptance Criteria

1. **Given** uma classe anotada com múltiplos `@JdempotentProperty`, **When** a chave é composta via `IdempotencyKeyResolver` usando serialização canônica (`TreeMap`), **Then** a chave resultante é determinística e `@JdempotentId` deixa de compor a chave.
2. **And** o mesmo `IdempotencyKeyResolver` é o único ponto de composição de chave, reutilizável por qualquer entrypoint futuro (HTTP ou mensageria).

## Tasks / Subtasks

- [ ] Task 1: Confirmar o estado atual (contexto) (AC: #1)
  - [ ] **`@JdempotentProperty` já existe** (`utils/annotation/jdempotent/JdempotentProperty.java`, atributo `value()`) e já é processado por `JdempotentPropertyAnnotationChain` — este AC não introduz a anotação do zero, aproveita a que já existe.
  - [ ] **Confirmado por leitura direta**: a cadeia de resolução (`JdempotentIgnoreAnnotationChain` → `JdempotentPropertyAnnotationChain` → `JdempotentDefaultChain`, montada em `IdempotentAspect.fillChains()`) **não tem nenhum elo que trate `@JdempotentId` de forma especial** — um campo anotado só com `@JdempotentId` (sem `@JdempotentIgnore`) cai no `JdempotentDefaultChain`, que usa o valor do campo normalmente na composição da chave. Ou seja, **hoje `@JdempotentId` de fato compõe a chave** (confirma a premissa do FR8 de que isso precisa parar) — `@JdempotentId` deveria servir só para receber de volta o valor gerado (via `IdempotentAspect.setJdempotentId()`, linha 278), não para influenciar o hash de entrada
- [ ] Task 2: Excluir `@JdempotentId` da composição de chave (AC: #1)
  - [ ] Adicionar um elo à cadeia (ou modificar `JdempotentDefaultChain`) que detecta `@JdempotentId` e retorna uma `KeyValuePair` vazia (mesmo padrão de retorno usado por `JdempotentIgnoreAnnotationChain` para campos ignorados), para que esse campo nunca entre na composição do hash
- [ ] Task 3: Criar `IdempotencyKeyResolver` como ponto único de composição (AC: #1, #2)
  - [ ] Extrair a lógica hoje espalhada entre `IdempotentAspect.getIdempotentNonIgnorableWrapper()` (coleta de campos + chain) e `DefaultKeyGenerator.generateIdempotentKey()` (hash + prefixo) para uma classe `IdempotencyKeyResolver` dedicada, que se torna o único lugar que sabe como transformar um `IdempotentRequestWrapper` numa `IdempotencyKey`
  - [ ] Usar serialização canônica via `TreeMap` (ordenação determinística por chave) ao montar o material a ser hasheado a partir dos `KeyValuePair` coletados pela `annotationChain` — hoje a ordem de iteração de `getDeclaredFields()` não é garantida entre execuções/JVMs da mesma forma que uma estrutura ordenada garante; a serialização canônica elimina essa fonte de não-determinismo
  - [ ] `IdempotentAspect.execute()` passa a chamar `IdempotencyKeyResolver` em vez de orquestrar diretamente `getIdempotentNonIgnorableWrapper()` + `keyGenerator.generateIdempotentKey()`
  - [ ] O `IdempotencyKeyResolver` deve ser desenhado sem acoplamento a `ProceedingJoinPoint`/AspectJ — ele recebe dados já extraídos (ex.: `IdempotentRequestWrapper`, `listenerName`), não o join point do AOP. Isto é o que garante o AC #2: o mesmo resolver poder ser reutilizado por um futuro entrypoint de mensageria (que não tem `ProceedingJoinPoint`), sem duplicar a lógica de composição de chave
- [ ] Task 4: Testes (AC: #1, #2)
  - [ ] Teste: uma classe com múltiplos campos `@JdempotentProperty` gera a mesma chave independente da ordem de declaração dos campos na classe (prova de determinismo via `TreeMap`)
  - [ ] Teste: um campo anotado só com `@JdempotentId` **não** influencia o hash resultante (compara a chave gerada com e sem esse campo presente, mesmos demais campos — deve ser idêntica)
  - [ ] Teste: `IdempotencyKeyResolver` invocado diretamente (sem passar por `IdempotentAspect`/AOP) produz o mesmo resultado que o caminho via aspecto — prova de que é reutilizável fora do contexto HTTP/AOP

## Dev Notes

- Esta story depende da Story 1.5 do Epic 1 (módulo `jdempotent-api`, onde as anotações `@Jdempotent*` podem ter migrado) — confirmar o pacote atual de `JdempotentProperty`/`JdempotentId` no momento da implementação; ver `_bmad-output/implementation-artifacts/1-5-criar-módulos-api-e-mover-as-anotações-de-contrato.md` para o inventário de destino.
- Esta story também é pré-requisito direto da Story 3.13 (header `Idempotency-Key`), que exige "o mesmo `IdempotencyKeyResolver` é o único ponto de composição de chave, reutilizável por qualquer entrypoint" (ADD-4) — implementar 3.12 antes de 3.13.
- **NFR4**: extrair a lógica existente para uma classe nova não deve mudar o valor de hash resultante para o caso comum (campos sem `@JdempotentId` presentes) — só o caso com `@JdempotentId` muda de comportamento, que é exatamente o AC desta story.

### Project Structure Notes

- Arquivo novo: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/generator/IdempotencyKeyResolver.java` (ou pacote equivalente).
- Arquivos modificados: `IdempotentAspect.java` (usa o resolver em vez de orquestrar diretamente), cadeia de anotações (exclusão de `@JdempotentId`).

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java#L162-L201,L293-L322]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/chain/JdempotentPropertyAnnotationChain.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/annotation/jdempotent/JdempotentProperty.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/annotation/jdempotent/JdempotentId.java]
- [Source: _bmad-output/implementation-artifacts/1-5-criar-módulos-api-e-mover-as-anotações-de-contrato.md]
- [Source: _bmad-output/planning-artifacts/epics.md#story-312-introduzir-idempotencykeyresolver-com-composição-de-chave-declarativa]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
