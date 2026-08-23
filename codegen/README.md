# SCOS Foundation Codegen

Módulo só de recursos — sem código Java, sem dependência de runtime. Guarda os templates
[Mustache](https://mustache.github.io/) customizados usados pelo
[OpenAPI Generator](https://openapi-generator.tech/) para gerar controllers, delegates, DTOs e
validação Bean Validation consistentes com as anotações `@Scos*` da foundation (hoje em `web`).

## Templates

| Arquivo | Gera |
|---|---|
| `api.mustache` | Interface com assinaturas de método e `@ScosRequest*` |
| `apiController.mustache` | Classe que delega para o `Delegate` |
| `apiDelegate.mustache` | Implementação padrão que lança `MethodNotImplementedException` |
| `responseType.mustache` | Tipo de retorno do método (ex: `ScosResponseDTO<UserDTO>`) |
| `pojo.mustache` / `model.mustache` | DTOs de request/response |
| `beanValidation.mustache` / `beanValidationCore.mustache` | Anotações de Bean Validation nos campos gerados |
| `bodyParams.mustache` / `pathParams.mustache` / `queryParams.mustache` / `headerParams.mustache` / `dateTimeParam.mustache` | Parâmetros de request |

Mais contexto sobre o comportamento gerado por cada template:
[`etc/doc/ideia/rfc9457-exception-compliance.md`](../etc/doc/ideia/rfc9457-exception-compliance.md#impacto-na-geração-de-controllers-via-swagger-mustache).

## Como usar

Nenhum serviço deste reactor consome este módulo — ele existe para ser referenciado por serviços
consumidores externos que geram código a partir de um contrato OpenAPI. Um serviço consumidor
aponta o `templateDirectory` do `openapi-generator-maven-plugin` para os recursos extraídos deste
jar (`src/main/resources/mustaches/`), por exemplo desempacotando a dependência com o
`maven-dependency-plugin` antes da geração.
