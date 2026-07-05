## ADDED Requirements

### Requirement: Convenção de commits com scope documentada
O arquivo `etc/doc/commit-convention.md` SHALL documentar os scopes válidos por módulo, o formato de commit convencional (`type(scope): description`) e exemplos de breaking changes, com tabela de mapeamento scope → módulo Maven.

#### Scenario: Documento acessível no repositório
- **WHEN** um colaborador quer saber qual scope usar para o módulo audit
- **THEN** `etc/doc/commit-convention.md` contém a tabela com `audit → scos-foundation-audit` e exemplos de commits válidos

#### Scenario: Breaking change documentado corretamente
- **WHEN** um commit introduz breaking change
- **THEN** o documento mostra o formato `feat(audit)!: descrição` ou footer `BREAKING CHANGE:` como opções válidas
