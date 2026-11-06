# Evidências — Aula 13, etapa de refresh tokens

## Escopo

Esta evidência registra a implementação de refresh tokens opacos para investidores, com hash persistido, rotação, detecção de reutilização, revogação por família, logout, alterações de credenciais e inativação de conta. Access tokens já emitidos não são revogados automaticamente quando um refresh token é revogado; eles continuam válidos até a expiração configurada.

Não foram implementados frontend JWT, refresh tokens administrativos, `AdminUser` ou dashboard.

## Comandos e resultados

Os comandos foram executados a partir de `backend`, usando um repositório Maven local relativo ao repositório para contornar uma restrição de permissões do cache global:

```text
$env:MAVEN_ARGS='-Dmaven.repo.local=../.m2-repository'; mvn.cmd --batch-mode '-Dtest=RefreshTokenTests,RefreshTokenConcurrencyTests,AccountManagementTests' test
Resultado final: BUILD SUCCESS; 16 testes, 0 falhas, 0 erros, 0 ignorados.

$env:MAVEN_ARGS='-Dmaven.repo.local=../.m2-repository'; mvn.cmd --batch-mode '-Dtest=SchemaMigrationTests' test
Resultado final: BUILD SUCCESS; 15 testes, 0 falhas, 0 erros, 0 ignorados.

$env:MAVEN_ARGS='-Dmaven.repo.local=../.m2-repository'; mvn.cmd --batch-mode clean verify
Resultado final: BUILD SUCCESS; 365 testes, 0 falhas, 0 erros, 10 ignorados.
JaCoCo: All coverage checks have been met.

Teste de regressão transacional executado antes da correção:

```text
$env:MAVEN_ARGS='-Dmaven.repo.local=../.m2-repository'; mvn.cmd --batch-mode '-Dtest=RefreshTokenReuseTransactionRegressionTests' test
Resultado Red real: BUILD FAILURE; 1 teste, 1 falha. Após A gerar B e A ser reutilizado, a tentativa de renovar B retornou 204 em vez de 401.
Falha observada em RefreshTokenReuseTransactionRegressionTests.java:99.
```

Após a inclusão do teste de expiração e a limpeza do contexto JPA nesse teste, a execução final foi repetida:

```text
$env:MAVEN_ARGS='-Dmaven.repo.local=../.m2-repository'; mvn.cmd --batch-mode '-Dtest=RefreshTokenTests,RefreshTokenConcurrencyTests' test
Resultado final: BUILD SUCCESS; 11 testes, 0 falhas, 0 erros, 0 ignorados.

$env:MAVEN_ARGS='-Dmaven.repo.local=../.m2-repository'; mvn.cmd --batch-mode '-Dtest=RefreshTokenReuseTransactionRegressionTests,RefreshTokenTests,RefreshTokenConcurrencyTests,JwtAuthenticationTests,JwtSessionCompatibilityTests,JwtDefaultSessionCompatibilityTests,JwtBearerFinancialAuthorizationTests,SchemaMigrationTests' test
Resultado Green: BUILD SUCCESS; 45 testes, 0 falhas, 0 erros, 0 ignorados.

$env:MAVEN_ARGS='-Dmaven.repo.local=../.m2-repository'; mvn.cmd --batch-mode clean verify
Resultado final: BUILD SUCCESS; 369 testes, 0 falhas, 0 erros, 10 ignorados.
JaCoCo: All coverage checks have been met.
```

Durante a primeira execução completa, antes da correção da FK da nova tabela, foram observados 362 testes, 0 falhas, 20 erros e 10 ignorados. Os erros eram falhas de limpeza dos testes ao excluir `account` enquanto existiam linhas em `refresh_token`. A migração foi ajustada com `ON DELETE CASCADE`; a execução completa posterior passou com 365 testes.

Também foi executado:

```text
graphify update .
Resultado final: grafo atualizado; 2.943 nós, 11.653 arestas e 119 comunidades.
```

## Cobertura JaCoCo final

Relatório gerado por `clean verify` em `backend/target/site/jacoco`:

| Métrica | Cobertos | Total | Percentual |
|---|---:|---:|---:|
| Instruções | 13.927 | 14.680 | 94,87% |
| Branches | 906 | 1.130 | 80,18% |
| Linhas | 2.687 | 2.839 | 94,65% |
| Métodos | 718 | 759 | 94,60% |
| Classes | 192 | 194 | 98,97% |

Os limites existentes de linhas e branches não foram reduzidos, e não foram adicionadas exclusões artificiais.

## Evidência dos testes

Foram cobertos os seguintes comportamentos:

- hash do refresh persistido sem o valor opaco original;
- rotação com emissão de novo access token e novo refresh token;
- uso único e detecção de reutilização;
- revogação da família após reutilização indevida;
- logout e limpeza do cookie;
- revogação por alteração de senha, alteração de e-mail e inativação;
- conta inativa sem possibilidade de renovação;
- concorrência de duas renovações sobre o mesmo token, permitindo somente uma;
- preservação da identidade do investidor no access token renovado;
- proteção CSRF dos endpoints que recebem refresh token por cookie;
- rollback do change set `v11-refresh-token` em banco H2 independente.

## Fortalecimento dos testes de refresh - execucao atual

Esta secao registra uma execucao posterior e independente das execucoes historicas acima. O HEAD analisado foi `8f4190198a961b47bd4136b4cd764e63fa51e254`, sem commit criado durante esta etapa.

### Ambiente

```text
docker info
Resultado: falhou porque o Docker CLI nao conseguiu conectar ao daemon Docker Engine
(pipe npipe:////./pipe/docker_engine inexistente). Os testes PostgreSQL/Testcontainers
nao foram considerados aprovados.
```

### Testes focados

```text
$env:MAVEN_ARGS='-Dmaven.repo.local=../.m2-repository'; mvn.cmd --batch-mode '-Dtest=RefreshTokenTests,RefreshTokenReuseTransactionRegressionTests,RefreshTokenConcurrencyTests,SchemaMigrationTests,JwtAuthenticationTests,JwtSessionCompatibilityTests,JwtDefaultSessionCompatibilityTests,JwtBearerFinancialAuthorizationTests' test
Resultado: BUILD SUCCESS; 46 testes, 0 falhas, 0 erros, 0 ignorados.
```

`RefreshTokenConcurrencyTests` agora exige exatamente um resultado de sucesso e um de rejeicao (`true` e `false`), alem de confirmar no banco uma unica sucessao persistida e dois tokens na familia. O teste tambem remove a conta criada ao final.

`RefreshTokenTests` deixou de usar `@Transactional` na classe. Cada requisicao MockMvc passa por seu proprio limite transacional; a limpeza remove sessoes JDBC e a conta criada. O teste `rotationFailureRollsBackConsumedTokenAndSuccessor` injeta uma falha no `flush` real do repositorio, espera HTTP 500 e confirma em consulta independente que o token consumido nao foi alterado e nenhum sucessor foi persistido.

### Build completo e cobertura atual

```text
$env:MAVEN_ARGS='-Dmaven.repo.local=../.m2-repository'; mvn.cmd --batch-mode clean verify
Resultado: BUILD SUCCESS; 370 testes, 0 falhas, 0 erros, 10 ignorados.
JaCoCo: All coverage checks have been met.
```

Os 10 testes ignorados pertencem a cenarios PostgreSQL local e Testcontainers: cinco de autenticacao/conta PostgreSQL local, um de sessao PostgreSQL local, tres de migracao PostgreSQL Testcontainers e um de compra PostgreSQL local. A causa ambiental foi a indisponibilidade do daemon Docker e a ausencia de um banco PostgreSQL local utilizavel.

O relatorio atual em `backend/target/site/jacoco/jacoco.xml` registrou:

| Metrica | Cobertos | Total | Percentual |
|---|---:|---:|---:|
| Instrucoes | 13.927 | 14.680 | 94,87% |
| Branches | 906 | 1.130 | 80,18% |
| Linhas | 2.681 | 2.833 | 94,63% |
| Metodos | 718 | 759 | 94,60% |
| Classes | 192 | 194 | 98,97% |

### Liquibase e Graphify

Os testes H2 executados confirmaram a aplicacao do change set `v11-refresh-token` e seu rollback. A definicao versionada continua contendo a FK de `refresh_token.account_id` com `ON DELETE CASCADE` e os indices de conta e familia. A validacao especifica em PostgreSQL permaneceu pendente porque os testes Testcontainers foram ignorados pelo ambiente sem Docker; nao foi registrada como aprovada.

```text
graphify update .
Resultado: grafo atualizado; 2.945 nos, 11.663 arestas e 118 comunidades.

openspec validate --changes --strict
Resultado: 4 changes validos, 0 falhas.
```

### TDD desta execucao

Nao foi registrado um novo ciclo formal Red -> Green -> Refactor nesta execucao. Os testes foram fortalecidos e validados diretamente; a falha inicial de configuracao do spy Mockito foi corrigida antes da evidencia final e nao representa o comportamento de negocio esperado. As evidencias historicas de TDD acima foram preservadas sem mistura com os resultados atuais.

## Correcoes apos a revisao independente - execucao atual

Esta secao registra somente os resultados desta tentativa de correcao. Os resultados historicos de 46 testes focados, 370 testes e JaCoCo 80,18% permanecem nas secoes anteriores e nao foram reutilizados como evidencia atual.

### Correcoes realizadas

- `RefreshTokenConcurrencyTests` passou a usar latch de prontidao com timeout, liberacao simultanea, timeout individual em cada `Future.get`, cancelamento de tarefas pendentes e encerramento limitado do executor.
- O teste documenta que a barreira confirma prontidao comum, mas nao prova a sobreposicao de todas as etapas internas no banco.
- `RefreshTokenTests.rotationFailureRollsBackConsumedTokenAndSuccessor` agora captura hash e familia antes da falha, limpa o contexto JPA e consulta o banco em chamadas JDBC independentes. As verificacoes exigem token original nao revogado, sem `replaced_by_id`, mesmo hash e mesma familia, sem sucessor e somente um registro persistido.

### Docker e PostgreSQL

```text
docker info
Resultado: falhou no Windows. O cliente 29.4.3 nao conseguiu conectar ao daemon pela
pipe npipe:////./pipe/docker_engine; tambem houve aviso de acesso negado ao arquivo
C:\Users\Arklok\.docker\config.json.
```

Nenhum teste PostgreSQL/Testcontainers foi executado nesta tentativa. Os testes Testcontainers de `PostgreSqlMigrationTests` dependem do Docker e usam `@Testcontainers(disabledWithoutDocker = true)`. Os testes PostgreSQL locais dependem de `RUN_LOCAL_POSTGRES_TESTS=true`; eles usam `LOCAL_POSTGRES_TEST_URL` opcional, com banco padrao `gestao_acoes_test`, e exigem `LOCAL_POSTGRES_TEST_USER` e `LOCAL_POSTGRES_TEST_PASSWORD`. Essas variaveis nao foram habilitadas nem configuradas, e nenhum banco foi alterado.

### Maven e cobertura

Foram tentadas execucoes focadas e o build completo sem builds paralelos. O cache Maven compartilhado apresentou `java.nio.file.AccessDeniedException` ao fechar JARs no `ZipFileSystemProvider`, inicialmente em `jakarta.transaction-api-2.0.1.jar`. O arquivo e legivel pelo sistema e `jar tf` lista seu conteudo, mas o compilador JDK falha ao fechar o arquivo. O mesmo comportamento foi reproduzido com outro JAR (`log4j-api-2.24.1.jar`) e em cache temporario isolado (`commons-collections4-4.4.jar`).

```text
mvn.cmd --batch-mode "-Dmaven.repo.local=C:\Users\Arklok\AppData\Local\Temp\corretora-m2-robocopy-20261106\repository" "-Dtest=RefreshTokenTests,RefreshTokenConcurrencyTests,RefreshTokenReuseTransactionRegressionTests" test
Resultado: bloqueado na compilacao antes da execucao dos testes; AccessDeniedException ao fechar JAR do classpath.

mvn.cmd --batch-mode "-Dmaven.repo.local=C:\Users\Arklok\AppData\Local\Temp\corretora-m2-robocopy-20261106\repository" clean verify
Resultado: BUILD FAILURE na compilacao principal pelo mesmo AccessDeniedException; nenhum teste ou relatorio JaCoCo atual foi produzido.
```

Nao ha cobertura JaCoCo nova desta tentativa. O ultimo percentual conhecido, 80,18% de branches, e historico e nao foi declarado como resultado atual.

### Validacoes de artefatos

```text
graphify update .
Resultado: grafo atualizado; 2.947 nos, 11.669 arestas e 113 comunidades.

openspec validate --changes --strict
Resultado: 4 changes validos, 0 falhas.

git diff --check
Resultado: sem erros de whitespace; apenas avisos de normalizacao LF/CRLF do Git.
```

Nao foi registrado novo ciclo formal Red -> Green -> Refactor nesta tentativa.

## Validacao com Docker - execucao de 06/11/2026

Esta secao registra uma execucao posterior, com o Docker Engine disponivel. Os resultados foram conferidos nos relatorios JUnit do Surefire em `backend/target/surefire-reports` e no relatorio agregado `backend/target/site/jacoco/jacoco.xml`. Eles nao substituem nem misturam os historicos anteriores.

### Maven e testes

```text
mvn.cmd --batch-mode clean verify
Resultado: BUILD SUCCESS; 370 testes, 0 falhas, 0 erros, 7 ignorados.
```

Os testes diretamente relevantes desta execucao foram:

```text
RefreshTokenConcurrencyTests: 1 teste, 0 falhas, 0 erros, 0 ignorados.
RefreshTokenTests: 11 testes, 0 falhas, 0 erros, 0 ignorados.
PostgreSqlMigrationTests: 3 testes, 0 falhas, 0 erros, 0 ignorados.
```

`PostgreSqlMigrationTests` utilizou o `PostgreSQLContainer` do Testcontainers com Docker. Os tres testes confirmaram a aplicacao do changelog Liquibase, as constraints PostgreSQL e o schema historico. A aplicacao e o rollback do change set `v11-refresh-token` tambem foram aprovados pelos testes correspondentes.

Os 7 testes ignorados foram exclusivamente os testes opt-in contra PostgreSQL local: 2 em `LocalPostgreSqlAccountRegistrationTests`, 3 em `LocalPostgreSqlAuthenticationTests`, 1 em `LocalPostgreSqlSessionTests` e 1 em `LocalPostgreSqlPurchaseTests`. Eles continuam dependendo de `RUN_LOCAL_POSTGRES_TESTS=true`, `LOCAL_POSTGRES_TEST_USER`, `LOCAL_POSTGRES_TEST_PASSWORD` e, opcionalmente, `LOCAL_POSTGRES_TEST_URL`.

`RefreshTokenConcurrencyTests` usa `@ActiveProfiles("test")`; o perfil configura H2 em memoria. Portanto, o resultado de concorrencia desta classe foi executado em H2, nao em PostgreSQL. Nenhum teste de concorrencia de refresh token foi executado contra PostgreSQL nesta execucao. Os 3 testes PostgreSQL executados cobrem migracao e constraints, mas nao substituem a validacao do locking concorrente do refresh token nesse banco.

### JaCoCo

Os contadores agregados do `jacoco.xml` desta execucao foram:

| Metrica | Cobertos | Total | Percentual |
|---|---:|---:|---:|
| Instrucoes | 13.921 | 14.680 | 94,83% |
| Branches | 906 | 1.130 | 80,18% |
| Linhas | 2.678 | 2.833 | 94,53% |
| Metodos | 715 | 759 | 94,20% |
| Classes | 192 | 194 | 98,97% |

Os gates existentes foram atendidos, incluindo o minimo de 80% para linhas e branches. O `jacoco.csv` da mesma pasta apresenta contadores de linhas 2.684/2.839, diferentes dos contadores agregados do `jacoco.xml` e da pagina HTML. A tabela acima usa deliberadamente o contador raiz do `jacoco.xml`; a divergencia entre formatos deve ser revisada antes do fechamento definitivo da evidencia.

### Conclusao da cobertura PostgreSQL

Docker/Testcontainers esta validado para a migracao Liquibase e constraints PostgreSQL. A concorrencia de refresh token permanece validada somente em H2. Esta limitacao nao foi ocultada nem tratada como equivalencia entre os bancos; ela deve ser considerada no risco da revisao independente desta etapa.

## TDD

Foi registrado um ciclo real para o defeito transacional de reutilização:

1. **Red:** o teste sem `@Transactional` compartilhado demonstrou HTTP 204 para B após a reutilização de A.
2. **Green:** uma exceção específica de família revogada foi configurada com `noRollbackFor` restrito; a revogação passou a ser confirmada antes do 401 e os 45 testes focados passaram.
3. **Refactor:** a exceção específica foi mantida separada da exceção genérica de autenticação, com `noRollbackFor` restrito àquele tipo; foram adicionados cenários de reutilização repetida e isolamento entre famílias/contas, e a suíte final passou.

O ciclo acima é específico deste defeito e não representa, sozinho, os três ciclos formais exigidos para concluir a Aula 15.

## Segurança e dados sensíveis

Os testes geram a chave de teste em memória e não registram o valor de tokens. O código de produção persiste apenas o hash do refresh token e não registra tokens completos ou segredos. Os valores usados nos testes não são credenciais de ambiente nem dados pessoais reais.

## Fortalecimento PostgreSQL e auditoria JaCoCo - execução atual

Esta seção registra exclusivamente a tentativa posterior de adicionar a concorrência real em PostgreSQL. A execução foi feita sem modificar produção, CI, branch ou os históricos acima.

### Implementação do teste PostgreSQL

Foi criado `backend/src/test/java/com/projeto/gestao/service/RefreshTokenPostgreSqlConcurrencyTests.java`. O teste usa `PostgreSQLContainer("postgres:16-alpine")`, o `RefreshTokenService` e o `RefreshTokenRepository` reais, datasource PostgreSQL configurado pelo container, transações independentes por thread, barreira de prontidão, timeout de lock/statement, timeout de `Future.get`, cancelamento e limpeza.

As asserções seguem a especificação de reutilização: uma renovação é aceita, a outra é rejeitada por reutilização, o sucessor é persistido, pertence à mesma conta e família, possui hash diferente e termina revogado quando a família é revogada. O teste não exige que o sucessor permaneça ativo, pois isso contradiz a revogação da família prevista no OpenSpec.

### Resultados efetivamente obtidos

```text
docker info
Resultado: falhou. O cliente 29.4.3 não conseguiu acessar o daemon pela pipe
npipe:////./pipe/docker_engine; também houve aviso de acesso negado a
C:\Users\Arklok\.docker\config.json.

mvn.cmd --batch-mode "-Dmaven.repo.local=C:\Users\Arklok\AppData\Local\Temp\corretora-m2-robocopy-20261106\repository" "-Dtest=RefreshTokenConcurrencyTests,RefreshTokenPostgreSqlConcurrencyTests,RefreshTokenTests,RefreshTokenReuseTransactionRegressionTests,PostgreSqlMigrationTests,SchemaMigrationTests" test
Resultado: BUILD FAILURE antes dos testes, durante testCompile, por
AccessDeniedException ao fechar postgresql-42.7.4.jar no cache Maven.

mvn.cmd --batch-mode "-Dmaven.repo.local=C:\Users\Arklok\AppData\Local\Temp\corretora-m2-robocopy-20261106\repository" "-Dmaven.compiler.fork=true" "-Dtest=RefreshTokenConcurrencyTests,RefreshTokenPostgreSqlConcurrencyTests,RefreshTokenTests,RefreshTokenReuseTransactionRegressionTests,PostgreSqlMigrationTests,SchemaMigrationTests" test
Resultado: BUILD FAILURE durante testCompile, com o mesmo problema de acesso
ao cache e diagnósticos derivados do compilador forked.

mvn.cmd --batch-mode "-Dmaven.repo.local=C:\Users\Arklok\AppData\Local\Temp\corretora-m2-robocopy-20261106\repository" "-Dtest=RefreshTokenConcurrencyTests,RefreshTokenPostgreSqlConcurrencyTests,RefreshTokenTests,RefreshTokenReuseTransactionRegressionTests,PostgreSqlMigrationTests,SchemaMigrationTests" surefire:test
Resultado: BUILD SUCCESS apenas para o goal direto do Surefire; o novo teste
PostgreSQL foi ignorado porque o Testcontainers não encontrou um Docker válido.
Esse comando não é evidência de execução H2 completa, pois a compilação anterior
não deixou todas as classes de teste disponíveis.

mvn.cmd --batch-mode "-Dmaven.repo.local=C:\Users\Arklok\AppData\Local\Temp\corretora-m2-robocopy-20261106\repository" clean verify
Resultado: BUILD FAILURE durante a compilação principal por
AccessDeniedException ao fechar commons-collections4-4.4.jar. Nenhum teste ou
relatório JaCoCo novo foi produzido nesta tentativa.
```

Consequentemente, a concorrência PostgreSQL não foi validada nesta execução, e a execução H2 dos testes alterados também não pode ser declarada como resultado atual. Os relatórios 370/7 e JaCoCo da seção anterior permanecem históricos da execução com Docker; não foram reutilizados como resultado desta tentativa.

### Causa da divergência JaCoCo

A comparação dos artefatos anteriores mostrou que instruções, branches, métodos e classes coincidem entre `jacoco.xml` e `jacoco.csv`. A diferença existe somente em linhas: `jacoco.xml` agrega linhas únicas por pacote/arquivo-fonte (2.678/2.833), enquanto o CSV soma contadores por classe (2.684/2.839). No pacote `com/projeto/gestao/service`, os contadores das classes somam 1.124 linhas cobertas, mas o contador do pacote soma 1.118; as seis linhas repetidas pertencem a classes que compartilham arquivos-fonte, incluindo classes internas.

Assim, os percentuais anteriores foram calculados corretamente para cada formato: 94,53% no agregado XML/HTML e 94,54% na soma por classe do CSV. A fonte oficial para os gates de bundle permanece o contador agregado XML/HTML, sem alterar exclusões ou configurações. Como o `clean verify` desta tentativa falhou antes do relatório, não há percentual JaCoCo novo a registrar.

### Validações de artefatos

```text
graphify update .
Resultado: grafo atualizado; 2.963 nós, 11.739 arestas e 119 comunidades.

openspec validate --changes --strict
Resultado: 4 changes válidos, 0 falhas.

git diff --check
Resultado: sem erros de whitespace; apenas avisos de normalização LF/CRLF do Git.
```

Não foi registrado novo ciclo formal Red -> Green -> Refactor nesta tentativa.

## Execução confirmada no PowerShell do Windows - 06/11/2026

Esta seção registra a execução posterior realizada diretamente no PowerShell do Windows pelo usuário. Os artefatos JUnit do Surefire e JaCoCo presentes em `backend/target` foram conferidos após essa execução. Ela permanece separada das tentativas anteriores do Codex, que tiveram limitações de acesso ao Docker e ao cache Maven.

### Maven e testes

```text
mvn.cmd --batch-mode clean verify
Resultado: BUILD SUCCESS; 371 testes, 0 falhas, 0 erros, 7 ignorados.
```

Os relatórios JUnit confirmam:

```text
RefreshTokenConcurrencyTests: 1 teste, 0 falhas, 0 erros, 0 ignorados.
RefreshTokenPostgreSqlConcurrencyTests: 1 teste, 0 falhas, 0 erros, 0 ignorados.
RefreshTokenTests: 11 testes, 0 falhas, 0 erros, 0 ignorados.
RefreshTokenReuseTransactionRegressionTests: 3 testes, 0 falhas, 0 erros, 0 ignorados.
PostgreSqlMigrationTests: 3 testes, 0 falhas, 0 erros, 0 ignorados.
SchemaMigrationTests: 15 testes, 0 falhas, 0 erros, 0 ignorados.
```

`RefreshTokenConcurrencyTests` foi executado em H2 pelo perfil `test`. `RefreshTokenPostgreSqlConcurrencyTests` foi executado sem ignorar o teste, usando PostgreSQL 16 via Testcontainers; portanto, o locking pessimista, as transações independentes e o resultado de uma renovação aceita e outra rejeitada foram exercitados no PostgreSQL real do container.

As verificações do teste PostgreSQL confirmaram um único sucessor persistido, `replaced_by_id` apontando para esse registro, mesma conta e família, hash diferente do token original e estado de revogação coerente com a revogação da família após a reutilização concorrente.

Os 7 testes ignorados foram exclusivamente os testes opt-in contra PostgreSQL local: 2 em `LocalPostgreSqlAccountRegistrationTests`, 3 em `LocalPostgreSqlAuthenticationTests`, 1 em `LocalPostgreSqlSessionTests` e 1 em `LocalPostgreSqlPurchaseTests`. Eles continuam dependendo de `RUN_LOCAL_POSTGRES_TESTS=true`, `LOCAL_POSTGRES_TEST_USER`, `LOCAL_POSTGRES_TEST_PASSWORD` e, opcionalmente, `LOCAL_POSTGRES_TEST_URL`. Não foram tratados como aprovados.

`PostgreSqlMigrationTests` confirmou a aplicação do changelog Liquibase no PostgreSQL, incluindo o change set `v11-refresh-token`, constraints e schema histórico. O teste de rollback H2 do change set continua registrado separadamente nas evidências anteriores.

### JaCoCo confirmado

Os contadores agregados do `backend/target/site/jacoco/jacoco.xml` desta execução foram:

| Métrica | Cobertos | Total | Percentual |
|---|---:|---:|---:|
| Instruções | 13.921 | 14.680 | 94,83% |
| Branches | 906 | 1.130 | 80,18% |
| Linhas | 2.678 | 2.833 | 94,53% |
| Métodos | 715 | 759 | 94,20% |
| Classes | 192 | 194 | 98,97% |

Todos os gates JaCoCo foram atendidos, incluindo o mínimo de 80% para linhas e branches. O `jacoco.csv` da mesma execução mantém 2.684/2.839 linhas, ou 94,54%; a diferença de seis linhas continua explicada pela soma por classe no CSV versus a agregação de linhas únicas por arquivo/pacote no XML/HTML. Nenhuma configuração ou exclusão foi alterada para produzir esses resultados.

### Diferença entre ambientes

As falhas de `AccessDeniedException` do Maven e a indisponibilidade da pipe Docker registradas nas seções anteriores pertencem às execuções realizadas no ambiente Codex. Elas não contradizem os resultados desta execução posterior no PowerShell do Windows, na qual o Docker/Testcontainers e o Maven funcionaram corretamente. A evidência atual usa os relatórios produzidos pela execução bem-sucedida do usuário e mantém as limitações históricas sem reclassificá-las como falhas dos testes aprovados.

Não foi registrado novo ciclo formal Red -> Green -> Refactor nesta execução; os resultados comprovam validação, não três ciclos TDD completos.

## Fortalecimento PostgreSQL com observabilidade de lock - execução Codex - 06/11/2026

Esta seção registra uma execução nova e independente das anteriores. Os testes foram executados sem alterar código de produção, frontend, CI ou branch.

### Alterações verificadas

`RefreshTokenPostgreSqlConcurrencyTests` passou a controlar a sobreposição das transações com uma barreira e um gate SQL de teste acionado após a atualização que adquire o lock da linha. Enquanto a primeira transação permanece no gate, a segunda é observada por uma consulta a `pg_stat_activity` com `pg_blocking_pids()`. A asserção exige PID bloqueado, PID bloqueador distinto, evento de espera `Lock` e consulta relacionada a `refresh_token`/`FOR UPDATE`.

O teste PostgreSQL também verifica explicitamente que há exatamente uma rotação aceita e uma rejeitada, um único sucessor, `replaced_by_id` apontando para registro persistido, mesma conta e família, hash persistido igual ao hash do token retornado e estado de revogação coerente com a revogação da família. O teste de rollback PostgreSQL consulta o banco após falha de persistência em contexto independente e confirma hash, família, `revoked_at`, `replaced_by_id` e ausência de sucessor.

### Execuções e resultados

```text
mvn.cmd --batch-mode "-Dmaven.repo.local=C:\Users\Arklok\AppData\Local\Temp\corretora-maven-repo" "-Dtest=RefreshTokenConcurrencyTests,RefreshTokenPostgreSqlConcurrencyTests,RefreshTokenTests,RefreshTokenReuseTransactionRegressionTests" test
Resultado: BUILD SUCCESS; 17 testes, 0 falhas, 0 erros, 0 ignorados.

mvn.cmd --batch-mode "-Dmaven.repo.local=C:\Users\Arklok\AppData\Local\Temp\corretora-maven-repo" clean verify
Resultado: BUILD SUCCESS; 372 testes, 0 falhas, 0 erros, 7 ignorados.
```

Relatórios JUnit da execução `clean verify`:

```text
RefreshTokenConcurrencyTests: 1 aprovado em H2.
RefreshTokenPostgreSqlConcurrencyTests: 2 aprovados no PostgreSQL 16 via Testcontainers (concorrência com lock observado e rollback).
RefreshTokenTests: 11 aprovados.
RefreshTokenReuseTransactionRegressionTests: 3 aprovados.
PostgreSqlMigrationTests: 3 aprovados; Liquibase v11 aplicado no PostgreSQL.
SchemaMigrationTests: 15 aprovados.
```

Os 7 testes ignorados permanecem os testes opt-in de PostgreSQL local: 2 em `LocalPostgreSqlAccountRegistrationTests`, 3 em `LocalPostgreSqlAuthenticationTests`, 1 em `LocalPostgreSqlSessionTests` e 1 em `LocalPostgreSqlPurchaseTests`. Eles dependem de `RUN_LOCAL_POSTGRES_TESTS=true`, `LOCAL_POSTGRES_TEST_USER`, `LOCAL_POSTGRES_TEST_PASSWORD` e, opcionalmente, `LOCAL_POSTGRES_TEST_URL`; não foram tratados como aprovados. O teste PostgreSQL com Testcontainers não foi ignorado nesta execução.

### JaCoCo da mesma execução

Os contadores agregados de `backend/target/site/jacoco/jacoco.xml`, gerados pelo mesmo `clean verify`, foram:

| Métrica | Cobertos | Total | Percentual |
|---|---:|---:|---:|
| Instruções | 13.927 | 14.680 | 94,87% |
| Branches | 906 | 1.130 | 80,18% |
| Linhas | 2.681 | 2.833 | 94,63% |
| Métodos | 718 | 759 | 94,60% |
| Classes | 192 | 194 | 98,97% |

Todos os gates foram atendidos. O `jacoco.csv` produzido na mesma execução soma por classe 2.687/2.839 linhas (94,65%). A diferença de seis registros em relação ao XML resulta da agregação de linhas únicas por arquivo/pacote no XML versus soma de contadores por classe no CSV, especialmente em arquivos com classes internas ou compartilhadas. Não houve alteração de exclusões ou limites.

### Ambiente

No ambiente Codex, `docker info` não conseguiu acessar a pipe `npipe:////./pipe/docker_engine` e houve acesso negado ao arquivo de configuração Docker. Apesar dessa limitação direta, o `clean verify` executado com o repositório Maven temporário conseguiu iniciar o PostgreSQL 16 via Testcontainers; portanto, a validação PostgreSQL desta seção é baseada nos relatórios JUnit efetivamente produzidos, não nos testes ignorados do PostgreSQL local.

Não foi registrado novo ciclo formal Red -> Green -> Refactor nesta execução. Os resultados demonstram fortalecimento e validação dos testes, mas não três ciclos TDD completos.

## Correção dos achados finais - rollback real e correlação de locks - 06/11/2026

Esta seção registra somente a execução posterior das correções dos achados da revisão independente. As execuções históricas de 370, 371 e 372 testes permanecem nas seções anteriores e não foram misturadas com os resultados abaixo.

### Rollback PostgreSQL real

`RefreshTokenPostgreSqlConcurrencyTests` mantém o teste anterior com Mockito para o comportamento específico de falha no `flush`, mas adiciona um cenário sem Mockito para a falha de banco. O teste persiste um refresh token válido, adiciona temporariamente no PostgreSQL a constraint `test_refresh_rotation_rejects_successor CHECK (replaced_by_id IS NULL)` e executa `RefreshTokenService.rotate` real. A rotação tenta persistir o sucessor e o PostgreSQL rejeita a atualização.

O erro observado foi SQLSTATE `23514`, com a constraint `test_refresh_rotation_rejects_successor`. Após o rollback, consultas JDBC independentes confirmaram um único token, `revoked_at IS NULL`, `replaced_by_id IS NULL`, hash e família originais e nenhum sucessor. A constraint é removida em `finally` e também no `tearDown`; ela não pertence ao Liquibase nem ao schema de produção.

### Correlação real da concorrência

O teste usa uma conexão PostgreSQL de controle que mantém um advisory lock transacional e registra seu PID. O trigger temporário é acionado somente pela atualização `replaced_by_id` da rotação real e tenta adquirir esse lock. Assim, a primeira operação real do `RefreshTokenService` mantém o lock pessimista da linha e aguarda a conexão de controle; a segunda operação real aguarda a primeira.

A consulta a `pg_stat_activity` e `pg_blocking_pids()` exige duas relações distintas: a primeira rotação bloqueada pelo PID da conexão de controle, com query `pg_advisory_xact_lock` e estado `idle in transaction`; e a segunda rotação bloqueada pelo PID da primeira, com query relacionada a `refresh_token`. Ambas precisam estar ativas, aguardando evento `Lock`, com `wait_event` não vazio e PIDs distintos. A liberação do advisory lock somente ocorre depois dessa cadeia ser observada. Mensagens de falha incluem as observações completas.

### Execuções desta correção

```text
$taskMavenRepo = Join-Path $env:TEMP 'corretora-maven-repo'; mvn.cmd --batch-mode "-Dmaven.repo.local=$taskMavenRepo" "-Dtest=RefreshTokenPostgreSqlConcurrencyTests" test
Primeira tentativa: BUILD FAILURE; a implementação do teste identificou corretamente o PID de controle, mas a consulta posterior de pg_backend_pid() substituiu a query observável do advisory lock.

Após reordenar as consultas de preparação da conexão de controle:
Resultado: BUILD SUCCESS; 3 testes, 0 falhas, 0 erros, 0 ignorados.

$taskMavenRepo = Join-Path $env:TEMP 'corretora-maven-repo'; mvn.cmd --batch-mode "-Dmaven.repo.local=$taskMavenRepo" "-Dtest=RefreshTokenConcurrencyTests,RefreshTokenPostgreSqlConcurrencyTests,RefreshTokenTests,RefreshTokenReuseTransactionRegressionTests,PostgreSqlMigrationTests,SchemaMigrationTests" test
Resultado: BUILD SUCCESS; 36 testes, 0 falhas, 0 erros, 0 ignorados.

$taskMavenRepo = Join-Path $env:TEMP 'corretora-maven-repo'; mvn.cmd --batch-mode "-Dmaven.repo.local=$taskMavenRepo" clean verify
Resultado: BUILD SUCCESS; 373 testes, 0 falhas, 0 erros, 7 ignorados.
```

Relatórios JUnit da execução completa:

```text
RefreshTokenConcurrencyTests: 1 aprovado em H2.
RefreshTokenPostgreSqlConcurrencyTests: 3 aprovados no PostgreSQL 16/Testcontainers: concorrência, rollback por constraint real e cenário anterior de flush.
RefreshTokenTests: 11 aprovados.
RefreshTokenReuseTransactionRegressionTests: 3 aprovados.
PostgreSqlMigrationTests: 3 aprovados; Liquibase v11 aplicado no PostgreSQL.
SchemaMigrationTests: 15 aprovados.
```

Os 7 testes ignorados continuam sendo os testes opt-in de PostgreSQL local e não foram tratados como aprovados. O container PostgreSQL 16 não foi ignorado; o log confirmou PostgreSQL 16.15 e conexão Docker/Testcontainers ativa.

### JaCoCo da execução completa

Os contadores agregados de `backend/target/site/jacoco/jacoco.xml` foram:

| Métrica | Cobertos | Total | Percentual |
|---|---:|---:|---:|
| Instruções | 13.927 | 14.680 | 94,87% |
| Branches | 906 | 1.130 | 80,18% |
| Linhas | 2.681 | 2.833 | 94,63% |
| Métodos | 718 | 759 | 94,60% |
| Classes | 192 | 194 | 98,97% |

O `jacoco.csv` da mesma execução soma 2.687/2.839 linhas por classe, 94,65%. A diferença de seis registros continua sendo a diferença entre linhas únicas agregadas por arquivo/pacote no XML e a soma por classe no CSV. Os gates de 80% foram preservados sem exclusões artificiais.

Não foi registrado novo ciclo formal Red -> Green -> Refactor; esta execução comprova correção e validação dos testes, não três ciclos TDD completos.

## Correção final da limpeza de recursos PostgreSQL - 06/11/2026

Esta seção registra a correção pontual do gerenciamento de recursos em
`RefreshTokenPostgreSqlConcurrencyTests`. As execuções históricas de 370, 371,
372 e 373 testes permanecem nas seções anteriores; os resultados abaixo não
substituem nem misturam essas execuções.

### Alterações validadas

- O `tearDown` executa liberação do gate, encerramento do executor, remoção da
  conta, remoção da constraint e remoção do gate em etapas independentes.
- Falhas secundárias de limpeza são agregadas com `addSuppressed`; uma falha em
  `existsById` ou `deleteById` não impede as remoções posteriores.
- `commit`, tentativa de `rollback` e `close` da conexão de controle são
  tratados independentemente, preservando a primeira falha e anexando as
  seguintes.
- O teste concorrente cancela tarefas pendentes e encerra o executor mesmo se
  a liberação do gate falhar. O teste de constraint real preserva sua exceção
  primária e anexa eventual falha de `DROP CONSTRAINT`.
- A limpeza PostgreSQL continua idempotente com `DROP IF EXISTS`; não foram
  criadas tabelas auxiliares permanentes nem alterado o Liquibase de produção.

### Execuções efetivas

```text
$taskMavenRepo = Join-Path $env:TEMP 'corretora-maven-repo'; mvn.cmd --batch-mode "-Dmaven.repo.local=$taskMavenRepo" "-Dtest=RefreshTokenPostgreSqlConcurrencyTests,RefreshTokenConcurrencyTests,RefreshTokenTests,RefreshTokenReuseTransactionRegressionTests" test
Resultado: BUILD SUCCESS; 18 testes, 0 falhas, 0 erros, 3 ignorados.
Os 3 ignorados foram os testes PostgreSQL/Testcontainers neste primeiro processo,
porque o ambiente sem elevação não conseguiu acessar \\.\pipe\docker_engine
(AccessDeniedException). O teste H2 e os testes de refresh executados passaram.

$taskMavenRepo = Join-Path $env:TEMP 'corretora-maven-repo'; mvn.cmd --batch-mode "-Dmaven.repo.local=$taskMavenRepo" clean verify
Resultado: BUILD FAILURE antes dos testes; o compilador não conseguiu ler
jackson-annotations-2.18.1.jar no repositório Maven temporário anterior.

$verifyMavenRepo = Join-Path $env:TEMP 'corretora-maven-repo-verify-20261106'; mvn.cmd --batch-mode "-Dmaven.repo.local=$verifyMavenRepo" clean verify
Resultado: BUILD SUCCESS; 373 testes, 0 falhas, 0 erros, 7 ignorados.
RefreshTokenConcurrencyTests: 1 aprovado em H2.
RefreshTokenPostgreSqlConcurrencyTests: 3 aprovados no PostgreSQL 16/Testcontainers.
RefreshTokenTests: 11 aprovados.
RefreshTokenReuseTransactionRegressionTests: 3 aprovados.
PostgreSqlMigrationTests: 3 aprovados; Liquibase v11 aplicado no PostgreSQL.
```

Os 7 testes ignorados na execução completa continuam sendo os testes opt-in de
PostgreSQL local: 2 em `LocalPostgreSqlAccountRegistrationTests`, 3 em
`LocalPostgreSqlAuthenticationTests`, 1 em `LocalPostgreSqlSessionTests` e 1 em
`LocalPostgreSqlPurchaseTests`. Eles dependem de
`RUN_LOCAL_POSTGRES_TESTS=true`, `LOCAL_POSTGRES_TEST_USER`,
`LOCAL_POSTGRES_TEST_PASSWORD` e, opcionalmente, `LOCAL_POSTGRES_TEST_URL`; não
foram tratados como aprovados. Os testes PostgreSQL 16 via Testcontainers não
foram ignorados na execução final.

### JaCoCo da execução final

Os contadores de `backend/target/site/jacoco/jacoco.xml`, gerados pelo mesmo
`clean verify` bem-sucedido, foram:

| Métrica | Cobertos | Total | Percentual |
|---|---:|---:|---:|
| Instruções | 13.927 | 14.680 | 94,87% |
| Branches | 906 | 1.130 | 80,18% |
| Linhas | 2.681 | 2.833 | 94,63% |
| Métodos | 718 | 759 | 94,60% |
| Classes | 192 | 194 | 98,97% |

Todos os gates foram atendidos. Nenhum limite ou exclusão de cobertura foi
alterado. Não foi registrado novo ciclo formal Red -> Green -> Refactor; esta
execução valida a correção da limpeza, mas não demonstra três ciclos TDD
completos.

### Revalidacao apos o tratamento de Throwable no cleanup

Depois de incluir tambem falhas de timeout do executor (AssertionError) no
agregador de limpeza, os testes foram executados novamente com
Docker/Testcontainers acessivel:

~~~text
$verifyMavenRepo = Join-Path $env:TEMP 'corretora-maven-repo-verify-20261106'; mvn.cmd --batch-mode "-Dmaven.repo.local=$verifyMavenRepo" "-Dtest=RefreshTokenPostgreSqlConcurrencyTests,RefreshTokenConcurrencyTests,RefreshTokenTests,RefreshTokenReuseTransactionRegressionTests" test
Resultado: BUILD SUCCESS; 18 testes, 0 falhas, 0 erros, 0 ignorados.

$verifyMavenRepo = Join-Path $env:TEMP 'corretora-maven-repo-verify-20261106'; mvn.cmd --batch-mode "-Dmaven.repo.local=$verifyMavenRepo" clean verify
Resultado: BUILD SUCCESS; 373 testes, 0 falhas, 0 erros, 7 ignorados.
~~~

Na mesma execucao final, RefreshTokenConcurrencyTests teve 1 aprovacao em H2,
RefreshTokenPostgreSqlConcurrencyTests teve 3 aprovacoes no PostgreSQL
16/Testcontainers, RefreshTokenTests teve 11 aprovacoes e
RefreshTokenReuseTransactionRegressionTests teve 3 aprovacoes. Os 7 testes
ignorados continuam sendo somente os testes opt-in de PostgreSQL local listados
acima; nao foram tratados como aprovados.

O JaCoCo gerado por esse ultimo clean verify permaneceu em 13.927/14.680
instrucoes (94,87%), 906/1.130 branches (80,18%), 2.681/2.833 linhas
(94,63%), 718/759 metodos (94,60%) e 192/194 classes (98,97%). Todos os gates
foram atendidos.
