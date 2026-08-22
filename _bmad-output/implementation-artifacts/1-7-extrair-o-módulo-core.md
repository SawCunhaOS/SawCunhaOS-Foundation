# Story 1.7: Extrair o módulo `core`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor que só precisa de utilitários simples,
Eu quero um módulo `core` sem dependência de Spring/JPA/Servlet,
Para importar `DateUtils`/`HashUtils` sem carregar o resto da stack.

## Acceptance Criteria

1. **Given** as classes do inventário (Story 1.1) destinadas a `core` (`DateUtils`, `HashUtils`, `StringFieldUtils`, `PropertiesOrder`, `ScosBaseUseCase`, `ScosUserAuthentication`), **When** o módulo é extraído em 2 commits separados (mover vs. ajustar comportamento), **Then** `core` compila sem `org.springframework`, `jakarta.persistence` ou `jakarta.servlet` no classpath.
2. **And** uma regra ArchUnit local em `core` falha o build se qualquer um desses pacotes for importado.
3. **And** os testes existentes das classes migradas continuam verdes sem alteração de comportamento.
4. **And** `ScosStartupListener` **não** entra nesta lista apesar de listado como `core` no addendum do PRD — sua assinatura (`onStartupSystem(ApplicationReadyEvent event)`) importa `org.springframework.boot.context.event.ApplicationReadyEvent`, violando a regra de zero-Spring do `core`; ele é migrado na Story 1.8 (`spring`), junto de `ScosOnStartupListener`, que já o consome via `List<ScosStartupListener>`.

## Tasks / Subtasks

- [ ] Task 1: Commit 1 — mover as 6 classes sem alterar comportamento (AC: #1, #4)
  - [ ] Criar módulo Maven `scos-foundation-core`, pacote raiz `br.com.sawcunhaos.foundation.core`
  - [ ] Mover, preservando lógica: `utils/src/main/java/.../utils/DateUtils.java`, `HashUtils.java`, `StringFieldUtils.java`, `sort/PropertiesOrder.java`, `specification/ScosBaseUseCase.java`, `specification/ScosUserAuthentication.java`
  - [ ] **Não mover** `specification/ScosStartupListener.java` nesta story mesmo estando listado como `core` no addendum do PRD — fica em `utils` até a Story 1.8, onde migra para `spring` junto de `ScosOnStartupListener`
  - [ ] Mover os testes correspondentes já existentes (`DateUtilsTest`, `StringFieldUtilsTest` confirmados em `utils/src/test/java/.../utils/`; verificar se há testes para `HashUtils`, `PropertiesOrder`, `ScosBaseUseCase`, `ScosUserAuthentication` e movê-los também, senão registrar a lacuna nas Completion Notes)
  - [ ] Dependências do `core`: `slf4j-api`, `lombok` (optional), `commons-lang3` — nenhuma outra
  - [ ] Commit isolado: só `git mv` + ajuste de pacote/import, zero mudança de lógica
- [ ] Task 2: Commit 2 — ajustar o que precisar (AC: #1)
  - [ ] Só neste segundo commit, separado do primeiro, aplicar qualquer ajuste de comportamento necessário para a classe funcionar isolada de Spring (se houver)
  - [ ] Se nenhum ajuste for necessário, documentar isso explicitamente em vez de forçar um commit vazio
- [ ] Task 3: Regra ArchUnit local de zero-Spring (AC: #2)
  - [ ] Adicionar teste ArchUnit **dentro do próprio módulo `core`** (não em `archtest` — é regra de um módulo só) que falha o build se qualquer classe do módulo importar `org.springframework..`, `jakarta.persistence..` ou `jakarta.servlet..`
  - [ ] Nasce no mesmo commit que cria o módulo (AD-5), não depois
- [ ] Task 4: Confirmar testes verdes (AC: #3)
  - [ ] Rodar a suíte migrada e confirmar 100% verde, sem alteração de asserção/comportamento em relação ao estado anterior à migração

## Dev Notes

- **NFR2/NFR4**: extração em 2 commits separados é requisito explícito do AC, não sugestão — "mover" e "ajustar comportamento" nunca no mesmo commit. Se não houver ajuste de comportamento necessário, ainda assim documentar essa constatação (não pular a distinção silenciosamente).
- Esta é a Fase 3 do plano de origem: "Tudo depende dele; sai primeiro." `core` é o módulo mais citado como dependência por todos os outros módulos novos (`spring`, `validation`, `cache`, `jpa`, `web`, `feign`, os três `-api`).
- **Contrato de exceção (`ScosException`, `ExceptionCode`, `LocaleService`, etc.) NÃO faz parte desta story** — apesar de o addendum do PRD listar essas classes como destino `core`, o AC desta story (Épico 1) restringe explicitamente o escopo às 6 classes citadas. A migração do contrato de exceção para `core` é a Story 2.8 (Épico 2), que roda depois e repontua `audit` para consumir `ScosException` do `core`.
- A regra ArchUnit desta story é a que "mais paga" segundo o plano de origem: "ela impede exatamente o movimento que criou o problema — alguém precisa de 'um lugar qualquer' e joga uma classe Spring no módulo compartilhado."
- `ScosUserAuthentication` é citada no plano de origem como "interface de um método, sem dependência alguma — o exemplo do que deveria estar num core desde o início" — migração deve ser trivial, útil como primeiro caso de teste da regra ArchUnit.

### Project Structure Notes

- Módulo Maven novo: `core/` — depende só de `slf4j-api`, `lombok` (optional), `commons-lang3`.
- `utils/` perde as 6 classes e seus testes migrados.
- `pom.xml` raiz ganha `<module>core</module>`.
- Nenhum consumidor (`audit`, `exception`, `jdempotent`) é repontado para `core` nesta story — isso é escopo de outras stories (Story 2.8 para `exception`/`audit`).

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#scos-foundation-core]
- [Source: etc/doc/plano/plano-decomposicao-utils.md#fase-3--extrair-core-1-2-dias]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Foundation-2026-08-19/ARCHITECTURE-SPINE.md#ad-1--fronteira-e-direção-de-dependência-entre-módulos-adopted]
- [Source: _bmad-output/planning-artifacts/epics.md#story-17-extrair-o-módulo-core]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
