
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

package br.com.sawcunhaos.foundation.jpa.entity;

import com.querydsl.core.annotations.QueryEmbeddable;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Superclasse mapeada ({@code @MappedSuperclass}) com as colunas de auditoria comuns às entidades:
 * {@code CREATED_AT}, {@code UPDATED_AT} e {@code USER_AT}. As entidades da aplicação a estendem e
 * herdam os campos e seus getters/setters (Lombok).
 *
 * <p>Preenchimento:
 * <ul>
 *   <li>{@code createdAt}/{@code updatedAt}: pelo Hibernate ({@code @CreationTimestamp} /
 *       {@code @UpdateTimestamp}), sem configuração extra.</li>
 *   <li>{@code userAt}: <b>manual</b>, via {@link #updateAuditInfo(String)}. O
 *       {@code AuditingEntityListener} está registrado, mas a classe não tem {@code @CreatedBy}/
 *       {@code @LastModifiedBy}, então ele não preenche {@code userAt} sozinho.</li>
 * </ul>
 *
 * @since 1.2.0
 */
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@QueryEmbeddable
public abstract class BaseEntity {

    /** Instante de criação, gravado pelo Hibernate no primeiro insert. */
    @CreationTimestamp
    @Column(name = "CREATED_AT")
    private Instant createdAt;
    /** Instante da última alteração, regravado pelo Hibernate a cada update. */
    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private Instant updatedAt;
    /** Usuário da última operação; só muda via {@link #updateAuditInfo(String)}. */
    @Column(name = "USER_AT")
    private String userAt;

    /**
     * Registra o usuário responsável pela operação em {@code userAt}. Chame antes de salvar;
     * nada o invoca automaticamente.
     *
     * @param user identificação do usuário (aceita {@code null})
     */
    public void updateAuditInfo(String user) {
        userAt = user;
    }

}