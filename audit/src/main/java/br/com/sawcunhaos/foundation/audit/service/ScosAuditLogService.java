package br.com.sawcunhaos.foundation.audit.service;

import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@ConditionalOnProperty(prefix="scos.audit", name = "enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class ScosAuditLogService {

    private final ScosAuditLogRepository scosAuditLogRepository;

    @Transactional("ScosAuditLogTransactionManager")
    public void saveLog(final ScosAuditLog scosAuditLog) {
        scosAuditLogRepository.saveAndFlush(scosAuditLog);
    }


}
