
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

package br.com.sawcunhaos.foundation.jdempotent.redis.configuration;


import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.cache.PolymorphicRedisSerializer;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Objects;

/**
 *
 */
@Configuration
@ConditionalOnProperty(
        prefix="scos.jdempotent", name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties({DataRedisProperties.class})
public class ScosJdempotentRedisConfiguration {

    private final DataRedisProperties redisProperties;

    @Bean(name = "JdempotentLettuceConnectionFactory")
    public LettuceConnectionFactory lettuceConnectionFactory() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))  // Timeout de comando
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofSeconds(3))  // Timeout de conexão
                                .keepAlive(true)
                                .build())
                        .timeoutOptions(TimeoutOptions.enabled())
                        .autoReconnect(true)  // Reconexão automática
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .build())
                .build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfiguration(), clientConfig);
        factory.setShareNativeConnection(true);  // Compartilha conexão entre threads
        factory.setValidateConnection(false);     // Não valida a cada operação (performance)

        return factory;
    }

    /**
     * Resolve a topologia de conexão (standalone/sentinel/cluster) a partir das mesmas
     * {@code spring.data.redis.*} que o {@code RedisAutoConfiguration} nativo do Boot usa,
     * em vez de assumir Sentinel incondicionalmente (causa do NPE em {@code getSentinel()}
     * quando a aplicação não configura o bloco {@code sentinel:}).
     */
    private RedisConfiguration redisConfiguration() {
        if (Objects.nonNull(redisProperties.getSentinel())) {
            return sentinelConfiguration();
        }
        if (Objects.nonNull(redisProperties.getCluster())) {
            return clusterConfiguration();
        }
        return standaloneConfiguration();
    }

    private RedisSentinelConfiguration sentinelConfiguration() {
        log.info("Configurando Jdempotent Redis Connection Factory (topologia: Sentinel)");

        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                .master(redisProperties.getSentinel().getMaster());

        redisProperties.getSentinel().getNodes().forEach(node -> {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : redisProperties.getPort();
            sentinelConfig.sentinel(host, port);
        });

        if (Objects.nonNull(redisProperties.getPassword())) {
            sentinelConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        sentinelConfig.setDatabase(redisProperties.getDatabase());
        return sentinelConfig;
    }

    private RedisClusterConfiguration clusterConfiguration() {
        log.info("Configurando Jdempotent Redis Connection Factory (topologia: Cluster)");

        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(redisProperties.getCluster().getNodes());

        if (Objects.nonNull(redisProperties.getCluster().getMaxRedirects())) {
            clusterConfig.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());
        }

        if (Objects.nonNull(redisProperties.getPassword())) {
            clusterConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        return clusterConfig;
    }

    private RedisStandaloneConfiguration standaloneConfiguration() {
        log.info("Configurando Jdempotent Redis Connection Factory (topologia: Standalone)");

        RedisStandaloneConfiguration standaloneConfig =
                new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());

        if (Objects.nonNull(redisProperties.getPassword())) {
            standaloneConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        standaloneConfig.setDatabase(redisProperties.getDatabase());
        return standaloneConfig;
    }

    @Bean("JdempotentRedisTemplate")
    @DependsOn({"JdempotentLettuceConnectionFactory"})
    public RedisTemplate<String, IdempotentResponseWrapper> redisTemplate(@Qualifier ("JdempotentLettuceConnectionFactory") LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate<String, IdempotentResponseWrapper> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setDefaultSerializer(new PolymorphicRedisSerializer());
        return redisTemplate;
    }
}
