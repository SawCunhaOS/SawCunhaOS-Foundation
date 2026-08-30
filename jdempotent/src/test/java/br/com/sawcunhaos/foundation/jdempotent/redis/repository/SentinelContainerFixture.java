
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

package br.com.sawcunhaos.foundation.jdempotent.redis.repository;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

/**
 * Story 3.19 (R-001 mitigation): builds a master + sentinel container pair reachable by the
 * test JVM via pre-allocated host ports and Docker's {@code host.testcontainers.internal}
 * host-gateway alias — used by {@code RedisIdempotentRepositoryTopologySmokeITTest} and, since
 * the amendment, by {@code PrimeNumbersJdempotentEnableITTest}/{@code DisableITTest} too (the
 * same fixed-port bug fix), so it lives here as a small shared test fixture rather than
 * triplicated.
 *
 * <p>Callers own the containers' lifecycle: this class only builds and configures them (never
 * starts them), so a plain-JUnit test can drive them via {@code @Container}/{@code @Testcontainers}
 * while a {@code @SpringBootTest} one starts them itself inside {@code @DynamicPropertySource}
 * (containers must already be running before Spring resolves connection properties).</p>
 */
public final class SentinelContainerFixture {

    public final GenericContainer<?> master;
    public final GenericContainer<?> sentinel;
    public final String masterName;
    public final int sentinelPort;

    private SentinelContainerFixture(GenericContainer<?> master, GenericContainer<?> sentinel, String masterName, int sentinelPort) {
        this.master = master;
        this.sentinel = sentinel;
        this.masterName = masterName;
        this.sentinelPort = sentinelPort;
    }

    public static SentinelContainerFixture build(String masterName) {
        int masterPort = findFreePort();
        int candidateSentinelPort = findFreePort();
        // findFreePort() opens+closes a socket each call, no reservation between the two calls,
        // so in theory both can return the same port — retry until they differ.
        while (candidateSentinelPort == masterPort) {
            candidateSentinelPort = findFreePort();
        }
        int sentinelPort = candidateSentinelPort;

        GenericContainer<?> master = new GenericContainer<>("bitnami/redis:latest")
                .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
                .withExposedPorts(6379)
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withPortBindings(
                        new PortBinding(Ports.Binding.bindPort(masterPort), new ExposedPort(6379))));

        GenericContainer<?> sentinel = new GenericContainer<>("bitnami/redis-sentinel:latest")
                .withExtraHost("host.testcontainers.internal", "host-gateway")
                .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
                .withEnv("REDIS_MASTER_HOST", "host.testcontainers.internal")
                .withEnv("REDIS_MASTER_PORT_NUMBER", String.valueOf(masterPort))
                .withEnv("REDIS_MASTER_SET", masterName)
                .withEnv("REDIS_SENTINEL_QUORUM", "1")
                .withEnv("REDIS_SENTINEL_ANNOUNCE_IP", "127.0.0.1")
                .withEnv("REDIS_SENTINEL_ANNOUNCE_PORT", String.valueOf(sentinelPort))
                .withExposedPorts(26379)
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withPortBindings(
                        new PortBinding(Ports.Binding.bindPort(sentinelPort), new ExposedPort(26379))))
                .waitingFor(Wait.forLogMessage(".*Sentinel ID.*\\n", 1).withStartupTimeout(Duration.ofSeconds(60)));

        return new SentinelContainerFixture(master, sentinel, masterName, sentinelPort);
    }

    static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Could not find a free host port for the Sentinel test infra", e);
        }
    }
}
