# Story 3.11: Expor métricas de idempotência

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como operador monitorando idempotência em produção,
Eu quero métricas de acerto/colisão/degradação,
Para detectar problemas como cliente gerando chave dentro do laço de retry.

## Acceptance Criteria

1. **Given** operações de idempotência ocorrendo (acquire, hit, colisão, erro de backend, degradação), **When** `IdempotencyMetrics` é implementado (no-op por padrão, Micrometer condicional), **Then** as métricas `idempotency.acquired`, `.hit`, `.in_progress`, `.mismatch`, `.backend_error`, `.degraded` (gauge 0/1) e `.degraded.transitions` (counter) são expostas corretamente.

## Tasks / Subtasks

- [ ] Task 1: Confirmar a lacuna atual (contexto) (AC: #1)
  - [ ] **Confirmado por leitura direta**: busca por `Metrics`/`Micrometer` em todo `jdempotent/src/main/java` não encontrou nenhuma ocorrência — não existe hoje nenhuma instrumentação de métricas no módulo. Esta story cria o mecanismo do zero.
- [ ] Task 2: Definir a interface `IdempotencyMetrics` (AC: #1)
  - [ ] Criar a interface `IdempotencyMetrics` com métodos correspondentes a cada evento: `acquired()`, `hit()`, `inProgress()`, `mismatch()`, `backendError()`, `degraded(boolean)` (gauge 0/1), `degradedTransition()` (counter)
  - [ ] Implementação **no-op por padrão** (classe que não faz nada em cada método) registrada como bean default — consumidores que não têm Micrometer no classpath não pagam custo nem erro de bean ausente
  - [ ] Implementação condicional via Micrometer (`@ConditionalOnClass(MeterRegistry.class)` ou equivalente) que de fato registra as métricas via `Counter`/`Gauge` do Micrometer quando o consumidor tem a dependência
- [ ] Task 3: Instrumentar os pontos de emissão (AC: #1)
  - [ ] `idempotency.acquired`: emitido quando `tryAcquire` (Story 3.5) obtém o lock com sucesso
  - [ ] `idempotency.hit`: emitido quando uma chave já tem resposta cacheada e a resposta é servida do cache (caminho `contains() == true` / equivalente pós-Story 3.5)
  - [ ] `idempotency.in_progress`: emitido quando `tryAcquire` indica que a chave já está em processamento (409, Story 3.5) — esta métrica é o mecanismo de detecção em produção do cenário de lease expirando citado na Story 3.5 AC #4
  - [ ] `idempotency.mismatch`: emitido quando a checagem de payload (Story 3.6) detecta `422 PAYLOAD_MISMATCH`
  - [ ] `idempotency.backend_error`: emitido quando uma operação contra o repositório (Redis) falha e cai no caminho de fail-open (Story 3.7)
  - [ ] `idempotency.degraded` (gauge 0/1): reflete se o módulo está atualmente operando em modo degradado (ex.: circuit breaker aberto, Story 3.7) — 1 quando degradado, 0 quando normal
  - [ ] `idempotency.degraded.transitions` (counter): incrementado a cada transição de estado (normal→degradado ou degradado→normal), não a cada verificação
- [ ] Task 4: Testes (AC: #1)
  - [ ] Teste unitário por evento: confirmar que cada operação do `IdempotentAspect`/repositórios chama o método correto de `IdempotencyMetrics` exatamente uma vez por ocorrência
  - [ ] Teste confirmando que, sem Micrometer no classpath, a implementação no-op é usada sem erro de contexto Spring

## Dev Notes

- Esta story depende funcionalmente das Stories 3.5 (acquire/in_progress), 3.6 (mismatch) e 3.7 (backend_error/degraded) já terem os pontos de decisão implementados — os nomes de evento desta story mapeiam diretamente para os resultados que `tryAcquire`/`Lease` (3.5) e o circuit breaker (3.7) já produzem. Implementar esta story **depois** das três, para instrumentar pontos que já existem, em vez de adivinhar a interface antes dela existir.
- **Ponytail**: não criar uma abstração de "provider de métricas" plugável além de Micrometer (ex.: suporte a múltiplos backends de métricas simultâneos) — não foi pedido; o AC pede especificamente Micrometer condicional com fallback no-op, nada além disso.
- `idempotency.degraded` como gauge 0/1 (não um enum de estados) é uma decisão explícita do AC — não substituir por um valor mais granular (ex.: estado do circuit breaker CLOSED/OPEN/HALF_OPEN) sem confirmação; se for útil expor o estado detalhado também, isso seria uma métrica adicional, não substituta.

### Project Structure Notes

- Arquivo novo: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/metrics/IdempotencyMetrics.java` (interface) e implementações (no-op, Micrometer).
- Arquivos modificados: `IdempotentAspect.java`, `RedisIdempotentRepository.java` (pontos de emissão).

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java]
- [Source: _bmad-output/implementation-artifacts/3-5-tornar-a-aquisição-do-lock-atômica-tryacquire-lease.md]
- [Source: _bmad-output/implementation-artifacts/3-7-garantir-fail-open-com-circuit-breaker-quando-o-redis-está-i.md]
- [Source: _bmad-output/planning-artifacts/epics.md#story-311-expor-métricas-de-idempotência]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
