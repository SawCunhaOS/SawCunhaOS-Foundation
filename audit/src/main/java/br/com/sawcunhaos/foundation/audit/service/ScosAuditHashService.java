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

package br.com.sawcunhaos.foundation.audit.service;

import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditLogRepository;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Cálculo do hash SHA-256 encadeado de cada evento.
 *
 * <p>O hash cobre entidade, id, ação, {@code eventOrder}, JSON novo e antigo (canonicalizados, com
 * chaves ordenadas), {@code executionDate} truncada a microssegundos e o hash anterior. O primeiro
 * evento de um registro usa {@code GENESIS} como hash anterior.
 *
 * @since 1.2.0
 */
@ConditionalOnProperty(prefix = "scos.audit", name = "enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class ScosAuditHashService {

    private static final String GENESIS = "GENESIS";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ScosAuditLogRepository repository;

    /**
     * Calcula o hash SHA-256 (hexadecimal) de um evento encadeado ao anterior.
     *
     * @param log          evento cujo hash será calculado
     * @param previousHash hash do evento anterior do mesmo registro, ou {@code GENESIS}
     * @return hash SHA-256 em hexadecimal (64 caracteres)
     */
    public String computeHash(ScosAuditLog log, String previousHash) {
        // Covers metadata + JSONB content (entityNew/entityOld). The content is run through
        // canonicalizeJson, which sorts keys recursively — this neutralizes PostgreSQL JSONB
        // key reordering and whitespace changes so the hash is identical whether computed from
        // the in-memory snapshot (write path) or the DB-retrieved value (verify path).
        // executionDate is truncated to microseconds to match the persisted TIMESTAMP precision.
        String executionDateStr = log.getExecutionDate() != null
                ? log.getExecutionDate().truncatedTo(ChronoUnit.MICROS).toString()
                : "";
        String input = Objects.toString(log.getEntity(), "") +
                Objects.toString(log.getIdEntity(), "") +
                Objects.toString(log.getActionType(), "") +
                Objects.toString(log.getEventOrder(), "") +
                canonicalizeJson(log.getEntityNew()) +
                canonicalizeJson(log.getEntityOld()) +
                executionDateStr +
                previousHash;
        return sha256(input);
    }

    /**
     * Produces a canonical JSON string with keys sorted recursively, so that two semantically
     * equal JSON documents (differing only in key order or whitespace) yield the same string.
     */
    static String canonicalizeJson(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            JsonNode element = MAPPER.readTree(json);
            StringBuilder sb = new StringBuilder();
            canonicalize(element, sb);
            return sb.toString();
        } catch (JacksonException e) {
            return json;
        }
    }

    private static void canonicalize(JsonNode element, StringBuilder sb) {
        if (element.isObject()) {
            List<String> keys = new ArrayList<>();
            for (var entry : element.properties()) keys.add(entry.getKey());
            Collections.sort(keys);
            sb.append('{');
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(keys.get(i)).append("\":");
                canonicalize(element.get(keys.get(i)), sb);
            }
            sb.append('}');
        } else if (element.isArray()) {
            sb.append('[');
            for (int i = 0; i < element.size(); i++) {
                if (i > 0) sb.append(',');
                canonicalize(element.get(i), sb);
            }
            sb.append(']');
        } else {
            sb.append(element.toString());
        }
    }

    /**
     * Busca o hash do último evento do registro no banco, para encadear o próximo lote.
     *
     * @param entity   nome da entidade/tabela auditada
     * @param idEntity identificador do registro
     * @return hash do último evento, ou {@code GENESIS} se não há evento (ou ele não tem hash)
     */
    @Transactional(value = "ScosAuditLogTransactionManager", readOnly = true)
    public String findLastHashFromDb(String entity, String idEntity) {
        return repository.findLastByEntityAndIdEntity(entity, idEntity)
                .map(ScosAuditLog::getHashChain)
                .filter(h -> h != null && !h.isBlank())
                .orElse(GENESIS);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Hash anterior do primeiro evento de um registro.
     *
     * @return a constante {@code "GENESIS"}
     */
    public static String genesisHash() {
        return GENESIS;
    }

}
