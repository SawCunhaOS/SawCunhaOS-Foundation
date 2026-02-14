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

import java.util.UUID;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AuditTestApplication.class)
@Testcontainers
@ActiveProfiles("postgres")
@DisplayName("Tests de Auditoria com PostgreSQL")
public class AuditPostgresTest {
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
        // Limpar dados antes de cada teste
        scosAuditLogRepository.deleteAll();
        countryRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve auditar INSERT de um novo país")
    void testInsertCountry() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Test Insert")
                .code(12345)
                .acronym("TST")
                .description("País de teste para INSERT")
                .build();

        // Act
        Country savedCountry = countryRepository.save(country);
        sleep(1000);

        // Assert
        assertNotNull(savedCountry.getId(), "O ID do país deve ser gerado");

        var auditLogs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getActionType() == ActionType.INSERT && log.getIdEntity().equals(savedCountry.getId().toString()))
                .toList();

        assertEquals(1, auditLogs.size(), "Deve existir exatamente 1 log de auditoria para INSERT");

        ScosAuditLog scosAuditLog = auditLogs.get(0);
        assertEquals("Test", scosAuditLog.getUser(), "Usuário deve ser 'Test'");
        assertEquals("SCOS_AUDIT", scosAuditLog.getOriginSystem(), "Sistema de origem deve ser 'SCOS_AUDIT'");
        assertEquals(ActionType.INSERT, scosAuditLog.getActionType(), "Tipo de ação deve ser INSERT");
        assertEquals(savedCountry.getId().toString(), scosAuditLog.getIdEntity(), "ID da entidade deve corresponder");
        assertNotNull(scosAuditLog.getEntityNew(), "EntityNew deve estar preenchida");
        assertNull(scosAuditLog.getEntityOld(), "EntityOld deve ser nula para INSERT");
        assertEquals("SFA_COUNTRY", scosAuditLog.getEntity(), "Nome da entidade deve ser 'SFA_COUNTRY'");
    }

    @Test
    @DisplayName("Deve auditar UPDATE de um país existente")
    void testUpdateCountry() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Test Update")
                .code(12345)
                .acronym("TUP")
                .description("País de teste para UPDATE")
                .build();

        Country savedCountry = countryRepository.save(country);
        sleep(500);

        // Act
        savedCountry.setCode(67890);
        savedCountry.setName("Test Update - Modified");
        Country updatedCountry = countryRepository.save(savedCountry);
        sleep(1000);

        // Assert
        assertNotNull(updatedCountry.getId(), "O ID do país deve ser mantido");
        assertEquals(67890, updatedCountry.getCode(), "O código deve ser atualizado");

        var auditLogs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getActionType() == ActionType.UPDATE && log.getIdEntity().equals(updatedCountry.getId().toString()))
                .toList();

        assertEquals(1, auditLogs.size(), "Deve existir exatamente 1 log de auditoria para UPDATE");

        ScosAuditLog scosAuditLog = auditLogs.get(0);
        assertEquals("Test", scosAuditLog.getUser(), "Usuário deve ser 'Test'");
        assertEquals("SCOS_AUDIT", scosAuditLog.getOriginSystem(), "Sistema de origem deve ser 'SCOS_AUDIT'");
        assertEquals(ActionType.UPDATE, scosAuditLog.getActionType(), "Tipo de ação deve ser UPDATE");
        assertEquals(updatedCountry.getId().toString(), scosAuditLog.getIdEntity(), "ID da entidade deve corresponder");
        assertNotNull(scosAuditLog.getEntityOld(), "EntityOld deve estar preenchida");
        assertNotNull(scosAuditLog.getEntityNew(), "EntityNew deve estar preenchida");
        assertEquals("SFA_COUNTRY", scosAuditLog.getEntity(), "Nome da entidade deve ser 'SFA_COUNTRY'");
    }

    @Test
    @DisplayName("Deve auditar DELETE de um país")
    void testDeleteCountry() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Test Delete")
                .code(12345)
                .acronym("TDL")
                .description("País de teste para DELETE")
                .build();

        Country savedCountry = countryRepository.save(country);
        UUID countryId = savedCountry.getId();
        sleep(500);

        // Act
        countryRepository.delete(savedCountry);
        sleep(1000);

        // Assert
        assertTrue(countryRepository.findById(countryId).isEmpty(), "O país deve ser deletado");

        var auditLogs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getActionType() == ActionType.DELETE && log.getIdEntity().equals(countryId.toString()))
                .toList();

        assertEquals(1, auditLogs.size(), "Deve existir exatamente 1 log de auditoria para DELETE");

        ScosAuditLog scosAuditLog = auditLogs.get(0);
        assertEquals("Test", scosAuditLog.getUser(), "Usuário deve ser 'Test'");
        assertEquals("SCOS_AUDIT", scosAuditLog.getOriginSystem(), "Sistema de origem deve ser 'SCOS_AUDIT'");
        assertEquals(ActionType.DELETE, scosAuditLog.getActionType(), "Tipo de ação deve ser DELETE");
        assertEquals(countryId.toString(), scosAuditLog.getIdEntity(), "ID da entidade deve corresponder");
        assertNotNull(scosAuditLog.getEntityOld(), "EntityOld deve estar preenchida");
        assertNull(scosAuditLog.getEntityNew(), "EntityNew deve ser nula para DELETE");
        assertEquals("SFA_COUNTRY", scosAuditLog.getEntity(), "Nome da entidade deve ser 'SFA_COUNTRY'");
    }

    @Test
    @DisplayName("Deve auditar múltiplas operações em sequência")
    void testMultipleOperations() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Test Multiple Operations")
                .code(11111)
                .acronym("TMO")
                .description("País de teste para operações múltiplas")
                .build();

        // Act - INSERT
        Country savedCountry = countryRepository.save(country);
        String countryId = savedCountry.getId().toString();
        sleep(500);

        // Act - UPDATE
        savedCountry.setCode(22222);
        countryRepository.save(savedCountry);
        sleep(500);

        // Act - UPDATE again
        savedCountry.setCode(33333);
        countryRepository.save(savedCountry);
        sleep(500);

        // Act - DELETE
        countryRepository.delete(savedCountry);
        sleep(1000);

        // Assert
        var allLogs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getIdEntity().equals(countryId))
                .toList();

        long insertCount = allLogs.stream()
                .filter(log -> log.getActionType() == ActionType.INSERT)
                .count();
        long updateCount = allLogs.stream()
                .filter(log -> log.getActionType() == ActionType.UPDATE)
                .count();
        long deleteCount = allLogs.stream()
                .filter(log -> log.getActionType() == ActionType.DELETE)
                .count();

        assertEquals(1, insertCount, "Deve haver 1 log de INSERT para esta entidade");
        assertEquals(2, updateCount, "Deve haver 2 logs de UPDATE para esta entidade");
        assertEquals(1, deleteCount, "Deve haver 1 log de DELETE para esta entidade");
    }

    @Test
    @DisplayName("Deve auditar fields específicos quando alterados")
    void testAuditFieldChanges() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Test Field Changes")
                .code(54321)
                .acronym("TFC")
                .description("Descrição original")
                .build();

        Country savedCountry = countryRepository.save(country);
        String countryId = savedCountry.getId().toString();
        sleep(500);

        // Act - Alterar apenas a descrição
        savedCountry.setDescription("Descrição alterada");
        countryRepository.save(savedCountry);
        sleep(1000);

        // Assert
        var auditLogs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getActionType() == ActionType.UPDATE && log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(1, auditLogs.size(), "Deve existir exatamente 1 log de auditoria para UPDATE");

        ScosAuditLog auditLog = auditLogs.get(0);
        assertNotNull(auditLog.getEntityOld(), "Deve conter o estado anterior");
        assertNotNull(auditLog.getEntityNew(), "Deve conter o novo estado");

        assertTrue(auditLog.getEntityOld().contains("Descrição original"), "EntityOld deve conter descrição original");
        assertTrue(auditLog.getEntityNew().contains("Descrição alterada"), "EntityNew deve conter descrição alterada");
    }

    @Test
    @DisplayName("Deve conter informações de timestamp e usuário")
    void testAuditLogMetadata() throws InterruptedException {
        // Arrange
        Country country = Country.builder()
                .name("Test Metadata")
                .code(99999)
                .acronym("TMD")
                .description("País para teste de metadata")
                .build();

        // Act
        Country savedCountry = countryRepository.save(country);
        String countryId = savedCountry.getId().toString();
        sleep(1000);

        // Assert
        var auditLogs = scosAuditLogRepository.findAll().stream()
                .filter(log -> log.getActionType() == ActionType.INSERT && log.getIdEntity().equals(countryId))
                .toList();

        assertEquals(1, auditLogs.size(), "Deve existir exatamente 1 log de auditoria");

        ScosAuditLog auditLog = auditLogs.get(0);
        assertNotNull(auditLog.getExecutionDate(), "Deve conter data/hora de execução");
        assertNotNull(auditLog.getUser(), "Deve conter informação do usuário");
        assertNotNull(auditLog.getOriginSystem(), "Deve conter sistema de origem");
        assertNotNull(auditLog.getId(), "Deve conter UUID único");
    }
}
