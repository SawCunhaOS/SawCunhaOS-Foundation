package br.com.sawcunhaos.foundation.audit.domain.repository;

import br.com.sawcunhaos.foundation.audit.domain.entity.ActionType;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScosAuditLogRepository extends JpaRepository<ScosAuditLog, UUID> {

    Optional<ScosAuditLog> findByActionType(ActionType actionType);

}
