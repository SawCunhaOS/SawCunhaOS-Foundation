package br.com.sawcunhaos.foundation.audit.domain.entity;

import br.com.sawcunhaos.foundation.utils.annotation.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "SFA_COUNTRY")
@Auditable
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "COUNTRY_ID")
    private UUID id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "ACRONYM")
    private String acronym;
    @Column(name = "DESCRIPTION")
    private String description;
    @Column(name = "CODE")
    private Integer code;
}
