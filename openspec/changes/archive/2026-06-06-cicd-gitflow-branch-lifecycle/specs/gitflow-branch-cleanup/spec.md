## ADDED Requirements

### Requirement: Branch de release anterior deletada ao publicar minor
Após publicar uma release minor `X.Y.0`, o `release.sh` SHALL deletar `release/X.(Y-1).0` local e remotamente, se existir. A deleção SHALL ser silenciosa se o branch não existir.

#### Scenario: release/1.1.0 deletada ao publicar 1.2.0
- **WHEN** `release.sh minor` é executado em `release/1.2.0`
- **THEN** `release/1.1.0` é deletada local e remotamente após o merge-back

#### Scenario: Deleção silenciosa quando branch anterior não existe
- **WHEN** `release.sh minor` é executado e `release/X.(Y-1).0` não existe
- **THEN** o script prossegue sem erro

#### Scenario: Nenhuma deleção no primeiro minor de uma major
- **WHEN** `release.sh minor` é executado com `CUR_MINOR = 0`
- **THEN** nenhuma branch é deletada (guard contra `release/X.-1.0` inválido)
