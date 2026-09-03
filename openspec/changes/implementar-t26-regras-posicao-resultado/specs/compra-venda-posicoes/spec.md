## MODIFIED Requirements

### Requirement: Calcular posicao

O sistema SHALL manter quantidade, custo total, preco medio unitario e valor de mercado da posicao com precisao decimal e historico auditavel. Ao comprar unidades de uma posicao existente, SHALL calcular o novo preco medio pela media ponderada entre o custo anterior e o custo da compra. Ao vender, SHALL reduzir o custo pelo preco medio vigente e preservar esse preco medio na quantidade restante.

#### Scenario: Atualizar preco medio ponderado

- **WHEN** uma posicao de 10 unidades a R$ 20,00 receber compra de 10 unidades a R$ 30,00
- **THEN** a posicao SHALL conter 20 unidades, custo total de R$ 500,00 e preco medio de R$ 25,00, sem usar float ou double

#### Scenario: Atualizar preco medio

- **WHEN** uma compra for efetivada para um ativo ja mantido
- **THEN** o sistema SHALL recalcular o preco medio pela media ponderada sem usar float ou double

#### Scenario: Venda parcial preserva media

- **WHEN** uma posicao de 20 unidades a R$ 25,00 vender 5 unidades a R$ 30,00
- **THEN** SHALL restar 15 unidades a preco medio de R$ 25,00 e o custo restante SHALL ser R$ 375,00

#### Scenario: Venda total encerra posicao

- **WHEN** a quantidade vendida for igual a quantidade da posicao
- **THEN** a quantidade, custo total e valor de mercado da posicao aberta SHALL ser zero e a posicao nao SHALL ser apresentada como aberta

#### Scenario: Recompra apos zeragem

- **WHEN** um ativo cuja posicao foi zerada for comprado novamente
- **THEN** o novo preco medio SHALL usar somente o custo da recompra, preservando a posicao anterior no historico

## ADDED Requirements

### Requirement: Calcular resultado de compra e venda

O sistema SHALL calcular o resultado realizado de uma venda como (preco unitario de venda menos preco medio unitario vigente) multiplicado pela quantidade vendida, sem taxas, impostos ou custos adicionais.

#### Scenario: Resultado realizado positivo

- **WHEN** 5 unidades com preco medio de R$ 25,00 forem vendidas a R$ 30,00
- **THEN** o resultado realizado SHALL aumentar em R$ 25,00

#### Scenario: Resultado realizado negativo

- **WHEN** unidades forem vendidas abaixo do preco medio vigente
- **THEN** o sistema SHALL registrar resultado realizado negativo pela mesma formula, sem alterar artificialmente o custo restante

### Requirement: Converter operacao internacional

O sistema SHALL converter valores de ativos norte-americanos para reais multiplicando o valor em USD pela cotacao USD/BRL fornecida, sem taxa cambial, e SHALL arredondar valores financeiros para duas casas com `HALF_UP`.

#### Scenario: Compra internacional convertida

- **WHEN** um ativo cotado a USD 10,00 for operado em quantidade 2 com cotacao USD/BRL de R$ 5,00
- **THEN** o valor financeiro em reais SHALL ser R$ 100,00
