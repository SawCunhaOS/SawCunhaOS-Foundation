package br.com.sawcunhaos.foundation.security.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "SCOS_PERMISSION")
@DynamicUpdate
public class Permission {

    @Id
    @Column(name = "PERMISSION")
    private String permission;
    @Column(name = "DESCRIPTION_PTBR")
    private String descriptionPtBr;
    @Column(name = "DESCRIPTION_ENG")
    private String descriptionEng;
    @Column(name = "SYSTEM_MODULE")
    private String module;
    @Column(name = "END_POINT")
    private String endPoint;
    @Column(name = "FEATURES", columnDefinition = "text[]")
    private List<String> features;

}
