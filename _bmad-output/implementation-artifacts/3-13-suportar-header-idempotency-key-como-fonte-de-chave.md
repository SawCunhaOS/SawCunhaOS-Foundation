# Story 3.13: Suportar header `Idempotency-Key` como fonte de chave

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor da API,
Eu quero enviar minha própria chave de idempotência via header,
Para controlar a chave sem depender só dos campos do corpo.

## Acceptance Criteria

1. **Given** uma requisição HTTP com o header `Idempotency-Key`, **When** `@JdempotentResource(keySource=HEADER_THEN_FIELDS, headerName="Idempotency-Key", onMismatch=CONFLICT)` está configurado, **Then** a precedência é header → campos anotados → hash, e `X-Request-ID` nunca é reutilizado para esse fim.
2. **And** para contexto de mensageria (sem contexto web), o resolver de chave devolve `null` de forma limpa, nunca lança exceção.

## Tasks / Subtasks

- [ ] Task 1: Confirmar o estado atual (contexto) (AC: #1)
  - [ ] **Confirmado por leitura direta**: `JdempotentResource` (`utils/annotation/jdempotent/JdempotentResource.java`) hoje só tem `cachePrefix()`, `ttl()`, `ttlTimeUnit()` — **nenhum atributo `keySource`/`headerName`/`onMismatch` existe ainda**. A composição de chave hoje é 100% via campos anotados + hash (`DefaultKeyGenerator`), sem nenhuma leitura de header HTTP em lugar nenhum do módulo `jdempotent`
- [ ] Task 2: Adicionar os novos atributos a `@JdempotentResource` (AC: #1)
  - [ ] Criar um enum `KeySource` (ex.: `FIELDS_ONLY` como default preservando comportamento atual, `HEADER_THEN_FIELDS`)
  - [ ] Adicionar `KeySource keySource() default KeySource.FIELDS_ONLY`, `String headerName() default ""`, e um enum/atributo `onMismatch` (ex.: `CONFLICT`) à anotação
- [ ] Task 3: Implementar a leitura do header no `IdempotencyKeyResolver` (Story 3.12) (AC: #1)
  - [ ] Quando `keySource == HEADER_THEN_FIELDS`, o `IdempotencyKeyResolver` tenta ler o header nomeado por `headerName` do contexto HTTP atual (via `RequestContextHolder`/`HttpServletRequest`, disponível quando há contexto web)
  - [ ] Precedência exigida pelo AC: **header → campos anotados → hash** — se o header estiver presente, ele é a fonte da chave; se ausente, cai para a composição por campos anotados (`@JdempotentProperty`, Story 3.12); o hash (`DefaultKeyGenerator`) continua sendo o mecanismo de fallback final quando nem header nem campos suficientes estão disponíveis
  - [ ] **Nunca reutilizar `X-Request-ID`** para este fim, mesmo que pareça conveniente — são conceitos diferentes (correlação de requisição vs. idempotência de negócio); confirmar que nenhum ponto do código introduzido nesta story lê `X-Request-ID` como fallback de `Idempotency-Key`
- [ ] Task 4: Resolver de chave limpo em contexto de mensageria (AC: #2)
  - [ ] O `IdempotencyKeyResolver` (Story 3.12) já foi desenhado para não depender de `ProceedingJoinPoint`/AOP diretamente — mas a leitura de header HTTP (`RequestContextHolder.getRequestAttributes()`) pode retornar `null` fora de um contexto de requisição web (ex.: consumidor de mensageria chamando o método anotado fora de um `DispatcherServlet`)
  - [ ] Garantir que, quando `RequestContextHolder` não tem contexto web disponível, a tentativa de leitura do header **retorna `null` de forma limpa** (o resolver cai para o próximo passo da precedência: campos anotados) — **nunca lançar exceção** (ex.: `NullPointerException`/`IllegalStateException`) só porque não há contexto web; isso é comportamento explicitamente exigido pelo AC #2, não um detalhe de implementação incidental
- [ ] Task 5: Testes (AC: #1, #2)
  - [ ] Teste HTTP: requisição com header `Idempotency-Key` presente → a chave resultante é derivada do header, não dos campos do corpo
  - [ ] Teste HTTP: requisição sem o header → cai para composição por campos anotados (comportamento da Story 3.12)
  - [ ] Teste de contexto não-web (simulando chamada de listener de mensageria, sem `RequestContextHolder` populado): confirmar que o resolver não lança exceção e retorna o resultado esperado da precedência (campos anotados, já que não há header disponível fora de contexto web)
  - [ ] Teste confirmando que `X-Request-ID` nunca é lido em nenhum ponto do fluxo de resolução de chave desta story

## Dev Notes

- **Depende diretamente da Story 3.12** (`IdempotencyKeyResolver` como ponto único de composição) — implementar esta story depois, reutilizando o resolver já extraído, não criando um caminho de resolução de header separado e paralelo.
- `onMismatch=CONFLICT` sugere uma política de comportamento quando o header e os campos anotados discordam entre si (ex.: cliente reenvia com o mesmo header mas payload diferente) — esta semântica se sobrepõe conceitualmente com a Story 3.6 (colisão de payload); ao implementar, decidir explicitamente se `onMismatch` desta story reusa o mecanismo de `422 PAYLOAD_MISMATCH` da 3.6 ou é um caminho de erro distinto, e documentar a decisão nas Completion Notes — não implicitamente assumir que são a mesma coisa sem verificar.
- **Ponytail**: não adicionar suporte a múltiplos headers alternativos configuráveis (ex.: lista de nomes de header a tentar em ordem) — o AC pede um único `headerName` string; suporte a múltiplos seria flexibilidade não pedida.

### Project Structure Notes

- Arquivo modificado: `utils/src/main/java/br/com/sawcunhaos/foundation/utils/annotation/jdempotent/JdempotentResource.java` (novos atributos).
- Arquivo novo: enum `KeySource` (mesmo pacote de `JdempotentResource`).
- Arquivo modificado: `IdempotencyKeyResolver` (Story 3.12).

### References

- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/annotation/jdempotent/JdempotentResource.java]
- [Source: _bmad-output/implementation-artifacts/3-12-introduzir-idempotencykeyresolver-com-composição-de-chave-de.md]
- [Source: _bmad-output/planning-artifacts/epics.md#story-313-suportar-header-idempotency-key-como-fonte-de-chave]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
