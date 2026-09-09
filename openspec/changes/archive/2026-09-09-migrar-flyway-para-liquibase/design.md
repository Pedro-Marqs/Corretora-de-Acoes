## Context

Conforme `proposal.md`, o backend possui oito migrations Flyway em `db/migration`, com uma V2 específica para PostgreSQL e outra para H2. O PostgreSQL local é o banco de validação principal, H2 continua sendo usado nos testes rápidos, e `ddl-auto=validate` já impede que o ORM seja responsável por criar o esquema.

## Goals / Non-Goals

**Goals:**

- Introduzir um único ponto de entrada Liquibase, com histórico e lock persistidos no banco.
- Reproduzir o esquema atual sem alterar o modelo relacional ou as regras de unicidade específicas de cada banco.
- Permitir validação automatizada em H2 e validação real em PostgreSQL antes de remover a referência Flyway.

**Non-Goals:**

- Não remodelar entidades, endpoints, dados financeiros ou tabelas de negócio.
- Não criar uma migração de dados de produção diferente da aplicação ordenada das migrations já existentes.
- Não usar Docker, recriar banco existente ou alterar credenciais locais.

## Decisions

- **Changelog mestre com includes explícitos:** `db.changelog-master.xml` incluirá arquivos em `changes/` em ordem V1, V2, V3 até V8. Isso torna a completude auditável e evita depender da ordenação incidental de nomes.
- **Um changeSet por migration lógica:** cada migration Flyway será representada por changeSet XML identificável e irrepetível; SQL de alteração, backfill e constraints será preservado quando necessário dentro do XML, em vez de reinterpretar expressões complexas e mudar sua semântica.
- **V2 por banco via `dbms`:** os SQLs PostgreSQL e H2 serão changeSets distintos, condicionados ao banco correspondente. A alternativa de um único SQL foi rejeitada porque os índices parciais/expressões do PostgreSQL e as colunas geradas do H2 não são equivalentes sintaticamente.
- **Liquibase como configuração explícita do Spring:** será usado `spring.liquibase.enabled=true` e o caminho absoluto de classpath solicitado; as propriedades Flyway e suas dependências serão removidas para impedir execução concorrente ou ambígua.
- **Remoção em último passo:** a pasta Flyway permanecerá até a comparação de todos os arquivos e a validação dos testes e do PostgreSQL. Em caso de falha, o rollback do trabalho é manter a referência e corrigir o changelog, não apagar ou recriar o banco.

## Risks / Trade-offs

- [Diferenças de sintaxe entre PostgreSQL e H2] → manter changeSets condicionais para V2 e executar a suíte Maven antes da remoção da referência.
- [ChangeSet aplicado parcialmente em banco local] → inspecionar `DATABASECHANGELOGLOCK`, corrigir a causa e repetir em banco de validação preservado, sem marcar manualmente o changeSet como concluído.
- [Alteração acidental da semântica por converter SQL complexo para tags genéricas] → comparar cada changeSet com o SQL original e preservar operações de backfill, constraints e índices explicitamente.
- [Banco existente com histórico Flyway] → a implementação deve validar o cenário esperado da etapa em banco vazio/isolado; não deve apagar tabelas ou tentar uma conversão destrutiva de histórico sem decisão adicional.

## Migration Plan

1. Adicionar Liquibase, configurar o master changelog e criar os changeSets mantendo `db/migration` intacto.
2. Executar `cd backend && .\mvnw.cmd test` e corrigir incompatibilidades de H2.
3. Iniciar o backend com PostgreSQL local preservado, confirmar aplicação do esquema e consultar `DATABASECHANGELOG` e `DATABASECHANGELOGLOCK`.
4. Repetir a inicialização para comprovar idempotência e lock liberado; somente então remover `db/migration/`.
5. Se a validação falhar antes da remoção, manter os arquivos de referência e reverter apenas a configuração/changelog incompletos. Não recriar nem apagar o banco principal.
