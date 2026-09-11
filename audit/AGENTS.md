## audit

## Conventions that differ from defaults

- `@Auditable`/`@AuditAction` vêm do contrato `audit-api`, não de `audit` nem de `utils` (módulo removido).

## Known pitfalls

- `README.md` deste módulo ainda mostra `import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable` (linhas 92, 111-112) — pacote `utils` foi removido (Story 1.14). O import real é `br.com.sawcunhaos.foundation.audit.api.Auditable`/`AuditAction`. Não copie o exemplo do README sem corrigir.
