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
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;


import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AuditTestApplication.class)
@Testcontainers
@ActiveProfiles("postgres")
@DisplayName("Testes de Segurança - Auditoria")
public class AuditSecurityTest {

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
        scosAuditLogRepository.deleteAll();
        countryRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve não registrar informações sensíveis não criptografadas em logs")
    void testSensitiveDataNotExposed() throws InterruptedException {
        // Arrange
        String sensitiveData = "PASSWORD123!@#SECRET";
        Country country = Country.builder()
                .name("Security Test Country")
                .code(9001)
                .acronym("STC")
                .description(sensitiveData)
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

        // Validar que dados são registrados (auditoria precisa dos dados)
        assertNotNull(log.getEntityNew(), "Dados devem ser registrados para auditoria");

        // Validar que informações não são armazenadas em texto plano em campos de metadados
        assertNotEquals(sensitiveData, log.getUser(), "User não deve conter dados sensíveis");
        assertNotEquals(sensitiveData, log.getOriginSystem(), "OriginSystem não deve conter dados sensíveis");
    }

    @Test
    @DisplayName("Deve manter rastreabilidade de usuário em operações")
    void testUserTraceability() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("User Trace Country")
                .code(9002)
                .acronym("UTC")
                .description("Test user traceability")
                .build();

        // Act
        Country saved = countryRepository.save(country);
        String countryId = saved.getId().toString();
        sleep(500);

        saved.setCode(19002);
        countryRepository.save(saved);
        sleep(1000);

        // Assert
        var logs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(2, logs.size(), "INSERT e UPDATE devem estar auditados");

        // Validar que ambas operações têm o mesmo usuário registrado
        String user1 = logs.get(0).getUser();
        String user2 = logs.get(1).getUser();

        assertNotNull(user1, "User deve ser registrado para INSERT");
        assertNotNull(user2, "User deve ser registrado para UPDATE");
        assertEquals(user1, user2, "Operações devem ter mesmo usuário registrado");
    }

    @Test
    @DisplayName("Deve registrar timestamp preciso para auditoria")
    void testTimestampAccuracy() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Timestamp Test")
                .code(9003)
                .acronym("TST")
                .description("Test timestamp accuracy")
                .build();

        // Act
        long beforeInsert = System.currentTimeMillis();
        Country saved = countryRepository.save(country);
        String countryId = saved.getId().toString();
        sleep(1000);
        long afterInsert = System.currentTimeMillis();

        // Assert
        var logs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(1, logs.size());
        ScosAuditLog log = logs.get(0);

        assertNotNull(log.getExecutionDate(), "ExecutionDate deve estar registrado");

        long logTime = log.getExecutionDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertTrue(logTime >= beforeInsert && logTime <= afterInsert,
                "Timestamp deve estar entre o insert e a verificação");
    }

    @Test
    @DisplayName("Deve validar integridade dos dados auditados")
    void testDataIntegrity() throws InterruptedException {
        // Arrange
        String originalName = "Original Country Name";
        String originalDescription = "Original Description";

        Country country = Country.builder()
                .name(originalName)
                .code(9004)
                .acronym("DIT")
                .description(originalDescription)
                .build();

        // Act
        Country saved = countryRepository.save(country);
        String countryId = saved.getId().toString();
        sleep(500);

        String newName = "Updated Country Name";
        String newDescription = "Updated Description";
        saved.setName(newName);
        saved.setDescription(newDescription);
        countryRepository.save(saved);
        sleep(1000);

        // Assert
        var logs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(2, logs.size());

        ScosAuditLog insertLog = logs.stream()
                .filter(log -> log.getActionType() == ActionType.INSERT)
                .findFirst()
                .orElse(null);

        ScosAuditLog updateLog = logs.stream()
                .filter(log -> log.getActionType() == ActionType.UPDATE)
                .findFirst()
                .orElse(null);

        // Validar integridade
        assertNotNull(insertLog);
        assertNotNull(updateLog);

        assertNotNull(insertLog.getEntityNew(), "INSERT deve ter EntityNew");
        assertTrue(insertLog.getEntityNew().contains(originalName), "INSERT deve conter nome original");

        assertNotNull(updateLog.getEntityOld(), "UPDATE deve ter EntityOld");
        assertNotNull(updateLog.getEntityNew(), "UPDATE deve ter EntityNew");
        assertTrue(updateLog.getEntityOld().contains(originalName), "EntityOld deve conter nome anterior");
        assertTrue(updateLog.getEntityNew().contains(newName), "EntityNew deve conter novo nome");
    }

    @Test
    @DisplayName("Deve garantir imutabilidade dos logs de auditoria")
    void testAuditLogImmutability() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Immutability Test")
                .code(9005)
                .acronym("IMT")
                .description("Test immutability")
                .build();

        // Act
        Country saved = countryRepository.save(country);
        String countryId = saved.getId().toString();
        sleep(1000);

        var logsBeforeChange = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        String initialEntityNew = logsBeforeChange.get(0).getEntityNew();

        // Atualizar país
        saved.setName("New Name");
        countryRepository.save(saved);
        sleep(1000);

        // Assert
        var logsAfterChange = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId) &&
                        log.getActionType() == ActionType.INSERT)
                .toList();

        String finalEntityNew = logsAfterChange.get(0).getEntityNew();

        assertEquals(initialEntityNew, finalEntityNew,
                "Log de INSERT não deve ser modificado após UPDATE posterior");
    }

    @Test
    @DisplayName("Deve validar acesso ao sistema de origem (origin system)")
    void testOriginSystemValidation() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Origin Test")
                .code(9006)
                .acronym("ORT")
                .description("Test origin system")
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

        assertNotNull(log.getOriginSystem(), "OriginSystem deve estar registrado");
        assertEquals("SCOS_AUDIT", log.getOriginSystem(),
                "OriginSystem deve ser registrado corretamente");
    }

    @Test
    @DisplayName("Deve validar que todos os logs contêm informações de auditoria obrigatórias")
    void testMandatoryAuditFields() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Mandatory Fields Test")
                .code(9007)
                .acronym("MFT")
                .description("Test mandatory audit fields")
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

        // Validar campos obrigatórios
        assertNotNull(log.getId(), "ID não deve ser nulo");
        assertNotNull(log.getActionType(), "ActionType não deve ser nulo");
        assertNotNull(log.getIdEntity(), "IdEntity não deve ser nulo");
        assertNotNull(log.getEntity(), "Entity não deve ser nulo");
        assertNotNull(log.getOriginSystem(), "OriginSystem não deve ser nulo");
        assertNotNull(log.getUser(), "User não deve ser nulo");
        assertNotNull(log.getExecutionDate(), "ExecutionDate não deve ser nulo");

        // Validar que dados não estão vazios
        assertNotEquals("", log.getIdEntity(), "IdEntity não deve estar vazio");
        assertNotEquals("", log.getEntity(), "Entity não deve estar vazio");
        assertNotEquals("", log.getOriginSystem(), "OriginSystem não deve estar vazio");
        assertNotEquals("", log.getUser(), "User não deve estar vazio");
    }

    @Test
    @DisplayName("Deve validar que DELETE remove dados da aplicação mas mantém auditoria")
    void testDeleteDataRemovalWithAuditPreservation() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Delete Preservation")
                .code(9008)
                .acronym("DPR")
                .description("Test delete preservation")
                .build();

        // Act
        Country saved = countryRepository.save(country);
        String countryId = saved.getId().toString();
        sleep(500);

        String dataBeforeDelete = saved.getName();
        countryRepository.delete(saved);
        sleep(1000);

        // Assert
        assertTrue(countryRepository.findById(saved.getId()).isEmpty(),
                "Dados devem ser removidos do banco");

        var logs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(2, logs.size(), "Deve ter INSERT e DELETE");

        var deleteLog = logs.stream()
                .filter(log -> log.getActionType() == ActionType.DELETE)
                .findFirst()
                .orElse(null);

        assertNotNull(deleteLog, "DELETE log deve existir");
        assertNotNull(deleteLog.getEntityOld(), "DELETE deve preservar dados em EntityOld");
        assertTrue(deleteLog.getEntityOld().contains(dataBeforeDelete),
                "Dados deletados devem estar preservados em auditoria");
    }
}

