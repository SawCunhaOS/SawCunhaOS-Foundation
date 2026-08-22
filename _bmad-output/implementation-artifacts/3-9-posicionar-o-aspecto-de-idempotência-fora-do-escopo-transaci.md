# Story 3.9: Posicionar o aspecto de idempotência fora do escopo transacional

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor usando `@Transactional`,
Eu quero que um rollback de transação não deixe a chave de idempotência órfã no Redis,
Para não bloquear indevidamente uma nova tentativa legítima.

## Acceptance Criteria

1. **Given** um método anotado com `@Jdempotent*` e `@Transactional` que sofre rollback, **When** o `@Order` do aspecto está fora do escopo do `@Transactional`, **Then** a chave não fica presa no Redis após o rollback.

## Tasks / Subtasks

- [ ] Task 1: Confirmar a lacuna atual (contexto) (AC: #1)
  - [ ] **Confirmado por leitura direta**: `IdempotentAspect` (`@Aspect`, `@Around` em `execute()`) **não declara nenhum `@Order`** — busca por `@Order`/`Ordered.` em todo `jdempotent/src/main/java` não encontrou nenhuma ocorrência. Sem `@Order` explícito, a ordem de execução entre o aspecto de idempotência e o interceptor transacional do Spring (`@Transactional`, que tem sua própria ordem de AOP, tipicamente próxima de `Ordered.LOWEST_PRECEDENCE` por padrão) fica sujeita à ordem de registro dos beans — não determinística o suficiente para garantir a semântica exigida por este AC
  - [ ] O risco concreto: se o aspecto de idempotência executa **dentro** do escopo da transação (interceptor transacional "por fora", aspecto de idempotência "por dentro"), um rollback da transação desfaz efeitos de banco mas **não desfaz** a chave já gravada no Redis pelo `IdempotentAspect` (Redis não participa da transação JDBC/JPA) — a chave fica presa até o TTL expirar, bloqueando uma nova tentativa legítima do mesmo consumidor
- [ ] Task 2: Definir e aplicar o `@Order` correto (AC: #1)
  - [ ] Adicionar `@Order` explícito a `IdempotentAspect` com um valor que garanta que ele executa **fora** (antes/por volta, "outer") do interceptor transacional — ou seja, o aspecto de idempotência deve envolver a chamada de forma que, se a transação sofrer rollback, o bloco `catch` do próprio `IdempotentAspect.execute()` (linhas 183-189, que já faz `idempotentRepository.remove(idempotencyKey)` em caso de exceção) seja alcançado **depois** que o rollback já aconteceu — ou seja, a exceção que dispara o rollback precisa se propagar através do `catch` do aspecto, não ser interceptada antes
  - [ ] Valor concreto de `@Order`: usar um número menor que a ordem do interceptor transacional padrão do Spring (`AbstractTransactionAttributeSource`/`TransactionInterceptor`, tipicamente `Ordered.LOWEST_PRECEDENCE` a menos que sobrescrito) — números menores em `@Order` executam "por fora" no Spring AOP (entram primeiro, saem por último), então o aspecto de idempotência precisa de uma ordem **menor** (maior precedência) que o interceptor transacional
- [ ] Task 3: Teste de rollback (AC: #1)
  - [ ] Escrever um teste de integração com um método `@Transactional` + `@JdempotentResource` que lança uma exceção de negócio causando rollback, e confirmar que a chave correspondente **não existe mais** no repositório (`InMemoryIdempotentRepository`/`RedisIdempotentRepository`, via Testcontainers) logo após o rollback — este teste deve **falhar contra o código atual** (sem `@Order`) antes de aplicar a correção, se o comportamento atual realmente demonstrar o problema; documentar o resultado observado mesmo que ele já funcione por acaso na ordem de registro atual (nesse caso, o `@Order` explícito ainda é necessário para tornar o comportamento determinístico, não dependente de acidente de registro de beans)

## Dev Notes

- Esta story interage com a Story 3.4 (registro do bean `IdempotentAspect` via `@ConditionalOnMissingBean`) e com a Story 3.8 (política `RELEASE`/`KEEP_FAILED` no mesmo bloco `catch`) — as três tocam a mesma região de `ScosJdempotentConfig`/`IdempotentAspect.execute()`. Coordenar a ordem de implementação para não conflitar em merge (sugestão: 3.4 → 3.8 → 3.9, já que 3.9 só adiciona `@Order`, não muda a lógica do `catch` em si).
- **Atenção**: se a Story 3.5 (lock atômico) já estiver implementada quando esta story for feita, o "remove no catch" pode ter mudado de forma/local — reconferir contra o código real no momento da implementação, não assumir que as linhas 183-189 ainda existem exatamente como descritas aqui.
- Não introduzir um mecanismo de compensação/two-phase-commit entre Redis e o banco — fora de escopo, não pedido pelo AC. A solução é puramente de ordenação de AOP.

### Project Structure Notes

- Arquivo modificado: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java` (adição de `@Order`).

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-39-posicionar-o-aspecto-de-idempotência-fora-do-escopo-transacional]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
