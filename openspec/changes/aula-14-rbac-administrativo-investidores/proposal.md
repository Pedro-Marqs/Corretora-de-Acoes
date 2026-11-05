## Why

O projeto possui somente a identidade financeira `Account`, adequada para investidores, mas ainda não possui uma fronteira administrativa independente. A Aula 14 exige RBAC sem transformar papel administrativo em autorização para movimentar ou consultar indiscriminadamente carteiras individuais.

## What Changes

- Criar a identidade administrativa independente `AdminUser`, sem saldo, carteira, posição ou relação financeira com `Account`.
- Criar login administrativo JWT com audience, tipo de principal e chave/configuração separados dos tokens de investidores.
- Definir somente duas identidades funcionais: investidor `USER` em `Account` e administrador `ADMIN` em `AdminUser`.
- Impedir cadastro público e definir bootstrap seguro do primeiro administrador.
- Proteger endpoints administrativos com autorização em nível de método para `ADMIN`.
- Criar consulta, ativação e desativação paginadas de investidores, sem operações financeiras administrativas.
- Criar dashboard agregado com métricas de contas, negociações, volumes, séries temporais e ranking de ativos.
- Tratar moedas sem somar valores incompatíveis e registrar conversões quando houver equivalente em BRL.
- Registrar auditoria de ativação, desativação, bootstrap, login administrativo e tentativas negadas sensíveis.
- Impedir acesso cruzado entre tokens de investidores e endpoints administrativos.

## Evolução futura

Um operador administrativo somente leitura poderá ser introduzido em change posterior, com papel e matriz de permissões próprios. Essa possibilidade não faz parte desta versão e não possui tarefas neste change.

## Capabilities

### New Capabilities

- `identidade-administrativa`: identidade, login, papéis, bootstrap, gestão limitada de investidores e auditoria.
- `dashboard-administrativo`: métricas agregadas e ranking operacional sem exposição de carteiras individuais.

### Modified Capabilities

- Nenhuma. A identidade `Account` e a autorização horizontal do investidor permanecem contratos existentes.

## Impact

Afeta persistência e migrações administrativas, configuração JWT, cadeia de segurança, method security, endpoints e serviços administrativos, consultas agregadas de movimentos e contas, frontend separado do painel e testes de isolamento. Depende do contrato JWT definido em `aula-13-migrar-autenticacao-jwt` e não altera endpoints financeiros existentes.
