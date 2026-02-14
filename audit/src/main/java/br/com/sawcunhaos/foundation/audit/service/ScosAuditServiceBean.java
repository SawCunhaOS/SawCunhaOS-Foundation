package br.com.sawcunhaos.foundation.audit.service;

import br.com.sawcunhaos.foundation.audit.configuration.properties.ScosAuditLogProperties;
import br.com.sawcunhaos.foundation.audit.domain.entity.ActionType;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.specification.ScosAuditService;
import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import br.com.sawcunhaos.foundation.utils.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InvalidClassException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ConditionalOnProperty(prefix="scos.audit", name = "enabled", havingValue = "true")
@Service("ScosAuditService")
@RequiredArgsConstructor
@Slf4j
public class ScosAuditServiceBean implements ScosAuditService {

    private final ScosAuditLogService scosAuditLogService;
    private final ScosAuditLogProperties scosAuditLogProperties;

    @Async("ScosAuditLogAsyncExecutor")
    public void saveAuditLog(final AbstractEvent abstractEvent, final String user, final String ipAddress, final String xRequestId) {
        log.info("Init save Log Audit");
        try {
            switch (abstractEvent) {
                case PostInsertEvent persistEvent -> saveAuditLog(persistEvent, user, ipAddress, xRequestId);
                case PostUpdateEvent persistEvent -> saveAuditLog(persistEvent, user, ipAddress, xRequestId);
                case PostDeleteEvent persistEvent -> saveAuditLog(persistEvent, user, ipAddress, xRequestId);
                default -> log.info("Not support event");
            }
        } catch (Exception exception) {
            log.error("Error save audit", exception);
        }
        log.info("End save Log Audit");
    }

    private void saveAuditLog(PostInsertEvent postInsertEvent, final String user, final String ipAddress, final String xRequestId) throws InvalidClassException {
        if(!isClassAuditable(postInsertEvent.getEntity().getClass())) {
            log.debug("Not support class: {}", postInsertEvent.getEntity().getClass().getName());
            return;
        }

        ScosAuditLog scosAuditLog = ScosAuditLog.builder()
                .actionType(ActionType.INSERT)
                .entity(postInsertEvent.getPersister().getIdentifierTableName().toUpperCase())
                .idEntity(postInsertEvent.getId().toString())
                .entityNew(
                        createJsonObject(
                                postInsertEvent.getPersister().getPropertyNames(),
                                postInsertEvent.getState()
                        )
                )
                .user(user)
                .originSystem(scosAuditLogProperties.getSystem())
                .executionDate(LocalDateTime.now())
                .ipAddress(ipAddress)
                .xRequestId(xRequestId)
                .build();

        scosAuditLogService.saveLog(scosAuditLog);
    }

    private void saveAuditLog(PostUpdateEvent postUpdateEvent, final String user, final String ipAddress, final String xRequestId) throws InvalidClassException {
        if(!isClassAuditable(postUpdateEvent.getEntity().getClass())) {
            log.debug("Not support class: {}", postUpdateEvent.getEntity().getClass().getName());
            return;
        }

        ScosAuditLog scosAuditLog = ScosAuditLog.builder()
                .actionType(ActionType.UPDATE)
                .entity(postUpdateEvent.getPersister().getIdentifierTableName().toUpperCase())
                .idEntity(postUpdateEvent.getId().toString())
                .entityOld(
                        createJsonObject(
                                postUpdateEvent.getPersister().getPropertyNames(),
                                postUpdateEvent.getOldState()
                        )
                )
                .entityNew(
                        createJsonObject(
                                postUpdateEvent.getPersister().getPropertyNames(),
                                postUpdateEvent.getState()
                        )
                )
                .user(user)
                .originSystem(scosAuditLogProperties.getSystem())
                .executionDate(LocalDateTime.now())
                .ipAddress(ipAddress)
                .xRequestId(xRequestId)
                .build();

        scosAuditLogService.saveLog(scosAuditLog);
    }

    private void saveAuditLog(PostDeleteEvent postDeleteEvent, final String user, final String ipAddress, final String xRequestId) throws InvalidClassException {
        if(!isClassAuditable(postDeleteEvent.getEntity().getClass())) {
            log.debug("Not support class: {}", postDeleteEvent.getEntity().getClass().getName());
            return;
        }
        ScosAuditLog scosAuditLog = ScosAuditLog.builder()
                .actionType(ActionType.DELETE)
                .entity(postDeleteEvent.getPersister().getIdentifierTableName().toUpperCase())
                .idEntity(postDeleteEvent.getId().toString())
                .entityOld(
                        createJsonObject(
                                postDeleteEvent.getPersister().getPropertyNames(),
                                postDeleteEvent.getDeletedState()
                        )
                )
                .user(user)
                .originSystem(scosAuditLogProperties.getSystem())
                .executionDate(LocalDateTime.now())
                .ipAddress(ipAddress)
                .xRequestId(xRequestId)
                .build();

        scosAuditLogService.saveLog(scosAuditLog);
    }

    private boolean isClassAuditable(Class<?> entityClass) throws InvalidClassException {
        return entityClass.getAnnotation(Auditable.class) != null;
    }

    private String createJsonObject(String[] propertyNames, Object[] state) {
        if (state == null) return null;
        Map<String, Object> stateMap = new HashMap<>();
        for (int i = 0; i < propertyNames.length; i++) {
            stateMap.put(
                    propertyNames[i],
                    Objects.nonNull(state[i]) ? state[i].toString() : null
            );
        }
        return GsonUtils.getInstance().toJson(stateMap);
    }

}
