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
import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.MDC;

@RequiredArgsConstructor
public final class ScosHibernateAuditListener
        implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener, PostLoadEventListener {

    private final ScosAuditService scosAuditService;
    private final ScosUserAuthentication scosSoftwaresUserAuthentication;

    @Override
    public void onPostDelete(PostDeleteEvent postDeleteEvent) {
        if (!isAuditable(postDeleteEvent.getEntity())) return;
        scosAuditService.saveAuditLog(
                postDeleteEvent,
                scosSoftwaresUserAuthentication.findUserAuthentication(),
                getIpAddress(),
                getXRequestId());
    }

    @Override
    public void onPostInsert(PostInsertEvent postInsertEvent) {
        if (!isAuditable(postInsertEvent.getEntity())) return;
        scosAuditService.saveAuditLog(
                postInsertEvent,
                scosSoftwaresUserAuthentication.findUserAuthentication(),
                getIpAddress(),
                getXRequestId());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent postUpdateEvent) {
        if (!isAuditable(postUpdateEvent.getEntity())) return;
        scosAuditService.saveAuditLog(
                postUpdateEvent,
                scosSoftwaresUserAuthentication.findUserAuthentication(),
                getIpAddress(),
                getXRequestId());
    }

    @Override
    public void onPostLoad(PostLoadEvent postLoadEvent) {
        Auditable auditable = postLoadEvent.getEntity().getClass().getAnnotation(Auditable.class);
        if (auditable == null || !auditable.auditRead()) {
            return;
        }
        scosAuditService.recordRead(
                postLoadEvent.getPersister().getTableName().toUpperCase(),
                postLoadEvent.getId().toString(),
                scosSoftwaresUserAuthentication.findUserAuthentication(),
                getIpAddress(),
                getXRequestId());
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister entityPersister) {
        return false;
    }

    private boolean isAuditable(Object entity) {
        return entity != null && entity.getClass().isAnnotationPresent(Auditable.class);
    }

    private String getIpAddress() {
        return MDC.get("IS_IP");
    }

    private String getXRequestId() {
        return MDC.get("X-Request-ID");
    }

}
