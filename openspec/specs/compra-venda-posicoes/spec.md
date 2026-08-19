# Compra, venda e posicoes Specification

## Purpose

Registrar operacoes de compra e venda e manter posicoes financeiras consistentes.

## Requirements

### Requirement: Executar operacao

O sistema SHALL validar saldo, quantidade, ativo, corretora e cotacao antes de registrar compra ou venda.

#### Scenario: Compra valida

- **WHEN** o investidor enviar uma compra com saldo suficiente
- **THEN** o sistema SHALL registrar a movimentacao, debitar o saldo e atualizar a posicao

#### Scenario: Venda valida

- **WHEN** o investidor vender quantidade disponivel
- **THEN** o sistema SHALL registrar a movimentacao, creditar o saldo e reduzir a posicao

### Requirement: Rejeitar operacao inconsistente

O sistema SHALL rejeitar quantidade, preco, ativo, corretora ou saldo invalidos sem alterar o estado financeiro.

#### Scenario: Saldo ou posicao insuficiente

- **WHEN** uma compra nao tiver saldo ou uma venda nao tiver quantidade suficiente
- **THEN** o sistema SHALL rejeitar a operacao sem movimentacao parcial

### Requirement: Calcular posicao

O sistema SHALL manter quantidade, preco medio e valor da posicao com precisao decimal e historico auditavel.

#### Scenario: Atualizar preco medio

- **WHEN** uma compra for efetivada para um ativo ja mantido
- **THEN** o sistema SHALL recalcular o preco medio sem usar float ou double
