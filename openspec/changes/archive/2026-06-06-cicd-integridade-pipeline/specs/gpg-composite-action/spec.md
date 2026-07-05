## ADDED Requirements

### Requirement: Composite action reutilizável para setup de GPG
O arquivo `.github/actions/setup-gpg/action.yml` SHALL encapsular o setup completo de GPG (import de chave privada, configuração de loopback pinentry) e ser usada por `publish-snapshot.yml` e `manual-release.yml` eliminando duplicação.

#### Scenario: publish-snapshot usa composite action
- **WHEN** `publish-snapshot.yml` executa o step de setup GPG
- **THEN** a composite action `setup-gpg` é invocada e o GPG fica funcional para assinatura de artefatos

#### Scenario: manual-release usa composite action
- **WHEN** `manual-release.yml` executa o step de setup GPG
- **THEN** a mesma composite action `setup-gpg` é invocada com comportamento idêntico ao anterior
