
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Story 3.19 (Emenda 2026-08-30, item 5): same fixed-port fix as {@code PrimeNumbersJdempotentEnableITTest}
 * — see its Javadoc for the reasoning. The module is disabled here ({@code scos.jdempotent.enabled=false})
 * so Redis is never actually connected to, but the containers/dynamic properties are kept for
 * parity/consistency with the Enable test (Code Map decision).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
            JdempotentTestApplication.class
        })
@TestPropertySource(properties = "scos.jdempotent.enabled=false")
class PrimeNumbersJdempotentDisableITTest {

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
    @Autowired(required = false)
    private IdempotentAspect idempotentAspect;
    @Autowired(required = false)
    private ScosJdempotentRedisProperties scosJdempotentRedisProperties;
    @LocalServerPort
    private int port;

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

        assertNull(idempotentAspect);
        assertNull(scosJdempotentRedisProperties);

    }

}
