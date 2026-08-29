<!-- bmad:context -->
<!-- Verified 2026-08-29 against 4dcab58. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->

## web

## Where things are

- Tratamento de erros HTTP (RFC 9457): `ExceptionsHandler`, envolto por `ScosWebErrorHandlerAutoConfiguration` (toggle `scos.web.error-handler.enabled`, default `true`). O antigo módulo `exception` foi absorvido por `web` (HTTP) + `core` (contrato de domínio, `ScosException` etc.) na Story 2.9 — não existe mais no reactor.

<!-- /bmad:context -->
