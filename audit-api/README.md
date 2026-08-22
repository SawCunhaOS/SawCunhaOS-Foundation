# scos-foundation-audit-api

Este artefato não executa nada; a implementação é `scos-foundation-audit`.

Módulo de contrato puro: contém apenas a anotação `@Auditable` e o enum `AuditAction` que ela
referencia. Sem dependência de runtime além do JDK — pensado para um módulo de domínio (ex.:
SCOS-Flow) anotar suas entidades/métodos sem carregar Spring/Hibernate.

Depender só deste módulo compila, mas não audita nada em tempo de execução: sem
`scos-foundation-audit` no classpath, não há listener para ler a anotação. Nenhum erro, nenhum
aviso — inclua sempre o módulo de implementação junto.

## Conteúdo

| Tipo | Descrição |
|---|---|
| `@Auditable` | Marca uma entidade (auditoria automática de C/U/D via Hibernate listener) ou um método (auditoria manual de leitura) |
| `AuditAction` | Enum `INSERT`, `UPDATE`, `DELETE`, `READ` — usado pelo atributo `action()` de `@Auditable` |

## Regra de fronteira

Uma regra ArchUnit local (`ArchitectureTest`) falha o build se qualquer classe deste módulo não
for `@interface`/`enum`, ou se qualquer dependência além do JDK for introduzida.
