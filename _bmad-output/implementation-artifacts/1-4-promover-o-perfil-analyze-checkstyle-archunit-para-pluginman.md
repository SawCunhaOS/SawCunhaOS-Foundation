# Story 1.4: Promover o perfil `analyze` (Checkstyle + ArchUnit) para `pluginManagement` do POM pai

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor do `scos-foundation`,
Eu quero que o perfil `analyze` cubra 100% dos módulos a partir do POM pai,
Para que nenhum módulo novo nasça sem o gate mecânico.

## Acceptance Criteria

1. **Given** o perfil `analyze` hoje presente em 4 de 5 módulos, **When** ele é promovido a `pluginManagement` do POM pai, **Then** todo módulo do reactor (existente e futuro) herda o perfil sem precisar declará-lo individualmente.
2. **And** a dependência `com.tngtech.archunit:archunit-junit5:1.5.0` (escopo `test`) é adicionada ao gerenciamento de dependências do POM pai.
3. **And** o `failOnViolation` do Checkstyle **não** é ativado globalmente nesta story — o mecanismo fica disponível, mas bloquear o build do reactor inteiro só acontece na Story 4.16, depois que todo módulo já tiver Javadoc.

## Tasks / Subtasks

- [ ] Task 1: Confirmar o estado atual como baseline (AC: #1)
  - [ ] Confirmado nesta análise: `utils`, `exception`, `jdempotent` e `audit` já têm o perfil `analyze` declarado individualmente; `privacy` é o módulo sem o perfil (o "1 de 5" citado no AC)
  - [ ] Rodar `mvn -Panalyze verify` no `jdempotent` (que já tem o perfil) e documentar se o Checkstyle de fato falha o build para um método público sem Javadoc — esse diagnóstico é a mesma pergunta que a Story 4.1 formaliza depois; registrar aqui o achado bruto para reaproveitar lá
- [ ] Task 2: Mover o bloco `<profile id="analyze">` para `pluginManagement` do `pom.xml` raiz (AC: #1)
  - [ ] Extrair o conteúdo do perfil `analyze` de um dos 4 módulos que já o têm (ex.: `jdempotent/pom.xml`) como template canônico
  - [ ] Adicionar o mesmo bloco de perfil no `pom.xml` raiz, dentro de `<profiles>` do pai (não em `pluginManagement` de `<build>` — perfis não vivem em `pluginManagement`; os *plugins declarados dentro do perfil* é que vão para `pluginManagement` de modo que cada módulo apenas ative o perfil, herdando plugin+config sem redeclarar)
  - [ ] Remover a declaração duplicada do perfil `analyze` dos 4 módulos (`utils`, `exception`, `jdempotent`, `audit`) — eles passam a herdar do pai
  - [ ] `privacy` passa a herdar o perfil automaticamente, sem precisar de nenhuma edição no seu próprio `pom.xml`
  - [ ] Atenção aos `<excludes>` do Jacoco hoje configurados em `utils/pom.xml` — apontam para o pacote legado `br/com/insidesoftwares/commons/**` (não bate com o pacote real atual `br/com/sawcunhaos/foundation/utils/**`); ao centralizar, não copiar esse exclude obsoleto para o pai — decidir com o time se ele deve ser descartado ou corrigido antes da promoção
- [ ] Task 3: Adicionar ArchUnit ao dependency management do pai (AC: #2)
  - [ ] Adicionar `com.tngtech.archunit:archunit-junit5:1.5.0` (escopo `test`) em `<dependencyManagement>` do `pom.xml` raiz — só gerenciamento de versão, cada módulo que precisar declara a dependência sem versão
  - [ ] Confirmar que a versão `1.5.0` não é gerenciada por nenhum BOM já importado (`scos-bom`) — se for, usar a versão gerenciada em vez de forçar
- [ ] Task 4: Garantir que `failOnViolation` continua desligado globalmente (AC: #3)
  - [ ] Auditar a configuração do `maven-checkstyle-plugin` movida para o pai: confirmar que nenhuma execução `check` com `failOnViolation=true` é introduzida nesta story
  - [ ] Deixar registrado no Dev Notes/README que a ativação do gate global é escopo exclusivo da Story 4.16, para não haver dúvida futura

## Dev Notes

- **NFR2** (build verde a cada commit): mover um perfil inteiro de 4 módulos para o pai é uma mudança estrutural — fazer em um commit isolado, com `mvn verify` (sem `-Panalyze`, comportamento default) e depois `mvn -Panalyze verify` (com o perfil ativo) verdes antes de prosseguir para a próxima story.
- Pré-condição desta story confirmada nesta análise (não redescobrir): o `analyze` profile de `utils/pom.xml` inclui Jacoco (mínimo 80% via `COVEREDRATIO`), `findbugs-maven-plugin` (duplicado duas vezes no XML atual — vale corrigir a duplicação ao consolidar), `dependency-check-maven` (OWASP) e `maven-checkstyle-plugin` apontando para `etc/devops/checkstyle/checkstyle.xml`.
- O `configLocation` do Checkstyle usa `${project.parent.basedir}` — ao mover para o pai, essa referência relativa muda de significado (o pai não tem "parent.basedir" da mesma forma); ajustar para `${maven.multiModuleProjectDirectory}` ou caminho equivalente que funcione tanto no pai quanto em cada módulo filho.
- Este é o mesmo veículo mencionado no AD-5 da arquitetura: ArchUnit no perfil `analyze` promovido a `pluginManagement`, cobrindo 100% dos módulos existentes e futuros — inclui `privacy`, hoje sem o perfil.
- **Não é escopo desta story** escrever nenhuma regra ArchUnit — só disponibilizar a dependência gerenciada. As primeiras regras reais nascem na Story 1.5 (`*-api`) e Story 1.7 (`core`), cada uma no commit que cria o módulo que protege (AD-5).

### Project Structure Notes

- Arquivo modificado: `pom.xml` (raiz) — ganha `<profiles><profile id="analyze">...</profile></profiles>` e entrada em `<dependencyManagement>` para ArchUnit.
- Arquivos modificados: `utils/pom.xml`, `exception/pom.xml`, `jdempotent/pom.xml`, `audit/pom.xml` — perdem o bloco `<profile id="analyze">` duplicado.
- `privacy/pom.xml` não precisa de nenhuma edição — passa a herdar o perfil do pai automaticamente.

### References

- [Source: pom.xml#L84-L90]
- [Source: utils/pom.xml#L259-L363]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Foundation-2026-08-19/ARCHITECTURE-SPINE.md#ad-5--archunit-como-mecanismo-de-imposição-das-regras-de-módulo-adopted]
- [Source: etc/doc/plano/plano-decomposicao-utils.md#9-como-impedir-que-o-utils-volte-a-crescer]
- [Source: _bmad-output/planning-artifacts/epics.md#story-14-promover-o-perfil-analyze-checkstyle--archunit-para-pluginmanagement-do-pom-pai]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
