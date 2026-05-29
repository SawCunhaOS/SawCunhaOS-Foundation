---
name: tdd-workflow
description: >
  Drive Java/Spring Boot development with test-driven development — both to write
  code test-first and to review the quality of existing tests. Use this skill
  whenever the user wants to practice TDD, write a failing test first, do
  red-green-refactor, add unit tests for a class or method, improve test
  coverage meaningfully, assess whether tests are brittle or over-mocked, or
  refactor a test suite — even if they don't say "TDD". Also trigger when the
  user asks "how should I test this", mentions JUnit 5, Mockito, AssertJ, test
  doubles, or wants help deciding what to mock. Targets JUnit 5 (Jupiter),
  AssertJ, Mockito on the current stable Spring Boot 4.0.x / Java 25. For
  integration tests with real databases or message brokers, prefer the
  testcontainers-integration skill.
---

# TDD Workflow (JUnit 5 / Java 25 / Spring Boot 4.0.x)

This skill drives development through tests. Spring Boot 4 removed JUnit 4, so
everything here is the JUnit Jupiter programming model — note the SCOS BOM brings
**JUnit 6** (Jupiter), whose API matches the JUnit 5 examples shown here — with
AssertJ for assertions and Mockito for test doubles. The point of TDD is not
coverage for its own sake — it's using the test as the first consumer of your
code, which forces small, well-shaped units and gives you a safety net for
refactoring.

## Project foundation — the SCOS BOM (required)

Test dependencies come from the user's BOM, which pins JUnit Jupiter (6),
Mockito, AssertJ, Testcontainers, plus REST Assured and WireMock for API/contract
tests. Import it in `dependencyManagement` and declare test dependencies WITHOUT
versions; don't pin versions the BOM controls.

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

## First step: decide the mode

- Writing new code or a new feature → follow **Generate (test-first)**.
- Looking at existing tests to judge or improve them → follow **Review**.

Read `references/junit5-patterns.md` before writing tests so the structure,
naming, and assertion style stay consistent. For the discipline of the cycle
itself, `references/tdd-cycle.md`.

## Generate (test-first)

Work the red-green-refactor loop one tiny behavior at a time:

1. **Red** — write the smallest test that expresses the next bit of behavior and
   watch it fail for the right reason (a failing assertion, not a compile error
   you forgot about). Name it after the behavior, not the method:
   `confirmingAnEmptyOrderIsRejected`, not `testConfirm`.
2. **Green** — write the least code that makes it pass, even if crude. Resist
   implementing things no test demands yet.
3. **Refactor** — with the bar green, clean up names, duplication, and structure.
   The tests let you do this without fear.
4. Repeat. Use triangulation when one example isn't enough to force the real
   implementation — add a second, more specific case.

What to put under test directly vs. mock:
- **Don't mock the domain.** Value objects, entities, aggregates — exercise the
  real things. They're fast and pure; mocking them tests nothing.
- **Mock collaborators at the boundary** — repositories, gateways, clients,
  publishers — when testing an application service in isolation. Mock roles you
  own, not types you don't (don't mock `List` or third-party value types).
- If you're mocking a lot to test one class, that's a design smell the test is
  surfacing — consider whether the class has too many responsibilities.

Pair naturally with `ddd-tactical-design`: test the aggregate's invariants with
plain unit tests (no Spring context), and test the application service with
mocked repositories.

## Review

Assess tests against `references/review-checklist.md`. The trap to watch for is
tests that pass but don't protect anything: asserting on mock interactions
instead of real outcomes, or so tightly coupled to implementation that any
refactor breaks them. Coverage numbers are a weak signal — a test that calls a
method and asserts nothing meaningful still counts as covered.

Structure the review as a short health summary, then findings ordered by impact
(tests that give false confidence > brittle tests > slow tests > style), each
with a concrete fix.

## Layered testing strategy (so you reach for the right tool)

- **Pure unit tests** (no Spring) — domain logic, application services with
  mocked ports. The bulk of your tests; milliseconds each.
- **Spring slice tests** — `@WebMvcTest` for controllers, `@DataJpaTest` for
  repository mappings. Loads only the relevant slice of context.
- **Integration tests** — full wiring against real infrastructure. Use the
  `testcontainers-integration` skill; keep these fewer and slower.

Favor the base of that pyramid. If every test boots a full `@SpringBootTest`,
the suite gets slow and the feedback loop that makes TDD worthwhile is lost.
