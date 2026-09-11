## web

## Where things are

- Tratamento de erros HTTP (RFC 9457): `ExceptionsHandler`, envolto por `ScosWebErrorHandlerAutoConfiguration` (toggle `scos.web.error-handler.enabled`, default `true`). O antigo módulo `exception` foi absorvido por `web` (HTTP) + `core` (contrato de domínio, `ScosException` etc.) na Story 2.9 — não existe mais no reactor.
