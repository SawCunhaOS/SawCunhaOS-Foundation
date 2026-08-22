# Story 3.1: Corrigir a inicialização do `RedisConnectionFactory`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor com Redis standalone ou cluster,
Eu quero que o módulo inicialize sem NPE,
Para poder habilitar `jdempotent` sem workaround.

## Acceptance Criteria

1. **Given** uma aplicação com Redis standalone/cluster (não Sentinel), **When** o módulo inicializa usando `RedisConnectionFactory` do Spring Boot em vez de montar `RedisSentinelConfiguration` hardcoded, **Then** a inicialização não lança NPE em `getSentinel()`.

## Tasks / Subtasks

- [ ] Task 1: Remover a montagem manual de `RedisSentinelConfiguration` (AC: #1)
  - [ ] Em `ScosJdempotentRedisConfiguration.lettuceConnectionFactory()` (linhas 56-94), remover a construção de `new RedisSentinelConfiguration().master(redisProperties.getSentinel().getMaster())` — hoje isso chama `.getSentinel()` incondicionalmente; em topologia standalone/cluster (não Sentinel) essa chamada retorna `null` e `.getMaster()` lança NPE
  - [ ] Substituir por injeção do `RedisConnectionFactory` autoconfigurado pelo Spring Boot (via `org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration`, que já lê `spring.data.redis.*` e resolve standalone/sentinel/cluster corretamente a partir da mesma `DataRedisProperties` já injetada nesta classe)
  - [ ] Manter o `LettuceClientConfiguration` customizado (timeouts de comando/conexão, `disconnectedBehavior`) — não descartar essa configuração de resiliência, só a montagem hardcoded do Sentinel
- [ ] Task 2: Preservar o bean `JdempotentRedisTemplate` (AC: #1)
  - [ ] `redisTemplate()` (linhas 96-105) continua recebendo a connection factory por parâmetro — só troca a origem do bean, não a assinatura do método nem o `PolymorphicRedisSerializer` já configurado
- [ ] Task 3: Cobrir os três cenários de topologia com teste
  - [ ] Teste de contexto Spring com `spring.data.redis.host`/`port` apenas (standalone) confirmando que a inicialização não lança `NullPointerException`
  - [ ] Teste (ou verificação por inspeção de configuração) confirmando que a topologia Sentinel configurada via `spring.data.redis.sentinel.*` continua funcionando (não é regressão desta correção)

## Dev Notes

- **Bug real confirmado por leitura direta do código** (não é hipotético): `ScosJdempotentRedisConfiguration.java`, método `lettuceConnectionFactory()`, linhas 61-75 — `redisProperties.getSentinel().getMaster()` é chamado sem checar se `getSentinel()` retorna `null`. Em `application.yml` com `spring.data.redis.host`/`port` (standalone) e sem bloco `sentinel:`, `DataRedisProperties.getSentinel()` retorna `null` e a inicialização quebra com NPE antes mesmo do contexto Spring subir.
- A classe já injeta `DataRedisProperties redisProperties` (Spring Boot `spring.data.redis.*`) via `@RequiredArgsConstructor` — é a mesma propriedade que o `RedisAutoConfiguration` nativo do Boot usa para decidir standalone vs. sentinel vs. cluster automaticamente. Não é necessário introduzir uma nova fonte de configuração.
- O `LettuceClientConfiguration` manual (timeouts, `autoReconnect`, `disconnectedBehavior=REJECT_COMMANDS`) é comportamento deliberado desta lib e deve ser preservado — a correção é só na fonte da topologia de conexão, não na configuração de resiliência do cliente Lettuce.
- **NFR4** (escopo de mudança): não alterar o `PolymorphicRedisSerializer` nem o `StringRedisSerializer` já configurados no `redisTemplate()` — fora do escopo desta story (`PolymorphicRedisSerializer` é tratado na Story 3.16).

### Project Structure Notes

- Arquivo modificado: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/configuration/ScosJdempotentRedisConfiguration.java`.
- Nenhum módulo novo é criado nesta story.

### References

- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/configuration/ScosJdempotentRedisConfiguration.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-31-corrigir-a-inicialização-do-redisconnectionfactory]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Foundation-2026-08-19/ARCHITECTURE-SPINE.md]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
