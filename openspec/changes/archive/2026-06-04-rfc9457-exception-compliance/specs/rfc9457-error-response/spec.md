## ADDED Requirements

### Requirement: Error response body SHALL conform to RFC 9457 ProblemDetail format
Toda resposta de erro (`4xx`/`5xx`) retornada pela foundation SHALL usar o formato `ProblemDetail` da RFC 9457 como root object, sem wrapper `data`. Os campos obrigatórios `type`, `title`, `status`, `detail` e `instance` SHALL estar presentes em todas as respostas de erro. Os campos de extensão SCOS `code`, `requestId` e `timestamp` SHALL estar presentes em todas as respostas. O campo `errors` SHALL estar presente apenas em erros de validação.

#### Scenario: Validation error returns RFC 9457 compliant body
- **WHEN** um `MethodArgumentNotValidException` é lançado para `POST /api/users` com campo `email` inválido e campo `name` em branco
- **THEN** a response SHALL ter status HTTP 400
- **THEN** o `Content-Type` SHALL ser `application/problem+json`
- **THEN** o body SHALL conter `type` com URI `https://docs.sawcunhaos.com.br/problems/validation-error`
- **THEN** o body SHALL conter `title` igual a `"Validation Error"`
- **THEN** o body SHALL conter `status` igual a `400`
- **THEN** o body SHALL conter `detail` com mensagem localizada de validação
- **THEN** o body SHALL conter `instance` igual a `"/api/users"`
- **THEN** o body SHALL conter `code` igual a `"SCOS-001"`
- **THEN** o body SHALL conter `errors` com dois itens: `{"pointer": "#/email", "detail": "deve ser um e-mail válido"}` e `{"pointer": "#/name", "detail": "não deve estar em branco"}`
- **THEN** o body SHALL conter `requestId` com valor UUID
- **THEN** o body SHALL conter `timestamp` em formato ISO-8601 UTC

#### Scenario: Business error returns RFC 9457 compliant body
- **WHEN** uma `ScosException` é lançada com código `SCOS-007` e mensagem "CPF informado é inválido" em `POST /api/persons`
- **THEN** a response SHALL ter status HTTP 400
- **THEN** o `Content-Type` SHALL ser `application/problem+json`
- **THEN** o body SHALL conter `type` com URI correspondente ao código do erro
- **THEN** o body SHALL conter `title` correspondente ao tipo de erro
- **THEN** o body SHALL conter `status` igual a `400`
- **THEN** o body SHALL conter `detail` igual à mensagem localizada da exceção
- **THEN** o body SHALL conter `code` igual a `"SCOS-007"`
- **THEN** o body NÃO SHALL conter o campo `errors`
- **THEN** o body SHALL conter `requestId` e `timestamp`

#### Scenario: Access denied returns RFC 9457 compliant body
- **WHEN** `ScosSecurityException` de acesso negado é lançada em `GET /api/admin/users`
- **THEN** a response SHALL ter status HTTP 403
- **THEN** o `Content-Type` SHALL ser `application/problem+json`
- **THEN** o body SHALL conter `type` com URI `https://docs.sawcunhaos.com.br/problems/access-denied`
- **THEN** o body SHALL conter `title` igual a `"Access Denied"`
- **THEN** o body SHALL conter `status` igual a `403`
- **THEN** o body SHALL conter `code` com código de erro de segurança
- **THEN** o body SHALL conter `requestId` e `timestamp`

#### Scenario: Internal server error returns RFC 9457 compliant body
- **WHEN** uma exceção não tratada é lançada durante processamento de request
- **THEN** a response SHALL ter status HTTP 500
- **THEN** o body SHALL conter `type` com URI `https://docs.sawcunhaos.com.br/problems/internal-error`
- **THEN** o body SHALL conter `title` igual a `"Internal Server Error"`
- **THEN** o body SHALL conter `status` igual a `500`
- **THEN** o body NÃO SHALL expor stack trace ou detalhes internos no campo `detail`

### Requirement: Current error data SHALL be fully preserved in new format
Nenhum dado retornado atualmente SHALL ser descartado. Os campos `message`, `codeError` e `validationErrors` do formato atual SHALL ser mapeados para seus equivalentes RFC 9457, sem perda de informação. A regra de mapeamento é: `data.message` → `detail`, `data.codeError` → `code`, `data.validationErrors[].attribute` → `errors[].pointer` (prefixar `#/`), `data.validationErrors[].message` → `errors[].detail`.

#### Scenario: message field is preserved as detail
- **WHEN** `ExceptionsHandler` processa uma `ScosException` com mensagem "CPF informado é inválido"
- **THEN** o campo `detail` da response SHALL conter a mesma mensagem localizada que hoje estaria em `data.message`

#### Scenario: codeError field is preserved as code
- **WHEN** `ExceptionsHandler` processa uma `ScosException` com código `SCOS-007`
- **THEN** o campo `code` da response SHALL conter `"SCOS-007"`, o mesmo valor que hoje estaria em `data.codeError`

#### Scenario: validationErrors attribute is converted to JSON Pointer
- **WHEN** `MethodArgumentNotValidException` contém erro no campo `email`
- **THEN** o item correspondente em `errors[]` SHALL ter `pointer` igual a `"#/email"`

#### Scenario: validationErrors message is preserved as errors detail
- **WHEN** `MethodArgumentNotValidException` contém erro no campo `email` com mensagem "deve ser um e-mail válido"
- **THEN** o item correspondente em `errors[]` SHALL ter `detail` igual a `"deve ser um e-mail válido"`

#### Scenario: nested field attribute is converted to nested JSON Pointer
- **WHEN** `MethodArgumentNotValidException` contém erro no campo aninhado `address.street`
- **THEN** o item correspondente em `errors[]` SHALL ter `pointer` igual a `"#/address/street"`

### Requirement: All three error handlers SHALL return unified ProblemDetail format
Os handlers `ExceptionsHandler`, `AccessDeniedExceptionHandler` e `ExceptionHandlerFilter` SHALL retornar respostas com o mesmo formato `ProblemDetail`. Não SHALL existir diferença estrutural entre erros de negócio, erros de acesso e erros de segurança.

#### Scenario: ExceptionsHandler returns ProblemDetail
- **WHEN** `ExceptionsHandler` processa qualquer exceção
- **THEN** o tipo de retorno SHALL ser `ResponseEntity<ProblemDetail>`
- **THEN** o `Content-Type` SHALL ser `application/problem+json`
- **THEN** o wrapper `ScosResponseDTO` NÃO SHALL estar presente no body

#### Scenario: AccessDeniedExceptionHandler returns ProblemDetail
- **WHEN** `AccessDeniedExceptionHandler` processa `AccessDeniedException`
- **THEN** a estrutura do body SHALL ser idêntica à do `ExceptionsHandler`
- **THEN** o `Content-Type` SHALL ser `application/problem+json`

#### Scenario: ExceptionHandlerFilter returns ProblemDetail
- **WHEN** `ExceptionHandlerFilter` captura `ScosSecurityException` antes da cadeia de filtros
- **THEN** a estrutura do body SHALL ser idêntica à do `ExceptionsHandler`
- **THEN** o `Content-Type` da response SHALL ser definido como `application/problem+json` antes de escrever o body

### Requirement: ExceptionCode interface SHALL carry RFC 9457 metadata
A interface `ExceptionCode` SHALL expor métodos `getType()` e `getTitle()` com implementações `default` que derivam valores das URIs da foundation. Implementadores existentes NÃO SHALL ser obrigados a sobrescrever esses métodos. `ScosExceptionCode` SHALL sobrescrever `getTitle()` para cada categoria de erro.

#### Scenario: ExceptionCode default getType derives URI from code
- **WHEN** `ExceptionCode.getType()` é chamado em um código que não sobrescreve o método
- **THEN** o retorno SHALL ser uma URI no formato `https://docs.sawcunhaos.com.br/problems/<code-lowercase-hifenizado>`

#### Scenario: ExceptionCode default getTitle returns generic title
- **WHEN** `ExceptionCode.getTitle()` é chamado em um código que não sobrescreve o método
- **THEN** o retorno SHALL ser `"Error"`

#### Scenario: ScosExceptionCode overrides getTitle for validation errors
- **WHEN** `ScosExceptionCode.ATTRIBUTE_NOT_VALID.getTitle()` é chamado
- **THEN** o retorno SHALL ser `"Validation Error"`

### Requirement: ScosFieldError record SHALL use JSON Pointer format for field identification
O record `ScosFieldError` (renomeado de `FieldError`) SHALL usar o campo `pointer` com formato JSON Pointer (RFC 6901) e o campo `detail` para a mensagem de validação. O nome `ScosFieldError` SHALL evitar conflito com `org.springframework.validation.FieldError`.

#### Scenario: ScosFieldError uses pointer and detail fields
- **WHEN** `ScosFieldError` é instanciado para o campo `email` com mensagem "deve ser um e-mail válido"
- **THEN** `scosFieldError.pointer()` SHALL retornar `"#/email"`
- **THEN** `scosFieldError.detail()` SHALL retornar `"deve ser um e-mail válido"`

#### Scenario: ScosFieldError can be imported alongside Spring FieldError
- **WHEN** `ExceptionsHandler` usa `ScosFieldError` e `org.springframework.validation.FieldError` no mesmo arquivo
- **THEN** NÃO SHALL existir conflito de import entre os dois tipos
