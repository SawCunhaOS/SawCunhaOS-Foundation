# CI/CD — Ciclo de Vida de Branches no Gitflow

**Data**: 2026-06-04  
**Status**: 🔄 Em Análise  
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `cicd-gitflow-branch-lifecycle`
- **Resumo em uma frase**: Adicionar merge-back automático para `develop` e limpeza de branches obsoletas ao processo de release, garantindo que o histórico do git permaneça íntegro e que branches antigas não se acumulem.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
O `release.sh` atual cria novas branches de versão mas nunca fecha o ciclo anterior:
- `release/1.1.0` permanece viva após 1.2.0 ser lançada — branch obsoleta acumula
- Fixes e ajustes feitos na `release/1.1.0` durante o processo de RC nunca chegam em `develop`
- Hotfixes em `fix/X.Y.Z` igualmente nunca são mergeados de volta — develop fica desatualizado
- Resultado: `develop` diverge silenciosamente da linha de releases

**Situação real observada:**
```
release/1.1.0  ← tag 1.1.0 existe, branch ainda ativa (deveria ter sido deletada)
release/1.2.0  ← atual em desenvolvimento
fix/1.0.1      ← hotfix remoto, sem merge de volta ao develop
fix/1.1.1      ← hotfix remoto, sem merge de volta ao develop
```

### Objetivo
Ao final de cada release, o script executa automaticamente:
1. **Merge-back**: branch lançada é incorporada ao `develop` (develop ganha conflitos de POM)
2. **Limpeza**: branch de release anterior é deletada (local + remote) após minor release
3. Develop fica sempre sincronizado com o que foi efetivamente publicado

### Fora de Escopo
- Automação de CHANGELOG e GitHub Release — ideia `cicd-automacao-release-changelog`
- Análise estática e cobertura — ideia `cicd-analise-estatica-qualidade`
- Branches `feature/*` — gerenciadas por PR, sem impacto
- Fix branches não têm prazo de expiração — permanecem para suporte a versões antigas

---

## 2️⃣ Requisitos

### Modelo de Branches

```
develop          ← branch paralela, features novas, NUNCA é source de release branches
                   (exceto no caso major, onde o release parte do develop)

[publicar 1.1.0]              [publicar 1.2.0]
      │                              │
release/1.1.0 ────────────► release/1.2.0 ────────────► release/1.3.0
      │    └─ cria ──► fix/1.1.1         └─ cria ──► fix/1.2.1
      │                    │                               │
      │               fix/1.1.2 (se publicar fix)      fix/1.2.2 (se publicar fix)
      │
   (tag 1.1.0)
```

**Regras de criação de branches:**
- `fix/X.Y.Z` é criado SOMENTE quando uma minor é publicada — par obrigatório da próxima release
- `release/X.(Y+1).0` também criado na publicação da minor
- Publicar um fix cria apenas o próximo fix (`fix/X.Y.(Z+1)`) — sem nova release minor
- Release minor e fix nunca partem do develop — sempre da branch de release anterior

Develop recebe merge-back para incorporar correções feitas na linha de release, mas não é source de nenhuma branch.

### Cadeia de Merge — Hierarquia Obrigatória

Merge sempre percorre a hierarquia em ordem, nunca pula níveis:

```
fix/X.Y.Z ──► release/X.(Y+1).0 ──► develop
              (próxima minor — nasceu junto com o fix)

release/X.Y.0 ──► develop
(minor vai direto, sem intermediário)

major: N/A (já em develop)
```

**Regra**: fix só existe depois que sua minor foi publicada. A minor publicada (release/X.Y.0) já está fechada — sem sentido mergear nela. O destino correto é `release/X.(Y+1).0`, a próxima minor ativa que nasceu junto com o fix.

Exemplo: fix/1.2.1 e release/1.3.0 são criados juntos ao publicar 1.2.0.
Publicar fix/1.2.1 → merge em release/1.3.0 → merge em develop.

### Regras por Tipo de Release

| Tipo | Cadeia de merge | Branch deletada | Nova branch criada de |
|------|----------------|-----------------|-----------------------|
| **major** | N/A | Nada | develop (único caso) |
| **minor** | `release/X.Y.0` → `develop` | `release/X.(Y-1).0` | `release/X.Y.0` atual |
| **fix** | `fix/X.Y.Z` → `release/X.(Y+1).0` → `develop` | Nada | `fix/X.Y.Z` atual |

Próxima minor de `fix/X.Y.Z` calculada como `release/X.(Y+1).0`.

### Funcionais
- [ ] **RF-01**: Release MINOR executa `git merge release/X.Y.0 → develop` com estratégia `-X ours --no-ff`
- [ ] **RF-02**: Release FIX executa merge em dois passos: `fix/X.Y.Z → release/X.(Y+1).0` e depois `release/X.(Y+1).0 → develop`, ambos com `-X ours --no-ff`
- [ ] **RF-03**: Próxima minor de um fix é calculada dinamicamente: `release/${CUR_MAJOR}.$((CUR_MINOR + 1)).0`
- [ ] **RF-05**: Release MINOR deleta `release/<CUR_MAJOR>.<CUR_MINOR-1>.0` local e remota, se existir
- [ ] **RF-06**: Deleção da branch anterior é silenciosa se a branch não existir (não quebra o script)
- [ ] **RF-07**: Após merge-back e deleção, retorna ao branch de release correto antes de criar novas branches
- [ ] **RF-08**: Release MAJOR não deleta nenhuma branch e não faz merge-back (release parte do develop)

### Não-Funcionais
- [ ] **RNF-01**: `set -e` deve continuar ativo — qualquer falha de merge ou deleção aborta o processo
- [ ] **RNF-02**: Conflito de POM resolvido automaticamente via `-X ours` — sem interação manual em nenhum passo da cadeia
- [ ] **RNF-03**: Mensagens de merge padronizadas: `chore: merge fix/X.Y.Z into release/X.Y.0` e `chore: merge release/X.Y.0 into develop`

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
scripts/
└── release.sh          (MODIFICADO — inserção de merge-back e limpeza por tipo)
```

Sem alteração em workflows do GitHub Actions — toda a lógica fica no script shell, que é chamado pelo `manual-release.yml`.

### Fluxo Completo — Release MINOR (ex: liberar 1.2.0)

```
[em release/1.2.0 com versão 1.2.0-SNAPSHOT]
  (release/1.2.0 foi criada de release/1.1.0 — não de develop)
       │
       ▼
close_version.sh          → pom = 1.2.0, commit
git tag 1.2.0 + push
deploy.sh                 → mvn deploy (publica 1.2.0)
       │
       ▼ NOVO — merge de correções para develop (develop corre em paralelo)
git checkout develop
git merge release/1.2.0 -X ours --no-ff -m "chore: merge release 1.2.0 into develop"
  → develop absorve correções feitas na linha de release
  → conflitos de pom.xml: develop vence (mantém sua própria versão SNAPSHOT)
git push origin develop
       │
       ▼ NOVO — limpeza da era anterior
PREV_BRANCH = release/1.(Y-1).0    (ex: release/1.1.0)
git branch -D $PREV_BRANCH          (local, se existir)
git push origin --delete $PREV_BRANCH  (remoto, se existir)
       │
       ▼ continua da release/1.2.0 (não do develop)
git checkout release/1.2.0
starts_new_version.sh minor   → pom = 1.3.0-SNAPSHOT, commit
create_branch_with_snapshot release/1.3.0  ← criada de release/1.2.0
create_branch_with_snapshot fix/1.2.1      ← criada de release/1.2.0
```

### Fluxo Completo — Release FIX (ex: liberar 1.1.2 de fix/1.1.2)

```
[em fix/1.1.2 com versão 1.1.2-SNAPSHOT]
  (fix/1.1.x nasceu junto com release/1.2.0 ao publicar release/1.1.0)
  (release/1.1.0 já está fechada/publicada — não é destino do merge)
       │
       ▼
close_version.sh          → pom = 1.1.2, commit
git tag 1.1.2 + push
deploy.sh                 → mvn deploy (publica 1.1.2)
       │
       ▼ NOVO — passo 1: fix → próxima minor
NEXT_MINOR = release/1.2.0  (CUR_MAJOR.(CUR_MINOR+1).0)
git checkout release/1.2.0
git merge fix/1.1.2 -X ours --no-ff -m "chore: merge fix/1.1.2 into release/1.2.0"
  → próxima release incorpora o hotfix antes de ser publicada
  → pom.xml da release/1.2.0 prevalece
git push origin release/1.2.0
       │
       ▼ NOVO — passo 2: próxima minor → develop
git checkout develop
git merge release/1.2.0 -X ours --no-ff -m "chore: merge release/1.2.0 into develop"
  → develop absorve via cadeia: fix → release → develop
  → pom.xml do develop prevalece
git push origin develop
       │
       ▼ (sem deleção)
git checkout fix/1.1.2
starts_new_version.sh fix   → pom = 1.1.3-SNAPSHOT, commit
create_branch_with_snapshot fix/1.1.3   ← criada de fix/1.1.2 (não do develop)
```

### Fluxo Completo — Release MAJOR (sem alteração)

```
[em develop]
close_version → tag → deploy → starts_new_version (develop → next major)
create_branch_with_snapshot release/<next minor>
create_branch_with_snapshot fix/<current +1>
(merge-back: N/A — já está em develop)
(deleção: não ocorre)
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Estratégia de merge | `-X ours --no-ff` | `-X theirs` ou rebase | develop deve ser a source of truth de versão; `--no-ff` preserva o grafo de merges |
| Timing do merge | após deploy, antes de starts_new_version | após starts_new_version | merge em estado limpo (1.2.0 sem SNAPSHOT); evita conflito de duas versões SNAPSHOT diferentes |
| Local da lógica | release.sh (shell) | workflow YAML | script é reutilizável localmente; workflow só chama o script |
| Deleção de branch | branch anterior calculada dinamicamente | lista hard-coded | cálculo `CUR_MINOR - 1` é genérico para qualquer versão futura |
| Major não deleta | nada deletado | deletar release/<X-1>.Y.0 | major representa mudança de era — histórico de release series anterior pode ser relevante |
| Fix branches | sem expiração | expirar após N versões | suporte a versões antigas não tem prazo definido — decisão futura |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos

**Modificados**:
- `scripts/release.sh` — inserção de função `merge_back_to_develop` e função `delete_previous_release_branch`; chamadas nos cases `minor` e `fix`

### Funções a adicionar em `release.sh`

```
merge_back_to_develop(branch):
  git checkout develop
  git merge $branch -X ours --no-ff -m "chore: merge $branch into develop"
  git push origin develop
  git checkout $branch   ← retorna ao branch original

delete_previous_release_branch(cur_major, cur_minor):
  prev = "release/${cur_major}.$((cur_minor - 1)).0"
  if local exists: git branch -D $prev
  if remote exists: git push origin --delete $prev
```

### Tarefas
- [ ] **T-01**: Adicionar função `merge_into(target, source)` em `release.sh` com estratégia `-X ours --no-ff` e mensagem padronizada
- [ ] **T-02**: Adicionar função `delete_previous_release_branch` em `release.sh` com verificação de existência local e remota
- [ ] **T-03**: No case `minor`: chamar `merge_into develop release/X.Y.0` (após deploy, antes de `starts_new_version.sh`)
- [ ] **T-04**: No case `fix`: calcular `NEXT_MINOR=release/${CUR_MAJOR}.$((CUR_MINOR + 1)).0`; chamar `merge_into $NEXT_MINOR fix/X.Y.Z` e depois `merge_into develop $NEXT_MINOR`
- [ ] **T-05**: Inserir chamada de `delete_previous_release_branch` no case `minor` (após merge-back)
- [ ] **T-06**: **[BUG FIX]** No case `minor`, salvar o branch original antes de criar `release/<next>` e usá-lo para criar `fix/<current+1>` — atualmente `git checkout develop` faz o fix nascer com código não lançado de develop. Substituir por `git checkout $ORIGINAL_BRANCH`
- [ ] **T-07**: Testar localmente com simulação de merge conflitante em pom.xml para validar `-X ours`
- [ ] **T-08**: Testar deleção silenciosa quando `release/<X>.<Y-1>.0` não existe
- [ ] **T-09**: Validar que fix branch criada após minor contém código de `release/X.Y.0` e não de develop

### Riscos e Edge Cases
1. **Primeiro minor da major**: `CUR_MINOR = 0` → prev seria `release/X.-1.0` — validar `CUR_MINOR >= 1` antes de calcular prev branch; se `CUR_MINOR == 0`, não deletar nada
2. **Branch develop com proteção no GitHub**: se develop for branch protegida, push direto pode falhar — verificar se `PAT_TOKEN` tem permissão de push em protected branches
3. **Conflitos além do pom.xml**: `-X ours` resolve TODO conflito pelo develop — se houver código conflitante (não só versão), develop ganha silenciosamente. Documentar isso como comportamento esperado
4. **merge_back falha**: `set -e` vai abortar o script — a tag e o deploy já ocorreram, mas as branches ainda não foram criadas. Estado parcialmente aplicado. Considerar logar o estado atual antes das operações novas
5. **Push para develop em CI**: `manual-release.yml` executa o script com `github-actions[bot]` — garantir que o git config de user.name/email está configurado antes do merge commit

---

## 📎 Referências
- `scripts/release.sh` — implementação atual do processo de release
- `scripts/close_version.sh`, `scripts/starts_new_version.sh`
- `.github/workflows/manual-release.yml` — orquestrador do release.sh
- Branches atuais observadas: `release/1.1.0` (obsoleta), `release/1.2.0`, `fix/1.0.1`, `fix/1.1.1`
