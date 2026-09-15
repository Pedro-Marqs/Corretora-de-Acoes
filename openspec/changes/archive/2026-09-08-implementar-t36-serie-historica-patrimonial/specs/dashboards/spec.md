## ADDED Requirements

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
