# Skills de configuração — módulos scos-foundation

Uma skill por módulo, para acelerar a configuração de **novos sistemas** que consomem a foundation.
Cada `SKILL.md` cobre: dependência Maven, ativação, chaves de `application.yml`, beans de override e
exemplo de uso.

| Skill | Módulo | Ativação | Config principal |
|---|---|---|---|
| [scos-utils-config](scos-utils-config/SKILL.md) | `scos-foundation-utils` | `@ComponentScan("br.com.sawcunhaos")` | filtros de log, `scos.cache.*`, validadores |
| [scos-exception-config](scos-exception-config/SKILL.md) | `scos-foundation-exception` | component scan | `@ControllerAdvice` RFC 9457, i18n |
| [scos-privacy-config](scos-privacy-config/SKILL.md) | `scos-foundation-privacy` | auto-config (`scos.privacy.enabled`, default on) | `privacy-masking.yml`, `%mask` |
| [scos-audit-config](scos-audit-config/SKILL.md) | `scos-foundation-audit` | `scos.audit.enabled=true` | `spring.datasource.audit.*`, `@Auditable` |
| [scos-jdempotent-config](scos-jdempotent-config/SKILL.md) | `scos-foundation-jdempotent` | auto-config (`scos.jdempotent.enabled`, default on) | Redis, `@JdempotentResource` |
| [scos-security-config](scos-security-config/SKILL.md) | `scos-foundation-security` | component scan | JWT issuer, `scos.security.datasource.*`, CORS |

## Ordem de setup num sistema novo

1. **utils** — base. Adicionar dep + `@SpringBootApplication` com `@ComponentScan("br.com.sawcunhaos")`.
   Puxa `privacy` transitivo.
2. **exception** — sobe junto pelo component scan; padroniza erros.
3. **privacy** — já no classpath via utils; ajustar `privacy-masking.yml` e `scos.privacy.*`.
4. **audit** — se houver trilha: dep + `scos.audit.enabled=true` + datasource `spring.datasource.audit.*`.
5. **jdempotent** — se houver idempotência: dep + Redis + `@JdempotentResource`.
6. **security** — se houver auth: dep + `issuer-uri` JWT + `scos.security.datasource.*` + CORS.

Padrões transversais (todos): `@ComponentScan("br.com.sawcunhaos")` no app, BOM `scos-bom` gerenciando
versões (declarar deps **sem** `<version>`), pacote `br.com.sawcunhaos.<sistema>`.

> Estes arquivos são documentação de configuração. Para que o Claude Code os carregue como skills
> ativáveis, copie/linke as pastas para `.claude/skills/`.
