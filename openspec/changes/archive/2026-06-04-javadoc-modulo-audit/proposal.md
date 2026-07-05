## Why

O módulo `scos-foundation-audit` não possui Javadoc em nenhuma interface pública, entidade ou enum — quem consume `ScosAuditQueryService`, `ScosAuditLog` ou `ActionType` precisa ler o README ou o código-fonte para entender contratos e semântica de campos, e IDEs não exibem documentação útil no hover.

## What Changes

- Adicionar Javadoc padrão Oracle/OpenJDK nas 3 interfaces de especificação (`ScosAuditService`, `ScosAuditQueryService`, `ScosAuditIntegrityService`)
- Adicionar Javadoc de campo nas 2 entidades de domínio (`ScosAuditLog`, `ScosAuditDlqLog`)
- Adicionar Javadoc em cada valor do enum `ActionType` (5 valores)
- Adicionar Javadoc de campo nas 5 classes de properties (`ScosAuditPerformanceProperties`, `ScosAuditDurabilityProperties`, `ScosAuditRetentionProperties`, `ScosAuditImmutabilityProperties`, `ScosAuditLogProperties`)
- Nenhuma mudança de comportamento — puramente documentação

## Capabilities

### New Capabilities

- `audit-public-api-javadoc`: Documentação Javadoc completa de todos os tipos públicos do módulo audit — interfaces, entidades, enums e properties — seguindo padrão Oracle/OpenJDK com `@param`, `@return`, `@since`, `{@code}` e `{@link}`.

### Modified Capabilities

<!-- Nenhuma mudança de requisitos funcionais -->

## Impact

- **Arquivos modificados**: 11 arquivos `.java` no módulo `audit` (apenas comentários Javadoc)
- **Código de produção**: sem alteração de comportamento
- **Compatibilidade**: sem breaking changes
- **Dependências**: nenhuma nova dependência
