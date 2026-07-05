# CI/CD — Template de PR e Validação de Preenchimento

**Data**: 2026-06-04  
**Status**: 🔄 Em Análise  
**Tipo**: 🆕 Nova Feature

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `cicd-pr-template-validacao`
- **Resumo em uma frase**: Criar template obrigatório de PR com seções definidas e bloquear via CI qualquer PR que não mantenha as headings do template com conteúdo preenchido abaixo de cada uma.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
PRs são abertas sem descrição, sem contexto de módulo afetado e sem checklist — revisor não sabe o que está olhando, histórico do git perde rastreabilidade.

### Objetivo
Toda PR deve conter as seções definidas no template com conteúdo real após cada heading. PRs que removam seções obrigatórias ou as deixem vazias são bloqueadas antes do merge.

**Regra central**: heading `##` deve existir **e** ter ao menos uma linha de conteúdo não-vazia abaixo dela.

**Passa:**
```markdown
## Descrição
Adiciona suporte a masking de CPF no módulo privacy.

## Tipo de Mudança
- [x] feat — nova feature

## Módulo(s) Afetado(s)
- [x] privacy
```

**Bloqueia:**
```markdown
## Descrição
<!-- descreva aqui -->     ← marcador não substituído

## Tipo de Mudança
                           ← vazio após heading

## Módulo(s) Afetado(s)
- [ ] privacy              ← nenhum checkbox marcado
```

### Fora de Escopo
- Validação de qualidade da escrita (gramática, tamanho mínimo)
- Conventional commits no título da PR — ideia separada se necessário
- PRs de bots (Dependabot, github-actions[bot]) — check pulado para atores automatizados

---

## 2️⃣ Requisitos

### Template — Seções Obrigatórias

```markdown
## Descrição
<!-- Descreva o que esta PR faz e por que. -->

## Tipo de Mudança
- [ ] feat — nova feature
- [ ] fix — correção de bug  
- [ ] chore — manutenção / refatoração
- [ ] docs — documentação
- [ ] breaking — breaking change

## Módulo(s) Afetado(s)
- [ ] audit
- [ ] privacy
- [ ] exception
- [ ] security
- [ ] utils
- [ ] jdempotent
- [ ] ci/build
- [ ] docs

## Checklist
- [ ] Testes adicionados ou atualizados
- [ ] Breaking changes documentados (se houver)

## Breaking Changes
<!-- Se não houver, escreva "Nenhum". -->

## Referências
<!-- Issues, tickets ou PRs relacionados. Se não houver, escreva "Nenhum". -->
```

### Regras de Validação por Seção

| Seção | Obrigatória | Regra |
|-------|-------------|-------|
| `## Descrição` | ✅ | Ao menos 1 linha não-vazia, sem marcadores `<!-- -->` |
| `## Tipo de Mudança` | ✅ | Ao menos 1 checkbox marcado `- [x]` |
| `## Módulo(s) Afetado(s)` | ✅ | Ao menos 1 checkbox marcado `- [x]` |
| `## Checklist` | ✅ | Ao menos 1 checkbox marcado `- [x]` |
| `## Breaking Changes` | ✅ | Ao menos 1 linha não-vazia (aceita "Nenhum") |
| `## Referências` | ✅ | Ao menos 1 linha não-vazia (aceita "Nenhum") |

**Regra geral**: se qualquer heading obrigatório estiver ausente do body → FAIL. Se heading existe mas não tem conteúdo válido abaixo → FAIL.

### Funcionais
- [ ] **RF-01**: `.github/pull_request_template.md` criado com as 6 seções definidas acima
- [ ] **RF-02**: Workflow `validate-pr-template.yml` disparado em `pull_request` (opened, edited, synchronize, reopened)
- [ ] **RF-03**: PRs de atores automatizados (`dependabot[bot]`, `github-actions[bot]`, `renovate[bot]`) são ignoradas — check retorna success automaticamente
- [ ] **RF-04**: Validação verifica presença de cada heading obrigatório no body
- [ ] **RF-05**: Validação verifica conteúdo não-vazio após cada heading (ignora linhas em branco e comentários `<!-- -->`)
- [ ] **RF-06**: Seções com checkboxes (`Tipo`, `Módulo`, `Checklist`) exigem ao menos um `- [x]`
- [ ] **RF-07**: Mensagem de falha lista especificamente quais seções falharam e por quê
- [ ] **RF-08**: Check é required status check — bloqueia merge se falhar

### Não-Funcionais
- [ ] **RNF-01**: Script de validação executável localmente (`scripts/validate-pr-template.sh`)
- [ ] **RNF-02**: Tempo de execução < 15 segundos
- [ ] **RNF-03**: Usa apenas `GITHUB_TOKEN` padrão (sem PAT) — leitura do body da PR via `github.event.pull_request.body`

---

## 3️⃣ Arquitetura

### Componentes Afetados

```
.github/
├── pull_request_template.md      (NOVO — template pré-preenchido ao abrir PR)
└── workflows/
    └── validate-pr-template.yml  (NOVO — check obrigatório de preenchimento)

scripts/
└── validate-pr-template.sh       (NOVO — lógica de validação, testável localmente)
```

### Fluxo do Check

```
PR aberta / editada
        │
        ▼
validate-pr-template.yml
        │
        ├─ actor ∈ {dependabot[bot], github-actions[bot], renovate[bot]}?
        │     └─ ✅ SKIP (retorna success)
        │
        ├─ body é null ou vazio?
        │     └─ ❌ FAIL: "PR sem descrição"
        │
        └─ validate-pr-template.sh $BODY
              │
              ├─ "## Descrição" presente?
              │   └─ tem linha não-vazia não-comentário abaixo? → ❌ FAIL se não
              │
              ├─ "## Tipo de Mudança" presente?
              │   └─ tem "- [x]" abaixo? → ❌ FAIL se não
              │
              ├─ "## Módulo(s) Afetado(s)" presente?
              │   └─ tem "- [x]" abaixo? → ❌ FAIL se não
              │
              ├─ "## Checklist" presente?
              │   └─ tem "- [x]" abaixo? → ❌ FAIL se não
              │
              ├─ "## Breaking Changes" presente?
              │   └─ tem linha não-vazia não-comentário abaixo? → ❌ FAIL se não
              │
              ├─ "## Referências" presente?
              │   └─ tem linha não-vazia não-comentário abaixo? → ❌ FAIL se não
              │
              └─ ✅ PASS
```

### Algoritmo de Validação de Seção

```
para cada heading obrigatório H:
  1. encontrar linha "## H" no body
  2. se não encontrar → FAIL "seção ausente: H"
  3. coletar linhas após H até o próximo "## " ou fim do body
  4. filtrar: remover linhas vazias e linhas que são só comentário <!-- ... -->
  5. se seção tem checkboxes: verificar se alguma linha começa com "- [x]"
     se não tem checkboxes: verificar se restam linhas com conteúdo
  6. se vazio após filtro → FAIL "seção sem conteúdo: H"
```

### Exemplo de Mensagem de Falha

```
❌ PR description validation failed

  Sections missing or empty:

  • "## Tipo de Mudança" — nenhum checkbox marcado. Marque ao menos um: - [x]
  • "## Breaking Changes" — seção vazia ou contém apenas comentários.
    Escreva o conteúdo ou "Nenhum".

  All required sections must be present with content:
  ## Descrição, ## Tipo de Mudança, ## Módulo(s) Afetado(s),
  ## Checklist, ## Breaking Changes, ## Referências
```

### Decisões Técnicas

| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Detecção de "vazio" | presença de linha não-vazia não-comentário | contagem mínima de caracteres | Flexível — "Nenhum" passa, marcador não-removido falha |
| Checkboxes obrigatórios | `- [x]` explícito | presença de texto | Sem `[x]` = checkbox não preenchido — semanticamente vazio |
| Template único vs múltiplos | único template | template por tipo de branch | Um template único reduz confusão; validação adapta por seção |
| Pular bots | sim, por actor name | por label | Actor name é determinístico; label requer configuração extra |
| Body lido via | `github.event.pull_request.body` no env | `gh pr view` | Mais simples, sem chamada de API adicional |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `.github/pull_request_template.md` — template com 6 seções, checkboxes e comentários guia
- `.github/workflows/validate-pr-template.yml` — workflow com trigger `pull_request`; escreve body em arquivo temporário e chama o script
- `scripts/validate-pr-template.sh` — lógica de parse e validação; recebe path do body como argumento

### Tarefas
- [ ] **T-01**: Criar `.github/pull_request_template.md` com as 6 seções, checkboxes e marcadores `<!-- -->` como guia
- [ ] **T-02**: Criar `scripts/validate-pr-template.sh` com funções: `section_exists`, `section_has_content`, `section_has_checked_box`; iterar seções obrigatórias e acumular erros
- [ ] **T-03**: Criar `.github/workflows/validate-pr-template.yml`: skip para bots, escrever `PR_BODY` em arquivo temp, chamar script, exibir erros se falhar
- [ ] **T-04**: Testar localmente: criar arquivos de body mock (preenchido, vazio, sem seção, marcador não removido) e rodar script
- [ ] **T-05**: Configurar required status check no GitHub para branches `release/*`, `fix/*`, `develop`

### Riscos e Edge Cases
1. **PR editada após falha**: check roda novamente em `synchronize` e `edited` — PR é desbloqueada quando corrigida ✅
2. **Heading com capitalização diferente**: `## descrição` vs `## Descrição` — validação case-sensitive ou case-insensitive? Usar case-sensitive para simplicidade; template define o padrão exato
3. **Body com CRLF no Windows**: `\r\n` pode quebrar parsing de linhas — usar `tr -d '\r'` no script antes de processar
4. **Quebra de linha no body passado via env**: body pode ter aspas, backticks — usar arquivo temporário em vez de variável de ambiente inline
5. **PR template não aparece automaticamente**: GitHub só pré-preenche o body se o PR for aberto via UI — PRs abertas via `gh pr create` sem `--body` ficam vazias; o check captura isso via "body vazio"

---

## 📎 Referências
- [GitHub Docs: PR templates](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/creating-a-pull-request-template-for-your-repository)
- `etc/doc/ideia/20260604_cicd-pr-validacao-branch.md` — check complementar de branch target
- `.github/workflows/build.yml` — modelo de workflow existente
