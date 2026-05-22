package br.com.sawcunhaos.foundation.security.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "v_scos_cache_permission")
public class PermissionCache {

    @Id
    @Column(name = "FEATURE")
    private String feature;

    @Column(name = "PERMISSIONS")
    private Set<String> permissions;

}
