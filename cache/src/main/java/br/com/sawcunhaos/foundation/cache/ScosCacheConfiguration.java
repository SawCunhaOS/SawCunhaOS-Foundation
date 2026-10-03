
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

package br.com.sawcunhaos.foundation.cache;

import br.com.sawcunhaos.foundation.cache.properties.ScosCacheModel;
import br.com.sawcunhaos.foundation.cache.properties.ScosCacheProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuração de cache Redis do módulo, ativa por padrão (desligue com
 * {@code spring.cache.enabled=false}).
 *
 * <ul>
 *   <li>Conexão Lettuce montada a partir de {@code spring.data.redis.*}: Sentinel, Cluster ou
 *       Standalone, nessa ordem de precedência.</li>
 *   <li>Chaves em {@link StringRedisSerializer}; valores em {@link PolymorphicRedisSerializer}
 *       (com a allowlist padrão, sem tipos extras: um tipo de terceiros em cache exige um
 *       {@code RedisCacheConfiguration} próprio).</li>
 *   <li>TTL padrão {@code scos.cache.redis-time-to-live}; TTL por cache em
 *       {@code scos.cache.caches[]}; prefixo global {@code scos.cache.key-prefix}.</li>
 *   <li>Resiliência: se o {@code ping} no startup falhar, o {@link CacheManager} vira um
 *       {@link NoOpCacheManager} (a aplicação sobe sem cache); falhas em runtime são só logadas
 *       pelo {@link CacheErrorHandler} e o método anotado executa normalmente.</li>
 * </ul>
 *
 * <p>Limitações reais: a decisão de fallback acontece uma única vez, no startup (um Redis que volta
 * depois não reativa o cache sem reiniciar); {@code null} nunca é cacheado; as propriedades
 * {@code enableCompression}, {@code compressionThreshold}, {@code allowNullValues} e
 * {@code maxSize} existem em {@link ScosCacheProperties}/{@link ScosCacheModel} mas não são lidas
 * por esta classe.</p>
 */
@Slf4j
@ConditionalOnProperty(
        prefix = "spring.cache",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Configuration
@EnableCaching
@RequiredArgsConstructor
@EnableConfigurationProperties({ScosCacheProperties.class, DataRedisProperties.class})
public class ScosCacheConfiguration implements CachingConfigurer {

    private final ScosCacheProperties scosCacheProperties;
    private final DataRedisProperties redisProperties;

    /**
     * Cria a {@link LettuceConnectionFactory} {@code @Primary}: comando 5 s, conexão 3 s,
     * reconexão automática e comandos rejeitados (falha rápida) enquanto desconectado.
     *
     * @return factory com conexão nativa compartilhada
     */
    @Bean(name = "ScosLettuceConnectionFactory")
    @Primary
    public LettuceConnectionFactory scosLettuceConnectionFactory() {
        // Configuração do cliente Lettuce com timeouts e resiliência
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
        log.info("Configurando ScosCache Redis Connection Factory (topologia: Sentinel)");

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
        log.info("Configurando ScosCache Redis Connection Factory (topologia: Cluster)");

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
        log.info("Configurando ScosCache Redis Connection Factory (topologia: Standalone)");

        RedisStandaloneConfiguration standaloneConfig =
                new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());

        if (Objects.nonNull(redisProperties.getPassword())) {
            standaloneConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        standaloneConfig.setDatabase(redisProperties.getDatabase());
        return standaloneConfig;
    }

    /**
     * Configuração padrão dos caches: TTL global, prefixo, chaves string e valores via
     * {@link PolymorphicRedisSerializer} (apesar do nome do helper, o formato é Smile, não JSON).
     *
     * @return configuração aplicada a todo cache sem entrada própria em {@code scos.cache.caches}
     */
    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(scosCacheProperties.getRedisTimeToLive()))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(createJacksonSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith(scosCacheProperties.getKeyPrefix() != null
                        ? scosCacheProperties.getKeyPrefix() + ":"
                        : "");
    }

    /**
     * {@link CacheManager} {@code @Primary}: testa o Redis com {@code ping} no startup e, se falhar,
     * devolve um {@link NoOpCacheManager}.
     *
     * @param redisConnectionFactory a factory {@code ScosLettuceConnectionFactory}
     * @return {@link RedisCacheManager} transaction-aware, ou {@link NoOpCacheManager} sem Redis
     */
    @Bean
    @Primary
    @DependsOn({"ScosLettuceConnectionFactory"})
    public CacheManager cacheManager(@Qualifier("ScosLettuceConnectionFactory") RedisConnectionFactory redisConnectionFactory) {
        try {

            // Testa conexão no startup. Cada cache de scos.cache.caches usa seu TTL; os demais caem
            // em defaultCacheConfiguration(). A conexão do ping não é fechada aqui.
            redisConnectionFactory.getConnection().ping();
            log.info("Redis conectado. Cache HABILITADO.");

            // Monta configurações customizadas por cache
            Map<String, RedisCacheConfiguration> cacheConfigs = buildCacheConfigurations();

            return RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(defaultCacheConfiguration())
                    .withInitialCacheConfigurations(cacheConfigs)
                    .transactionAware()
                    .build();

        } catch (Exception e) {
            log.error("Falha ao conectar Redis Sentinel: {}. Cache DESABILITADO - sistema funcionará sem cache.",
                    e.getMessage());
            log.debug("Stack trace completo:", e);
            return new NoOpCacheManager();
        }
    }

    /**
     * Constrói configurações customizadas para cada cache
     */
    private Map<String, RedisCacheConfiguration> buildCacheConfigurations() {
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        if (Objects.nonNull(scosCacheProperties.getCaches())) {
            scosCacheProperties.getCaches().forEach(cacheModel -> {
                RedisCacheConfiguration config = buildRedisCacheConfig(cacheModel);
                cacheConfigs.put(cacheModel.getName(), config);
                log.debug("Cache configurado: {} com TTL de {}s",
                        cacheModel.getName(), cacheModel.getTimeToLiveSeconds());
            });
        }

        return cacheConfigs;
    }

    /**
     * Cria configuração individual para um cache
     */
    private RedisCacheConfiguration buildRedisCacheConfig(ScosCacheModel cacheModel) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(cacheModel.getTimeToLiveSeconds()))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(createJacksonSerializer()))
                .disableCachingNullValues();

        // Adiciona prefixo se configurado
        if (Objects.nonNull(scosCacheProperties.getKeyPrefix())) {
            config = config.prefixCacheNameWith(scosCacheProperties.getKeyPrefix() + ":");
        }

        return config;
    }

    /**
     * Expõe o {@link ScosCacheKeyGenerator} como bean {@code "ScosCacheKeyGenerator"}.
     *
     * @return o gerador de chaves do módulo
     */
    @Bean("ScosCacheKeyGenerator")
    public KeyGenerator keyGenerator() {
        return new ScosCacheKeyGenerator();
    }

    /**
     * {@link CacheErrorHandler} que engole (log WARN) erros de get/put/evict/clear: o cache vira
     * best-effort e a aplicação continua sem ele. Atenção: um {@code evict} que falha deixa dado
     * velho no Redis até o TTL, sem propagar erro.
     *
     * @return handler resiliente
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Erro ao buscar do cache [{}:{}]. Executando método sem cache. Erro: {}",
                        cache.getName(), key, exception.getMessage());
                log.debug("Stack trace:", exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache,
                                            Object key, Object value) {
                log.warn("Erro ao gravar no cache [{}:{}]. Continuando sem cachear. Erro: {}",
                        cache.getName(), key, exception.getMessage());
                log.debug("Stack trace:", exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Erro ao remover do cache [{}:{}]. Continuando. Erro: {}",
                        cache.getName(), key, exception.getMessage());
                log.debug("Stack trace:", exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Erro ao limpar cache [{}]. Continuando. Erro: {}",
                        cache.getName(), exception.getMessage());
                log.debug("Stack trace:", exception);
            }
        };
    }

    private PolymorphicRedisSerializer createJacksonSerializer() {
        return new PolymorphicRedisSerializer();
    }
}
