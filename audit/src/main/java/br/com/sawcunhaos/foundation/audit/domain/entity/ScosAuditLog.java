package br.com.sawcunhaos.foundation.audit.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@Table(name = "SFA_LOG_AUDIT")
@Entity
public class ScosAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_LOG")
    private UUID id;
    @Column(name = "ORIGIN_SYSTEM")
    private String originSystem;
    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION_TYPE")
    private ActionType actionType;
    @Column(name = "ID_ENTITY")
    private String idEntity;
    @Column(name = "ENTITY")
    private String entity;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ENTITY_OLD", columnDefinition = "jsonb")
    private String entityOld;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ENTITY_NEW", columnDefinition = "jsonb")
    private String entityNew;
    @Column(name = "LOGGED_USER")
    private String user;
    @Column(name = "EXECUTION_DATE")
    private LocalDateTime executionDate;
    @Column(name = "IP_ADDRESS")
    private String ipAddress;
    @Column(name = "X_REQUEST_ID")
    private String xRequestId;

}
