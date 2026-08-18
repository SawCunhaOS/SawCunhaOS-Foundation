# Convenção de Commits — SCOS Foundation

Os commits seguem [Conventional Commits](https://www.conventionalcommits.org/). O
scope identifica o módulo afetado; o `git-cliff` agrupa o `CHANGELOG` e as release
notes por scope. **A convenção vale a partir do primeiro commit após o merge desta
mudança** — commits antigos sem scope aparecem na seção "Outros".

## Formato

```
type(scope): descrição no imperativo

[corpo opcional]

[footer opcional]
```

- `type` — natureza da mudança (ver tabela de tipos).
- `scope` — módulo afetado (ver tabela de scopes). Obrigatório quando a mudança
  pertence a um módulo; commits sem scope caem em "Outros".
- `descrição` — curta, no imperativo, sem ponto final.

## Tabela de scopes → módulo Maven

| Scope        | Módulo Maven                  | Quando usar                                  |
|--------------|-------------------------------|----------------------------------------------|
| `audit`      | `scos-foundation-audit`       | Auditoria, accountability, LGPD              |
| `privacy`    | `scos-foundation-privacy`     | Masking, anonimização, dados pessoais        |
| `exception`  | `scos-foundation-exception`   | Tratamento de erros, RFC 9457, handlers      |
| `utils`      | `scos-foundation-utils`       | Utilitários compartilhados                   |
| `jdempotent` | `scos-foundation-jdempotent`  | Idempotência                                 |
| `ci`         | —                             | Workflows, pipelines, automação CI/CD        |
| `build`      | —                             | Build, dependências, configuração Maven      |
| `docs`       | —                             | Documentação (READMEs, guias, skills)        |

## Tipos

| Type       | Seção no CHANGELOG |
|------------|--------------------|
| `feat`     | Features           |
| `fix`      | Correções          |
| `docs`     | Documentação       |
| `perf`     | Performance        |
| `refactor` | Refatoração        |
| `test`     | Testes             |
| `chore`    | Manutenção         |

## Exemplos

```
feat(audit): adicionar batch pipeline
fix(privacy): corrigir masking de CPF
docs(ci): documentar workflow de release notes
chore(build): atualizar versão do spring-boot
```

## Breaking changes

Indique uma quebra de compatibilidade de uma das duas formas:

1. Sufixo `!` após `type(scope)`:

   ```
   feat(security)!: remover endpoint deprecated /api/v1/auth
   ```

2. Footer `BREAKING CHANGE:`:

   ```
   feat(security): migrar para OAuth2 resource server

   BREAKING CHANGE: o header X-Auth-Token deixa de ser aceito; use Authorization: Bearer.
   ```

Commits breaking são destacados como **⚠ Breaking Changes** no CHANGELOG e nas
release notes.
