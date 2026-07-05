# JUnit 5 / AssertJ / Mockito — patterns

Java 25, JUnit Jupiter, AssertJ, Mockito. JUnit 4 is gone in Spring Boot 4, so
no `@RunWith`, no `@Before` — use the Jupiter equivalents. The SCOS BOM pins
JUnit 6; its Jupiter API is the same as the JUnit 5 examples below, so these
patterns carry over unchanged.

## Table of contents
- Test structure & naming
- Lifecycle & nesting
- AssertJ assertions
- Parameterized tests
- Mockito with the Jupiter extension
- Test data builders

## Test structure & naming

Arrange / Act / Assert (a.k.a. given/when/then). Name the test after the
behavior and outcome, and use `@DisplayName` for human-readable reports.

```java
class OrderTest {

    @Test
    @DisplayName("confirming an empty order is rejected")
    void confirmingAnEmptyOrderIsRejected() {
        var order = new Order(OrderId.newId());          // arrange

        var thrown = catchThrowable(order::confirm);     // act

        assertThat(thrown)                               // assert
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("empty");
    }
}
```

One behavior per test. Multiple assertions are fine if they describe one
outcome; if they describe several, split the test.

## Lifecycle & nesting

```java
@BeforeEach void setUp() { ... }
@AfterEach  void tearDown() { ... }

@Nested
@DisplayName("when the order is confirmed")
class WhenConfirmed {
    @Test void cannotAddMoreLines() { ... }
}
```

`@Nested` groups related cases and lets you share setup for a sub-context without
polluting the rest of the class.

## AssertJ assertions

Prefer AssertJ's fluent chains over JUnit's `assertEquals` — they read better
and give richer failure messages.

```java
assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
assertThat(order.lines()).hasSize(2).extracting(OrderLine::productId).contains(productId);
assertThat(thrown).isInstanceOf(DomainException.class).hasMessageContaining("currency");
assertThatThrownBy(() -> money.add(otherCurrency)).isInstanceOf(IllegalArgumentException.class);
```

## Parameterized tests

Drive several inputs through one test instead of copy-pasting.

```java
@ParameterizedTest
@ValueSource(ints = {0, -1, -10})
void quantityBelowOneIsRejected(int qty) {
    assertThatThrownBy(() -> new OrderLine(productId, qty))
        .isInstanceOf(IllegalArgumentException.class);
}

@ParameterizedTest
@CsvSource({"10.00, USD, 5.00, USD, 15.00", "1.50, EUR, 0.50, EUR, 2.00"})
void addsAmountsOfSameCurrency(BigDecimal a, Currency ca, BigDecimal b, Currency cb, BigDecimal sum) {
    assertThat(new Money(a, ca).add(new Money(b, cb)).amount()).isEqualByComparingTo(sum);
}
```

## Mockito with the Jupiter extension

Use `@ExtendWith(MockitoExtension.class)` — not the old JUnit 4 runner.

```java
@ExtendWith(MockitoExtension.class)
class ConfirmOrderServiceBeanTest {

    @Mock OrderRepository orders;
    @Mock DomainEventPublisher publisher;
    @InjectMocks ConfirmOrderServiceBean service;

    @Test
    void confirmsAnExistingOrderAndPublishesEvents() {
        var order = OrderFixture.draftWithOneLine();
        given(orders.findById(order.id())).willReturn(Optional.of(order));

        service.confirm(order.id());

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);  // assert the real outcome
        then(orders).should().save(order);
        then(publisher).should().publish(any(OrderConfirmed.class));
    }

    @Test
    void failsWhenOrderDoesNotExist() {
        given(orders.findById(any())).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirm(OrderId.newId()))
            .isInstanceOf(ScosException.class);
        then(orders).should(never()).save(any());
    }
}
```

Prefer BDD style (`given`/`then().should()`) for readability. Assert on real
state where you can; fall back to verifying interactions only when the effect is
purely a call to a collaborator (e.g. publishing).

## Test data builders

Keep tests readable and resilient by centralizing object creation. When a
constructor changes, you fix one builder instead of fifty tests.

```java
final class OrderFixture {
    static Order draftWithOneLine() {
        var order = new Order(OrderId.newId());
        order.addLine(new ProductId(UUID.randomUUID()), 1);
        return order;
    }
}
```
