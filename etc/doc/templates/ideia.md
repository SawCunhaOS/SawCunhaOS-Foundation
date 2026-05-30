# [TÍTULO DA MUDANÇA/FEATURE]

**Data de Criação**: YYYY-MM-DD HH:MM:SS  
**Status**: 🔄 Em Análise | ✅ Aprovado | ⚠️ Aprovado com Ressalvas | ❌ Rejeitado  
**ID da Decisão**: ADR-[número]  
**Tipo**: 🆕 Nova Feature | 🔧 Refatoração | 🐛 Bug Fix | ⚡ Performance | 🗄️ Banco de Dados

---

## 1️⃣ Solicitação do Usuário

### Requisição Original
```
[Transcrever exatamente a solicitação do usuário]
```

### Contexto Adicional
[Se o usuário forneceu contexto adicional, descrever aqui]

### Arquivos Mencionados
- `caminho/arquivo1.ext` - [descrição]
- `caminho/arquivo2.ext` - [descrição]

### Prioridade
- [ ] 🔴 Crítica (produção quebrada)
- [ ] 🟠 Alta (feature bloqueante)
- [ ] 🟡 Média (melhoria importante)
- [ ] 🟢 Baixa (nice to have)

---

## 2️⃣ Objetivo do Desenvolvimento

> **Definido por**: 🔧 @dev-senior

### Problema Identificado
[Descrição clara e objetiva do problema que está sendo resolvido]

**Sintomas**:
- [Sintoma 1]
- [Sintoma 2]

**Causa Raiz**:
[Explicação da causa real do problema]

### Objetivo Principal
[O que se pretende alcançar com esta mudança]

### Objetivos Secundários
- [Objetivo secundário 1]
- [Objetivo secundário 2]

### Critérios de Sucesso
- [ ] [Critério 1 - mensurável]
- [ ] [Critério 2 - mensurável]
- [ ] [Critério 3 - mensurável]

### Não-Objetivos (Out of Scope)
[O que explicitamente NÃO será feito nesta mudança]
- [Não-objetivo 1]
- [Não-objetivo 2]

---

## 3️⃣ Solução Proposta

> **Proposta inicial**: 🔧 @dev-senior  
> **Refinamentos**: 🏗️ @arquiteto | 🎯 @especialista | 🗄️ @dba

### 3.1 Abordagem Escolhida

**Nome da Abordagem**: [Ex: Padrão Strategy, Refatoração com Repository, etc]

**Descrição**:
[Explicação detalhada da solução técnica]

**Justificativa da Escolha**:
[Por que esta abordagem foi escolhida em vez de outras]

### 3.2 Alternativas Consideradas

#### Alternativa 1: [Nome]
- **Descrição**: [resumo]
- **Vantagens**: [lista]
- **Desvantagens**: [lista]
- **Por que não foi escolhida**: [razão]

#### Alternativa 2: [Nome]
- **Descrição**: [resumo]
- **Vantagens**: [lista]
- **Desvantagens**: [lista]
- **Por que não foi escolhida**: [razão]

### 3.3 Arquitetura da Solução

**Componentes Afetados**:
```
[Módulo 1]
├── Mudança: [descrição]
└── Justificativa: [razão]

[Módulo 2]
├── Mudança: [descrição]
└── Justificativa: [razão]
```

**Fluxo da Solução**:
```
[Descrever o fluxo de execução]
Cliente → Controller → Service → Repository → Database
```

**Diagrama** (se aplicável):
```
[Diagrama em ASCII art ou referência a arquivo de imagem]
```

### 3.4 Design Patterns Utilizados

| Pattern | Onde | Justificativa |
|---------|------|---------------|
| [Nome] | [Classe/Módulo] | [Por que este pattern] |

### 3.5 Conformidade Arquitetural

**Validado por**: 🏗️ @arquiteto

#### SOLID
| Princípio | Conformidade | Observação |
|-----------|--------------|------------|
| Single Responsibility | ✅/⚠️/❌ | [nota] |
| Open/Closed | ✅/⚠️/❌ | [nota] |
| Liskov Substitution | ✅/⚠️/❌ | [nota] |
| Interface Segregation | ✅/⚠️/❌ | [nota] |
| Dependency Inversion | ✅/⚠️/❌ | [nota] |

#### Separação de Camadas
- **Respeitada**: ✅ Sim | ⚠️ Com ressalvas | ❌ Não
- **Observações**: [comentários do arquiteto]

#### Acoplamento & Coesão
- **Acoplamento**: 🟢 Baixo | 🟡 Médio | 🔴 Alto
- **Coesão**: 🟢 Alta | 🟡 Média | 🔴 Baixa
- **Análise**: [comentários]

### 3.6 Validação Técnica

**Validado por**: 🎯 @especialista

#### Segurança
- **Vulnerabilidades verificadas**: ✅ Sim | ❌ Não
- **CVEs encontrados**: [lista ou "Nenhum"]
- **Input validation**: ✅ Implementada | ⚠️ Parcial | ❌ Ausente
- **Output sanitization**: ✅ Implementada | ❌ Ausente

#### Performance
- **Complexidade algorítmica**: O([valor])
- **Uso de memória**: [estimativa]
- **Impacto em performance**: 🟢 Positivo | ➖ Neutro | 🔴 Negativo
- **Análise**: [detalhes]

#### Edge Cases Identificados
1. [Edge case 1]
2. [Edge case 2]
3. [Edge case 3]

### 3.7 Validação de Banco de Dados

**Validado por**: 🗄️ @dba (quando aplicável)

**Impacto em BD**: ✅ Sim | ❌ Não

[SE SIM, PREENCHER SEÇÕES ABAIXO]

#### Banco de Dados
- **SGBD**: [PostgreSQL / MySQL / MongoDB / etc]
- **Versão**: [15.2 / 8.0 / etc]

#### Mudanças de Schema
```sql
-- Estrutura ANTES
[SQL da estrutura atual]

-- Estrutura DEPOIS
[SQL da estrutura proposta]
```

#### Índices
**Criar**:
```sql
CREATE INDEX [nome] ON [tabela]([campos]);
-- Justificativa: [razão]
```

**Remover**:
```sql
DROP INDEX [nome];
-- Justificativa: [razão]
```

#### Performance de Queries
| Query | Antes | Depois | Melhoria |
|-------|-------|--------|----------|
| [descrição] | [tempo] | [tempo] | [%] |

#### Migrations
**UP**: `migrations/YYYYMMDD_HHmmss_descricao.sql`
**DOWN**: `migrations/YYYYMMDD_HHmmss_descricao_rollback.sql`

---

## 4️⃣ Código Proposto

### 4.1 Estrutura de Arquivos

**Novos Arquivos**:
- `src/caminho/arquivo-novo.ext` - [descrição]

**Arquivos Modificados**:
- `src/caminho/arquivo-existente.ext` - [tipo de mudança]

**Arquivos Removidos**:
- `src/caminho/arquivo-antigo.ext` - [razão da remoção]

### 4.2 Código - Antes e Depois

#### Arquivo: `caminho/arquivo1.ext`

**ANTES**:
```[linguagem]
// Código atual com problema
class UserService {
  validate(email) {
    // Validação manual com regex
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
}
```

**DEPOIS**:
```[linguagem]
// Código proposto com melhoria
import { validateEmail } from './validators';

class UserService {
  validate(email) {
    // Usa biblioteca validator (já presente no projeto)
    return validateEmail(email);
  }
}
```

**Mudanças**:
- ✅ Substituída validação manual por biblioteca
- ✅ Consistente com resto do código
- ✅ Mais robusto (valida edge cases)

---

#### Arquivo: `caminho/arquivo2.ext`

**ANTES**:
```[linguagem]
[código anterior]
```

**DEPOIS**:
```[linguagem]
[código novo]
```

**Mudanças**:
- [mudança 1]
- [mudança 2]

---

### 4.3 Testes

#### Testes Unitários - Novos

**Arquivo**: `tests/unit/arquivo.test.ext`
```[linguagem]
describe('UserService', () => {
  describe('validate', () => {
    it('should validate correct email', () => {
      const service = new UserService();
      expect(service.validate('user@example.com')).toBe(true);
    });

    it('should reject invalid email', () => {
      const service = new UserService();
      expect(service.validate('invalid-email')).toBe(false);
    });

    it('should handle edge cases', () => {
      const service = new UserService();
      expect(service.validate('user+tag@example.co.uk')).toBe(true);
    });
  });
});
```

#### Testes de Integração - Novos

**Arquivo**: `tests/integration/arquivo.test.ext`
```[linguagem]
[código dos testes de integração]
```

#### Testes Modificados
- `tests/unit/arquivo-existente.test.ext` - [o que mudou]

### 4.4 Migrations (se aplicável)

#### Migration UP

**Arquivo**: `migrations/20250227143022_add_phone_to_users.sql`
```sql
-- Migration UP: Adicionar campo phone_number
-- Banco: PostgreSQL 15.2
-- Tempo estimado: 5-10 segundos
-- Requer backup: SIM

BEGIN;

ALTER TABLE users 
ADD COLUMN phone_number VARCHAR(20);

-- Criar índice (se necessário)
CREATE INDEX CONCURRENTLY idx_users_phone 
ON users(phone_number) 
WHERE phone_number IS NOT NULL;

COMMIT;
```

#### Migration DOWN

**Arquivo**: `migrations/20250227143022_add_phone_to_users_rollback.sql`
```sql
-- Migration DOWN: Remover campo phone_number
BEGIN;

DROP INDEX IF EXISTS idx_users_phone;
ALTER TABLE users DROP COLUMN phone_number;

COMMIT;
```

### 4.5 Configurações

**Arquivos de Configuração Alterados**:

#### `config/database.yml`
```yaml
# Mudança necessária na configuração
production:
  pool: 20  # Aumentado de 10 para 20
  timeout: 5000
```

---

## 5️⃣ Benefícios da Alteração

### 5.1 Benefícios Técnicos

#### Manutenibilidade
- ✅ **[Benefício 1]**
  - Descrição: [como melhora a manutenibilidade]
  - Impacto: [quantificar se possível]

- ✅ **[Benefício 2]**
  - Descrição: [como melhora]
  - Impacto: [quantificar]

#### Performance
- ⚡ **[Benefício de performance]**
  - Antes: [métrica]
  - Depois: [métrica]
  - Melhoria: [%]

#### Segurança
- 🔒 **[Benefício de segurança]**
  - Descrição: [o que melhora]
  - Impacto: [redução de risco]

#### Testabilidade
- 🧪 **[Benefício de testes]**
  - Descrição: [como facilita testes]
  - Cobertura: [antes vs depois]

### 5.2 Benefícios de Negócio

- 💼 **[Benefício 1]**
  - Descrição: [valor para o negócio]
  - Impacto: [em usuários/receita/etc]

- 💼 **[Benefício 2]**
  - Descrição: [valor]
  - Impacto: [quantificar]

### 5.3 Benefícios para o Usuário Final

- 👤 **[Benefício 1]**
  - Descrição: [o que muda para o usuário]
  - Impacto: [experiência melhorada]

- 👤 **[Benefício 2]**
  - Descrição: [melhoria]
  - Impacto: [como afeta uso]

### 5.4 Redução de Dívida Técnica

- **Dívida reduzida**: ✅ Sim | ❌ Não
- **Descrição**: [como reduz dívida técnica existente]
- **Quantificação**: [linhas de código removidas, complexidade reduzida, etc]

---

## 6️⃣ Impactos da Alteração

### 6.1 Impactos em Código

#### Módulos Afetados
| Módulo | Tipo de Impacto | Severidade | Descrição |
|--------|----------------|------------|-----------|
| [módulo 1] | [Modificação/Adição/Remoção] | 🔴/🟡/🟢 | [descrição] |
| [módulo 2] | [Modificação/Adição/Remoção] | 🔴/🟡/🟢 | [descrição] |

#### Dependências
- **Novas dependências**: [lista ou "Nenhuma"]
- **Dependências removidas**: [lista ou "Nenhuma"]
- **Dependências atualizadas**: [lista ou "Nenhuma"]

#### Breaking Changes
- **Identificados**: ✅ Sim | ❌ Não
- **Descrição**: [se sim, listar breaking changes]
- **Plano de migração**: [como lidar com breaking changes]

### 6.2 Impactos em Performance

#### Recursos Computacionais
- **CPU**: ⬆️ Aumenta | ➖ Neutro | ⬇️ Reduz - [%]
- **Memória**: ⬆️ Aumenta | ➖ Neutro | ⬇️ Reduz - [MB]
- **Disco**: ⬆️ Aumenta | ➖ Neutro | ⬇️ Reduz - [GB]
- **Network**: ⬆️ Aumenta | ➖ Neutro | ⬇️ Reduz - [Mbps]

#### Tempo de Resposta
| Operação | Antes | Depois | Variação |
|----------|-------|--------|----------|
| [operação 1] | [tempo] | [tempo] | [%] |
| [operação 2] | [tempo] | [tempo] | [%] |

### 6.3 Impactos em Banco de Dados

[SE APLICÁVEL]

#### Espaço
- **Tabelas**: +[X GB] / -[Y GB] / ➖ Sem mudança
- **Índices**: +[X GB] / -[Y GB] / ➖ Sem mudança
- **Total**: +[Z GB] estimado

#### Performance de Queries
- **Leituras**: ⬆️ Mais lentas | ➖ Neutro | ⬇️ Mais rápidas
- **Escritas**: ⬆️ Mais lentas | ➖ Neutro | ⬇️ Mais rápidas
- **Detalhes**: [explicação]

#### Downtime
- **Necessário**: ✅ Sim | ❌ Não
- **Duração estimada**: [tempo]
- **Janela recomendada**: [horário/dia]

### 6.4 Impactos em Testes

#### Cobertura de Testes
- **Antes**: [%]
- **Depois**: [%]
- **Variação**: [+/-X%]

#### Novos Testes Necessários
- [ ] Testes unitários: [quantidade]
- [ ] Testes de integração: [quantidade]
- [ ] Testes E2E: [quantidade]
- [ ] Testes de performance: [sim/não]

#### Testes a Atualizar
- [teste 1] - [tipo de atualização]
- [teste 2] - [tipo de atualização]

### 6.5 Impactos em Deploy

#### Processo de Deploy
- **Complexidade**: 🟢 Simples | 🟡 Moderada | 🔴 Complexa
- **Riscos**: 🟢 Baixo | 🟡 Médio | 🔴 Alto

#### Ordem de Deploy
1. [ ] [Passo 1]
2. [ ] [Passo 2]
3. [ ] [Passo 3]

#### Rollback
- **Facilidade**: 🟢 Fácil | 🟡 Moderado | 🔴 Difícil
- **Plano**: [descrever estratégia de rollback]

### 6.6 Impactos em Documentação

#### Documentação a Atualizar
- [ ] README.md
- [ ] API Documentation
- [ ] User Guide
- [ ] Architecture Decision Records (ADRs)
- [ ] [Outro documento]

#### Nova Documentação Necessária
- [ ] [Documento 1]
- [ ] [Documento 2]

### 6.7 Impactos em Usuários

#### Experiência do Usuário
- **Impacto**: 🟢 Positivo | ➖ Neutro | 🔴 Negativo
- **Descrição**: [como afeta usuários]

#### Compatibilidade
- **Backward compatible**: ✅ Sim | ❌ Não
- **Migração necessária**: ✅ Sim | ❌ Não
- **Detalhes**: [se necessária migração, descrever]

#### Comunicação Necessária
- [ ] Enviar email aos usuários
- [ ] Atualizar changelog
- [ ] Publicar release notes
- [ ] [Outra comunicação]

---

## 7️⃣ Resultados Esperados

### 7.1 Métricas de Sucesso

#### Métricas Técnicas
| Métrica | Baseline (Antes) | Meta (Depois) | Como Medir |
|---------|------------------|---------------|------------|
| [métrica 1] | [valor] | [valor] | [ferramenta/método] |
| [métrica 2] | [valor] | [valor] | [ferramenta/método] |
| [métrica 3] | [valor] | [valor] | [ferramenta/método] |

#### Métricas de Negócio
| Métrica | Baseline | Meta | Como Medir |
|---------|----------|------|------------|
| [métrica 1] | [valor] | [valor] | [ferramenta/método] |
| [métrica 2] | [valor] | [valor] | [ferramenta/método] |

### 7.2 Validação Pós-Deploy

#### Checklist de Validação
- [ ] Código deployado com sucesso
- [ ] Testes automatizados passando
- [ ] Smoke tests executados
- [ ] Performance monitorada
- [ ] Logs verificados (sem erros críticos)
- [ ] [SE BD] Migrations executadas com sucesso
- [ ] [SE BD] Integridade de dados validada
- [ ] Métricas de negócio coletadas

#### Período de Monitoramento
- **Duração**: [24h / 48h / 1 semana]
- **Responsável**: [nome/time]

#### Critérios de Sucesso
1. ✅ [Critério 1 - específico e mensurável]
2. ✅ [Critério 2 - específico e mensurável]
3. ✅ [Critério 3 - específico e mensurável]

#### Critérios de Rollback
Se qualquer um ocorrer, considerar rollback:
1. ❌ [Critério 1 - ex: taxa de erro > 5%]
2. ❌ [Critério 2 - ex: performance degradou > 20%]
3. ❌ [Critério 3 - ex: reclamações de usuários > X]

### 7.3 Monitoramento

#### Dashboards
- [Dashboard 1]: [URL]
- [Dashboard 2]: [URL]

#### Alertas Configurados
- 🔔 [Alerta 1]: Condição e ação
- 🔔 [Alerta 2]: Condição e ação

#### Logs a Acompanhar
```bash
# Comandos para verificar logs
[comando 1]
[comando 2]
```

---

## 8️⃣ Revisado Por

### 8.1 Validações dos Agentes

#### 🔧 Dev Senior
- **Nome/ID**: [identificação]
- **Data**: YYYY-MM-DD HH:MM
- **Decisão**: ✅ Aprovado | ⚠️ Com Ressalvas | ❌ Reprovado
- **Comentários**: 
  - [Comentário 1]
  - [Comentário 2]

---

#### 🏗️ Arquiteto
- **Nome/ID**: [identificação]
- **Data**: YYYY-MM-DD HH:MM
- **Decisão**: ✅ Aprovado | ⚠️ Com Ressalvas | ❌ Reprovado
- **Aspectos Validados**:
  - SOLID: [status]
  - Separação de Camadas: [status]
  - Acoplamento: [status]
  - Padrões de Design: [status]
- **Comentários**:
  - [Comentário 1]
  - [Comentário 2]
- **Ressalvas** (se houver):
  - [Ressalva 1]
  - [Ressalva 2]

---

#### 🎯 Especialista
- **Nome/ID**: [identificação]
- **Data**: YYYY-MM-DD HH:MM
- **Decisão**: ✅ Aprovado | ⚠️ Com Ressalvas | ❌ Reprovado
- **Aspectos Validados**:
  - Segurança: [status]
  - Performance: [status]
  - CVEs: [status]
  - Edge Cases: [status]
- **Comentários**:
  - [Comentário 1]
  - [Comentário 2]
- **Impacto em BD Identificado**: ✅ Sim | ❌ Não
- **Decisão sobre DBA**: [Chamado | Não necessário]

---

#### 🗄️ DBA
[SE APLICÁVEL]

- **Nome/ID**: [identificação]
- **Data**: YYYY-MM-DD HH:MM
- **Decisão**: ✅ Aprovado | ⚠️ Com Ressalvas | ❌ Reprovado
- **Aspectos Validados**:
  - Performance de Queries: [status]
  - Integridade de Dados: [status]
  - Migrations: [status]
  - Índices: [status]
- **Comentários**:
  - [Comentário 1]
  - [Comentário 2]
- **Condições** (se houver):
  - [ ] [Condição 1]
  - [ ] [Condição 2]

---

### 8.2 Decisão Final Consolidada

**Consolidado por**: 🔧 @dev-senior  
**Data**: YYYY-MM-DD HH:MM  
**Status Final**: ✅ APROVADO | ⚠️ APROVADO COM RESSALVAS | ❌ REPROVADO

#### Resumo das Validações
| Agente | Decisão | Peso |
|--------|---------|------|
| Dev Senior | [✅/⚠️/❌] | Proponente |
| Arquiteto | [✅/⚠️/❌] | Alto |
| Especialista | [✅/⚠️/❌] | Alto |
| DBA | [✅/⚠️/❌] | **Crítico** |

#### Consenso
- **Atingido**: ✅ Sim | ❌ Não
- **Análise**: [descrição do consenso ou divergências]

#### Próximos Passos
1. [ ] [Passo 1]
2. [ ] [Passo 2]
3. [ ] [Passo 3]

---

## 9️⃣ Histórico de Alterações

### Versão 1.0 - YYYY-MM-DD HH:MM
**Autor**: 🔧 @dev-senior  
**Tipo**: Criação inicial

**Mudanças**:
- ✨ Proposta inicial criada
- 📝 Objetivo definido
- 💻 Código proposto

---

### Versão 1.1 - YYYY-MM-DD HH:MM
**Autor**: 🏗️ @arquiteto  
**Tipo**: Validação arquitetural

**Mudanças**:
- ✅ Validação SOLID realizada
- 📐 Padrões arquiteturais verificados
- 💬 Sugestões de ajuste adicionadas

**Decisão**: [✅/⚠️/❌]

---

### Versão 1.2 - YYYY-MM-DD HH:MM
**Autor**: 🎯 @especialista  
**Tipo**: Validação técnica

**Mudanças**:
- 🔒 Segurança validada (CVEs verificados)
- ⚡ Performance analisada
- 🧪 Edge cases identificados
- 🗄️ Impacto em BD detectado

**Decisão**: [✅/⚠️/❌]  
**Ação**: DBA chamado para validação

---

### Versão 1.3 - YYYY-MM-DD HH:MM
**Autor**: 🗄️ @dba  
**Tipo**: Validação de banco de dados

**Mudanças**:
- 📊 Queries analisadas
- 🔑 Índices propostos
- 📝 Migrations criadas
- ⚠️ Riscos de BD documentados

**Decisão**: [✅/⚠️/❌]

---

### Versão 2.0 - YYYY-MM-DD HH:MM
**Autor**: 🔧 @dev-senior  
**Tipo**: Consolidação final

**Mudanças**:
- 📊 Todas as validações consolidadas
- 💻 Código final ajustado com feedbacks
- 📝 Documentação completa
- ✅ Aprovação final registrada

**Decisão Final**: [✅ APROVADO / ⚠️ APROVADO COM RESSALVAS / ❌ REPROVADO]

---

### Versão 2.1 - YYYY-MM-DD HH:MM
[SE HOUVE REFINAMENTO]

**Autor**: 🔧 @dev-senior  
**Tipo**: Refinamento após reprovação

**Mudanças**:
- 🔄 Proposta refinada incorporando feedbacks
- 💻 Código ajustado
- 📝 Documentação atualizada

**Motivo do refinamento**: [resumo das reprovações]

---

## 📎 Anexos

### Referências Externas
- [Título do link 1](URL)
- [Título do link 2](URL)
- [Documentação oficial consultada](URL)

### Pesquisas Realizadas

#### Por Dev Senior
- [Pesquisa 1]: [resultado]
- [Pesquisa 2]: [resultado]

#### Por Arquiteto
- ADR consultado: [ADR-XXX - título]
- [Pesquisa sobre patterns]: [resultado]

#### Por Especialista
- CVE Database: [resultado da busca]
- Compatibilidade: [verificações realizadas]

#### Por DBA
- Documentação do [banco]: [seções consultadas]
- Benchmarks: [resultados encontrados]

### Discussões Relacionadas
- Issue #[número]: [título] - [link]
- PR #[número]: [título] - [link]
- ADR anterior relacionado: [ADR-XXX]

### Arquivos Relacionados
- Diagrama de arquitetura: `docs/diagrams/[nome].png`
- Mockups: `docs/mockups/[nome].png`
- Scripts auxiliares: `scripts/[nome].sh`

---

## ✅ Checklist de Implementação

### Pré-Deploy
- [ ] Code review realizado
- [ ] Testes unitários criados/atualizados
- [ ] Testes de integração executados
- [ ] Cobertura de testes adequada (>80%)
- [ ] Documentação atualizada
- [ ] [SE BD] Backup do banco realizado
- [ ] [SE BD] Migration testada em dev/staging
- [ ] [SE BD] Pré-validações de dados executadas

### Deploy
- [ ] Código merged na branch principal
- [ ] Pipeline de CI/CD passou
- [ ] Deploy realizado em [ambiente]
- [ ] [SE BD] Migration executada
- [ ] [SE BD] Índices criados
- [ ] Smoke tests executados com sucesso
- [ ] Logs verificados (sem erros críticos)

### Pós-Deploy
- [ ] Monitoramento ativo por [período]
- [ ] Métricas coletadas e dentro do esperado
- [ ] [SE BD] Performance de queries validada
- [ ] [SE BD] Integridade de dados verificada
- [ ] Alertas funcionando corretamente
- [ ] Comunicação aos stakeholders (se necessário)
- [ ] Rollback plan testado e documentado

---

## 📝 Notas Adicionais

[Qualquer informação adicional relevante que não se encaixa nas seções anteriores]

---

**Fim do Documento**

---

## 📌 Metadados do Documento

- **Template Version**: 1.0
- **Última Atualização do Template**: 2025-02-27
- **Localização**: `/docs/decisions/YYYYMMDD_HHmmss_titulo-da-mudanca.md`
- **Formato**: Markdown
- **Encoding**: UTF-8

---

## 🔖 Tags

`#feature` `#refactoring` `#bug-fix` `#performance` `#database` `#security` `#architecture`

[Adicionar tags relevantes para facilitar busca]