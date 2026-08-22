# Story 3.8: Tornar a política de falha por exceção de negócio declarável por método

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor cujo endpoint pode falhar por regra de negócio,
Eu quero escolher entre `RELEASE` (permite retry) e `KEEP_FAILED` (evita duplicar efeito colateral),
Para adequar o comportamento ao meu caso de uso.

## Acceptance Criteria

1. **Given** um método anotado com a política `KEEP_FAILED`, **When** a execução lança exceção de negócio, **Then** o erro é gravado como resultado (chave não é removida).
2. **And** o comportamento padrão (`RELEASE`, remove a chave) é mantido para métodos sem a anotação explícita.

## Tasks / Subtasks

- [ ] Task 1: Confirmar o comportamento atual (contexto) (AC: #2)
  - [ ] **Confirmado por leitura direta**: `IdempotentAspect.execute()`, linhas 183-189 — `catch (Exception e) { idempotentRepository.remove(idempotencyKey); throw e; }`. Hoje **toda** exceção de negócio remove a chave incondicionalmente (equivalente ao futuro `RELEASE`), sem opção de manter o erro registrado. Não existe hoje nenhum enum ou atributo de anotação para esta política.
- [ ] Task 2: Criar o enum de política (AC: #1, #2)
  - [ ] Criar um enum (ex.: `IdempotentFailurePolicy` com valores `RELEASE`, `KEEP_FAILED`) em `utils/annotation/jdempotent/` (mesmo pacote de `JdempotentResource`, já que módulos `*-api`/anotações ainda não existem — este módulo é `utils`, migra para `jdempotent-api` só na Epic 1 Story 1.5, que já foi concluída; confirmar se `JdempotentResource` já vive no novo módulo `jdempotent-api` ou ainda em `utils` antes de decidir o pacote final)
  - [ ] Adicionar um atributo à anotação `@JdempotentResource` (ex.: `IdempotentFailurePolicy onBusinessException() default IdempotentFailurePolicy.RELEASE`) — `RELEASE` como default preserva o comportamento atual para métodos sem declaração explícita (AC #2)
- [ ] Task 3: Implementar a política `KEEP_FAILED` em `IdempotentAspect` (AC: #1)
  - [ ] No bloco `catch` de `execute()`, ler o atributo de política da anotação `@JdempotentResource` do método
  - [ ] Se `RELEASE` (ou ausente): manter o comportamento atual — `idempotentRepository.remove(idempotencyKey)`
  - [ ] Se `KEEP_FAILED`: em vez de remover a chave, gravar o erro como resultado usando o mesmo mecanismo de `setResponse`/`IdempotentResponseWrapper` já usado para respostas de sucesso (decidir se a exceção é serializada diretamente ou envolvida numa representação equivalente — manter consistência com o que `IdempotentResponseWrapper` já suporta)
- [ ] Task 4: Testes (AC: #1, #2)
  - [ ] Teste: método anotado com `KEEP_FAILED` lança exceção de negócio → confirmar que a chave permanece no repositório e uma chamada subsequente com a mesma chave recebe o erro gravado (não reexecuta o método)
  - [ ] Teste: método sem a anotação explícita de política (comportamento padrão) lança exceção → confirmar que a chave é removida (comportamento idêntico ao atual, sem regressão)

## Dev Notes

- Esta é uma mudança de comportamento **aditiva** (novo atributo com default que preserva o comportamento atual) — não deve quebrar nenhum consumidor existente que já usa `@JdempotentResource` sem essa configuração.
- **NFR4**: o comportamento padrão (`RELEASE`) deve ser bit-a-bit idêntico ao comportamento atual para não introduzir regressão silenciosa nos consumidores que não optarem por `KEEP_FAILED`.
- Não confundir com o `errorCallback`/`ErrorConditionalCallback` já existente (linhas 191-194 de `IdempotentAspect.execute()`) — esse mecanismo trata uma condição de erro **dentro de um resultado bem-sucedido** (não uma exceção lançada), é um caminho diferente do `catch (Exception e)` que esta story modifica. Não misturar os dois mecanismos.

### Project Structure Notes

- Arquivo modificado: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java`.
- Arquivo novo: enum de política de falha (local exato depende de onde `JdempotentResource` estiver após a Epic 1 — confirmar antes de criar).
- Arquivo modificado: a anotação `JdempotentResource` (novo atributo).

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java#L183-L194]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/annotation/jdempotent/JdempotentResource.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-38-tornar-a-política-de-falha-por-exceção-de-negócio-declarável-por-método]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
