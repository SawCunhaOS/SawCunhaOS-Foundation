package br.com.sawcunhaos.foundation.security.service;


import br.com.sawcunhaos.foundation.exception.error.ScosSecurityException;
import br.com.sawcunhaos.foundation.security.domain.cache.LoginCacheDTO;
import br.com.sawcunhaos.foundation.security.domain.repository.LoginCacheRepository;
import br.com.sawcunhaos.foundation.security.domain.repository.PermissionCacheRepository;
import br.com.sawcunhaos.foundation.security.utils.SecurityExceptionCode;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScosSecurityService {

    private final LoginCacheRepository loginCacheRepository;
    private final PermissionCacheRepository permissionCacheRepository;

    @Autowired
    @Qualifier("loginCache")
    private Cache<String, LoginCacheDTO> loginCache;

    @Autowired
    @Qualifier("permissionCache")
    private Cache<String, Set<String>> permissionCache;

    public Collection<GrantedAuthority> getAllGrantedAuthority(@NonNull String login) {
        LoginCacheDTO loginCacheDTO = loginCache.getIfPresent(login);
        if (loginCacheDTO == null) {
            log.warn("Login {} not found in cache", login);
            loginCacheDTO = loginCacheRepository.findByLogin(login);
            loginCache.put(login, loginCacheDTO);
        }

        validateLogin(loginCacheDTO);

        Set<String> permissions = loginCacheDTO.features().stream().flatMap(
                feature -> {
                    Set<String> perms = null;
                    try {
                        perms = permissionCache.getIfPresent(feature);
                    } catch (Exception e) {
                        log.warn("Error loading permissions from cache");
                        log.debug("Error loading permissions from cache", e);
                    }
                    if (perms == null) {
                        log.warn("Feature {} not found in cache", feature);
                        perms = permissionCacheRepository.findPermissionByFeature(feature);
                        if (perms.isEmpty()) {
                            log.warn("No permissions found for feature {}", feature);
                        } else {
                            log.info("Loaded {} permissions for feature {} from database", perms.size(), feature);
                            permissionCache.put(feature, perms);
                        }
                    }
                    return perms.stream();
                }
        ).collect(Collectors.toSet());

        return permissions.stream()
                .map(pc -> (GrantedAuthority) pc::toString)
                .collect(Collectors.toSet());
    }

    private void validateLogin(@NonNull LoginCacheDTO loginCacheDTO) {
        if (!loginCacheDTO.active()) throw new ScosSecurityException(SecurityExceptionCode.AUTH_007);

        if (!loginCacheDTO.status().equals("ENABLE")) {
            throw new ScosSecurityException(SecurityExceptionCode.AUTH_008, loginCacheDTO.status());
        }
    }

}
