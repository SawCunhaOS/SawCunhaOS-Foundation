## Why

O perfil `analyze` do Maven já carrega JaCoCo e OWASP dependency-check, mas os resultados nunca chegam ao desenvolvedor: SpotBugs está ausente, relatórios JaCoCo não são publicados como artefatos, `checkstyle` e `dependency-check` têm `continue-on-error: true` (nunca bloqueiam), e o módulo `privacy` está faltando no passo de archive. A análise existe no papel, mas é invisível na prática.

## What Changes

- **SpotBugs** adicionado ao perfil `analyze` do `pom.xml` (nível MEDIUM+) com arquivo de excludes para Lombok/MapStruct em `etc/spotbugs/exclude.xml`
- **JaCoCo HTML report** publicado como artefato no GitHub Actions (retention 7 dias) após cada execução do job `security-check`
- **SpotBugs report** publicado como artefato por módulo (retention 7 dias)
- **`continue-on-error: true` removido** do step `checkstyle:check` — passa a bloquear CI se violar regras
- **`dependency-check`** mantém `continue-on-error: true` — NVD API tem instabilidade conhecida
- **Módulo `privacy`** adicionado ao passo de `Archive artifacts` no `build.yml` (estava ausente)
- Retention de artefatos padronizada para 7 dias (era 3–5 dias)

## Capabilities

### New Capabilities

- `spotbugs-analise`: Análise de bugs potenciais (null pointer, race conditions) via SpotBugs no perfil `analyze` do Maven, com excludes para código gerado e relatório publicado como artefato do CI
- `jacoco-relatorio-ci`: Publicação do relatório HTML de cobertura JaCoCo como artefato do GitHub Actions após cada build, tornando visível a cobertura por módulo sem gate duro

### Modified Capabilities

*(sem modificação de specs existentes — mudanças são de configuração de CI/build)*

## Impact

- `pom.xml` (raiz): perfil `analyze` recebe `spotbugs-maven-plugin` v4.x
- `.github/workflows/build.yml`: Archive de `privacy`, upload de artefatos JaCoCo/SpotBugs, remoção de `continue-on-error` no checkstyle
- `etc/spotbugs/exclude.xml`: novo arquivo de excludes
- Sem impacto em APIs, contratos ou módulos funcionais — mudanças restritas a CI/build
