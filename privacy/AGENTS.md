<!-- bmad:context -->
<!-- Verified 2026-08-29 against 4dcab58. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->

## privacy

## Conventions that differ from defaults

- Não adicione dependência de `utils`/`core` neste módulo — deliberado, evita o ciclo `utils↔privacy` (comentário em `pom.xml`, bloco `<dependencies>`).
- Beans Spring aqui são opcionais (`optional=true`, `@ConditionalOnMissingBean`) — o módulo precisa continuar funcionando sem Spring no classpath, via `MaskingEngine.fromYaml(...)`.

## Running and verifying

- Benchmarks JMH (`src/jmh`) só compilam/rodam com `-Pperf`; `mvn test` não os toca.

<!-- /bmad:context -->
