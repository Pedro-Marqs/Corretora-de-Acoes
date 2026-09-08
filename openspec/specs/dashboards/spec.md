# Dashboards Specification

## Purpose

Apresentar uma visao consolidada de saldo, posicoes, rentabilidade e patrimonio do investidor.

## Requirements

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

### Requirement: Estado dos dados

O sistema SHALL indicar carregamento, erro, ausencia de dados e cotacoes desatualizadas sem ocultar inconsistencias.

#### Scenario: Fonte externa indisponivel

- **WHEN** dados de mercado nao puderem ser atualizados
- **THEN** o dashboard SHALL indicar a desatualizacao e preservar o ultimo estado valido

### Requirement: Consolidar valores internacionais

O sistema SHALL converter o valor de mercado e o custo de ativos norte-americanos para BRL usando a cotacao USD/BRL aplicavel, sem taxa cambial, antes de consolidar patrimonio, valorizacao e distribuicoes.

#### Scenario: Posicao USD no dashboard

- **WHEN** uma posicao valer USD 20,00 e a cotacao USD/BRL for R$ 5,00
- **THEN** seu valor consolidado SHALL ser R$ 100,00 e o dashboard SHALL retornar a cotacao usada

### Requirement: Exibir resultado sem aporte

O sistema SHALL calcular lucro, prejuizo e valorizacao somente a partir das operacoes e da variacao das posicoes, excluindo saldo inicial e aportes.

#### Scenario: Aporte sem variacao de investimento

- **WHEN** uma conta sem variacao de precos receber um aporte
- **THEN** saldo e patrimonio SHALL aumentar pelo aporte, enquanto resultado realizado, valorizacao nao realizada e resultado total SHALL permanecer inalterados

### Requirement: Consolidar indicadores internacionais

O sistema SHALL converter para BRL o valor de mercado e o custo de ativos norte-americanos usando a cotacao USD/BRL aplicavel, sem taxa cambial, antes de consolidar patrimonio, valorizacao e resultados. A resposta SHALL retornar a cotacao USD/BRL e o instante utilizados quando houver posicao internacional.

#### Scenario: Posicao USD no dashboard

- **WHEN** uma posicao valer USD 20,00 e a cotacao USD/BRL for R$ 5,00
- **THEN** seu valor consolidado SHALL ser R$ 100,00 e o dashboard SHALL retornar a cotacao usada

#### Scenario: Cambio indisponivel sem cache

- **WHEN** uma conta possuir ativo norte-americano e nao existir cotacao USD/BRL utilizavel
- **THEN** o sistema SHALL rejeitar a consulta com erro funcional padronizado, sem inventar conversao ou valor consolidado

### Requirement: Excluir aportes dos resultados

O sistema SHALL calcular lucro, prejuizo e valorizacao somente a partir das operacoes e da variacao das posicoes, excluindo saldo inicial e aportes.

#### Scenario: Aporte sem variacao de investimento

- **WHEN** uma conta sem variacao de precos receber um aporte
- **THEN** saldo e patrimonio SHALL aumentar pelo aporte, enquanto resultado realizado, valorizacao nao realizada e resultado total SHALL permanecer inalterados

### Requirement: Proteger consistencia e isolamento dos indicadores

O sistema SHALL retornar somente posicoes, movimentacoes, cotacoes e saldo pertencentes à conta autenticada, SHALL preservar o ultimo valor valido quando uma cotacao estiver desatualizada e SHALL indicar essa desatualizacao na resposta sem alterar dados financeiros.

#### Scenario: Cotacao antiga

- **WHEN** o dashboard usar uma cotacao de mercado ou USD/BRL acima do limite de atualizacao
- **THEN** o sistema SHALL retornar os indicadores calculados com o ultimo valor valido, o instante desse valor e um indicador de desatualizacao

#### Scenario: Isolamento entre contas

- **WHEN** duas contas autenticadas consultarem seus dashboards
- **THEN** cada resposta SHALL conter somente os indicadores e posicoes da propria conta

### Requirement: Consultar serie historica patrimonial

O sistema SHALL permitir que o investidor autenticado consulte no dashboard a serie historica dos pontos patrimoniais da propria conta para um periodo selecionado entre `4W`, `3M`, `6M`, `1Y`, `5Y` e `MAX`. A resposta SHALL retornar somente pontos patrimoniais efetivamente gravados, em ordem cronologica, com instante no horario de Brasilia e valor patrimonial em BRL. O limite inicial dos periodos relativos SHALL ser calculado a partir do instante atual do sistema; o periodo `MAX` SHALL usar a criacao da conta como limite inicial. Quando nao houver ponto cobrindo o inicio do periodo, a resposta SHALL iniciar no primeiro ponto disponivel posterior ao limite. Atualizacoes isoladas de cotacao SHALL nao criar pontos para a serie.

#### Scenario: Serie para periodo selecionado

- **WHEN** o investidor autenticado consultar o dashboard com um dos periodos suportados
- **THEN** o sistema SHALL retornar somente pontos da propria conta dentro do periodo, ordenados do mais antigo para o mais recente, sem criar valores intermediarios

#### Scenario: Periodo maximo

- **WHEN** o investidor selecionar `MAX`
- **THEN** o sistema SHALL considerar como limite inicial a data de criacao da conta e retornar os pontos patrimoniais disponiveis desde esse limite

#### Scenario: Cobertura parcial

- **WHEN** o periodo selecionado iniciar antes do primeiro ponto patrimonial disponivel
- **THEN** o sistema SHALL iniciar a serie no primeiro ponto disponivel posterior ao limite, sem inventar um ponto para preencher a lacuna

#### Scenario: Conta sem pontos no periodo

- **WHEN** nao existir ponto patrimonial da conta dentro do periodo selecionado
- **THEN** o sistema SHALL retornar uma serie vazia, sem valores sinteticos ou alteracao persistida

#### Scenario: Atualizacao de cotacao sem novo ponto

- **WHEN** ocorrer somente uma atualizacao de cotacao ou cambio sem movimentacao financeira
- **THEN** a serie historica SHALL permanecer sem um ponto correspondente a essa atualizacao

#### Scenario: Periodo invalido ou ausencia de sessao

- **WHEN** o investidor informar periodo diferente dos seis valores suportados ou consultar sem sessao autenticada valida
- **THEN** o sistema SHALL rejeitar a requisicao com o contrato uniforme de validacao ou autenticacao e SHALL NOT revelar pontos patrimoniais

#### Scenario: Isolamento da serie

- **WHEN** duas contas autenticadas consultarem o mesmo periodo
- **THEN** cada resposta SHALL conter somente pontos patrimoniais pertencentes à conta autenticada
