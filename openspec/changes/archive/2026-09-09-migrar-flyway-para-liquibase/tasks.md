## 1. Configuração do mecanismo

- [x] 1.1 Substituir as dependências Flyway por `liquibase-core` e configurar `spring.liquibase.enabled=true` com o changelog mestre solicitado, removendo `spring.flyway.locations` e preservando `spring.jpa.hibernate.ddl-auto=validate`; verificar a árvore de dependências e as propriedades efetivas.
- [x] 1.2 Criar `db/changelog/db.changelog-master.xml` e a estrutura `db/changelog/changes/`, mantendo `db/migration/` intacto como referência; verificar que o master possui includes explícitos e ordenados para todos os changeSets.

## 2. Conversão dos changeSets

- [x] 2.1 Converter a migration inicial e a migration V2 específica de PostgreSQL em changeSets XML, incluindo tabelas de negócio, tabelas Spring Session, constraints e índices; comparar o SQL original com o changelog e validar em banco PostgreSQL vazio.
- [x] 2.2 Converter as migrations V3 a V8 em changeSets XML na mesma ordem, preservando backfills, defaults, constraints, índices e alterações financeiras; comparar cada arquivo de referência e validar a aplicação completa em H2.
- [x] 2.3 Configurar a variante H2 da V2 como changeSet condicionado ao banco correto, sem executar a variante PostgreSQL no H2; comprovar as unicidades ativas e normalizadas com teste ou validação de schema em H2.

## 3. Validação e limpeza

- [x] 3.1 Executar `cd backend && .\mvnw.cmd test` e corrigir qualquer incompatibilidade da conversão até a suíte passar sem usar Flyway; verificar também que `ddl-auto=validate` continua ativo.
- [x] 3.2 Iniciar o backend com PostgreSQL local preservado e confirmar que o schema é aplicado, `DATABASECHANGELOG` contém todos os changeSets, `DATABASECHANGELOGLOCK` está liberado e uma segunda inicialização é idempotente; registrar falhas sem recriar ou apagar o banco principal.
- [x] 3.3 Somente após as validações anteriores, remover `backend/src/main/resources/db/migration/` e executar validação final de completude (`openspec validate --strict`, testes Maven e `git diff --check`), confirmando que o master Liquibase permanece o único ponto de entrada.
