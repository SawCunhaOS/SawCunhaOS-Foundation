# CI/CD — Validação de Branch Target na Abertura de PR

**Data**: 2026-06-04  
**Status**: 🔄 Em Análise  
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `cicd-pr-validacao-branch`
- **Resumo em uma frase**: Workflow de CI que bloqueia PRs cujo branch de destino (base) viola a hierarquia de merge do gitflow — fix só pode ir para o próximo fix ou próxima minor, release só pode ir para develop ou próxima minor.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
Não há nada impedindo um desenvolvedor de abrir uma PR de `fix/1.1.2` diretamente para `develop`, pulando a cadeia de merge obrigatória (`fix → próxima minor → develop`). O erro só seria detectado na publicação — tarde demais.

A mesma regra que governa o processo de publicação (`release.sh`) deve ser enforçada na abertura de PRs, antes de qualquer merge acontecer.

### Objetivo
Todo PR que viola a hierarquia de merge é bloqueado por um check obrigatório no GitHub, com mensagem clara do motivo e dos destinos permitidos.

**Exemplo de PR bloqueada:**
```
❌ PR: fix/1.1.2 → develop
   Motivo: fix branches não podem ir diretamente para develop.
   Destinos permitidos: fix/1.1.3 (se existir) ou release/1.2.0
```

**Exemplo de PR permitida:**
```
✅ PR: fix/1.1.2 → release/1.2.0
✅ PR: feature/minha-feature → develop
✅ PR: feature/minha-feature → release/1.2.0  (feature pode qualquer branch)
```

### Fora de Escopo
- Validação de título ou conteúdo da PR (conventional commits) — ideia separada
- Automação de CHANGELOG — ideia `cicd-automacao-release-changelog`
- Merge automático após validação — o check só valida, não age
- Sem escape hatch por label (a regra é sempre bloqueante)

---

## 2️⃣ Requisitos

### Matriz de Branches Permitidos

```
HEAD (origem)      BASE (destino) permitido
─────────────────────────────────────────────────────────────
feature/*          QUALQUER branch — sem restrição
fix/X.Y.Z          fix/X.Y.(Z+1)     se existir remotamente
                   release/X.(Y+1).0  sempre (deve existir)
                   ❌ develop direto
                   ❌ qualquer outra branch
release/X.Y.0      develop
                   release/X.(Y+1).0  caso pós-major (develop avançou de major)
                   ❌ fix/*
                   ❌ outra release diferente da próxima
develop            sem restrição (PRs de develop são raras/incomuns)
```

**Caso pós-major para release/X.Y.0:** quando o develop já avançou para uma major superior (ex: 2.x) mas ainda há releases da série 1.x sendo trabalhadas, o destino `release/X.(Y+1).0` é válido pois não faz sentido mergear 1.x direto no develop de 2.x. A regra permite ambos: `develop` OU `release/X.(Y+1).0`.

### Funcionais
- [ ] **RF-01**: Workflow `validate-pr-target.yml` disparado em `pull_request` (tipos: opened, edited, synchronize, reopened)
- [ ] **RF-02**: `feature/*` como HEAD passa sempre, sem verificação de BASE
- [ ] **RF-03**: `fix/X.Y.Z` como HEAD: BASE deve ser `fix/X.Y.(Z+1)` (se existir) ou `release/X.(Y+1).0`
- [ ] **RF-04**: `release/X.Y.0` como HEAD: BASE deve ser `develop` ou `release/X.(Y+1).0`
- [ ] **RF-05**: Verificação de existência do `fix/X.Y.(Z+1)` via GitHub API antes de incluí-lo como opção
- [ ] **RF-06**: Falha com mensagem clara: branch de origem, destino tentado, destinos permitidos
- [ ] **RF-07**: Check é obrigatório (required status check) — bloqueia merge se falhar
- [ ] **RF-08**: Branches não reconhecidos pelo padrão (ex: `hotfix/*`, `chore/*`) devem passar sem erro — check só age em padrões conhecidos

### Não-Funcionais
- [ ] **RNF-01**: Script de validação (`scripts/validate-pr-target.sh`) executável localmente para testes
- [ ] **RNF-02**: Tempo de execução do check < 30 segundos
- [ ] **RNF-03**: Usa `GITHUB_TOKEN` (sem PAT) — apenas leitura de branches remotos

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
.github/workflows/
└── validate-pr-target.yml   (NOVO — trigger: pull_request)

scripts/
└── validate-pr-target.sh    (NOVO — lógica de validação, testável localmente)
```

### Fluxo do Check

```
PR aberta/editada
      │
      ▼
validate-pr-target.yml
      │
      ├─ HEAD = feature/*?  → ✅ PASS (sem verificação)
      │
      ├─ HEAD = fix/X.Y.Z?
      │     ├─ calcular: next_fix = fix/X.Y.(Z+1)
      │     ├─ calcular: next_minor = release/X.(Y+1).0
      │     ├─ checar se next_fix existe (gh api)
      │     └─ BASE ∈ {next_fix [se existir], next_minor}?
      │           ├─ sim → ✅ PASS
      │           └─ não → ❌ FAIL + mensagem
      │
      ├─ HEAD = release/X.Y.0?
      │     ├─ calcular: next_minor = release/X.(Y+1).0
      │     └─ BASE ∈ {develop, next_minor}?
      │           ├─ sim → ✅ PASS
      │           └─ não → ❌ FAIL + mensagem
      │
      └─ HEAD = outros padrões → ✅ PASS (não gerenciado)
```

### Cálculo de Versões a partir do Nome do Branch

```bash
# fix/1.1.2 → CUR_MAJOR=1, CUR_MINOR=1, CUR_FIX=2
head_ref="fix/1.1.2"
version="${head_ref#fix/}"          # "1.1.2"
IFS='.' read -r MAJOR MINOR FIX <<< "$version"

next_fix="fix/${MAJOR}.${MINOR}.$((FIX + 1))"
next_minor="release/${MAJOR}.$((MINOR + 1)).0"

# release/1.2.0 → CUR_MAJOR=1, CUR_MINOR=2
head_ref="release/1.2.0"
version="${head_ref#release/}"      # "1.2.0"
IFS='.' read -r MAJOR MINOR _ <<< "$version"

next_minor="release/${MAJOR}.$((MINOR + 1)).0"
```

### Verificação de Branch Remoto via GitHub API

```bash
# Verifica se next_fix existe remotamente
branch_exists() {
  local branch="$1"
  gh api "repos/$GITHUB_REPOSITORY/branches/$branch" \
    --silent 2>/dev/null && echo "true" || echo "false"
}
```

### Mensagem de Erro (exemplo)

```
❌ Merge target inválido para branch fix/1.1.2

  Branch de origem : fix/1.1.2
  Branch de destino: develop  ← não permitido

  Destinos permitidos:
    • release/1.2.0  (próxima minor — sempre disponível)

  A branch fix/1.1.3 não existe — se existisse, também seria permitida.

  Motivo: fix branches devem seguir a cadeia de merge:
    fix → próxima minor → develop
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Onde roda a lógica | script shell externo | YAML inline | Testável localmente sem CI |
| API para checar branch | `gh api` (GitHub CLI) | `git ls-remote` | `gh api` é mais rápido e retorna status code limpo |
| Branches não reconhecidos | pass-through | fail | Evita quebrar fluxos inesperados (dependabot, renovate, etc.) |
| Escape hatch por label | sem escape | com label | Regra deve ser sempre respeitada — exceção cria confusão |
| `feature/*` sem restrição | sem verificação | verificar release destino | Feature pode ser direcionada a qualquer branch — flexibilidade intencional |
| Required status check | bloqueante | advisory | Validação que não bloqueia é ignorada |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `.github/workflows/validate-pr-target.yml` — workflow disparado em pull_request; chama o script
- `scripts/validate-pr-target.sh` — lógica de validação com extração de versão, verificação via API, mensagem de erro

**Modificados**:
- Branch protection rules (configuração manual no GitHub): adicionar `validate-pr-target` como required status check nas branches `release/*`, `fix/*`, `develop`

### Tarefas
- [ ] **T-01**: Criar `scripts/validate-pr-target.sh` com funções: extração de tipo de branch, cálculo de próximo fix/minor, verificação de existência via `gh api`, validação de BASE contra regras
- [ ] **T-02**: Criar `.github/workflows/validate-pr-target.yml` com trigger `pull_request` (opened/edited/synchronize/reopened), passando `github.head_ref`, `github.base_ref`, `github.repository` para o script
- [ ] **T-03**: Cobrir todos os casos no script: feature (pass-through), fix (verifica next_fix + next_minor), release (verifica develop + next_minor), outros (pass-through)
- [ ] **T-04**: Testar localmente: simular HEAD/BASE como variáveis de ambiente e rodar script
- [ ] **T-05**: Configurar branch protection no GitHub para adicionar `validate-pr-target` como required status check
- [ ] **T-06**: Validar comportamento com PR de Dependabot (branch `dependabot/*`) — deve passar sem erro

### Riscos e Edge Cases
1. **PR de Dependabot/Renovate**: branches como `dependabot/maven/...` não casam com nenhum padrão → pass-through correto pelo RF-08
2. **`release/X.Y.(Z+1).0` com Z > 0**: o cálculo assume patch=0 para release branches — se existir `release/1.2.1` (incomum), a regex vai extrair o terceiro segmento como `1` e o "next" seria `release/1.2.2` em vez de `release/1.3.0`; solução: validar que release branches sempre terminam em `.0`
3. **`gh api` indisponível ou rate limit**: adicionar `|| true` no check de next_fix existence e assumir que o branch não existe — não bloqueia PR por falha de infra
4. **Branch protection não configurado**: o check roda mas não bloqueia merge se não for required — documentar configuração obrigatória
5. **PR entre forks**: `github.repository` aponta para o repo base; `gh api` funciona normalmente

---

## 📎 Referências
- `etc/doc/ideia/20260604_cicd-gitflow-branch-lifecycle.md` — regras de merge que esta validação espelha
- [GitHub Docs: pull_request event](https://docs.github.com/en/actions/writing-workflows/choosing-when-your-workflow-runs/events-that-trigger-workflows#pull_request)
- [GitHub Docs: required status checks](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches#require-status-checks-before-merging)
