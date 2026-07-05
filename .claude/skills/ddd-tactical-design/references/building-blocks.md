# DDD Building Blocks — canonical Java examples

Spring Boot 4.0.x / Java 25. Examples use records for value objects, JSpecify
`@NonNull`/`@Nullable` where nullability matters, and keep the domain free of
Spring/JPA imports.

## Table of contents
- Value Objects
- Entities
- Aggregates & aggregate roots
- Domain events
- Repositories (domain interface vs infrastructure adapter)
- Domain services vs application services
- Persistence ignorance — the pragmatic trade-off

## Value Objects

A value object has no identity; two are equal if their contents are equal. Make
them immutable and validate in the constructor so an invalid instance can never
exist. Records are ideal.

```java
package com.example.ordering.domain.model;

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null || currency == null)
            throw new IllegalArgumentException("amount and currency are required");
        if (amount.scale() > currency.getDefaultFractionDigits())
            amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
        if (amount.signum() < 0)
            throw new IllegalArgumentException("amount cannot be negative");
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency))
            throw new IllegalArgumentException("currency mismatch");
    }
}
```

The win: `Money` cannot be negative or mix currencies anywhere in the system,
because the rule lives in one place instead of being re-checked in every service.

## Entities

An entity has identity that persists through changes. Equality is by identity,
not attributes. Keep mutation behind intention-revealing methods.

```java
public class OrderLine {
    private final ProductId productId;   // reference other aggregates by ID
    private int quantity;

    OrderLine(ProductId productId, int quantity) {   // package-private: created via the root
        if (quantity < 1) throw new IllegalArgumentException("quantity must be >= 1");
        this.productId = productId;
        this.quantity = quantity;
    }

    void changeQuantity(int newQuantity) {
        if (newQuantity < 1) throw new IllegalArgumentException("quantity must be >= 1");
        this.quantity = newQuantity;
    }
}
```

## Aggregates & aggregate roots

The aggregate root is the only object outside code talks to. It guards the
invariants of everything inside it and is the unit of persistence and
transactional consistency. Reference other aggregates by ID.

```java
public class Order {                       // aggregate root
    private final OrderId id;
    private OrderStatus status;
    private final List<OrderLine> lines = new ArrayList<>();
    private final List<DomainEvent> events = new ArrayList<>();

    public void addLine(ProductId product, int quantity) {
        if (status != OrderStatus.DRAFT)
            throw new IllegalStateException("can only add lines to a draft order");
        lines.add(new OrderLine(product, quantity));   // outsiders never new an OrderLine
    }

    public void confirm() {
        if (lines.isEmpty())
            throw new IllegalStateException("cannot confirm an empty order");
        this.status = OrderStatus.CONFIRMED;
        events.add(new OrderConfirmed(id, Instant.now()));
    }

    public List<DomainEvent> pullEvents() {
        var pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }
}
```

Note: no setters, no `@Entity`, no Spring. The invariants ("can't confirm an
empty order", "can't modify a confirmed order") are impossible to bypass.

## Domain events

Plain immutable records. Raised inside aggregates; published by the application
layer after the transaction commits.

```java
public sealed interface DomainEvent permits OrderConfirmed, OrderCancelled {}

public record OrderConfirmed(OrderId orderId, Instant occurredOn) implements DomainEvent {}
```

## Repositories

The **interface** lives in the domain and speaks the ubiquitous language —
collection-like, returning domain types, no SQL or ORM concepts:

```java
package com.example.ordering.domain.model;

public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
}
```

The **implementation** lives in infrastructure and is the only place that knows
about JPA:

```java
package com.example.ordering.infrastructure.persistence;

@Repository
class JpaOrderRepository implements OrderRepository {
    private final OrderJpaEntityRepository jpa;     // Spring Data interface
    private final OrderMapper mapper;

    public Optional<Order> findById(OrderId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    public void save(Order order) {
        jpa.save(mapper.toEntity(order));
    }
}
```

## Domain services vs application services

- **Domain service**: stateless domain logic that doesn't fit one entity, e.g. a
  pricing policy spanning several value objects. Lives in the domain layer, no
  framework deps.
- **Application service / use case**: orchestration. Loads aggregates, invokes a
  method, persists, publishes events. Owns the transaction. In SCOS style (see
  the `scos-conventions` skill) it's an interface in a `specification` package
  with a `Bean` implementation, constructor-injected via Lombok, and signals
  errors with `ScosException` + an `ExceptionCode`.

```java
// ...application.specification
public interface ConfirmOrderService {
    void confirm(OrderId id);
}

// ...application
@Service("ConfirmOrderService")
@RequiredArgsConstructor
@Slf4j
public class ConfirmOrderServiceBean implements ConfirmOrderService {

    private final OrderRepository orders;
    private final DomainEventPublisher publisher;

    @Override
    @Transactional                                   // transaction boundary lives HERE
    public void confirm(OrderId id) {
        var order = orders.findById(id)
            .orElseThrow(() -> new ScosException(OrderExceptionCode.ORDER_NOT_FOUND, id.value()));
        order.confirm();                             // business rule lives in the aggregate
        orders.save(order);
        order.pullEvents().forEach(publisher::publish);
    }
}
```

The aggregate keeps enforcing its own invariants with plain exceptions (it stays
framework-free); the application service translates boundary conditions into
`ScosException`, which the foundation's global `ExceptionsHandler` turns into a
localized response.

## Persistence ignorance — the pragmatic trade-off

Purest form: the domain `Order` has zero persistence annotations, and a separate
`OrderJpaEntity` + mapper handles storage. Cleanest, but costs mapping
boilerplate.

Pragmatic middle ground used by many teams: annotate the domain model directly
with JPA. Faster, but couples the domain to Hibernate and tends to push you
toward setters and no-arg constructors that weaken invariants.

Recommend the separate-entity approach for rich domains and long-lived systems,
and note the mapper cost honestly. For simple CRUD-ish contexts, the pragmatic
approach is a reasonable call — say so rather than being dogmatic.
