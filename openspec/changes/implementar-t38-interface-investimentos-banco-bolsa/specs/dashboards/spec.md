## ADDED Requirements

### Requirement: Apresentar visão de investimentos

A seção Investimentos SHALL apresentar, nesta ordem, o total de patrimônio, o saldo em conta, uma distribuição visual entre ações nacionais, ações internacionais e saldo em conta, e a carteira disponível. O total SHALL usar o patrimônio oficial retornado pelo dashboard, em que ações e dinheiro em conta compõem a visão consolidada.

#### Scenario: Investimentos com posições
- **WHEN** o dashboard autenticado retornar saldo e posições
- **THEN** a interface SHALL exibir os blocos na ordem definida, formatar valores em reais com duas casas e SHALL NOT recalcular o patrimônio no navegador

#### Scenario: Investimentos sem posições
- **WHEN** a conta possuir saldo mas nenhuma posição aberta
- **THEN** a interface SHALL exibir patrimônio e saldo oficiais, categorias de ações vazias ou zeradas e um estado vazio compreensível para a carteira

### Requirement: Abrir carteira por categoria de mercado

A distribuição de Investimentos SHALL permitir selecionar `Ações nacionais` ou `Ações internacionais`. A seleção SHALL abrir uma lista com todas as posições abertas da categoria escolhida, contendo os dados retornados pelo backend e uma ação `Investir` para cada ativo.

#### Scenario: Selecionar ações nacionais
- **WHEN** o investidor selecionar a categoria de ações nacionais
- **THEN** a interface SHALL listar somente as posições nacionais abertas e SHALL oferecer `Investir` em cada item

#### Scenario: Selecionar ações internacionais
- **WHEN** o investidor selecionar a categoria de ações internacionais
- **THEN** a interface SHALL listar somente as posições internacionais abertas, preservando valores em USD/BRL e avisos de câmbio quando fornecidos

#### Scenario: Categoria sem posições
- **WHEN** a categoria selecionada não possuir posições abertas
- **THEN** a interface SHALL exibir estado vazio sem inventar ativos ou oferecer uma operação preenchida
