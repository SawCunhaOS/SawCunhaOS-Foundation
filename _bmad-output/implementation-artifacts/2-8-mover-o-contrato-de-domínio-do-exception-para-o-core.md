# Story 2.8: Mover o contrato de domínio do `exception` para o `core`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero que exceções de domínio não dependam de Spring,
Para que `core` continue sem essa dependência.

## Acceptance Criteria

1. **Given** `ScosException`, `ExceptionCode`, `ScosExceptionCode`, `LocaleService`, `ScosNoContentException`, `ScosNoRollbackException`, `ScosSecurityException`, `MethodNotImplementedException` já corrigidos nas stories anteriores, **When** essas classes são movidas para o módulo `core` (criado no Epic 1), **Then** `core` continua compilando sem `org.springframework` no classpath.
2. **And** o módulo `audit` é repontado para consumir `ScosException` do `core` em vez do `exception` antigo.
3. **And** a extração ocorre em commit de "mover", separado de qualquer ajuste de comportamento.

## Tasks / Subtasks

- [ ] Task 1: Mover as 8 classes de domínio para `core` (AC: #1)
  - [ ] Origem real confirmada de cada uma (o inventário congelado da Story 1.1 não lista `exception` como pacote de origem — conferir/ajustar lá se necessário, já que estas 8 classes ficam hoje espalhadas por dois módulos diferentes, não um só):
    - `ScosException.java`: `utils/src/main/java/br/com/sawcunhaos/foundation/utils/exception/`
    - `ExceptionCode.java`: `utils/src/main/java/br/com/sawcunhaos/foundation/utils/specification/`
    - `ScosExceptionCode.java`: `utils/src/main/java/br/com/sawcunhaos/foundation/utils/enums/`
    - `LocaleService.java`: `utils/src/main/java/br/com/sawcunhaos/foundation/utils/specification/`
    - `ScosNoContentException.java`, `ScosNoRollbackException.java`, `ScosSecurityException.java`, `MethodNotImplementedException.java`: `exception/src/main/java/br/com/sawcunhaos/foundation/exception/error/`
  - [ ] Mover todas as 8 para `core/src/main/java/br/com/sawcunhaos/foundation/core/...`, ajustando pacote para `br.com.sawcunhaos.foundation.core` (seguindo a convenção ADD-5, sem sub-pacote genérico `util`/`common`)
  - [ ] Confirmar que nenhuma delas importa `org.springframework.*`/`jakarta.persistence.*`/`jakarta.servlet.*` — todas as 8, pelo código já lido nas stories anteriores (2.1–2.6), são POJOs/enums/interfaces puros, sem dependência de Spring
- [ ] Task 2: Repontar `audit` para consumir do `core` (AC: #2)
  - [ ] `audit/pom.xml` hoje depende de `scos-foundation-utils` e `scos-foundation-exception` — adicionar dependência em `scos-foundation-core` (Story 1.7) e atualizar imports
  - [ ] Uso real confirmado de `ScosException` em `audit`: `audit/src/test/java/br/com/sawcunhaos/foundation/audit/service/InsideAuditExampleService.java` (import `br.com.sawcunhaos.foundation.utils.exception.ScosException`, escopo `test`) — atualizar o import para `br.com.sawcunhaos.foundation.core.exception.ScosException` (ou pacote equivalente escolhido na Task 1)
  - [ ] Confirmar se a dependência `scos-foundation-exception` do `audit/pom.xml` pode ser removida nesta story ou só depois da Story 2.9 (quando o módulo `exception` deixa de existir) — se `audit` não usa nenhuma classe do módulo `exception` em si (só as que estão migrando de `utils` para `core`), a dependência pode já ser removida aqui
- [ ] Task 3: Confirmar regra ArchUnit de zero-Spring em `core` (AC: #1)
  - [ ] A regra ArchUnit local de `core` (já existente desde a Story 1.7) deve continuar passando após a adição destas 8 classes — não criar uma regra nova, só confirmar que a existente cobre o pacote onde elas caem
- [ ] Task 4: Commit de mover separado (AC: #3)
  - [ ] Um commit só move os arquivos (rename/move), sem tocar em uma linha de lógica — qualquer ajuste (ex.: pacote nos imports que dependem delas) entra no mesmo commit de mover por ser mecânico, não comportamental; qualquer mudança de comportamento real fica para commit separado, se houver

## Dev Notes

- Esta story só pode rodar **depois** que `ScosException`/`ScosNoRollbackException`/`ScosExceptionCode`/`LocaleService` já estiverem com as correções das Stories 2.3 (novo `ScosExceptionCode.NOT_IMPLEMENTED`) e 2.6 (`LocaleService.getMessageOrDefault`) aplicadas — mover primeiro e corrigir depois duplicaria trabalho entre dois commits de módulos diferentes.
- **Confirmado**: as 8 classes já vivem sem dependência de Spring hoje (verificado nas stories 2.1–2.6) — esta story é puramente uma movimentação de pacote/módulo, não uma reescrita.
- `MethodNotImplementedException` muda de módulo mas não de forma (`RuntimeException` simples, sem `ExceptionCode`) — a decisão de integrá-la ou não ao padrão `ScosException` continua fora do escopo (mesma nota da Story 2.3).
- `ExceptionCode` é uma interface (`PROBLEM_TYPE_BASE_URI`, `getCode()`, `getType()` default, `getTitle()` default, `getHttpCode()` default) — mover junto com `ScosExceptionCode` (a única implementação hoje no repo) evita separar contrato e implementação entre módulos.

### Project Structure Notes

- Módulo `core` (Epic 1, Story 1.7) ganha 8 novas classes: `ScosException`, `ExceptionCode`, `ScosExceptionCode`, `LocaleService`, `ScosNoContentException`, `ScosNoRollbackException`, `ScosSecurityException`, `MethodNotImplementedException`.
- `utils` perde 4 classes (`ScosException`, `ExceptionCode`, `ScosExceptionCode`, `LocaleService`).
- `exception` perde 4 classes (`ScosNoContentException`, `ScosNoRollbackException`, `ScosSecurityException`, `MethodNotImplementedException`) — depois da Story 2.9 (que move o restante), o módulo `exception` fica vazio e sai do reactor.
- `audit/pom.xml` ganha dependência em `scos-foundation-core`.

### References

- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/exception/ScosException.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/specification/ExceptionCode.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/enums/ScosExceptionCode.java]
- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/specification/LocaleService.java]
- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/error/ScosNoContentException.java]
- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/error/ScosNoRollbackException.java]
- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/error/ScosSecurityException.java]
- [Source: exception/src/main/java/br/com/sawcunhaos/foundation/exception/error/MethodNotImplementedException.java]
- [Source: audit/src/test/java/br/com/sawcunhaos/foundation/audit/service/InsideAuditExampleService.java]
- [Source: audit/pom.xml]
- [Source: _bmad-output/implementation-artifacts/1-7-extrair-o-módulo-core.md]
- [Source: _bmad-output/planning-artifacts/epics.md#story-28-mover-o-contrato-de-domínio-do-exception-para-o-core]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
