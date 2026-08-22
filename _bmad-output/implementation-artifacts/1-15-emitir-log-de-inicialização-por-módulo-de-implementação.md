# Story 1.15: Emitir log de inicialização por módulo de implementação

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor operando em produção,
Eu quero um log `INFO` na subida de cada módulo de implementação informando quantas classes anotadas foram encontradas,
Para detectar um módulo `-api` usado sem a implementação correspondente no classpath.

## Acceptance Criteria

1. **Given** uma aplicação consumidora com anotações de `jdempotent-api`/`audit-api`/`validation-api` no classpath, **When** a aplicação sobe com o módulo de implementação correspondente presente, **Then** um log `INFO` é emitido contando quantas classes anotadas foram encontradas.
2. **And** módulos `*-api` não emitem esse log (não têm lógica de runtime).
3. **And** cada README de módulo `-api` abre com a frase "este artefato não executa nada; a implementação é `scos-foundation-<x>`".

## Tasks / Subtasks

- [ ] Task 1: Listener de contagem no módulo `jdempotent` (AC: #1)
  - [ ] Implementar um `ApplicationListener<ApplicationReadyEvent>` (ou reaproveitar mecanismo de startup já existente no módulo, se houver) que faz scan do classpath da aplicação consumidora por classes anotadas com `@JdempotentResource`/`@JdempotentId` e loga em `INFO` a contagem encontrada
- [ ] Task 2: Listener de contagem no módulo `audit` (AC: #1)
  - [ ] Mesmo mecanismo, contando classes anotadas com `@Auditable`
- [ ] Task 3: Listener de contagem no módulo `validation` (AC: #1)
  - [ ] Mesmo mecanismo, contando campos/classes anotados com `@CPF`/`@CNPJ`/`@TaxIdentifier`/`@ZipCode` — este é o caso menos direto dos três, porque a validação Bean Validation já faz seu próprio scan implícito; o log aqui é só observabilidade (contagem visível), não integração nova com o mecanismo de validação existente
- [ ] Task 4: Confirmar que os módulos `*-api` não emitem log (AC: #2)
  - [ ] `audit-api`, `jdempotent-api`, `validation-api` (Story 1.5) não recebem nenhum listener — são só anotações, sem lógica de runtime, por definição
- [ ] Task 5: Confirmar a frase padrão nos READMEs dos `*-api` (AC: #3)
  - [ ] Verificar que os READMEs criados na Story 1.5 já abrem com "este artefato não executa nada; a implementação é `scos-foundation-<x>`" (a Story 1.5 já pede essa frase); se algum README não seguir o padrão exato, corrigir aqui

## Dev Notes

- Depende da Story 1.5 (`*-api` criados) já concluída — o log detecta justamente o cenário "anotação do `-api` presente, implementação ausente ou vice-versa".
- Modo de falha que este log mitiga (citação do plano de origem): "depender só do `-api` compila e não faz nada em tempo de execução — sem o módulo de implementação no classpath, não há listener nem aspecto para ler a anotação. Nenhum erro, nenhum aviso." O log de contagem é o sinal visível: zero classes anotadas com o módulo ligado, ou anotações presentes sem o módulo, viram sinal detectável em produção.
- Esta é a última story do Épico 1 — depende implicitamente de `jdempotent` e `audit` já estarem repontados para os `-api` correspondentes (Story 1.5) para o scan ter algo consistente para contar.
- Não é escopo desta story mudar o comportamento funcional de `jdempotent`/`audit`/`validation` — só adicionar o log de contagem na subida.

### Project Structure Notes

- Arquivos novos: um listener de startup por módulo de implementação (`jdempotent`, `audit`, `validation`), seguindo o padrão que já existe no repo para listeners on-startup (`ScosOnStartupListener`/`ScosStartupListener`, migrados para `spring` na Story 1.8 — usar o mesmo padrão de extensão, se aplicável, ou `ApplicationListener<ApplicationReadyEvent>` direto).
- Nenhum arquivo em `audit-api`, `jdempotent-api`, `validation-api` é tocado (AC #2 exige ausência de lógica ali).

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#o-modo-de-falha-a-documentar]
- [Source: _bmad-output/implementation-artifacts/1-5-criar-módulos-api-e-mover-as-anotações-de-contrato.md]
- [Source: _bmad-output/implementation-artifacts/1-8-extrair-o-módulo-spring.md] (padrão `ScosOnStartupListener`/`ScosStartupListener`)
- [Source: _bmad-output/planning-artifacts/epics.md#story-115-emitir-log-de-inicialização-por-módulo-de-implementação]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
