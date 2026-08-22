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

import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditLogProperties;
import br.com.sawcunhaos.foundation.audit.domain.entity.ActionType;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.specification.ScosAuditService;
import br.com.sawcunhaos.foundation.privacy.core.MaskingEngine;
import br.com.sawcunhaos.foundation.privacy.crypto.ScosFieldCipher;
import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.InvalidClassException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

@ConditionalOnProperty(prefix = "scos.audit", name = "enabled", havingValue = "true")
@Service("ScosAuditService")
@RequiredArgsConstructor
@Slf4j
public class ScosAuditServiceBean implements ScosAuditService {

    private final ScosAuditBatchConsumer batchConsumer;
    private final ScosAuditLogProperties scosAuditLogProperties;
    private final ObjectProvider<MaskingEngine> maskingEngineProvider;
    private final ObjectMapper objectMapper;

    @Async("ScosAuditLogAsyncExecutor")
    public void saveAuditLog(final AbstractEvent abstractEvent, final String user, final String ipAddress, final String xRequestId) {
        try {
            switch (abstractEvent) {
                case PostInsertEvent event -> enqueue(buildLog(ActionType.INSERT,
                        event.getPersister().getIdentifierTableName().toUpperCase(),
                        event.getId().toString(),
                        null,
                        createJsonObject(event.getPersister().getPropertyNames(), event.getState()),
                        user, ipAddress, xRequestId, event.getEntity().getClass()));
                case PostUpdateEvent event -> enqueue(buildLog(ActionType.UPDATE,
                        event.getPersister().getIdentifierTableName().toUpperCase(),
                        event.getId().toString(),
                        createJsonObject(event.getPersister().getPropertyNames(), event.getOldState()),
                        createJsonObject(event.getPersister().getPropertyNames(), event.getState()),
                        user, ipAddress, xRequestId, event.getEntity().getClass()));
                case PostDeleteEvent event -> enqueue(buildLog(ActionType.DELETE,
                        event.getPersister().getIdentifierTableName().toUpperCase(),
                        event.getId().toString(),
                        createJsonObject(event.getPersister().getPropertyNames(), event.getDeletedState()),
                        null,
                        user, ipAddress, xRequestId, event.getEntity().getClass()));
                default -> log.debug("Unsupported event type: {}", abstractEvent.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Error building audit event", e);
        }
    }

    @Override
    @Async("ScosAuditLogAsyncExecutor")
    public void recordRead(final String entity, final String idEntity, final String user, final String ipAddress, final String xRequestId) {
        ScosAuditLog log = ScosAuditLog.builder()
                .actionType(ActionType.SELECT)
                .entity(entity.toUpperCase())
                .idEntity(idEntity)
                .user(user)
                .originSystem(scosAuditLogProperties.getSystem())
                .executionDate(LocalDateTime.now())
                .ipAddress(ipAddress)
                .xRequestId(xRequestId)
                .build();
        batchConsumer.offerOrDlq(log);
    }

    private void enqueue(ScosAuditLog log) {
        if (log != null) {
            batchConsumer.offerOrDlq(log);
        }
    }

    private ScosAuditLog buildLog(ActionType actionType, String entity, String idEntity,
                                   String entityOld, String entityNew,
                                   String user, String ipAddress, String xRequestId,
                                   Class<?> entityClass) {
        try {
            if (!isClassAuditable(entityClass)) {
                log.debug("Not auditable class: {}", entityClass.getName());
                return null;
            }
        } catch (InvalidClassException e) {
            return null;
        }
        return ScosAuditLog.builder()
                .actionType(actionType)
                .entity(entity)
                .idEntity(idEntity)
                .entityOld(entityOld)
                .entityNew(entityNew)
                .user(user)
                .originSystem(scosAuditLogProperties.getSystem())
                .executionDate(LocalDateTime.now())
                .ipAddress(ipAddress)
                .xRequestId(xRequestId)
                .build();
    }

    private boolean isClassAuditable(Class<?> entityClass) throws InvalidClassException {
        return entityClass.getAnnotation(Auditable.class) != null;
    }

    private String resolveUser() {
        try {
            String mdcUser = MDC.get("user");
            return mdcUser != null ? mdcUser : "SYSTEM";
        } catch (Exception e) {
            return "SYSTEM";
        }
    }

    private String createJsonObject(String[] propertyNames, Object[] state) {
        if (state == null) return null;

        final MaskingEngine engine = maskingEngineProvider.getIfAvailable();
        final ScosFieldCipher cipher = engine != null ? engine.fieldCipher() : null;
        final Set<String> encryptFields = engine != null ? engine.auditEncryptFields() : Set.of();
        final boolean encryptEnabled = cipher != null && !encryptFields.isEmpty();

        // TreeMap ensures alphabetical key order, matching PostgreSQL JSONB normalization.
        // Consistent key ordering is required for deterministic hash-chain computation.
        Map<String, Object> stateMap = new TreeMap<>();
        for (int i = 0; i < propertyNames.length; i++) {
            final String name = propertyNames[i];
            String value = Objects.nonNull(state[i]) ? state[i].toString() : null;
            if (encryptEnabled && value != null && encryptFields.contains(name)) {
                value = cipher.encrypt(value);
            }
            stateMap.put(name, value);
        }
        // Null policy: Jackson includes null map values by default (unlike Gson, which omits them
        // unless serializeNulls() is set — the old GsonUtils instance did set it). No explicit
        // @JsonInclude needed here since the default already matches; see audit/README.md.
        return objectMapper.writeValueAsString(stateMap);
    }

}
