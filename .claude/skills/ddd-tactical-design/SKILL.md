---
name: ddd-tactical-design
description: >
  Apply Domain-Driven Design tactical patterns to Java/Spring Boot code — both
  to generate new code and to review existing code. Use this skill whenever the
  user is structuring a Spring Boot service around a domain model, mentions
  aggregates, value objects, entities, domain events, repositories, bounded
  contexts, hexagonal/ports-and-adapters/clean architecture, or anemic domain
  models — even if they don't say "DDD" explicitly. Also use it when the user
  asks to review whether a service "follows DDD", to refactor an anemic model,
  or to decide where a piece of logic belongs (domain vs application vs
  infrastructure). Targets the current stable Spring Boot 4.0.x / Spring
  Framework 7 / Java 25.
---

# DDD Tactical Design (Java 25 / Spring Boot 4.0.x)

This skill helps build and review domain models using DDD tactical patterns. It
targets the current stable Spring Boot 4.0.x line (Spring Framework 7, Java 25,
JSpecify null-safety, Jakarta EE, Hibernate 7) and assumes the user keeps the
patch version current — when a concrete version string is needed in config,
prefer the latest stable 4.0.x rather than pinning an old patch. The goal is a
domain layer that expresses business rules clearly and stays independent of
frameworks, with Spring wiring pushed to the edges.

## Project foundation — the SCOS BOM (required)

Projects in this ecosystem use the user's own Bill of Materials, which pins the
whole stack (Spring Boot 4.0.x, Spring Framework 7, Hibernate 7, Jackson 3,
JUnit Jupiter, Mockito, Testcontainers, Lombok, MapStruct, and more). When
scaffolding any module or pom, import it in `dependencyManagement` and declare
dependencies WITHOUT versions — the BOM manages them. Do not import other BOMs
or pin versions the SCOS BOM already controls.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>br.com.sawcunhaos</groupId>
      <artifactId>scos-bom</artifactId>
      <version>1.0.0</version> <!-- track the latest SCOS BOM release -->
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Two consequences for the domain model: **MapStruct** is available, so the
domain↔JPA-entity mapper in `references/building-blocks.md` can be a MapStruct
mapper rather than hand-written. **Lombok** is also on the classpath — fine for
DTOs and JPA entities, but don't put `@Data`/`@Setter` on aggregates or value
objects; that reintroduces the public setters and mutability this skill works to
avoid. Keep aggregates hand-written with intention-revealing methods.

## First step: decide the mode

Figure out whether the user wants to **generate** new code or **review/refactor**
existing code. The two paths share the same principles but diverge in execution.

- Generating a new aggregate, module, or bounded context → follow **Generate**.
- Evaluating, critiquing, or refactoring existing code → follow **Review**.
- Ambiguous → ask one short question, then proceed.

Whichever the mode, read `references/building-blocks.md` and
`references/layering.md` before producing substantial output — they define the
patterns this skill enforces and keep the vocabulary consistent.

## The layered shape (why it matters)

The core idea is dependency direction: the domain knows nothing about the
outside world; everything points inward toward it. Concretely, four layers:

- **domain** — entities, value objects, aggregates, domain events, domain
  services, and repository *interfaces*. No Spring, no JPA annotations, no
  framework imports. This is the part that must be easy to test and reason about.
- **application** — use cases / application services that orchestrate the domain,
  manage transactions, and call repository interfaces. Thin; holds no business
  rules of its own.
- **infrastructure** — repository *implementations* (JPA, etc.), messaging,
  external clients. Depends on domain interfaces, never the reverse.
- **interfaces / api** — REST controllers, DTOs, mappers. Translates the outside
  world into application calls.

When a piece of logic has no obvious home, the rule of thumb is: if it enforces
an invariant about the data, it belongs *inside* the aggregate; if it
coordinates multiple aggregates or external resources, it's an application
service; if it expresses domain logic that spans entities but enforces no single
invariant, it's a domain service.

## Generate

1. Confirm the bounded context and the aggregate(s) at stake. A good aggregate is
   small, has one root, and is a transactional consistency boundary — prefer
   referencing other aggregates by ID, not by object reference.
2. Scaffold the package layout from `references/layering.md`.
3. Build the domain first, test-first if pairing with the `tdd-workflow` skill:
   - Value objects for any concept with rules but no identity (Money, Email,
     CPF). Make them immutable and self-validating in the constructor.
   - The aggregate root as the only entry point for changing its internals.
     Mutations go through intention-revealing methods (`order.confirm()`), never
     through public setters.
   - Raise domain events from inside the aggregate for things other parts of the
     system care about.
   - A repository interface expressed in domain terms (`OrderRepository`), living
     in the domain layer.
4. Add the application service that loads the aggregate, calls a method on it,
   and saves it — wrapped in `@Transactional` *here*, not in the domain.
5. Add infrastructure last: the JPA adapter implementing the repository. Keep
   persistence concerns (entity annotations, mapping) out of the pure domain
   where practical — see the persistence-ignorance note in
   `references/building-blocks.md` for the pragmatic trade-offs.

Default to Maven unless the user uses Gradle. State versions/assumptions inline
rather than inventing config the user didn't ask for.

## Review

Run the code against `references/review-checklist.md`. Don't just list
violations — for each finding, explain *why* it's a problem and show the smallest
refactor that fixes it. The single most common and most important finding is the
**anemic domain model**: entities that are bags of getters/setters with all the
behavior living in services. If you see that, lead with it.

Structure the review as: a one-paragraph summary of the design's health, then
findings ordered by impact (architectural > tactical > naming), each with a
concrete before/after snippet where useful. Avoid drowning the user in nitpicks.

## Anti-patterns to flag (quick reference)

- **Anemic domain** — logic in services, data in dumb entities.
- **Leaky persistence** — JPA/Spring annotations bleeding into domain types, or
  repositories returning ORM entities straight to controllers.
- **God aggregate** — one huge aggregate that should be several; cross-aggregate
  references by object instead of ID.
- **Public setters on the root** — invariants can be bypassed.
- **Transactions in the domain** — `@Transactional` belongs in application
  services.
- **Primitive obsession** — `String email`, `BigDecimal amount` instead of
  value objects.

See `references/review-checklist.md` for the full rubric and `references/building-blocks.md`
for canonical code examples of each pattern.
