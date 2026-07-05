# CI/CD — Análise Estática e Relatório de Qualidade

**Data**: 2026-06-04  
**Status**: 🔄 Em Análise  
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `cicd-analise-estatica-qualidade`
- **Resumo em uma frase**: Integrar SpotBugs ao perfil `analyze` do Maven e publicar relatórios de cobertura (JaCoCo ≥80%) e bugs potenciais como artefatos do GitHub Actions — sem gate duro, apenas visibilidade.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
O perfil `analyze` do `pom.xml` já tem JaCoCo e OWASP dependency-check, mas:
- SpotBugs não está configurado — bugs potenciais (null pointer, race conditions) passam invisíveis
- JaCoCo gera relatório mas nenhum workflow o publica como artefato
- `checkstyle` e `dependency-check` têm `continue-on-error: true` em `build.yml` — nunca bloqueiam, feedback ignorado
- `privacy` module não está no passo de `Archive artifacts` do `build.yml`

### Objetivo
Após esta mudança, cada execução de CI publica:
- Relatório HTML de cobertura JaCoCo (threshold 80% visível, sem fail automático)
- Relatório SpotBugs por módulo
- Relatório Checkstyle sem `continue-on-error`
- Todos os módulos arquivados corretamente (incluindo `privacy`)

### Fora de Escopo
- Gate duro de cobertura (fail build se < 80%) — pertence a decisão futura
- SonarCloud/SonarQube — sem servidor externo
- PMD — SpotBugs já cobre bugs potenciais, PMD seria adicional
- Integridade de pipeline (snapshot dependency) — ideia separada
- Automação de CHANGELOG — ideia separada

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: SpotBugs configurado no perfil `analyze` do `pom.xml` com nível `HIGH`/`MEDIUM`
- [ ] **RF-02**: JaCoCo relatório publicado como artefato no job `security-check` do `build.yml`
- [ ] **RF-03**: Relatório SpotBugs publicado como artefato por módulo
- [ ] **RF-04**: `privacy` module adicionado ao passo de `Archive artifacts` no `build.yml`
- [ ] **RF-05**: `continue-on-error: true` removido de `checkstyle:check` (deve bloquear se violar regra)
- [ ] **RF-06**: `dependency-check` mantém `continue-on-error: true` (NVD pode estar indisponível — flaky externo)

### Não-Funcionais
- [ ] **RNF-01**: Job `security-check` não deve ultrapassar 20 minutos (SpotBugs pode ser lento em multi-módulo)
- [ ] **RNF-02**: Artefatos retidos por 7 dias (hoje são 3-5)
- [ ] **RNF-03**: SpotBugs usa excludes para não reportar falsos positivos de geração automática (Lombok, MapStruct)

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
pom.xml (raiz)
└── profile analyze: adição do SpotBugs plugin + configuração de excludes

.github/workflows/build.yml
├── Archive artifacts: adição do módulo privacy
├── security-check job: remoção de continue-on-error no checkstyle
├── security-check job: publicação JaCoCo HTML report como artefato
└── security-check job: publicação SpotBugs report como artefato

etc/spotbugs/
└── exclude.xml: regras de exclusão (Lombok, geração automática)
```

### Fluxo Principal
```
push/PR → build job → security-check job
                           ├── mvn -Panalyze checkstyle:check   (bloqueia se falhar)
                           ├── mvn -Panalyze spotbugs:check     (bloqueia se bug HIGH/MEDIUM)
                           ├── mvn -Panalyze jacoco:report       (gera HTML)
                           ├── mvn -Panalyze dependency-check:check (continue-on-error)
                           └── upload-artifact: jacoco-report, spotbugs-report
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Ferramenta de bug detection | SpotBugs | PMD | SpotBugs detecta bugs reais; PMD foca em estilo — Checkstyle já cobre estilo |
| Nível de severidade SpotBugs | MEDIUM+ | HIGH only | HIGH perde muitos problemas; LOW gera ruído excessivo |
| dependency-check continue-on-error | manter true | false | NVD API tem instabilidade conhecida — não penalizar build por falha de infra externa |
| Checkstyle continue-on-error | remover | manter | Checkstyle é local, determinístico — falhar CI é o comportamento correto |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `etc/spotbugs/exclude.xml` — regras de exclusão para Lombok, geração automática, e falsos positivos conhecidos

**Modificados**:
- `pom.xml` — adicionar `spotbugs-maven-plugin` no profile `analyze`
- `.github/workflows/build.yml` — Archive privacy module + publicar relatórios JaCoCo/SpotBugs + remover continue-on-error do checkstyle

### Tarefas
- [ ] **T-01**: Adicionar `spotbugs-maven-plugin` (v4.x) ao perfil `analyze` no `pom.xml` com goal `check` e `report`
- [ ] **T-02**: Criar `etc/spotbugs/exclude.xml` com exclusões para Lombok e classes geradas
- [ ] **T-03**: Adicionar `privacy` ao passo de archive no `build.yml`
- [ ] **T-04**: Remover `continue-on-error: true` do step `checkstyle:check`
- [ ] **T-05**: Adicionar steps de upload-artifact para JaCoCo HTML e SpotBugs report no `security-check` job
- [ ] **T-06**: Validar que tempo total de `security-check` fica abaixo de 20min

### Riscos e Edge Cases
1. SpotBugs pode ser muito lento em multi-módulo — mitigado com `spotbugs:check` só no profile `analyze` (não no build normal)
2. Falsos positivos em código gerado por Lombok/MapStruct — mitigado pelo `exclude.xml`
3. Checkstyle pode ter regras muito restritivas hoje e passar a bloquear CI em PRs existentes — verificar regras atuais antes de remover `continue-on-error`

---

## 📎 Referências
- [SpotBugs Maven Plugin](https://spotbugs.github.io/spotbugs-maven-plugin/)
- `pom.xml` perfil `analyze` (linhas 240-310)
- `.github/workflows/build.yml`
