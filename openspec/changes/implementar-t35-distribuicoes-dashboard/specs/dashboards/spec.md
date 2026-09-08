## MODIFIED Requirements

### Requirement: Consultar dashboard

O sistema SHALL disponibilizar `GET /api/dashboard` para o investidor autenticado e SHALL retornar dados consolidados da propria conta, com valores monetarios em duas casas e datas no horario de Brasilia. A resposta SHALL conter saldo, posicoes, patrimonio, preco medio, resultado realizado, valorizacao nao realizada e resultado total calculados pelas mesmas regras financeiras das operacoes. O patrimonio SHALL ser o saldo em reais acrescido do valor de mercado das posicoes; a valorizacao nao realizada SHALL ser o valor de mercado das posicoes menos seu custo total; o resultado total SHALL ser a soma do resultado realizado e da valorizacao nao realizada. Aportes e saldo inicial SHALL ficar fora desses resultados. A resposta SHALL identificar cotacoes ou cambio antigos quando utilizados, incluindo o instante e o fator USD/BRL aplicados. A consulta SHALL aceitar opcionalmente o identificador opaco de uma corretora propria ativa para restringir posicoes, resultados e distribuicoes àquela corretora; sem esse filtro, SHALL considerar todas as corretoras da conta. O saldo SHALL permanecer identificado como compartilhado pela conta, inclusive na visao filtrada.

#### Scenario: Dashboard com dados

- **WHEN** o investidor autenticado consultar `GET /api/dashboard`
- **THEN** o sistema SHALL retornar saldo, posicoes, preco medio, patrimonio, resultado realizado, valorizacao nao realizada e resultado total calculados pelas mesmas regras financeiras

#### Scenario: Dashboard sem movimentacoes

- **WHEN** a conta ainda nao possuir operacoes
- **THEN** o sistema SHALL retornar estado vazio com saldo inicial correto e resultados de investimento iguais a zero

#### Scenario: Patrimonio com posicoes

- **WHEN** o saldo for R$ 1.000,00 e as posicoes forem avaliadas em R$ 2.500,00
- **THEN** o patrimonio SHALL ser R$ 3.500,00

#### Scenario: Dashboard sem autenticacao

- **WHEN** uma requisicao para `GET /api/dashboard` nao possuir sessao autenticada valida
- **THEN** o sistema SHALL rejeitar a consulta com o contrato uniforme de autenticacao e SHALL NOT revelar indicadores, posicoes ou metadados da conta

#### Scenario: Dashboard filtrado por corretora propria

- **WHEN** o investidor autenticado consultar `GET /api/dashboard` informando uma corretora propria ativa
- **THEN** o sistema SHALL retornar somente as posicoes, resultados e distribuicoes daquela corretora e SHALL manter o saldo como compartilhado pela conta

#### Scenario: Corretora de outra conta

- **WHEN** o investidor informar uma corretora que nao pertença à sua conta ou nao esteja ativa
- **THEN** o sistema SHALL rejeitar a consulta com erro funcional padronizado e SHALL NOT revelar dados da corretora ou de suas posicoes

## ADDED Requirements

### Requirement: Retornar distribuicoes da carteira

O sistema SHALL retornar no dashboard as distribuicoes agregadas por ativo, corretora e mercado, com cada parcela identificada pela dimensao correspondente e com valor numerico em BRL. As agregacoes SHALL respeitar a visao geral ou a corretora selecionada, SHALL incluir somente posicoes abertas da conta autenticada e SHALL ser reproduziveis a partir dos valores de mercado consolidados das posicoes, incluindo conversao USD/BRL quando aplicavel.

#### Scenario: Distribuicoes por dimensao

- **WHEN** o investidor possuir posicoes em dois ativos, duas corretoras ou dois mercados
- **THEN** o dashboard SHALL retornar parcelas separadas por ativo, corretora e mercado, cada uma com seu valor em BRL

#### Scenario: Soma das parcelas

- **WHEN** o dashboard retornar distribuicoes de uma visao
- **THEN** a soma das parcelas de cada dimensao SHALL corresponder ao valor de mercado consolidado das posicoes daquela mesma visao, respeitando o arredondamento financeiro

#### Scenario: Distribuicao sem posicoes

- **WHEN** a conta ou a corretora selecionada nao possuir posicoes abertas
- **THEN** o sistema SHALL retornar distribuicoes vazias e indicadores de investimento zerados, sem criar valores artificiais

#### Scenario: Isolamento das distribuicoes

- **WHEN** duas contas autenticadas consultarem o dashboard ou uma conta consultar corretoras diferentes
- **THEN** cada resposta SHALL conter somente parcelas derivadas das posicoes pertencentes à conta e à visao solicitada
