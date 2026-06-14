## 1. Configuração do git-cliff

- [x] 1.1 Criar `cliff.toml` na raiz com `tag_pattern = "^\d+\.\d+\.\d+$"`, commit parsers para `feat`/`fix`/`chore`/`docs`/`breaking`, e template Tera agrupando commits por scope
- [x] 1.2 Configurar seção `[changelog]` com `header`, `body` (template Tera com `group_by scope`), e `footer`
- [x] 1.3 Configurar `commit_parsers` para mapear `feat` → "Features", `fix` → "Correções", commits com `!` → "Breaking Changes", e `scope = null` → "Outros" _(scope null tratado via `default_scope = "Outros"` — group_by descartava commits sem scope)_
- [x] 1.4 Testar localmente: `git cliff --latest` deve gerar notas da versão mais recente agrupadas por módulo _(validado com git-cliff 2.6.1: seções `### audit`/`### privacy`/`### Outros` + breaking `!`)_

## 2. Documento de Convenção de Commits

- [x] 2.1 Criar `etc/doc/commit-convention.md` com tabela de scopes: audit, privacy, exception, security, utils, jdempotent, ci, build, docs
- [x] 2.2 Incluir exemplos: `feat(audit): adicionar batch pipeline`, `fix(privacy): corrigir masking de CPF`, `feat(security)!: remover endpoint deprecated`
- [x] 2.3 Documentar como indicar breaking change: sufixo `!` no type(scope) ou footer `BREAKING CHANGE:`

## 3. Workflow release-notes.yml

- [x] 3.1 Criar `.github/workflows/release-notes.yml` com trigger `on: push: tags: ['*.*.*']`
- [x] 3.2 Adicionar step de checkout com `fetch-depth: 0` (necessário para git-cliff processar histórico completo)
- [x] 3.3 Adicionar step `orhun/git-cliff-action@v4` com `args: --latest --strip header` para gerar notes do último release em arquivo temporário
- [~] 3.4 ~~Adicionar step `orhun/git-cliff-action@v4` com `args: --output CHANGELOG.md`~~ _(DESCOPADO — decisão: só GitHub Release, sem commit do CHANGELOG.md)_
- [~] 3.5 ~~Adicionar step de commit e push do `CHANGELOG.md`~~ _(DESCOPADO — decisão: só GitHub Release, sem commit do CHANGELOG.md)_
- [x] 3.6 Adicionar step de criação/atualização do GitHub Release: verificar se release existe com `gh release view`; usar `gh release create` ou `gh release edit` conforme resultado
- [x] 3.7 Configurar `env.GH_TOKEN` com PAT_TOKEN para o step do `gh` CLI

## 4. Validação

- [x] 4.1 Criar tag de teste em branch de feature (ex: `99.0.0-test`) e verificar que workflow executa e gera CHANGELOG corretamente
- [x] 4.2 Verificar que GitHub Release é criado com notas agrupadas por módulo
- [x] 4.3 Deletar tag de teste e limpar GitHub Release criado pelo teste
- [x] 4.4 Adotar convenção de commits com scope a partir do primeiro commit após merge desta mudança
