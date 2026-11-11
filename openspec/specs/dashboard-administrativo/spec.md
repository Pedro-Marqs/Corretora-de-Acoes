# dashboard-administrativo Specification

## Purpose

Disponibilizar visão operacional agregada da corretora para administradores autorizados, preservando privacidade, coerência monetária, paginação e filtros temporais.

## Requirements

### Requirement: Métricas agregadas de contas e negociações

O dashboard administrativo SHALL apresentar, somente ao `ADMIN`, indicadores agregados de estado atual, novas contas no período solicitado, atividade de negociações no período solicitado e volume financeiro separado por moeda. `totalAccounts`, `activeAccounts` e `inactiveAccounts` SHALL ser fotografias atuais das contas `ACTIVE` e `INACTIVE`, independentes do período e excluindo `DELETED`. `newAccounts` SHALL contar contas `ACTIVE` ou `INACTIVE` criadas dentro do período solicitado. `purchaseCount`, `saleCount`, `purchasedShares` e `soldShares` SHALL permanecer limitadas ao período solicitado.

#### Scenario: Consulta agregada válida

- **WHEN** um administrador `ADMIN` consultar período válido
- **THEN** o sistema SHALL retornar os oito grupos de métricas agregadas sem identificadores de investidores ou posições individuais

#### Scenario: Período inválido

- **WHEN** o período estiver ausente, invertido, fora do limite permitido ou em formato inválido
- **THEN** o sistema SHALL responder erro de validação sem executar consulta parcial

### Requirement: Valores monetários e conversão

Volumes em BRL e USD SHALL ser mantidos separados. Quando houver conversão para um total equivalente em BRL, o sistema SHALL informar a cotação e o instante utilizados; ausência de cotação válida SHALL impedir ou sinalizar o equivalente sem inventar valor.

#### Scenario: Operações em moedas diferentes

- **WHEN** o período contiver operações BRL e USD
- **THEN** o sistema SHALL retornar totais por moeda e SHALL não somá-los diretamente

#### Scenario: Câmbio indisponível

- **WHEN** uma conversão necessária não possuir câmbio válido
- **THEN** o sistema SHALL retornar estado de dependência indisponível ou equivalente ausente, sem alterar dados financeiros

### Requirement: Séries temporais e ranking

O dashboard SHALL retornar evolução temporal de cadastros e negociações e ranking de ativos mais negociados com ordenação determinística, limite explícito e filtros aplicados ao mesmo período.

#### Scenario: Ranking determinístico

- **WHEN** dois ativos tiverem a mesma quantidade negociada
- **THEN** o sistema SHALL aplicar critério secundário estável e SHALL respeitar o limite solicitado

### Requirement: Isolamento do dashboard administrativo

O dashboard SHALL aceitar somente JWT administrativo com audience correta e SHALL impedir acesso a carteiras, históricos, saldos, posições, CPF completo, e-mail completo ou qualquer projeção individual de investidor.

#### Scenario: Investidor acessa dashboard administrativo

- **WHEN** um JWT válido de investidor acessar o dashboard administrativo
- **THEN** o sistema SHALL responder `403` ou `401` conforme a falha de audience/autorização e SHALL não retornar métricas protegidas

#### Scenario: Consulta não altera domínio financeiro

- **WHEN** um administrador `ADMIN` consultar qualquer métrica, ranking ou série temporal
- **THEN** o sistema SHALL não criar, alterar ou excluir saldo, posição, movimentação ou ponto patrimonial

### Requirement: Contrato completo do dashboard administrativo

O sistema SHALL expor `GET /api/admin/dashboard?period=4W|3M|6M|1Y|5Y|MAX&limit=1..50`, exigindo `ADMIN`. `period` SHALL ser obrigatório, `limit` SHALL ter default 10 e o cálculo SHALL usar `America/Sao_Paulo`. Para `MAX`, o intervalo SHALL ir do primeiro dado relevante disponível até o fim do período atual, com agregação diária e consultas limitadas ao necessário. Ausência ou formato inválido SHALL responder `400`; ausência de autenticação SHALL responder `401`; identidade sem `ADMIN` SHALL responder `403`.

O retorno SHALL conter exatamente estas oito métricas escalares: `totalAccounts`, `activeAccounts`, `inactiveAccounts`, `newAccounts`, `purchaseCount`, `saleCount`, `purchasedShares` e `soldShares`. Também SHALL conter volumes nativos de compra e venda separados por `BRL` e `USD`, sem somá-los diretamente, séries temporais diárias e ranking limitado de ativos.

#### Scenario: Métricas de estado atual e atividade no período

- **WHEN** um `ADMIN` consultar período válido
- **THEN** o sistema SHALL calcular `totalAccounts`, `activeAccounts` e `inactiveAccounts` como o estado atual de contas `ACTIVE` e `INACTIVE`, independentemente do período
- **AND** o sistema SHALL calcular `newAccounts` somente para contas `ACTIVE` ou `INACTIVE` criadas dentro do período definido
- **AND** o sistema SHALL calcular compras, vendas, ações compradas e ações vendidas somente dentro do período definido
- **AND** o sistema SHALL preservar `totalAccounts = activeAccounts + inactiveAccounts`, sem exigir que `newAccounts` seja igual ao total atual

#### Scenario: Volumes em moedas diferentes

- **WHEN** o período possuir operações BRL e USD
- **THEN** o sistema SHALL retornar volumes nativos separados por moeda e SHALL não somá-los como se fossem uma única moeda

#### Scenario: Equivalente em BRL

- **WHEN** houver volume USD e uma taxa `USD/BRL` válida
- **THEN** o sistema SHALL informar o equivalente em BRL juntamente com a taxa e o instante usados

#### Scenario: Câmbio indisponível

- **WHEN** uma taxa válida não estiver disponível ou estiver fora da validade
- **THEN** o sistema SHALL manter os volumes nativos, retornar equivalente nulo com estado `UNAVAILABLE` e SHALL não inventar valor

### Requirement: Séries e ranking determinísticos

As séries SHALL ser diárias e conter somente data, novas contas, compras e vendas, sem identificadores de investidores. O ranking SHALL retornar no máximo `limit` ativos, ordenar por quantidade negociada decrescente e desempatar por ticker e depois mercado.

#### Scenario: Empate no ranking

- **WHEN** dois ativos tiverem a mesma quantidade negociada
- **THEN** o sistema SHALL aplicar ticker e mercado como critérios secundários estáveis e SHALL respeitar o limite solicitado

#### Scenario: Dados vazios

- **WHEN** o período válido não possuir contas ou negociações
- **THEN** o sistema SHALL retornar as oito métricas zeradas, séries vazias e ranking vazio sem erro interno

### Requirement: Privacidade do dashboard

O dashboard SHALL retornar somente projeções agregadas. Não SHALL retornar saldo, patrimônio, posição, carteira, histórico, CPF completo, e-mail completo, identificador de investidor ou corretora individual.

#### Scenario: Ausência de dados individuais

- **WHEN** um `ADMIN` consultar métricas, séries ou ranking
- **THEN** a resposta SHALL conter somente agregados e SHALL preservar os dados financeiros individuais dos investidores


