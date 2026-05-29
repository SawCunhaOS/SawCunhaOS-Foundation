# Testcontainers + Spring Boot 4.0.x — setup

Maven, Java 25, JUnit 5, Spring Boot 4.0.x. The key modern pieces are
`spring-boot-testcontainers` and `@ServiceConnection`, which remove the manual
property wiring older guides show.

## Table of contents
- Maven dependencies
- @ServiceConnection (preferred)
- @DynamicPropertySource (fallback)
- @DataJpaTest against a real database
- Singleton container pattern (suite speed)
- Container reuse (local loop)
- Testcontainers at development time

## Maven dependencies

The SCOS BOM (`br.com.sawcunhaos:scos-bom`) already manages Testcontainers,
JUnit Jupiter, and the JDBC drivers, so import it in `dependencyManagement` and
add the modules WITHOUT versions and WITHOUT importing the `testcontainers-bom`.
Keep them in `test` scope.

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

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Note the absence of `<version>` tags — that's intentional. The BOM is the single
source of truth, which is how the user keeps the stack aligned and current.

## @ServiceConnection (preferred)

Spring reads the container and configures the datasource automatically — no URL,
username, or password wiring.

```java
@SpringBootTest
@Testcontainers
class OrderRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired OrderRepository orders;

    @Test
    void persistsAndReadsBackAnOrder() {
        var order = OrderFixture.draftWithOneLine();
        orders.save(order);

        assertThat(orders.findById(order.id())).isPresent();
    }
}
```

`@ServiceConnection` works for the common containers (Postgres, MySQL, MongoDB,
Redis, Kafka, RabbitMQ, and more). When it covers your service, use it.

## @DynamicPropertySource (fallback)

For services without `@ServiceConnection` support, or custom mapping:

```java
@DynamicPropertySource
static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

## @DataJpaTest against a real database

`@DataJpaTest` loads only the JPA slice. By default it swaps in an embedded DB;
disable that so it uses the container — this is how you verify mappings and
migrations against the real engine.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderJpaMappingIT {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    // ... inject the Spring Data repository and assert mappings/queries
}
```

## Singleton container pattern (suite speed)

`@Container` on a static field per class still starts a container per class. For
a fast suite, start one container for the whole run and share it. The trick:
start it manually (not via the `@Testcontainers` extension) so it isn't stopped
between classes — Ryuk reaps it when the JVM exits.

```java
public abstract class AbstractPostgresIT {
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17-alpine");
    static {
        POSTGRES.start();   // started once, reused by every subclass
    }
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

Integration test classes then `extends AbstractPostgresIT`. This is usually the
single biggest win for a slow suite.

## Container reuse (local loop)

For an even tighter local loop, enable reuse so the container survives between
runs. Set `withReuse(true)` on the container and add `testcontainers.reuse.enable=true`
to `~/.testcontainers.properties`. Reuse is a local convenience — don't depend on
it in CI, where each run should be clean.

## Testcontainers at development time

Spring Boot can also run containers when you start the app locally (not just in
tests). Add a test `@TestConfiguration` exposing the container as a
`@ServiceConnection` bean and launch via a test `main` that calls
`SpringApplication.from(App::main).with(Containers.class).run(args)`. Handy for
running the app without installing a local Postgres. Mention this when the user
wants a frictionless dev startup, not just tests.
