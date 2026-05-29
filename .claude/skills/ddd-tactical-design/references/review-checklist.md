# DDD tactical review checklist

Use this as a rubric, not a script. For each item, the question is "does the code
honor the intent?" — explain findings with the *why* and a minimal fix, ordered
by impact. Skip items that don't apply.

## 1. Architecture / dependency direction (highest impact)
- Does the domain layer import any framework (Spring, JPA/Hibernate, Jackson)?
  It shouldn't. Persistence annotations on domain types are the usual leak.
- Do dependencies point inward only (`api → application → domain`,
  `infrastructure → domain`)? Any inward type referencing an outer one is a red
  flag.
- Are ORM entities returned straight from controllers? They should be mapped to
  DTOs at the api boundary.

## 2. Aggregate design
- Is each aggregate small with a single root? Giant aggregates that load dozens
  of child collections usually want splitting.
- Are other aggregates referenced by ID rather than object reference?
- Is the root the only mutation entry point? Child entities created/changed only
  through root methods?
- Is the aggregate the transactional consistency boundary — i.e., does one
  transaction modify one aggregate instance?

## 3. Behavior vs data (the anemic-model test)
- Do entities/aggregates have real behavior, or just getters/setters with all
  logic in services? If the latter, that's an anemic model — the headline
  finding. Move invariant-enforcing logic into the aggregate.
- Are there public setters that let callers bypass invariants? Replace with
  intention-revealing methods (`confirm()`, `changeQuantity()`).
- Are state transitions guarded (can't confirm an empty order, can't ship a
  cancelled one)?

## 4. Value objects
- Are concepts with rules but no identity modeled as value objects, or as raw
  primitives (primitive obsession — `String email`, `BigDecimal price`)?
- Are value objects immutable and self-validating in the constructor?
- Are IDs typed (`OrderId`) rather than bare `UUID`/`Long`?

## 5. Layer responsibilities
- Is `@Transactional` on application services, not in the domain or on
  controllers?
- Are application services thin (orchestration only) and free of business rules?
- Are repository interfaces in the domain and implementations in infrastructure?
- Do repositories speak the ubiquitous language (`findActiveSubscriptions`)
  rather than exposing query/ORM mechanics?

## 6. Domain events
- Are events immutable, past-tense named (`OrderConfirmed`), and raised inside
  the aggregate?
- Are they published after commit by the application layer, not fired
  mid-transaction in a way that can leave listeners and the DB inconsistent?

## 7. Naming / ubiquitous language
- Do type and method names match the language the domain experts actually use?
- Any technical-jargon names (`OrderManager`, `OrderHelper`, `processData`) that
  hide intent?

## Output format for a review
1. **Health summary** — one paragraph: overall shape, biggest risk.
2. **Findings** — ordered by impact (architecture → aggregate → anemia → VOs →
   layers → events → naming). Each: what, why it matters, minimal before/after.
3. **What's already good** — call out 1–3 things done well; reviews land better
   and it tells the user what to preserve.

Keep it proportional to the code under review. A 200-line service doesn't need 30
findings — surface the few that matter.
