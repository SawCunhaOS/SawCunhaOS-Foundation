# Story 2.4: Padronizar log por faixa de status HTTP

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como operador monitorando a aplicação,
Eu quero que erros `4xx` logem em `WARN` sem stack trace e `5xx` em `ERROR` com stack trace,
Para não poluir alertas com ruído de erro de cliente.

## Acceptance Criteria

1. **Given** exceções mapeadas para diferentes faixas de status, **When** cada uma é tratada pelo handler, **Then** `4xx` loga em `WARN` sem stack trace, `5xx` em `ERROR` com stack trace, e `ScosNoContentException` (204) em `DEBUG`.
2. **And** o handler genérico (fallback sem tipo específico) é o único que loga `ERROR` incondicionalmente.
3. **And** todo uso de Log4j2 direto é substituído por `@Slf4j`.

## Tasks / Subtasks

- [ ] Task 1: Levantar o estado atual de log em `ExceptionsHandler.java` (AC: #1, #2)
  - [ ] Hoje **todo** handler chama `log.error("handleSecurity - <Nome>: ", ex)` incondicionalmente, incluindo os que respondem `403`/`400`/`204` — nenhuma diferenciação por faixa de status existe (linhas 83, 115, 146, 175, 208, 223, 237, 244, 258, 272)
  - [ ] Mapear cada handler para sua faixa de status real: `handleHttpMessageNotReadable`→400, `handleMethodArgumentNotValid`→400, `handleHandlerMethodValidationException`→400, `handleConstraintViolationException`→400, `handleScosException`→variável (`resolveHttpCode(exception.getHttpCode())`, pode ser 4xx ou 5xx conforme o `ExceptionCode` usado), `handleScosNoRollbackException`→variável (mesmo padrão), `handleScosNoContentException`→204, `handleAccessDeniedException` (ambos overloads)→403, `handleGenericException`→500
- [ ] Task 2: Aplicar o nível de log correto por faixa (AC: #1, #2)
  - [ ] Para os handlers de status fixo 4xx (validação, access denied): `log.warn(...)` sem passar a exceção como último argumento (evita stack trace no append padrão do SLF4J)
  - [ ] Para `handleScosNoContentException`: `log.debug(...)`
  - [ ] Para `handleScosException`/`handleScosNoRollbackException` (status dinâmico via `resolveHttpCode`): decidir o nível **depois** de resolver o `HttpStatus` — `WARN` sem stack trace se `4xx`, `ERROR` com stack trace se `5xx`. Extrair essa decisão para um método privado único (ex.: `logByStatus(HttpStatus status, String context, Throwable ex)`) reaproveitado pelos dois handlers, para não duplicar a lógica de faixa
  - [ ] `handleGenericException` continua `log.error(...)` incondicional (é o único fallback sem tipo específico, AC #2) — não precisa da lógica de faixa, seu status é sempre 500
- [ ] Task 3: Substituir `@Log4j2` por `@Slf4j` (AC: #3)
  - [ ] `ExceptionsHandler.java` usa `lombok.extern.log4j.Log4j2` (import linha 28, anotação linha 70) — trocar para `lombok.extern.slf4j.Slf4j` (padrão já usado em 16 outras classes do repo; só mais 1 outra classe no repo inteiro usa `@Log4j2` hoje: `utils/.../listener/ScosOnStartupListener.java`, que pertence ao módulo `spring` do Epic 1 e está **fora do escopo desta story** — confirmar com quem mantém o épico antes de tocar nela, já que o AC não delimita módulo)
- [ ] Task 4: Testes (AC: #1, #2)
  - [ ] Cobrir com teste (usando `ListAppender`/captura de log, ou verificação de nível via mock de logger se o padrão do repo já suportar) que um handler 4xx não inclui stack trace e um handler 5xx inclui

## Dev Notes

- **Estado real confirmado**: hoje não existe nenhuma diferenciação de nível de log no arquivo — é uma padronização genuína, não uma correção de bug pontual.
- `handleScosException`/`handleScosNoRollbackException` são os únicos dois casos onde o status HTTP **não** é fixo no código (vem de `exception.getHttpCode()` via `resolveHttpCode`) — por isso a decisão do nível de log tem que acontecer depois de resolver o status, não pode ser hardcoded por handler como nos demais.
- Escopo desta story é só o módulo `exception` (futuro `web`, Epic 1 Story 1.12) — o `@Log4j2` de `ScosOnStartupListener.java` pertence ao módulo `spring` e não tem relação com o tratamento de erro HTTP; tratá-lo aqui seria misturar dois módulos numa mesma story. Documentado como decisão consciente de escopo, não omissão.
- Não introduzir uma abstração de "log level resolver" genérica reaproveitável por todo o projeto — o método privado da Task 2 resolve só o caso concreto de `ScosException`/`ScosNoRollbackException` deste handler; generalizar além disso não foi pedido pelo epics.md.

### Project Structure Notes

- Arquivo modificado: `exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java` (import, todas as chamadas `log.error`, novo método privado de decisão de nível).
- Nenhum módulo novo.

### References

- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/listener/ScosOnStartupListener.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-24-padronizar-log-por-faixa-de-status-http]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
