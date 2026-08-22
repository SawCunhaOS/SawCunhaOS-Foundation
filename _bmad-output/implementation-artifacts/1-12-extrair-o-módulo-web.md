# Story 1.12: Extrair o módulo `web`

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como time consumidor de endpoints REST,
Eu quero um módulo `web` com `@Cacheable` embutido,
Para ligar controllers sem montar a integração manualmente.

## Acceptance Criteria

1. **Given** `ScosController`, `IpAddressExtractor`, `ScosResponseUtils`, `PaginationUtils`, `JacksonXmlUtils`, `MultiReadHttpServletRequest`, `ScosJacksonConfig`, `LoggingInitialFilter`/`LoggingFinalFilter` do inventário, **When** o módulo `web` é extraído dependendo de `core`, `cache` e `privacy`, **Then** `web` compila isoladamente, `@Cacheable` funciona nas anotações `ScosRequestGET/POST/PUT/DELETE`, e os testes migrados permanecem verdes.

## Tasks / Subtasks

- [ ] Task 1: Criar o módulo e mover as classes listadas no AC
  - [ ] Criar módulo Maven `scos-foundation-web`, pacote raiz `br.com.sawcunhaos.foundation.web`, dependendo de `core` (Story 1.7), `cache` (Story 1.10), `privacy` (já existe), `spring-boot-starter-web`, `spring-data-commons`
  - [ ] Mover: `annotation/ScosController.java`, `utils/IpAddressExtractor.java`, `ScosResponseUtils.java`, `PaginationUtils.java`, `JacksonXmlUtils.java`, `configuration/rest/filter/MultiReadHttpServletRequest.java`, `configuration/rest/ScosJacksonConfig.java`, `configuration/rest/filter/LoggingInitialFilter.java`, `LoggingFinalFilter.java`
  - [ ] Mover também as anotações de rota (`annotation/request/ScosRequestMapping.java`, `ScosRequestGET.java`, `ScosRequestPOST.java`, `ScosRequestPUT.java`, `ScosRequestDELETE.java`, `ScosRequestPATCH.java`) — necessárias para o AC "`@Cacheable` funciona nas anotações `ScosRequestGET/POST/PUT/DELETE`" fazer sentido; `ScosRequestPATCH` não está citada no texto do AC mas pertence à mesma família e precisa mover junto para o módulo compilar (mesma divergência de contagem já registrada na Story 1.5)
  - [ ] Mover os DTOs de suporte usados por `ScosResponseUtils`/`PaginationUtils`: `dto/request/ScosPaginationFilterDTO.java`, `dto/response/ScosPaginatedDTO.java`, `ScosResponseDTO.java` — não citados no texto do AC, mas exigidos para essas classes compilarem (comportamento end-to-end, não escopo novo)
  - [ ] Mover `configuration/rest/filter/properties/ScosFilterProperties.java` (propriedades dos filtros de logging movidos)
  - [ ] Mover testes correspondentes: `configuration/rest/filter/LoggingFilterMaskingE2ETest.java`, `LoggingFinalFilterTest.java`, `LoggingInitialFilterTest.java`, `utils/PaginationUtilsTest.java`
  - [ ] Commit isolado de "mover" separado de qualquer ajuste de comportamento (NFR2/NFR4, mesmo padrão das stories anteriores de extração — o AC desta story não repete a exigência de 2 commits textualmente, mas a convenção do épico (NFR2) se aplica igual)
- [ ] Task 2: Confirmar `@Cacheable` funcionando (AC: #1)
  - [ ] `web` depende diretamente de `cache` (decisão já tomada na arquitetura — aresta `web → cache` no grafo de dependência, resolvendo a Decisão D1 do plano de origem pela opção "web depende de cache")
  - [ ] Confirmar que `nameCache()` com `@AliasFor` continua resolvendo `@Cacheable` corretamente após a migração de pacote
- [ ] Task 3: Confirmar a aresta `web → privacy` (AC: #1)
  - [ ] `LoggingInitialFilter`/`LoggingFinalFilter` importam `jakarta.servlet.*` **e** `SanitizationBodyComponent`/`SanitizationHeadersComponent` de `privacy` — essa combinação é a razão real dessas duas classes pertencerem a `web` e não a `spring` (aresta documentada como real, não hipotética, na arquitetura)
- [ ] Task 4: Confirmar testes verdes (AC: #1)

## Dev Notes

- Depende das Stories 1.7 (`core`) e 1.10 (`cache`) já concluídas; `privacy` já existe no reactor sem mudança.
- **Decisão D1 do plano de origem já resolvida pela arquitetura**: `web` depende de `cache` diretamente (opção "a" das três apresentadas) — não reabrir essa decisão nesta story.
- `web` é o módulo mais acoplado entre os módulos folha — depende de `core` + `cache` + `privacy` simultaneamente. É o último da ordem de extração da Fase 4 (`spring` → `validation` → `cache` → `jpa` → `web`).
- Feign (`JacksonEncoderCustom`/`JacksonDecoderCustom`) **não** entra nesta story — vai para o módulo `feign` próprio na Story 1.13 (Decisão D3 do plano de origem: módulo dedicado, não dependência dentro de `web`).

### Project Structure Notes

- Módulo Maven novo: `web/` — depende de `core`, `cache`, `privacy`, `spring-boot-starter-web`, `spring-data-commons`.
- `utils/` perde as classes de `annotation/`, `annotation/request/`, `configuration/rest/*`, `dto/*`, e os utilitários `IpAddressExtractor`/`ScosResponseUtils`/`PaginationUtils`/`JacksonXmlUtils`.
- `pom.xml` raiz ganha `<module>web</module>`.

### References

- [Source: etc/doc/plano/plano-decomposicao-utils.md#scos-foundation-web]
- [Source: etc/doc/plano/plano-decomposicao-utils.md#7-decisões-que-preciso-que-você-tome] (Decisão D1)
- [Source: _bmad-output/planning-artifacts/architecture/architecture-SawCunhaOS-Foundation-2026-08-19/ARCHITECTURE-SPINE.md#ad-1--fronteira-e-direção-de-dependência-entre-módulos-adopted] (aresta `web → privacy`)
- [Source: _bmad-output/implementation-artifacts/1-5-criar-módulos-api-e-mover-as-anotações-de-contrato.md] (divergência de contagem `ScosRequestPATCH`)
- [Source: _bmad-output/planning-artifacts/epics.md#story-112-extrair-o-módulo-web]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
