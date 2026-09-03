# Historico e registro patrimonial Specification

## Purpose

Manter historico imutavel de movimentacoes e pontos patrimoniais para consulta e auditoria.

## Requirements

### Requirement: Historico de movimentacoes
O sistema SHALL registrar operacoes, aportes, transferencias e ajustes com conta, instante, valores e origem. Uma compra concluída SHALL registrar ao menos o tipo da operação, conta, ativo, corretora, quantidade, preço determinado pelo backend, valor financeiro e moeda/conversão aplicável. Atualizacoes automaticas de cotacoes ou cambio, sem operacao financeira, SHALL NOT criar movimentacoes.

#### Scenario: Registrar compra concluída
- **WHEN** uma compra for efetivada
- **THEN** o sistema SHALL persistir uma movimentação imutável vinculada à conta autenticada, contendo os dados financeiros efetivamente usados

#### Scenario: Compra rejeitada
- **WHEN** uma compra for rejeitada antes da conclusão
- **THEN** o sistema SHALL não criar movimentação

#### Scenario: Consultar historico proprio
- **WHEN** o investidor autenticado consultar seu historico
- **THEN** o sistema SHALL retornar registros ordenados e pertencentes somente a sua conta

#### Scenario: Atualizacao de mercado sem movimentacao
- **WHEN** um ciclo automatico atualizar cotacoes ou cambio
- **THEN** o sistema SHALL manter o historico de movimentacoes inalterado

### Requirement: Registro patrimonial
O sistema SHALL registrar pontos patrimoniais consistentes com saldo e posicoes em instante controlado. Uma compra concluída SHALL gerar o ponto com o saldo debitado, as posições resultantes, o câmbio utilizado quando aplicável e o valor total calculado no mesmo processamento. Atualizacoes isoladas de cotacao ou cambio SHALL NOT gerar ponto patrimonial.

#### Scenario: Gerar ponto patrimonial após compra
- **WHEN** ocorrer o processamento bem-sucedido de uma compra
- **THEN** o sistema SHALL persistir um ponto patrimonial consistente com o novo saldo e as posições resultantes

#### Scenario: Gerar ponto patrimonial
- **WHEN** ocorrer o processamento de um ponto patrimonial
- **THEN** o sistema SHALL persistir saldo, posicoes, cambio e valor total usados no calculo

#### Scenario: Compra com falha de registro
- **WHEN** falhar o registro da movimentação ou do ponto patrimonial durante uma compra
- **THEN** o sistema SHALL reverter também o débito e a alteração da posição

#### Scenario: Atualizacao de mercado sem ponto patrimonial
- **WHEN** um ciclo automatico atualizar somente cotacoes ou cambio
- **THEN** o sistema SHALL deixar inalterados os pontos patrimoniais persistidos

### Requirement: Imutabilidade
O sistema SHALL impedir alteracao silenciosa de registros historicos ja persistidos.

#### Scenario: Tentativa de alterar historico
- **WHEN** uma operacao tentar modificar um registro historico
- **THEN** o sistema SHALL rejeitar a alteracao ou criar um novo evento auditavel
