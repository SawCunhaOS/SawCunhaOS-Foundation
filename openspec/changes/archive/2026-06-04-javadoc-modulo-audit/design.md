## Context

O módulo `scos-foundation-audit` expõe três interfaces de especificação, duas entidades JPA, um enum e cinco classes de properties — todos sem Javadoc. A ausência de documentação força o consumidor a ler o código-fonte ou o README para entender contratos como a semântica de `entityOld`/`entityNew`, o que `TOMBSTONE` representa, ou quais valores são obrigatórios em `ScosAuditRetentionProperties`.

A mudança é puramente documental: adiciona comentários Javadoc nos tipos públicos sem alterar nenhuma linha de código de produção.

## Goals / Non-Goals

**Goals:**
- Javadoc padrão Oracle/OpenJDK em todos os 11 arquivos públicos do módulo
- Hover no IDE exibe descrição de contrato, parâmetros e retorno para qualquer tipo público
- `@since 1.2.0` em todas as interfaces e classes

**Non-Goals:**
- Beans de implementação (`*ServiceBean`, `*BatchConsumer`, `*HashService`, etc.)
- Mudança de comportamento ou refatoração
- Tags `@throws`, `@author`, `@version`
- Javadoc em testes

## Decisions

### Padrão: Oracle/OpenJDK

Primeira frase é o resumo (usada por IDEs no tooltip curto). Parágrafos adicionais com `<p>`. Literais com `{@code}`. Referências cruzadas com `{@link}`.

Alternativa descartada: estilo Kotlin/KDoc — inconsistente com o ecossistema Spring/Java já presente no projeto.

### Escopo: apenas tipos públicos do contrato

Beans internos (`*ServiceBean`, `*BatchConsumer`, `*HashService`, `*Aspect`, `*Job`) são excluídos. Implementações mudam com frequência; Javadoc em impl. gera documentação que desatualiza rapidamente e não agrega valor ao consumidor.

Alternativa descartada: documentar todos os arquivos — aumenta manutenção sem benefício para o consumidor da lib.

### `@param` e `@return` obrigatórios, `@throws` omitido

`@param` e `@return` definem o contrato visível. `@throws` é omitido: exceções de runtime não fazem parte do contrato público de uma interface Java; documentá-las criaria acoplamento com a implementação.

### Campos de entidade: Javadoc de campo (não getter)

O Javadoc é colocado no campo declarado, não no getter gerado pelo Lombok. IDEs como IntelliJ IDEA resolvem o Javadoc do campo mesmo quando o acesso é via getter.

### `@since 1.2.0`

Usada apenas em tipos (interfaces e classes), não em métodos individuais. Indica a versão em que o tipo foi introduzido — útil ao gerar Javadoc HTML do módulo.

## Risks / Trade-offs

- **Javadoc desatualizado com o tempo** → Mitigação: Javadoc descreve apenas O QUÊ (contrato), não COMO (implementação), tornando-o mais estável a refatorações internas.
- **Lombok + Javadoc de campo** → IDEs resolvem corretamente; ferramentas como Checkstyle e `javadoc` tool também processam corretamente campos com anotações Lombok.
- **`ScosAuditLogProperties` mistura `@ConfigurationProperties` e `@Value`** → Documentar com os valores default reais observados no código (não nos metadados do Spring Boot).
