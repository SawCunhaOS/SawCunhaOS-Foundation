
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

package br.com.sawcunhaos.foundation.audit.service;

import br.com.sawcunhaos.foundation.audit.specification.ScosAuditService;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.MDC;

@RequiredArgsConstructor
public final class ScosHibernateAuditListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private final ScosAuditService scosAuditService;
    private final ScosUserAuthentication scosSoftwaresUserAuthentication;

    @Override
    public void onPostDelete(PostDeleteEvent postDeleteEvent) {
        scosAuditService.saveAuditLog(
                postDeleteEvent,
                scosSoftwaresUserAuthentication.findUserAuthentication(),
                getIpAddress(),
                getXRequestId());
    }

    @Override
    public void onPostInsert(PostInsertEvent postInsertEvent) {
        scosAuditService.saveAuditLog(
                postInsertEvent,
                scosSoftwaresUserAuthentication.findUserAuthentication(),
                getIpAddress(),
                getXRequestId());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent postUpdateEvent) {
        scosAuditService.saveAuditLog(
                postUpdateEvent,
                scosSoftwaresUserAuthentication.findUserAuthentication(),
                getIpAddress(),
                getXRequestId());
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister entityPersister) {
        return false;
    }

    private String getIpAddress() {
        return MDC.get("IS_IP");
    }

    private String getXRequestId() {
        return MDC.get("X-Request-ID");
    }

}
