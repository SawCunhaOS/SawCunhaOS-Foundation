# scos-foundation-jdempotent

Módulo de idempotência para métodos anotados com `@JdempotentResource` (contrato em
`scos-foundation-jdempotent-api`). `IdempotentAspect` intercepta a chamada, adquire um lock atômico
(`tryAcquire`) num repositório (Redis ou in-memory) e, numa chamada repetida com a mesma chave,
devolve a resposta cacheada em vez de reexecutar o método protegido.

## Como configurar

O caminho normal de uso é a auto-configuração Spring Boot (`ScosJdempotentConfig`, ativada por
`scos.jdempotent.enabled=true`, padrão) — ver o exemplo completo de `application.yml` e anotações
no README da raiz do repositório. Nenhuma instanciação manual é necessária nesse caminho.

Para montar um `IdempotentAspect` fora do Spring (testes, uso programático), use o builder — não há
mais construtores públicos (Story 3.17: os 7 construtores telescópicos anteriores, combinando
`IdempotentRepository`/`ErrorConditionalCallback`/`DefaultKeyGenerator` em todas as combinações,
foram substituídos por esta API fluente):

```java
IdempotentAspect aspect = IdempotentAspect.builder()
        .repository(new RedisIdempotentRepository(redisTemplate, redisProperties, metrics)) // opcional, padrão: InMemoryIdempotentRepository
        .errorCallback(myErrorCallback)                                                     // opcional, padrão: nenhum
        .keyGenerator(new DefaultKeyGenerator(namespace))                                    // opcional, padrão: DefaultKeyGenerator sem namespace
        .build();
```

## Cache é fast-path, não garantia

O Redis (ou o repositório in-memory) usado pelo `jdempotent` é um mecanismo de **fast-path para
evitar reprocessamento**, não a garantia final contra duplicidade. Ele funciona bem no caminho
feliz, mas falha de forma aberta (fail-open, Story 3.7): se o Redis está indisponível ou lento, o
circuit breaker interrompe rápido e a requisição de negócio **prossegue sem o lock de idempotência**
— nunca `FAIL_CLOSED`.

Isso cria um cenário de split-brain concreto e já catalogado (Story 3.7, AC #3): o lock é adquirido
com sucesso (Redis up), o Redis cai durante o processamento do método protegido, `setResponse`
falha silenciosamente — a chamada ainda retorna sucesso ao cliente, mas a resposta **não fica
cacheada**, e um retry subsequente do cliente **reexecuta o método de negócio** do zero.

A garantia real contra duplicidade nesse cenário de falha (e, na prática, em qualquer cenário) é a
constraint `UNIQUE` do banco de dados sobre a chave de negócio (NFR6) — **não** o cache de
idempotência. Todo método com chave de idempotência natural (ex.: `transactionId`, número de
pedido) deve ter essa constraint `UNIQUE` no banco **antes** de habilitar `@JdempotentResource`
sobre ele. Sem ela, o cenário de split-brain acima produz um registro duplicado de verdade — o
`jdempotent` reduz a frequência com que isso acontece, não a elimina.

## Relação entre os três timeouts

O módulo depende de três timeouts independentes, configurados em lugares diferentes:

1. **`ttl` do lease de idempotência** (`@JdempotentResource(ttl = ...)`, Story 3.5) — por quanto
   tempo uma chave fica marcada como "em processamento".
2. **`slow-call-duration-threshold` do circuit breaker** (Story 3.7) — a partir de quanto tempo uma
   chamada ao Redis é considerada lenta e conta para abrir o breaker.
3. **`spring.data.redis.timeout`** — o timeout de rede do próprio cliente Redis (Lettuce).

**Invariante obrigatória de ordem relativa**, cada um com margem sobre o anterior:

```
ttl do lease  >  slow-call-duration-threshold  >  spring.data.redis.timeout
```

Por que essa ordem:

- Se `spring.data.redis.timeout` for **maior ou igual** ao `slow-call-duration-threshold`, o
  circuit breaker nunca detecta lentidão antes do timeout de rede já ter estourado — o breaker
  perde o propósito de fail-fast, porque a chamada trava no timeout de rede primeiro.
- Se o `ttl` do lease for **menor ou igual** ao `slow-call-duration-threshold` (combinado com o
  tempo de processamento real do método protegido), o lease pode expirar antes do método protegido
  terminar — uma segunda chamada concorrente adquire um novo lease sobre a mesma chave enquanto a
  primeira ainda está em andamento (risco já catalogado e aceito na Story 3.5, AC #4; a métrica
  `idempotency.in_progress` é o sinal de detecção em produção). Manter o `ttl` acima dos outros
  dois, com margem, minimiza — não elimina — essa janela.

O **dimensionamento exato** de cada valor (quantos milissegundos/segundos) é responsabilidade do
time consumidor: depende do perfil de latência de cada aplicação e do ambiente de execução (OQ-3 do
PRD — nenhum valor é fixado por este módulo). Este README documenta só a ordem relativa
obrigatória entre os três, não valores numéricos prescritivos — dimensionar sem respeitar essa
ordem deixa ambíguo qual dos três timeouts é o dono real do lock quando um expira antes do outro.
