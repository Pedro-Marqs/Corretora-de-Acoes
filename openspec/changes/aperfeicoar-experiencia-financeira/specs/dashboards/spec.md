## ADDED Requirements

### Requirement: Apresentar gráficos do dashboard

O dashboard SHALL apresentar gráficos legíveis para a evolução patrimonial e para as distribuições por ativo, corretora e mercado, usando exclusivamente os valores retornados pela API. Os gráficos SHALL respeitar a visão e o período selecionados, informar estados vazios e não inventar pontos.

#### Scenario: Dashboard com dados
- **WHEN** a API retornar série patrimonial ou distribuições não vazias
- **THEN** a interface SHALL renderizar os gráficos correspondentes com legendas, valores e período/dimensão identificáveis

#### Scenario: Dashboard sem dados
- **WHEN** a série ou uma distribuição estiver vazia
- **THEN** a interface SHALL exibir estado vazio específico em vez de desenhar valores artificiais

#### Scenario: Filtro alterado
- **WHEN** o investidor alterar corretora ou período
- **THEN** a interface SHALL consultar os dados oficiais novamente e SHALL atualizar os gráficos sem misturar a resposta anterior
