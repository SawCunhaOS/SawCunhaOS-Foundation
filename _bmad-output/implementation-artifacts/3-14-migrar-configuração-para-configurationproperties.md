# Story 3.14: Migrar configuração para `@ConfigurationProperties`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor configurando o módulo,
Eu quero propriedades tipadas e centralizadas,
Para não depender de `@Value` solto nem de propriedades mortas.

## Acceptance Criteria

1. **Given** a configuração atual via `@Value` e um `EnvironmentPostProcessor` declarado incorretamente em `AutoConfiguration.imports` (nunca executa), **When** a migração para `@ConfigurationProperties` é concluída, **Then** propriedades mortas são removidas e o `EnvironmentPostProcessor` passa a executar corretamente.

## Tasks / Subtasks

- [ ] Task 1: Confirmar o registro incorreto do `EnvironmentPostProcessor` (contexto) (AC: #1)
  - [ ] **Confirmado por leitura direta**: `jdempotent/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` lista `ScosJdempotentRedisEnvironmentPostProcessor` junto com as demais classes `@AutoConfiguration` (`ScosJdempotentRedisProperties`, `ScosJdempotentRedisConfiguration`, `ScosJdempotentConfig`). O contrato `EnvironmentPostProcessor` do Spring Boot é carregado por um SPI **diferente e mais cedo** (`org.springframework.boot.env.EnvironmentPostProcessor`, via `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` no Spring Boot 3+/4, executado por `SpringApplication` **antes** do `ApplicationContext` existir) — listar a classe em `AutoConfiguration.imports` só faz o Spring tentar instanciá-la como bean `@Configuration` durante o refresh do contexto, momento em que `postProcessEnvironment()` **nunca é chamado** por esse caminho. O efeito prático: `spring.data.redis.repositories.enabled=false` (a única coisa que este post-processor faz, ver `ScosJdempotentRedisEnvironmentPostProcessor.java` linha 40) **nunca é aplicado** hoje
- [ ] Task 2: Registrar o `EnvironmentPostProcessor` no arquivo correto (AC: #1)
  - [ ] Criar/editar `jdempotent/src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` contendo `br.com.sawcunhaos.foundation.jdempotent.redis.configuration.ScosJdempotentRedisEnvironmentPostProcessor`
  - [ ] Remover a linha correspondente de `org.springframework.boot.autoconfigure.AutoConfiguration.imports` — a classe não deve estar nos dois arquivos (duplicaria a instanciação por dois SPIs distintos, com riscos diferentes: o SPI de `EnvironmentPostProcessor` roda sem contexto Spring, então a classe não pode depender de injeção de dependência do container nesse momento)
  - [ ] Remover a anotação `@AutoConfiguration` e o `@ConditionalOnProperty` da classe (esse post-processor roda antes de qualquer property source de aplicação estar totalmente resolvida via `@ConditionalOnProperty` — o padrão correto do Spring Boot para `EnvironmentPostProcessor` condicional é checar a property diretamente dentro de `postProcessEnvironment()`, não via anotação de bean)
- [ ] Task 3: Migrar `@Value` para `@ConfigurationProperties` (AC: #1)
  - [ ] `ScosJdempotentRedisProperties.java` já é `@Data` com múltiplos `@Value("${scos.jdempotent.cache.redis.*}")` — converter para `@ConfigurationProperties(prefix = "scos.jdempotent.cache.redis")` com os campos correspondentes, removendo `@Value` campo a campo
  - [ ] `ConfigUtility.java` (`jdempotent/core/config/ConfigUtility.java`) usa `@Value("${scos.jdempotent.cryptography.algorithm:md5}")` — **esta propriedade é morta**: o campo `algorithm` não tem getter e não é lido em nenhum lugar do código (confirmado durante a Story 3.2, que já hardcoda o algoritmo via `CryptographyAlgorithm.SHA256`). Remover `ConfigUtility.java` inteiramente (a classe só existe para essa propriedade morta) ou, se alguma referência externa depender da property key existir (confirmar via busca antes de remover), documentar a remoção no CHANGELOG
  - [ ] Avaliar se `@RefreshScope` (de `spring-cloud-context`, presente em `ScosJdempotentRedisProperties`/`ConfigUtility`/`ScosJdempotentConfig`) continua necessário após a migração — isso conecta com a avaliação já feita na Epic 1 Story 1.2 sobre a necessidade real de `spring-cloud-starter`; não remover `@RefreshScope` nesta story sem confirmar que nenhum consumidor depende de refresh dinâmico dessas propriedades em produção (mudança de comportamento, não só de mecanismo)
- [ ] Task 4: Testes (AC: #1)
  - [ ] Teste de contexto Spring confirmando que `spring.data.redis.repositories.enabled` é efetivamente `false` após a subida da aplicação (prova de que o `EnvironmentPostProcessor` agora executa) — este teste deve **falhar contra o registro atual** (em `AutoConfiguration.imports`) e passar após a correção
  - [ ] Teste confirmando o binding de `@ConfigurationProperties` com valores customizados via `application.yml`/`application.properties` de teste

## Dev Notes

- Esta story tem sobreposição com a Story 3.10 (namespace configurável via propriedade Spring) — coordenar para não duplicar a criação da classe de propriedades. Ver Dev Notes da Story 3.10.
- **Achado adicional, fora do AC literal mas relevante para o dev-agent**: `ScosJdempotentRedisProperties` tem `@Value("${scos.jdempotent.cache.redis.expirationTimeHour}")` **sem valor default** — se a propriedade não for configurada, o binding falha na subida da aplicação (comportamento já "explícito" nesse campo específico, diferente do problema do namespace na Story 3.10, que é silencioso). Ao migrar para `@ConfigurationProperties`, preservar esse comportamento de obrigatoriedade (não introduzir um default silencioso onde hoje não existe).
- **NFR2**: separar em commits distintos — (1) mover o `EnvironmentPostProcessor` para o arquivo SPI correto, (2) migrar `@Value` → `@ConfigurationProperties`, (3) remover `ConfigUtility`/propriedades mortas — são três mudanças de comportamento/mecanismo diferentes, mesmo que relacionadas.

### Project Structure Notes

- Arquivos modificados: `jdempotent/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, `ScosJdempotentRedisEnvironmentPostProcessor.java`, `ScosJdempotentRedisProperties.java`.
- Arquivo novo: `jdempotent/src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`.
- Arquivo possivelmente removido: `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/config/ConfigUtility.java` (propriedade morta).

### References

- [Source: jdempotent/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/configuration/ScosJdempotentRedisEnvironmentPostProcessor.java]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/redis/configuration/ScosJdempotentRedisProperties.java]
- [Source: jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/config/ConfigUtility.java]
- [Source: _bmad-output/planning-artifacts/epics.md#story-314-migrar-configuração-para-configurationproperties]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
