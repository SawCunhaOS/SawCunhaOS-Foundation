# Story 3.15: Corrigir TTL e `equals`/`hashCode`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero que o TTL configurado seja sempre respeitado e que os objetos de requisição tenham `equals`/`hashCode` corretos,
Para eliminar comportamento inconsistente entre construtores.

## Acceptance Criteria

1. **Given** os 7 construtores de `InMemoryIdempotentRepository` (hoje 4 ignoram o TTL configurado), **When** a correção é aplicada, **Then** todos os construtores respeitam o TTL.
2. **And** `equals`/`hashCode` de `IdempotentRequestWrapper` passam a ser reflexivos e simétricos.

## Tasks / Subtasks

- [ ] Task 1: **Discrepância confirmada entre o epics.md e o código real — ler antes de implementar** (AC: #1)
  - [ ] O epics.md descreve "os 7 construtores de `InMemoryIdempotentRepository` (hoje 4 ignoram o TTL configurado)". **Por leitura direta do código atual, isso não bate**: `InMemoryIdempotentRepository.java` tem **apenas 1 construtor** (`public InMemoryIdempotentRepository()`, sem parâmetro de TTL). O TTL não é passado por construtor nenhum neste repositório — é passado como **parâmetro dos métodos** `store(key, request, ttl, timeUnit)` e `setResponse(key, request, response, ttl, timeUnit)` (assinaturas herdadas de `IdempotentRepository`/`AbstractIdempotentRepository`)
  - [ ] **O bug real, confirmado por leitura de `AbstractIdempotentRepository.java`** (linhas 39-52): tanto `store()` (linha 40-42: `getMap().put(key, new IdempotentRequestResponseWrapper(request))`) quanto `setResponse()` (linha 44-52) **recebem `ttl`/`timeUnit` como parâmetro e os ignoram completamente** — nunca usados no corpo do método. Como `getMap()` retorna um `ConcurrentHashMap` puro (sem suporte nativo a expiração de entrada), o TTL é ignorado **em 100% das chamadas**, não em "4 de 7 construtores". Isto é uma contradição textual entre o épico e o código real — implementar a correção contra o **código real** (os métodos que ignoram o parâmetro), não contra a contagem de "7 construtores" do epics.md, que não corresponde ao estado atual do arquivo
  - [ ] Se o time que mantém o PRD/epics.md quiser reconciliar essa contagem específica de "7 construtores", isso é uma decisão de quem mantém o plano de origem — este story não deve inventar 7 construtores que não existem só para bater com o texto; documentar a discrepância nas Completion Notes para rastreabilidade
- [ ] Task 2: Fazer `InMemoryIdempotentRepository`/`AbstractIdempotentRepository` respeitar o TTL (AC: #1)
  - [ ] Implementar expiração de entrada no repositório em memória — como `ConcurrentHashMap` não expira entradas nativamente, isso exige guardar o instante de expiração junto ao valor (ex.: em `IdempotentRequestResponseWrapper` ou um wrapper equivalente) e checar expiração em `contains()`/`getResponse()` (removendo/ignorando entradas expiradas), ou usar um mecanismo de scheduler para limpeza — escolher a abordagem mais simples que cumpre o AC sem introduzir uma dependência nova (ex.: Caffeine) não pedida pelo escopo
  - [ ] `store()` e `setResponse()` passam a usar de fato o `ttl`/`timeUnit` recebido como parâmetro, em vez de ignorá-lo
  - [ ] Confirmar que `RedisIdempotentRepository` (que já usa TTL corretamente via `valueOperations.set(key, value, ttl, timeUnit)`, linhas 79 e 109) não precisa de mudança — o bug é específico do repositório em memória
- [ ] Task 3: Corrigir `equals`/`hashCode` de `IdempotentRequestWrapper` (AC: #2)
  - [ ] **Bug real confirmado**: `IdempotentRequestWrapper.equals()` (linhas 54-57) hoje é `!Objects.isNull(request) && request.stream().anyMatch(req -> req.equals(obj))` — compara se **qualquer elemento da lista interna `request`** é igual ao objeto `obj` passado, em vez de comparar se `obj` é outro `IdempotentRequestWrapper` com a mesma lista `request`. Isso quebra tanto a reflexividade (`x.equals(x)` pode ser `false` se `x` não é elemento da sua própria lista) quanto a simetria (`x.equals(y)` pode ser `true` enquanto `y.equals(x)` é `false`, já que `y` não necessariamente implementa `equals` do mesmo jeito)
  - [ ] Reescrever `equals()` para o contrato padrão: checar `this == obj`, depois `obj instanceof IdempotentRequestWrapper`, depois comparar `Objects.equals(this.request, other.request)`
  - [ ] `hashCode()` (linha 50-52) já delega para `request.hashCode()` — manter consistente com o novo `equals()` (deve permanecer `Objects.hashCode(request)` ou equivalente)
  - [ ] **Atenção ao comentário `@SuppressFBWarnings(EQ_UNUSUAL, ...)`** já presente na classe (linhas 35-37), que justifica o `equals` atual como "intencional... mudar alteraria o comportamento de dedup" — este comentário está descrevendo o próprio bug que este AC pede para corrigir; ao aplicar a correção, remover ou atualizar esse `@SuppressFBWarnings` para não deixar uma justificativa desatualizada no código
- [ ] Task 4: Testes (AC: #1, #2)
  - [ ] Teste: armazenar uma entrada com TTL curto no repositório em memória, aguardar a expiração, confirmar que `contains()`/`getResponse()` não retornam mais a entrada
  - [ ] Teste de reflexividade: `wrapper.equals(wrapper)` é sempre `true`
  - [ ] Teste de simetria: para dois wrappers `a` e `b` com o mesmo `request`, `a.equals(b) == b.equals(a)`
  - [ ] Confirmar que a mudança de `equals()` não quebra o uso de `IdempotentRequestWrapper` como chave/valor em qualquer `Map`/`Set` existente no módulo (buscar todos os usos antes de alterar)

## Dev Notes

- **Esta é a discrepância mais significativa encontrada neste epic entre o texto do epics.md e o código real** — documentar isso é mais importante do que forçar a implementação a "bater" artificialmente com a contagem de 7 construtores citada no plano de origem. O comportamento observável a corrigir (TTL sempre ignorado no repositório em memória) é o mesmo espírito do AC, só a causa raiz descrita está desatualizada.
- **NFR4**: a introdução de expiração no repositório em memória é uma mudança de comportamento real (hoje entradas nunca expiram nesse repositório) — não é "mover/renomear", é correção de bug funcional, coerente com o objetivo declarado da story.
- Mudar `equals()`/`hashCode()` de uma classe usada como valor em estruturas de dados é sensível — revisar todos os pontos de uso de `IdempotentRequestWrapper` no módulo antes de aplicar, para confirmar que nenhum código depende do comportamento quebrado atual (mesmo que "depender de um bug" seja indesejável, precisa ser identificado antes do fix, não descoberto depois em produção).

### Project Structure Notes

- Arquivos modificados: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/datasource/InMemoryIdempotentRepository.java`, `AbstractIdempotentRepository.java`, `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/model/IdempotentRequestWrapper.java`.

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/datasource/InMemoryIdempotentRepository.java]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/datasource/AbstractIdempotentRepository.java#L39-L52]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/model/IdempotentRequestWrapper.java#L49-L57]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/repository/RedisIdempotentRepository.java#L76-L83,L102-L114]
- [Source: _bmad-output/planning-artifacts/epics.md#story-315-corrigir-ttl-e-equalshashcode]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
