## ADDED Requirements

### Requirement: PR bloqueada se body vazio ou ausente
O check SHALL falhar imediatamente se o body da PR for nulo, vazio ou conter apenas espaços em branco.

#### Scenario: PR sem body é bloqueada
- **WHEN** PR é aberta via `gh pr create` sem `--body` (body nulo)
- **THEN** o check falha com mensagem `"PR sem descrição"`

### Requirement: PR bloqueada se heading obrigatória ausente ou sem conteúdo
O check SHALL falhar se qualquer das 6 headings obrigatórias estiver ausente do body, ou se a heading existir mas não tiver nenhuma linha não-vazia e não-comentário abaixo dela. Seções com checkboxes exigem ao menos um `- [x]` ou `- [X]`.

#### Scenario: Seção Descrição preenchida com texto passa
- **WHEN** body contém `## Descrição\nAdiciona suporte a masking de CPF.`
- **THEN** a seção passa na validação

#### Scenario: Seção com comentário não substituído é bloqueada
- **WHEN** body contém `## Descrição\n<!-- Descreva aqui -->` sem texto adicional
- **THEN** o check falha com mensagem indicando que `## Descrição` está vazia

#### Scenario: Seção de checkbox sem marcação é bloqueada
- **WHEN** body contém `## Tipo de Mudança\n- [ ] feat\n- [ ] fix` sem nenhum `[x]`
- **THEN** o check falha com mensagem indicando que `## Tipo de Mudança` não tem checkbox marcado

#### Scenario: Breaking Changes com "Nenhum" passa
- **WHEN** body contém `## Breaking Changes\nNenhum`
- **THEN** a seção passa na validação

### Requirement: PRs de bots automatizados ignoradas
O check SHALL retornar success sem validação quando o `github.actor` for `dependabot[bot]`, `github-actions[bot]` ou `renovate[bot]`.

#### Scenario: PR de Dependabot passa sem validação
- **WHEN** PR é aberta pelo actor `dependabot[bot]`
- **THEN** o check passa com sucesso sem verificar o body

### Requirement: Check roda novamente ao editar PR
O check SHALL ser disparado quando a PR for editada (`edited`) ou receber novos commits (`synchronize`), desbloqueando a PR quando o body for corrigido.

#### Scenario: PR corrigida após falha é desbloqueada
- **WHEN** PR falhou na validação e o desenvolvedor edita o body para preencher as seções
- **THEN** o check roda novamente e passa, removendo o bloqueio
