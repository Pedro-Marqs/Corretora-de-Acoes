# Saldo e aportes Specification

## Purpose

Controlar o saldo em reais e os aportes do investidor com registros financeiros precisos.

## Requirements

### Requirement: Aporte de saldo

O sistema SHALL permitir aporte em reais de valor igual ou superior a R$ 10,00 para a conta autenticada e SHALL atualizar o saldo, registrar a movimentação de aporte e criar o ponto patrimonial correspondente atomicamente.

#### Scenario: Aporte valido

- **WHEN** o investidor autenticado informar aporte igual ou superior a R$ 10,00
- **THEN** o sistema SHALL creditar integralmente o valor no saldo e registrar a movimentação e o ponto patrimonial na mesma transação

#### Scenario: Aporte invalido

- **WHEN** o valor do aporte for ausente, zero, negativo ou inferior a R$ 10,00
- **THEN** o sistema SHALL rejeitar a solicitação sem alterar saldo, histórico ou patrimônio

#### Scenario: Falha durante o aporte

- **WHEN** qualquer etapa de atualização do saldo, registro histórico ou ponto patrimonial falhar
- **THEN** o sistema SHALL reverter integralmente a operação e não manter alteração parcial

### Requirement: Consistencia do saldo

O sistema SHALL calcular e persistir saldo usando BigDecimal, duas casas e arredondamento HALF_UP.

#### Scenario: Saldo apos movimentacao

- **WHEN** um aporte ou operacao for confirmado
- **THEN** o saldo SHALL refletir a movimentacao integralmente sem perda de precisao

### Requirement: Consulta do saldo próprio

O sistema SHALL permitir que o investidor autenticado consulte o saldo único em reais associado à própria conta, independentemente da existência de corretoras vinculadas.

#### Scenario: Consultar saldo

- **WHEN** o investidor autenticado consultar seu saldo
- **THEN** o sistema SHALL retornar o saldo atual da própria conta

#### Scenario: Conta recém-criada

- **WHEN** o investidor de uma conta recém-criada consultar seu saldo antes de outras movimentações
- **THEN** o sistema SHALL retornar R$ 10.000,00

#### Scenario: Isolamento entre contas

- **WHEN** um investidor realizar uma operação de saldo
- **THEN** o sistema MUST limitar a operação à conta associada à sessão autenticada

### Requirement: Aporte não representa rendimento

O sistema SHALL tratar aportes como entrada de capital do investidor e MUST NOT contabilizá-los como lucro, prejuízo ou valorização dos investimentos.

#### Scenario: Aporte sem resultado de investimentos

- **WHEN** um aporte for concluído em uma conta sem resultado de investimentos
- **THEN** saldo e patrimônio SHALL aumentar pelo valor correspondente, enquanto lucro, prejuízo e valorização SHALL permanecer inalterados
