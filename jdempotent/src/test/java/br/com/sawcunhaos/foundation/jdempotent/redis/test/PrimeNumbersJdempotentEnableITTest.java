
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
import br.com.sawcunhaos.foundation.jdempotent.core.aspect.IdempotentAspect;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.redis.configuration.ScosJdempotentRedisProperties;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
            JdempotentTestApplication.class
        })
@Testcontainers
class PrimeNumbersJdempotentEnableITTest {

    @Container
    public ComposeContainer environment =
            new ComposeContainer (new File("src/test/resources/docker-compose.yml"))
                    .withExposedService("redis", 6379,
                            Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)))
                    .withExposedService("redis-sentinel", 26379,
                            Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private IdempotentAspect idempotentAspect;
    @Autowired
    private ScosJdempotentRedisProperties scosJdempotentRedisProperties;
    @LocalServerPort
    private int port;

    private static final IdempotencyKey KEY_DEFAULT_PRIME_NUMBER = new IdempotencyKey("PrimeNumber.generatePrimeNumber-25c01e4fc78fc1df544d8b77bb4773f");

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
