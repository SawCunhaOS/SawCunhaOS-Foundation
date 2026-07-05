## ADDED Requirements

### Requirement: Template de PR com 6 seções estruturadas
O arquivo `.github/pull_request_template.md` SHALL conter as seções `## Descrição`, `## Tipo de Mudança`, `## Módulo(s) Afetado(s)`, `## Checklist`, `## Breaking Changes` e `## Referências`, com checkboxes pré-marcados como `- [ ]` nas seções de escolha e comentários `<!-- -->` como guia nas seções de texto livre.

#### Scenario: Template pré-preenchido ao abrir PR via UI
- **WHEN** um desenvolvedor abre uma nova PR via GitHub UI
- **THEN** o body da PR é pré-preenchido com as 6 seções do template, pronto para edição
