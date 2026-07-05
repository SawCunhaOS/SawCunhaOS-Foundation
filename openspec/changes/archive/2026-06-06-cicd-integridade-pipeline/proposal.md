## Why

O `publish-snapshot.yml` publica snapshots com `mvn deploy -DskipTests` de forma completamente independente do `build.yml` — um snapshot pode chegar ao repositório mesmo que os testes falhem. Além disso, o setup de GPG está duplicado entre dois workflows e os scripts `deploy.sh` e `release.sh` não têm nenhuma validação de pré-condição.

## What Changes

- **`publish-snapshot.yml`** passa a depender do `build.yml` via trigger `workflow_run` — só executa quando o build passar na mesma branch
- **`deploy.sh`** ganha validações: versão deve ser `-SNAPSHOT` e working tree deve estar limpo antes de qualquer operação Maven
- **`release.sh`** ganha validação de working tree limpo no início do script
- **Composite action `setup-gpg`** criada em `.github/actions/setup-gpg/action.yml`, eliminando o bloco GPG duplicado entre `publish-snapshot.yml` e `manual-release.yml`
- **`-DskipTests` mantido** no deploy de snapshot — testes já rodaram no `build.yml`, rodar novamente dobra o tempo sem ganho

## Capabilities

### New Capabilities

- `snapshot-dependency-build`: Snapshots só são publicados após o job de build+testes passar na mesma branch — via `workflow_run` trigger
- `deploy-preconditions`: Scripts `deploy.sh` e `release.sh` validam pré-condições (versão SNAPSHOT, working tree limpo) antes de qualquer operação
- `gpg-composite-action`: Setup de GPG extraído para composite action reutilizável, eliminando duplicação entre workflows

### Modified Capabilities

*(sem modificação de specs existentes)*

## Impact

- `.github/workflows/publish-snapshot.yml`: trigger muda de `push` para `workflow_run`; usa composite action GPG
- `.github/workflows/manual-release.yml`: usa composite action GPG
- `.github/actions/setup-gpg/action.yml`: novo arquivo
- `scripts/deploy.sh`: adiciona guards de versão e working tree
- `scripts/release.sh`: adiciona guard de working tree
- Sem impacto em módulos funcionais ou APIs
