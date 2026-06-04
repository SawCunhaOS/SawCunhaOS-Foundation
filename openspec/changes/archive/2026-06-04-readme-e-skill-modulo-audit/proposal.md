## Why

O README do módulo `audit` não mostra a arquitetura interna — o consumidor não entende a fila em memória, o que acontece quando está cheia, ou como o DLQ se comporta; a skill `scos-audit-config` cobre apenas `@Auditable` e cifra PII, deixando o Claude sem guia para as funcionalidades mais ativadas: query, hash-chain, retenção e DLQ.

## What Changes

- Adicionar diagrama ASCII do fluxo interno completo no README (Hibernate/AOP → Queue → BatchConsumer → DB/DLQ)
- Adicionar seção **Troubleshooting** no README com os 4 casos mais comuns (módulo não inicia, eventos não aparecem, fila cheia, DLQ acumulando)
- Expandir skill `scos-audit-config` com seções 4–8: leituras PII, query service, hash-chain, retenção, DLQ monitoring
- Adicionar seção 10 na skill: tabela de referência completa de todas as properties `scos.audit.*` + `spring.datasource.audit.*`
- Atualizar seção Pegadinhas da skill com casos de DLQ desabilitado e retenção sem `ttl-days`

## Capabilities

### New Capabilities

- `audit-readme-diagrama`: Diagrama ASCII da arquitetura interna e seção de troubleshooting no README do módulo audit.
- `audit-skill-happy-path`: Skill `scos-audit-config` expandida com happy-path completo (seções 4–10) e tabela de referência de properties.

### Modified Capabilities

<!-- Nenhuma mudança de requisitos funcionais em specs existentes -->

## Impact

- **Arquivos modificados**: `audit/README.md`, `etc/doc/skills/scos-audit-config/SKILL.md`
- **Código de produção**: sem alteração
- **Compatibilidade**: sem breaking changes
- **Dependências**: nenhuma nova dependência
