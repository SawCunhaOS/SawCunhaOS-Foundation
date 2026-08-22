# Story 3.3: Resolver campos anotados em toda a hierarquia de classes

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor com hierarquia de herança nos DTOs,
Eu quero que todos os campos anotados componham a chave,
Para não ter colisão por campo herdado ignorado.

## Acceptance Criteria

1. **Given** uma classe com campos anotados herdados de uma superclasse, **When** a chave é composta, **Then** a resolução percorre toda a hierarquia (não apenas `getDeclaredFields()`).

## Tasks / Subtasks

- [ ] Task 1: Corrigir a coleta de campos em `IdempotentAspect.getIdempotentNonIgnorableWrapper()` (AC: #1)
  - [ ] Em `IdempotentAspect.java`, linha 296, `Field[] declaredFields = arg.getClass().getDeclaredFields();` só enxerga os campos declarados na classe concreta do argumento — campos declarados em superclasses (via `extends`) nunca entram no loop e portanto nunca passam pela `annotationChain` (`JdempotentIgnoreAnnotationChain` → `JdempotentPropertyAnnotationChain` → `JdempotentDefaultChain`)
  - [ ] Substituir por uma coleta que percorre `arg.getClass()` e todos os `getSuperclass()` até `Object.class`, concatenando os `getDeclaredFields()` de cada nível
  - [ ] Preservar o comportamento atual para campos da própria classe (não reordenar nem deduplicar além do necessário para evitar processar o mesmo campo duas vezes em caso de shadowing de nome entre subclasse e superclasse)
- [ ] Task 2: Teste com hierarquia de herança (AC: #1)
  - [ ] Criar um DTO de teste com uma superclasse tendo um campo anotado (`@JdempotentProperty` ou campo simples sem anotação, conforme o comportamento padrão da `JdempotentDefaultChain`) e confirmar que o valor da superclasse aparece na chave composta
  - [ ] Cobrir também o caminho `setJdempotentId()` (linha 281, mesmo padrão de `getDeclaredFields()` sem herança) **apenas se o epics.md ou o dono do requisito confirmar que também deve percorrer hierarquia** — o AC desta story menciona explicitamente "a chave é composta", que é o caminho de `getIdempotentNonIgnorableWrapper()`; `setJdempotentId()` tem outro propósito (gravar o valor gerado de volta no campo `@JdempotentId`) e não foi citado no AC — não estender o escopo sem confirmação

## Dev Notes

- **Confirmado por leitura direta**: `IdempotentAspect.java` usa `arg.getClass().getDeclaredFields()` em dois pontos (linha 281, dentro de `setJdempotentId()`, e linha 296, dentro de `getIdempotentNonIgnorableWrapper()`). O AC desta story cobre explicitamente a composição da chave (linha 296) — o outro uso (linha 281) é uma dúvida em aberto documentada na Task 2, não assumida como dentro do escopo.
- A cadeia de responsabilidade (`AnnotationChain`) já processa qualquer `Field` que receber — o bug não está na cadeia, está na coleta de campos que alimenta a cadeia. Corrigir na origem (coleta), não na cadeia.
- **Ponytail**: não introduzir cache de reflection (ex.: `Map<Class<?>, List<Field>>`) nesta story — não foi pedido no AC e adiciona estado mutável estático sem necessidade comprovada; se performance de reflection repetida virar problema medido, isso é uma otimização a discutir depois, não uma antecipação aqui.

### Project Structure Notes

- Arquivo modificado: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java`.

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java#L293-L310]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/chain/JdempotentDefaultChain.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-33-resolver-campos-anotados-em-toda-a-hierarquia-de-classes]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
