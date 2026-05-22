package br.com.sawcunhaos.foundation.security.configuration.cache;

import br.com.sawcunhaos.foundation.security.configuration.properties.SecurityCacheProperties;
import br.com.sawcunhaos.foundation.security.domain.cache.LoginCacheDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(SecurityCacheProperties.class)
public class SecurityCacheMonitor {

    private final SecurityCacheProperties securityCacheProperties;

    @Autowired
    @Qualifier("loginCache")
    private Cache<String, LoginCacheDTO> loginCache;

    @Autowired
    @Qualifier("permissionCache")
    private Cache<String, Set<String>> permissionCache;

    @Scheduled(fixedRate = 60000) // A cada 1 minuto
    public void logCacheStatistics() {
        CacheStats loginStats = loginCache.stats();
        CacheStats permissionStats = permissionCache.stats();

        log.info("===== CACHE STATISTICS =====");

        log.info("Login Cache - Size: {}/{}, Hit Rate: {}, Misses: {}, Evictions: {}",
                loginCache.estimatedSize(),
                securityCacheProperties.getMaximumSizeLogins(),
                loginStats.hitRate() * 100,
                loginStats.missCount(),
                loginStats.evictionCount());

        log.info("Permissions Cache - Size: {}/{}, Hit Rate: {}, Misses: {}, Evictions: {}",
                permissionCache.estimatedSize(),
                securityCacheProperties.getMaximumSizePermission(),
                permissionStats.hitRate() * 100,
                permissionStats.missCount(),
                permissionStats.evictionCount());

        // Estimativa de memória
        long userMemory = loginCache.estimatedSize() * 50;
        long featureMemory = permissionCache.estimatedSize() * 2000;

        log.info("Estimated Memory - User: {}KB, Feature: {}KB, Total: {}KB",
                userMemory / 1024,
                featureMemory / 1024,
                (userMemory + featureMemory) / 1024);

        log.info("============================");

        // Alerta se hit rate muito baixo
        if (loginStats.hitRate() < 0.8) {
            log.warn("Usuario cache hit rate is below 80%! Consider increasing TTL");
        }
        if (permissionStats.hitRate() < 0.9) {
            log.warn("Cargo cache hit rate is below 90%! Consider increasing TTL");
        }
    }
}
