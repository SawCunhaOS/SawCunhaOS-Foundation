## ADDED Requirements

### Requirement: fix/* só pode ter target na próxima minor ou próximo fix
O check SHALL bloquear PRs onde o HEAD é `fix/X.Y.Z` e o BASE não é `fix/X.Y.(Z+1)` (se existir remotamente) nem `release/X.(Y+1).0`.

#### Scenario: fix para release next minor é permitido
- **WHEN** PR é aberta de `fix/1.1.2` para `release/1.2.0`
- **THEN** o check passa com sucesso

#### Scenario: fix para develop direto é bloqueado
- **WHEN** PR é aberta de `fix/1.1.2` para `develop`
- **THEN** o check falha com mensagem indicando os targets permitidos

#### Scenario: fix para next fix (se existir) é permitido
- **WHEN** PR é aberta de `fix/1.1.2` para `fix/1.1.3` e `fix/1.1.3` existe remotamente
- **THEN** o check passa com sucesso

### Requirement: release/* só pode ter target em develop ou próxima minor
O check SHALL bloquear PRs onde o HEAD é `release/X.Y.0` e o BASE não é `develop` nem `release/X.(Y+1).0`.

#### Scenario: release para develop é permitido
- **WHEN** PR é aberta de `release/1.2.0` para `develop`
- **THEN** o check passa com sucesso

#### Scenario: release para próxima minor é permitido (pós-major)
- **WHEN** PR é aberta de `release/1.2.0` para `release/1.3.0`
- **THEN** o check passa com sucesso

#### Scenario: release para fix é bloqueado
- **WHEN** PR é aberta de `release/1.2.0` para `fix/1.2.1`
- **THEN** o check falha com mensagem indicando os targets permitidos

### Requirement: feature/* pode qualquer branch de destino
O check SHALL passar sem verificação quando o HEAD é `feature/*`.

#### Scenario: feature para develop é permitido
- **WHEN** PR é aberta de `feature/minha-feature` para `develop`
- **THEN** o check passa com sucesso

#### Scenario: feature para release é permitido
- **WHEN** PR é aberta de `feature/minha-feature` para `release/1.2.0`
- **THEN** o check passa com sucesso

### Requirement: Branches não reconhecidos passam sem verificação
O check SHALL retornar success para branches cujo nome não corresponde a `feature/*`, `fix/*` ou `release/*`.

#### Scenario: Branch de bot passa sem verificação
- **WHEN** PR de `dependabot/maven/...` é aberta para qualquer branch
- **THEN** o check passa sem verificar o target
