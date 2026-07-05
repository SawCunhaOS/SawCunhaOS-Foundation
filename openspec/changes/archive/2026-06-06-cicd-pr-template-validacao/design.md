## Context

GitHub pré-preenche o body da PR com `.github/pull_request_template.md` ao abrir via UI. PRs abertas via `gh pr create --body ""` ficam com body vazio — o check captura esse caso também. O body chega ao workflow via `github.event.pull_request.body`.

## Goals / Non-Goals

**Goals:**
- Toda PR tem estrutura previsível e mínima
- Headings ausentes ou vazias bloqueiam merge
- Script testável sem CI

**Non-Goals:**
- Validar qualidade da escrita (gramática, tamanho)
- Conventional commits no título da PR
- Escape hatch por label (regra sempre bloqueante)

## Decisions

**Validação por heading + conteúdo**
Regra central: heading `##` deve existir E ter ao menos uma linha não-vazia e não-comentário abaixo. Seções com checkboxes exigem ao menos um `- [x]`. Simples de implementar em bash, intuitivo para o desenvolvedor.

**Body via env var, depois arquivo temporário (anti script-injection)**
O body da PR é controlado por quem abre a PR. Interpolar `${{ github.event.pull_request.body }}` (ou `toJSON(...)`) diretamente dentro de um `run:` permite script injection no runner — `toJSON` não escapa aspas simples, então um `'` no body fecha a string do shell e executa o resto. Padrão seguro do GitHub: passar o valor via bloco `env:` (`PR_BODY: ${{ github.event.pull_request.body }}`) e gravar em arquivo temporário com `printf '%s' "$PR_BODY"`. Variáveis de ambiente preservam aspas, backticks e multiline sem quebrar — não há necessidade de `toJSON`/python. O script lê o arquivo.

**6 seções obrigatórias**
Descrição (texto livre), Tipo (checkbox), Módulo (checkbox), Checklist (checkbox), Breaking Changes (texto — aceita "Nenhum"), Referências (texto — aceita "Nenhum"). Equilibra rigor com praticidade.

**Pular bots por `github.actor`**
`dependabot[bot]`, `github-actions[bot]`, `renovate[bot]` têm formato de body próprio. Verificar por actor name é determinístico — não depende de label.

**`tr -d '\r'` antes de processar**
Body de PR pode ter CRLF se o cliente for Windows. Remover `\r` antes de processar evita falsos negativos no parsing de linhas.

## Risks / Trade-offs

**PR editada depois de falha** → Check roda em `edited` — PR é desbloqueada automaticamente quando o body é corrigido.

**`- [x]` com espaço diferente** → Markdown aceita `- [x]`, `- [X]` — validar case-insensitive no grep (`-i`).

**Heading com capitalização diferente** → Usar capitalização exata do template. Documentar no template que as seções não devem ser renomeadas.

## Migration Plan

1. Criar `scripts/validate-pr-template.sh` e testar com body mock
2. Criar `.github/pull_request_template.md`
3. Criar `.github/workflows/validate-pr-template.yml`
4. Configurar required status check no GitHub
5. Abrir PR de teste com body vazio → confirmar bloqueio
6. Abrir PR com template corretamente preenchido → confirmar aprovação
