# SawCunhaOS-Foundation

![Java](https://img.shields.io/badge/Java-25-orange.svg)
![Maven](https://img.shields.io/badge/Maven-3.x-blue.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)
![Version](https://img.shields.io/badge/Version-1.2.0--SNAPSHOT-yellow.svg)

Core foundation framework that provides shared infrastructure, integration utilities, auditing, exception handling, caching, PII masking/privacy and cross-cutting components for all SCOS projects.

## 📋 Sumário

- [Sobre o Projeto](#sobre-o-projeto)
- [Módulos](#módulos)
  - [SCOS Foundation Privacy](#-scos-foundation-privacy)
  - [SCOS Foundation Utils](#-scos-foundation-utils)
  - [SCOS Foundation Web](#-scos-foundation-web)
  - [SCOS Foundation Audit](#-scos-foundation-audit)
  - [SCOS Foundation Jdempotent](#-scos-foundation-jdempotent)
- [Requisitos](#requisitos)
- [Instalação](#instalação)
- [Skills de configuração](#skills-de-configuração)
- [Exemplos de Uso](#exemplos-de-uso)
- [Contribuindo](#contribuindo)
- [Licença](#licença)

## 🚀 Sobre o Projeto

O **SawCunhaOS-Foundation** é um framework fundamental que fornece componentes reutilizáveis e infraestrutura compartilhada para todos os projetos do ecossistema SCOS (SawCunha Open System). Este projeto facilita o desenvolvimento de aplicações Spring Boot com funcionalidades padronizadas para auditoria, tratamento de exceções, utilitários comuns e controle de idempotência.

### Características Principais

- ✅ Tratamento centralizado de exceções (RFC 9457 / `ProblemDetail`)
- ✅ Sistema de auditoria automatizado (com cifra em repouso de PII opt-in)
- ✅ Masking de PII de alto desempenho (logs, HTTP, auditoria) — foco LGPD
- ✅ Controle de idempotência para operações críticas
- ✅ Utilitários para validação, cache, paginação e mais
- ✅ Integração com Spring Boot e Spring Cloud
- ✅ Suporte para Java 25

## 📦 Módulos

### 🛡️ SCOS Foundation Privacy

**Artifact ID:** `scos-foundation-privacy`  
**Versão:** `1.2.0-SNAPSHOT`

Motor de **masking de PII** de alto desempenho, multithread-safe, com regras em YAML. Uma única fonte de
regras alimenta três superfícies: filtro de log HTTP, converter Logback `%mask`, e cifra de campos do audit.
O núcleo é **utilizável fora do Spring** via `MaskingEngine.fromYaml(...)`.

> O antigo `utils.lgpd` foi **removido**; o masking agora vive inteiramente neste módulo. `utils` e `audit`
> dependem de `privacy`.

#### Funcionalidades

- **Regras como dado (`privacy-masking.yml`)** — resolução externo → classpath → builtins + WARN; sem banco,
  sem recompilar.
- **Estratégias** — `fixed`, `partial`, `email`, `hash` (HMAC), `encrypt` (`enc:vN:`), `redact`, com fail-safe
  e validação no startup.
- **Builtins por país/região** — `generic`, `br`, `us`, `eu`, `uk`, `in` (cpf/cnpj/credit-card validam DV).
- **Auto-configuração** — carrega ao estar no classpath (`scos.privacy.enabled`, default ligado); tudo
  `@ConditionalOnMissingBean`; SPI `DataMaskingValues` opcional (precedência builtins → YAML → SPI).
- **Masking de log Logback** — `%mask(%msg)` (texto livre) e `%maskmdc{chave}` (um valor de MDC), registrados
  programaticamente (sem editar `logback.xml`).
- **Cifra em repouso (audit)** — `ScosFieldCipher` (AES-256/GCM, token `enc:vN:`) + `ScosCryptoKeyProvider`
  (Jasypt default, plugável Vault/KMS), chave versionada por registro.
- **Performance** — `Pattern` pré-compilado, lookup O(1), Aho-Corasick, walk JSON in-place, fast-path
  zero-alocação, proteção ReDoS; JMH com baseline.

#### Como Usar

**1. Adicione a dependência** (vem transitiva via `scos-foundation-utils`):

```xml
<dependency>
    <groupId>br.com.sawcunhaos</groupId>
    <artifactId>scos-foundation-privacy</artifactId>
</dependency>
```

**2. Defina as regras em `privacy-masking.yml` (classpath):**

```yaml
scos:
  privacy:
    masking:
      builtins:
        enabled: [generic, br]
      body:
        - key: cpf
          strategy: partial
          keep-first: 0
          keep-last: 2
        - key: email
          strategy: email
      log-patterns:
        - regex: '(?<![A-Za-z0-9])(\d{14}|[A-Za-z0-9]{8}\d{6})(?![A-Za-z0-9])'
          strategy: partial
          keep-first: 3
          keep-last: 2
      audit-encrypt-fields: [cpf, email]
```

**3. Flags em `application.yml`:**

```yaml
scos:
  privacy:
    enabled: true
    strict: false
    max-payload-kb: 64
    log:
      register-converter: true
    crypto:
      secret: ${SCOS_PRIVACY_CRYPTO_SECRET}
```

**4. Masking do log da aplicação (Logback):**

```xml
<conversionRule conversionWord="mask"
                converterClass="br.com.sawcunhaos.foundation.privacy.logback.ScosMaskingConverter"/>
<conversionRule conversionWord="maskmdc"
                converterClass="br.com.sawcunhaos.foundation.privacy.logback.ScosMaskingMdcConverter"/>
<pattern>%d %-5level %logger - %mask(%msg)%n</pattern>
```

> **`%mask(%msg)` é obrigatório no pattern** — o `%msg` default não mascara. Regras de `log-patterns` não
> podem ser ancoradas `^...$`. Ver `privacy/README.md` para detalhes (AsyncAppender, Logstash/JSON, opt-out).

**5. Uso standalone (sem Spring):**

```java
MaskingEngine engine = MaskingEngine.fromYaml(Path.of("/etc/scos/privacy-masking.yml"));
engine.maskText("cpf=12345678901");          // -> cpf=*********01
engine.maskStructured("cpf", "12345678901");
```

---

### 🔧 SCOS Foundation Utils

**Artifact ID:** `scos-foundation-utils`  
**Versão:** `1.2.0-SNAPSHOT`

Módulo com utilitários comuns e componentes de infraestrutura compartilhados.

#### Funcionalidades

- **Validações Customizadas**
  - Validação de CPF/CNPJ (`@CPF`, `@CNPJ`, `@TaxIdentifier`)
  - Validação de CEP (`@ZipCode`)
  
- **Utilitários**
  - `DateUtils`: Manipulação de datas
  - `StringFieldUtils`: Manipulação de strings
  - `HashUtils`: `pseudonymize(value, secret)` (HMAC-SHA256, pseudonimização LGPD Art.13) e `createHash`
    (SHA-256, **apenas checksum** — não é mecanismo de privacidade)
  - `GsonUtils` e `JacksonXmlUtils`: Serialização/Deserialização JSON/XML
  - `IpAddressExtractor`: Extração de endereços IP
  - `PaginationUtils`: Utilitários para paginação
  - `ScosResponseUtils`: Padronização de respostas HTTP

- **Anotações**
  - `@ScosController`: Controller REST com configurações padrão
  - `@NormalizeStrings`: Normalização automática de strings
  - `@Auditable`: Marca entidades para auditoria
  - `@ScosRequestGET`, `@ScosRequestPOST`, `@ScosRequestPUT`, `@ScosRequestDELETE`: Mapeamento de requisições HTTP

- **DTOs Padrão**
  - `ScosResponseDTO<T>`: Resposta padronizada da API
  - `ScosPaginatedDTO<T>`: Resposta paginada
  - `ScosPaginationFilterDTO`: Filtros de paginação

- **Cache**
  - Configuração Redis com suporte a tipos polimórficos
  - Gerador de chaves de cache customizável
  - Integração com Spring Cache

#### Como Usar

**1. Adicione a dependência no seu `pom.xml`:**

```xml
<dependency>
    <groupId>br.com.sawcunhaos</groupId>
    <artifactId>scos-foundation-utils</artifactId>
    <version>1.2.0-SNAPSHOT</version>
</dependency>
```

**2. Exemplo de uso com validações:**

```java
public class PessoaDTO {
    @CPF
    private String cpf;
    
    @ZipCode
    private String cep;
    
    // getters e setters
}
```

**3. Exemplo de Controller:**

```java
@ScosController
public class PessoaController {
    
    @ScosRequestGET
    public ScosResponseDTO<List<PessoaDTO>> listar() {
        List<PessoaDTO> pessoas = service.listar();
        return ScosResponseUtils.ok(pessoas);
    }
    
    @ScosRequestPOST
    @NormalizeStrings(function = StringTransformRule.UPPER_CASE)
    public ScosResponseDTO<PessoaDTO> criar(@Valid @RequestBody PessoaDTO dto) {
        PessoaDTO criado = service.criar(dto);
        return ScosResponseUtils.created(criado);
    }
}
```

**4. Exemplo de paginação:**

```java
@ScosRequestGET("/paginado")
public ScosPaginatedDTO<PessoaDTO> listarPaginado(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    Page<Pessoa> pessoas = service.findAll(PageRequest.of(page, size));
    return PaginationUtils.toPaginatedDTO(pessoas, PessoaDTO.class);
}
```

---

### ⚠️ SCOS Foundation Web

**Artifact ID:** `scos-foundation-web`  
**Versão:** `1.2.0-SNAPSHOT`

Módulo (entre outras responsabilidades de camada web) para tratamento centralizado de exceções e padronização de respostas de erro.

#### Funcionalidades

- **Exceções Customizadas**
  - `ScosException`: Exceção base do sistema
  - `ScosNoContentException`: Para recursos não encontrados
  - `ScosSecurityException`: Para erros de segurança
  - `ScosNoRollbackException`: Exceção sem rollback de transação
  - `MethodNotImplementedException`: Para métodos não implementados

- **Handler Global**
  - `ExceptionsHandler`: Tratamento automático de todas as exceções, incluindo 404 de rota
    inexistente, método HTTP não suportado e `Accept` inválido (`handleExceptionInternal`)
  - Respostas de erro em conformidade com a RFC 9457 (`ProblemDetail`,
    `Content-Type: application/problem+json`)
  - Suporte a internacionalização de mensagens
  - ⚠️ **Pré-requisito para 404 de rota inexistente unificado**: a aplicação consumidora precisa
    ligar `spring.mvc.throw-exception-if-no-handler-found=true` no seu próprio `application.yml`.
    Essa propriedade é consumida pela `WebMvcAutoConfiguration` do Spring Boot, que configura o
    `DispatcherServlet` da aplicação hospedeira — uma biblioteca (`@ControllerAdvice`) não tem
    acesso a esse `DispatcherServlet` para ligá-la sozinha. Sem ela, uma rota inexistente cai na
    página de erro padrão do Boot em vez de `ScosProblemDetails` (método HTTP não suportado e
    `Accept` inválido não dependem dessa propriedade — já são cobertos independentemente).

- **Modelos de Resposta**
  - `ProblemDetail` (Spring 7, nativo): corpo de erro RFC 9457 com
    `type`, `title`, `status`, `detail`, `instance` e as extensões SCOS
    `code`, `errors`, `requestId`, `timestamp`
  - `ScosProblemDetails`: fábrica central que monta e enriquece o `ProblemDetail`
    (usada pelo `@ControllerAdvice` e pelos handlers de segurança)
  - `ScosFieldError`: erro de campo com `pointer` (JSON Pointer, RFC 6901) e `detail`

> **Nota:** o wrapper `ScosResponseDTO` permanece apenas para respostas de
> sucesso (`2xx`). Erros (`4xx`/`5xx`) usam `ProblemDetail` sem wrapper.

#### Como Usar

**1. Adicione a dependência:**

```xml
<dependency>
    <groupId>br.com.sawcunhaos</groupId>
    <artifactId>scos-foundation-web</artifactId>
    <version>1.2.0-SNAPSHOT</version>
</dependency>
```

**2. Defina seus códigos de erro:**

```java
public enum MeuExceptionCode implements ExceptionCode {
    PESSOA_NAO_ENCONTRADA("PESSOA_001"),
    CPF_JA_CADASTRADO("PESSOA_002");
    
    private final String code;
    
    MeuExceptionCode(String code) {
        this.code = code;
    }
    
    @Override
    public String getCode() {
        return code;
    }
}
```

**3. Lance exceções:**

```java
@Service
public class PessoaService {
    
    public PessoaDTO buscarPorId(Long id) {
        return repository.findById(id)
            .map(mapper::toDTO)
            .orElseThrow(() -> new ScosException(MeuExceptionCode.PESSOA_NAO_ENCONTRADA, id));
    }
    
    public void validarCpf(String cpf) {
        if (repository.existsByCpf(cpf)) {
            throw new ScosException(MeuExceptionCode.CPF_JA_CADASTRADO);
        }
    }
}
```

**4. Crie arquivo de mensagens `messages.properties`:**

```properties
PESSOA_001=Pessoa com ID {0} não encontrada
PESSOA_002=CPF já cadastrado no sistema
```

---

### 📊 SCOS Foundation Audit

**Artifact ID:** `scos-foundation-audit`  
**Versão:** `1.2.0-SNAPSHOT`

Módulo para auditoria automática de operações em entidades JPA.

#### Funcionalidades

- **Auditoria Automática**
  - Captura de operações INSERT, UPDATE e DELETE
  - Registro de usuário responsável pela operação
  - Registro de data/hora da operação
  - Armazenamento de valores antigos e novos (em formato JSON)
  - Suporte a múltiplas origens/sistemas

- **Cifra em repouso de PII (opt-in, via `privacy`)**
  - Campos listados em `audit-encrypt-fields` são cifrados antes de gravar o JSONB (`enc:vN:`)
  - Exige `scos.privacy.crypto.secret`; lista vazia = comportamento atual (texto em claro)
  - Rotação por `keyId`; histórico não é re-cifrado

- **Configuração**
  - DataSource dedicado para auditoria (`spring.datasource.audit.*`)
  - Integração com Liquibase para gerenciamento de schemas
  - Configuração de pool de conexões Hikari
  - Execução assíncrona (`ScosAuditLogAsyncExecutor`)

- **Entidades**
  - `ScosAuditLog`: Registro de auditoria
  - `ActionType`: Tipo de ação (INSERT, UPDATE, DELETE)

#### Como Usar

**1. Adicione a dependência:**

```xml
<dependency>
    <groupId>br.com.sawcunhaos</groupId>
    <artifactId>scos-foundation-audit</artifactId>
    <version>1.2.0-SNAPSHOT</version>
</dependency>
```

**2. Configure o datasource de auditoria no `application.yml`:**

```yaml
scos:
  audit:
    system: SCOS_AUDIT
    enabled: true
    liquibase:
      enable: true

spring:
  cloud:
    compatibility-verifier:
      enabled: false
  datasource:
    audit:
      url: jdbc:postgresql://localhost:5432/test
      username: test
      password: test
      hikari:
        connection-timeout: 5000
        idle-timeout: 150000
        max-lifetime: 300000
        minimum-idle: 10
        maximum-pool-size: 50
  jpa:
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        enable_lazy_load_no_trans: true
        ddl-auto: none
        dialect: org.hibernate.dialect.PostgreSQLDialect
        naming:
          implicit-strategy: org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl
          physical-strategy: org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy
        type:
          json_format_mapper: br.com.sawcunhaos.foundation.jpa.hibernate.JacksonCustomJsonFormatMapper
  aop:
    proxy-target-class: true
    auto: true

liquibase:
  scos-audit:
    contexts: test
    database-change-log-lock-table: IF_DATABASECHANGELOGLOCK
    database-change-log-table: IF_DATABASECHANGELOG
    change-log: classpath:db/changelog/db_audit.changelog-master.yaml
    default-schema: public
```

**3. Marque suas entidades para auditoria:**

```java
@Entity
@Table(name = "pessoa")
@Auditable
public class Pessoa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nome;
    private String cpf;
    
    // getters e setters
}
```

**4. As operações serão auditadas automaticamente:**

```java
@Service
public class PessoaService {
    
    public Pessoa criar(Pessoa pessoa) {
        return repository.save(pessoa); // Auditado como CREATE
    }
    
    public Pessoa atualizar(Pessoa pessoa) {
        return repository.save(pessoa); // Auditado como UPDATE
    }
    
    public void deletar(Long id) {
        repository.deleteById(id); // Auditado como DELETE
    }
}
```

**5. Consulte os logs de auditoria:**

```java
@Service
public class AuditoriaService {
    
    @Autowired
    private ScosAuditLogRepository auditRepository;
    
    public List<ScosAuditLog> buscarAuditoriasPorEntidade(String entityName) {
        return auditRepository.findByEntityName(entityName);
    }
}
```

---

### 🔄 SCOS Foundation Jdempotent

**Artifact ID:** `scos-foundation-jdempotent`  
**Versão:** `1.2.0-SNAPSHOT`

Módulo para garantir idempotência em operações críticas, prevenindo execuções duplicadas.

#### Funcionalidades

- **Controle de Idempotência**
  - Detecção automática de requisições duplicadas
  - Armazenamento de respostas para retorno imediato
  - Suporte a Redis ou memória in-memory
  - Geração de chaves de idempotência customizáveis

- **Anotações**
  - `@JdempotentResource`: Marca um método como idempotente
  - `@JdempotentId`: Define campo como identificador de idempotência
  - `@JdempotentProperty`: Inclui propriedade na geração da chave
  - `@JdempotentRequestPayload`: Usa todo o payload na chave
  - `@JdempotentIgnore`: Ignora campos específicos

- **Configurações**
  - Hashing de chave: SHA-256 (fixo, via `HexFormat`)
  - Tempo de expiração configurável
  - Repositórios: Redis ou In-Memory

#### Como Usar

**1. Adicione a dependência:**

```xml
<dependency>
    <groupId>br.com.sawcunhaos</groupId>
    <artifactId>scos-foundation-jdempotent</artifactId>
    <version>1.2.0-SNAPSHOT</version>
</dependency>
```

**2. Configure no `application.yml`:**

```yaml
server:
  port: 8880

spring:
  cloud:
    compatibility-verifier:
      enabled: false
  main:
    allow-bean-definition-overriding: true
  cache:
    enable: false
  data:
    redis:
      database: 0
      repositories:
        enabled: false
      port: 26379
      password: jdempotent
      timeout: 5000ms
      sentinel:
        master: jdempotent
        nodes: localhost
scos:
  jdempotent:
    enabled: true
    cache:
      redis:
        expirationTimeHour: 2
        dialTimeoutSecond: 3
        readTimeoutSecond: 3
        writeTimeoutSecond: 3
        maxRetryCount: 3
        persistReqRes: true
```

**3. Use as anotações em seus métodos:**

```java
@Service
public class PagamentoService {
    
    @JdempotentResource(ttl = 3600)
    public PagamentoDTO processarPagamento(@JdempotentId String transactionId, 
                                           PagamentoRequest request) {
        // Processamento do pagamento
        // Se chamado novamente com mesmo transactionId, retorna resultado cacheado
        return realizarPagamento(request);
    }
}
```

**4. Ou use com o payload completo:**

```java
@RestController
@ScosController
public class PedidoController {
    
    @ScosRequestPOST("/pedidos")
    @JdempotentResource
    public ScosResponseDTO<PedidoDTO> criar(
            @JdempotentRequestPayload @RequestBody PedidoRequest request) {
        
        PedidoDTO pedido = service.criar(request);
        return ScosResponseUtils.created(pedido);
    }
}
```

**5. Exemplo com propriedades seletivas:**

```java
public class TransferenciaRequest {
    @JdempotentProperty
    private String contaOrigem;
    
    @JdempotentProperty
    private String contaDestino;
    
    @JdempotentProperty
    private BigDecimal valor;
    
    @JdempotentIgnore // Este campo não influenciará a idempotência
    private String observacao;
}

@Service
public class TransferenciaService {
    
    @JdempotentResource(ttl = 1800)
    public TransferenciaDTO transferir(TransferenciaRequest request) {
        // A idempotência é baseada apenas em: contaOrigem, contaDestino e valor
        return executarTransferencia(request);
    }
}
```

---

## 📋 Requisitos

- **Java:** 25
- **Maven:** 3.9+
- **Spring Boot:** 4.x (gerenciado pelo scos-bom)
- **Banco de Dados:** MySQL, PostgreSQL, ou outro compatível com JPA (para audit)
- **Redis:** Opcional (para cache e jdempotent)

## 🔧 Instalação

### 1. Como Dependência (Recomendado)

Adicione o BOM do SCOS no seu `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>br.com.sawcunhaos</groupId>
            <artifactId>scos-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Depois adicione os módulos necessários:

```xml
<dependencies>
    <!-- base: traz scos-foundation-privacy transitivamente -->
    <dependency>
        <groupId>br.com.sawcunhaos</groupId>
        <artifactId>scos-foundation-utils</artifactId>
    </dependency>
    <dependency>
        <groupId>br.com.sawcunhaos</groupId>
        <artifactId>scos-foundation-privacy</artifactId>
    </dependency>
    <dependency>
        <groupId>br.com.sawcunhaos</groupId>
        <artifactId>scos-foundation-web</artifactId>
    </dependency>
    <dependency>
        <groupId>br.com.sawcunhaos</groupId>
        <artifactId>scos-foundation-audit</artifactId>
    </dependency>
    <dependency>
        <groupId>br.com.sawcunhaos</groupId>
        <artifactId>scos-foundation-jdempotent</artifactId>
    </dependency>
</dependencies>
```

> **Ativação:** o app consumidor precisa de `@SpringBootApplication` com
> `@ComponentScan(basePackages = {"br.com.sawcunhaos"})` — `utils` sobe por
> component scan; `web` (tratamento de erro), `privacy`, `audit` e `jdempotent` por
> auto-configuração.

### 2. Build do Projeto

Para construir o projeto localmente:

```bash
# Clone o repositório
git clone https://github.com/SawCunhaOS/SawCunhaOS-Foundation.git
cd SawCunhaOS-Foundation

# Compile e instale no repositório local
mvn clean install

# Ou compile e execute os testes
mvn clean verify
```

### 3. Deploy para Repositório

Para fazer deploy para o Maven Central:

```bash
# Configure as credenciais no settings.xml
# Execute o script de deploy
./scripts/deploy.sh
```

## 🧭 Skills de configuração

Para acelerar a configuração de **novos sistemas**, há uma skill de configuração por módulo em
[`etc/doc/skills`](etc/doc/skills/README.md) — cada uma cobre dependência, ativação, chaves de
`application.yml`, override de beans, exemplo de uso e pegadinhas:

| Skill | Módulo |
|---|---|
| [scos-privacy-config](etc/doc/skills/scos-privacy-config/SKILL.md) | `scos-foundation-privacy` |
| [scos-utils-config](etc/doc/skills/scos-utils-config/SKILL.md) | `scos-foundation-utils` |
| [scos-exception-config](etc/doc/skills/scos-exception-config/SKILL.md) | `scos-foundation-exception` |
| [scos-audit-config](etc/doc/skills/scos-audit-config/SKILL.md) | `scos-foundation-audit` |
| [scos-jdempotent-config](etc/doc/skills/scos-jdempotent-config/SKILL.md) | `scos-foundation-jdempotent` |

## 📚 Exemplos de Uso

### Exemplo Completo de API REST

```java
// 1. Entidade
@Entity
@Table(name = "produto")
@Auditable
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nome;
    private BigDecimal preco;
    private Integer estoque;
}

// 2. DTO
public class ProdutoDTO {
    @NotNull
    private String nome;
    
    @NotNull
    @Positive
    private BigDecimal preco;
    
    @NotNull
    @PositiveOrZero
    private Integer estoque;
}

// 3. Controller
@ScosController
@RequestMapping("/produtos")
public class ProdutoController {
    
    @Autowired
    private ProdutoService service;
    
    @ScosRequestGET
    public ScosPaginatedDTO<ProdutoDTO> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.listarPaginado(page, size);
    }
    
    @ScosRequestGET("/{id}")
    public ScosResponseDTO<ProdutoDTO> buscarPorId(@PathVariable Long id) {
        ProdutoDTO produto = service.buscarPorId(id);
        return ScosResponseUtils.ok(produto);
    }
    
    @ScosRequestPOST
    @JdempotentResource
    public ScosResponseDTO<ProdutoDTO> criar(
            @Valid @RequestBody @JdempotentRequestPayload ProdutoDTO dto) {
        ProdutoDTO criado = service.criar(dto);
        return ScosResponseUtils.created(criado);
    }
    
    @ScosRequestPUT("/{id}")
    public ScosResponseDTO<ProdutoDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoDTO dto) {
        ProdutoDTO atualizado = service.atualizar(id, dto);
        return ScosResponseUtils.ok(atualizado);
    }
    
    @ScosRequestDELETE("/{id}")
    public ScosResponseDTO<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ScosResponseUtils.noContent();
    }
}

// 4. Service
@Service
@Transactional
public class ProdutoService {
    
    @Autowired
    private ProdutoRepository repository;
    
    public ScosPaginatedDTO<ProdutoDTO> listarPaginado(int page, int size) {
        Page<Produto> produtos = repository.findAll(PageRequest.of(page, size));
        return PaginationUtils.toPaginatedDTO(produtos, ProdutoDTO.class);
    }
    
    public ProdutoDTO buscarPorId(Long id) {
        return repository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new ScosNoContentException());
    }
    
    public ProdutoDTO criar(ProdutoDTO dto) {
        Produto produto = toEntity(dto);
        produto = repository.save(produto); // Auditado automaticamente
        return toDTO(produto);
    }
    
    public ProdutoDTO atualizar(Long id, ProdutoDTO dto) {
        Produto produto = repository.findById(id)
            .orElseThrow(() -> new ScosNoContentException());
        
        produto.setNome(dto.getNome());
        produto.setPreco(dto.getPreco());
        produto.setEstoque(dto.getEstoque());
        
        produto = repository.save(produto); // Auditado automaticamente
        return toDTO(produto);
    }
    
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new ScosNoContentException();
        }
        repository.deleteById(id); // Auditado automaticamente
    }
}
```

## 🤝 Contribuindo

Contribuições são bem-vindas! Para contribuir:

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/MinhaFeature`)
3. Commit suas mudanças (`git commit -m 'Adiciona MinhaFeature'`)
4. Push para a branch (`git push origin feature/MinhaFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está licenciado sob a Apache License 2.0 - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 📞 Suporte

- **Issues:** [GitHub Issues](https://github.com/SawCunhaOS/SawCunhaOS-Foundation/issues)
- **Documentação:** [Wiki do Projeto](https://github.com/SawCunhaOS/SawCunhaOS-Foundation/wiki)

## 🏢 Organização

**SawCunha Open System**  
GitHub: [https://github.com/SawCunhaOS](https://github.com/SawCunhaOS)

---

Desenvolvido com ❤️ pela comunidade SCOS
