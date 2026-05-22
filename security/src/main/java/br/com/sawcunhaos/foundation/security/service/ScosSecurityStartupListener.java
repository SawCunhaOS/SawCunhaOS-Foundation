package br.com.sawcunhaos.foundation.security.service;

import br.com.sawcunhaos.foundation.utils.specification.ScosStartupListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@Primary
public class ScosSecurityStartupListener implements ScosStartupListener {

    private final ScosSecurityCacheService scosSecurityCacheService;
    private final ScosPermissionService insideSecurityService;

    @Override
    public void onStartupSystem(ApplicationReadyEvent event) {
        log.info("Starting ScosSecurityStartupListener");

        insideSecurityService.createAndUpdatePermission();
        scosSecurityCacheService.loadPermissionCache();
        scosSecurityCacheService.loadLoginCache();

        log.info("Finished ScosSecurityStartupListener");
    }





}
