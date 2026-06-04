# audit-batch-pipeline Specification

## Purpose
TBD - created by archiving change audit-conformidade-lgpd. Update Purpose after archive.
## Requirements
### Requirement: Eventos de audit enfileirados de forma assíncrona
O `ScosAuditServiceBean` SHALL enfileirar o evento em uma `ConcurrentLinkedQueue<ScosAuditLog>` via `@Async` em vez de chamar `saveLog` diretamente. O thread Hibernate não SHALL ser bloqueado pela persistência.

#### Scenario: Evento C/U/D enfileirado com sucesso
- **WHEN** um `PostInsertEvent`, `PostUpdateEvent` ou `PostDeleteEvent` é recebido pelo listener
- **THEN** o evento é convertido em `ScosAuditLog` e adicionado à fila em memória sem bloquear o thread Hibernate

#### Scenario: Fila com capacidade configurável
- **WHEN** `scos.audit.performance.queue-capacity` está definido
- **THEN** a fila aceita no máximo esse número de elementos; ao ultrapassar, o evento excedente é enviado diretamente à DLQ sem bloquear o negócio

### Requirement: Consumer drena a fila em lotes
O `ScosAuditBatchConsumer` SHALL rodar em uma virtual thread dedicada e drenar a fila quando `batch-size` for atingido **ou** `flush-interval-ms` expirar, o que vier primeiro.

#### Scenario: Drain por tamanho de lote
- **WHEN** a fila acumula N eventos iguais ao `scos.audit.performance.batch-size`
- **THEN** o consumer drena exatamente N eventos e chama `saveAll(lote)` em uma única transação

#### Scenario: Drain por intervalo de tempo
- **WHEN** `scos.audit.performance.flush-interval-ms` expirar e a fila tiver pelo menos 1 evento
- **THEN** o consumer drena todos os eventos disponíveis (até `batch-size`) e chama `saveAll(lote)`

#### Scenario: Fila vazia não dispara saveAll
- **WHEN** `flush-interval-ms` expirar e a fila estiver vazia
- **THEN** nenhuma chamada ao banco é realizada

### Requirement: Retry com backoff em falha de persistência do lote
Quando `saveAll` falhar, o consumer SHALL realizar retry com backoff exponencial até `scos.audit.durability.retry-max` tentativas.

#### Scenario: Falha transiente — retry bem-sucedido
- **WHEN** `saveAll` lançar exceção e o banco voltar a responder antes de esgotar as tentativas
- **THEN** o lote é persistido com sucesso em uma tentativa subsequente

#### Scenario: Falha persistente — envio para DLQ
- **WHEN** `saveAll` falhar em todas as `retry-max` tentativas
- **THEN** todos os eventos do lote são inseridos na tabela `SFA_AUDIT_DLQ` com o erro registrado

### Requirement: DLQ reprocessada periodicamente
Um `@Scheduled` job SHALL reprocessar a tabela `SFA_AUDIT_DLQ` em batches de no máximo 50 registros por execução.

#### Scenario: Reprocessamento bem-sucedido da DLQ
- **WHEN** o job de reprocessamento executa e `SFA_AUDIT_DLQ` tem registros
- **THEN** os registros são movidos para `SFA_LOG_AUDIT` e deletados de `SFA_AUDIT_DLQ`

#### Scenario: Reprocessamento falha — registro permanece na DLQ
- **WHEN** o job de reprocessamento tenta persistir um registro da DLQ e falha
- **THEN** o `retry_count` do registro é incrementado; o registro permanece na DLQ

### Requirement: Configuração da pipeline via properties
O sistema SHALL expor as seguintes properties com valores default funcionais:

| Property | Default | Descrição |
|---|---|---|
| `scos.audit.performance.queue-capacity` | `10000` | Capacidade máxima da fila em memória |
| `scos.audit.performance.batch-size` | `100` | Eventos por lote no `saveAll` |
| `scos.audit.performance.flush-interval-ms` | `500` | Intervalo máximo de drain em ms |
| `scos.audit.durability.retry-max` | `3` | Tentativas antes de ir para DLQ |
| `scos.audit.durability.dlq-enabled` | `true` | Ativa/desativa a tabela DLQ |

#### Scenario: Defaults preservam comportamento quando não configurados
- **WHEN** nenhuma property `scos.audit.performance.*` é definida pelo consumidor
- **THEN** a pipeline opera com os valores default sem erro de inicialização

