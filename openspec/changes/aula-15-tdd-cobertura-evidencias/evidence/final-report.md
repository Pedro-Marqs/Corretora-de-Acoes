# Evidências finais — Aula 15

Data da validação: 2026-11-12  
Branch: `feature/aulas-13-15`  
HEAD: `ed0939d` (`Habilita JWT administrativo no Compose local`)  
Sincronização registrada antes desta validação: `0/0` com `origin/feature/aulas-13-15`.

Este relatório não contém senhas, JWTs, cookies, secrets, hashes, CPFs ou dados pessoais.

## Estado do Git

- O worktree já continha alterações preservadas antes desta retomada; elas não foram descartadas, restauradas ou staged.
- Nenhum arquivo foi staged, commitado, enviado, restaurado ou descartado.
- Os commits anteriores permanecem preservados.
- As alterações desta validação são a evidência documental, a atualização de `tasks.md` para as tarefas comprovadas e a correção determinística de um fixture de teste PostgreSQL.

## Tarefas e evidências

### 2.1 — autenticação e sessão

Permanece concluída com a evidência histórica em `evidence/aula-13-refresh-stage.md` e `evidence/baseline.md`, incluindo rejeição de token inválido/expirado, rotação e revogação de refresh token.

### 2.2 — audience `USER`/`ADMIN`

Os comportamentos estão cobertos no estado atual por `AdminAuthenticationTests` e `AdminFinancialIsolationTests`, incluindo rejeição cruzada, respostas `401`/`403`, principal incompatível e ausência de mutação.

Não foi produzido um novo ciclo RED → GREEN → REFACTOR nesta retomada. A implementação já estava correta no HEAD; introduzir uma falha artificial apenas para fabricar um RED não seria evidência TDD autêntica. A tarefa permanece pendente até existir um RED histórico verificável ou uma fronteira ainda não coberta que exija implementação real.

### 2.3 — fronteiras financeiras e monetárias

O estado atual possui cobertura GREEN em `PurchaseServiceIntegrationTests`, `FinancialAmountTests`, `PositionFinancialCalculatorTests`, `PurchaseRollbackTests`, `SaleRollbackTests` e `TransferRollbackTests`, cobrindo insuficiência de saldo, propriedade, BRL/USD, conversão, arredondamento, rollback e conservação de posições.

Não foi produzido um novo ciclo RED → GREEN → REFACTOR sem alterar artificialmente uma regra já correta. A tarefa permanece pendente pela mesma limitação de evidência, não por uma falha funcional observada.

## Investigação histórica das evidências TDD

A investigação dos changes arquivados das Aulas 13 e 14 encontrou ciclos TDD autênticos, mas eles não podem ser atribuídos retroativamente às tarefas 2.2 e 2.3 da Aula 15:

- `evidence/baseline.md` registra o ciclo da infraestrutura JWT da Aula 13: o teste `JwtAuthenticationTests` foi criado antes das classes de implementação e falhou na compilação (RED), a implementação mínima fez os cinco testes passarem (GREEN) e uma etapa posterior explicitou conversor, validações de secret/TTL, defaults e limpeza do `SecurityContext`, mantendo os testes verdes (REFACTOR).
- `evidence/aula-13-refresh-stage.md` registra o ciclo de refresh token: o teste de reuso falhou com `204` em vez de `401` (RED), a correção de transação/exceção fez os 45 testes passarem (GREEN) e a separação de exceções, o escopo de `noRollbackFor` e os cenários de isolamento foram refinados depois (REFACTOR).
- O commit `9334d6a` adicionou a implementação JWT administrativa e `AdminAuthenticationTests` no mesmo commit. O commit `a22d9fb` adicionou RBAC financeiro e `AdminFinancialIsolationTests` no mesmo commit. O commit `96c47eb` adicionou `FinancialAmount` e seus testes no mesmo commit. Esses commits comprovam implementação e cobertura, mas não comprovam um RED executado antes da implementação.

Não foi localizado nos artefatos arquivados um comando/resultado contemporâneo que demonstre RED → GREEN → REFACTOR especificamente para audience ADMIN/INVESTOR, RBAC administrativo, arredondamento ou moedas da Aula 14. Portanto, não há base para declarar esses ciclos como evidência da Aula 15.

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
| Testes PostgreSQL locais | Limitação ambiental | Sete testes `LocalPostgreSql*` | Ignorados por ausência de `RUN_LOCAL_POSTGRES_TESTS` | Não tratados como aprovados; Testcontainers executou |

Legenda: TDD exige uma falha legítima antes da implementação, passagem após a implementação e passagem após refatoração. Teste de regressão confirma comportamento já implementado. Teste de mutação verifica se a suíte detecta uma alteração incorreta temporária. Essas categorias não são intercambiáveis.

## Baseline e resultado final

O baseline registrado em `evidence/baseline.md` foi mantido como histórico, sem sobrescrever seus resultados. A comparação abaixo é indicativa: a suíte cresceu durante as Aulas 13 e 14, portanto não representa uma comparação causal isolada da Aula 15.

| Métrica | Baseline registrado | Estado final desta validação | Gate |
|---|---:|---:|---:|
| Backend — testes | 352 executados, 0 falhas, 0 erros, 7 ignorados | 525 executados, 0 falhas, 0 erros, 7 ignorados | Aprovado |
| JaCoCo — linhas | 94,80% | 95,16% | 80% |
| JaCoCo — branches | 80,04% | 80,10% | 80% |
| Frontend — testes | 274 aprovados | 396 aprovados | Aprovado |
| Vitest — statements | 79,86% | 88,44% | 80% |
| Vitest — branches | 72,24% | 81,49% | 80% |
| Vitest — functions | 84,39% | 92,08% | 80% |
| Vitest — lines | 84,74% | 93,29% | 80% |

Os resultados finais não reduzem thresholds, não excluem classes artificialmente e não contam testes ignorados como aprovados.

## Avaliação literal da task 4.2

O relatório agora consolida em Markdown seguro os ciclos históricos comprovados, os limites de 2.2 e 2.3, a matriz de audience/isolamento e fronteiras financeiras, a mutação controlada, os mocks, a cobertura, os testes ignorados e os riscos. Não há senhas, tokens, hashes, chaves, CPFs ou dados pessoais.

Ainda assim, todos os critérios literais da task 4.2 não podem ser declarados satisfeitos: ela exige a consolidação de três ciclos TDD reais, e somente os ciclos históricos da Aula 13 estão comprovados. O ciclo de audience ADMIN/INVESTOR e o ciclo financeiro/moeda não possuem RED contemporâneo verificável. Por isso, a task 4.2 permanece pendente no `tasks.md`.

Para concluí-la sem falsificar evidência, falta uma decisão acadêmica explícita: aceitar os ciclos históricos da Aula 13 mais a caracterização/regressão e a mutação controlada como trilha metodológica suficiente, ou definir um requisito novo, legítimo e ainda não implementado que permita executar os ciclos 2.2 e 2.3 de forma autêntica. Não é adequado alterar a especificação ou marcar a task apenas para satisfazer o contador.

## Conclusão das lacunas contratuais

### Task 2.2

Não foi encontrada lacuna funcional real. A implementação atual e os testes cobrem audience e authorities incompatíveis, tokens ausentes, inválidos, expirados ou malformados, rejeição cruzada entre áreas, principal inativo, respostas `401`/`403`, isolamento de conta e ausência de mutação após rejeição. As principais evidências são `JwtAuthenticationTests`, `AdminAuthenticationTests`, `AdminFinancialIsolationTests` e `JwtBearerFinancialAuthorizationTests`.

### Task 2.3

Não foi encontrada lacuna contratual real. A cobertura atual inclui saldo insuficiente sem registros parciais, propriedade de conta e associação de corretora, BRL/USD e conversão, arredondamento `HALF_UP`, valores inválidos, compra/venda/transferência, rollback e conservação de posições. As principais evidências são `PurchaseServiceIntegrationTests`, `FinancialAmountTests`, `PositionFinancialCalculatorTests`, `PurchaseRollbackTests`, `SaleRollbackTests` e `TransferRollbackTests`.

Os testes existentes devem ser classificados como regressão e caracterização do comportamento correto. A mutação controlada documentada na task 3.1 é evidência de mutação, não um ciclo TDD de implementação. Como não existe uma lacuna legítima, não foi criado teste RED artificial, não foi alterada regra de produção e 2.2/2.3 permanecem pendentes. A alternativa acadêmica transparente é submeter os ciclos históricos da Aula 13 e a mutação controlada como evidências metodológicas, solicitando ao responsável aprovação explícita para aceitar a ausência de RED legítimo nessas duas tarefas; caso não aceite, deve ser definido um requisito novo e real ainda não implementado. O OpenSpec não foi alterado silenciosamente.

## Testes ignorados no backend

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

- Foco de segurança/financeiro: `106` testes, `0` falhas, `0` erros, `0` ignorados — aprovado.
- `mvn clean verify`: `525` testes executados, `0` falhas, `0` erros, `7` ignorados — aprovado.
- O fixture de `AdminDashboardPostgreSqlTests.aggregatesAdministrativeDashboardOnPostgreSqlWithoutLoadingFinancialEntities` foi corrigido de forma mínima para usar `now.minusDays(1).withHour(1).withMinute(0).withSecond(0).withNano(0)`. O instante mantém o offset existente e fica fixado em 01:00 do dia local anterior, sem depender do horário de execução nem cruzar meia-noite. Nenhum código de produção ou regra financeira foi alterado.
- JaCoCo backend: linhas `3443/3618` (`95,16%`) e branches `1115/1392` (`80,10%`); todos os checks configurados foram atendidos.
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
- `graphify update .` — executado após a alteração documental; não houve mudança em arquivos de código, e a atualização não substitui a revisão humana do diff.
- `git diff --check` — sem erros de whitespace.
- Reviewer e tester são gates externos previstos pelo projeto e não foram executados pelo Codex.

## Parecer desta retomada

O código funcional não apresentou regressão nos testes focados ou frontend. A mutação controlada foi detectada e completamente restaurada; as justificativas de mocks estão documentadas.

Os gates automatizados internos estão verdes após a correção determinística do fixture PostgreSQL. Não houve correção de produção nesta retomada. Ainda não há evidência suficiente para concluir a Aula 15: 2.2 e 2.3 não possuem RED autêntico verificável para os comportamentos já corretos, 4.2 depende da consolidação metodológica dos ciclos e 4.3 aguarda a revisão independente do reviewer/tester conforme a política do projeto. Parecer: bloqueado para conclusão automática, apto para revisão acadêmica sobre a alternativa metodológica documentada.
