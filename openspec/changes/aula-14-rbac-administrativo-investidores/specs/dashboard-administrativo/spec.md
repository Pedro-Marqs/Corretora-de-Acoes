## Purpose

Disponibilizar visão operacional agregada da corretora para administradores autorizados, preservando privacidade, coerência monetária, paginação e filtros temporais.

## ADDED Requirements

### Requirement: Métricas agregadas de contas e negociações

O dashboard administrativo SHALL apresentar, para o período solicitado e somente ao `ADMIN`, total de contas, contas ativas e inativas, novas contas, quantidade de compras e vendas, quantidade de ações compradas e vendidas e volume financeiro separado por moeda.

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
