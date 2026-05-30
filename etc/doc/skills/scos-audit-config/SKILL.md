---
name: scos-audit-config
description: >
  Configurar o módulo scos-foundation-audit num sistema consumidor SCOS — trilha de auditoria via
  @Auditable + listener Hibernate, datasource dedicado (spring.datasource.audit.*), Liquibase próprio,
  executor @Async e cifra em repouso de PII (auditEncryptFields via privacy). Use ao auditar entidades
  ou ao configurar o banco/Liquibase da trilha.
---

# Configuração — `scos-foundation-audit`

Registra INSERT/UPDATE/DELETE de entidades anotadas em uma trilha (`ScosAuditLog`, JSONB
`entityOld`/`entityNew`) via evento Hibernate, em **datasource separado** e thread `@Async`.

## 1. Dependência

```xml
<dependency>
  <groupId>br.com.sawcunhaos</groupId>
  <artifactId>scos-foundation-audit</artifactId>
</dependency>
```

Depende de `utils` e `privacy`. Auto-configura por `AutoConfiguration.imports`.

## 2. Ativação + datasource dedicado

Ligado por `scos.audit.enabled=true`. A trilha usa um datasource **próprio** (`spring.datasource.audit`),
separado do datasource da aplicação.

```yaml
scos:
  audit:
    enabled: true
    system: MEU_SISTEMA            # preenche originSystem na trilha
    liquibase:
      enabled: true
      change-log: classpath:/db/changelog/db.audit.changelog-master.yaml
spring:
  datasource:
    audit:
      url: jdbc:postgresql://localhost:5432/audit
      username: audit
      password: ${AUDIT_DB_PASSWORD}
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 10
```

`scos.audit.liquibase.*` estende as chaves base de Liquibase (`change-log`, `default-schema`,
`database-change-log-table`, `contexts`, `labels`, …).

## 3. Auditar uma entidade

```java
@Entity
@Table(name = "SFA_COMPANY")
@Auditable
public class Company { /* ... */ }
```

Cada persistência gera um `ScosAuditLog` (actionType, entity, idEntity, entityOld/New, user, originSystem,
ipAddress, xRequestId). A escrita ocorre na thread `ScosAuditLogAsyncExecutor` (`@Async`).

## 4. Cifra em repouso de PII (opt-in, via `privacy`)

Campos listados em `audit-encrypt-fields` (no `privacy-masking.yml`) são cifrados antes de gravar o JSONB,
gerando tokens `enc:vN:`. Exige a chave do `privacy`:

```yaml
scos:
  privacy:
    crypto:
      secret: ${SCOS_PRIVACY_CRYPTO_SECRET}
    masking:
      # privacy-masking.yml
      # audit-encrypt-fields: [cpf, email]
```

Lista vazia = comportamento atual (texto em claro). Rotação: chave nova só afeta registros novos; histórico
é decifrado pelo `keyId` embutido no token. Ver `scos-privacy-config`.

## Pegadinhas

- `scos.audit.enabled=true` é **obrigatório** (sem `matchIfMissing`); sem isso os beans não sobem.
- O datasource é `spring.datasource.audit.*` (separado) — não reaproveita o datasource principal.
- Cifra só atua se houver `scos.privacy.crypto.secret` **e** `audit-encrypt-fields` não vazio.
- A entidade precisa de `@Auditable`; sem a anotação não é auditada.
