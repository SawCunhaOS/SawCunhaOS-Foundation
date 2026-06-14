## ADDED Requirements

### Requirement: SpotBugs configurado no perfil analyze
O perfil `analyze` do `pom.xml` SHALL incluir `spotbugs-maven-plugin` com nível de esforço `default` e threshold MEDIUM, usando `etc/spotbugs/exclude.xml` para suprimir falsos positivos de código gerado (Lombok, MapStruct).

#### Scenario: Build com perfil analyze detecta bug potencial
- **WHEN** `mvn -Panalyze spotbugs:check` é executado e existe uma NPE potencial no código
- **THEN** o build falha com relatório indicando a classe, linha e categoria do bug

#### Scenario: Falsos positivos de Lombok são suprimidos
- **WHEN** `mvn -Panalyze spotbugs:check` é executado em código que usa `@Builder` do Lombok
- **THEN** o build não reporta warnings de null-return gerados pelo bytecode do Lombok

### Requirement: Relatório SpotBugs publicado como artefato de CI
O job `security-check` no `build.yml` SHALL fazer upload do relatório SpotBugs (XML/HTML) como artefato do GitHub Actions com retention de 7 dias, mesmo quando o step de check falhar.

#### Scenario: Relatório disponível após build com falha
- **WHEN** `spotbugs:check` falha por bug encontrado
- **THEN** o artefato `spotbugs-report` está disponível para download na UI do GitHub Actions

#### Scenario: Relatório disponível após build com sucesso
- **WHEN** `spotbugs:check` passa sem bugs encontrados
- **THEN** o artefato `spotbugs-report` está disponível com o relatório vazio de bugs
