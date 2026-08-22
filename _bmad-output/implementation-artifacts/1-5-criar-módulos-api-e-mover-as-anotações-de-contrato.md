# Story 1.5: Criar módulos `*-api` e mover as anotações de contrato

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor de domínio puro (ex.: SCOS-Flow),
Eu quero anotações `@Auditable`/`@Jdempotent*`/de validação disponíveis sem depender de implementação,
Para poder anotar meu domínio sem carregar Spring/JPA.

## Acceptance Criteria

1. **Given** as anotações hoje presas aos módulos de implementação, **When** `audit-api`, `jdempotent-api` e `validation-api` são criados, **Then** cada um contém apenas `@interface`/`enum`, sem dependência além de `jakarta.validation-api`.
2. **And** uma regra ArchUnit local em cada módulo `*-api` falha o build se qualquer classe de lógica ou dependência de runtime além de `jakarta.validation-api` for adicionada.

## Tasks / Subtasks

- [ ] Task 1: Criar o módulo `scos-foundation-audit-api` (AC: #1)
  - [ ] Novo módulo Maven, pacote `br.com.sawcunhaos.foundation.audit.api`, sem dependência de runtime
  - [ ] Mover `utils/src/main/java/.../annotation/audit/Auditable.java` e `AuditAction.java` para o novo módulo
  - [ ] Repontar `audit/pom.xml` para depender de `scos-foundation-audit-api` em vez de `scos-foundation-utils` para essas duas anotações (o restante da dependência de `audit` em `utils` continua até a Story 1.7/2.8)
  - [ ] README do módulo abre com "este artefato não executa nada; a implementação é `scos-foundation-audit`" (mesma frase exigida pela Story 1.15)
- [ ] Task 2: Criar o módulo `scos-foundation-jdempotent-api` (AC: #1)
  - [ ] Novo módulo Maven, pacote `br.com.sawcunhaos.foundation.jdempotent.api`, sem dependência de runtime
  - [ ] Mover as 5 anotações de `utils/src/main/java/.../annotation/jdempotent/`: `JdempotentId.java`, `JdempotentIgnore.java`, `JdempotentProperty.java`, `JdempotentRequestPayload.java`, `JdempotentResource.java`
  - [ ] Repontar `jdempotent/pom.xml` para depender de `scos-foundation-jdempotent-api`
  - [ ] **Coordenação necessária**: a Story 3.13 (Épico 3) adiciona atributos novos a `@JdempotentResource` (`keySource`, `headerName`, `onMismatch`) — esta story só move a anotação como está hoje; os atributos novos entram depois, no módulo `-api` já criado aqui
  - [ ] README abre com "este artefato não executa nada; a implementação é `scos-foundation-jdempotent`"
- [ ] Task 3: Criar o módulo `scos-foundation-validation-api` (AC: #1)
  - [ ] Novo módulo Maven, pacote `br.com.sawcunhaos.foundation.validation.api`, dependência única permitida: `jakarta.validation-api`
  - [ ] Mover as anotações de validação de `utils/src/main/java/.../validation/`: `CPF.java`, `CNPJ.java`, `TaxIdentifier.java` (a anotação, não o value object `valueobjects/TaxIdentifier.java` — são classes distintas no mesmo nome, confirmar qual é qual antes de mover), `zipcode/ZipCode.java`
  - [ ] Os `ConstraintValidator` (`CnpjValidator`, `CpfValidator`, `TaxIdentifierValidator`, `ZipCodeValidator`) **não** vão para `validation-api` — são lógica de validação, ficam no módulo `validation` (Story 1.9); `validation-api` recebe só a anotação `@interface`
  - [ ] Nenhum consumidor repontado ainda nesta story (os `ConstraintValidator` continuam em `utils` até a Story 1.9)
  - [ ] README abre com "este artefato não executa nada; a implementação é `scos-foundation-validation`"
- [ ] Task 4: Regra ArchUnit por módulo `*-api` (AC: #2)
  - [ ] Em cada um dos 3 módulos novos, adicionar um teste ArchUnit local (usa a dependência gerenciada pela Story 1.4) que falha se qualquer classe do módulo não for `@interface` ou `enum`, e se qualquer dependência de runtime além de `jakarta.validation-api` for declarada
  - [ ] Cada regra nasce no mesmo commit que cria o módulo que protege — não adiar para uma story posterior de "regras cross-módulo" (essas cross-módulo são a Story 1.14, via `archtest`; esta regra é local a cada `*-api` e roda dentro do próprio módulo)

## Dev Notes

- **Divergência a resolver antes de mover, não depois**: o inventário de anotações do plano de origem lista "18 anotações" no `utils`, mas a árvore atual (`utils/src/main/java/.../annotation/request/`) contém **6** classes de request (`ScosRequestDELETE`, `ScosRequestGET`, `ScosRequestMapping`, `ScosRequestPATCH`, `ScosRequestPOST`, `ScosRequestPUT`), e o plano só cita 5 (sem `PATCH`) na tabela de destino. `ScosRequestPATCH` também vai para `web` (mesma família de `ScosRequestGET/POST/PUT/DELETE`), mas **não é escopo desta story** — `web` não ganha módulo `-api` (ver Princípio no addendum do PRD: `@ScosController`/`@ScosRequest*` marcam beans Spring, sem consumidor de domínio puro plausível). Registrar essa contagem revisada nas Completion Notes para manter o inventário da Story 1.1 consistente.
- Esta story **não move** `ScosController`, `ScosRequestMapping`/`GET`/`POST`/`PUT`/`PATCH`/`DELETE`, `ScosRule`, `ScosRuleService` nem `NormalizeStrings` — essas vão direto para `web`/`spring` nas Stories 1.8/1.12, sem módulo `-api` companheiro (decisão já registrada no addendum: só ganham `-api` anotações com consumidor plausível de domínio puro).
- Regra que impede o `*-api` de virar um novo `utils` (citação direta do plano de origem, é o critério de aceite real por trás do AC #1/#2): "ele contém apenas anotações e os enums que as anotações referenciam nos seus atributos. Nada mais — sem helper, sem constante solta, sem interface de serviço."
- **Ordem de execução (NFR1 / plano de origem, Fase 2)**: esta story deve rodar **antes** de qualquer trabalho do Épico 3 (`jdempotent`) que toque `@JdempotentResource`, porque a Story 3.13 desse épico escreve atributos novos na anotação e espera que ela já esteja no módulo `-api` final.
- Modo de falha silencioso a ter em mente (não é escopo desta story corrigir, mas informa o design): depender só do `-api` compila e não faz nada em runtime sem o módulo de implementação no classpath — é isso que a Story 1.15 (log de contagem na subida) e o README padronizado desta story mitigam.

### Project Structure Notes

- 3 módulos Maven novos: `audit-api`, `jdempotent-api`, `validation-api`, cada um `pom.xml` + `README.md` + as anotações movidas + 1 teste ArchUnit local.
- `utils/pom.xml` perde as classes movidas; `audit/pom.xml` e `jdempotent/pom.xml` ganham dependência nova nos respectivos `-api`.
- Adicionar os 3 módulos novos em `<modules>` do `pom.xml` raiz.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#estratégia--api-anotação-separada-da-implementação]
- [Source: etc/doc/plano/plano-decomposicao-utils.md#todas-as-anotações-por-destino]
- [Source: _bmad-output/planning-artifacts/prds/prd-SawCunhaOS-Foundation-2026-08-18/addendum.md#princípio-por-que-web-e-spring-não-ganham-módulo--api-companheiro]
- [Source: _bmad-output/planning-artifacts/epics.md#story-15-criar-módulos-api-e-mover-as-anotações-de-contrato]
- [Source: _bmad-output/planning-artifacts/epics.md#story-313-suportar-header-idempotency-key-como-fonte-de-chave]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
