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

## TDD

Foi registrado um ciclo real para o defeito transacional de reutilização:

1. **Red:** o teste sem `@Transactional` compartilhado demonstrou HTTP 204 para B após a reutilização de A.
2. **Green:** uma exceção específica de família revogada foi configurada com `noRollbackFor` restrito; a revogação passou a ser confirmada antes do 401 e os 45 testes focados passaram.
3. **Refactor:** a exceção específica foi mantida separada da exceção genérica de autenticação, com `noRollbackFor` restrito àquele tipo; foram adicionados cenários de reutilização repetida e isolamento entre famílias/contas, e a suíte final passou.

O ciclo acima é específico deste defeito e não representa, sozinho, os três ciclos formais exigidos para concluir a Aula 15.

## Segurança e dados sensíveis

Os testes geram a chave de teste em memória e não registram o valor de tokens. O código de produção persiste apenas o hash do refresh token e não registra tokens completos ou segredos. Os valores usados nos testes não são credenciais de ambiente nem dados pessoais reais.
