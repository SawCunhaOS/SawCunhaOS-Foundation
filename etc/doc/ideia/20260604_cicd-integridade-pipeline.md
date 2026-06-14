# CI/CD — Integridade de Pipeline (Snapshot com Validação)

**Data**: 2026-06-04  
**Status**: 🔄 Em Análise  
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `cicd-integridade-pipeline`
- **Resumo em uma frase**: Garantir que snapshots só sejam publicados após build+testes verdes, e adicionar validações de pré-condição nos scripts de deploy e release.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
`publish-snapshot.yml` executa `mvn deploy -DskipTests` de forma completamente independente do `build.yml`:
- Snapshot pode ser publicado mesmo que testes falhem
- Sem verificação se a versão no POM é realmente SNAPSHOT
- `deploy.sh` é apenas uma linha (`mvn clean deploy -DskipTests -Prelease`) — sem nenhuma guarda
- GPG setup está duplicado entre `publish-snapshot.yml` e `manual-release.yml`

### Objetivo
Snapshot só chega ao repositório se `build.yml` passou. Deploy e release têm validações de pré-condição explícitas. GPG setup é composite action reutilizável.

### Fora de Escopo
- Análise estática e cobertura — ideia separada
- Automação de CHANGELOG/GitHub Release — ideia separada
- Mudança no processo de release (major/minor/fix) — sem alteração de lógica de versão

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `publish-snapshot.yml` só executa se `build.yml` passou na mesma branch (via `workflow_run` ou `needs`)
- [ ] **RF-02**: `deploy.sh` verifica que versão corrente é `-SNAPSHOT` antes de publicar
- [ ] **RF-03**: `deploy.sh` verifica que não há arquivos não commitados (`git status --porcelain`)
- [ ] **RF-04**: `release.sh` verifica que não há arquivos não commitados antes de iniciar
- [ ] **RF-05**: GPG setup extraído para composite action em `.github/actions/setup-gpg/action.yml`
- [ ] **RF-06**: `publish-snapshot.yml` usa composite action de GPG (elimina duplicação)
- [ ] **RF-07**: `manual-release.yml` usa composite action de GPG (elimina duplicação)

### Não-Funcionais
- [ ] **RNF-01**: Composite action de GPG deve funcionar identicamente ao setup atual (compatibilidade com base64 e plain text)
- [ ] **RNF-02**: Validação de SNAPSHOT em `deploy.sh` deve falhar com mensagem clara antes de qualquer operação Maven

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
.github/
├── actions/
│   └── setup-gpg/
│       └── action.yml                  (NOVO — composite action)
└── workflows/
    ├── publish-snapshot.yml            (MODIFICADO — workflow_run trigger + validações)
    └── manual-release.yml              (MODIFICADO — usa composite action GPG)

scripts/
├── deploy.sh                           (MODIFICADO — validações de pré-condição)
└── release.sh                          (MODIFICADO — verificação de working tree limpo)
```

### Fluxo Principal
```
push develop/fix/**/release/** 
  → build.yml (testes rodam)
  → [build.yml SUCCESS] → publish-snapshot.yml dispara (workflow_run)
                              ├── verifica versão é SNAPSHOT
                              ├── setup GPG (composite action)
                              └── mvn deploy -Prelease (mantém -DskipTests — testes já rodaram)

push feature/** 
  → build.yml (testes rodam)
  → publish-snapshot.yml NÃO dispara (branch não elegível)
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Dependência entre workflows | `workflow_run` trigger | `needs` (mesmo workflow) | São workflows separados; `workflow_run` é o mecanismo correto para dependência cross-workflow |
| Manter `-DskipTests` no deploy | sim | rodar testes novamente | Testes já rodaram no build.yml; rodar de novo dobra tempo sem ganho |
| Validação de SNAPSHOT | no script shell | no workflow YAML | Script é reutilizável localmente; validação no shell garante consistência em ambos os contextos |
| Composite action vs reusable workflow | composite action | reusable workflow (.github/workflows) | GPG setup é um passo, não um job completo — composite action é mais leve e não usa runner extra |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `.github/actions/setup-gpg/action.yml` — composite action que encapsula import de GPG key + configuração de loopback pinentry

**Modificados**:
- `.github/workflows/publish-snapshot.yml` — trigger muda para `workflow_run` (on: build.yml success), adiciona step de validação de versão SNAPSHOT, usa composite action GPG
- `.github/workflows/manual-release.yml` — substitui bloco GPG inline pelo composite action
- `scripts/deploy.sh` — adiciona verificação de versão SNAPSHOT + working tree limpo
- `scripts/release.sh` — adiciona verificação de working tree limpo no início

### Tarefas
- [ ] **T-01**: Criar `.github/actions/setup-gpg/action.yml` extraindo o bloco de GPG atual
- [ ] **T-02**: Atualizar `publish-snapshot.yml`: trigger `workflow_run` + step de validação SNAPSHOT + usar composite action GPG
- [ ] **T-03**: Atualizar `manual-release.yml`: substituir bloco GPG pelo composite action
- [ ] **T-04**: Atualizar `deploy.sh`: adicionar guards de versão SNAPSHOT e working tree limpo
- [ ] **T-05**: Atualizar `release.sh`: adicionar verificação de working tree limpo no início do script
- [ ] **T-06**: Testar que `workflow_run` dispara corretamente em push para develop (simulação em branch de teste)

### Riscos e Edge Cases
1. `workflow_run` não dispara para forks — sem impacto (projeto é single-owner)
2. `workflow_run` usa o código do branch padrão para o workflow consumidor, não do branch que disparou o build — verificar comportamento com `workflow_run.head_branch` para garantir deploy do branch correto
3. Se `build.yml` for renomeado, o `workflow_run` quebra silenciosamente — documentar essa dependência no YAML
4. Validação de working tree no CI sempre passa (checkout limpo) — a validação é útil principalmente ao rodar scripts localmente

---

## 📎 Referências
- [GitHub Docs: workflow_run trigger](https://docs.github.com/en/actions/writing-workflows/choosing-when-your-workflow-runs/events-that-trigger-workflows#workflow_run)
- `.github/workflows/publish-snapshot.yml`
- `.github/workflows/manual-release.yml`
- `scripts/deploy.sh`
