# [TÍTULO DA FUNCIONALIDADE]

**Data**: YYYY-MM-DD  
**Status**: 🔄 Em Análise | ✅ Aprovado | ❌ Rejeitado  
**Tipo**: 🆕 Nova Feature | 🔧 Refatoração | 🐛 Bug Fix | ⚡ Performance | 🗄️ Banco de Dados

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `[nome único e específico]`
- **Resumo em uma frase**: [O que faz, para quem, qual benefício entrega]

**Checklist SRP**:
- [ ] Esta ideia cobre exatamente uma funcionalidade
- [ ] Não mistura features independentes no mesmo arquivo
- [ ] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
[Qual problema existe? Quem é afetado? Qual o impacto atual?]

### Objetivo
[O que será alcançado? Critério de sucesso mensurável.]

### Fora de Escopo
- [Funcionalidade A — pertence a outra ideia]
- [Funcionalidade B — fora do objetivo]

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: [requisito funcional 1]
- [ ] **RF-02**: [requisito funcional 2]

### Não-Funcionais
- [ ] **RNF-01**: [performance / segurança / disponibilidade / LGPD / etc]

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
[Módulo]
├── [Componente A]: [adição | modificação | remoção]
└── [Componente B]: [adição | modificação | remoção]
```

### Fluxo Principal
```
[Entrada] → [Passo 1] → [Passo 2] → [Saída]
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| [ex: cache] | [ex: Redis] | [ex: memória local] | [razão] |

### Banco de Dados
- **Impacto**: ✅ Sim | ❌ Não
- [Se sim: tabelas afetadas, índices, migrations necessárias — resumo]

---

## 4️⃣ Implementação

### Arquivos

**Novos**:
- `caminho/Arquivo.java` — [propósito]

**Modificados**:
- `caminho/Existente.java` — [tipo de mudança]

### Tarefas
- [ ] **T-01**: [tarefa 1]
- [ ] **T-02**: [tarefa 2]
- [ ] **T-03**: [tarefa 3]

### Riscos e Edge Cases
1. [Risco ou caso extremo 1]
2. [Risco ou caso extremo 2]

---

## 📎 Referências
- [ADR / Issue / PR / Documento relacionado]

---