package br.com.sawcunhaos.foundation.utils.configuration.cache;

import br.com.sawcunhaos.foundation.utils.configuration.cache.properties.ScosCacheModel;
import br.com.sawcunhaos.foundation.utils.configuration.cache.properties.ScosCacheProperties;
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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuração resiliente de cache com Redis Sentinel
 * - Sistema funciona sem Redis (cache desabilitado)
 * - Reconexão automática em caso de falha
 * - Configurações customizadas por cache
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
     * Cria ConnectionFactory com Sentinel e configurações resilientes
     */
    @Bean(name = "ScosLettuceConnectionFactory")
    @Primary
    public LettuceConnectionFactory scosLettuceConnectionFactory() {
        log.info("Configurando Redis Sentinel Connection Factory");

        // Configuração do Sentinel
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

        LettuceConnectionFactory factory = new LettuceConnectionFactory(sentinelConfig, clientConfig);
        factory.setShareNativeConnection(true);  // Compartilha conexão entre threads
        factory.setValidateConnection(false);     // Não valida a cada operação (performance)

        return factory;
    }

    /**
     * Configuração padrão do Redis Cache com Jackson2
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
     * CacheManager com fallback para NoOp se Redis falhar
     */
    @Bean
    @Primary
    @DependsOn({"ScosLettuceConnectionFactory"})
    public CacheManager cacheManager(@Qualifier("ScosLettuceConnectionFactory") RedisConnectionFactory redisConnectionFactory) {
        try {

            // Testa conexão no startup
            redisConnectionFactory.getConnection().ping();
            log.info("Redis Sentinel conectado. Cache HABILITADO.");

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
     * KeyGenerator customizado
     */
    @Bean("ScosCacheKeyGenerator")
    public KeyGenerator keyGenerator() {
        return new ScosCacheKeyGenerator();
    }

    /**
     * Error Handler que captura falhas do Redis em runtime
     * Permite que sistema continue funcionando mesmo com erros de cache
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
