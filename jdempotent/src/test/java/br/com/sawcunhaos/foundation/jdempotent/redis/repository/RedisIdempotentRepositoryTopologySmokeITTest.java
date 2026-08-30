
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

import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.Lease;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.RedisServer;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Story 3.19 / AD-7: Sentinel + Cluster concurrency smoke (2-3 real calls, same key) — split out
 * of {@code RedisIdempotentRepositoryTopologyITTest} (review finding, iteration 1) so that a
 * Sentinel/Cluster infra failure never blocks the Standalone regression battery in that other
 * class, which has no relation to this one's containers.
 *
 * <p>The atomicity itself comes from Redis's {@code SET NX PX} primitive, identical across
 * topologies; what actually varies here is the connection wiring, already covered without real
 * infra by {@code ScosJdempotentRedisConfigurationTest} (Story 3.1). This smoke proves the same
 * {@code tryAcquire -> Lease} contract holds once real Sentinel/Cluster infra is wired in.</p>
 */
@Testcontainers
class RedisIdempotentRepositoryTopologySmokeITTest {

    private static final Duration TASK_TIMEOUT = Duration.ofSeconds(30);

    private enum Topology {
        SENTINEL, CLUSTER
    }

    // ---------------------------------------------------------------------
    // Sentinel: R-001 mitigation — host ports for master + sentinel are pre-allocated
    // (found free, then bound explicitly) BEFORE the containers are created, so
    // REDIS_SENTINEL_ANNOUNCE_IP/PORT can point at an address the test JVM (running on
    // the host, not in Docker) can actually reach. The sentinel container additionally
    // resolves the master via "host.testcontainers.internal" (Docker's host-gateway
    // alias), because the master's own published host port is reachable from other
    // containers through that gateway just like it is from the host JVM.
    // ---------------------------------------------------------------------
    private static final SentinelContainerFixture SENTINEL = SentinelContainerFixture.build("jdempotent-topology");

    @Container
    static GenericContainer<?> sentinelMaster = SENTINEL.master;

    @Container
    static GenericContainer<?> sentinelSentinel = SENTINEL.sentinel;

    // ---------------------------------------------------------------------
    // Cluster: R-002 mitigation — grokzen/redis-cluster self-bootstraps a full 6-node
    // cluster (3 masters + 3 replicas) inside a single container, avoiding manual
    // multi-node CLUSTER MEET/slot assignment. It needs its "IP" announce env to equal
    // whatever address the client will actually dial, and its ports fixed 1:1 host<->
    // container (the announced address is "IP:PORT" verbatim, not remapped) — the base
    // port is still discovered free at runtime (not a hardcoded literal) to avoid the
    // fixed-port collision bug already known from docker-compose.yml.
    // ---------------------------------------------------------------------
    private static final int CLUSTER_NODE_COUNT = 6; // 3 masters + 1 replica each (image defaults)
    private static final int CLUSTER_BASE_PORT = findFreeConsecutivePorts(CLUSTER_NODE_COUNT);

    @Container
    static GenericContainer<?> clusterRedis = clusterContainer();

    /**
     * Finds {@code count} consecutive free ports (grokzen/redis-cluster numbers its nodes
     * sequentially from a single {@code INITIAL_PORT}), by probing candidate bases until every
     * port in the block binds successfully.
     */
    private static int findFreeConsecutivePorts(int count) {
        for (int attempt = 0; attempt < 50; attempt++) {
            int base = SentinelContainerFixture.findFreePort();
            if (base + count > 65535) {
                continue;
            }
            List<ServerSocket> opened = new ArrayList<>();
            try {
                boolean allFree = true;
                for (int i = 0; i < count; i++) {
                    try {
                        opened.add(new ServerSocket(base + i));
                    } catch (IOException e) {
                        allFree = false;
                        break;
                    }
                }
                if (allFree) {
                    return base;
                }
            } finally {
                opened.forEach(s -> {
                    try {
                        s.close();
                    } catch (IOException ignored) {
                        // best-effort release, port is about to be reused by the container
                    }
                });
            }
        }
        throw new IllegalStateException("Could not find " + count + " consecutive free host ports for the Cluster test infra");
    }

    // grokzen/redis-cluster's own node template has no "protected-mode" directive, so Redis
    // defaults it to enabled whenever a non-loopback bind address is used (BIND_ADDRESS=0.0.0.0
    // here) — Docker's port-forwarding makes our host-published connection look non-loopback to
    // the server inside the container, so it gets denied ("DENIED Redis is running in protected
    // mode ..."). bitnami's images avoid this because their entrypoint sets protected-mode no
    // automatically for ALLOW_EMPTY_PASSWORD=yes; grokzen's does not, so the node template is
    // overridden here (same content as the image's own, plus that one directive) before start.
    private static final String CLUSTER_NODE_TEMPLATE = """
            bind ${BIND_ADDRESS}
            port ${PORT}
            protected-mode no
            cluster-enabled yes
            cluster-config-file nodes.conf
            cluster-node-timeout 5000
            appendonly yes
            dir /redis-data/${PORT}
            """;

    private static GenericContainer<?> clusterContainer() {
        Integer[] ports = new Integer[CLUSTER_NODE_COUNT];
        for (int i = 0; i < CLUSTER_NODE_COUNT; i++) {
            ports[i] = CLUSTER_BASE_PORT + i;
        }
        GenericContainer<?> container = new GenericContainer<>("grokzen/redis-cluster:latest")
                .withEnv("IP", "127.0.0.1")
                .withEnv("INITIAL_PORT", String.valueOf(CLUSTER_BASE_PORT))
                .withCopyToContainer(Transferable.of(CLUSTER_NODE_TEMPLATE), "/redis-conf/redis-cluster.tmpl")
                // withExposedPorts REPLACES the list on every call, so all 6 ports must go in
                // a single call rather than accumulated one-by-one in the loop below.
                .withExposedPorts(ports)
                // Ports open (supervisord) well before "redis-cli --cluster create" (run right
                // after) actually finishes assigning slots; waiting only on listening ports lets
                // tests race a cluster that isn't routable yet. This exact line is redis-cli's
                // own completion marker for slot assignment.
                .waitingFor(Wait.forLogMessage(".*All 16384 slots covered.*", 1).withStartupTimeout(Duration.ofMinutes(2)));
        for (int port : ports) {
            container = container.withCreateContainerCmdModifier(cmd -> {
                Ports bindings = cmd.getHostConfig().getPortBindings();
                if (bindings == null) {
                    bindings = new Ports();
                }
                bindings.bind(new ExposedPort(port), Ports.Binding.bindPort(port));
                cmd.getHostConfig().withPortBindings(bindings);
            });
        }
        return container;
    }

    private static RedisSentinelConfiguration sentinelConfiguration() {
        RedisSentinelConfiguration configuration = new RedisSentinelConfiguration()
                .master(SENTINEL.masterName);
        configuration.sentinel("127.0.0.1", SENTINEL.sentinelPort);
        return configuration;
    }

    private static RedisClusterConfiguration clusterConfiguration() {
        List<String> nodes = new ArrayList<>();
        for (int i = 0; i < CLUSTER_NODE_COUNT; i++) {
            nodes.add("127.0.0.1:" + (CLUSTER_BASE_PORT + i));
        }
        return new RedisClusterConfiguration(nodes);
    }

    private static RedisConfiguration configurationFor(Topology topology) {
        return switch (topology) {
            case SENTINEL -> sentinelConfiguration();
            case CLUSTER -> clusterConfiguration();
        };
    }

    static Stream<Topology> smokeTopologies() {
        return Stream.of(Topology.SENTINEL, Topology.CLUSTER);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("smokeTopologies")
    void given_concurrent_calls_with_same_key_when_tryAcquire_then_exactly_one_acquires(Topology topology) throws Exception {
        RedisConfiguration configuration = configurationFor(topology);
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(configuration);
        try {
            // afterPropertiesSet() must run inside this try: if it throws, the finally below
            // still destroys the (partially-constructed) factory instead of leaking it.
            connectionFactory.afterPropertiesSet();
            switch (topology) {
                case SENTINEL -> smokeCheckSentinelInfra(connectionFactory);
                case CLUSTER -> smokeCheckClusterInfra(connectionFactory);
            }
            RedisIdempotentRepository repository = RedisIdempotentRepositoryTopologyITTest.repositoryUsing(connectionFactory);
            IdempotencyKey key = new IdempotencyKey("smoke-" + topology + "-" + UUID.randomUUID());
            int callCount = 3;

            ExecutorService pool = Executors.newFixedThreadPool(callCount);
            List<Future<Lease>> futures = new ArrayList<>();
            boolean completedNormally = false;
            try {
                CountDownLatch ready = new CountDownLatch(callCount);
                CountDownLatch start = new CountDownLatch(1);
                List<Callable<Lease>> tasks = new ArrayList<>();
                for (int i = 0; i < callCount; i++) {
                    tasks.add(() -> {
                        ready.countDown();
                        start.await();
                        return repository.tryAcquire(key, "smoke-payload-hash", Duration.ofSeconds(30));
                    });
                }
                for (Callable<Lease> task : tasks) {
                    futures.add(pool.submit(task));
                }
                ready.await();
                start.countDown();

                List<Lease> results = new ArrayList<>();
                for (Future<Lease> future : futures) {
                    results.add(future.get(TASK_TIMEOUT.toSeconds(), TimeUnit.SECONDS));
                }

                long acquiredCount = results.stream().filter(Lease::isAcquired).count();
                assertEquals(1, acquiredCount,
                        "exactly one concurrent caller must acquire the lease over " + topology + " (same tryAcquire -> Lease contract as Standalone)");
                long inProgressCount = results.stream().filter(l -> !l.isAcquired() && !l.hasCachedResponse()).count();
                assertEquals(callCount - 1, inProgressCount,
                        "every other concurrent caller must be told the key is already in progress over " + topology);
                completedNormally = true;
            } finally {
                // shutdown() alone doesn't interrupt threads still blocked on Redis I/O — they'd
                // keep running against infra @AfterAll may already be tearing down.
                if (completedNormally) {
                    pool.shutdown();
                } else {
                    pool.shutdownNow();
                    futures.forEach(f -> f.cancel(true));
                }
            }
        } finally {
            connectionFactory.destroy();
        }
    }

    /**
     * R-001 mitigation: proves the Sentinel infra itself is wired correctly (equivalent to
     * {@code SENTINEL get-master-addr-by-name} resolving to a host the test JVM can actually
     * reach) BEFORE running the concurrency assertions. Without this, a broken announce-ip/port
     * would surface as a confusing "expected 1 but was 3" from the concurrency asserts below
     * (tryAcquire fail-opens on any connection error, so every concurrent caller would look
     * "acquired") instead of a clear infra failure message.
     */
    private static void smokeCheckSentinelInfra(LettuceConnectionFactory sentinelAwareFactory) {
        RedisServer master;
        try (RedisSentinelConnection sentinelConnection = sentinelAwareFactory.getSentinelConnection()) {
            master = sentinelConnection.masters().stream()
                    .filter(m -> SENTINEL.masterName.equals(m.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "infra smoke (R-001): Sentinel reported no monitored master named " + SENTINEL.masterName));
        } catch (IOException e) {
            throw new UncheckedIOException("infra smoke (R-001): failed to close the Sentinel connection", e);
        }

        LettuceConnectionFactory masterFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(master.getHost(), master.getPort()));
        masterFactory.afterPropertiesSet();
        try (RedisConnection masterConnection = masterFactory.getConnection()) {
            assertEquals("PONG", masterConnection.ping(), "infra smoke (R-001): master address " + master.getHost() + ":" + master.getPort()
                    + " reported by Sentinel must be reachable from the test JVM");
        } finally {
            masterFactory.destroy();
        }
    }

    /**
     * R-002 mitigation: proves the Cluster infra itself is reachable/routable before running the
     * concurrency assertions — same reasoning as {@link #smokeCheckSentinelInfra}: without this,
     * a broken Cluster wiring would surface as the same confusing "expected 1 but was 3" instead
     * of a clear infra failure message.
     */
    private static void smokeCheckClusterInfra(LettuceConnectionFactory clusterAwareFactory) {
        try (RedisConnection connection = clusterAwareFactory.getConnection()) {
            assertEquals("PONG", connection.ping(),
                    "infra smoke (R-002): Cluster must be reachable and routable from the test JVM before running concurrency asserts");
        }
    }
}
