package br.com.sawcunhaos.foundation.security.service;

import br.com.sawcunhaos.foundation.security.domain.cache.LoginCacheDTO;
import br.com.sawcunhaos.foundation.security.domain.repository.LoginCacheRepository;
import br.com.sawcunhaos.foundation.security.domain.repository.PermissionCacheRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScosSecurityCacheService {

    private final LoginCacheRepository loginCacheRepository;
    private final PermissionCacheRepository permissionCacheRepository;

    @Autowired
    @Qualifier("loginCache")
    private Cache<String, LoginCacheDTO> loginCache;

    @Autowired
    @Qualifier("permissionCache")
    private Cache<String, Set<String>> permissionCache;

    public void loadPermissionCache() {
        log.info("Loading permission cache");
        permissionCacheRepository.findAll()
                .forEach(
                        loginCacheDTO -> permissionCache.put(loginCacheDTO.getFeature(), loginCacheDTO.getPermissions())
                );

        log.info("Finished loading permission cache");
    }

    public void loadLoginCache() {
        log.info("Loading login cache");
        loginCacheRepository.findAllLoginCache().stream()
                .parallel()
                .forEach(loginCacheDTO -> loginCache.put(loginCacheDTO.login(), loginCacheDTO));

        log.info("Finished login permission cache");
    }

    public void clearCache() {
        log.info("Clearing all caches");
        loginCache.invalidateAll();
        permissionCache.invalidateAll();
        log.info("Finished clearing all caches");
        log.info("Loading caches again");
        loadPermissionCache();
        loadLoginCache();
        log.info("Finished loading caches again");
    }

}
