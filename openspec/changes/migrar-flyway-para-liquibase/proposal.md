## Why

As migrações atuais ainda dependem do Flyway, enquanto a Etapa 2 requer a padronização do controle de esquema com Liquibase. A conversão precisa preservar o esquema já definido e permitir validar a inicialização do PostgreSQL sem perder a referência das migrations existentes durante o trabalho.

## What Changes

- Adicionar Liquibase como mecanismo de migração do backend e remover as dependências Flyway.
- Criar o changelog mestre e changeSets XML ordenados para todas as migrations atuais, incluindo as variações de banco necessárias.
- Configurar o Spring Boot para executar o changelog mestre, mantendo `ddl-auto=validate`.
- Validar testes automatizados e execução real no PostgreSQL, incluindo as tabelas de controle do Liquibase.
- Remover `db/migration/` somente após a conversão e a validação bem-sucedidas.

## Capabilities

### New Capabilities

- `database-migrations`: Aplicação ordenada e validável do esquema relacional por changelog Liquibase.

### Modified Capabilities

## Impact

- Afeta `backend/pom.xml`, propriedades do Spring e os recursos de banco em `backend/src/main/resources/db/`.
- A inicialização do backend passa a criar e consultar `DATABASECHANGELOG` e `DATABASECHANGELOGLOCK` em vez das tabelas de histórico do Flyway.
- Não altera contratos HTTP, regras financeiras, modelo lógico do esquema ou comportamento de `ddl-auto=validate`.
