# Story 2.9: Mover a tradução HTTP do `exception` para o `web` e registrar via autoconfiguração

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como consumidor da API,
Eu quero que o tratamento de erro HTTP seja ativado automaticamente ao importar o módulo `web`,
Para não precisar de código de integração manual.

## Acceptance Criteria

1. **Given** `ExceptionsHandler`, `ScosProblemDetails`, `ScosFieldError`, `ExceptionUtils` já corrigidos e testados, **When** são movidos para o módulo `web` (criado no Epic 1) e registrados via `AutoConfiguration.imports`, **Then** o handler é ativado por padrão via `@ConditionalOnProperty(scos.web.error-handler.enabled, matchIfMissing=true)` e ordenado com `@Order(Ordered.LOWEST_PRECEDENCE)`.
2. **And** `scos-foundation-exception` deixa de existir como módulo e sai do reactor Maven.

## Tasks / Subtasks

- [ ] Task 1: Mover as 4 classes para `web` (AC: #1)
  - [ ] Mover `exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java`, `exception/.../model/ScosProblemDetails.java`, `exception/.../model/ScosFieldError.java`, `exception/.../utils/ExceptionUtils.java` para `web/src/main/java/br/com/sawcunhaos/foundation/web/...` (Story 1.12, Epic 1)
  - [ ] Ajustar imports internos para os tipos movidos ao `core` na Story 2.8 (`ScosException`, `ExceptionCode`, `LocaleService`, `ScosNoContentException`, `ScosNoRollbackException`, `MethodNotImplementedException`)
  - [ ] Mover os 5 testes já existentes junto: `ExceptionsHandlerAccessDeniedTest`, `ExceptionsHandlerInternalErrorTest`, `ExceptionsHandlerMdcTest`, `ExceptionsHandlerScosExceptionTest`, `ExceptionsHandlerValidationTest`, `ScosFieldErrorTest`, mais os testes novos das Stories 2.1–2.7
- [ ] Task 2: Registrar via `AutoConfiguration.imports` (AC: #1)
  - [ ] `web` hoje não tem `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — criar `web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` seguindo o padrão já usado em `jdempotent`/`audit`/`privacy` (um FQN de classe de configuração por linha)
  - [ ] `ExceptionsHandler` é hoje um `@ControllerAdvice` direto (não uma `@Configuration` com `@Bean`) — para registrar via `AutoConfiguration.imports` com `@ConditionalOnProperty` e `@Order`, envolvê-lo numa classe `@AutoConfiguration` dedicada (ex.: `ScosWebErrorHandlerAutoConfiguration`) que declara `ExceptionsHandler` como `@Bean` com `@ConditionalOnProperty(prefix = "scos.web.error-handler", name = "enabled", matchIfMissing = true)` e `@Order(Ordered.LOWEST_PRECEDENCE)` — sem alterar a lógica interna do `ExceptionsHandler` em si
- [ ] Task 3: Remover o módulo `exception` do reactor (AC: #2)
  - [ ] Depois que as Stories 2.8 e 2.9 esvaziarem `exception/src/main/java` por completo, remover `<module>exception</module>` do `pom.xml` raiz e apagar o diretório `exception/`
  - [ ] Atualizar `audit/pom.xml` e qualquer outro `pom.xml` que ainda declare `scos-foundation-exception` como dependência — repontar para `core`/`web` conforme o que cada consumidor realmente usa

## Dev Notes

- Depende diretamente da Story 2.8 (classes de domínio já em `core`) e da Story 1.12 do Epic 1 (`web` já existe, dependendo de `core`+`cache`+`privacy`).
- **Padrão de referência já existente no repo** para `AutoConfiguration.imports`: `jdempotent/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — reaproveitar o mesmo formato (uma linha, FQN completo), não inventar um novo.
- Esta story depende também da Story 2.7 (200/404 unificado): mover o handler antes de resolver o gap do `throw-exception-if-no-handler-found` deixaria a Story 2.7 sem onde aterrissar sua correção — a ordem correta é 2.1–2.7 (correções de comportamento) antes de 2.8–2.9 (movimentação de módulo), para não misturar commit de "mover" com commit de "corrigir" na mesma classe (NFR2).
- `@ConditionalOnProperty(matchIfMissing = true)` significa: se a aplicação consumidora não setar `scos.web.error-handler.enabled`, o handler fica ativo por padrão (comportamento atual, sem regressão) — só desativa explicitamente com `scos.web.error-handler.enabled=false`.

### Project Structure Notes

- Módulo `web` (Epic 1, Story 1.12) ganha: `ExceptionsHandler`, `ScosProblemDetails`, `ScosFieldError`, `ExceptionUtils`, mais a nova classe `ScosWebErrorHandlerAutoConfiguration`.
- Novo arquivo: `web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Módulo `exception` removido do reactor Maven (`pom.xml` raiz) e do disco.
- `audit/pom.xml` perde a dependência `scos-foundation-exception` (já repontada para `core` na Story 2.8; nada resta que justifique a dependência em `exception`).

### References

- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/ExceptionsHandler.java]
- [Source: jdempotent/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports]
- [Source: _bmad-output/implementation-artifacts/1-12-extrair-o-módulo-web.md]
- [Source: _bmad-output/implementation-artifacts/2-8-mover-o-contrato-de-domínio-do-exception-para-o-core.md]
- [Source: _bmad-output/planning-artifacts/epics.md#story-29-mover-a-tradução-http-do-exception-para-o-web-e-registrar-via-autoconfiguração]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
