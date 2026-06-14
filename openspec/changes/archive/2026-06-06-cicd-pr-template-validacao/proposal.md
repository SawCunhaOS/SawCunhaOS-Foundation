## Why

PRs são abertas sem contexto — sem descrição do que muda, sem módulo identificado, sem checklist. Revisor não sabe o que está olhando e o histórico perde rastreabilidade. Um template existe para resolver isso, mas sem enforcement é ignorado.

## What Changes

- **`.github/pull_request_template.md`** criado com 6 seções obrigatórias: Descrição, Tipo de Mudança, Módulo(s) Afetado(s), Checklist, Breaking Changes, Referências
- **Workflow `validate-pr-template.yml`** criado, bloqueando PRs que não mantenham as headings `##` com conteúdo válido abaixo de cada uma
- **Script `scripts/validate-pr-template.sh`** com lógica de validação por seção, testável localmente
- PRs de bots automatizados (Dependabot, github-actions[bot]) são ignoradas pelo check
- Check é **required status check** — bloqueia merge se falhar

## Capabilities

### New Capabilities

- `pr-template-obrigatorio`: Template de PR com 6 seções estruturadas pré-preenchido automaticamente ao abrir uma PR via UI do GitHub
- `pr-template-validacao-ci`: Workflow de CI que valida se todas as headings obrigatórias do template estão presentes e com conteúdo não-vazio, bloqueando PRs que ignorem a estrutura

### Modified Capabilities

*(sem modificação de specs existentes)*

## Impact

- `.github/pull_request_template.md`: novo arquivo
- `.github/workflows/validate-pr-template.yml`: novo workflow
- `scripts/validate-pr-template.sh`: novo script
- Branch protection rules (configuração manual): adicionar required status check
- Sem impacto em módulos funcionais, APIs ou scripts de release
