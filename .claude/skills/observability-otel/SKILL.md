---
name: observability-otel
description: >
  Add and review observability — metrics, traces, and logs — in SCOS Spring Boot
  services using Micrometer and OpenTelemetry (OTLP). Use this skill whenever the
  user wants tracing/metrics/structured logging, mentions OpenTelemetry, OTLP,
  Micrometer, Prometheus, Grafana/Tempo/Loki, distributed tracing, correlation
  IDs, custom metrics, spans, sampling, or wants to make a service observable or
  review its instrumentation — even if they don't name the tool. Targets the
  current stable Spring Boot 4.0.x (the new spring-boot-starter-opentelemetry),
  Micrometer, Java 25. Follows the SCOS conventions (services as
  specification+Bean; the foundation already provides request-logging filters and
  GELF/Logstash structured logging).
---

# Observability with Micrometer + OpenTelemetry (Spring Boot 4.0.x)

This skill instruments and reviews the three pillars — metrics, traces, logs — for
SCOS services. The model to internalize: Micrometer is the instrumentation API
the Spring portfolio uses; OTLP is the export protocol. You instrument with
Micrometer (or the Observation API) and export to any OpenTelemetry-capable
backend (Grafana LGTM, etc.) via OTLP. Spring Boot 4 collapsed the old pile of
dependencies into a single `spring-boot-starter-opentelemetry`.

## First step: decide the mode

- Adding instrumentation/wiring to a service → **Generate**.
- Reviewing existing observability for gaps → **Review**.

Read `references/otel-setup.md` (dependency, config, correlation, custom
signals) before producing substantial output.

## Generate

1. Add `spring-boot-starter-opentelemetry` (it pulls Micrometer + OTLP exporters).
   Let the SCOS BOM manage versions; prefer Boot 4.0.1+ for the RestClient
   auto-instrumentation fixes.
2. Configure OTLP export and sampling (see `references/otel-setup.md`):
   endpoint, `management.tracing.sampling.probability` (1.0 in dev, ~0.1 in prod),
   and the service name/resource attributes.
3. Lean on auto-instrumentation first — incoming HTTP, RestClient/HTTP interface
   clients, JDBC, etc. are traced without code. Add custom signals only where the
   auto layer doesn't answer a real question.
4. Custom metrics/traces go inside the application-service `Bean` (SCOS style):
   inject `MeterRegistry` for counters/timers, or use the `ObservationRegistry` /
   `@Observed` for spans around business operations. Name metrics with a stable,
   dotted convention (`orders.placed`).
5. Correlate logs with traces. Spring Boot does NOT bridge Logback/Log4j2 to OTel
   automatically, so either add the OpenTelemetry Logback appender, or put
   trace/span IDs into MDC and emit structured logs. The foundation already ships
   GELF/Logstash structured logging and request-logging filters
   (`LoggingInitialFilter`/`LoggingFinalFilter`, `xRequestId`) — wire the trace ID
   into that structured output rather than inventing a parallel scheme.
6. Expose Actuator health/info and, if scraping, the metrics endpoint; keep
   liveness/readiness probe groups on for orchestration.

## Review

Assess against `references/otel-setup.md`'s checklist. The recurring gaps:
no tracing at all (so cross-service debugging is guesswork), logs that can't be
correlated to traces (no trace ID), sampling at 100% in production (cost/perf),
and metrics that are vanity counts rather than tied to SLOs (latency, error
rate, saturation).

Frame as a one-paragraph health summary, then impact-ordered findings (no
traces/metrics > no log↔trace correlation > bad sampling/cardinality > naming),
each with a concrete fix.

## What's worth measuring (so you don't drown in signals)

Anchor on the things teams actually alert on: request latency (p95/p99), error
rate, throughput, and resource saturation; plus a few business metrics that
matter (orders placed, payments failed). Avoid high-cardinality tags (user IDs,
raw URLs with IDs) — they explode metric storage. More dashboards is not more
observability; signals tied to questions you'll ask during an incident are.
