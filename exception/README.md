# SCOS Foundation Exception

Módulo de tratamento centralizado de exceções: `ExceptionsHandler` (`@ControllerAdvice`) unifica
toda resposta de erro no formato RFC 9457 (`ProblemDetail` / `ScosProblemDetails`,
`Content-Type: application/problem+json`).

## Pré-requisito para 404 unificado de rota inexistente

Por padrão, quando nenhuma rota (`@RequestMapping`) casa com a requisição, o `DispatcherServlet`
do Spring Boot **não lança exceção alguma** — ele delega para o handler de recurso estático
padrão (`ResourceHttpRequestHandler`, mapeado em `/**`), que responde a página de erro
Whitelabel/JSON **nativa do Boot**, sem passar pelo `@ControllerAdvice` desta biblioteca. Ou
seja: mesmo com `ExceptionsHandler#handleExceptionInternal` sobrescrito (Story 2.7), uma rota
inexistente continua devolvendo o formato nativo do Spring em vez de `ScosProblemDetails`, a
menos que a aplicação consumidora ligue explicitamente a propriedade:

```yaml
spring:
  mvc:
    throw-exception-if-no-handler-found: true
```

Com essa propriedade `true`, o Spring passa a lançar `NoResourceFoundException` (Spring
Framework 6.1+) para uma rota genuinamente inexistente, que **flui através do
`@ControllerAdvice`** e é capturada pelo `handleExceptionInternal` sobrescrito desta biblioteca
— só então o 404 de rota inexistente sai no mesmo formato `ScosProblemDetails` de qualquer
outro erro.

### Por que este módulo não liga essa propriedade sozinho

`spring.mvc.throw-exception-if-no-handler-found` é consumida pela `WebMvcAutoConfiguration` do
Spring Boot, que configura o `DispatcherServlet` da **aplicação hospedeira** — não deste módulo.
Uma biblioteca (`@ControllerAdvice`) não tem acesso ao `DispatcherServlet` para alterar essa
configuração; ela só intercepta exceções que o `DispatcherServlet` decide lançar. Portanto, cada
aplicação consumidora precisa ligar essa propriedade explicitamente no seu próprio
`application.yml`/`.properties`.

### O que acontece sem essa propriedade

- Rota inexistente → página de erro Whitelabel/JSON **padrão do Boot** (não
  `ScosProblemDetails`), pois o `DispatcherServlet` nunca lança exceção para o
  `@ControllerAdvice` capturar.
- Método HTTP não suportado (`HttpRequestMethodNotSupportedException`) e `Accept` inválido
  (`HttpMediaTypeNotAcceptableException`) **não dependem** dessa propriedade — já são cobertos
  pelo `handleExceptionInternal` desta biblioteca independentemente da configuração acima, pois
  o Spring já lança exceção para esses dois casos por padrão quando a rota existe.

## Uso

Veja a seção "SCOS Foundation Exception" no [README raiz do projeto](../README.md) para
exemplos completos de `ScosException`, códigos de erro customizados e mensagens
internacionalizadas.
