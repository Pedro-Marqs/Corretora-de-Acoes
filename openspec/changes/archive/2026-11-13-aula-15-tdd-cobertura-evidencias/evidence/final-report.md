# Evidências finais — Aula 15

Data da validação: 2026-11-13
Branch: `feature/aulas-13-15`  
HEAD: `5adb3a1` (`Documenta testes e evidências da Aula 15`)
Sincronização registrada antes desta validação: `0/0` com `origin/feature/aulas-13-15`.

Este relatório não contém senhas, JWTs, cookies, secrets, hashes, CPFs ou dados pessoais.

## Encerramento independente da task 4.3 — 2026-11-13

A trilha da Aula 15 foi submetida a duas instâncias nativas independentes do Codex, em contextos separados do implementador e sem alteração de arquivos durante a revisão:

- **REVIEWER — APROVADO:** `explorer`, instância `01a1d4ba-3eae-7753-a99e-79e2d8877c83`. Inspecionou código, testes, migration 015, changelog, OpenSpec, relatório e arquivos não rastreados; não executou testes.
- **TESTER — APROVADO:** `worker`, instância `01a1d4c0-383f-7112-b4ee-142c10d9fc5e`, identificação `TST-CODEX-2026-11-13-0853-BRT`. Executou testes focados, PostgreSQL/Testcontainers, `mvn clean verify`, JaCoCo, OpenSpec strict e `git diff --check`.

O tester confirmou 543 testes contabilizados, 536 aprovados, 0 falhas, 0 erros e 7 ignorados; JaCoCo reportou 3503/3676 linhas (95,29%) e 1141/1422 branches (80,24%). Os sete testes PostgreSQL locais opt-in continuam classificados como ignorados, nunca como aprovados. A migration 015 passou por aplicação, rollback, reaplicação e falha segura diante de auditoria `RESET_ADMIN_PASSWORD`; o conjunto combinado relevante passou com 27/27 e `GlobalExceptionHandlerTests` passou com 9/9 sem reproduzir a intermitência histórica.

Os dois riscos residuais de baixa severidade permanecem abertos e não foram reclassificados como resolvidos: (1) o teste de rollback preserva as ações observadas, mas não compara todos os campos dos registros de auditoria; (2) `GlobalExceptionHandlerTests` usa `ListAppender` e conserva risco teórico de intermitência, embora não observado nas execuções atuais. A intermitência histórica não é declarada definitivamente eliminada.

Esta conclusão refere-se exclusivamente ao escopo da Aula 15. A correção posterior de recolhimento/reabertura da sidebar em `frontend/src/styles/navigation.css` ocorreu depois dos pareceres, não altera a implementação da Aula 15 e deve ser versionada em commit separado.

## Estado do Git

- O worktree já continha alterações preservadas antes desta retomada; elas não foram descartadas, restauradas ou staged.
- Nenhum arquivo foi staged, commitado, enviado, restaurado ou descartado.
- Os commits anteriores permanecem preservados.
- As alterações desta trilha incluem a evidência documental, a adequação de proposal/design/spec/tasks ao enunciado da seção 21, a correção determinística de fixtures de teste PostgreSQL, a correção do rollback da migration 015 e a implementação do reset administrativo por CLI descrita abaixo.
- A implementação funcional desta correção inclui `AdminPasswordResetCommand`, `AdminPasswordResetService`, seus testes unitários, o teste PostgreSQL isolado `AdminPasswordResetPostgreSqlTests` e a migration `015-admin-password-reset-audit-action.xml`, incluída no changelog mestre. O comando permanece exclusivamente operacional por CLI; não foi criado endpoint HTTP.

## Tarefas e evidências

### 2.1 — autenticação e sessão

Permanece concluída com a evidência histórica em `evidence/aula-13-refresh-stage.md` e `evidence/baseline.md`, incluindo rejeição de token inválido/expirado, rotação e revogação de refresh token.

### 2.2 — audience `USER`/`ADMIN`

Os comportamentos estão cobertos no estado atual por `AdminAuthenticationTests` e `AdminFinancialIsolationTests`, incluindo rejeição cruzada, respostas `401`/`403`, principal incompatível e ausência de mutação.

Não foi produzido um novo ciclo RED → GREEN → REFACTOR nesta retomada. A implementação já estava correta no HEAD; introduzir uma falha artificial apenas para fabricar um RED não seria evidência TDD autêntica. Com a adequação do OpenSpec ao enunciado original, esta task é concluída como regressão obrigatória, sem alegar um ciclo TDD inexistente.

### 2.3 — fronteiras financeiras e monetárias

O estado atual possui cobertura GREEN em `PurchaseServiceIntegrationTests`, `FinancialAmountTests`, `PositionFinancialCalculatorTests`, `PurchaseRollbackTests`, `SaleRollbackTests` e `TransferRollbackTests`, cobrindo insuficiência de saldo, propriedade, BRL/USD, conversão, arredondamento, rollback e conservação de posições.

Não foi produzido um novo ciclo RED → GREEN → REFACTOR sem alterar artificialmente uma regra já correta. Com a adequação do OpenSpec ao enunciado original, esta task é concluída como regressão financeira obrigatória, sem alegar um ciclo TDD inexistente.

## Investigação histórica das evidências TDD

A investigação dos changes arquivados das Aulas 13 e 14 encontrou ciclos TDD autênticos, mas eles não podem ser atribuídos retroativamente às tarefas 2.2 e 2.3 da Aula 15:

- `evidence/baseline.md` registra o ciclo da infraestrutura JWT da Aula 13: o teste `JwtAuthenticationTests` foi criado antes das classes de implementação e falhou na compilação (RED), a implementação mínima fez os cinco testes passarem (GREEN) e uma etapa posterior explicitou conversor, validações de secret/TTL, defaults e limpeza do `SecurityContext`, mantendo os testes verdes (REFACTOR).
- `evidence/aula-13-refresh-stage.md` registra o ciclo de refresh token: o teste de reuso falhou com `204` em vez de `401` (RED), a correção de transação/exceção fez os 45 testes passarem (GREEN) e a separação de exceções, o escopo de `noRollbackFor` e os cenários de isolamento foram refinados depois (REFACTOR).
- O commit `9334d6a` adicionou a implementação JWT administrativa e `AdminAuthenticationTests` no mesmo commit. O commit `a22d9fb` adicionou RBAC financeiro e `AdminFinancialIsolationTests` no mesmo commit. O commit `96c47eb` adicionou `FinancialAmount` e seus testes no mesmo commit. Esses commits comprovam implementação e cobertura, mas não comprovam um RED executado antes da implementação.

Não foi localizado nos artefatos arquivados um comando/resultado contemporâneo que demonstre RED → GREEN → REFACTOR especificamente para audience ADMIN/INVESTOR, RBAC administrativo, arredondamento ou moedas da Aula 14. Portanto, não há base para declarar esses ciclos como evidência da Aula 15.

## Correspondência com o enunciado original

Foi consultado o material oficial [Aula 15 — Testes de software, TDD e cobertura](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-15-TESTES-E-COBERTURA.md), especialmente a seção 21. O enunciado exige relatório JaCoCo inicial, classe com branches parcialmente cobertos, lista de comportamentos e valores de fronteira, pelo menos três ciclos RED → GREEN → REFACTOR, evidência de falha anterior à implementação, evolução de branches, justificativa de mocks e mutação detectada.

O OpenSpec foi corrigido para refletir essa distinção: as tasks 2.2 e 2.3 continuam exigindo testes de regressão de audience/RBAC e fronteiras financeiras, mas não determinam artificialmente que esses testes sejam os ciclos TDD. A task 2.4 agora descreve o requisito operacional aprovado e sua trilha RED/GREEN/REFACTOR. Assim, nenhum requisito original foi removido e nenhuma evidência regressiva foi promovida indevidamente a TDD.

## Terceiro ciclo TDD — redefinição administrativa por CLI

O requisito foi considerado novo após a inspeção de `AdminBootstrapCommand`, `AdminBootstrapService`, `AdminUser`, `AdminUserService` e `AdminRefreshTokenService`: existia apenas bootstrap idempotente, não havia redefinição de senha para administrador existente.

### RED — falha real antes da implementação

Foram adicionados primeiro `AdminPasswordResetServiceTests` e `AdminPasswordResetCommandTests`, sem classes de produção correspondentes. O comando executado foi:

```text
cd backend
.\mvnw.cmd '-Dtest=AdminPasswordResetServiceTests,AdminPasswordResetCommandTests' test
```

Resultado verificável: `BUILD FAILURE` na fase `testCompile`, com 20 erros de compilação. A saída apontou a ausência de `AdminPasswordResetService`, `AdminPasswordResetCommand`, `findForUpdateByEmailIgnoreCase` e `RESET_ADMIN_PASSWORD`. Essa falha é RED de ausência do comportamento, não uma mutação artificial.

### GREEN — implementação mínima

Foram implementados `AdminPasswordResetService`, `AdminPasswordResetCommand`, a busca pessimista por e-mail, `AdminUser.changePasswordHash` e a ação de auditoria `RESET_ADMIN_PASSWORD`. O comando só reage a `--admin-password-reset` com `--admin-password-reset-email`, lê a senha com `Console.readPassword` e zera o buffer `char[]` após o uso. O serviço exige administrador existente, mantém o status da conta, persiste apenas hash BCrypt, revoga refresh tokens do alvo e audita como `SYSTEM`, sem endpoint HTTP ou senha em `.env`.

O mesmo comando focado passou após a implementação: `5` testes, `0` falhas, `0` erros, `0` ignorados, `BUILD SUCCESS`.

### REFACTOR — remoção de dependência sem comportamento

Contagem da fase REFACTOR antes do cenário adicional: `5` testes, `0` falhas, `0` erros, `0` ignorados, `BUILD SUCCESS`.

Após o GREEN, a dependência `Clock` não utilizada foi removida do serviço; o horário da auditoria continua encapsulado em `AdminAuditService`. Os mesmos testes foram executados novamente e passaram: `6` testes, `0` falhas, `0` erros, `0` ignorados, `BUILD SUCCESS`.

O ciclo não altera contas reais, investidores, contas financeiras ou outros administradores. Refresh tokens administrativos são revogados; access tokens JWT já emitidos não são alegados como revogados imediatamente e mantêm validade residual até sua expiração natural.

Como verificação complementar, foi adicionado depois do REFACTOR o cenário que redefine a senha de administrador inativo sem reativá-lo; a execução focada final anterior passou com `6` testes, `0` falhas, `0` erros e `0` ignorados. Após os findings, a suíte focada atual passou com `8` testes de comando e `4` de serviço, e a suíte PostgreSQL isolada passou com `5` testes.

Na correção dos findings, o fluxo foi endurecido sem reescrever a cronologia TDD: `Console.readPassword` alimenta `char[]`, o comando e o serviço limpam os buffers em `finally`, e o encoder recebe um `CharBuffer`. A limitação residual é documentada: o contrato de `PasswordEncoder` retorna `String` e a JVM/encoder podem manter cópias imutáveis que não podem ser zeroizadas pelo chamador. A unidade atual passou com `8` testes do comando e `4` do serviço; a integração PostgreSQL passou com `5` testes.

O teste PostgreSQL isolado comprovou sucesso, rollback quando a revogação falha, rollback quando a auditoria obrigatória falha, administrador inexistente, administrador inativo e preservação de outro administrador, investidor e saldo. A migration 015 corrige a divergência entre o enum `RESET_ADMIN_PASSWORD` e a constraint persistida `ck_admin_audit_action`; ela não altera regras financeiras.

### Finding do reviewer — rollback da migration 015

O rollback original da migration `015-admin-password-reset-audit-action.xml` reconstruía `ck_admin_audit_action` sem `ACTIVATE_ADMIN` e `DEACTIVATE_ADMIN`, embora essas duas ações fossem válidas após a migration 014. A correção mínima restaurou exatamente as nove ações da 014, sem modificar a migration já executada em ambientes existentes.

`PostgreSqlAdminAuditMigrationRollbackTests` executou em PostgreSQL 16 descartável via Testcontainers. O cenário aplicou as migrations até a 014, persistiu auditorias `ACTIVATE_ADMIN` e `DEACTIVATE_ADMIN`, aplicou e reverteu somente a 015, confirmou a preservação dos registros e rejeitou `RESET_ADMIN_PASSWORD` após o rollback. Em seguida reaplicou a 015, confirmou as dez ações permitidas e a integridade dos registros.

Quando já existe uma auditoria `RESET_ADMIN_PASSWORD`, o rollback não é seguro porque a constraint da 014 não pode representar esse dado. O teste exige falha explícita do rollback; a transação DDL é desfeita, a 015 continua registrada como aplicada, nenhum registro é removido ou reclassificado e a constraint de dez ações permanece válida. Uma reversão posterior exigirá política explícita de retenção/transformação de auditoria; ela não foi inventada nesta correção.

## Investigação anterior de branches parciais

O relatório JaCoCo final foi consultado para escolher um comportamento relevante com branches parciais. `PatrimonyCalculator` apareceu com `11/18` branches cobertos (`61,11%`) e foi inspecionado por ser financeiro. A classe já implementa as fronteiras de conta inválida, cotação ausente, moeda incompatível, taxa USD/BRL ausente ou não positiva e valor de posição não positivo. Os testes atuais cobrem cálculo BRL/USD, caixa, cotação ausente e câmbio ausente.

Os branches restantes representam caminhos de erro já implementados. Eles foram úteis para escolher a investigação, mas não foram usados para fabricar um RED; o terceiro ciclo legítimo foi realizado no requisito operacional de redefinição administrativa documentado acima.

## Matriz consolidada de evidências

| Critério | Classificação | Evidência auditável | Resultado | Limitação ou decisão |
|---|---|---|---|---|
| Token de investidor ausente, inválido, adulterado ou expirado | TDD histórico + regressão | `evidence/baseline.md`, `JwtAuthenticationTests`, `JwtSecurityConfigurationTests` | RED, GREEN e REFACTOR históricos comprovados; regressão atual aprovada | Ciclo pertence à Aula 13, não às tasks 2.2/2.3 |
| Audience e authorities ADMIN/INVESTOR | Regressão/caracterização | `AdminAuthenticationTests`, `AdminFinancialIsolationTests` | Tokens incompatíveis rejeitados com `401`/`403` | Não há RED contemporâneo verificável antes da implementação administrativa |
| Acesso cruzado entre áreas | Regressão/caracterização + mutação relacionada | `AdminFinancialIsolationTests`, `JwtBearerFinancialAuthorizationTests`; mutação de `exactAudience` documentada na task 3.1 | Acesso cruzado bloqueado e estado financeiro preservado | A mutação comprova detecção, mas não substitui ciclo TDD |
| Isolamento e propriedade de conta | Regressão de integração | `JwtBearerFinancialAuthorizationTests`, `PurchaseServiceIntegrationTests` | Conta incorreta não opera nem obtém dados financeiros indevidos | PostgreSQL local opt-in não foi executado |
| Saldo insuficiente e rejeição sem mutação | Regressão de integração | `PurchaseServiceIntegrationTests` | Sem alteração indevida de saldo, posições, movimentos ou pontos | Não há RED histórico da Aula 14 documentado |
| BRL/USD e conversão cambial | Caracterização/regressão financeira | `PurchaseServiceIntegrationTests`, `FinancialAmountTests`, `PositionFinancialCalculatorTests` | Conversão e separação monetária preservadas | Dependências externas são isoladas por mocks justificados |
| Arredondamento e valores inválidos | Teste de domínio/regressão | `FinancialAmountTests`, `PositionFinancialCalculatorTests` | `HALF_UP`, taxas inválidas e estados inválidos tratados conforme contrato | Não há ciclo RED contemporâneo comprovado |
| Compra, venda, transferência e rollback | Regressão transacional | `PurchaseRollbackTests`, `SaleRollbackTests`, `TransferRollbackTests` | Falha injetada não deixa mutação parcial | `@MockitoSpyBean` injeta falha de persistência, sem substituir a regra financeira |
| Mutações controladas | Teste de mutação | `AdminAuthenticationTests#invalidAdministrativeClaimsAreRejected` com remoção temporária de `exactAudience` | O teste falhou com `200` em vez de `401`; implementação restaurada e teste voltou a passar | Evidência de mutação, não de RED de implementação |
| Redefinição administrativa por CLI | TDD contemporâneo desta retomada + integração | `AdminPasswordResetCommandTests`, `AdminPasswordResetServiceTests`, `AdminPasswordResetPostgreSqlTests` | Senha lida sem argumento, hash BCrypt persistido, refresh tokens revogados, auditoria SYSTEM e rollback comprovados | Cópias internas criadas pelo `PasswordEncoder` não são controláveis; access tokens emitidos mantêm validade residual |
| Rollback da migration 015 | Integração PostgreSQL/Testcontainers | `PostgreSqlAdminAuditMigrationRollbackTests` | Rollback restaura as nove ações da 014, preserva registros, rejeita RESET após rollback, reaplicação restaura as dez ações e rollback inseguro falha explicitamente | Com auditorias RESET já persistidas, é necessária política de retenção/transformação antes de uma reversão destrutiva; nenhuma foi aplicada |
| Testes PostgreSQL locais | Limitação ambiental | Sete testes `LocalPostgreSql*` | Ignorados por ausência de `RUN_LOCAL_POSTGRES_TESTS` | Não tratados como aprovados; Testcontainers executou |

Legenda: TDD exige uma falha legítima antes da implementação, passagem após a implementação e passagem após refatoração. Teste de regressão confirma comportamento já implementado. Teste de mutação verifica se a suíte detecta uma alteração incorreta temporária. Essas categorias não são intercambiáveis.

## Baseline e resultado final

O baseline registrado em `evidence/baseline.md` foi mantido como histórico, sem sobrescrever seus resultados. A comparação abaixo é indicativa: a suíte cresceu durante as Aulas 13 e 14, portanto não representa uma comparação causal isolada da Aula 15.

| Métrica | Baseline registrado | Estado final desta validação | Gate |
|---|---:|---:|---:|
| Backend — testes | 352 executados, 0 falhas, 0 erros, 7 ignorados | 543 executados, 0 falhas, 0 erros, 7 ignorados | Aprovado |
| JaCoCo — linhas | 94,80% | 95,29% | 80% |
| JaCoCo — branches | 80,04% | 80,24% | 80% |
| Frontend — testes | 274 aprovados | 396 aprovados | Aprovado |
| Vitest — statements | 79,86% | 88,44% | 80% |
| Vitest — branches | 72,24% | 81,49% | 80% |
| Vitest — functions | 84,39% | 92,08% | 80% |
| Vitest — lines | 84,74% | 93,29% | 80% |

Os resultados finais não reduzem thresholds, não excluem classes artificialmente e não contam testes ignorados como aprovados.

## Avaliação literal da task 4.2

O relatório agora consolida em Markdown seguro os ciclos históricos comprovados, os testes de regressão concluídos em 2.2 e 2.3, a matriz de audience/isolamento e fronteiras financeiras, a mutação controlada, os mocks, a cobertura, os testes ignorados e os riscos. Não há senhas, tokens, hashes, chaves, CPFs ou dados pessoais.

Com o ciclo de redefinição administrativa acima, a task 4.2 agora consolida três ciclos TDD comprováveis: JWT, refresh token e reset administrativo por CLI. Audience/RBAC e finanças permanecem explicitamente classificados como regressão e caracterização, não como ciclos TDD retrospectivos. Os critérios documentais de baseline, branches, mocks, mutação, limitações e segurança foram consolidados sem dados sensíveis.

## Conclusão das lacunas contratuais

### Task 2.2

Não foi encontrada lacuna funcional real. A implementação atual e os testes cobrem audience e authorities incompatíveis, tokens ausentes, inválidos, expirados ou malformados, rejeição cruzada entre áreas, principal inativo, respostas `401`/`403`, isolamento de conta e ausência de mutação após rejeição. As principais evidências são `JwtAuthenticationTests`, `AdminAuthenticationTests`, `AdminFinancialIsolationTests` e `JwtBearerFinancialAuthorizationTests`.

### Task 2.3

Não foi encontrada lacuna contratual real. A cobertura atual inclui saldo insuficiente sem registros parciais, propriedade de conta e associação de corretora, BRL/USD e conversão, arredondamento `HALF_UP`, valores inválidos, compra/venda/transferência, rollback e conservação de posições. As principais evidências são `PurchaseServiceIntegrationTests`, `FinancialAmountTests`, `PositionFinancialCalculatorTests`, `PurchaseRollbackTests`, `SaleRollbackTests` e `TransferRollbackTests`.

Os testes existentes de audience/RBAC e finanças devem continuar classificados como regressão e caracterização do comportamento correto. A mutação controlada documentada na task 3.1 é evidência de mutação, não substitui um ciclo TDD. O terceiro ciclo desta retomada foi executado sobre um requisito operacional novo e legítimo, sem alterar regras financeiras; o OpenSpec não reduziu a exigência de três ciclos.

## Correção pós-revisão independente — 2026-11-13

Os findings do reviewer e as falhas reportadas pelo tester foram investigados sem alterar regras de produção, migrations já executadas ou contratos financeiros.

- `PostgreSqlAdminAuditMigrationRollbackTests` agora declara e verifica exatamente as nove ações da migration 014 após o rollback, preserva todos os registros, rejeita `RESET_ADMIN_PASSWORD`, reaplica a 015 com as dez ações e exige falha explícita quando já existe auditoria RESET.
- `AdminPasswordResetPostgreSqlTests` agora compara cardinalidade, identidade, hash, administrador, família, datas, substituição e estado dos refresh tokens antes/depois. O sucesso revoga exatamente os tokens do administrador alvo e preserva os tokens do outro administrador; os rollbacks preservam as linhas sem revogação.
- O timestamp do administrador inativo passou a usar fixture fixa com precisão de microssegundos compatível com PostgreSQL, sem tolerância ampla e sem alteração de produção.
- A falha anterior de `GlobalExceptionHandlerTests.typedBusinessAndConflictMessagesContainOnlyControlledInformation` não foi reproduzida: a classe isolada passou com 9/9, o conjunto combinado afetado passou com 27/27 e a suíte completa pós-correção também passou. Não foi feita alteração artificial na asserção; a não reprodução permanece registrada como limitação histórica.

Validação pós-correção: `mvn clean verify` executou 543 testes, com 0 falhas, 0 erros e 7 ignorados; JaCoCo reportou linhas `3503/3676` (`95,29%`) e branches `1141/1422` (`80,24%`). Os sete testes PostgreSQL locais opt-in continuam ignorados e não são considerados aprovados. O rollback/reaplicação da migration 015 passou em PostgreSQL 16 descartável via Testcontainers.

### Decisão metodológica formalizada

Em 2026-11-13, o responsável pelo projeto autorizou nesta conversa a adequação da interpretação das tasks 2.2 e 2.3 à seção 21 do material original. A exigência de três ciclos RED → GREEN → REFACTOR permanece integral: os ciclos históricos de JWT e refresh token e o terceiro ciclo contemporâneo de reset administrativo por CLI continuam documentados. Audience ADMIN/INVESTOR, RBAC, propriedade e fronteiras financeiras permanecem regressões obrigatórias, sem cronologia RED retroativa. Esta é uma decisão interna registrada pelo responsável; não é apresentada como aprovação individual do professor.

### Matriz dos findings desta rodada

| Finding | Causa confirmada | Correção | Teste executado | Resultado |
|---|---|---|---|---|
| Rollback incompleto da 015 | O teste anterior cobria apenas duas ações da 014 | Conjunto exato das nove ações após rollback e dez após reaplicação | `PostgreSqlAdminAuditMigrationRollbackTests` 1/1 | Resolvido |
| Preservação de refresh tokens | `allMatch` não detectava remoção ou substituição | Comparação de cardinalidade, identidade, hash, vínculo, família, datas e revogação | `AdminPasswordResetPostgreSqlTests` 5/5 | Resolvido |
| Precisão de `inactivatedAt` | `OffsetDateTime.now()` produzia nanos incompatíveis com a precisão PostgreSQL | Fixture fixa com seis casas decimais | `AdminPasswordResetPostgreSqlTests` 5/5 e `mvn clean verify` | Resolvido |
| Falha intermitente do handler | Causa não reproduzida no estado atual | Nenhuma alteração artificial; experimento documentado | Classe 9/9, foco combinado 27/27, suíte completa 543/543 | Não reproduzido; limitação histórica registrada |

## Testes ignorados no backend

## Matriz de correção dos findings

| Finding original | Causa | Correção | Teste/evidência | Situação |
|---|---|---|---|---|
| 1 — senha em memória | O fluxo anterior convertia a senha lida pelo console para `String` e não limpava todos os caminhos | `char[]` até o encoder, `CharBuffer`, limpeza em `finally` no comando e no serviço, sem credenciais em argumentos ou logs | `AdminPasswordResetCommandTests` e `AdminPasswordResetServiceTests` cobrem sucesso, `null`, exceção de leitura e falha do serviço; risco residual documentado | Resolvido, com limitação inevitável de cópias internas do encoder/JVM |
| 2 — atomicidade | Não havia prova real em PostgreSQL para falhas de revogação e auditoria; a constraint não aceitava a nova ação | Serviço transacional único, migration 015 para a constraint e cenários de falha via triggers somente no Testcontainers | `AdminPasswordResetPostgreSqlTests`: 5/5, incluindo rollback e preservação de outros administradores/investidor | Resolvido nos testes isolados; reviewer deve confirmar a semântica transacional |
| 3 — JWT desativado | O comando e o serviço eram condicionais e uma solicitação explícita podia não produzir resposta clara | `ObjectProvider` lazy no comando: sem flag, startup normal; com flag e serviço indisponível, erro explícito antes da leitura | `AdminPasswordResetCommandTests` cobre as duas situações; `AdminJwtDisabledTests` passou na suíte completa | Resolvido |
| 4 — relatório desatualizado | O relatório afirmava que não havia código funcional novo | Relatório atualizado com arquivos, migration, testes, cobertura, findings e limitações; cronologia TDD preservada | Este relatório e `git diff --check` | Resolvido |
| 5 — falhas do console | Faltavam cenários observáveis para console indisponível, leitura nula/excepcional e erro posterior | Testes de erro e limpeza de buffers em sucesso e falha, sem expor senha em mensagens | `AdminPasswordResetCommandTests`: 8/8; `AdminPasswordResetServiceTests`: 4/4 | Resolvido |

Nenhum reviewer ou tester independente foi executado nesta etapa. A matriz registra as correções e as evidências automatizadas disponíveis; a aprovação independente continua sendo um gate posterior.

Os sete testes ignorados no `mvn clean verify` são todos testes PostgreSQL local opt-in:

1. `LocalPostgreSqlAccountRegistrationTests.persistsAccountInitialMovementAndPatrimonialPointInPostgreSql`
2. `LocalPostgreSqlAccountRegistrationTests.postgreSqlEnforcesActiveCpfUniqueness`
3. `LocalPostgreSqlAuthenticationTests.inactivatesAndReactivatesPreservedAccountInExclusivePostgreSqlTestDatabase`
4. `LocalPostgreSqlAuthenticationTests.changesCredentialAndRevokesEverySessionInExclusivePostgreSqlTestDatabase`
5. `LocalPostgreSqlAuthenticationTests.persistsAndRecoversMinimalPrincipalInExclusivePostgreSqlTestDatabase`
6. `LocalPostgreSqlSessionTests.persistsAndRecoversSecuritySessionInExclusiveLocalTestDatabase`
7. `LocalPostgreSqlPurchaseTests.persistsLockedPurchaseAtomicallyInIsolatedLocalPostgreSql`

Todos foram ignorados porque a variável `RUN_LOCAL_POSTGRES_TESTS` não existe no ambiente. Esses testes exigem um banco PostgreSQL local de teste explicitamente configurado (`LOCAL_POSTGRES_TEST_URL`, `LOCAL_POSTGRES_TEST_USER` e `LOCAL_POSTGRES_TEST_PASSWORD`); não devem apontar para produção. Não são considerados aprovados. Os testes PostgreSQL via Testcontainers executaram normalmente.

### 3.1 — mutação controlada

Foi feita uma única mutação temporária, fora do estado final do repositório, em `AdminJwtSecurityConfiguration.java`: a condição de `exactAudience` foi removida apenas durante a execução de um teste focado.

Comando RED:

```text
cd backend
.\mvnw.cmd --batch-mode "-Dmaven.repo.local=..\.m2-repository" "-Dtest=AdminAuthenticationTests#invalidAdministrativeClaimsAreRejected" "-Dlogging.level.root=ERROR" clean test
```

Resultado: `BUILD FAILURE`; 1 teste executado, 1 falha. O teste detectou que a audience incorreta seria aceita (esperado `401`, recebido `200`).

A linha original foi restaurada imediatamente após o processo terminar. O mesmo comando foi executado novamente com a implementação correta: `BUILD SUCCESS`, 1 teste, 0 falhas, 0 erros. `git diff` confirmou que não restou alteração funcional.

### 3.2 — justificativa dos mocks

| Dependência | Uso | Por que é isolada | Sistema sob teste preservado |
|---|---|---|---|
| `BrazilMarketDataPort`, `UsMarketDataPort` e `UsdBrlExchangeRatePort` | Testes de compra, cache e rollback | Evitar rede, cotações instáveis e câmbio externo | Serviços, entidades, transações, repositórios e cálculos financeiros reais |
| `CompanyRegistryPort` e `RegulatoryRegistryPort` | Testes de cadastro/associação e fluxos financeiros | Controlar respostas de provedores externos e suas falhas | Regras de propriedade e persistência reais |
| `Clock.fixed(...)` | Testes de validade, período, expiração e auditoria | Tornar instantes determinísticos | Regras que consomem o relógio real do sistema |
| `@MockitoSpyBean` em repositórios | Testes de rollback | Injetar falha de persistência controlada | Transação e rollback reais; não substitui a regra de negócio |
| Chaves/serviços de token gerados nos testes | Testes de autenticação | Isolar credenciais de teste sem reutilizar secrets | Validação real de claims, audience, issuer, expiração e authorities |
| PostgreSQL via Testcontainers | Testes de integração | Não é mock; valida o banco real quando Docker está disponível | Queries, transações e mapeamentos reais |

Não foi adicionado mock do comportamento financeiro que o teste pretende comprovar.

## Comandos e resultados

### Backend

- Foco desta correção: `8` testes de `AdminPasswordResetCommandTests`, `4` de `AdminPasswordResetServiceTests`, `5` de `AdminPasswordResetPostgreSqlTests` e `1` de `PostgreSqlAdminAuditMigrationRollbackTests`, todos com `0` falhas, `0` erros e `0` ignorados — aprovado.
- `mvn clean verify`: `543` testes executados, `0` falhas, `0` erros, `7` ignorados — `BUILD SUCCESS` após normalizar a asserção de `inactivatedAt` para a precisão de microssegundos persistida pelo PostgreSQL.
- A execução completa incluiu os cinco testes PostgreSQL isolados da redefinição administrativa com Testcontainers; os relatórios Surefire confirmam `0` falhas e `0` erros em todas as classes. Alguns logs de encerramento de contextos paralelos registraram conexões já encerradas, sem falha de teste.
- O fixture de `AdminDashboardPostgreSqlTests.aggregatesAdministrativeDashboardOnPostgreSqlWithoutLoadingFinancialEntities` permanece determinístico, usando `now.minusDays(1).withHour(1).withMinute(0).withSecond(0).withNano(0)`. Nesta correção, também foi adicionada a migration 015 para aceitar a auditoria do reset administrativo; nenhuma regra financeira foi alterada.
- JaCoCo backend: linhas `3503/3676` (`95,29%`) e branches `1141/1422` (`80,24%`); todos os checks configurados foram atendidos.
- A execução usou PostgreSQL/Testcontainers; portanto, os testes de integração realmente foram exercitados quando Docker esteve disponível.

### Frontend

- `npm test`: `43` arquivos, `396/396` testes — aprovado.
- `npm run test`: `43` arquivos, `396/396` testes — aprovado.
- `npm run test:coverage`: `396/396` testes — aprovado.
- Coverage V8: Statements `88,44%`; Branches `81,49%`; Functions `92,08%`; Lines `93,29%`.
- `npm run lint` — aprovado, sem warnings.
- `npm run build` — aprovado.

Uma tentativa inicial com `npm test -- --runInBand` foi rejeitada pelo Vitest por opção inexistente; o script oficial, sem esse argumento, foi executado com sucesso.

### OpenSpec, Graphify e Git

- `openspec validate aula-15-tdd-cobertura-evidencias --strict` — change válido.
- `graphify update .` — executado após as alterações de código e documentação; a atualização não substitui a revisão humana do diff.
- `git diff --check` — sem erros de whitespace.
- Reviewer e tester são gates externos previstos pelo projeto e não foram executados pelo Codex.

## Parecer desta retomada

O código funcional não apresentou regressão nos testes focados ou frontend. A mutação controlada foi detectada e completamente restaurada; as justificativas de mocks estão documentadas.

Os gates automatizados internos estão verdes após a correção determinística do fixture PostgreSQL e os testes focados do novo CLI. As tasks 2.2 e 2.3 estão concluídas como regressões obrigatórias, 2.4 e 4.2 estão concluídas com a evidência do terceiro ciclo e 4.3 permanece pendente para revisão independente do reviewer/tester conforme a política do projeto. Parecer: pronto para revisão independente; não arquivar ainda.
