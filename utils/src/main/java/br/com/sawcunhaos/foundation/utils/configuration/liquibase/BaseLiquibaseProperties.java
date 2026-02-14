package br.com.sawcunhaos.foundation.utils.configuration.liquibase;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Propriedades base para configuração do Liquibase
 * Estende as propriedades padrões do Spring Boot com funcionalidades adicionais
 */
@Data
public abstract class BaseLiquibaseProperties {

    // ========================================
    // CONFIGURAÇÕES PRINCIPAIS
    // ========================================

    /**
     * Localização do arquivo de changelog master
     * Pode ser classpath, file system ou URL
     *
     * Exemplos:
     * - classpath:/db/changelog/db.changelog-master.yaml
     * - classpath:/db/changelog/db.changelog-master.xml
     * - file:/opt/liquibase/changelog.sql
     */
    @NotBlank(message = "ChangeLog é obrigatório")
    private String changeLog = "classpath:/db/changelog/db.changelog-master.yaml";

    /**
     * Habilita/desabilita a execução do Liquibase
     * false: migrations não serão executadas
     */
    private boolean enabled = true;

    // ========================================
    // SCHEMAS E TABLESPACES
    // ========================================

    /**
     * Schema padrão do banco de dados onde os objetos serão criados
     * Se null, usa o schema padrão da conexão
     */
    private String defaultSchema;

    /**
     * Schema onde as tabelas de controle do Liquibase serão criadas
     * Se null, usa o defaultSchema
     */
    private String liquibaseSchema;

    /**
     * Tablespace para as tabelas do Liquibase (Oracle específico)
     */
    private String liquibaseTablespace;

    // ========================================
    // TABELAS DE CONTROLE
    // ========================================

    /**
     * Nome customizado da tabela de changelog
     * Armazena histórico de changesets executados
     */
    private String databaseChangeLogTable = "IF_DATABASECHANGELOG";

    /**
     * Nome customizado da tabela de lock
     * Previne execuções concorrentes do Liquibase
     */
    private String databaseChangeLogLockTable = "IF_DATABASECHANGELOGLOCK";

    // ========================================
    // FILTROS E CONTEXTOS
    // ========================================

    /**
     * Contextos para executar (separados por vírgula)
     * Permite executar changesets específicos por ambiente
     *
     * Exemplo: "dev,test" ou "prod"
     */
    private String contexts;

    /**
     * Labels para filtrar changesets
     * Permite controle fino sobre quais changesets executar
     *
     * Exemplo: ["v1.0", "hotfix", "feature-x"]
     */
    private List<String> labelFilter;

    /**
     * Labels como string (alternativa ao labelFilter)
     * Formato: "label1 OR label2 AND !label3"
     */
    private String labels;

    // ========================================
    // CREDENCIAIS (DATASOURCE ALTERNATIVO)
    // ========================================

    /**
     * URL JDBC customizada (opcional)
     * Se não fornecida, usa o DataSource principal
     */
    private String url;

    /**
     * Usuário do banco (opcional)
     * Se não fornecido, usa o DataSource principal
     */
    private String user;

    /**
     * Senha do banco (opcional)
     * Se não fornecida, usa o DataSource principal
     */
    private String password;

    /**
     * Driver JDBC customizado (opcional)
     */
    private String driverClassName;

    // ========================================
    // OPERAÇÕES ESPECIAIS
    // ========================================

    /**
     * ⚠️ CUIDADO: Remove TODOS os objetos do schema antes de rodar
     * NUNCA use true em produção!
     *
     * Útil para resetar ambiente de desenvolvimento
     */
    private boolean dropFirst = false;

    /**
     * Limpa checksums armazenados na próxima execução
     * Útil quando changesets foram modificados manualmente
     */
    private boolean clearChecksums = false;

    /**
     * Testa rollback após cada update (dev/test only)
     * Valida se os changesets são reversíveis
     */
    private boolean testRollbackOnUpdate = false;

    // ========================================
    // VERSIONAMENTO E TAGS
    // ========================================

    /**
     * Tag para marcar o estado atual do banco
     * Permite rollback para versões específicas
     *
     * Exemplo: "v1.0.0", "release-2024-01"
     */
    private String tag;

    // ========================================
    // PARÂMETROS E CUSTOMIZAÇÕES
    // ========================================

    /**
     * Parâmetros customizados passados para os changesets
     * Acessíveis via ${parametro} nos arquivos de changelog
     *
     * Exemplo:
     * parameters:
     *   app.version: "1.0.0"
     *   environment: "production"
     */
    private Map<String, String> parameters = new HashMap<>();

    /**
     * Arquivo para salvar SQL de rollback
     * Útil para auditoria e rollback manual
     */
    private File rollbackFile;

    // ========================================
    // NOVAS PROPRIEDADES (FALTAVAM)
    // ========================================

    /**
     * Timeout para lock do Liquibase (minutos)
     * Previne deadlocks em ambientes concorrentes
     * Default: 5 minutos
     */
    private Duration lockTimeout = Duration.ofMinutes(5);

    /**
     * Valida XML/YAML dos changesets antes de executar
     * Recomendado: true
     */
    private boolean validateOnMigrate = true;

    /**
     * Ordem de execução dos changesets
     * - ALPHA: ordem alfabética
     * - FILENAME: ordem por nome de arquivo
     * Default: FILENAME
     */
    private String changeLogOrder = "FILENAME";

    /**
     * Executa Liquibase em modo offline
     * Útil para gerar SQL sem executar
     */
    private boolean offline = false;

    /**
     * Diretório de saída para modo offline
     */
    private File outputDefaultSchema;

    /**
     * Catalog do banco de dados (MySQL/PostgreSQL)
     */
    private String defaultCatalog;

    /**
     * Catalog para tabelas do Liquibase
     */
    private String liquibaseCatalog;

    /**
     * Formato de saída do changelog gerado
     * Valores: XML, YAML, JSON, SQL
     */
    private String changeLogFormat = "YAML";

    /**
     * Habilita logging detalhado do Liquibase
     */
    private boolean verbose = false;

    /**
     * Nível de log do Liquibase
     * Valores: DEBUG, INFO, WARNING, SEVERE, OFF
     */
    private String logLevel = "INFO";

    /**
     * Habilita validação de checksums
     * false: ignora mudanças em changesets já executados
     */
    private boolean failOnChecksumValidation = true;

    /**
     * Executa em modo diff para comparar schemas
     */
    private boolean diff = false;

    /**
     * URL de referência para diff
     */
    private String referenceUrl;

    /**
     * Usuário da base de referência
     */
    private String referenceUser;

    /**
     * Senha da base de referência
     */
    private String referencePassword;

    /**
     * Driver da base de referência
     */
    private String referenceDriver;

    /**
     * Schema de referência
     */
    private String referenceDefaultSchema;

    /**
     * Tipos de objetos a incluir no diff
     * Exemplo: "tables,views,columns,indexes"
     */
    private String diffTypes;

    /**
     * Habilita rollback automático em caso de erro
     */
    private boolean rollbackOnError = false;

    /**
     * Número de changesets a incluir no rollback
     */
    private Integer rollbackCount;

    /**
     * Data/tag para rollback
     */
    private String rollbackTag;

    /**
     * Habilita histórico de rollbacks
     */
    private boolean showSummary = true;

    /**
     * Exibe apenas summary sem executar
     */
    private boolean showSummaryOutput = false;

    /**
     * Formato de data usado nos changesets
     * Default: yyyy-MM-dd HH:mm:ss
     */
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";

    /**
     * Timezone para timestamps
     */
    private String timeZone = "UTC";

    /**
     * Encoding dos arquivos de changelog
     */
    private String encoding = "UTF-8";

    /**
     * Habilita suporte a multi-tenant
     */
    private boolean multiTenant = false;

    /**
     * Nome do tenant (para multi-tenant)
     */
    private String tenantId;

    /**
     * Lista de tenants para processar
     */
    private List<String> tenants = new ArrayList<>();

    /**
     * Propriedades adicionais customizadas
     * Para extensões ou plugins do Liquibase
     */
    private Map<String, Object> customProperties = new HashMap<>();

    // ========================================
    // MÉTODOS UTILITÁRIOS
    // ========================================

    /**
     * Verifica se está configurado para usar DataSource customizado
     */
    public boolean hasCustomDataSource() {
        return url != null && !url.trim().isEmpty();
    }

    /**
     * Verifica se tem filtros de label configurados
     */
    public boolean hasLabelFilters() {
        return (labelFilter != null && !labelFilter.isEmpty()) ||
                (labels != null && !labels.trim().isEmpty());
    }

    /**
     * Verifica se tem contextos configurados
     */
    public boolean hasContexts() {
        return contexts != null && !contexts.trim().isEmpty();
    }

    /**
     * Adiciona parâmetro customizado
     */
    public void addParameter(String key, String value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
    }

    /**
     * Adiciona tenant à lista
     */
    public void addTenant(String tenant) {
        if (tenants == null) {
            tenants = new ArrayList<>();
        }
        if (!tenants.contains(tenant)) {
            tenants.add(tenant);
        }
    }

    /**
     * Valida configuração básica
     */
    public void validate() {
        if (enabled && (changeLog == null || changeLog.trim().isEmpty())) {
            throw new IllegalStateException(
                    "ChangeLog é obrigatório quando Liquibase está habilitado"
            );
        }

        if (dropFirst && !isDevelopmentMode()) {
            throw new IllegalStateException(
                    "dropFirst não pode estar habilitado em ambiente de produção!"
            );
        }

        if (lockTimeout != null && lockTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "lockTimeout não pode ser negativo"
            );
        }
    }

    /**
     * Detecta se está em modo de desenvolvimento
     * Sobrescrever em subclasses se necessário
     */
    protected boolean isDevelopmentMode() {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("dev") || profile.contains("local");
    }
}
