# Story 3.5: Tornar a aquisição do lock atômica (`tryAcquire`/`Lease`)

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor fazendo chamadas concorrentes com a mesma chave,
Eu quero que a segunda chamada responda `409 IN_PROGRESS` em vez de `null` silencioso,
Para saber que já existe processamento em curso.

## Acceptance Criteria

1. **Given** duas chamadas concorrentes com a mesma chave, **When** a aquisição do lock passa a usar `tryAcquire(key, payloadHash, ttl) → Lease` atômico (Lua script), substituindo `contains → store → setResponse`, **Then** a segunda chamada responde `409 IN_PROGRESS`.
2. **And** o TTL do lease é configurável por método (não fixo) e o tipo de `ttl` é sempre `java.time.Duration`.
3. **And** um teste de concorrência real (não simulado) cobre esse cenário.
4. **And** um teste cobre o cenário do lease expirar antes do método protegido terminar (risco catalogado no plano de origem, probabilidade média): o comportamento observável é documentado e a métrica `idempotency.in_progress` (FR7) permite detectar esse caso em produção.

## Tasks / Subtasks

- [ ] Task 1: Confirmar a race condition atual (contexto, não código novo) (AC: #1)
  - [ ] **Confirmado por leitura direta**: `IdempotentAspect.execute()` (linhas 163-201) faz `if (idempotentRepository.contains(idempotencyKey)) { ... }` seguido, em caminho separado, de `idempotentRepository.store(...)` (linha 181) e só depois `idempotentRepository.setResponse(...)` (linha 197) — três operações não-atômicas. Entre o `contains()` retornar `false` e o `store()` executar, uma segunda chamada concorrente com a mesma chave também vê `contains() == false` e prossegue — as duas chamadas executam `pjp.proceed()` (o método de negócio) em paralelo, exatamente o que a idempotência deveria impedir
- [ ] Task 2: Introduzir `tryAcquire(key, payloadHash, ttl) → Lease` (AC: #1, #2)
  - [ ] Adicionar `tryAcquire(IdempotencyKey key, String payloadHash, Duration ttl)` ao contrato `IdempotentRepository` (hoje em `jdempotent/core/datasource/IdempotentRepository.java`), retornando um novo tipo `Lease` (registra se a aquisição obteve o lock, e dados suficientes para decidir 409 vs. 422 na Story 3.6)
  - [ ] Implementar em `RedisIdempotentRepository` via **Lua script** executado atomicamente pelo Redis (ex.: `SET key value NX PX <ttl_ms>` como base, mas via script Lua se a semântica exigir checar payloadHash na mesma operação atômica — decidir a forma exata do script na implementação, mantendo a atomicidade como requisito não-negociável do AC)
  - [ ] Implementar em `InMemoryIdempotentRepository`/`AbstractIdempotentRepository` usando uma operação atômica equivalente do `ConcurrentHashMap` (`putIfAbsent` ou `compute`) — não duas chamadas separadas
  - [ ] `ttl` do `Lease` é **sempre** `java.time.Duration` (nunca `long`/`Long` cru) — isto é requisito do ADD-4 e vale também para as assinaturas afetadas de `IdempotentRepository`; a assinatura atual de `store`/`setResponse` usa `Long ttl, TimeUnit timeUnit` (dois parâmetros) — ao introduzir `tryAcquire`, usar `Duration` desde o início nessa nova API, sem repetir o padrão antigo de dois parâmetros separados
  - [ ] `JdempotentResource.ttl()` hoje é `long ttl() default 0L` (tipo primitivo, sem unidade own) — a conversão para `Duration` no `IdempotentAspect` é responsabilidade desta story, usando o `ttlTimeUnit()` já existente na anotação para construir o `Duration` correto na borda (a anotação em si não precisa mudar de tipo, já que anotações Java não aceitam `Duration` como tipo de atributo — a conversão acontece ao ler o valor)
  - [ ] Substituir o fluxo `contains → store → setResponse` em `IdempotentAspect.execute()` por uma chamada a `tryAcquire`; se o `Lease` indicar que o lock já está em uso por outra chamada, lançar/responder o equivalente a `409 IN_PROGRESS` (mecanismo exato de propagação do status HTTP é decisão de integração com o módulo `web`, documentar a interface aqui)
- [ ] Task 3: Teste de concorrência real (AC: #3)
  - [ ] Escrever um teste com múltiplas threads reais (`ExecutorService`, não `Thread.sleep`/mock de tempo) disparando a mesma chave simultaneamente contra `RedisIdempotentRepository` (via Testcontainers, já disponível no `pom.xml` do módulo) e confirmar que exatamente uma chamada obtém o lock e as demais recebem o sinal de "já em progresso"
- [ ] Task 4: Teste do cenário de lease expirando antes do método terminar (AC: #4)
  - [ ] Escrever um teste onde o TTL do lease é propositalmente menor que o tempo de execução do método protegido (simular método lento) e documentar o comportamento observável resultante (ex.: uma terceira chamada após a expiração do lease mas antes do método original terminar pode adquirir um novo lock e executar em paralelo — **risco catalogado, não é bug desta story eliminar totalmente**, é comportamento a documentar)
  - [ ] Confirmar que a métrica `idempotency.in_progress` (implementada na Story 3.11) é o mecanismo de detecção em produção deste cenário — esta story só precisa garantir que o `Lease`/`tryAcquire` expõe informação suficiente para a métrica ser alimentada corretamente depois; não implementar a métrica em si aqui (fora do escopo, é FR7/Story 3.11)

## Dev Notes

- Esta é a story mais crítica do épico: substitui o núcleo do mecanismo de idempotência. Coordenar com as Stories 3.6 (colisão de payload, usa o mesmo `tryAcquire`) e 3.9 (posicionamento do aspecto fora do `@Transactional`) — as três mexem na mesma região de código (`IdempotentAspect.execute()`).
- **NFR3**: este é exatamente o tipo de mudança que exige cobertura de 80% nas áreas tocadas (teste de concorrência real, Task 3) — não é opcional.
- **Sequenciamento**: a Story 3.6 (colisão de payload) depende do `payloadHash` já estar disponível no momento do `tryAcquire` — implementar o parâmetro `payloadHash` nesta story mesmo que a lógica de comparação/422 só seja escrita na 3.6, para não duplicar a mudança de assinatura do contrato depois.
- Não introduzir um mecanismo de retry/backoff automático para quem recebe `409 IN_PROGRESS` — não foi pedido pelo AC; a decisão de tentar de novo é do consumidor da API, não deste módulo.

### Project Structure Notes

- Arquivos modificados: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/datasource/IdempotentRepository.java`, `AbstractIdempotentRepository.java`, `InMemoryIdempotentRepository.java`, `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/repository/RedisIdempotentRepository.java`, `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java`.
- Arquivo novo: uma classe `Lease` (local a definir dentro de `jdempotent/core/model/`, mesmo pacote de `IdempotencyKey`/`IdempotentRequestWrapper`).

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java#L163-L201]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/datasource/AbstractIdempotentRepository.java]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/repository/RedisIdempotentRepository.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/annotation/jdempotent/JdempotentResource.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-35-tornar-a-aquisição-do-lock-atômica-tryacquirelease]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
