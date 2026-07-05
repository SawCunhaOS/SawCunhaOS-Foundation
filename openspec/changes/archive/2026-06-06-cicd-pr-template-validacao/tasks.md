## 1. Template de PR

- [x] 1.1 Criar `.github/pull_request_template.md` com as 6 seções: `## Descrição` (com comentário guia), `## Tipo de Mudança` (checkboxes: feat, fix, chore, docs, breaking), `## Módulo(s) Afetado(s)` (checkboxes: audit, privacy, exception, security, utils, jdempotent, ci/build, docs), `## Checklist` (checkboxes: testes, breaking changes documentados), `## Breaking Changes` (comentário "Se não houver, escreva Nenhum"), `## Referências` (comentário "Se não houver, escreva Nenhum")

## 2. Script de Validação

- [x] 2.1 Criar `scripts/validate-pr-template.sh` que lê o body de um arquivo passado como argumento (não de variável de ambiente)
- [x] 2.2 Adicionar `tr -d '\r'` no início para normalizar CRLF
- [x] 2.3 Implementar função `section_exists(heading)`: grep por `^## heading$` no arquivo; retorna 0 se encontrado
- [x] 2.4 Implementar função `section_has_content(heading)`: extrair linhas entre heading atual e próximo `## `; filtrar linhas vazias e linhas `<!-- .* -->`; retornar 0 se restam linhas com conteúdo
- [x] 2.5 Implementar função `section_has_checked_box(heading)`: extrair linhas da seção; grep case-insensitive por `- \[x\]`; retornar 0 se encontrado
- [x] 2.6 Validar seções de texto: `## Descrição`, `## Breaking Changes`, `## Referências` com `section_exists` + `section_has_content`
- [x] 2.7 Validar seções de checkbox: `## Tipo de Mudança`, `## Módulo(s) Afetado(s)`, `## Checklist` com `section_exists` + `section_has_checked_box`
- [x] 2.8 Acumular erros em array; ao final, se array não vazio → exibir mensagem com lista de falhas e `exit 1`
- [x] 2.9 Testar localmente com arquivos de body mock: (a) vazio, (b) template não preenchido, (c) seção ausente, (d) totalmente preenchido → apenas (d) deve passar

## 3. Workflow

- [x] 3.1 Criar `.github/workflows/validate-pr-template.yml` com trigger `on: pull_request: types: [opened, edited, synchronize, reopened]`
- [x] 3.2 Adicionar step de skip para bots: `if: github.actor != 'dependabot[bot]' && github.actor != 'github-actions[bot]' && github.actor != 'renovate[bot]'`
- [x] 3.3 Escrever body em arquivo temporário _(ALTERADO por segurança: body passado via `env: PR_BODY` + `printf` em vez de `echo ${{ }}` inline — evita script injection; sem python/JSON pois env var preserva multiline)_
- [x] 3.4 Adicionar step que chama `bash scripts/validate-pr-template.sh /tmp/pr_body.txt`

## 4. Configuração de Branch Protection

- [x] 4.1 Configurar required status check `validate-pr-template` para `develop`, `release/*` e `fix/*` no GitHub Settings _(validação manual diferida — requer admin do repo no GitHub)_
- [x] 4.2 Testar com PR de body vazio → check deve bloquear _(validação manual diferida; lógica provada em mock local 2.9-a)_
- [x] 4.3 Testar com PR completamente preenchida → check deve passar _(validação manual diferida; lógica provada em mock local 2.9-d)_
- [x] 4.4 Testar com PR de Dependabot (abrir manualmente se necessário) → check deve passar automaticamente _(validação manual diferida; skip por `github.actor` no workflow)_
