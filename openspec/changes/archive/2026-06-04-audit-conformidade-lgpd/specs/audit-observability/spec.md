## ADDED Requirements

### Requirement: Gauge de profundidade da fila
O `ScosAuditBatchConsumer` SHALL registrar um Micrometer `Gauge` chamado `audit.queue.depth` que reflete o tamanho atual da `ConcurrentLinkedQueue` em tempo real.

#### Scenario: Gauge reflete fila vazia
- **WHEN** a fila está vazia
- **THEN** `audit.queue.depth` reporta `0`

#### Scenario: Gauge reflete fila com eventos
- **WHEN** 250 eventos estão na fila aguardando drain
- **THEN** `audit.queue.depth` reporta `250`

### Requirement: DistributionSummary do tamanho de lote
O `ScosAuditBatchConsumer` SHALL registrar um Micrometer `DistributionSummary` chamado `audit.batch.size` registrando o tamanho de cada lote enviado ao `saveAll`.

#### Scenario: Lote de 100 registra tamanho 100
- **WHEN** `saveAll` é chamado com 100 eventos
- **THEN** `audit.batch.size` registra observação de valor `100`

#### Scenario: Lote por flush-interval registra tamanho real
- **WHEN** drain ocorre por tempo e há apenas 17 eventos na fila
- **THEN** `audit.batch.size` registra observação de valor `17`

### Requirement: Counter de eventos enviados à DLQ
O `ScosAuditBatchConsumer` SHALL registrar um Micrometer `Counter` chamado `audit.events.dlq` incrementado a cada evento enviado à tabela `SFA_AUDIT_DLQ`.

#### Scenario: Counter incrementa por evento na DLQ
- **WHEN** um lote de 30 eventos falha em todos os retries e vai para DLQ
- **THEN** `audit.events.dlq` é incrementado em 30

#### Scenario: Counter não incrementa em operação normal
- **WHEN** todos os lotes persistem com sucesso
- **THEN** `audit.events.dlq` permanece em `0`

### Requirement: Métricas opcionais via ObjectProvider
As métricas SHALL ser registradas via `ObjectProvider<MeterRegistry>`. Se `MeterRegistry` não estiver no classpath (sem Spring Actuator), a pipeline SHALL funcionar sem erro — as métricas são sem-op.

#### Scenario: MeterRegistry ausente — pipeline funciona normalmente
- **WHEN** `MeterRegistry` não está disponível no contexto Spring
- **THEN** `ScosAuditBatchConsumer` inicia sem exceção e opera sem emitir métricas
