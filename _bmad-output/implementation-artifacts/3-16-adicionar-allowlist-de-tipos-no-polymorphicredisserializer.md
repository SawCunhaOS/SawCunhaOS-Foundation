# Story 3.16: Adicionar allowlist de tipos no `PolymorphicRedisSerializer`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como mantenedor preocupado com segurança,
Eu quero que apenas tipos permitidos sejam desserializados do Redis,
Para eliminar o risco de desserialização de tipo arbitrário.

## Acceptance Criteria

1. **Given** um valor vindo do Redis antes de `Class.forName`, **When** a allowlist de tipos permitidos é aplicada, **Then** tipos fora da allowlist são rejeitados antes da desserialização.

## Tasks / Subtasks

- [ ] Task 1: Confirmar a vulnerabilidade atual (contexto) (AC: #1)
  - [ ] **Confirmado por leitura direta**: `PolymorphicRedisSerializer.deserialize()` (`utils/configuration/cache/PolymorphicRedisSerializer.java`, linhas 58-70) lê um `Payload(String type, JsonNode value)` do Redis e faz `Class<?> clazz = Class.forName(payload.type())` (linha 64) **sem nenhuma validação do nome da classe antes de resolvê-la** — qualquer string de nome de classe presente no Redis é resolvida e usada para desserialização polimórfica via `mapper.treeToValue(payload.value(), clazz)`. Se um atacante conseguir escrever no Redis usado por esta aplicação (ex.: Redis compartilhado, credencial vazada, ou mesmo um bug de outro consumidor gravando dados não confiáveis na mesma instância), pode forçar a desserialização de qualquer classe presente no classpath, incluindo classes com efeitos colaterais perigosos na construção/desserialização (gadget chains) — este é o achado de segurança citado no FR32
- [ ] Task 2: Implementar a allowlist (AC: #1)
  - [ ] Adicionar uma allowlist de tipos permitidos (via `Set<String>`/`Set<Class<?>>` configurável, ou por convenção de pacote — ex.: só permitir classes sob `br.com.sawcunhaos.foundation.*` mais os tipos conhecidos usados pelo módulo, como `IdempotentResponseWrapper`/`IdempotentRequestResponseWrapper`) verificada **antes** de `Class.forName(payload.type())` ser chamado
  - [ ] Se `payload.type()` não estiver na allowlist, `deserialize()` deve lançar `SerializationException` (mesmo tipo de exceção já usado pela classe no `catch` genérico) de forma explícita, sem tentar resolver a classe
  - [ ] A allowlist deve ser extensível pelo consumidor (este serializer é usado por qualquer módulo que precise cache Redis polimórfico, não só `jdempotent`) — permitir configuração da allowlist no construtor, mantendo um conjunto default razoável para não quebrar o uso atual sem configuração explícita
- [ ] Task 3: Testes (AC: #1)
  - [ ] Teste: payload com `type` de uma classe permitida → desserializa normalmente (sem regressão)
  - [ ] Teste: payload com `type` de uma classe fora da allowlist (ex.: uma classe arbitrária do JDK não relacionada ao domínio) → `deserialize()` rejeita com `SerializationException`, sem chamar `Class.forName` para o tipo não permitido
  - [ ] Teste: payload com `type` malformado/inexistente → comportamento de erro claro, sem vazar detalhes internos desnecessários na exceção

## Dev Notes

- `PolymorphicRedisSerializer` vive em `utils` (módulo compartilhado, não é exclusivo do `jdempotent`) — confirmar se, no momento da implementação, esta classe já migrou para o módulo `cache` criado na Epic 1 (Story 1.10); se sim, o path de arquivo muda mas a correção é a mesma.
- Este é o único uso de `Class.forName` sobre dado vindo de fonte externa (Redis) encontrado no módulo — não há necessidade de generalizar a allowlist para outros serializers que não existem.
- **Ponytail**: não construir um sistema de allowlist plugável/configurável via arquivo externo (YAML de allowlist, etc.) — um `Set<String>` (ou `Set<Class<?>>`) passado no construtor, com um default sensato, cobre o AC sem introduzir infraestrutura de configuração não pedida.

### Project Structure Notes

- Arquivo modificado: `utils/src/main/java/br/com/sawcunhaos/foundation/utils/configuration/cache/PolymorphicRedisSerializer.java` (ou o path equivalente no módulo `cache`, se já migrado pela Epic 1).

### References

- [Source: utils/src/main/java/br/com/sawcunhaos/foundation/utils/configuration/cache/PolymorphicRedisSerializer.java#L58-L70]
- [Source: _bmad-output/planning-artifacts/epics.md#story-316-adicionar-allowlist-de-tipos-no-polymorphicredisserializer]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
