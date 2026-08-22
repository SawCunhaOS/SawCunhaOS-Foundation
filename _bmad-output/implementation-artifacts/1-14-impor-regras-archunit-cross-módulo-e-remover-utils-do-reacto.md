# Story 1.14: Impor regras ArchUnit cross-módulo e remover `utils` do reactor

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero que o build falhe automaticamente se qualquer módulo violar a fronteira de dependência global,
Para que a decomposição não regrida silenciosamente.

## Acceptance Criteria

1. **Given** o módulo `archtest` (Story 1.6) e todos os módulos extraídos (Stories 1.5–1.13), **When** as regras cross-módulo são adicionadas, **Then** o build falha se algum módulo além de uma aplicação consumidora depender de `web`.
2. **And** o build falha se houver ciclo entre quaisquer dois módulos do reactor.
3. **And** após a última classe migrada, `scos-foundation-utils` é removido do reactor Maven.
4. **And** pendência conhecida, não bloqueante desta story: FR-19/AD-1 definem regras negativas explícitas só para `core`, `spring`, `*-api` e a proibição genérica de depender de `web` — não há regra declarada especificamente para o que `cache`/`jpa`/`feign`/`audit-api` **não podem** importar. A cobertura "100%" de FR-20 é satisfeita aqui pelas regras cross-módulo (ciclo, dependência de `web`) mais o próprio grafo de dependência Maven; se uma regra negativa específica para esses módulos for necessária, isso deve ser levantado com quem mantém o PRD/Architecture antes da implementação, não decidido ad-hoc nesta story.

## Tasks / Subtasks

- [ ] Task 0: Confirmar pré-condição — todas as Stories 1.5 a 1.13 concluídas (todas as classes migradas para os módulos novos)
  - [ ] Verificar no `sprint-status.yaml` que 1-5 até 1-13 estão `done` antes de iniciar; esta story depende de que **nenhuma classe reste em `utils`**
- [ ] Task 1: Regra cross-módulo "nada depende de `web` exceto aplicações" (AC: #1)
  - [ ] Adicionar em `archtest` (Story 1.6) uma classe de teste ArchUnit que falha se qualquer módulo do reactor (`core`, `spring`, `validation`, `cache`, `jpa`, `feign`, `audit`, `jdempotent`, os três `-api`, `privacy`) importar pacotes de `br.com.sawcunhaos.foundation.web`
- [ ] Task 2: Regra cross-módulo "sem ciclo entre módulos" (AC: #2)
  - [ ] Adicionar em `archtest` uma classe de teste ArchUnit de detecção de ciclo (`slices().matching(...).should().beFreeOfCycles()` ou equivalente) cobrindo todos os módulos do reactor
- [ ] Task 3: Remover `utils` do reactor (AC: #3)
  - [ ] Confirmar (via `grep`) que nenhuma classe restante em `utils/src/main/java` está sem módulo de destino migrado — o inventário congelado na Story 1.1 é a referência de conferência
  - [ ] Remover `<module>utils</module>` de `pom.xml` raiz
  - [ ] Remover a pasta `utils/` do repositório
  - [ ] Remover qualquer `<dependency>scos-foundation-utils</dependency>` residual em `audit/pom.xml`, `exception/pom.xml`, `jdempotent/pom.xml` — nesta altura da sequência, `exception` ainda não foi migrado (Épico 2, Stories 2.8/2.9) e `audit`/`jdempotent` só devem depender dos módulos novos correspondentes; se `exception` ainda depender de `utils` para o contrato de exceção, documentar essa dependência residual como aceita até o Épico 2 rodar, não como bloqueio desta story
- [ ] Task 4: Registrar a pendência conhecida (AC: #4)
  - [ ] Deixar explícito nas Completion Notes que a ausência de regra negativa específica para `cache`/`jpa`/`feign`/`audit-api` é uma decisão consciente desta story, não uma omissão — e que qualquer regra negativa adicional para esses módulos exige alinhamento com quem mantém PRD/Architecture antes de ser implementada ad-hoc

## Dev Notes

- Esta é a Fase 5 do plano de origem: "remoção e documentação" — última fase antes do piso de documentação do Épico 4.
- **Ordem de execução (NFR1)**: esta story é a última do bloco de extração modular — só faz sentido depois que Stories 1.5 a 1.13 já moveram todas as classes. Rodar fora de ordem quebra a AC #3 (classe sem destino ainda em `utils`).
- Regras que citam **um módulo só** (ex.: "`core` não importa Spring", já criada na Story 1.7) não são repetidas aqui — só as regras que citam mais de um módulo entram em `archtest`, conforme o princípio já estabelecido na Story 1.6.
- A dependência residual de `exception` em `utils` (contrato de exceção) é esperada nesta altura — a extinção completa de `exception` só acontece nas Stories 2.8/2.9 do Épico 2, que dependem de `core` já existir (Story 1.7, já concluída neste ponto). Não forçar essa migração aqui, é fora do escopo do Épico 1.

### Project Structure Notes

- `archtest/src/test/java/` ganha as primeiras classes de teste reais (regra "sem depender de `web`", regra "sem ciclo").
- `utils/` é removido do repositório e de `pom.xml` raiz.
- Verificar `dependencyManagement` do `pom.xml` raiz por qualquer entrada residual de `scos-foundation-utils` a remover junto.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#fase-5--remoção-e-documentação-1-dia]
- [Source: etc/doc/plano/plano-decomposicao-utils.md#9-como-impedir-que-o-utils-volte-a-crescer]
- [Source: _bmad-output/implementation-artifacts/1-1-congelar-o-inventário-classe-módulo.md]
- [Source: _bmad-output/planning-artifacts/epics.md#story-114-impor-regras-archunit-cross-módulo-e-remover-utils-do-reactor]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
