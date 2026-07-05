# Project skeleton — pom, layout, config

Maven, Java 25, Spring Boot 4.0.x, SCOS BOM. The defining choice: import
`br.com.sawcunhaos:scos-bom` for dependency management instead of inheriting
`spring-boot-starter-parent`, and declare dependencies without versions.

## pom.xml

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.example</groupId>
  <artifactId>ordering-service</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>jar</packaging>

  <properties>
    <java.version>25</java.version>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

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
    <!-- declared WITHOUT versions — the SCOS BOM manages them -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.liquibase</groupId>
      <artifactId>liquibase-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.mapstruct</groupId>
      <artifactId>mapstruct</artifactId>
    </dependency>

    <!-- SCOS foundation libraries (house style: controllers, DTOs, exception handler, etc.) -->
    <dependency>
      <groupId>br.com.sawcunhaos</groupId>
      <artifactId>scos-foundation-utils</artifactId>
    </dependency>
    <dependency>
      <groupId>br.com.sawcunhaos</groupId>
      <artifactId>scos-foundation-exception</artifactId>
    </dependency>
    <!-- add scos-foundation-audit / scos-foundation-jdempotent when those features are needed -->

    <!-- test (versions from the BOM) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <!-- align the plugin version with the Spring Boot line the BOM pins (4.0.x) -->
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <annotationProcessorPaths>
            <path>
              <groupId>org.mapstruct</groupId>
              <artifactId>mapstruct-processor</artifactId>
            </path>
            <!-- add lombok / lombok-mapstruct-binding here if Lombok is used on DTOs/entities -->
          </annotationProcessorPaths>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

Note: since the project doesn't inherit `spring-boot-starter-parent`, the
`spring-boot-maven-plugin` isn't given a version by a parent. Manage it in
`<pluginManagement>` with a `${spring-boot.version}` property kept in lockstep
with the Spring Boot line the SCOS BOM pins (the current 4.0.x), so the plugin
and the runtime never drift apart.

## Package layout

Mirror the `ddd-tactical-design` layering, one bounded context per top package:

```
com.example.ordering
├── OrderingApplication.java        (@SpringBootApplication — this skill)
├── api/                            (controllers, DTOs, mappers — this skill)
├── application/                    (use cases — DDD skill)
├── domain/                         (model + repository interfaces — DDD skill)
└── infrastructure/                 (JPA adapters, messaging — DDD skill)
```

```java
@SpringBootApplication
public class OrderingApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderingApplication.class, args);
    }
}
```

## application.yml

Profiles for environment differences; secrets via environment variables, never
committed. Actuator endpoints exposed for k8s probes.

```yaml
spring:
  application:
    name: ordering-service
  main:
    allow-bean-definition-overriding: true   # the SCOS foundation relies on this
  threads:
    virtual:
      enabled: true              # Java 25 + Boot 4: virtual threads for request handling
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/ordering}
    username: ${DB_USER:ordering}
    password: ${DB_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: validate          # schema owned by Liquibase, not Hibernate
    open-in-view: false           # avoid lazy-loading in the web layer
    properties:
      hibernate:
        type:
          json_format_mapper: br.com.sawcunhaos.foundation.utils.configuration.hibernate.JacksonCustomJsonFormatMapper
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      probes:
        enabled: true             # liveness/readiness groups for Kubernetes

---
spring:
  config:
    activate:
      on-profile: prod
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
```

Keep `ddl-auto: validate` (or `none`) in real environments — let Liquibase own
the schema so changes are versioned and reviewable, and so Testcontainers tests
exercise the same migrations.
