# Correção: chave de idempotência ignora a posição/identidade do payload

**Data**: 2026-08-17
**Status**: 🔄 Em Análise
**Tipo**: 🐛 Bug Fix

---

## ⚠️ Princípio SRP — Uma Funcionalidade por Ideia

> **Regra**: Uma ideia = uma funcionalidade. Features independentes → arquivos separados.

- **Nome da funcionalidade**: `jdempotent-chave-por-posicao-payload`
- **Resumo em uma frase**: Fazer a chave de idempotência do `jdempotent` depender da posição/nome de cada argumento `@JdempotentRequestPayload`, não apenas do conjunto de valores, para eliminar colisões quando dois argumentos trocam de valor entre si.

**Checklist SRP**:
- [x] Esta ideia cobre exatamente uma funcionalidade
- [x] Não mistura features independentes no mesmo arquivo
- [x] O nome é específico (não genérico como "melhorar módulo X")

---

## 1️⃣ Visão

### Problema
`IdempotentAspect.getIdempotentNonIgnorableWrapper()` funde **todos** os argumentos anotados com `@JdempotentRequestPayload` (ou o único argumento do método) em um **único** `Map<String,Object>` dentro de `IdempotentIgnorableWrapper`, sem registrar de qual argumento cada entrada veio. Isso causa dois defeitos observados/derivados a partir do mesmo mecanismo:

1. **Colisão entre argumentos primitivos do mesmo tipo.** Quando o argumento é "primitivo" (`CharSequence`, `Boolean`, `Number` — inclui `Long`), a chave inserida no mapa é o próprio valor (`arg.toString()`), não o nome/posição do parâmetro. Um método com dois `Long` (`id`, `profileId`) anotados gera, para `(id=1, profileId=2)` e `(id=2, profileId=1)`, o mesmo `Map` `{"1"=1, "2"=2}` — a ordem de inserção não importa para `HashMap.equals()`/`toString()`. Ambas as chamadas produzem a mesma `IdempotencyKey` (MD5 idêntico), tratando os dois valores como um **conjunto não-ordenado** em vez de um par ordenado. Foi assim que os testes `approved(1,2)` e `rejected(2,1)` colidiram.
2. **Sobrescrita silenciosa entre payloads POJO com campo de mesmo nome.** Se dois argumentos `@JdempotentRequestPayload` de tipos diferentes (ou iguais) tiverem um campo com o mesmo nome, o segundo `put()` no mapa compartilhado sobrescreve o valor do primeiro — o campo do primeiro payload nunca entra no hash. Caso reproduzível com `TestIdempotentResource.idempotentMethodWithThreeParamaterAndMultipleJdempotentRequestPayloadAnnotation` se os dois payloads anotados tiverem campos homônimos.

Validado em código (não é suposição):
- `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java:293-310` (`getIdempotentNonIgnorableWrapper`) — mapa único, chave = valor para tipos primitivos.
- `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java:244-267` (`findIdempotentRequestArg`) — tanto o caminho de 1 argumento quanto o de N payloads chamam `getIdempotentNonIgnorableWrapper` e envolvem o resultado (um único `IdempotentIgnorableWrapper`) em `IdempotentRequestWrapper` via o construtor de objeto único — confirmado por busca no repositório que o construtor `IdempotentRequestWrapper(List<Object>)` com mais de 1 elemento **nunca é exercitado em produção**; o `sort()` em `IdempotentRequestWrapper.toString()` é hoje um no-op.

### Objetivo
Garantir que a chave de idempotência seja determinística **por posição/identidade do argumento**, não apenas pelo conjunto de valores — ou seja, `approved(1,2)` e `rejected(2,1)` devem gerar `IdempotencyKey` diferentes, e dois payloads com campos homônimos não devem se sobrescrever.

Critério de sucesso: novo teste reproduzindo o caso `approved(1,2)` vs `rejected(2,1)` (dois `Long` anotados com `@JdempotentRequestPayload`) passa a gerar chaves **diferentes**; suíte atual de `IdempotentAspectITTest`/`IdempotentAspectUTTest` continua verde.

### Fora de Escopo
- Reestruturar `IdempotentRequestWrapper`/`IdempotentIgnorableWrapper` para usar uma lista real por payload (Opção 3 discutida) — mudança de maior superfície, mais arriscada, não necessária para resolver o defeito relatado.
- Remover/ligar a `JdempotentNoAnnotationChain`, hoje instanciada em `IdempotentAspect.fillChains()` mas nunca conectada como elo da cadeia — é um achado separado, sem relação com a colisão de chave.
- Qualquer mudança em `@JdempotentProperty` (nome de campo) ou nos demais elos da `AnnotationChain` — já funcionam corretamente para campos de um único objeto.

---

## 2️⃣ Requisitos

### Funcionais
- [ ] **RF-01**: A chave de idempotência deve diferenciar entradas pela posição do argumento (índice na lista de payloads), não apenas pelo valor.
- [ ] **RF-02**: Deve ser possível nomear explicitamente um payload via `@JdempotentRequestPayload(value = "...")`, sobrepondo o índice para gerar uma chave mais legível.
- [ ] **RF-03**: Quando `@JdempotentRequestPayload` não define `value()` (ou não é usado — caso de 1 argumento único), o índice do argumento é usado como prefixo automaticamente (fallback), sem exigir mudança no código de quem já usa a lib.

### Não-Funcionais
- [ ] **RNF-01 (compatibilidade)**: A assinatura pública `IdempotentAspect.getIdempotentNonIgnorableWrapper(List<Object>)` pode mudar — validado que nenhum teste do módulo a chama diretamente (apenas uso interno via `findIdempotentRequestArg`).
- [ ] **RNF-02 (impacto operacional)**: O formato da `IdempotencyKey` gerada muda para *todo* método idempotente (inclusive o caso de 1 argumento, que passa a ter prefixo de índice `"0:"`). Chaves já persistidas em Redis/cache no momento do deploy deixam de casar durante a janela de TTL restante — efeito é, no pior caso, uma reexecução duplicada pontual, não corrupção de dado. Deve constar no changelog/release notes do módulo.

---

## 3️⃣ Arquitetura

### Componentes Afetados
```
utils (annotation)
└── JdempotentRequestPayload.java: modificação — adicionar `String value() default ""`

jdempotent (core.aspect)
└── IdempotentAspect.java: modificação
    ├── findIdempotentRequestArg(...): passa a coletar, junto com cada payload, um prefixo
    │   (nome da anotação se presente, senão índice)
    └── getIdempotentNonIgnorableWrapper(...): assinatura ganha lista de prefixos paralela;
        chave inserida no Map passa a ser `prefixo + ":" + chaveAtual`
```

### Fluxo Principal
```
Args do método → filtra @JdempotentRequestPayload (ou usa arg único)
                → para cada arg: prefixo = value() da anotação (se não vazio) OU índice posicional
                → getIdempotentNonIgnorableWrapper(args, prefixos)
                    → primitivo: Map.put(prefixo + ":" + valor, valor)
                    → POJO: Map.put(prefixo + ":" + nomeCampo, valorCampo)   [via AnnotationChain, já existente]
                → IdempotentIgnorableWrapper → IdempotentRequestWrapper → MD5 → IdempotencyKey
```

### Decisões Técnicas
| Decisão | Escolha | Alternativa Descartada | Motivo |
|---------|---------|------------------------|--------|
| Como diferenciar argumentos | Prefixo (índice ou nome) por entrada no mapa existente | Reestruturar para lista de wrappers + ordem preservada (Opção 3) | Menor superfície de mudança; resolve os dois defeitos (primitivo e POJO homônimo) sem mexer no modelo `IdempotentRequestWrapper`/sort |
| Índice vs nome obrigatório | Híbrido: nome via `@JdempotentRequestPayload(value=...)` opcional, fallback automático para índice | Só índice (Opção 1) — funciona mas gera chaves ilegíveis no Redis (`"0:1"`) | Nome legível quando o consumidor da lib quiser, sem quebrar quem não anotar |

### Banco de Dados
- **Impacto**: ❌ Não (Redis é cache de idempotência com TTL, não fonte de dados durável; ver RNF-02 sobre efeito colateral de deploy)

---

## 4️⃣ Implementação

### Arquivos

**Modificados**:
- `utils/src/main/java/br/com/sawcunhaos/foundation/utils/annotation/jdempotent/JdempotentRequestPayload.java` — adicionar `String value() default "";`
- `jdempotent/src/main/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspect.java`:
  - `findIdempotentRequestArg(ProceedingJoinPoint pjp)` (linha ~240-269) — capturar prefixo (nome da anotação ou índice) junto de cada payload coletado; caso de 1 argumento único usa prefixo `"0"`.
  - `getIdempotentNonIgnorableWrapper(List<Object> args)` (linha ~293-310) — nova assinatura recebendo a lista de prefixos paralela; usar `prefixo + ":" + chaveAtual` como chave do `Map` em ambos os ramos (primitivo e reflexão de campos).

**Testes a adicionar/ajustar** (não criar novos arquivos de produção):
- `jdempotent/src/test/java/br/com/sawcunhaos/foundation/jdempotent/core/aspect/IdempotentAspectITTest.java` (ou UT equivalente) — caso com dois payloads primitivos do mesmo tipo em ordens trocadas gerando chaves diferentes.
- Caso com dois payloads POJO com campo homônimo, validando que ambos os valores contribuem para a chave (nenhum é sobrescrito).
- `TestIdempotentResource.java` — adicionar método(s) de apoio com dois `Long` (ou `String`) anotados com `@JdempotentRequestPayload`, com e sem `value()` explícito, para cobrir o fallback e o nome customizado.

### Tarefas
- [ ] **T-01**: Adicionar `value()` opcional em `JdempotentRequestPayload`.
- [ ] **T-02**: Alterar `findIdempotentRequestArg` para coletar prefixo por payload (nome ou índice).
- [ ] **T-03**: Alterar `getIdempotentNonIgnorableWrapper` para usar o prefixo na construção da chave do `Map`, tanto para o ramo primitivo quanto para o ramo de reflexão de campos.
- [ ] **T-04**: Adicionar método(s) de teste em `TestIdempotentResource` reproduzindo o caso `approved(1,2)`/`rejected(2,1)`.
- [ ] **T-05**: Escrever teste comprovando que a colisão não ocorre mais (chaves diferentes) e teste comprovando que campos homônimos entre dois payloads não se sobrescrevem.
- [ ] **T-06**: Rodar suíte completa de `jdempotent` (UT + IT) para garantir que nenhum teste existente dependia do formato antigo da chave.
- [ ] **T-07**: Registrar a mudança de formato de chave no changelog do módulo (RNF-02).

### Riscos e Edge Cases
1. Deploy com chaves antigas ainda válidas no Redis: dedup momentaneamente ineficaz para requisições em voo na janela de TTL — aceitável, sem corrupção de dado (ver RNF-02).
2. Métodos com muitos payloads primitivos do mesmo valor em posições diferentes (ex.: `f(1,1)`) devem continuar gerando chave estável e determinística — cobrir em teste.
3. `@JdempotentRequestPayload(value="")` (string vazia explícita) deve cair no mesmo fallback de índice que a ausência de `value()` — usar `StringUtils.isBlank`, não apenas `null`.
4. Dois consumidores usando o mesmo `value()` customizado em posições diferentes do mesmo método (erro de configuração do usuário da lib) — fora de escopo validar/alertar, mas vale nota na Javadoc da anotação.

---

## 📎 Referências
- Conversa de diagnóstico: colisão encontrada em testes `approved(1,2)` / `rejected(2,1)` usando dois campos `Long` (`id`, `profileId`) anotados com `@JdempotentRequestPayload`.
- Código-fonte analisado: `IdempotentAspect`, `IdempotentRequestWrapper`, `IdempotentIgnorableWrapper`, `DefaultKeyGenerator`, `AnnotationChain` (e implementações) no módulo `jdempotent`.

---
