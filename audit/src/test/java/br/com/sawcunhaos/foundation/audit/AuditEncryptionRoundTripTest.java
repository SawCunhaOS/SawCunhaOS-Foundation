
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
import br.com.sawcunhaos.foundation.privacy.core.MaskingEngine;
import br.com.sawcunhaos.foundation.privacy.crypto.JasyptCryptoKeyProvider;
import br.com.sawcunhaos.foundation.privacy.crypto.ScosFieldCipher;
import br.com.sawcunhaos.foundation.privacy.specification.DataMaskingValues;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip check that opt-in {@code auditEncryptFields} are encrypted at rest in the JSONB snapshot and can
 * be decrypted back, while non opt-in fields stay plaintext. The cipher round-trip in isolation is covered by
 * {@code ScosFieldCipherTest}; this exercises it through the real persist path (Hibernate event &rarr; audit).
 */
@SpringBootTest(classes = AuditTestApplication.class)
@Testcontainers
@ActiveProfiles("postgres")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(AuditEncryptionRoundTripTest.EncryptionConfig.class)
@DisplayName("Auditoria - cifra em repouso de PII (round-trip)")
class AuditEncryptionRoundTripTest {

    private static final String SECRET = "round-trip-secret-0123456789";

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
    @DisplayName("Cifra o campo opt-in e mantém os demais em claro, com decrypt round-trip")
    void encryptsOptInFieldAndDecrypts() throws InterruptedException {
        Country country = Country.builder()
                .name("SecretName")
                .code(4242)
                .acronym("SEC")
                .description("desc")
                .build();

        Country saved = countryRepository.save(country);
        String id = saved.getId().toString();
        sleep(1500);

        ScosAuditLog log = scosAuditLogRepository.findAll().stream()
                .filter(l -> l.getIdEntity().equals(id) && l.getActionType() == ActionType.INSERT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("INSERT audit log não encontrado"));

        JsonObject snapshot = JsonParser.parseString(log.getEntityNew()).getAsJsonObject();
        String storedName = snapshot.get("name").getAsString();
        String storedAcronym = snapshot.get("acronym").getAsString();

        assertTrue(storedName.startsWith(ScosFieldCipher.PREFIX), "name deve estar cifrado em repouso");
        assertEquals("SEC", storedAcronym, "campo não opt-in deve permanecer em claro");

        ScosFieldCipher cipher = new ScosFieldCipher(new JasyptCryptoKeyProvider(SECRET));
        assertEquals("SecretName", cipher.decrypt(storedName), "decrypt deve recuperar o valor original");
    }

    @TestConfiguration
    static class EncryptionConfig {
        @Bean
        MaskingEngine maskingEngine() {
            return MaskingEngine.builder()
                    .keyProvider(new JasyptCryptoKeyProvider(SECRET))
                    .addSpi(new DataMaskingValues() {
                        @Override
                        public Set<String> auditEncryptFields() {
                            return Set.of("name");
                        }
                    })
                    .build();
        }
    }
}
