package br.com.sawcunhaos.foundation.security.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Set;

@Entity
@Table(name = "v_scos_cache_login")
public class LoginCache {
    @Id
    @Column(name = "login")
    private String login;
    @Column(name = "profile_id")
    private Long profileId;
    @Column(name = "status")
    private String status;
    @Column(name = "active")
    private boolean active;
    @Column(name = "features")
    private Set<String> features;
}
