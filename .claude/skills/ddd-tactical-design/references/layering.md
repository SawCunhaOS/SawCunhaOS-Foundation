# Layering & package structure

Package-by-feature at the top (one package per bounded context), then
package-by-layer inside. This keeps a context's code together and makes the
dependency direction visible.

```
com.example.ordering            <- one bounded context
├── domain
│   └── model
│       ├── Order.java                 (aggregate root)
│       ├── OrderLine.java             (entity, package-private constructor)
│       ├── OrderId.java               (value object / typed id)
│       ├── Money.java                 (value object)
│       ├── OrderStatus.java           (enum)
│       ├── OrderConfirmed.java        (domain event)
│       └── OrderRepository.java       (repository INTERFACE)
├── application
│   ├── ConfirmOrderUseCase.java       (@Service, @Transactional)
│   └── DomainEventPublisher.java      (port interface)
├── infrastructure
│   ├── persistence
│   │   ├── OrderJpaEntity.java        (@Entity — lives here, not in domain)
│   │   ├── OrderJpaEntityRepository.java (Spring Data interface)
│   │   ├── OrderMapper.java
│   │   └── JpaOrderRepository.java    (implements domain OrderRepository)
│   └── messaging
│       └── SpringDomainEventPublisher.java
└── api
    ├── OrderController.java           (@RestController)
    ├── dto
    │   ├── PlaceOrderRequest.java
    │   └── OrderResponse.java
    └── OrderApiMapper.java
```

## Enforcing the dependency rule

Direction must be: `api → application → domain` and `infrastructure → domain`.
The domain depends on nothing else in the project.

To stop violations from creeping in, suggest one of:

- **ArchUnit** tests that assert the domain package imports no Spring/JPA and
  that layers only depend inward. Example:

```java
@AnalyzeClasses(packages = "com.example.ordering")
class ArchitectureTest {
    @ArchTest
    static final ArchRule domain_is_framework_free =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..");
}
```

- **Spring Modulith** when contexts live in one deployable and you want runtime
  verification of module boundaries plus documentation generation. Mention it as
  an option for modular monoliths; it pairs naturally with this layout.

## Typed IDs

Prefer a value-object ID over a bare `UUID`/`Long`. It prevents passing an
`OrderId` where a `CustomerId` is expected and reads better.

```java
public record OrderId(UUID value) {
    public OrderId { if (value == null) throw new IllegalArgumentException("id required"); }
    public static OrderId newId() { return new OrderId(UUID.randomUUID()); }
}
```

## One context per package, not per project (at first)

Don't split into Maven/Gradle modules or microservices prematurely. Start with
package boundaries inside one module; promote to separate modules or services
only when team ownership, deployment, or scaling actually demands it. Splitting
too early ossifies boundaries you don't understand yet.
