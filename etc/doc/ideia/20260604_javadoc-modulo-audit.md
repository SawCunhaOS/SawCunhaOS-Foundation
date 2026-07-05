# Javadoc padrão de mercado nas interfaces públicas do módulo audit

**Data**: 2026-06-04  
**Status**: 🔄 Em Análise  
**Tipo**: 🔧 Refatoração

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

- **Nome da funcionalidade**: `javadoc-modulo-audit`
- **Resumo em uma frase**: Adicionar Javadoc padrão Oracle/OpenJDK nas interfaces públicas, entidades, enums e properties do módulo `scos-foundation-audit`.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico

---

## 1️⃣ Visão

### Problema
O módulo `audit` não tem Javadoc em nenhuma interface pública. Quem consome `ScosAuditQueryService`, `ScosAuditIntegrityService`, `ScosAuditLog` ou as classes de properties precisa ler o README ou o código-fonte para entender contratos, semântica de campos e valores de enum. IDEs não exibem documentação útil no hover.

### Objetivo
Adicionar Javadoc padrão de mercado (Oracle/OpenJDK) em todas as interfaces públicas, entidades, enums e properties do módulo. Critério de sucesso: hover no IDE exibe descrição do contrato, parâmetros e retorno para qualquer tipo público do módulo.

### Fora de Escopo
- Beans internos (`ScosAuditServiceBean`, `ScosAuditBatchConsumer`, `ScosAuditHashService`, etc.) — implementações internas não precisam de Javadoc público.
- Tags `@throws` — não aplicável (interfaces não declaram exceções de runtime).
- Tags `@author` e `@version` — substituídos por `git blame`/`git tag`.
- README e skill — cobre [[readme-e-skill-modulo-audit]].

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: `ScosAuditService` — Javadoc na interface e em cada método (`saveAuditLog`, `recordRead`).
- [ ] **RF-02**: `ScosAuditQueryService` — Javadoc na interface e em cada método (`findByEntity`, `findByUser`, `findByPeriod`, `findByXRequestId`).
- [ ] **RF-03**: `ScosAuditIntegrityService` — Javadoc na interface e no método `verifyChain`.
- [ ] **RF-04**: `ScosAuditLog` — Javadoc de campo em todos os atributos da entidade.
- [ ] **RF-05**: `ScosAuditDlqLog` — Javadoc de campo em todos os atributos.
- [ ] **RF-06**: `ActionType` — Javadoc em cada valor do enum explicando quando é gerado.
- [ ] **RF-07**: `ScosAuditPerformanceProperties` — Javadoc de campo em `queueCapacity`, `batchSize`, `flushIntervalMs`.
- [ ] **RF-08**: `ScosAuditDurabilityProperties` — Javadoc de campo em `retryMax`, `dlqEnabled`.
- [ ] **RF-09**: `ScosAuditRetentionProperties` — Javadoc de campo em `enabled`, `ttlDays`.
- [ ] **RF-10**: `ScosAuditImmutabilityProperties` — Javadoc de campo em `hashChain`.
- [ ] **RF-11**: `ScosAuditLogProperties` — Javadoc de campo em `system`, `enable`, `enableLiquibase`.

### Não-Funcionais
- [ ] **RNF-01**: Javadoc segue padrão Oracle/OpenJDK — primeira frase resumo, `<p>` para detalhes, `{@code}` para literais, `{@link}` para referências cruzadas.
- [ ] **RNF-02**: Parâmetros com `@param nome descrição` em todos os métodos com parâmetros.
- [ ] **RNF-03**: Retornos com `@return` em todos os métodos não-`void`.
- [ ] **RNF-04**: Tag `@since 1.2.0` em todas as interfaces e classes anotadas.
- [ ] **RNF-05**: Nenhum Javadoc descreve COMO — apenas O QUÊ e o CONTRATO.

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
audit/src/main/java/br/com/sawcunhaos/foundation/audit/
├── specification/
│   ├── ScosAuditService.java             — modificação (Javadoc)
│   ├── ScosAuditQueryService.java        — modificação (Javadoc)
│   └── ScosAuditIntegrityService.java    — modificação (Javadoc)
├── domain/entity/
│   ├── ScosAuditLog.java                 — modificação (Javadoc campos)
│   ├── ScosAuditDlqLog.java              — modificação (Javadoc campos)
│   └── ActionType.java                   — modificação (Javadoc valores)
└── configuration/properties/
    ├── ScosAuditPerformanceProperties.java  — modificação (Javadoc campos)
    ├── ScosAuditDurabilityProperties.java   — modificação (Javadoc campos)
    ├── ScosAuditRetentionProperties.java    — modificação (Javadoc campos)
    ├── ScosAuditImmutabilityProperties.java — modificação (Javadoc campos)
    └── ScosAuditLogProperties.java          — modificação (Javadoc campos)
```

### Padrão aplicado

```java
// Interface:
/**
 * Contrato para consulta paginada da trilha de auditoria.
 *
 * <p>Todas as queries operam no datasource dedicado de auditoria
 * ({@code spring.datasource.audit}) e não interferem no datasource
 * de negócio.
 *
 * @since 1.2.0
 */
public interface ScosAuditQueryService {

    /**
     * Retorna a trilha de um registro específico, ordenada por data de execução.
     *
     * @param entity   nome da entidade/tabela auditada (ex: {@code "SFA_PEDIDO"})
     * @param idEntity identificador do registro auditado
     * @param pageable paginação e ordenação
     * @return página de logs do registro, sem garantia de ordem se {@code pageable} não especificar sort
     */
    Page<ScosAuditLog> findByEntity(String entity, String idEntity, Pageable pageable);
}

// Enum valor:
/**
 * Leitura explícita de dado sensível, registrada via
 * {@link ScosAuditReadAspect} ou chamada direta a {@link ScosAuditService#recordRead}.
 */
SELECT,

// Campo de entidade:
/** Conteúdo serializado em JSONB do estado anterior ao evento. {@code null} em INSERT. */
@Column(name = "ENTITY_OLD", columnDefinition = "jsonb")
private String entityOld;

// Campo de properties:
/** Número máximo de tentativas de persistência antes de rotear para a DLQ. Padrão: {@code 3}. */
private int retryMax = 3;
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Escopo | Apenas interfaces públicas e tipos de domínio | Todos os beans | Beans internos mudam com frequência; Javadoc em impl. gera ruído |
| `@throws` | Não incluir | Declarar exceções de runtime | Contratos de runtime não são convencionalmente declarados em `@throws` Java |
| `@author` / `@version` | Não incluir | Incluir | Git blame/tag são a fonte de verdade; duplicar no Javadoc gera desatualização |
| `@since` | `1.2.0` em todas as interfaces e classes | Omitir | Facilita identificar a versão de introdução ao gerar documentação |

### Banco de Dados
- **Impacto**: ❌ Não

---

## 4️⃣ Implementação

### Arquivos Modificados
- `specification/ScosAuditService.java`
- `specification/ScosAuditQueryService.java`
- `specification/ScosAuditIntegrityService.java`
- `domain/entity/ScosAuditLog.java`
- `domain/entity/ScosAuditDlqLog.java`
- `domain/entity/ActionType.java`
- `configuration/properties/ScosAuditPerformanceProperties.java`
- `configuration/properties/ScosAuditDurabilityProperties.java`
- `configuration/properties/ScosAuditRetentionProperties.java`
- `configuration/properties/ScosAuditImmutabilityProperties.java`
- `configuration/properties/ScosAuditLogProperties.java`

### Tarefas
- [ ] **T-01**: Adicionar Javadoc em `ScosAuditService` (interface + 2 métodos)
- [ ] **T-02**: Adicionar Javadoc em `ScosAuditQueryService` (interface + 4 métodos)
- [ ] **T-03**: Adicionar Javadoc em `ScosAuditIntegrityService` (interface + 1 método)
- [ ] **T-04**: Adicionar Javadoc de campo em `ScosAuditLog` (11 campos)
- [ ] **T-05**: Adicionar Javadoc de campo em `ScosAuditDlqLog` (6 campos)
- [ ] **T-06**: Adicionar Javadoc em cada valor de `ActionType` (5 valores)
- [ ] **T-07**: Adicionar Javadoc de campo nas 5 classes de properties

### Riscos e Edge Cases
1. Campos Lombok (`@Getter`/`@Setter`) — Javadoc de campo é gerado corretamente por ferramentas como Checkstyle e IDEs mesmo com Lombok.
2. `ScosAuditLogProperties` mistura `@ConfigurationProperties` e `@Value` — documentar os campos com os valores padrão reais.

---

## 📎 Referências
- [[readme-e-skill-modulo-audit]] — ideia complementar (README + skill)
- [Oracle Javadoc Style Guide](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html)
- [OpenJDK Writing Great Javadoc](https://openjdk.org/groups/quality/JavadocGuide.html)

---
