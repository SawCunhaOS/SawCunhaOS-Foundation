---
name: scos-exception-config
description: >
  Configurar o módulo scos-foundation-exception num sistema consumidor SCOS — tratamento central de erros
  via ExceptionsHandler (@ControllerAdvice, RFC 9457 ProblemDetail + MDC), família ScosException e bundle
  de mensagens i18n. Use ao montar a camada web de um serviço SCOS ou ao padronizar respostas de erro.
---

# Configuração — `scos-foundation-exception`

Tratamento central de exceções da camada web: converte exceções em **RFC 9457** (`ProblemDetail`), inclui
`X-Request-ID` do MDC e resolve mensagens por bundle i18n.

## 1. Dependência

```xml
<dependency>
  <groupId>br.com.sawcunhaos</groupId>
  <artifactId>scos-foundation-exception</artifactId>
</dependency>
```

## 2. Ativação

`ExceptionsHandler` é um `@ControllerAdvice` (estende `ResponseEntityExceptionHandler`). Sobe pelo
**component scan** do app — exige `@ComponentScan(basePackages = {"br.com.sawcunhaos"})` (ver
`scos-utils-config`). Sem config `scos.*` própria.

## 3. Mensagens i18n

O bundle padrão acompanha o módulo. Sobrescreva criando `messages.properties` /
`messages_<locale>.properties` no classpath do app e apontando o `MessageSource`:

```yaml
spring:
  messages:
    basename: messages
    encoding: UTF-8
```

## 4. Uso

Lance exceções da família `ScosException` (código + mensagem i18n); o handler mapeia para `ProblemDetail`:

```java
throw new ScosException(ScosExceptionCode.NOT_FOUND, "empresa.nao.encontrada");
```

Resposta (RFC 9457):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Empresa não encontrada",
  "instance": "/api/empresas/42",
  "requestId": "e908494b-fee2-4cf1-b169-57d4b65f43e2"
}
```

## Pegadinhas

- Sem `@ComponentScan("br.com.sawcunhaos")` o `@ControllerAdvice` não é registrado e os erros caem no
  handler padrão do Spring.
- Erros de validação (`@Valid`) e de parsing já são tratados pelo handler — não duplicar `@ExceptionHandler`
  no app para os mesmos casos.
