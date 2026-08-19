# Plano de Atualização — `scos-foundation-jdempotent`

**Origem / Alvo:** `1.2.0-SNAPSHOT` (mesma versão — nada foi publicado ainda) · **Esforço estimado:** 10–14 dias úteis

---

## 1. Objetivo

Três frentes, nesta ordem de dependência:

| # | Frente | Por quê |
|---|--------|---------|
| **A** | Correção da geração de chaves | Duas fontes independentes de colisão fazem requisições distintas compartilharem resposta |
| **B** | Correção da execução simultânea | Não há operação atômica: chamadas concorrentes com a mesma chave executam as duas |
| **C** | Opção de header de idempotência | Cobrir métodos sem chave natural e dar autoridade ao cliente sobre o que é retry |

**A** e **B** são correção de defeito. **C** é funcionalidade nova. A ordem importa: adicionar header sobre uma base que colide chaves e não serializa execução só aumenta a superfície do problema.

---

## 2. Princípio que orienta o plano

> Idempotência em cache é **fast-path**, não garantia.

Onde a chave for natural (`login`, `tenant + cpf`), a garantia é a constraint `UNIQUE` no banco. O `jdempotent` evita o trabalho e devolve a resposta original em vez de um erro de constraint — mas quando ele falha aberto, o banco ainda segura.

Isso precisa estar escrito no README do módulo, porque hoje a ausência dessa frase leva times a confiarem no Redis como barreira única.

### Premissas fixas

Não são decisões abertas neste plano:

1. **Redis fora ⇒ a aplicação se comporta como se a idempotência não estivesse habilitada.** Fail-open. O módulo nunca derruba uma requisição de negócio por indisponibilidade de infraestrutura. Detalhamento na seção 2.4.
2. Decorre de (1): constraint `UNIQUE` no banco é **obrigatória** em todo método com chave natural.
3. **A biblioteca não opina sobre a infraestrutura Redis do consumidor.** Topologia, endereço, timeout e pool são configuração da aplicação. O módulo consome o `RedisConnectionFactory` que o Spring Boot já monta a partir de `spring.data.redis.*` — não constrói conexão própria. Detalhamento na seção 2.6.

---

## 3. Fases

### Fase 0 — Rede de proteção
**2 dias · pré-requisito de tudo**

Nenhuma correção entra sem um teste que falhe antes dela.

| Entregável | Detalhe |
|---|---|
| Teste de concorrência | N threads (`CountDownLatch`) na mesma chave, contador de execuções reais do método. Hoje passa de 1 — deve ficar em 1 |
| Teste de colisão de chave | Payloads escolhidos para colidir no hex sem padding; hoje geram a mesma chave |
| Teste de duplo aspecto | Contexto com `ErrorConditionalCallback` declarado; assertar exatamente 1 bean `IdempotentAspect` |
| Teste de indisponibilidade | Testcontainers Redis derrubado no meio; assertar que o método executa (fail-open), que o gauge de degradação sobe e que a **latência adicional por requisição fica dentro do orçamento** — hoje seria o timeout de 5 s |
| Baseline de cobertura | Adicionar regra `jacoco:check` no pom — o módulo é o único sem, os outros exigem 80% |

**Critério de aceite:** os quatro testes existem e falham.

---

### Fase 1 — Correção das chaves
**2 dias · sem mudança de API**

| # | Correção | Onde |
|---|---|---|
| 1.1 | `HexFormat.of().formatHex(digest)` no lugar do laço com `Integer.toHexString` | `DefaultKeyGenerator` |
| 1.2 | MD5 → SHA-256, algoritmo configurável usando o enum `CryptographyAlgorithm` que já existe e nunca foi usado | `IdempotentAspect` |
| 1.3 | `@ConditionalOnMissingBean(IdempotentAspect.class)` no bean sem condição | `ScosJdempotentConfig` |
| 1.4 | Percorrer a hierarquia de classes, não só `getDeclaredFields()` — campos herdados são ignorados hoje | `IdempotentAspect` |
| 1.5 | `MessageDigest` local por chamada, removendo o `ThreadLocal` (contraproducente com virtual threads e com NPE latente quando o algoritmo não existe) | `IdempotentAspect` |

**Impacto operacional:** 1.1 e 1.2 mudam **todas** as chaves geradas. Como nada foi publicado, não há contrato a preservar — mas se houver algum ambiente de dev com Redis vivo, as entradas antigas viram lixo até expirarem pelo TTL. Nada corrompe.

**Critério de aceite:** testes 0.2 e 0.3 passam. Nenhum comportamento novo.

---

### Fase 2 — Execução simultânea
**4 dias · núcleo do trabalho**

O contrato atual (`contains` → `store` → `setResponse`) é irrecuperável: são três operações não atômicas com janelas entre elas. Substituir, não remendar.

#### 2.1 Novo contrato do repositório

Uma única operação decide tudo:

```
tryAcquire(key, payloadHash, ttl) → Lease
```

`Lease` é um dos quatro estados:

| Estado | Significado | Ação do aspecto |
|---|---|---|
| `ACQUIRED` | Primeiro a chegar | Executa o método |
| `IN_PROGRESS` | Outra chamada está executando agora | `409 Conflict` (ou `425`) — **hoje devolve `null` silenciosamente** |
| `COMPLETED` | Já executou, resposta guardada | Devolve a resposta original |
| `PAYLOAD_MISMATCH` | Mesma chave, corpo diferente | `422 Unprocessable Entity` |

#### 2.2 Implementação Redis

##### Por que Lua

O `SET key value NX PX ttl` do Redis resolve **um** dos quatro casos: ele diz se você adquiriu ou não. Não diz *por que* não adquiriu — se já concluiu, se está em andamento, ou se o payload diverge. Descobrir isso exige um `GET` adicional, e entre o `SET` e o `GET` existe uma janela em que outro processo muda o estado.

Pior é a conclusão e a liberação: ambas são **compare-and-set** ("só altere se eu ainda for o dono"). Isso é impossível com comandos soltos. `GET` + `DEL` tem o defeito clássico do lock distribuído em Redis: se o seu lease expirou e outro processo já adquiriu a chave, o seu `DEL` apaga o lock **dele**.

Lua roda atomicamente no servidor: a decisão inteira é uma única operação indivisível.

##### Estrutura da chave

Hash com campos separados, em vez de um blob serializado:

| Campo | Conteúdo |
|---|---|
| `state` | `IN_PROGRESS` ou `COMPLETED` |
| `payload_hash` | Para detectar reuso de chave com corpo diferente |
| `owner` | Token do processo que detém o lease (fencing) |
| `response` | Resposta serializada, preenchida só na conclusão |

Efeito colateral positivo: o Lua manipula strings, então esse caminho **deixa de usar o `PolymorphicRedisSerializer`** — some dele o `Class.forName` sem allowlist apontado no review. A serialização da resposta passa a acontecer em Java, antes da chamada.

Requer um `RedisTemplate<String, String>` separado do template polimórfico atual.

##### Script 1 — `tryAcquire`

`KEYS[1]` chave · `ARGV[1]` payload hash · `ARGV[2]` owner token · `ARGV[3]` TTL de lease (ms)

```lua
local state = redis.call('HGET', KEYS[1], 'state')

if state == false then
  redis.call('HSET', KEYS[1],
    'state', 'IN_PROGRESS',
    'payload_hash', ARGV[1],
    'owner', ARGV[2])
  redis.call('PEXPIRE', KEYS[1], ARGV[3])
  return {'ACQUIRED', ''}
end

if redis.call('HGET', KEYS[1], 'payload_hash') ~= ARGV[1] then
  return {'MISMATCH', ''}
end

if state == 'IN_PROGRESS' then
  return {'IN_PROGRESS', ''}
end

return {'COMPLETED', redis.call('HGET', KEYS[1], 'response')}
```

A verificação de `MISMATCH` vem **antes** da de estado: payload divergente é erro do cliente independentemente de a operação anterior ter concluído ou não.

##### Script 2 — `complete`

`ARGV[1]` owner token · `ARGV[2]` resposta serializada · `ARGV[3]` TTL de negócio (ms)

```lua
if redis.call('HGET', KEYS[1], 'owner') ~= ARGV[1] then
  return 0
end

redis.call('HSET', KEYS[1], 'state', 'COMPLETED', 'response', ARGV[2])
redis.call('HDEL', KEYS[1], 'owner')
redis.call('PEXPIRE', KEYS[1], ARGV[3])
return 1
```

Repare na troca de TTL: o lease curto (~30 s) dá lugar ao TTL de negócio (~24 h) só no momento da conclusão.

##### Script 3 — `release`

Usado quando o método lança e a política é `RELEASE`.

```lua
if redis.call('HGET', KEYS[1], 'owner') == ARGV[1] then
  return redis.call('DEL', KEYS[1])
end
return 0
```

**Este é o script inegociável.** Sem ele, um processo cujo lease expirou apaga a chave de quem assumiu depois — e duas execuções concorrentes passam.

##### Retorno `0` nos scripts 2 e 3

Significa que o lease foi perdido: o método demorou mais que o TTL de lease e outro processo assumiu. Não é erro fatal, mas é sinal de que o lease está curto demais para aquele método.

→ Métrica `idempotency.lease_lost`, com tag `cachePrefix`. Se ela subir, aumente o TTL de lease daquele método.

##### Notas de implementação

- **Sem `nil` em tabelas Lua.** Uma tabela com `nil` no meio é truncada na conversão para o protocolo Redis. Por isso os scripts devolvem `''` e não `nil`.
- **`HGET` devolve `false`**, não `nil`, quando o campo não existe. É o teste usado para "chave inexistente".
- **Determinismo.** Nada de `TIME`, `RANDOMKEY` ou geração de token dentro do script — o owner token e os TTLs vêm de Java via `ARGV`.
- **Chave única por script.** Compatível com Redis Cluster sem problema de cross-slot, caso o SCOS venha a shardar.
- **Carregamento.** `DefaultRedisScript` + `ResourceScriptSource` apontando para arquivos `.lua` em `resources/scripts/`. Manter os scripts em arquivo, não em `String` no código — versionáveis, revisáveis e testáveis isoladamente.
- **`EVALSHA`.** O Spring já envia `EVALSHA` e refaz com `EVAL` no `NOSCRIPT`, então um restart do Redis ou um `SCRIPT FLUSH` se resolve sozinho.


#### 2.3 Política de falha

Hoje qualquer exceção remove a chave. Precisa ser declarável, porque a decisão certa depende do domínio:

- `RELEASE` — remove a chave, permite retry. Correto quando a exceção ocorreu antes de qualquer efeito colateral.
- `KEEP_FAILED` — grava o erro como resultado. Correto quando o efeito já aconteceu e o retry duplicaria.

Default `RELEASE` (comportamento atual), configurável por método.

#### 2.4 Redis indisponível — modo degradado

**Premissa fixa do projeto:** Redis fora → a aplicação se comporta como se a idempotência não estivesse habilitada. Fail-open. Não há modo `FAIL_CLOSED`; a idempotência nunca derruba uma requisição de negócio.

O comportamento final é o mesmo de hoje. O que muda é que ele deixa de ser um acidente por chamada e passa a ser um **estado declarado do módulo**:

| Hoje | Alvo |
|---|---|
| `catch → return false`, por chamada | Estado de degradação explícito |
| Log em `ERROR`, só `getMessage()` | `WARN` na transição, não a cada requisição |
| Invisível em métrica | Gauge `idempotency.degraded` (0/1) + counter de transições |
| Sem sinal externo | `HealthIndicator` reportando `DEGRADED`, não `DOWN` |
| Cada chamada paga o timeout | Circuit breaker: degradação **rápida** |

##### O ponto crítico: degradar rápido

Fail-open só cumpre a premissa se for barato. Hoje a configuração é `commandTimeout(5s)` e `connectTimeout(3s)`. Com o Redis fora e fail-open ingênuo, **cada requisição paga até 5 s antes de seguir**. A aplicação não se comporta "como se estivesse desabilitada" — ela se comporta como se estivesse caindo, e o pool de threads satura antes de qualquer alerta disparar.

Então a premissa *exige* um circuit breaker no acesso ao Redis:

- **Fechado** — operação normal.
- **Aberto** — após N falhas consecutivas, as chamadas ao Redis são puladas **sem I/O**. Custo por requisição ≈ zero. É aqui que o módulo realmente "não está habilitado".
- **Meio-aberto** — sondagem periódica; ao voltar, fecha e sai do modo degradado.

Timeout de comando reduzido (~200 ms) para que o próprio período de detecção não machuque. `DisconnectedBehavior.REJECT_COMMANDS`, que já está configurado, ajuda: rejeita imediatamente em vez de enfileirar.

##### Circuit breaker — decisões

**Antes do breaker, resolver o timeout — sem fixar número.** O `commandTimeout(5s)` de hoje está **hardcoded** na `ScosJdempotentRedisConfiguration`, ignorando o `spring.data.redis.timeout` que a própria aplicação configurou. Esse é o defeito real: a biblioteca sobrepõe a decisão do consumidor.

Como esta é uma lib Spring usada por aplicações diferentes, **não se pode assumir onde o Redis está**. Pode ser Sentinel na mesma rede, pode ser um serviço gerenciado em outra região, pode ser Redis compartilhado entre sistemas. Qualquer default numérico que a foundation escolha estará errado para alguém: baixo demais derruba um consumidor com Redis remoto, alto demais deixa o outro pagando latência.

Regra adotada:

1. **Respeitar `spring.data.redis.timeout`** quando a aplicação o definir. A lib deixa de sobrepor.
2. **`scos.jdempotent.cache.redis.command-timeout`** como override opcional, para quem quiser um limite mais agressivo só na idempotência do que no resto do uso de Redis.
3. **Sem default embutido na lib** além do que o Spring Boot já traz. Documentar a recomendação (algo entre 2× e 5× o p99 medido do ambiente), não codificá-la.

##### Detecção por lentidão, não só por falha

Como a latência aceitável é desconhecida em tempo de biblioteca, o breaker não pode depender só de `failure-rate`. O Resilience4j oferece detecção de chamada lenta, que é o mecanismo certo aqui: em vez de a lib decidir "200 ms é o limite", cada aplicação declara o seu, e o breaker abre quando uma fração das chamadas ultrapassa esse limite — mesmo que nenhuma chegue a estourar timeout.

Isso cobre exatamente o caso perigoso que motivou o breaker (Redis vivo porém lento) sem exigir que a foundation chute um número.

##### Redis possivelmente compartilhado

A mesma restrição implica que o Redis pode não ser exclusivo desta aplicação. Duas consequências:

- **Namespace de chave obrigatório e configurável.** Hoje o `DefaultKeyGenerator` lê o nome da aplicação de `System.getenv(APP_NAME)` — variável de ambiente, fora do modelo de configuração do Spring, e silenciosamente vazia quando ausente. Passa a ser propriedade Spring com valor obrigatório, e o prefixo entra em toda chave.
- **Não usar `SCRIPT FLUSH`, `FLUSHDB` nem `KEYS`** em nenhum ponto do módulo, incluindo testes que rodem contra ambiente compartilhado.


**Lettuce já cobre metade do problema.** `DisconnectedBehavior.REJECT_COMMANDS`, que já está configurado, rejeita comandos imediatamente quando a conexão está reconhecidamente caída. O breaker existe para o caso perigoso, que o Lettuce não cobre: **Redis vivo mas lento** — pausa de GC, sobrecarga, rede engolindo pacotes. Aí cada chamada paga o timeout inteiro e nada avisa.

**Implementação: Spring Cloud CircuitBreaker sobre Resilience4j.**

Registro honesto do que isso significa: **não existe circuit breaker nativo do Spring**. `spring-cloud-circuitbreaker` é uma abstração (`CircuitBreakerFactory`); a implementação de referência é o Resilience4j. Escolher "o jeito Spring" traz o Resilience4j junto, com uma camada de abstração por cima.

Compatibilidade verificada: o release train Spring Cloud 2025.1.2 (Oakwood) cobre Spring Boot 4.1. É preciso importar o BOM `spring-cloud-dependencies` no `scos-bom` para alinhar as versões — não fixar versão de artefato individual.

**Por que compensa aqui, apesar do peso:** o `resilience4j-micrometer` publica automaticamente `resilience4j.circuitbreaker.state`, `.calls` e `.failure.rate`. As duas métricas que a premissa de fail-open exige (estado degradado e transições) saem de graça, instrumentadas pela própria biblioteca em vez de por código nosso. Dado que a validação do modo degradado é feita por métrica, isso elimina justamente a parte mais fácil de errar.

**Impacto nas outras fases:** a Fase 5 previa remover o `spring-cloud-starter` do módulo. Isso fica cancelado — vira **trocar** `spring-cloud-starter` por `spring-cloud-starter-circuitbreaker-resilience4j`, marcado `<optional>true</optional>` para que o consumidor não herde a dependência. Como efeito colateral, o `@RefreshScope` continua viável.

**Máquina de estados:**

| Estado | Comportamento |
|---|---|
| Fechado | Executa normalmente |
| Aberto | Retorna degradado **sem I/O** — a chamada nem chega ao Lettuce |
| Meio-aberto | Um número limitado de chamadas reais passa como sondagem |

**Trade-off aceito conscientemente:** o desenho anterior previa sondagem `PING` em background, para que nenhuma requisição de usuário pagasse o custo da reconexão. O Resilience4j não funciona assim — no meio-aberto, requisições reais servem de sondagem. Com `permitted-number-of-calls-in-half-open-state: 2`, o custo fica limitado a duas chamadas por janela, cada uma limitada pelo timeout que a aplicação configurou. Como esse timeout é decisão do consumidor, o orçamento do modo degradado também é — entra na documentação do módulo como algo a dimensionar, não como número dado.

**Uso programático, não por anotação.** O módulo já *é* um aspecto; empilhar o `@CircuitBreaker` anotado cria dependência de ordem entre advices. Injetar `CircuitBreakerFactory` e envolver a chamada no `RedisIdempotentRepository` é mais previsível e mais testável.

**Sem `TimeLimiter`.** Ele exige pool de threads próprio e mudaria o modelo de execução do aspecto. O limite de tempo já vem do `commandTimeout` do Lettuce.

**O que conta como falha** vira configuração declarativa, o que é uma vantagem sobre implementação própria:

- `record-exceptions` — `RedisConnectionFailureException`, `QueryTimeoutException`.
- `ignore-exceptions` — erro de script Lua e de serialização. São bugs de código: precisam falhar alto, não abrir o breaker mascarados como degradação de infra.

**Escopo:** um breaker nomeado por instância de `RedisIdempotentRepository`. Decisão local por pod, não distribuída — correto para este caso.

**Interação com o lease:** se o breaker abrir entre o `tryAcquire` e a conclusão, sobra uma chave `IN_PROGRESS` órfã no Redis. O TTL curto de lease já resolve sozinho — é exatamente por isso que ele é separado do TTL de negócio. Não exige tratamento, mas precisa estar documentado.

**Configuração** — valores de partida a serem dimensionados por ambiente, não defaults da biblioteca:

```yaml
resilience4j.circuitbreaker.instances.jdempotent-redis:
  sliding-window-type: COUNT_BASED
  sliding-window-size: 20
  failure-rate-threshold: 50
  slow-call-duration-threshold: 200ms    # dimensionar pelo p99 do ambiente
  slow-call-rate-threshold: 50
  wait-duration-in-open-state: 10s
  permitted-number-of-calls-in-half-open-state: 2
  automatic-transition-from-open-to-half-open-enabled: true
  record-exceptions:
    - org.springframework.data.redis.RedisConnectionFailureException
    - org.springframework.dao.QueryTimeoutException
```

O timeout de comando **não** é definido aqui: vem de `spring.data.redis.timeout` da aplicação, com `scos.jdempotent.cache.redis.command-timeout` como override opcional.

##### Consequência que precisa estar registrada

Com fail-open como premissa, **a constraint `UNIQUE` no banco deixa de ser recomendação e vira obrigação** para todo método cuja chave seja natural (`login`, `tenant + cpf`). Durante a degradação o Redis não segura nada — o banco é a única barreira restante contra duplicata.

Isso vai no README do módulo e no checklist de revisão de qualquer endpoint que use `@JdempotentResource` com chave natural.

##### O que continua sendo decisão do consumidor

A premissa fixa é sobre **indisponibilidade da infraestrutura**. Ela não se aplica a `PAYLOAD_MISMATCH` nem a `IN_PROGRESS`, que são respostas a requisições malformadas ou concorrentes, com o Redis saudável. Esses continuam retornando `422` e `409`.

#### 2.5 Ordem em relação à transação

`@Order` explícito no aspecto, garantindo que ele rode **fora** de `@Transactional`. Sem isso, um rollback deixa a chave órfã no Redis sem resposta, e todos os retries legítimos batem em `IN_PROGRESS` até o lease expirar.

#### 2.6 Conexão Redis delegada ao Spring Boot

**Decisão:** o módulo deixa de construir a própria `LettuceConnectionFactory` e passa a injetar a que o Spring Boot já cria.

##### O que sai

A `ScosJdempotentRedisConfiguration` monta hoje um `RedisSentinelConfiguration` a partir de `redisProperties.getSentinel().getMaster()`. Em qualquer aplicação que use Redis standalone ou cluster, `getSentinel()` é `null` e o startup morre com NPE — a topologia do ambiente de origem virou requisito obrigatório para todos os consumidores.

Some junto: parsing manual dos nós Sentinel, montagem de `LettuceClientConfiguration`, `commandTimeout` fixo em 5 s, `setValidateConnection(false)`, `setShareNativeConnection(true)`. Cerca de 50 linhas.

##### O que entra

```java
@Bean("JdempotentRedisTemplate")
@ConditionalOnBean(RedisConnectionFactory.class)
public RedisTemplate<String, String> jdempotentRedisTemplate(RedisConnectionFactory factory) { ... }
```

O `RedisAutoConfiguration` do Boot já resolve standalone, sentinel e cluster conforme `spring.data.redis.*`, e já aplica `spring.data.redis.timeout`. O módulo não precisa saber qual é o caso.

##### Consequências

| Ponto | Efeito |
|---|---|
| Topologia | Deixa de ser problema — não é mais decisão do módulo |
| Timeout | Vem da aplicação, resolvendo o item da seção 2.4 na origem |
| Pool | Compartilhado com o resto do uso de Redis da aplicação |
| `ScosJdempotentRedisProperties` | Perde as 4 propriedades de timeout, que já eram mortas |
| Serializers | O template do Lua é `<String, String>`; o `PolymorphicRedisSerializer` sai deste caminho |
| `@ConditionalOnBean` | Se a aplicação não tem Redis configurado, o módulo simplesmente não se ativa — em vez de quebrar o startup |

##### Trade-off registrado

Perde-se o isolamento de pool. Foi uma boa decisão no módulo `audit` (DataSource dedicado), mas o caso é diferente: lá são escritas em lote, longas, com perfil de carga próprio. Aqui as chamadas são curtas, o volume acompanha o tráfego HTTP e já existe circuit breaker protegendo o caminho.

Se algum consumidor precisar de isolamento no futuro, a saída é declarar o próprio `RedisConnectionFactory` qualificado — sem que a foundation precise voltar a opinar sobre topologia.

##### Cuidado no teste

Com pool compartilhado, derrubar o Redis nos testes da Fase 0 afeta também qualquer outro uso de Redis da aplicação de teste. Manter a aplicação de teste enxuta, ou usar um `RedisConnectionFactory` dedicado apenas no contexto de teste.



O módulo não tem uma métrica sequer, enquanto o `audit` tem gauge, counter e summary. Adicionar:

##### Como integrar sem impor a dependência

Hoje o `audit` importa `io.micrometer.*` em tempo de compilação, mas **o Micrometer não está declarado em nenhum pom** — chega transitivamente por `utils → spring-boot-starter-actuator`. No dia em que a Fase 5 quebrar o `utils` em módulos menores, o `audit` para de compilar. Não repetir esse erro aqui.

1. Declarar `micrometer-core` explicitamente como `<optional>true</optional>` — o consumidor não herda a dependência.
2. Definir uma interface própria `IdempotencyMetrics` no `core`, com implementação no-op e implementação sobre Micrometer. O caminho quente do aspecto nunca referencia classes do Micrometer diretamente, então a ausência do jar não causa `NoClassDefFoundError`.
3. Registrar a implementação Micrometer sob `@ConditionalOnClass(MeterRegistry.class)`.

Sobre a resolução do registry: o `audit` usa `ObjectProvider<MeterRegistry>` + `getIfAvailable()`, o que espalha `if (counter != null)` pelo código. Preferir `getIfAvailable(CompositeMeterRegistry::new)` — um composite sem filhos descarta os registros e elimina todas as verificações de nulo.

#### 2.7 Observabilidade

##### Métricas

`idempotency.acquired` · `idempotency.hit` · `idempotency.in_progress` · `idempotency.mismatch` · `idempotency.backend_error` — todos com tag `cachePrefix`.

Mais duas, exigidas pela premissa de fail-open: `idempotency.degraded` (gauge 0/1) e `idempotency.degraded.transitions` (counter). Com a adoção do Resilience4j, ambas podem ser derivadas de `resilience4j.circuitbreaker.state{name=jdempotent-redis}` em vez de instrumentadas à mão — o binder do Resilience4j já as publica. Manter os nomes próprios só se o painel exigir vocabulário de domínio.

Sem taxa de hit, não há como saber se a idempotência está funcionando ou apenas não colidindo. E sem o gauge de degradação, um fail-open silencioso é indistinguível de um sistema saudável — que é exatamente a falha do código atual.

**Alerta obrigatório:** `idempotency.degraded == 1` por mais de X minutos. A premissa diz que a aplicação não quebra; não diz que ninguém precisa ficar sabendo.

##### As métricas como instrumento de teste

Isto muda a Fase 0: com `SimpleMeterRegistry` nos testes, os critérios deixam de ser "parece funcionar" e viram asserção numérica.

| Cenário | Asserção |
|---|---|
| N threads na mesma chave | `acquired == 1` e `in_progress == N-1` |
| Redis derrubado no meio | gauge `degraded` vai a 1; `transitions == 1` |
| Requisições durante degradação | executam normalmente; `backend_error` para de crescer depois que o breaker abre — prova que não há mais I/O |
| Redis restabelecido | gauge volta a 0 dentro do `probe-interval`, sem nenhuma requisição ter pago o custo da reconexão |
| Mesma chave, corpo diferente | `mismatch == 1`, método não executado |

O terceiro caso é o que realmente valida o breaker: se `backend_error` continuar subindo com o Redis fora, o breaker não abriu e cada requisição ainda está pagando timeout.

**Em produção**, a mesma instrumentação denuncia o risco alto da seção 6: taxa de `hit` próxima de zero num endpoint com retry ativo significa chave sendo gerada dentro do laço de retry do cliente.

**Critério de aceite:** teste 0.1 (concorrência) passa. Teste 0.4 confirma que, com o Redis derrubado, o método executa normalmente, a latência adicional fica dentro do orçamento do circuit breaker e o gauge de degradação sobe.

---

### Fase 3 — Chave composta declarativa
**2 dias**

Hoje a chave sai do `toString()` do request inteiro. Dois defeitos:

- Campo alterado que não faz parte da identidade (telefone, na criação de usuário) gera chave nova → duplicata criada.
- Valor de campo cujo tipo não sobrescreve `toString()` sai como `Bar@1b6d3586` (identity hash) → **chave diferente a cada chamada, idempotência silenciosamente desativada**.

#### Entregáveis

1. **`IdempotencyKeyResolver`** — classe nova no `core`, isola "de onde vem a chave" do aspecto. É o ponto de extensão da Fase 4.
2. **`@JdempotentProperty` vira seletor**, não filtro. Os campos anotados *compõem* a chave; os demais ficam de fora. O vocabulário de anotações já existe e está com `@Target(FIELD)` correto — falta o aspecto tratá-lo assim.
3. **Serialização canônica** — `TreeMap` com ordenação explícita e representação estável por tipo, no lugar de `toString()` de `HashMap`.
4. **`@JdempotentId` sai da composição da chave.** Hoje ele cai no `DefaultChain` e entra no hash; depois o aspecto preenche o campo por reflection. Se o cliente reenviar o payload já com o id, a chave muda e o retry vira execução nova.

**Exemplo do alvo:**

```java
record CreateUserRequest(
    @JdempotentProperty("tenant") String tenantId,   // compõe a chave
    @JdempotentProperty("login")  String login,      // compõe a chave
    @JdempotentIgnore             String nome,
    @JdempotentIgnore             String telefone
) {}
```

**Compatibilidade:** `keySource` default `PAYLOAD_HASH` mantém o comportamento atual. Quem quiser chave composta declara `keySource = FIELDS`.

---

### Fase 4 — Header de idempotência
**2 dias**

#### 4.1 Anotação

```java
@JdempotentResource(
    cachePrefix = "user-create",
    keySource   = KeySource.HEADER_THEN_FIELDS,
    headerName  = "Idempotency-Key",
    onMismatch  = MismatchPolicy.CONFLICT,
    ttl = 24, ttlTimeUnit = HOURS
)
```

Atributos novos, todos com default → nenhum método existente muda de comportamento.

Aproveitar para adicionar `@Target(ElementType.METHOD)` na anotação, que hoje não tem — pode ser posta em qualquer lugar sem efeito.

#### 4.2 Precedência de resolução

1. **Header** presente → é a chave. Autoridade do cliente.
2. **Campos compostos** → chave natural.
3. **Hash do payload** → último recurso, desligado por padrão nos métodos novos.

#### 4.3 Cuidado central: o módulo não é só HTTP

O aspecto chama o `cachePrefix` de *listener name* — foi desenhado para consumidores de mensageria, que não têm `HttpServletRequest`. O resolver **precisa devolver `null` limpo** quando não há contexto web e cair para o próximo nível, nunca estourar.

Correlato: `RequestContextHolder` é `ThreadLocal`. Se o método rodar em `@Async` ou numa thread lançada manualmente, o header some. Extrair o valor num filtro e propagar explicitamente — mesmo padrão que o `audit` já usa para `X-Request-ID` e IP.

#### 4.4 Não usar `X-Request-ID`

O `LoggingInitialFilter` do `utils` já coloca `X-Request-ID` no MDC, e é tentador reaproveitar. **Não serve.** `X-Request-ID` é por *tentativa* — gateways e clients emitem um novo a cada retry, e o próprio filtro gera um UUID quando o header vem vazio. Usá-lo como chave inverteria a idempotência: cada retry viraria operação nova.

`Idempotency-Key` é outro contrato — estável entre tentativas da mesma operação lógica. Headers distintos, campos distintos.

#### 4.5 Detecção de payload divergente

Guardar `payloadHash` no valor e comparar na entrada. Sem isso: cliente envia `{login: "saw", role: "USER"}`, erra, reenvia `{login: "saw", role: "ADMIN"}` com a mesma chave e recebe `200` com a resposta do primeiro — achando que virou admin.

O `IdempotentRequestResponseWrapper` já existe e está subutilizado; é onde o campo entra.

#### 4.6 Fora do código

- Liberar o header em `Access-Control-Allow-Headers` e conferir a passagem por qualquer proxy no caminho.
- Documentar por endpoint: header aceito, obrigatório ou não, e a resposta em caso de reuso com corpo diferente.
- **Alinhar com os times consumidores** que a chave deve ser gerada **antes** do laço de retry (Feign `Retryer`, resilience4j, retry do gateway). Gerar dentro do laço produz chave nova a cada tentativa — parece funcionar em teste e falha em produção sob timeout. É o erro mais comum desse padrão.

---

### Fase 5 — Endurecimento
**2 dias · pode ir em paralelo à 3 e 4**

| Item | Problema hoje |
|---|---|
| `@ConfigurationProperties` no lugar de `@Value` | 5 propriedades sem default + módulo ligado por `matchIfMissing = true` → o jar no classpath derruba o startup de quem não configurou |
| Remover propriedades mortas | `dialTimeoutSecond`, `readTimeoutSecond`, `writeTimeoutSecond`, `maxRetryCount` nunca são lidas; os timeouts reais estão hardcoded |
| `EnvironmentPostProcessor` → `spring.factories` | Está em `AutoConfiguration.imports`; EPPs rodam antes do contexto, então ele **nunca executa** e o `spring.data.redis.repositories.enabled=false` não tem efeito |
| ~~Sentinel opcional~~ | Resolvido na seção 2.6: ao delegar a conexão ao Boot, a topologia deixa de ser decisão do módulo |
| Allowlist no `PolymorphicRedisSerializer` | `Class.forName` sobre valor vindo do Redis, sem validação de tipo — agora guardando respostas de negócio |
| TTL no `InMemoryIdempotentRepository` | Ignora TTL por completo; é o default em 4 dos 7 construtores |
| `equals`/`hashCode` do `IdempotentRequestWrapper` | Compara elementos com o objeto inteiro; não é reflexivo nem simétrico. Foi silenciado com `@SuppressFBWarnings` — o SpotBugs estava certo |
| Construtores → builder | 7 construtores telescópicos, campo `idempotentRepository` com `@Setter` público, não-final, não-`volatile`, num singleton compartilhado |
| Trocar `spring-cloud-starter` | Substituir por `spring-cloud-starter-circuitbreaker-resilience4j`, marcado `optional`. Substituiu o item anterior ("remover spring-cloud"), cancelado pela decisão da seção 2.4 |
| README do módulo | `audit` e `privacy` têm; este não |

---

## 4. Cronograma

```
Fase 0  ██                    dias 1-2     rede de proteção
Fase 1    ██                  dias 3-4     chaves
Fase 2      ████              dias 5-8     concorrência
Fase 3          ██            dias 9-10    chave composta
Fase 4            ██          dias 11-12   header
Fase 5          ████          dias 9-12    endurecimento (paralelo)
```

**Ponto de corte útil:** Fases 0–2 já entregam um módulo correto. Se o prazo apertar, 3–5 podem sair depois — mas ver a ressalva na seção 5 antes de adiar as que quebram contrato.

---

## 5. Versão e ordem de execução

**Tudo entra na própria `1.2.0`, que ainda é SNAPSHOT.** Nada foi publicado sob essa versão e compatibilidade com consumidores não é restrição neste ciclo.

Isso remove do plano:

- A flag `legacy-in-progress-behavior` — o comportamento correto (`409` em vez de `null`) entra direto, sem via de escape.
- O default `keySource: PAYLOAD_HASH` "por compatibilidade" — o default passa a ser o desenho correto (`HEADER_THEN_FIELDS`), com o hash de payload disponível apenas para quem pedir explicitamente.
- A migração gradual das propriedades: `@Value` sai e `@ConfigurationProperties` entra de uma vez, com os nomes definitivos e as propriedades mortas removidas sem período de depreciação.

### Ressalva de janela

A liberdade acaba no momento em que `1.2.0` for publicada. Toda quebra de contrato prevista aqui deve caber neste ciclo — em particular os nomes de propriedade e os atributos de `@JdempotentResource`. Se as Fases 3–5 forem adiadas para depois da publicação, elas deixam de ser mudanças livres e voltam a exigir depreciação.

Recomendação: se houver corte de escopo, cortar **volume de trabalho**, não **quebras de contrato**. Fechar o formato de configuração e das anotações agora, mesmo que a implementação por trás venha em seguida.

### Ordem em relação ao plano do `utils`

Os dois planos disputam o mesmo território: as anotações `@Jdempotent*` moram hoje no `utils` e mudam de casa na decomposição.

**Executar primeiro a Fase 2 do plano de decomposição** (devolver as anotações aos módulos donos). Só então mexer nas anotações aqui. Fazer na ordem inversa significa mover as mesmas classes duas vezes e resolver conflito de merge em cima de arquivo renomeado.

## 6. Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Script Lua com bug de estado | Média | Testes de concorrência da Fase 0 rodando contra Testcontainers Redis real, não mock |
| Cliente gera chave dentro do laço de retry | **Alta** | Documentação + revisão da primeira integração; métrica de hit próxima de zero denuncia |
| Fail-open lento: Redis fora somando 5 s por requisição e saturando o pool | **Alta** | Circuit breaker + timeout de comando reduzido; teste 0.4 mede a latência em modo degradado |
| Degradação passa despercebida por dias | Média | Gauge `idempotency.degraded` + alerta; `HealthIndicator` em `DEGRADED` |
| Duplicata criada durante degradação | Média | Constraint `UNIQUE` obrigatória — é o único controle compensatório sob fail-open |
| Header perdido em `@Async` | Média | Extração no filtro, teste explícito com executor |
| Lease expira antes do método terminar | Média | TTL de lease configurável por método; monitorar `idempotency.in_progress` |
| Escopo cresce para o `utils` | Média | As anotações moram no `utils`; alterá-las obriga rebuild de todos os consumidores. Limitar a atributos aditivos nesta versão |

---

## 7. Sequência de commits sugerida

Um por linha, cada um verde no CI:

1. `test: reproduzir colisão de chave, corrida e duplo aspecto`
2. `fix: hex com zero-padding na geração da chave`
3. `fix: SHA-256 configurável no lugar de MD5 fixo`
4. `fix: registrar um único bean IdempotentAspect`
5. `refactor: delegar a conexão Redis ao RedisConnectionFactory do Boot`
6. `refactor: extrair contrato Lease do IdempotentRepository`
7. `feat: aquisição atômica e conclusão via scripts Lua`
8. `feat: política de falha por exceção de negócio (RELEASE/KEEP_FAILED)`
9. `feat: modo degradado com circuit breaker no acesso ao Redis`
10. `feat: métricas Micrometer, gauge de degradação e HealthIndicator`
11. `feat: IdempotencyKeyResolver com chave composta declarativa`
12. `feat: resolução de chave por header Idempotency-Key`
13. `fix: migrar properties para @ConfigurationProperties com defaults`
14. `docs: README do módulo com o modelo de garantia e o modo degradado`
