# Compra, venda e posicoes Specification

## Purpose

Registrar operacoes de compra e venda e manter posicoes financeiras consistentes.

## Requirements

### Requirement: Executar operacao
O sistema SHALL validar saldo, quantidade inteira positiva, ativo ativo, corretora ativa pertencente à conta autenticada e cotação utilizável antes de registrar uma compra ou venda. Na compra, o preço unitário e o valor financeiro SHALL ser determinados exclusivamente pelo backend a partir da cotação aplicável; qualquer preço informado pelo cliente SHALL ser ignorado e SHALL NOT influenciar o resultado.

#### Scenario: Compra valida
- **WHEN** o investidor autenticado enviar uma compra com saldo suficiente, quantidade válida, ativo operacional, corretora própria ativa e cotação utilizável
- **THEN** o sistema SHALL registrar a movimentação, debitar o saldo e criar ou atualizar a posição pela regra financeira vigente, sem usar preço do cliente

#### Scenario: Compra internacional válida
- **WHEN** o investidor comprar ativo norte-americano com cotação em USD e USD/BRL utilizável
- **THEN** o sistema SHALL calcular o débito em reais multiplicando o valor em USD pela cotação USD/BRL e arredondando para duas casas com `HALF_UP`

#### Scenario: Venda valida
- **WHEN** o investidor vender quantidade disponivel
- **THEN** o sistema SHALL registrar a movimentacao, creditar o saldo e reduzir a posicao

### Requirement: Rejeitar operacao inconsistente
O sistema SHALL rejeitar quantidade, preco, ativo, corretora ou saldo invalidos sem alterar o estado financeiro. A compra SHALL ser rejeitada quando não houver cotação utilizável ou, para ativo norte-americano, USD/BRL utilizável; dados de preço enviados pelo cliente SHALL ser desconsiderados, não validados como fonte financeira e não persistidos.

#### Scenario: Saldo ou posicao insuficiente
- **WHEN** uma compra nao tiver saldo ou uma venda nao tiver quantidade suficiente
- **THEN** o sistema SHALL rejeitar a operacao sem movimentacao parcial

#### Scenario: Cotação ausente ou câmbio ausente
- **WHEN** uma compra depender de cotação ou USD/BRL e não existir valor utilizável
- **THEN** o sistema SHALL rejeitar a operação sem debitar saldo, alterar posição ou registrar histórico/patrimônio

#### Scenario: Preço manipulado pelo cliente
- **WHEN** o cliente enviar preço diferente da cotação determinada pelo backend
- **THEN** o sistema SHALL concluir a compra com o valor do backend, sem persistir ou usar o preço enviado

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
