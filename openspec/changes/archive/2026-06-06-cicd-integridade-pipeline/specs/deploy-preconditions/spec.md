## ADDED Requirements

### Requirement: deploy.sh valida versão SNAPSHOT antes de publicar
O script `deploy.sh` SHALL verificar que a versão corrente do POM termina em `-SNAPSHOT` antes de executar `mvn deploy`. Se a versão não for SNAPSHOT, o script SHALL falhar com mensagem clara e código de saída não-zero, EXCETO quando a variável de ambiente `RELEASE_DEPLOY=1` estiver definida — usada pelo `release.sh` para publicar a versão de release já fechada.

#### Scenario: Deploy bloqueado para versão de release
- **WHEN** `deploy.sh` é executado e a versão do POM é `1.2.0` (sem SNAPSHOT) e `RELEASE_DEPLOY` não está definido
- **THEN** o script falha com mensagem `"Erro: versão 1.2.0 não é SNAPSHOT. Use release.sh para publicar releases."` e exit code 1

#### Scenario: Deploy permitido para versão SNAPSHOT
- **WHEN** `deploy.sh` é executado e a versão do POM é `1.2.0-SNAPSHOT`
- **THEN** o script prossegue normalmente para `mvn clean deploy`

#### Scenario: Deploy de release permitido via release.sh
- **WHEN** `release.sh` executa `RELEASE_DEPLOY=1 ./scripts/deploy.sh` com a versão de release `1.2.0`
- **THEN** o guard é ignorado e o script prossegue para `mvn clean deploy`

### Requirement: release.sh valida working tree limpo
O script `release.sh` SHALL verificar com `git status --porcelain` que não há arquivos modificados não commitados antes de iniciar qualquer operação. Se o working tree não estiver limpo, SHALL falhar com mensagem e exit code 1.

#### Scenario: Release bloqueado com mudanças não commitadas
- **WHEN** `release.sh` é executado com arquivos modificados no working tree
- **THEN** o script falha com mensagem `"Erro: working tree não está limpo. Faça commit ou stash das mudanças antes do release."` e exit code 1

#### Scenario: Release permitido com working tree limpo
- **WHEN** `release.sh` é executado sem arquivos modificados
- **THEN** o script prossegue normalmente
