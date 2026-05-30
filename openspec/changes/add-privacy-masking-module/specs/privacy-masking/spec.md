## ADDED Requirements

### Requirement: Motor de masking unificado e stateless

O sistema SHALL prover um `MaskingEngine` imutável e thread-safe que expõe
`maskStructured(key, value)` (key-based) e `maskText(message)` (text-based), montado
uma única vez no startup e compartilhado como singleton entre todas as threads sem
sincronização.

#### Scenario: Mascaramento key-based de campo conhecido

- **WHEN** `maskStructured("cpf", "12345678901")` é chamado e existe regra para `cpf`
- **THEN** o valor retornado é mascarado conforme a `strategy` da regra

#### Scenario: Mascaramento text-based de mensagem com PII

- **WHEN** `maskText("Pessoa criada cpf=12345678901")` é chamado com pattern de CPF ativo
- **THEN** o trecho de PII é substituído (ex.: `cpf=***`) e o restante é preservado

#### Scenario: Uso concorrente sem corrupção de estado

- **WHEN** N threads compartilham o mesmo `MaskingEngine` e mascaram payloads distintos em paralelo
- **THEN** cada resultado é idêntico ao obtido em execução sequencial

### Requirement: Estratégias de máscara como enum fechado

O sistema SHALL suportar exatamente as estratégias `fixed`, `partial`, `email`,
`hash`, `encrypt` e `redact`. Uma `strategy` desconhecida MUST falhar no carregamento.
`fixed` SHALL ser o default quando apenas `value` é informado.

#### Scenario: Estratégia partial preserva pontas

- **WHEN** a regra usa `strategy: partial`, `keep-first: 3`, `keep-last: 2` sobre `12345678901`
- **THEN** o resultado mantém as pontas e mascara o miolo (ex.: `123******01`)

#### Scenario: Fail-safe de partial em valor curto

- **WHEN** `keep-first + keep-last >= len(value)`
- **THEN** o valor é mascarado por completo (nunca revela o valor curto)

#### Scenario: Estratégia desconhecida rejeitada no load

- **WHEN** o YML declara `strategy: foo`
- **THEN** o carregamento falha no startup com erro descritivo

#### Scenario: hash é estável e determinístico

- **WHEN** `strategy: hash` é aplicada duas vezes ao mesmo valor com a mesma chave
- **THEN** as duas saídas são iguais; valores diferentes produzem saídas diferentes

#### Scenario: redact remove o campo

- **WHEN** `strategy: redact` é aplicada a um campo
- **THEN** o campo é removido da saída

### Requirement: Regras carregadas de arquivo YML

O sistema SHALL ler as regras (headers, body, log-patterns, audit-encrypt-fields) de
um arquivo YML dedicado, sem usar banco de dados. A resolução da fonte MUST seguir a
precedência: caminho externo configurável → fallback no classpath → ausência das duas
resulta em builtins (se ligados) e WARN no startup.

#### Scenario: Caminho externo tem prioridade sobre classpath

- **WHEN** existe YML no `config-path` externo e também no classpath
- **THEN** o do `config-path` externo é usado

#### Scenario: Fallback para classpath

- **WHEN** o `config-path` externo não está definido e existe `privacy-masking.yml` no classpath
- **THEN** o do classpath é usado

#### Scenario: Ausência total degrada para builtins com aviso

- **WHEN** não há YML externo nem no classpath
- **THEN** o engine sobe com builtins (se habilitados) e emite WARN no startup

#### Scenario: YML inválido falha no carregamento

- **WHEN** o YML contém chave desconhecida ou `keep-*` negativo
- **THEN** o carregamento falha no startup, não em produção

### Requirement: Núcleo portável sem dependência de Spring

O sistema SHALL permitir construir o `MaskingEngine` fora da camada SCOS via factory
`MaskingEngine.fromYaml(Path | InputStream)`, sem exigir contexto Spring nem banco.

#### Scenario: Construção standalone sem Spring

- **WHEN** uma aplicação Java pura chama `MaskingEngine.fromYaml(path)` sem contexto Spring
- **THEN** o engine é construído e `maskText`/`maskStructured` funcionam normalmente

### Requirement: Builtins de masking por país/região

O sistema SHALL prover packs de patterns embutidos (`generic`, `br`, `us`, `eu`,
`uk`, `in`) como dados embarcados no classpath, habilitáveis por lista e desligáveis
por item. Patterns com dígito verificador (`credit-card`, `cpf`, `cnpj`) MUST validar
Luhn/DV após o pré-screen.

#### Scenario: Habilitar packs por lista

- **WHEN** `builtins.enabled: [generic, br]`
- **THEN** os patterns dos packs `generic` e `br` ficam ativos

#### Scenario: Desligar item específico

- **WHEN** `builtins.disabled: [br.titulo-eleitor]` com pack `br` habilitado
- **THEN** todos os patterns de `br` ficam ativos exceto `titulo-eleitor`

#### Scenario: Validação de dígito reduz falso-positivo

- **WHEN** uma sequência numérica casa o regex de `cpf` mas falha no DV
- **THEN** ela não é mascarada como CPF

### Requirement: Precedência de fontes de regra

O sistema SHALL combinar regras na ordem builtins → YML → SPI, achatando-as em um
snapshot imutável único no build, onde fontes posteriores sobrepõem por chave.

#### Scenario: App sobrepõe builtin por chave

- **WHEN** um builtin define regra para `email` e o YML do app define outra para `email`
- **THEN** a regra do YML do app prevalece

#### Scenario: SPI soma ao YML

- **WHEN** o app fornece um `DataMaskingValues` com chave inexistente no YML
- **THEN** essa chave passa a ser mascarada, somando-se às regras do YML

### Requirement: Desempenho e proteção do hot path

O sistema SHALL compilar `Pattern` uma vez no build, usar lookup key-based O(1),
percorrer JSON in-place, aplicar fast-path de zero alocação quando não há gatilho de
PII, e rejeitar no build patterns com risco de backtracking catastrófico (ReDoS). Um
limite de tamanho de payload configurável MUST truncar entradas acima do limite antes
do masking.

#### Scenario: Fast-path sem PII não aloca

- **WHEN** `maskText` recebe uma linha sem nenhum gatilho de PII
- **THEN** retorna a mesma referência de `String` sem realizar substituição

#### Scenario: Pattern perigoso rejeitado no build

- **WHEN** uma regra declara um regex com backtracking catastrófico
- **THEN** o build do engine falha no startup com erro

#### Scenario: Cap de tamanho limita pior caso

- **WHEN** uma mensagem excede `max-payload-kb`
- **THEN** ela é truncada com marcador antes de ser mascarada
