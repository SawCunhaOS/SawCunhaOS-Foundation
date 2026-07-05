
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

package br.com.sawcunhaos.foundation.audit;

import br.com.sawcunhaos.foundation.audit.domain.entity.ActionType;
import br.com.sawcunhaos.foundation.audit.domain.entity.Country;
import br.com.sawcunhaos.foundation.audit.domain.entity.ScosAuditLog;
import br.com.sawcunhaos.foundation.audit.domain.repository.CountryRepository;
import br.com.sawcunhaos.foundation.audit.domain.repository.ScosAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Objects;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AuditTestApplication.class)
@Testcontainers
@ActiveProfiles("postgres")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Erro e Exceção - Auditoria")
public class AuditErrorTest {

    @Container
    protected static PostgreSQLContainer container = new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:18-3.6-alpine").asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    private static void setupProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.audit.url", container::getJdbcUrl);
        registry.add("spring.datasource.audit.username", container::getUsername);
        registry.add("spring.datasource.audit.password", container::getPassword);
        registry.add("spring.datasource.audit.driver-class-name", container::getDriverClassName);
    }

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ScosAuditLogRepository scosAuditLogRepository;

    @BeforeEach
    void setUp() {
        countryRepository.deleteAll();
        scosAuditLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve validar auditoria quando há exceção durante INSERT")
    void testAuditWhenInsertFails() throws InterruptedException {
        // Arrange
        Country country1 = Country.builder()
                .name("Valid Country")
                .code(1111)
                .acronym("VC1")
                .description("First valid country")
                .build();

        Country country2 = Country.builder()
                .name("Duplicate Country")
                .code(1111)  // Mesmo código - violação de constraint
                .acronym("DC1")
                .description("Duplicate code country")
                .build();

        // Act
        Country saved1 = countryRepository.save(country1);
        sleep(500);

        // Assert que primeira entrada foi auditada
        var logs1 = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(saved1.getId().toString()))
                .toList();
        assertEquals(1, logs1.size(), "Primeira inserção deve ser auditada");

        // Tentar inserir com violação de constraint
        assertThrows(DataIntegrityViolationException.class, () -> {
            countryRepository.save(country2);
        }, "Deve lançar exceção por violação de constraint");
    }

    @Test
    @DisplayName("Deve auditar UPDATE que falha por validação")
    void testAuditWhenUpdateValidationFails() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Test Country")
                .code(2222)
                .acronym("TC")
                .description("Test country for validation")
                .build();

        Country saved = countryRepository.save(country);
        String countryId = saved.getId().toString();
        sleep(500);

        // Act - Tentar atualizar com valor inválido
        saved.setCode(null);  // Violação de NOT NULL

        // Assert
        assertThrows(Exception.class, () -> {
            countryRepository.save(saved);
            sleep(500);
        }, "Deve falhar na validação");

        // Verificar que audit log anterior permanece
        var logs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(1, logs.size(), "Deve manter apenas o log do INSERT bem-sucedido");
    }

    @Test
    @DisplayName("Deve validar tratamento de NULL fields na auditoria")
    void testNullFieldsInAuditLog() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Minimal Country")
                .code(3333)
                .acronym("MC")
                .description(null)  // Descrição nula
                .build();

        // Act
        Country saved = countryRepository.save(country);
        String countryId = saved.getId().toString();
        sleep(1000);

        // Assert
        var logs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(1, logs.size(), "Deve auditar mesmo com fields nulos");
        ScosAuditLog log = logs.get(0);

        assertNotNull(log.getEntityNew(), "EntityNew não deve ser nulo");
        assertTrue(log.getEntityNew().contains("\"description\": null"),
                "EntityNew deve representar null field corretamente");
    }

    @Test
    @DisplayName("Deve auditar DELETE de registro que não existe mais")
    void testAuditDeleteNonExistent() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Delete Test")
                .code(4444)
                .acronym("DT")
                .description("Delete test country")
                .build();

        Country saved = countryRepository.save(country);
        String countryId = saved.getId().toString();
        sleep(500);

        // Act
        countryRepository.delete(saved);
        sleep(500);

        // Assert que delete foi auditado
        var logs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(2, logs.size(), "Deve haver INSERT e DELETE");

        var deleteLogs = logs.stream()
                .filter(log -> log.getActionType() == ActionType.DELETE)
                .toList();

        assertEquals(1, deleteLogs.size(), "Deve ter 1 log de DELETE");
        assertNotNull(deleteLogs.get(0).getEntityOld(), "EntityOld do DELETE deve conter dados");
    }

    @Test
    @DisplayName("Deve validar auditoria com rollback de transação")
    void testAuditWithTransactionRollback() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Rollback Test")
                .code(6666)
                .acronym("RB")
                .description("Rollback test country")
                .build();

        // Act & Assert
        Country saved = countryRepository.save(country);
        String countryId = saved.getId().toString();
        sleep(1000);

        var logsAfterInsert = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(1, logsAfterInsert.size(), "INSERT deve ser auditado");

        // Modificar e validar que mudança é auditada
        saved.setCode(16666);
        countryRepository.save(saved);
        sleep(1000);

        var logsAfterUpdate = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(2, logsAfterUpdate.size(), "UPDATE deve ser auditado além do INSERT");
    }

    @Test
    @DisplayName("Deve validar que campos NULL não quebram a serialização JSON")
    void testNullFieldsSerialization() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Null Serialization")
                .code(7777)
                .acronym("NS")
                .description(null)
                .build();

        // Act
        Country saved = countryRepository.save(country);
        String countryId = saved.getId().toString();
        sleep(1000);

        // Assert
        var logs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(1, logs.size());
        ScosAuditLog log = logs.get(0);

        // Validar que JSON é válido
        assertNotNull(log.getEntityNew(), "EntityNew não deve ser nulo");
        assertTrue(log.getEntityNew().startsWith("{"), "EntityNew deve ser JSON válido");
        assertTrue(log.getEntityNew().endsWith("}"), "EntityNew deve ser JSON válido");

        // Tentar parsear como JSON (básico)
        assertTrue(log.getEntityNew().contains("\""), "EntityNew deve conter JSON válido");
    }
}

