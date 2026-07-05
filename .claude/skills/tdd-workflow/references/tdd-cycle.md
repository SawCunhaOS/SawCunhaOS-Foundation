# The TDD cycle & test review rubric

Two parts: the discipline of red-green-refactor (for generating), and the rubric
for judging test quality (for reviewing).

## The cycle

**Red → Green → Refactor**, in baby steps:

1. **Red.** Write one small failing test. It must fail for the intended reason —
   run it and read the failure. A test that passes immediately tested nothing or
   the behavior already exists.
2. **Green.** Do the simplest thing that passes. "Fake it till you make it" is
   legitimate: returning a hard-coded value is fine if no test yet forces
   otherwise. This keeps steps tiny.
3. **Refactor.** Now improve the design — extract methods, remove duplication,
   rename — with the safety net green. Refactor the tests too; they're code.

**Triangulation:** when a single example lets you cheat (hard-code the answer),
add a second, different example. Two examples usually force the real algorithm.

**When NOT to be dogmatic.** TDD shines for logic with branches, edge cases, and
evolving requirements. For trivial glue, throwaway spikes, or exploratory work,
writing tests after (or spiking then deleting and redoing test-first) is a
reasonable call. Say so honestly rather than insisting on ceremony.

## Test review rubric

Order findings by impact. The worst tests are the ones that give false
confidence; a slow or ugly test is a lesser sin than one that passes while the
code is broken.

### 1. False confidence (highest impact)
- Does the test actually assert a meaningful outcome, or just call the method?
  Coverage without assertions protects nothing.
- Does it assert on real state/return values, or only on mock interactions
  (`verify(...)`) where it could check the actual effect?
- Are happy path AND failure/edge cases covered, or only the easy path?

### 2. Brittleness
- Will an internal refactor (that preserves behavior) break this test? If yes,
  it's coupled to implementation, not behavior.
- Over-mocking: are collaborators mocked so heavily the test mirrors the
  implementation step by step? That's a design smell and a brittle test.
- Are there assertions on incidental details (ordering, exact strings) that
  aren't part of the contract?

### 3. Isolation & speed
- Are unit tests free of Spring context, DB, network, sleep/clock dependence?
- Is shared mutable state leaking between tests (order-dependent tests)?
- Could this be a fast unit test but is needlessly a full `@SpringBootTest`?

### 4. Readability
- Does the name describe the behavior and outcome?
- Is it arrange/act/assert clear, or a tangle? Is object setup hidden behind a
  builder/fixture?
- One behavior per test, or several crammed together?

### Output format for a review
1. **Health summary** — one paragraph: does this suite actually protect the code?
2. **Findings** — impact-ordered, each with the why and a minimal fix.
3. **What's solid** — name 1–3 good tests/practices worth keeping.
