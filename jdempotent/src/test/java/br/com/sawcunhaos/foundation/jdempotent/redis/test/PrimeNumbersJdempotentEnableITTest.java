
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


import br.com.sawcunhaos.foundation.jdempotent.redis.repository.SentinelContainerFixture;
import br.com.sawcunhaos.foundation.jdempotent.redis.test.app.JdempotentTestApplication;
import br.com.sawcunhaos.foundation.jdempotent.core.aspect.IdempotentAspect;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.redis.configuration.ScosJdempotentRedisProperties;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Story 3.19 (Emenda 2026-08-30, item 5): used to depend on {@code docker-compose.yml}'s fixed
 * host ports ({@code 6379}/{@code 26379}, a known flaky pattern — deferred-work.md), which broke
 * {@code mvn verify} whenever those ports were already bound. Now uses the same dynamic-port
 * Sentinel pattern already proven in {@code RedisIdempotentRepositoryTopologySmokeITTest}
 * ({@link SentinelContainerFixture}), wired in via {@code @DynamicPropertySource} instead of the
 * (now removed) fixed {@code spring.data.redis.*} keys in {@code application.yml}.
 *
 * <p>Not {@code @Container}/{@code @Testcontainers}-managed: Spring resolves
 * {@code spring.data.redis.*} while building the {@code ApplicationContext}, so the containers
 * must already be running by the time {@code @DynamicPropertySource} runs (before
 * {@code @Testcontainers}' {@code @BeforeAll} would fire) — started explicitly below instead.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
            JdempotentTestApplication.class
        })
class PrimeNumbersJdempotentEnableITTest {

    private static final SentinelContainerFixture SENTINEL = SentinelContainerFixture.build("prime-numbers-sentinel");

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        SENTINEL.master.start();
        try {
            SENTINEL.sentinel.start();
        } catch (RuntimeException e) {
            // don't leave the master running orphaned if the sentinel container fails to start
            SENTINEL.master.stop();
            throw e;
        }
        registry.add("spring.data.redis.sentinel.master", () -> SENTINEL.masterName);
        registry.add("spring.data.redis.sentinel.nodes[0]", () -> "127.0.0.1:" + SENTINEL.sentinelPort);
    }

    @AfterAll
    static void tearDown() {
        SENTINEL.sentinel.stop();
        SENTINEL.master.stop();
    }

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private IdempotentAspect idempotentAspect;
    @Autowired
    private ScosJdempotentRedisProperties scosJdempotentRedisProperties;
    @LocalServerPort
    private int port;

    // Story 3.19: this hash was stale (a shorter, MD5-length hex string) — masked until now
    // because the fixed-port docker-compose bug always failed the test before reaching this
    // assertion. DefaultKeyGenerator's actual (SHA-256) output for this request is below;
    // verified deterministic across repeated runs.
    private static final IdempotencyKey KEY_DEFAULT_PRIME_NUMBER = new IdempotencyKey("PrimeNumber.generatePrimeNumber-ea2cfd6b48da4f849a45691d17d1bc2a40cd95e359017f43c590e3ff1ca4dffb");

    @BeforeEach
    public void initialiseRestAssuredMockMvcWebApplicationContext() {
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        RestAssured.port = port;
    }

    @Test
    void deveChamarAPIEAdicionarNaIdempotencyOResultadoENaSegundaChamadaRetornaOResultado() {

        given()
                .when()
                    .get("/prime-number")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .contentType(ContentType.JSON)
                    .body("$", hasKey("primesNumber"));

        assertTrue(idempotentAspect.getIdempotentRepository().contains(KEY_DEFAULT_PRIME_NUMBER));

        given()
                .when()
                    .get("/prime-number")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .contentType(ContentType.JSON)
                    .body("$", hasKey("primesNumber"));
    }

    @Test
    void deveChamarAPIEAdicionarNaIdempotencyOResultadoENaSegundaChamadaNaoRetornaOResultado() {
        ReflectionTestUtils.setField(scosJdempotentRedisProperties, "persistReqRes", false);

        given()
                .when()
                    .get("/prime-number")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .contentType(ContentType.JSON)
                    .body("$", hasKey("primesNumber"));

        assertTrue(idempotentAspect.getIdempotentRepository().contains(KEY_DEFAULT_PRIME_NUMBER));

        given()
                .when()
                    .get("/prime-number")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .contentType(StringUtils.EMPTY);
    }
}
