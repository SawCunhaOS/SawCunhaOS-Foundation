# Story 3.10: Tornar o namespace de prefixo de chave configurável

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor compartilhando a mesma instância de Redis entre aplicações,
Eu quero configurar o namespace via propriedade Spring,
Para não colidir chaves entre aplicações diferentes.

## Acceptance Criteria

1. **Given** a leitura atual via `System.getenv(APP_NAME)` (silenciosamente vazia quando ausente), **When** o namespace passa a ser obrigatório e configurável via propriedade Spring, **Then** a aplicação falha de forma explícita (não silenciosa) se o namespace não for configurado.

## Tasks / Subtasks

- [ ] Task 1: Confirmar o comportamento atual (contexto) (AC: #1)
  - [ ] **Confirmado por leitura direta**: `DefaultKeyGenerator` (linha 33) — `appName = System.getenv(EnvironmentVariableUtils.APP_NAME)`, onde `APP_NAME = "APP_NAME"` (`EnvironmentVariableUtils.java`). Se a variável de ambiente `APP_NAME` não estiver definida, `appName` fica `null`, e `generateIdempotentKey()` (linha 50) faz `if (!StringUtils.isBlank(appName)) { builder.append(appName)... }` — ou seja, **silenciosamente pula o prefixo** sem erro nem aviso. Duas aplicações diferentes rodando sem `APP_NAME` configurado, contra o mesmo Redis, podem colidir chaves de idempotência sem nenhum sinal de que isso está acontecendo
- [ ] Task 2: Introduzir a propriedade Spring obrigatória (AC: #1)
  - [ ] Adicionar uma propriedade `@ConfigurationProperties` (coordenar com a Story 3.14, que migra a configuração do módulo de `@Value` para `@ConfigurationProperties` — se a 3.14 já estiver implementada, adicionar o namespace lá; se não, introduzir aqui e deixar a 3.14 absorver depois) para o namespace de prefixo (ex.: `scos.jdempotent.namespace`), sem valor default
  - [ ] Se a propriedade não for configurada, a aplicação deve **falhar explicitamente na inicialização** (ex.: `@ConfigurationProperties` com validação `@NotBlank`/`@Validated`, ou um `@PostConstruct` que lança exceção clara) — não silenciosamente seguir sem namespace, como acontece hoje
- [ ] Task 3: Substituir o uso de `System.getenv(APP_NAME)` (AC: #1)
  - [ ] `DefaultKeyGenerator` deixa de ler `System.getenv` diretamente e passa a receber o namespace já resolvido pela configuração Spring (via injeção de dependência do bean de propriedades, não mais leitura direta de variável de ambiente no construtor)
- [ ] Task 4: Testes (AC: #1)
  - [ ] Teste: contexto Spring sobe sem a propriedade de namespace configurada → confirmar que a inicialização falha com uma mensagem de erro clara (não um `NullPointerException` genérico nem silêncio)
  - [ ] Teste: contexto Spring sobe com a propriedade configurada → confirmar que o prefixo aparece corretamente na chave gerada

## Dev Notes

- Esta story tem sobreposição direta com a Story 3.14 (migração `@Value` → `@ConfigurationProperties`) — decidir na implementação se o namespace entra como parte da mesma classe de propriedades criada pela 3.14 ou como uma propriedade isolada introduzida aqui. Documentar a decisão nas Completion Notes para não duplicar trabalho entre as duas stories.
- **Mudança de comportamento visível ao consumidor**: hoje uma aplicação sem `APP_NAME` sobe normalmente (com risco silencioso de colisão); depois desta story, a mesma aplicação **não sobe** sem a propriedade configurada. Isto é uma mudança de comportamento breaking, intencional e exigida pelo AC — documentar no CHANGELOG do módulo como mudança que requer ação do consumidor antes de atualizar a versão.
- Não implementar fallback automático para `System.getenv(APP_NAME)` como forma de manter compatibilidade — o AC pede explicitamente que a ausência de configuração **falhe**, não que degrade silenciosamente para o comportamento antigo.

### Project Structure Notes

- Arquivo modificado: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/generator/DefaultKeyGenerator.java`.
- Arquivo novo ou modificado: classe de propriedades Spring (local final depende da coordenação com a Story 3.14).

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/generator/DefaultKeyGenerator.java#L28-L34]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/constant/EnvironmentVariableUtils.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-310-tornar-o-namespace-de-prefixo-de-chave-configurável]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
