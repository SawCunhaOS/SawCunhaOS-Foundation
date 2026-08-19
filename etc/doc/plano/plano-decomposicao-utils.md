# Plano de Decomposição — `scos-foundation-utils`

**Origem / Alvo:** `1.2.0-SNAPSHOT` (mesma versão — nada foi publicado ainda) · **Esforço estimado:** 7–9 dias úteis

---

## 1. Diagnóstico

| Métrica | Valor |
|---|---|
| Classes em `src/main` | 71 |
| Pacotes de primeiro nível | 18 |
| Responsabilidades distintas | REST, cache/Redis, JPA, validação BR, Feign, Liquibase, serialização, exceções, value objects |
| Dependências arrastadas | web, data-jpa, data-redis, cache, cloud, actuator, validation, feign, querydsl, guava, bouncycastle, commons-io, stella, jackson, gson, snakeyaml, mapstruct |

Quem importa `scos-foundation-utils` para usar `DateUtils` herda todo esse conjunto. É o que trava a evolução dos outros quatro módulos: `jdempotent` depende de JPA sem motivo, `exception` depende de `utils` embora `utils` contenha a exceção base.

---

## 2. A descoberta que torna isso barato

Mapeei o que os outros módulos **de fato** consomem de `utils`. A superfície é minúscula — 12 tipos:

| Consumidor | Usa de `utils` |
|---|---|
| `privacy` | **nada** |
| `exception` | `ScosException`, `ScosExceptionCode`, `ExceptionCode`, `LocaleService` |
| `audit` | os 4 acima + `ScosUserAuthentication`, `GsonUtils`, `BaseLiquibaseProperties`, `JacksonCustomJsonFormatMapper`, `Auditable`, `AuditAction` |
| `jdempotent` | as 5 anotações `@Jdempotent*` + `PolymorphicRedisSerializer` |

Duas conclusões:

1. **As anotações `@Auditable`/`@Jdempotent*` só são usadas pelos próprios módulos donos.** Elas estão em `utils` sem nenhuma razão de compartilhamento — devolvê-las aos donos elimina metade do acoplamento sem discussão.
2. **O que sobra de compartilhado cabe num módulo pequeno.** Tirando as anotações, restam ~8 tipos: exceção base, contratos de serviço e alguns utilitários.

Ou seja: depois da separação, o `utils` atual vira uma **folha do grafo** — nenhum outro módulo da foundation depende dele. Isso significa que o risco da refatoração está quase todo fora do repositório, nos consumidores externos.

---

## 3. Módulos-alvo

### `scos-foundation-core`
Contratos e utilitários puros. **Regra: nenhuma dependência de Spring.**

- `exception/ScosException`
- `specification/` — `ExceptionCode`, `LocaleService`, `ScosBaseUseCase`, `ScosStartupListener`, `ScosUserAuthentication`
- `enums/` — `ScosExceptionCode`, `ValueType`, `SpecificationFunction`, `StringTransformRule`
- `utils/` — `DateUtils`, `HashUtils`, `StringFieldUtils`
- `sort/PropertiesOrder`

Deps: `slf4j-api`, `lombok` (optional), `commons-lang3`.

Nota: `ScosUserAuthentication` é uma interface de um método, sem dependência alguma — é o exemplo do que deveria estar num core desde o início.

### `scos-foundation-spring`
Peças transversais que precisam do Spring, mas **não** de web, JPA ou cache. Existe para que o `core` continue livre de Spring — sem ele, essas classes forçariam a quebra dessa regra.

- `annotation/rules/ScosRule`, `annotation/rules/ScosRuleService` — estereótipos (`@Component` + `@Order`) para as aplicações
- `annotation/normalizestrings/NormalizeStrings` + `aspect/StringProcessingAspect`
- `listener/ScosOnStartupListener` (a interface `ScosStartupListener` fica no `core`)

Deps: `scos-foundation-core`, `spring-context`, `spring-aop`.

### `scos-foundation-web`
- `annotation/ScosController`, `annotation/request/*`
- `configuration/rest/*` — `ScosJacksonConfig`, filtros de logging, `MultiReadHttpServletRequest`
- `dto/request/ScosPaginationFilterDTO`, `dto/response/*`
- `utils/` — `IpAddressExtractor`, `ScosResponseUtils`, `PaginationUtils`, `JacksonXmlUtils`
- `configuration/feign/*` (ver decisão D5)

Deps: `scos-foundation-core`, `scos-foundation-spring`, `spring-boot-starter-web`, `spring-data-commons`, `feign-core` (optional).

### `scos-foundation-cache`
- `configuration/cache/*` — incluindo `PolymorphicRedisSerializer`

Deps: `scos-foundation-core`, `spring-boot-starter-cache`, `spring-boot-starter-data-redis`.

É aqui que aterrissa a correção de allowlist do `Class.forName` apontada no review de segurança.

### `scos-foundation-jpa`
- `entity/BaseEntity`
- `configuration/hibernate/JacksonCustomJsonFormatMapper`
- `configuration/liquibase/BaseLiquibaseProperties`
- `utils/SpecificationRepository`

Deps: `scos-foundation-core`, `spring-boot-starter-data-jpa`, `querydsl-jpa`, `liquibase-core` (optional).

### `scos-foundation-validation`
- `validation/*/constraint/*` — os `ConstraintValidator` (as anotações ficam no `validation-api`)
- `valueobjects/` — `Cpf`, `Cnpj`, `Email`, `TaxIdentifier`

Deps: `scos-foundation-core`, `scos-foundation-validation-api`, `caelum-stella-core`, `jakarta.persistence-api` (**provided** — só anotações, ver decisão D2).

### Estratégia `-api`: anotação separada da implementação

O SCOS-Flow tem um módulo de domínio separado do módulo que sobe o Spring. Esse módulo precisa de `@Auditable` e `@JdempotentProperty` em tempo de compilação, mas **não** deveria arrastar Redis, aspecto AOP e listener do Hibernate junto.

Por isso as anotações não voltam para dentro dos módulos de implementação: cada dono ganha um par.

| Artefato `-api` | Conteúdo | Deps |
|---|---|---|
| `scos-foundation-audit-api` | `@Auditable`, `@AuditAction` | nenhuma |
| `scos-foundation-jdempotent-api` | as 5 anotações `@Jdempotent*`, mais `KeySource` e `MismatchPolicy` quando existirem | nenhuma |
| `scos-foundation-validation-api` | `@CPF`, `@CNPJ`, `@TaxIdentifier`, `@ZipCode` | `jakarta.validation-api` |

**Regra que impede o `-api` de virar um novo `utils`:** ele contém apenas anotações e os enums que as anotações referenciam nos seus atributos. Nada mais — sem helper, sem constante solta, sem interface de serviço. Verificável por ArchUnit, e é a regra que faltava no `utils`.

**Um `-api` não pode gerar par para tudo.** `audit`, `jdempotent` e `validation` ganham porque suas anotações aparecem em classes de domínio. `web` e `spring` **não** ganham: `@ScosController` e `@ScosRule` marcam beans Spring, que por definição só existem no módulo que já tem Spring. Criar `web-api` seria multiplicar artefato sem consumidor.

### O modo de falha a documentar

Depender só do `-api` compila e **não faz nada em tempo de execução** — sem o módulo de implementação no classpath, não há listener nem aspecto para ler a anotação. Nenhum erro, nenhum aviso.

Em trilha de auditoria isso significa descobrir na auditoria de verdade. Duas medidas, ambas baratas:

- Um `@ConditionalOnClass` no lado da implementação já não resolve (o problema é a ausência dela). O que resolve é o inverso: o módulo de implementação, ao subir, **loga em `INFO` quantas classes anotadas encontrou**. Zero classes anotadas com o módulo ligado, ou anotações presentes sem o módulo, viram sinal visível.
- README de cada `-api` abrindo com a frase: este artefato não executa nada; a implementação é `scos-foundation-<x>`.

### Todas as anotações, por destino

São 18 anotações no `utils`. Nenhuma fica sem casa:

| Anotação | Destino |
|---|---|
| `ScosController` | `web` |
| `ScosRequestMapping`, `ScosRequestGET/POST/PUT/DELETE` | `web` (ver D1) |
| `ScosRule`, `ScosRuleService` | `spring` |
| `NormalizeStrings` | `spring` |
| `CPF`, `CNPJ`, `TaxIdentifier`, `ZipCode` | `validation-api` |
| `Auditable`, `AuditAction` | `audit-api` |
| `JdempotentId`, `JdempotentIgnore`, `JdempotentProperty`, `JdempotentRequestPayload`, `JdempotentResource` | `jdempotent-api` |

`ScosRule` e `ScosRuleService` não têm **nenhum** consumidor dentro da foundation — são estereótipos criados para as aplicações. Isso não as torna descartáveis, mas confirma que pertencem a um módulo de estereótipos, não ao `core`.

### Efeito no acoplamento

A saída das anotações do `utils`, sozinha, já permite ao `jdempotent` deixar de depender de JPA e Redis. O split `-api` acrescenta o ganho do lado do consumidor: o módulo de domínio do SCOS-Flow passa a depender de três artefatos sem dependência alguma, em vez de arrastar a foundation inteira para compilar uma anotação.

---

## 4. Grafo resultante

```
(sem dependências)
  audit-api        ←  audit
  jdempotent-api   ←  jdempotent
  validation-api   ←  validation

core  ←  spring  ←  web
      ←  cache          ←  jdempotent
      ←  jpa            ←  audit
      ←  validation
      ←  exception
      (privacy não depende de nada disso)
```

Os três `-api` são folhas sem dependência nenhuma — é o que os torna importáveis por um módulo de domínio puro.

Sem ciclos. `core` é o único ponto compartilhado, e ele não conhece Spring.

Ganho concreto: `exception` deixa de depender de um módulo com JPA e Redis para usar quatro tipos. `jdempotent` idem.

---

## 5. Nomes de pacote

Decisão: **tudo entra na própria `1.2.0`, que ainda é SNAPSHOT.** Nada foi publicado sob essa versão, e compatibilidade com consumidores não é restrição neste ciclo.

Isso elimina a parte mais cara do plano original — agregador de transição, preservação de pacotes, janela de depreciação — e permite fazer certo de primeira:

**Os pacotes acompanham os módulos.**

| Módulo | Pacote raiz |
|---|---|
| `scos-foundation-core` | `br.com.sawcunhaos.foundation.core` |
| `scos-foundation-spring` | `br.com.sawcunhaos.foundation.spring` |
| `scos-foundation-web` | `br.com.sawcunhaos.foundation.web` |
| `scos-foundation-cache` | `br.com.sawcunhaos.foundation.cache` |
| `scos-foundation-jpa` | `br.com.sawcunhaos.foundation.jpa` |
| `scos-foundation-validation` | `br.com.sawcunhaos.foundation.validation` |
| `scos-foundation-audit-api` | `br.com.sawcunhaos.foundation.audit.api` |
| `scos-foundation-jdempotent-api` | `br.com.sawcunhaos.foundation.jdempotent.api` |
| `scos-foundation-validation-api` | `br.com.sawcunhaos.foundation.validation.api` |

Ganhos de decidir agora em vez de adiar para um `2.0.0`:

- **Sem split package.** Cada pacote vive num único JAR — JPMS e native image permanecem viáveis sem outra migração depois.
- **Sem agregador deprecated** carregando peso morto.
- **`utils` deixa de existir**, em vez de virar um POM fantasma. O nome era parte do problema: um módulo chamado "utils" convida qualquer classe sem casa.
- Renomear import é trabalho de IDE, não de arquitetura. Fazer com o repositório inteiro na mão é a hora mais barata que vai existir.

A única coisa que se perde é a rede de compatibilidade — que só teria valor se houvesse consumidor externo a proteger.

**Nota de fim de janela:** a partir do momento em que `1.2.0` for publicada, essa liberdade acaba. Vale concentrar aqui tudo que for quebra de contrato, incluindo o que o plano do `jdempotent` prevê.

## 6. Limpeza prévia

Rodar `mvn dependency:analyze` antes de dividir, para não carregar peso morto para os módulos novos. O que já encontrei por inspeção:

| Dependência | Classes que usam | Ação |
|---|---|---|
| `bcprov-jdk18on` (BouncyCastle) | **0** | Remover |
| `snakeyaml` | **0** | Remover |
| `mapstruct` | **0** | Remover |
| `guava` | 1 (`StringTransformRule`) | Substituir por `commons-lang3` ou código próprio e remover |
| `commons-io` | 1 | Avaliar substituição por `java.nio` |
| `spring-cloud-starter` | 3 | Verificar necessidade real |
| `jackson-dataformat-smile` | 1 (`PolymorphicRedisSerializer`) | Vai junto para `cache` |
| `gson` | 4 no `utils` + 5 fora dele | Removida — ver seção 6.1 |

Remover BouncyCastle de uma foundation é ganho desproporcional ao esforço: é um jar grande, com CVEs frequentes, que hoje não executa uma linha.

---

## 6.1 Remoção do Gson (decidido)

**Decisão: o Gson sai; a stack JSON é Jackson.**

Boa notícia de partida: só existe **uma** versão do Jackson no repositório (`tools.jackson`, Jackson 3). Não há mistura de Jackson 2 e 3 para resolver junto — o único conflito de stack é mesmo Gson × Jackson.

### O que some de graça

Os três adapters `LocalDateAdapter`, `LocalDateTimeAdapter` e `LocalTimeAdapter` existem **porque o Gson não trata `java.time` nativamente**. O Jackson trata. Os três, seus testes e o `GsonUtils` são apagados — não migrados.

Isso resolve a alocação que estava pendente: `JacksonXmlUtils` vai para `web` e nenhum módulo `foundation-json` é necessário.

### O que exige migração de verdade

O Gson não está só no `utils`. São 5 pontos, em dois outros módulos:

| Onde | Uso | Observação |
|---|---|---|
| `privacy/core/JsonMasker` | `new Gson()` próprio, com travessia de `JsonElement`/`JsonObject` | O Javadoc diz que usa Gson próprio **de propósito**, para o módulo não depender de `utils`. Mapeia direto para `JsonNode`/`ObjectNode`, mas é caminho quente de mascaramento — preservar a característica O(n) |
| `audit/ScosAuditHashService` | `JsonParser`, `JsonObject` | **Crítico — ver abaixo** |
| `audit/ScosAuditServiceBean` | `toJson(stateMap)` do estado da entidade | Muda o formato do payload gravado na trilha |
| `audit/ScosAuditBatchConsumer` | `toJson` do evento para a DLQ | Idem |
| `audit/ScosAuditDlqJob` | `fromJson` da linha da DLQ | Lê o que o item acima gravou |

### Duas armadilhas a nomear

**1. O hash da cadeia é calculado sobre JSON.** Trocar o serializador muda ordenação de chaves, tratamento de nulos e formato de datas — logo, **muda o hash**. Qualquer cadeia existente deixa de verificar.
Como a reformulação do `audit` já prevê remover o hash-chain, o encadeamento correto é: **remover o hash-chain primeiro, migrar o serializador depois**. Na ordem inversa, você quebra uma coisa que ia ser removida de qualquer jeito e gasta tempo investigando.

**2. Gson e Jackson divergem no default de nulos.** O Gson omite campos nulos por padrão; o Jackson os inclui. Como o payload da auditoria é o estado da entidade, isso muda o conteúdo gravado — e um campo que passa a aparecer como `null` explícito é diferente de um campo ausente, para quem for consultar a trilha depois.
Definir explicitamente a política (`@JsonInclude`) em vez de herdar o default, e registrar a escolha no README do `audit`.

### Sequência

A migração acontece **antes** da divisão em módulos: ela apaga 4 classes e remove uma dependência, então dividir depois é dividir menos coisa. Vira a Fase 1.5, entre a limpeza e a devolução das anotações.

---

## 7. Decisões que preciso que você tome

Estas mudam o desenho e não dá para decidir sozinho:

**D1 — `@Cacheable` na anotação de rota.**
`ScosRequestGET` (e POST/PUT/DELETE) declaram `@Cacheable` na meta-anotação, com `nameCache()` fazendo `@AliasFor`. Isso obriga `foundation-web` a depender de `foundation-cache`, o que reintroduz o acoplamento que estamos desfazendo.
Opções: (a) `web` depende de `cache`; (b) tirar o cache da meta-anotação e o consumidor compõe `@ScosRequestGET` + `@Cacheable`; (c) um módulo `web-cache` só com as anotações combinadas.
De passagem: `@Cacheable` numa anotação de POST/PUT/DELETE merece revisão à parte.

**D2 — `valueobjects` usam `@Embeddable`.**
Se ficarem em `validation`, ela precisa de `jakarta.persistence-api` como `provided` — funciona, porque são só anotações, mas mistura conceitos. Alternativa: mover para `jpa`, separando o value object da sua validação.

**D3 — Feign.**
São 2 classes. Módulo próprio, ou dependência `optional` dentro de `web` com `@ConditionalOnClass`? Minha inclinação é a segunda — não vale um artefato para dois encoders.

---

## 8. Fases de execução

### Fase 0 — Rede de proteção (1 dia)
- Inventário classe → módulo destino, revisado e congelado. **Nenhuma das 71 classes pode ficar sem destino** — o inventário é o artefato que garante isso, e foi onde surgiram as lacunas de `annotation/rules` e `listener/ScosOnStartupListener`.
- Build completo verde como linha de base — é a única rede necessária, já que não há contrato externo a preservar.

### Fase 1 — Limpeza (1 dia)
- Remover as dependências não utilizadas da seção 6.
- Substituir guava.
- Um commit por dependência removida, cada um verde.

### Fase 1.5 — Migrar Gson → Jackson (1–1,5 dia)
- Apagar os 3 adapters, o `GsonUtils` e seus testes.
- Migrar `JsonMasker` (`privacy`) e os 4 pontos do `audit`.
- Remover `gson` dos poms de `utils` e `privacy`.
- **Encadear com o plano do `audit`:** remover o hash-chain antes desta fase.

### Fase 2 — Criar os módulos `-api` (1–1,5 dia)
- Criar `audit-api`, `jdempotent-api` e `validation-api`; mover as anotações para eles.
- Repontar `audit`, `jdempotent` e o módulo de domínio do SCOS-Flow.
- Menor risco do plano e maior ganho isolado: quebra o acoplamento reverso imediatamente.
- **Coordenar com o plano do `jdempotent`**, que também mexe nessas anotações. Fazer esta fase **antes**, para o outro plano trabalhar sobre a estrutura final.

### Fase 3 — Extrair `core` (1–2 dias)
- Tudo depende dele; sai primeiro.
- Regra de ouro: se precisar importar `org.springframework`, a classe não pertence ao `core`.
- Repontar `exception`, `audit` e `jdempotent` para o `core`.

### Fase 4 — Extrair os módulos folha (2–3 dias)
- `spring` → `validation` → `cache` → `jpa` → `web`, nessa ordem (do menos para o mais acoplado).
- Cada módulo é um commit; o build inteiro fica verde entre eles.
- Cada extração é **dois** commits: um que só move e renomeia pacote, outro que ajusta o que precisar. Nunca misturar — renomeação em massa esconde mudança de comportamento no diff.

### Fase 5 — Remoção e documentação (1 dia)
- Remover o módulo `scos-foundation-utils` do reactor e do `dependencyManagement`.
- README com a tabela "quero X → importe o módulo Y".
- CHANGELOG com o caminho de migração.

---

## 9. Como impedir que o `utils` volte a crescer

O motivo de `utils` ter chegado a 71 classes é que não havia nada dizendo onde uma classe nova deveria morar. Corrigir a estrutura sem corrigir isso só adia o problema.

**Testes ArchUnit no build**, executados no perfil `analyze`:

- `foundation-core` não pode importar `org.springframework..` — é a regra que justifica a existência do módulo `spring`
- `foundation-spring` não pode importar `jakarta.servlet..`, `jakarta.persistence..` nem `org.springframework.data..`
- Qualquer módulo `*-api` só pode conter `@interface` e `enum` — a regra que faltava no `utils`
- Nenhum módulo `*-api` pode ter dependência além de `jakarta.validation-api`
- `foundation-core` não pode importar `jakarta.persistence..` nem `jakarta.servlet..`
- Nenhum módulo pode depender de `web`, exceto aplicações
- Nenhum ciclo entre pacotes de módulos diferentes
- Proibir a criação de pacote chamado `utils.utils` ou equivalente

A regra do `core` é a que mais paga: ela impede exatamente o movimento que criou o problema — alguém precisa de "um lugar qualquer" e joga uma classe Spring no módulo compartilhado.

---

## 10. Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Conflito com o plano do `jdempotent` | **Alta** | As anotações são território comum. Executar a Fase 2 daqui antes de tocar no plano do `jdempotent` — que também adiciona atributos a `@JdempotentResource` e passa a escrevê-los no `-api` |
| `-api` importado sem a implementação, sem erro | Média | Log de contagem de classes anotadas na subida + README explícito; ver seção 3 |
| `analyze` só existe em 4 dos 5 poms | Certa | O `privacy` não tem o perfil hoje; ao criar módulos novos, subir a configuração para `<pluginManagement>` do pai em vez de copiar |
| Refatoração se transformar em reescrita | **Alta** | Mover e renomear apenas; **não** reescrever lógica. As melhorias de código ficam para depois, com os módulos já isolados |
| Renomeação de pacote esconder mudança de comportamento no diff | Média | Commits separados: um só renomeia, outro só altera lógica. Nunca os dois juntos |

O último merece ênfase: a tentação de "já que estou mexendo, arrumo" é o que faz refatorações estruturais atrasarem. Mover primeiro, melhorar depois — com exceção das dependências mortas da Fase 1, que são remoção pura.

---

## 11. Sequência de commits sugerida

1. `test: congelar inventário classe → módulo destino`
2. `chore: remover dependências não utilizadas (bouncycastle, snakeyaml, mapstruct)`
3. `refactor: substituir guava por commons-lang3 em StringTransformRule`
4. `refactor: migrar JsonMasker do privacy para Jackson`
5. `refactor: migrar os pontos de serialização do audit para Jackson`
6. `chore: remover gson e os adapters java.time do utils`
7. `feat: criar scos-foundation-audit-api com as anotações de auditoria`
8. `feat: criar scos-foundation-jdempotent-api com as anotações de idempotência`
9. `feat: criar scos-foundation-validation-api com as anotações de validação`
10. `feat: criar módulo scos-foundation-core`
11. `refactor: repontar exception, audit e jdempotent para o core`
12. `feat: criar módulo scos-foundation-spring`
13. `feat: criar módulo scos-foundation-validation`
14. `feat: criar módulo scos-foundation-cache`
15. `feat: criar módulo scos-foundation-jpa`
16. `feat: criar módulo scos-foundation-web`
17. `refactor: remover o módulo scos-foundation-utils do reactor`
18. `test: regras ArchUnit de fronteira entre módulos`
19. `refactor: alinhar pacotes de exception, audit, jdempotent e privacy ao novo esquema`
20. `docs: mapa módulo → capacidade no README raiz`
