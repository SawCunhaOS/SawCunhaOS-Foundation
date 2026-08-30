
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.foundation.jdempotent.redis.test;


import br.com.sawcunhaos.foundation.jdempotent.redis.test.app.JdempotentTestApplication;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;

/**
 * Story 3.7, AC #2 / Task 4: end-to-end coverage through the real, {@code @JdempotentResource}
 * -protected HTTP endpoint — exactly the shape Task 4 asks for ("uma requisição de negócio
 * protegida por {@code @JdempotentResource} completa com sucesso mesmo com o Redis fora do ar").
 * The lower-level, faster-to-run coverage of the circuit breaker mechanics itself (breaker
 * opening, split-brain, the {@code OPEN -> HALF_OPEN} window) lives in
 * {@code RedisIdempotentRepositoryFailOpenITTest}, exercising {@code RedisIdempotentRepository}
 * directly instead of a full Spring context + HTTP round trip.
 *
 * <p>Standalone Redis (not Sentinel, unlike the sibling {@code PrimeNumbersJdempotentEnableITTest})
 * because this test needs to pause the single node mid-test — same
 * {@code @DynamicPropertySource}-starts-the-container-before-context-refresh reasoning documented
 * there applies here too.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
            JdempotentTestApplication.class
        })
class PrimeNumbersJdempotentRedisUnavailableITTest {

    private static final GenericContainer<?> REDIS = new GenericContainer<>("bitnami/redis:latest")
            .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        REDIS.start();
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @AfterAll
    static void tearDown() {
        REDIS.stop();
    }

    @Autowired
    private WebApplicationContext webApplicationContext;
    @LocalServerPort
    private int port;

    @BeforeEach
    void initialiseRestAssuredMockMvcWebApplicationContext() {
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        RestAssured.port = port;
    }

    @Test
    void given_redis_unavailable_when_a_jdempotent_protected_endpoint_is_called_then_the_business_request_still_succeeds() {
        // Warm-up while Redis is genuinely up: establishes the connection and exercises the
        // happy path once, so pausing below tests the command-level slow-call path (what
        // slow-call-duration-threshold guards) rather than a connection-establishment failure.
        given()
                .when()
                    .get("/prime-number")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .contentType(ContentType.JSON)
                    .body("$", hasKey("primesNumber"));

        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            // A different query param than the warm-up call above resolves to a different
            // idempotency key, so this exercises a fresh tryAcquire (not a cache hit on the
            // warm-up's key) with Redis genuinely unreachable — AC #1/#2: the business request
            // must still complete (never FAIL_CLOSED / never a 5xx because Redis is down).
            given()
                    .when()
                        .get("/prime-number?quantityPrimeNumbers=7")
                    .then()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(ContentType.JSON)
                        .body("$", hasKey("primesNumber"));
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
    }
}
