package br.com.sawcunhaos.foundation.jdempotent.redis.configuration;


import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;
import br.com.sawcunhaos.foundation.utils.configuration.cache.PolymorphicRedisSerializer;
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
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
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
        log.info("Configurando Jdempotent Redis Sentinel Connection Factory");

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
