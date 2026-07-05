# OpenTelemetry setup, signals & review checklist

Spring Boot 4.0.x, Micrometer, OTLP. The protocol (OTLP) is what matters — you
can point at any OTel-capable backend (Grafana Tempo/Mimir/Loki, etc.).

## Dependency

One starter replaces the old Boot 3 dependency pile:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

Version comes from the SCOS BOM. Use Boot 4.0.1+ (RestClient auto-instrumentation
fixes). For Prometheus scraping instead of OTLP push, add
`micrometer-registry-prometheus` and expose the Actuator `prometheus` endpoint.

## Configuration

```yaml
spring:
  application:
    name: ordering-service          # becomes the service.name resource attribute
management:
  otlp:
    tracing:
      endpoint: ${OTLP_ENDPOINT:http://otel-collector:4317}
    metrics:
      export:
        enabled: true
  tracing:
    sampling:
      probability: ${TRACE_SAMPLING:0.1}   # 1.0 in dev, lower in prod
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      probes:
        enabled: true
```

Resource attributes (service name/version/namespace) can also be set via the
OpenTelemetry environment variables Spring Boot honors. Keep the endpoint and
sampling rate externalized.

## Custom signals (SCOS style — inside the service Bean)

```java
@Service("PlaceOrderService")
@RequiredArgsConstructor
public class PlaceOrderServiceBean implements PlaceOrderService {

    private final MeterRegistry meters;
    private final ObservationRegistry observations;
    private final OrderRepository orders;

    @Override
    public OrderId place(PlaceOrderCommand cmd) {
        return Observation.createNotStarted("orders.place", observations).observe(() -> {
            var id = /* ... domain work ... */ ;
            meters.counter("orders.placed", "channel", cmd.channel()).increment();
            return id;
        });
    }
}
```

Prefer auto-instrumentation for HTTP/JDBC; reserve custom spans for meaningful
business operations. Use the `Observation` API rather than hand-rolling spans —
it produces both a trace span and timing metrics from one call.

## Log ↔ trace correlation

Spring Boot auto-configures `SdkLoggerProvider` but does NOT bridge Logback/Log4j2
to it. Two workable approaches:
- Add the OpenTelemetry Logback appender (`opentelemetry-logback-appender-1.0`) to
  ship logs via OTLP with trace context attached.
- Or keep the foundation's structured logging (GELF/Logstash) and ensure the
  trace/span IDs are in the MDC so each log line carries `traceId`/`spanId`. The
  foundation's request-logging filters and `xRequestId` already give you a
  correlation handle; add the OTel trace ID alongside it.

Either way, the goal: from a slow trace in Tempo you can jump to the exact logs,
and from a log line you can find the trace.

## Review checklist (impact order)

### 1. Are the pillars present?
- Is distributed tracing enabled at all (OTLP endpoint + sampling configured)?
- Are core metrics exported (latency, error rate, throughput)?
- Are logs structured (not plain text) so they're queryable?

### 2. Correlation
- Do log lines carry `traceId`/`spanId` (or `xRequestId`) so logs ↔ traces ↔
  metrics line up? Without this, the three pillars are three silos.

### 3. Sampling & cost
- Is production sampling sane (not 100%)? Is dev 1.0 for full visibility?
- Any unbounded metric cardinality (tags with user IDs, raw paths, UUIDs)?

### 4. Signal quality
- Are metrics tied to things you'd alert on, or vanity counters?
- Are custom spans on meaningful operations, or noise around trivial calls?
- Is the service name / resource attributes set so signals are attributable?

### Output format for a review
1. **Health summary** — one paragraph: during an incident, could you actually see
   what's happening?
2. **Findings** — impact-ordered, each with why + concrete fix.
3. **What's solid** — 1–3 things worth keeping.
