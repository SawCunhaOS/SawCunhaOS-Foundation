## ADDED Requirements

### Requirement: GitHub Release criado automaticamente ao publicar tag
O workflow `release-notes.yml` SHALL criar um GitHub Release para a tag detectada usando `gh release create`, com body gerado por `git-cliff --latest`, sem interação manual.

#### Scenario: GitHub Release criado para nova tag
- **WHEN** a tag `1.2.0` é pushed e o workflow executa
- **THEN** um GitHub Release `1.2.0` é criado com release notes em markdown agrupadas por módulo, visível na UI do GitHub em Releases

#### Scenario: GitHub Release não duplicado se já existir
- **WHEN** um GitHub Release para a tag `1.2.0` já existe (ex: criado manualmente)
- **THEN** o workflow atualiza as notas do release existente via `gh release edit` em vez de falhar
