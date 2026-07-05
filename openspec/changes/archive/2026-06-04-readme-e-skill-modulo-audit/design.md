## Context

O módulo `audit` possui README bem escrito com exemplos de configuração e uso, mas sem representação visual da arquitetura interna e sem guia de diagnóstico. A skill `scos-audit-config` é usada pelo Claude ao auxiliar consumidores a configurar o módulo, mas está incompleta — cobre apenas o caso básico (`@Auditable` em entidade + datasource). Features ativadas frequentemente (query, hash-chain, retenção) não têm guia na skill.

Esta mudança é puramente documental: nenhum código de produção é alterado.

## Goals / Non-Goals

**Goals:**
- README com diagrama ASCII do fluxo completo legível em terminal de 80 colunas
- Seção Troubleshooting no README com os 4 casos que exigem ação do consumidor
- Skill com happy-path completo (seções 1–10) referenciando o README para detalhes
- Tabela de referência de properties na skill para uso rápido pelo Claude

**Non-Goals:**
- Documentação de arquitetura (ADR)
- Alterações no código-fonte do módulo
- Cobertura exaustiva de tuning na skill (delegar ao README)

## Decisions

### Diagrama ASCII no README (não PlantUML/Mermaid)

ASCII funciona em qualquer viewer (GitHub, terminal, IDEs), sem dependência de renderer. PlantUML e Mermaid requerem suporte no viewer; o projeto não tem pipeline de renderização de diagramas.

### Troubleshooting no README (não documento separado)

O consumidor busca no README ao ter um problema. Documento separado seria ignorado. Quatro casos cobertos: módulo não inicia, eventos não aparecem, fila cheia, DLQ acumulando — todos requerem ação do consumidor, não são bugs internos.

### Skill: happy-path + referência ao README, não skill exaustiva

Uma skill maior que o README duplica manutenção e sai de sincronia. Cada seção nova da skill (4–8) tem exemplo mínimo funcional e aponta para `audit/README.md` para tuning avançado.

### Tabela de properties na skill (seção 10), não só no README

O Claude consulta a skill ao configurar um serviço. Ter a tabela na skill evita que o Claude precise abrir o README só para verificar o nome exato de uma property ou seu default.

### Posicionamento do diagrama no README

Logo após o parágrafo de introdução, antes da seção de dependência — o leitor entende a arquitetura antes de começar a configurar.

### Posicionamento do Troubleshooting no README

Após a seção de Métricas — segue a ordem natural de leitura: configurar → operar → monitorar → diagnosticar.

## Risks / Trade-offs

- **Diagrama desatualiza com o código** → Mitigação: adicionar nota `v1.2.0` no diagrama para rastreabilidade; diagrama descreve componentes estáveis (queue, batch, DLQ) que mudam raramente.
- **Skill diverge do README** → Mitigação: cada seção da skill referencia explicitamente a seção correspondente no README; ao atualizar o README, o revisor verifica a skill.
