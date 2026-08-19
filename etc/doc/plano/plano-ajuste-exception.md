# Plano de Ajuste — `scos-foundation-exception`

**Origem / Alvo:** `1.2.0-SNAPSHOT` · **Esforço estimado:** 4–5 dias úteis
**Depende de:** plano de decomposição do `utils` (Fase 3 — extração do `core`)

---

## 1. Diagnóstico

O módulo tem 8 classes e 310 linhas concentradas numa única, o `ExceptionsHandler`. A adoção de RFC 9457 é boa e bem documentada. Mas há um **bug de segurança silencioso**, uma inversão estrutural e uma quantidade grande de duplicação.

| Métrica | Valor |
|---|---|
| Classes | 8 (`ExceptionsHandler` com 310 linhas) |
| Handlers | 11 métodos `@ExceptionHandler` / `@Override` |
| Tipos consumidos do `utils` | `ScosException`, `ScosExceptionCode`, `ExceptionCode`, `LocaleService` |
| Registro Spring | **nenhum** — só funciona via component scan |

---

## 2. A inversão estrutural

O módulo se chama `exception`, mas **não contém a exceção base**: `ScosException` mora em `utils`, e o `exception` depende de `utils` para usá-la. Um módulo de exceções que precisa importar um módulo de utilitários para conhecer sua própria hierarquia.

Olhando o conteúdo real, há duas coisas distintas empacotadas juntas:

| Natureza | Classes | Quem precisa |
|---|---|---|
| **Contrato de domínio** — o que uma regra de negócio lança | `ScosException`, `ExceptionCode`, `ScosExceptionCode`, `LocaleService`, `ScosNoContentException`, `ScosNoRollbackException`, `ScosSecurityException`, `MethodNotImplementedException` | Qualquer camada, inclusive serviços sem web |
| **Tradução para HTTP** — como isso vira resposta | `ExceptionsHandler`, `ScosProblemDetails`, `ScosFieldError`, `ExceptionUtils` | Só quem expõe API |

### Decisão

**A hierarquia de exceções vai para o `core`.** É contrato de domínio, sem dependência de Spring — cabe exatamente na regra do `core`.

**A tradução HTTP vai para o `scos-foundation-web`.** O `ExceptionsHandler` é um `@ControllerAdvice` que estende `ResponseEntityExceptionHandler`: sem `spring-web` ele não existe. E quem tem `@ScosController` quer o handler.

**O módulo `scos-foundation-exception` deixa de existir.**

Por que fundir no `web` em vez de manter um artefato separado:

- Nenhum consumidor plausível quer o `ScosProblemDetails` sem `spring-web` — ele é uma fábrica de `ProblemDetail`, que é classe do `spring-web`.
- Manter separado exigiria que `web` dependesse de `exception` ou vice-versa; nos dois casos eles sobem sempre juntos.
- Um artefato a menos para versionar e documentar.

O advice fica sob `@ConditionalOnProperty(scos.web.error-handler.enabled, matchIfMissing = true)` e `@Order(Ordered.LOWEST_PRECEDENCE)`, para que uma aplicação com advice próprio tenha precedência sem precisar desligar nada.

### Ganho de acoplamento

Hoje o `audit` depende de `exception` (que traz `spring-web` e `spring-security-core`) para usar `ScosException`. Depois, depende só do `core`.

---

## 3. Bugs

### 3.1 🔴 O handler de acesso negado captura a exceção errada

```java
import java.nio.file.AccessDeniedException;   // ← sistema de arquivos
...
@ExceptionHandler(AccessDeniedException.class)
protected ResponseEntity<ProblemDetail> handleAccessDeniedException(...)
```

A exceção de autorização do Spring Security é `org.springframework.security.access.AccessDeniedException`. A importada é a do **NIO**, lançada quando o processo não consegue ler um arquivo.

Duas consequências, ambas ruins:

- **Falha de autorização não passa por aqui.** Só não explodiu porque `AuthorizationDeniedException` (o caminho novo do Spring Security 6) tem handler próprio logo abaixo. Quem usar `@PreAuthorize` pelo caminho antigo, ou lançar `AccessDeniedException` manualmente, cai no handler genérico e recebe **500 em vez de 403**.
- **Erro de I/O vira 403.** Um problema de permissão de arquivo no servidor é reportado ao cliente como acesso negado — diagnóstico errado e vazamento de semântica.

Correção: trocar o import e manter os dois handlers, um para cada tipo do Spring Security.

### 3.2 🔴 `IndexOutOfBoundsException` no handler de validação

```java
ex.getBeanResults().get(0).getFieldErrors()
```

`getBeanResults()` vem vazia quando a violação é em parâmetro simples (`@RequestParam @Min(1) int page`), e não em bean. O handler de erro lança erro — e o que o cliente recebe é o 500 do handler genérico, mascarando um 400.

Correção: iterar sobre todos os resultados e tratar também `getValueResults()`.

### 3.3 🟠 Regex sobre a mensagem da exceção

```java
String patternField = "(\\[\\\"[\\w,\\s]+\\\"\\])";
Matcher matcher = pattern.matcher(ex.getMessage());
```

Extrair o nome do campo por regex na mensagem do Jackson quebra em qualquer upgrade da biblioteca e depende do idioma da mensagem. O risco é concreto agora: o projeto está em **Jackson 3** (`tools.jackson`), cujas mensagens não são idênticas às do Jackson 2 — o padrão pode já não casar, e a falha é silenciosa (`field` fica vazio e a mensagem sai truncada).

Correção: navegar a causa (`InvalidFormatException` / `MismatchedInputException`) e usar `getPath()`, que é API estruturada.

### 3.4 🟠 Erro de cliente logado como `ERROR` com stack trace

Todos os 11 handlers chamam `log.error("...", ex)`, inclusive os de validação. Um cliente enviando payload inválido em laço gera stack traces em `ERROR` sem limite — ruído que afoga erro real, e vetor de enchimento de disco.

Regra: **4xx em `WARN` sem stack trace** (ou `DEBUG` com), **5xx em `ERROR` com stack trace**. O único que merece `ERROR` incondicional é o handler genérico.

Caso à parte: `ScosNoContentException` devolve **204**, que é sucesso, e loga em `ERROR`. Vai para `DEBUG`.

### 3.5 🟠 404 sai em formato diferente de todos os outros erros

As exceções tratadas pela superclasse `ResponseEntityExceptionHandler` e não sobrescritas (rota inexistente, método não suportado, media type inválido) saem no formato padrão do Spring, não no `ScosProblemDetails`. O contrato de erro da API é inconsistente: a maioria dos erros tem `code`, `requestId` e `timestamp`; alguns não.

Correção estrutural: sobrescrever **`handleExceptionInternal`** uma vez. Todo erro tratado pela superclasse passa a sair no formato SCOS, sem escrever um handler por tipo. Isso resolve o problema e elimina boa parte da duplicação da seção 4.

### 3.6 🟡 `MethodNotImplementedException` sem handler

Cai no genérico e vira **500 com mensagem genérica**. O correto é **501 Not Implemented**, que é justamente o que o nome da exceção diz.

### 3.7 🟡 Exceção como controle de fluxo em `resolveTitle`

```java
try { return localeService.getMessage(title); }
catch (Exception e) { return "Business Error"; }
```

Título ausente no bundle vira um literal em inglês, no meio de uma API cujas mensagens são localizadas. E usar `catch` para testar existência de chave esconde falhas reais do `LocaleService`.

Correção: `LocaleService` ganha `getMessageOrDefault(code, default)`; o default vem do `ExceptionCode.getTitle()`, que já existe e já tem valor.

### 3.8 🟡 Constante duplicada

`ScosProblemDetails.MDC_REQUEST_ID = "X-Request-ID"` repete a chave que o `LoggingInitialFilter` define do outro lado. Duas definições da mesma constante em módulos diferentes; se uma mudar, o `requestId` some da resposta sem erro.

Correção: constante única no `core`, consumida pelos dois.

---

## 4. Qualidade

### 4.1 Duplicação

Quatro handlers repetem o mesmo bloco: montar `List<ScosFieldError>` → resolver mensagem → `ofValidation` → `enrich` → `ResponseEntity.status(...).body(...)`. E `handleMethodArgumentNotValid` e `handleHandlerMethodValidationException` são praticamente idênticos.

Com o `handleExceptionInternal` da seção 3.5 mais um método privado `problem(status, code, detail, errors)`, os 310 linhas devem cair para algo em torno de 150.

### 4.2 Registro no Spring

Não existe `AutoConfiguration.imports` no módulo. O `@ControllerAdvice` só sobe se a aplicação escanear `br.com.sawcunhaos.foundation` — é a mesma inconsistência apontada no `audit` e no `jdempotent`. Com a fusão no `web`, entra na autoconfiguração desse módulo.

### 4.3 Fachada de log

`@Log4j2` aqui, `@Slf4j` nas outras 14 classes do repositório. E o `pom` não declara log4j — vem transitivo. Padronizar em `@Slf4j`.

### 4.4 Prefixo de log copiado

Todos os handlers logam `"handleSecurity - ..."`, inclusive os que não têm nada de segurança. Trocar pelo nome real ou remover.

### 4.5 Javadoc desatualizado

`ScosFieldError.of` documenta um `@param type` que não é parâmetro do método.

---

## 5. Fases

### Fase 0 — Testes que faltam (1 dia)
- Teste que prova o 3.1: lançar `org.springframework.security.access.AccessDeniedException` e assertar **403**. Hoje dá 500.
- Teste que prova o 3.2: `@RequestParam` inválido em parâmetro simples. Hoje dá 500.
- Teste de contrato de formato: rota inexistente deve responder com `code`, `requestId` e `timestamp`, como qualquer outro erro.
- Teste de nível de log por faixa de status.

Os três primeiros devem falhar antes das correções.

### Fase 1 — Correções de comportamento (1 dia)
3.1, 3.2, 3.6. São os que mudam código de resposta HTTP.

### Fase 2 — Reestruturação do handler (1–1,5 dia)
- Sobrescrever `handleExceptionInternal` (3.5).
- Extrair o bloco repetido (4.1).
- Substituir o regex por navegação da causa (3.3).
- Ajustar níveis de log (3.4) e a fachada (4.3, 4.4).

### Fase 3 — Movimentação para `core` e `web` (1–1,5 dia)
Executar **depois** da Fase 3 do plano de decomposição, quando o `core` já existir.

- `ScosException`, `ExceptionCode`, `ScosExceptionCode`, `LocaleService`, `ScosNoContentException`, `ScosNoRollbackException`, `ScosSecurityException`, `MethodNotImplementedException` → `core`
- `ExceptionsHandler`, `ScosProblemDetails`, `ScosFieldError`, `ExceptionUtils` → `web`
- `LocaleService.getMessageOrDefault` (3.7) e a constante de MDC (3.8) entram no `core` neste momento
- Remover `scos-foundation-exception` do reactor
- Repontar `audit` para o `core`

---

## 6. Ordem entre os três planos

Os três tocam os mesmos arquivos. A ordem correta:

| # | Plano | Fase | Por quê |
|---|---|---|---|
| 1 | `utils` | 1 e 1.5 | Limpeza de dependências e remoção do Gson, antes de qualquer movimentação |
| 2 | `exception` | 0 a 2 | Correções internas, com o módulo ainda no lugar — diff limpo |
| 3 | `utils` | 2 | Criar os módulos `-api` |
| 4 | `utils` | 3 | Extrair o `core` |
| 5 | `exception` | 3 | Mover a hierarquia para o `core` |
| 6 | `utils` | 4 e 5 | Extrair `web` (recebendo o handler) e remover o `utils` |
| 7 | `jdempotent` | todas | Trabalha sobre a estrutura final |

O princípio é o mesmo dos outros planos: **corrigir comportamento com o arquivo parado; mover depois.** Corrigir durante a movimentação produz um diff em que ninguém consegue separar renomeação de mudança de lógica.

---

## 7. Riscos

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Correção do 3.1 muda status de rotas que hoje dão 500 | **Alta — é o objetivo** | Está no CHANGELOG como correção; testes cobrindo os dois tipos de exceção |
| `handleExceptionInternal` alterar o corpo de erros hoje tratados pelo Spring | **Alta — é o objetivo** | Idem; é o que padroniza o contrato |
| Regex do 3.3 já estar quebrado em Jackson 3 sem ninguém ter notado | Média | O teste da Fase 0 revela; se estiver quebrado, é correção e não regressão |
| Fusão no `web` obrigar quem só quer as exceções a trazer `spring-web` | Baixa | Não ocorre: a hierarquia vai para o `core`; só o handler vai para o `web` |
| Corrigir e mover no mesmo commit | Média | Seção 6; fases separadas por natureza, não por arquivo |

---

## 8. Sequência de commits

1. `test: cobrir AccessDeniedException do Spring Security, parâmetro simples inválido e formato de 404`
2. `fix: capturar a AccessDeniedException do Spring Security, não a do NIO`
3. `fix: tratar validação de parâmetro simples sem IndexOutOfBounds`
4. `feat: responder 501 para MethodNotImplementedException`
5. `refactor: padronizar o corpo de erro via handleExceptionInternal`
6. `refactor: extrair a montagem repetida de ProblemDetail`
7. `fix: extrair o campo inválido pela causa Jackson, sem regex na mensagem`
8. `refactor: nivelar log por faixa de status e padronizar em Slf4j`
9. `refactor: mover a hierarquia de exceções para o core`
10. `refactor: mover o handler e o ProblemDetails para o web`
11. `refactor: remover o módulo scos-foundation-exception do reactor`
