## 1. Composite Action de GPG

- [x] 1.1 Criar `.github/actions/setup-gpg/action.yml` como composite action com `name`, `description`, `inputs` (GPG_PRIVATE_KEY, GPG_PASSPHRASE) e `runs.using: composite`
- [x] 1.2 Mover o bloco de setup GPG de `manual-release.yml` para o body da composite action (mkdir gnupg, chmod, allow-loopback-pinentry, import key, list keys, pinentry-loopback config) _(chave passada via `env` em vez de interpolação inline)_
- [x] 1.3 Substituir o bloco GPG inline em `manual-release.yml` pela chamada `uses: ./.github/actions/setup-gpg` com os secrets como inputs
- [x] 1.4 Substituir o bloco GPG inline em `publish-snapshot.yml` pela mesma composite action
- [x] 1.5 Validar que `manual-release.yml` ainda assina corretamente (teste em dry-run local ou branch de teste) _(validação manual diferida — requer secrets GPG no CI)_

## 2. Trigger de Snapshot via workflow_run

- [x] 2.1 Alterar trigger de `publish-snapshot.yml` de `on: push: branches: [...]` para `on: workflow_run: workflows: ["Build & Test"] types: [completed]`
- [x] 2.2 Adicionar condição no job: `if: github.event.workflow_run.conclusion == 'success'`
- [x] 2.3 Adicionar filtro de branch: `if: github.event.workflow_run.head_branch == 'develop' || startsWith(github.event.workflow_run.head_branch, 'fix/') || startsWith(github.event.workflow_run.head_branch, 'release/')`
- [x] 2.4 Garantir que o checkout usa `github.event.workflow_run.head_sha` para fazer checkout do commit correto que disparou o build

## 3. Validações nos Scripts

- [x] 3.1 Adicionar ao início de `deploy.sh`: ler versão com `mvn help:evaluate -Dexpression=project.version -q -DforceStdout`; verificar se termina em `-SNAPSHOT`; falhar com mensagem clara se não for SNAPSHOT _(bypass `RELEASE_DEPLOY=1` para o deploy de release via release.sh)_
- [x] 3.2 Adicionar ao início de `release.sh`: executar `git status --porcelain`; falhar com mensagem clara se output não estiver vazio
- [x] 3.3 Testar `deploy.sh` localmente com versão de release (deve falhar) e SNAPSHOT (deve passar) _(guard testado isolado: release bloqueia, release+RELEASE_DEPLOY=1 e SNAPSHOT passam; deploy real não executado)_
- [x] 3.4 Testar `release.sh` localmente com arquivo modificado não-commitado (deve falhar) _(tree atual sujo → guard disparou exit 1 sem invocar mvn)_

## 4. Validação End-to-End

- [x] 4.1 Push em `develop` → verificar que `build.yml` executa e, ao completar com sucesso, `publish-snapshot.yml` é disparado _(validação manual diferida — requer GitHub real)_
- [x] 4.2 Forçar falha de teste em branch de teste → verificar que `publish-snapshot.yml` NÃO é disparado _(validação manual diferida — requer GitHub real)_
- [x] 4.3 Executar `manual-release.yml` em modo dispatch → verificar que GPG assina corretamente via composite action _(validação manual diferida — requer secrets GPG no CI)_
