## Context

Tags são criadas por `release.sh` com formato semântico sem prefixo `v` (ex: `1.2.0`). O `CHANGELOG.md` existe mas é manual. git-cliff é um binário Rust com GitHub Action oficial (`orhun/git-cliff-action`) que processa commits conventional e gera markdown via templates Tera — sem Node.js, sem Ruby.

## Goals / Non-Goals

**Goals:**
- CHANGELOG.md atualizado automaticamente a cada release
- GitHub Release com notas agrupadas por módulo
- Convenção de commits com scope documentada

**Non-Goals:**
- Reprocessar histórico de commits anteriores sem scope
- Integrar git-cliff ao processo local de release (só no CI)
- Alterar `release.sh` para incluir geração de changelog

## Decisions

**git-cliff via GitHub Action (não via release.sh)**
O workflow `release-notes.yml` é disparado pela tag — independente do `manual-release.yml`. Isso desacopla a geração de notas do processo de release. Se git-cliff falhar, o release já ocorreu e pode ser reprocessado sem reabrir o release.

**Tag pattern `^\d+\.\d+\.\d+$`**
Tags atuais são `1.2.0` sem prefixo `v`. O pattern deve ser configurado sem `v` para compatibilidade com histórico existente.

**Scopes por módulo**
`feat(audit):`, `fix(privacy):` etc. git-cliff agrupa commits pelo scope — cada scope vira uma seção. Commits sem scope ficam em "Outros". Template Tera customizado para renderizar seções por módulo.

**Sem commit do CHANGELOG.md — só GitHub Release**
O workflow não commita `CHANGELOG.md` no repositório. A tag dispara em HEAD destacado e definir um branch destino para o commit é ambíguo no gitflow. git-cliff gera as notas (`--latest`) que são anexadas ao GitHub Release via `gh`. PAT_TOKEN é usado apenas para o `gh` CLI criar/editar o release. Sem commit automático, não há risco de loop de workflow.

**`gh release create` com `--notes-file`**
Evita interpolação de shell no body. git-cliff gera o arquivo de notas; `gh` lê e cria o release.

## Risks / Trade-offs

**`gh release create` falha se release já existe** → Adicionar `--notes-file` com verificação `gh release view <tag>` antes; usar `gh release edit` se já existe.

**Commits sem scope antes da mudança** → git-cliff os agrupa em "Outros" — não bloqueia, gera seção separada nas notas. Como `group_by(scope)` descarta commits sem scope, os parsers usam `default_scope = "Outros"`.

**CHANGELOG.md não versionado** → O histórico de releases fica apenas nas tags e nos GitHub Releases. Quem quiser um `CHANGELOG.md` no repo gera localmente com `git-cliff --output CHANGELOG.md` (não automatizado).

## Migration Plan

1. Criar `cliff.toml` e testar localmente: `git cliff --latest` (deve gerar notas do último tag)
2. Criar `etc/doc/commit-convention.md` com tabela de scopes
3. Criar `.github/workflows/release-notes.yml`
4. Testar em branch de feature: criar tag de teste (ex: `0.0.1-test`) e verificar workflow
5. Adotar convenção de commits com scope a partir do próximo commit após merge
