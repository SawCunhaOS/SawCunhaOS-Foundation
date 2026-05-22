package br.com.sawcunhaos.foundation.security.configuration.cache;


import br.com.sawcunhaos.foundation.security.configuration.properties.SecurityCacheProperties;
import br.com.sawcunhaos.foundation.security.domain.cache.LoginCacheDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(SecurityCacheProperties.class)
public class SecurityCacheConfig {

    private final SecurityCacheProperties securityCacheProperties;

    @Bean("loginCache")
    public Cache<String, LoginCacheDTO> loginCache(MeterRegistry meterRegistry) {
        Cache<String, LoginCacheDTO> cache = Caffeine.newBuilder()
                .maximumSize(securityCacheProperties.getMaximumSizeLogins())
                .expireAfterWrite(securityCacheProperties.getExpireAfterWriteLogin(), TimeUnit.MINUTES)
                .expireAfterAccess(securityCacheProperties.getExpireAfterAccessLogin(), TimeUnit.MINUTES)
                .recordStats()
                .removalListener((String key, LoginCacheDTO value, RemovalCause cause) -> {
                    if (cause == RemovalCause.SIZE) {
                        log.warn("Usuario cache eviction by size: {}", key);
                    }
                })
                .build();
        CaffeineCacheMetrics.monitor(meterRegistry, cache, "loginCache");
        return cache;
    }

    @Bean("permissionCache")
    public Cache<String, Set<String>> permissionCache(MeterRegistry meterRegistry) {
        Cache<String, Set<String>> cache = Caffeine.newBuilder()
                .maximumSize(securityCacheProperties.getMaximumSizePermission())
                .expireAfterWrite(securityCacheProperties.getExpireAfterWritePermission(), TimeUnit.MINUTES)
                .recordStats()
                .removalListener((String key, Set<String> value, RemovalCause cause) -> {
                    log.info("Cargo permissions cache removed: {} - Cause: {}",
                            key, cause);
                })
                .build();
        CaffeineCacheMetrics.monitor(meterRegistry, cache, "permissionCache");
        return cache;
    }

}
