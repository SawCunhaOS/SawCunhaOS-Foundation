# Story 3.7: Garantir fail-open com circuit breaker quando o Redis está indisponível

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor,
Eu quero que a indisponibilidade do Redis nunca bloqueie minha requisição de negócio,
Para não sofrer indisponibilidade em cascata por causa do cache de idempotência.

## Acceptance Criteria

1. **Given** o Redis indisponível ou lento, **When** o circuit breaker (`io.github.resilience4j:resilience4j-spring-boot4:2.4.0`, `optional=true`) detecta via `slow-call-duration-threshold` respeitando `spring.data.redis.timeout`, **Then** a requisição de negócio prossegue sem bloqueio (nunca `FAIL_CLOSED`).
2. **And** um teste com Testcontainers simulando indisponibilidade do Redis cobre esse cenário.
3. **And** um teste cobre o cenário de split-brain: lock adquirido com sucesso (Redis up), Redis cai durante o processamento, `setResponse` falha — a requisição ainda retorna sucesso ao cliente (fail-open), mas o comportamento (resposta não fica cacheada, retry subsequente reexecuta) é documentado explicitamente como risco aceito, não como bug.
4. **And** um teste cobre a janela de transição `OPEN → HALF_OPEN` do circuit breaker: duas chamadas concorrentes com a mesma chave nessa janela não podem ambas contornar o lock (uma via chamada de teste ao Redis real, outra via fail-open preventivo).

## Tasks / Subtasks

- [ ] Task 1: Confirmar o estado atual (contexto) (AC: #1)
  - [ ] **Confirmado por leitura direta**: `RedisIdempotentRepository` já envolve `contains`/`getResponse`/`store`/`remove`/`setResponse` em `try/catch (Exception e)` que loga e retorna um valor neutro (`false`/`null`, ou simplesmente não propaga a exceção) — ou seja, já existe uma forma **rudimentar** de fail-open (a chamada nunca lança para o `IdempotentAspect`). O que falta, confirmado por `grep` em todo o módulo `jdempotent`, é: (a) **nenhuma dependência resilience4j no `pom.xml`** do módulo, (b) **nenhum circuit breaker configurado**, (c) sem circuit breaker, cada chamada ao Redis indisponível ainda paga o timeout completo de rede (`spring.data.redis.timeout`) antes de cair no `catch` — não há short-circuit rápido após falhas repetidas, o que é exatamente o problema que `slow-call-duration-threshold` resolve
- [ ] Task 2: Adicionar a dependência do circuit breaker (AC: #1)
  - [ ] Adicionar `io.github.resilience4j:resilience4j-spring-boot4:2.4.0` ao `pom.xml` do módulo `jdempotent` como `optional=true` (ADD-3) — **não** usar `resilience4j-spring-boot3` nem deixar a versão gerenciada por BOM do projeto; fixar a versão explicitamente no `pom.xml` do módulo, conforme ADD-3
- [ ] Task 3: Configurar o circuit breaker por volta das chamadas Redis (AC: #1)
  - [ ] Envolver as operações de `RedisIdempotentRepository` com `@CircuitBreaker` (ou `CircuitBreakerRegistry` programático, conforme o padrão de configuração do resilience4j-spring-boot4) usando `slow-call-duration-threshold` configurado para respeitar `spring.data.redis.timeout` (o breaker deve considerar "lenta" uma chamada Redis que já ultrapassou o timeout configurado, não um valor arbitrário desacoplado)
  - [ ] O `fallback` do circuit breaker deve preservar o mesmo comportamento fail-open que já existe no `try/catch` — nunca `FAIL_CLOSED` (nunca bloquear/rejeitar a requisição de negócio por causa do estado do Redis)
- [ ] Task 4: Teste de indisponibilidade via Testcontainers (AC: #2)
  - [ ] Usar o Testcontainers já disponível no `pom.xml` do módulo (`org.testcontainers:testcontainers`, `testcontainers-junit-jupiter`) para subir um container Redis, depois pará-lo/isolá-lo em tempo de execução, e confirmar que uma requisição de negócio protegida por `@JdempotentResource` completa com sucesso mesmo com o Redis fora do ar
- [ ] Task 5: Teste de split-brain (AC: #3)
  - [ ] Simular: lock adquirido com sucesso (Redis up) → Redis cai durante a execução do método protegido → `setResponse` falha silenciosamente (fail-open) → confirmar que a resposta ainda retorna sucesso ao cliente
  - [ ] Documentar explicitamente (no Javadoc/README da Story 3.17, ou nas Completion Notes desta story) que, neste cenário, a resposta **não fica cacheada** e um retry subsequente do cliente **reexecuta o método de negócio** — comportamento aceito como risco conhecido, não como bug a corrigir aqui (a garantia real contra duplicidade nesse cenário de falha é a constraint `UNIQUE` do banco, NFR6, responsabilidade do consumidor)
- [ ] Task 6: Teste da janela `OPEN → HALF_OPEN` (AC: #4)
  - [ ] Forçar o circuit breaker ao estado `OPEN` (falhas/lentidão repetidas), aguardar a transição para `HALF_OPEN`, e disparar duas chamadas concorrentes com a mesma chave nessa janela — uma delas deve de fato testar o Redis real (chamada de prova do `HALF_OPEN`), a outra deve seguir o caminho de fail-open preventivo (breaker ainda não confirmou recuperação) — confirmar que as duas não conseguem **ambas** contornar o lock (ex.: ambas caindo no fail-open e executando o método de negócio em paralelo sem nenhuma proteção)

## Dev Notes

- O fail-open **já existe hoje de forma parcial e não intencional** via `try/catch` genérico em `RedisIdempotentRepository` — isto é uma descoberta relevante para o dev-agent: a mudança de comportamento observável para o consumidor final pode ser menor do que o AC sugere à primeira vista (a requisição já não bloqueia hoje). O que esta story realmente adiciona é: **circuit breaker mecânico** (short-circuit rápido, sem pagar o timeout completo repetidas vezes) + **testes que provam o comportamento** (hoje não há nenhum teste cobrindo indisponibilidade de Redis no módulo).
- **NFR3**: Testcontainers simulando indisponibilidade do Redis é um dos cenários explicitamente exigidos pelo NFR3 ("indisponibilidade de Redis via Testcontainers").
- **NFR6**: o README (Story 3.17) deve deixar claro que fail-open é "cache é fast-path, não garantia" — a constraint `UNIQUE` no banco é a garantia real sob esse cenário de falha. Esta story não implementa a constraint (responsabilidade do consumidor), só garante que o módulo não quebra sob falha do Redis.
- **Ponytail**: não configurar múltiplos circuit breakers por operação (um para `contains`, outro para `store`, etc.) — um único breaker por instância de `RedisIdempotentRepository` cobre o AC; granularidade por operação seria complexidade não pedida.

### Project Structure Notes

- Arquivo modificado: `jdempotent/pom.xml` (nova dependência), `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/repository/RedisIdempotentRepository.java`.
- Testcontainers já é dependência de teste existente do módulo — não precisa ser adicionado.

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/repository/RedisIdempotentRepository.java]
- [Source: jdempotent/pom.xml]
- [Source: _bmad-output/planning-artifacts/epics.md#story-37-garantir-fail-open-com-circuit-breaker-quando-o-redis-está-indisponível]
- [Source: _bmad-output/planning-artifacts/epics.md#add-3-ad-2--stack]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
