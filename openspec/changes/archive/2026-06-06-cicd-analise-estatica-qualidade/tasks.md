## 1. SpotBugs — Configuração Maven

- [x] 1.1 Adicionar `spotbugs-maven-plugin` v4.x ao perfil `analyze` no `pom.xml` com goals `check` e `report`, threshold `Medium`, effort `Default`
- [x] 1.2 Criar `etc/spotbugs/exclude.xml` com regras de exclusão para anotações Lombok (`@lombok.*`), classes geradas por MapStruct (`*MapperImpl`) e padrões de bytecode gerado (`*$Builder`, `*$$*`)
- [x] 1.3 Configurar `<excludeFilterFile>etc/spotbugs/exclude.xml</excludeFilterFile>` no plugin do `pom.xml`
- [x] 1.4 Rodar `mvn -Panalyze spotbugs:check` localmente e validar que: (a) não há falsos positivos de Lombok, (b) tempo total < 5min por módulo

## 2. GitHub Actions — Archive e Upload de Artefatos

- [x] 2.1 Adicionar step `Archive privacy module artifacts` no job `build` do `build.yml` para `privacy/target/` com retention 5 dias (padrão dos demais)
- [x] 2.2 Padronizar retention de todos os steps de archive para 7 dias (hoje são 3–5)
- [x] 2.3 Adicionar step `Upload JaCoCo report` no job `security-check` com `upload-artifact@v4`, path `**/target/site/jacoco/`, nome `jacoco-report-${{ matrix.java-version }}`, retention 7 dias, `if: always()`
- [x] 2.4 Adicionar step `Upload SpotBugs report` no job `security-check` com `upload-artifact@v4`, path `**/target/spotbugs*.xml` e `**/target/spotbugsXml.xml`, nome `spotbugs-report-${{ matrix.java-version }}`, retention 7 dias, `if: always()`

## 3. GitHub Actions — Correção do security-check

- [x] 3.1 Remover `continue-on-error: true` do step `Run checkstyle` no job `security-check`
- [x] 3.2 Confirmar que `continue-on-error: true` está preservado no step `Run dependency check`
- [x] 3.3 Adicionar step `mvn -Panalyze spotbugs:check` no job `security-check` após checkstyle, com `continue-on-error: false`
- [x] 3.4 Adicionar step `mvn -Panalyze jacoco:report` no job `security-check` com `if: always()` para garantir geração mesmo quando check anterior falha

## 4. Validação

- [x] 4.1 Abrir PR com as mudanças e verificar que o job `security-check` executa SpotBugs e JaCoCo
- [x] 4.2 Confirmar que os artefatos `jacoco-report` e `spotbugs-report` aparecem na UI do GitHub Actions
- [x] 4.3 Confirmar que o artefato `maven-build-privacy-25` aparece no job `build`
- [x] 4.4 Verificar tempo total do job `security-check` < 20 minutos
