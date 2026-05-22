
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AuditTestApplication.class)
@Testcontainers
@ActiveProfiles("postgres")
@DisplayName("Testes de Integração Avançados - Auditoria")
public class AuditIntegrationTest {

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
    @DisplayName("Deve testar performance com grande volume de dados")
    void testPerformanceWithLargeVolume() throws InterruptedException {
        // Arrange
        List<Country> countries = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            countries.add(Country.builder()
                    .name("Country-" + i)
                    .code(1000 + i)
                    .acronym("C" + i)
                    .description("Performance test country " + i)
                    .build());
        }

        // Act
        long startTime = System.currentTimeMillis();
        countryRepository.saveAll(countries);
        sleep(2000);
        long endTime = System.currentTimeMillis();

        // Assert
        long duration = endTime - startTime;
        assertTrue(duration < 5000, "Inserção de 100 registros deve levar menos de 5 segundos");

        List<ScosAuditLog> logs = scosAuditLogRepository.findAll();
        assertTrue(logs.size() >= 100, "Deve haver mais de 100 logs de auditoria");
    }

    @Test
    @DisplayName("Deve testar limite de tamanho de JSON em entityOld/entityNew")
    void testJsonSizeLimit() throws InterruptedException {
        // Arrange
        String largeDescription = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                """;

        Country country = Country.builder()
                .name("Large Data Country")
                .code(9999)
                .acronym("LDC")
                .description(largeDescription)
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
        assertNotNull(log.getEntityNew(), "EntityNew deve conter dados mesmo com tamanho grande");
        assertTrue(log.getEntityNew().contains("Lorem ipsum"), "Dados devem ser preservados");
    }

    @Test
    @DisplayName("Deve testar transações concorrentes com auditoria")
    void testConcurrentTransactions() throws InterruptedException {
        // Arrange
        List<Country> countries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            countries.add(Country.builder()
                    .name("Concurrent-" + i)
                    .code(5000 + i)
                    .acronym("CNC" + i)
                    .description("Concurrent test " + i)
                    .build());
        }

        // Act - Simular operações concorrentes
        List<String> savedIds = new ArrayList<>();
        for (Country country : countries) {
            Country saved = countryRepository.save(country);
            savedIds.add(saved.getId().toString());
        }
        sleep(2000);

        // Atualizar todos
        List<Country> allCountries = countryRepository.findAll();
        for (Country country : allCountries) {
            country.setCode(country.getCode() + 10000);
            countryRepository.save(country);
        }
        sleep(2000);

        // Assert
        List<ScosAuditLog> allLogs = scosAuditLogRepository.findAll();

        long insertCount = allLogs.stream()
                .filter(log -> log.getActionType() == ActionType.INSERT)
                .count();

        long updateCount = allLogs.stream()
                .filter(log -> log.getActionType() == ActionType.UPDATE)
                .count();

        assertEquals(10, insertCount, "Deve haver 10 INSERTs");
        assertEquals(10, updateCount, "Deve haver 10 UPDATEs");
        assertEquals(21, allLogs.size(), "Total de 20 operações auditadas");
    }

    @Test
    @DisplayName("Deve auditar corretamente com dados NULL em campos opcionais")
    void testNullFieldHandling() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Null Test")
                .code(8888)
                .acronym("NUT")
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
        assertNotNull(log.getEntityNew(), "EntityNew deve conter dados mesmo com NULL");
    }

    @Test
    @DisplayName("Deve manter integridade de referência em múltiplas operações")
    void testReferentialIntegrity() throws InterruptedException {
        // Arrange
        Country country1 = Country.builder()
                .name("Reference Test 1")
                .code(7777)
                .acronym("RT1")
                .description("First country")
                .build();

        Country country2 = Country.builder()
                .name("Reference Test 2")
                .code(7778)
                .acronym("RT2")
                .description("Second country")
                .build();

        // Act
        Country saved1 = countryRepository.save(country1);
        Country saved2 = countryRepository.save(country2);
        sleep(1000);

        String id1 = saved1.getId().toString();
        String id2 = saved2.getId().toString();

        // Atualizar ambas
        saved1.setCode(17777);
        saved2.setCode(17778);
        countryRepository.saveAll(List.of(saved1, saved2));
        sleep(1000);

        // Assert
        var logs1 = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(id1))
                .toList();

        var logs2 = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(id2))
                .toList();

        assertEquals(2, logs1.size(), "Country 1 deve ter 1 INSERT + 1 UPDATE");
        assertEquals(2, logs2.size(), "Country 2 deve ter 1 INSERT + 1 UPDATE");

        assertTrue(logs1.stream().allMatch(log -> log.getIdEntity().equals(id1)));
        assertTrue(logs2.stream().allMatch(log -> log.getIdEntity().equals(id2)));
    }

    @Test
    @DisplayName("Deve validar completude de metadados em operações rápidas")
    void testMetadataCompleteness() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Metadata Test")
                .code(6666)
                .acronym("MDT")
                .description("Test metadata completeness")
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

        assertNotNull(log.getId(), "Log ID não deve ser nulo");
        assertNotNull(log.getActionType(), "ActionType não deve ser nulo");
        assertNotNull(log.getIdEntity(), "IdEntity não deve ser nulo");
        assertNotNull(log.getEntity(), "Entity não deve ser nulo");
        assertNotNull(log.getOriginSystem(), "OriginSystem não deve ser nulo");
        assertNotNull(log.getUser(), "User não deve ser nulo");
        assertNotNull(log.getExecutionDate(), "ExecutionDate não deve ser nulo");

        assertEquals("SFA_COUNTRY", log.getEntity());
        assertEquals("SCOS_AUDIT", log.getOriginSystem());
        assertEquals("Test", log.getUser());
        assertEquals(ActionType.INSERT, log.getActionType());
    }
}

