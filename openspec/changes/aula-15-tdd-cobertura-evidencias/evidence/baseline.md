# Baseline da Aula 15

Data da execução: 2026-11-05 (America/Sao_Paulo)

## Identificação e estado preservado

- Branch: `dev`
- HEAD analisado: `75b1bafb14ee482eb74ae22c9181d4d8fae35f11`
- Estado antes da execução: já havia `docs/continuidade.md` modificado e os três diretórios de changes OpenSpec não versionados (`aula-13-migrar-autenticacao-jwt`, `aula-14-rbac-administrativo-investidores` e `aula-15-tdd-cobertura-evidencias`). Essas alterações foram preservadas.
- Nenhum arquivo de produção, teste existente, CI, branch ou commit foi alterado.
- `backend/target`, `frontend/dist` e `frontend/coverage` foram somente artefatos gerados pelos comandos e permanecem ignorados pelo Git.

## Comandos executados

Graphify, antes da leitura focalizada dos arquivos:

```text
graphify query "For the Aula 15 baseline, where are the backend test configuration, JaCoCo thresholds and reports, frontend coverage commands, PostgreSQL/Testcontainers tests, and current authentication or financial test suites?" --budget 6000
```

Backend:

```text
mvn.cmd --batch-mode clean verify
```

O Maven inicialmente não conseguiu usar o repositório local padrão (`C:\\m2\\repository`) nas tentativas com `mvnw.cmd` e com `-Dmaven.repo.local`. A execução efetiva usou o cache já existente no workspace:

```text
$env:MAVEN_ARGS='-Dmaven.repo.local=C:\\Users\\Arklok\\Desktop\\Faculdade\\Projetos\\corretora\\.m2-repository'
mvn.cmd --batch-mode clean verify
```

Resultado: `BUILD SUCCESS`; JaCoCo executou `report` e `check` no ciclo `verify` e informou que os limites configurados foram atendidos.

Frontend:

```text
npm test
npm run lint
npm run build
npm run test:coverage
```

O primeiro `npm test` dentro do sandbox falhou antes de carregar o Vitest por restrição de acesso ao diretório (`Access is denied` ao resolver `vite.config.js`). O mesmo comando foi repetido fora do sandbox, sem alteração no projeto, e passou. Os demais comandos também foram executados fora do sandbox por causa dessa limitação operacional.

## Configuração encontrada

- Backend: `backend/pom.xml:106-150`, JaCoCo `0.8.12`, relatório e verificação no `verify`; limites atuais de linha e branch em `0.80` (`backend/pom.xml:135-143`). O projeto também declara PostgreSQL e Testcontainers (`backend/pom.xml:57-88`).
- Frontend: scripts em `frontend/package.json:10-15`; cobertura V8, relatórios text/html/lcov e diretório `coverage` em `frontend/vite.config.js:19-29`; limiares atuais de linhas, branches, funções e statements em `80`.
- Relatórios gerados: `backend/target/site/jacoco/index.html`, `backend/target/site/jacoco/jacoco.xml`, `frontend/coverage/index.html` e `frontend/coverage/lcov.info`. São artefatos locais ignorados, não evidências versionadas.

## Resultado backend

Resumo dos XMLs em `backend/target/surefire-reports/`:

| Métrica | Resultado |
|---|---:|
| Arquivos de suíte | 78 |
| Testes | 336 |
| Falhas | 0 |
| Erros | 0 |
| Ignorados | 7 |

Cobertura consolidada do `backend/target/site/jacoco/jacoco.xml`:

| Métrica | Cobertos | Perdidos | Percentual |
|---|---:|---:|---:|
| Instruções | 12.801 | 678 | 94,97% |
| Ramos | 810 | 202 | 80,04% |
| Linhas | 2.442 | 134 | 94,80% |
| Métodos | 661 | 34 | 95,11% |
| Classes | 179 | 2 | 98,90% |

O gate JaCoCo passou por uma margem de `0,04` ponto percentual em branches. Nenhum segredo, token ou credencial foi adicionado às evidências. Os relatórios podem conter nomes de campos e caminhos de código como parte da instrumentação normal; não foram copiados valores de autenticação ou dados pessoais para este arquivo.

## Resultado frontend

| Comando | Resultado |
|---|---|
| `npm test` | 30 arquivos, 274 testes aprovados |
| `npm run lint` | passou sem warnings |
| `npm run build` | passou; bundle Vite gerado |
| `npm run test:coverage` | testes passaram, mas o processo falhou no gate de cobertura |

Cobertura V8 global exibida no terminal:

| Métrica | Cobertos | Total | Percentual | Limite |
|---|---:|---:|---:|---:|
| Statements | 1.468 | 1.838 | 79,86% | 80% |
| Branches | 1.119 | 1.549 | 72,24% | 80% |
| Functions | 384 | 455 | 84,39% | 80% |
| Lines | 1.067 | 1.259 | 84,74% | 80% |

Diagnóstico: a falha é real do gate atual do frontend, não foi causada por alteração deste baseline. O relatório indica lacunas relevantes em `src/layout/AppLayout.jsx`, `src/pages/OperationsPage.jsx`, `src/pages/WalletPage.jsx`, `src/pages/WalletDetailPage.jsx` e `src/pages/PortfolioPage.jsx`. Os limiares não foram reduzidos.

## Testes condicionais e limitações de ambiente

Os sete skips do backend foram identificados nos XMLs, todos por ausência de `RUN_LOCAL_POSTGRES_TESTS=true`:

- `LocalPostgreSqlAccountRegistrationTests`: 2
- `LocalPostgreSqlAuthenticationTests`: 3
- `LocalPostgreSqlSessionTests`: 1
- `LocalPostgreSqlPurchaseTests`: 1

Esses testes não representam falhas funcionais: não foram executados por serem opt-in e dependerem de PostgreSQL local. `PostgreSqlMigrationTests` está configurado com `@Testcontainers(disabledWithoutDocker = true)`; não houve falha dessa suíte no `verify` realizado. Os testes de smoke externos `T17ExternalSmokeTests` e `T21ExternalSmokeTests` também possuem gates por variáveis próprias e não foram tratados como prova de integração com provedores externos.

Conclusão operacional: o baseline local comprova a suíte H2 e o gate JaCoCo, mas não substitui uma execução com PostgreSQL local habilitado, Docker disponível e, quando aplicável, os ambientes externos configurados.

## Matriz inicial de comportamentos e evidências

| Área | Evidência atual | Lacuna ou foco para os ciclos |
|---|---|---|
| Autenticação de investidor | `AuthenticationTests`, `SecurityConfigTests`, `SecureSessionCookieTests`, `frontend/src/api/auth.test.js` | O baseline cobre a autenticação baseada em sessão; não há evidência JWT Bearer, token ausente/adulterado/expirado ou revogação JWT. |
| `Account`/propriedade horizontal | `AccountManagementTests`, `AccountLifecycleTests`, `WalletControllerTests`, `WalletServiceTests` | Preservar a regra de que o investidor opera somente a própria conta ao migrar a identidade. |
| Cadastro, ativação e desativação | `AccountRegistrationTests`, `AccountManagementTests`, `AuthenticationTests` | A cobertura de PostgreSQL desses fluxos está condicionada a `RUN_LOCAL_POSTGRES_TESTS`; a futura desativação administrativa precisa manter bloqueio de operações e reativação segura. |
| Compra | `PurchaseServiceTests`, `PurchaseServiceIntegrationTests`, `PurchaseRollbackTests`, `LocalPostgreSqlPurchaseTests` | O fluxo tem cobertura unitária/integração, mas a variante PostgreSQL local foi ignorada; testar também autorização e não mutação indevida após JWT/RBAC. |
| Venda | `SaleServiceTests`, `SaleServiceIntegrationTests`, `SaleRollbackTests` | Revalidar isolamento e consistência financeira após alterações de autenticação. |
| Transferências | `TransferServiceIntegrationTests`, `TransferRollbackTests`, `frontend/src/pages/TransfersPage.test.jsx` | Manter propriedade, atomicidade, moeda e rollback. |
| Valores financeiros, moeda e câmbio | `FinancialAmountTests`, `PositionFinancialCalculatorTests`, `WalletServiceConcurrencyTests` | Boa fronteira para ciclo TDD financeiro; preservar arredondamento, saldo, concorrência e ausência de mutação parcial. |
| Indicadores, período e histórico | `DashboardControllerTests`, `DashboardServiceTests`, `DashboardPeriodTests`, `HistoryControllerTests`, `FinancialHistoryServiceValidationTests` | Para Aula 14, separar indicadores globais administrativos de carteiras/históricos individuais e testar filtros, período e paginação. |
| `AdminUser`/`ADMIN` | Não há teste ou implementação de `AdminUser` no baseline atual | Deve ser criado somente na Aula 14, com identidade separada, audience distinta, `401/403`, sem operações financeiras e sem cadastro público. |
| Frontend de sessão | Testes das APIs, páginas de conta, carteira, transferências e `PrivateRoute` | A migração deverá definir transporte/armazenamento seguro do Bearer, logout, expiração, CSRF e CORS sem assumir `localStorage`. |

## Candidatos recomendados para os três ciclos TDD

Os percentuais abaixo vêm do contador `BRANCH` por classe no relatório JaCoCo, antes de qualquer implementação:

1. **Token e autenticação:** `com/projeto/gestao/api/controller/AuthenticationController` — `1/2` branches, `50,00%`, com `AuthenticationTests` como ponto de entrada. O ciclo deverá ser feito durante a Aula 13 e registrar os casos de token ausente, inválido, adulterado e expirado; o baseline atual ainda é de sessão.
2. **Identidade administrativa e desativação:** `com/projeto/gestao/service/AccountManagementService` — `13/14` branches, `92,86%`, junto de `AccountManagementTests` e `AccountLifecycleTests`. A classe não substitui o futuro `AdminUser`; é a fronteira existente mais próxima para ativação/desativação, e o novo isolamento deverá ter testes próprios sem acesso cruzado.
3. **Fronteira financeira:** `com/projeto/gestao/service/PurchaseService` — `19/26` branches, `73,08%`, com `PurchaseServiceTests`, `PurchaseServiceIntegrationTests` e `PurchaseRollbackTests`. Como complemento, `PositionFinancialCalculator` tem `20/24` (`83,33%`) e `WalletService` `11/16` (`68,75%`). O ciclo deve provar propriedade, saldo, arredondamento e ausência de mutação indevida.

Outras classes com branches parcialmente cobertos e potencial de investigação: `TransferService` `19/24` (`79,17%`), `PurchaseTransactionService` `42/52` (`80,77%`), `DashboardService` `83/102` (`81,37%`) e `SaleTransactionService` `32/48` (`66,67%`). A seleção final dos testes deve ser confirmada após os designs das Aulas 13 e 14.

Este arquivo registra somente o baseline. Os ciclos Red → Green → Refactor, mutação, novos mocks e implementação de JWT/RBAC ainda não foram iniciados.
