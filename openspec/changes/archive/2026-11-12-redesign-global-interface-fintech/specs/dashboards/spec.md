## MODIFIED Requirements

### Requirement: Consultar serie historica patrimonial

O sistema SHALL permitir que o investidor autenticado consulte no dashboard a serie historica dos pontos patrimoniais da propria conta para um periodo selecionado entre `4W`, `3M`, `6M`, `1Y`, `5Y` e `MAX`. A resposta SHALL retornar somente pontos patrimoniais efetivamente gravados, em ordem cronologica, com instante no horario de Brasilia e valor patrimonial em BRL. Para os periodos relativos `4W`, `3M`, `6M`, `1Y` e `5Y`, o limite inicial SHALL ser calculado a partir do instante atual do sistema e, quando existir, a resposta SHALL incluir como primeiro ponto o ultimo ponto patrimonial efetivamente persistido antes ou no limite inicial, preservando seu instante e valor oficiais, seguido dos demais pontos persistidos da janela em ordem cronologica. Quando nao houver ponto anterior ou coincidente, a resposta SHALL iniciar no primeiro ponto persistido dentro da janela. O periodo `MAX` SHALL usar a criacao da conta como limite inicial e somente os pontos persistidos desde entao. Atualizacoes isoladas de cotacao SHALL nao criar pontos para a serie.

#### Scenario: Serie para periodo selecionado

- **WHEN** o investidor autenticado consultar o dashboard com um dos periodos suportados
- **THEN** o sistema SHALL retornar somente pontos da propria conta, ordenados do mais antigo para o mais recente, sem criar valores intermediarios; nos periodos relativos, SHALL incluir o ultimo ponto persistido antes ou no limite inicial quando existir, seguido dos pontos persistidos dentro da janela, ou iniciar no primeiro ponto da janela quando nao houver ponto anterior ou coincidente

#### Scenario: Periodo maximo

- **WHEN** o investidor selecionar `MAX`
- **THEN** o sistema SHALL considerar como limite inicial a data de criacao da conta e retornar os pontos patrimoniais disponiveis desde esse limite

#### Scenario: Cobertura parcial

- **WHEN** o periodo selecionado iniciar antes do primeiro ponto patrimonial disponivel
- **THEN** o sistema SHALL iniciar a serie no ultimo ponto persistido anterior ou coincidente ao limite quando existir; caso contrario, SHALL iniciar no primeiro ponto persistido dentro da janela, sem inventar um ponto para preencher a lacuna

#### Scenario: Periodo relativo com patrimonio anterior

- **WHEN** o investidor consultar um periodo relativo e existir um ponto patrimonial persistido antes ou no inicio da janela
- **THEN** `patrimonyHistory` SHALL iniciar com esse ponto oficial e SHALL continuar com os pontos persistidos posteriores ate o fim do periodo, em ordem cronologica

#### Scenario: Periodo relativo sem patrimonio anterior

- **WHEN** nao existir ponto persistido ate o inicio da janela, mas existirem pontos dentro dela
- **THEN** `patrimonyHistory` SHALL iniciar no primeiro ponto persistido da janela, sem criar uma ancora artificial

#### Scenario: Conta sem pontos no periodo

- **WHEN** nao existir ponto patrimonial da conta dentro do periodo selecionado e nao existir ponto anterior ou coincidente aplicavel
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
