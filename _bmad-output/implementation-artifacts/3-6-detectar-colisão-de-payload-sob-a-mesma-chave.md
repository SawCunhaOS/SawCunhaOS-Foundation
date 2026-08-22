# Story 3.6: Detectar colisão de payload sob a mesma chave

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor,
Eu quero que duas requisições distintas com a mesma chave mas payload diferente respondam `422 PAYLOAD_MISMATCH`,
Para não receber por engano a resposta cacheada de outra requisição.

## Acceptance Criteria

1. **Given** duas requisições com a mesma chave e hash de payload distinto, **When** a segunda chega, **Then** a resposta é `422 PAYLOAD_MISMATCH` em vez da resposta cacheada compartilhada.
2. **And** quando as duas condições coincidem na mesma janela de corrida (mesma chave, payload diferente, primeira ainda em processamento), a checagem de mismatch de payload tem precedência sobre o `409 IN_PROGRESS` — a segunda requisição recebe `422 PAYLOAD_MISMATCH`, não `409`, porque o mismatch é semanticamente o erro mais específico.

## Tasks / Subtasks

- [ ] Task 1: Confirmar a lacuna atual (contexto) (AC: #1)
  - [ ] **Confirmado por leitura direta**: nem `IdempotentAspect.execute()` nem `AbstractIdempotentRepository`/`RedisIdempotentRepository` fazem qualquer comparação de payload hoje — `contains(key)` só olha a chave, nunca o conteúdo. Duas requisições com a mesma chave (ex.: colisão de hash, ou reuso indevido de `Idempotency-Key` pelo cliente) e payloads diferentes recebem hoje a **mesma resposta cacheada** da primeira, silenciosamente — o bug que este AC elimina
- [ ] Task 2: Comparar `payloadHash` no `tryAcquire` (AC: #1)
  - [ ] Esta story depende do `tryAcquire(key, payloadHash, ttl)` introduzido na Story 3.5 — usar o `payloadHash` já recebido nesse contrato para comparar contra o hash da requisição em andamento associada à mesma chave
  - [ ] Se a chave já existe (lock ativo ou resposta já cacheada) **e** o `payloadHash` armazenado difere do `payloadHash` da nova requisição, o `Lease`/resultado de `tryAcquire` deve sinalizar mismatch de forma distinguível de "já em progresso" (AC #1 e #2 exigem que o chamador consiga diferenciar os dois casos)
  - [ ] Propagar esse sinal até o ponto de integração HTTP como `422 PAYLOAD_MISMATCH` (mecanismo exato de propagação de status é decisão de integração com o módulo `web`, documentar a interface aqui, mesmo padrão da Story 3.5)
- [ ] Task 3: Precedência de mismatch sobre `409 IN_PROGRESS` na janela de corrida (AC: #2)
  - [ ] Cenário: chave A, payload P1 chega e adquire o lock (ainda processando); chave A, payload P2 (diferente) chega **enquanto P1 ainda está em processamento** — o resultado correto é `422 PAYLOAD_MISMATCH`, não `409 IN_PROGRESS`, mesmo que "já em progresso" também seja tecnicamente verdade
  - [ ] Implementar a ordem de checagem dentro da mesma operação atômica do `tryAcquire`: comparar `payloadHash` **antes** de decidir se o resultado é "lock obtido", "já em progresso" ou "mismatch" — não fazer duas chamadas separadas ao repositório (uma para checar payload, outra para checar lock), que reintroduziria a mesma classe de race condition que a Story 3.5 elimina
- [ ] Task 4: Testes (AC: #1, #2)
  - [ ] Teste sequencial: primeira requisição completa, segunda com payload diferente chega depois → `422 PAYLOAD_MISMATCH`
  - [ ] Teste de concorrência real (threads reais, mesmo padrão da Story 3.5 Task 3): primeira requisição ainda em processamento, segunda com payload diferente chega na janela de corrida → `422 PAYLOAD_MISMATCH`, nunca `409`

## Dev Notes

- Esta story **depende tecnicamente da Story 3.5** estar implementada primeiro — o `tryAcquire`/`Lease` é o mecanismo que ambas compartilham. Não implementar comparação de payload em cima do fluxo antigo `contains → store → setResponse` (seria trabalho descartável assim que a 3.5 substituir o fluxo).
- **NFR3**: cobertura de 80% aplica-se também aqui — colisão de payload é um dos cenários explicitamente citados no NFR3 ("testes de... colisão de hex").
- A precedência do AC #2 (mismatch > in-progress) é uma decisão de design deliberada do epics.md, não uma sugestão — implementar exatamente essa ordem, não a ordem inversa nem "o que chegar primeiro".

### Project Structure Notes

- Arquivos modificados: mesmos da Story 3.5 (`IdempotentRepository`, `AbstractIdempotentRepository`, `InMemoryIdempotentRepository`, `RedisIdempotentRepository`, `IdempotentAspect`) — esta story estende o mesmo mecanismo, não cria um caminho paralelo.

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java#L163-L201]
- [Source: _bmad-output/implementation-artifacts/3-5-tornar-a-aquisição-do-lock-atômica-tryacquire-lease.md]
- [Source: _bmad-output/planning-artifacts/epics.md#story-36-detectar-colisão-de-payload-sob-a-mesma-chave]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
