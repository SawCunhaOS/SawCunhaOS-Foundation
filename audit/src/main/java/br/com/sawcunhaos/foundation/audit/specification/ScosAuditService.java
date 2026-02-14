package br.com.sawcunhaos.foundation.audit.specification;

import org.hibernate.event.spi.AbstractEvent;

public interface ScosAuditService {

    void saveAuditLog(AbstractEvent listener, String user, String ipAddress, String xRequestId);
}
