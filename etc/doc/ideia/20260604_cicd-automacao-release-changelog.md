# CI/CD — Automação de Release Notes e CHANGELOG

**Data**: 2026-06-04  
**Status**: 🔄 Em Análise  
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `cicd-automacao-release-changelog`
- **Resumo em uma frase**: Automatizar a geração do `CHANGELOG.md` e criação de GitHub Releases com notas ricas agrupadas por módulo, usando git-cliff e conventional commits com escopo.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
- `CHANGELOG.md` é mantido manualmente — trabalhoso e esquecido
- GitHub não tem um Release criado automaticamente quando `release.sh` cria uma tag
- Commits atuais usam `feat:` e `fix:` sem scope — impossível agrupar por módulo automaticamente
- Não existe documento de visualização de mudanças (release notes estruturadas)

### Objetivo
Após um release:
1. `CHANGELOG.md` é atualizado automaticamente com entradas agrupadas por módulo
2. GitHub Release é criado com release notes ricas em markdown, diretamente na UI do GitHub
3. Convenção de commits com scope documentada e adotada a partir desta mudança

**Exemplo de release notes gerado:**
```markdown
## [1.2.0] — 2026-06-10

### 🆕 audit
- feat: batch pipeline com retry e DLQ (#12)
- feat: hash-chain tamper-evidence para imutabilidade (#14)

### 🆕 privacy
- feat: MaskingEngine AES-256/GCM com suporte a builtins regionais (#8)

### 🐛 Correções
- fix(exception): RFC 9457 status code em respostas 422 (#10)

### ⚠️ Breaking Changes
- feat(audit)!: Migração Liquibase obrigatória em SFA_LOG_AUDIT
```

### Fora de Escopo
- Gate duro de cobertura — ideia separada
- Integridade de pipeline (snapshot validation) — ideia separada
- Mudança no fluxo de versioning (major/minor/fix) — sem alteração
- CHANGELOG retroativo (commits anteriores sem scope) — não reprocessar histórico

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `cliff.toml` configurado com template Tera que agrupa por scope (módulo)
- [ ] **RF-02**: Workflow `release-notes.yml` dispara em push de tag `v*` ou tag sem prefixo semântico (ex: `1.2.0`)
- [ ] **RF-03**: Workflow executa `git-cliff` e atualiza `CHANGELOG.md` via commit automático
- [ ] **RF-04**: Workflow cria GitHub Release com body gerado pelo git-cliff
- [ ] **RF-05**: `manual-release.yml` atualizado para incluir step de geração de CHANGELOG antes do push da tag
- [ ] **RF-06**: Convenção de commit documentada em `CONTRIBUTING.md` ou `etc/doc/commit-convention.md`

### Escopo de commits por módulo
| Scope | Módulo |
|-------|--------|
| `audit` | scos-foundation-audit |
| `privacy` | scos-foundation-privacy |
| `exception` | scos-foundation-exception |
| `security` | scos-foundation-security |
| `utils` | scos-foundation-utils |
| `jdempotent` | scos-foundation-jdempotent |
| `ci` | GitHub Actions / workflows |
| `build` | pom.xml, Maven config |
| `docs` | documentação |

### Não-Funcionais
- [ ] **RNF-01**: git-cliff não deve depender de Node.js ou Ruby — usar GitHub Action oficial (`orhun/git-cliff-action`)
- [ ] **RNF-02**: Commits sem scope devem aparecer em seção `Outros` (não ignorados)
- [ ] **RNF-03**: Breaking changes (commit com `!` ou footer `BREAKING CHANGE:`) destacados em seção separada

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
/ (raiz)
├── cliff.toml                              (NOVO — configuração git-cliff)
└── CONTRIBUTING.md ou etc/doc/             (NOVO — convenção de commits)

.github/workflows/
├── release-notes.yml                       (NOVO — dispara em tag push)
└── manual-release.yml                      (MODIFICADO — step de CHANGELOG antes da tag)
```

### Fluxo Principal
```
[release.sh cria tag e faz push]
  → tag push detectado pelo GitHub
  → release-notes.yml dispara
      ├── git-cliff --latest → gera release notes do último tag
      ├── git-cliff --output CHANGELOG.md → atualiza CHANGELOG completo
      ├── git commit "chore: update CHANGELOG for vX.Y.Z" + push
      └── gh release create vX.Y.Z --notes-file <(git-cliff --latest)
```

### Template git-cliff — Estrutura de Saída

```
cliff.toml define:
  - commit_parsers: mapeia feat/fix/chore/breaking para seções
  - body template (Tera): agrupa por scope → nome do módulo
  - tag_pattern: detecta tags semânticas (ex: 1.2.0 sem prefixo v)
  - ignore_tags: RC/alpha/beta se aplicável
```

**Template de seções (Tera):**
```
{% for group, commits in commits | group_by(attribute="group") %}
### {{ group }}
{% for commit in commits %}
- {{ commit.message | upper_first }} ({{ commit.id | truncate(length=7, end="") }})
{% endfor %}
{% endfor %}
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Ferramenta de CHANGELOG | git-cliff | conventional-changelog (Node.js) | Sem runtime externo; GitHub Action oficial; Tera template flexível |
| Trigger do workflow | push de tag | workflow_run após release | Tag push é o evento natural de release — independe do manual-release.yml |
| Commit do CHANGELOG | via PAT_TOKEN | GITHUB_TOKEN | GITHUB_TOKEN não dispara outros workflows; PAT_TOKEN necessário para commit + push |
| Scopes sem prefixo `v` | suportado | exigir `v` | Tags atuais são `1.2.0` sem prefixo — compatibilidade com histórico |
| Histórico retroativo | não processar | reprocessar commits sem scope | Commits antigos sem scope gerariam CHANGELOG ruidoso — iniciar do próximo release |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `cliff.toml` — configuração git-cliff: tag pattern, commit parsers por tipo/scope, template Tera
- `.github/workflows/release-notes.yml` — workflow disparado em tag push; executa git-cliff, commita CHANGELOG, cria GitHub Release
- `etc/doc/commit-convention.md` — documenta convenção de commits com exemplos por módulo

**Modificados**:
- `.github/workflows/manual-release.yml` — sem mudança estrutural necessária (release-notes.yml cuida do pós-tag)

### Tarefas
- [ ] **T-01**: Criar `cliff.toml` com tag_pattern `^\d+\.\d+\.\d+`, parsers para feat/fix/chore/breaking, template Tera agrupando por scope
- [ ] **T-02**: Criar `.github/workflows/release-notes.yml` com trigger `on: push: tags: ['*.*.*']`
- [ ] **T-03**: Configurar step git-cliff no workflow: `orhun/git-cliff-action@v4` com `--latest` para release notes e `--output CHANGELOG.md` para arquivo
- [ ] **T-04**: Adicionar step de commit automático do CHANGELOG.md (usando PAT_TOKEN)
- [ ] **T-05**: Adicionar step `gh release create` com notas geradas pelo git-cliff
- [ ] **T-06**: Criar `etc/doc/commit-convention.md` com tabela de scopes + exemplos
- [ ] **T-07**: Testar com tag de dry-run em branch de feature antes de merge em develop

### Riscos e Edge Cases
1. **Commits sem scope antes desta mudança**: git-cliff os agrupa em "Outros" — aceitável, não bloqueia
2. **Tag criada sem workflow_run em andamento**: release-notes.yml é independente — sem risco
3. **Commit automático do CHANGELOG cria loop de CI**: build.yml filtra branch `main`/`develop` mas o commit vai para o branch onde a tag aponta — verificar se build.yml roda desnecessariamente no commit de CHANGELOG
4. **`gh release create` falha se release já existe**: adicionar `--notes` com `--draft` ou verificar existência antes
5. **Múltiplos commits no mesmo push de tag**: git-cliff com `--latest` pega apenas desde o tag anterior — comportamento correto

---

## 📎 Referências
- [git-cliff documentation](https://git-cliff.org/docs/)
- [orhun/git-cliff-action GitHub Action](https://github.com/orhun/git-cliff-action)
- [Conventional Commits spec](https://www.conventionalcommits.org/)
- `scripts/release.sh` — onde a tag é criada e pushed
- `.github/workflows/manual-release.yml`
