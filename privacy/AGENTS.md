## privacy

## Conventions that differ from defaults

- Não adicione dependência de `utils`/`core` neste módulo — deliberado, evita o ciclo `utils↔privacy` (comentário em `pom.xml`, bloco `<dependencies>`).
- Beans Spring aqui são opcionais (`optional=true`, `@ConditionalOnMissingBean`) — o módulo precisa continuar funcionando sem Spring no classpath, via `MaskingEngine.fromYaml(...)`.

## Running and verifying

- Benchmarks JMH (`src/jmh`) só compilam/rodam com `-Pperf`; `mvn test` não os toca.
