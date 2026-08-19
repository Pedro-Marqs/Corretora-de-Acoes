# Saldo e aportes Specification

## Purpose

Controlar o saldo em reais e os aportes do investidor com registros financeiros precisos.

## Requirements

### Requirement: Aporte de saldo

O sistema SHALL permitir aporte valido e atualizar o saldo atomically com seu registro historico.

#### Scenario: Aporte valido

- **WHEN** o investidor autenticado informar valor de aporte positivo
- **THEN** o sistema SHALL creditar o saldo e registrar o aporte

#### Scenario: Aporte invalido

- **WHEN** o valor for ausente, zero ou negativo
- **THEN** o sistema SHALL rejeitar a solicitacao sem alterar o saldo

### Requirement: Consistencia do saldo

O sistema SHALL calcular e persistir saldo usando BigDecimal, duas casas e arredondamento HALF_UP.

#### Scenario: Saldo apos movimentacao

- **WHEN** um aporte ou operacao for confirmado
- **THEN** o saldo SHALL refletir a movimentacao integralmente sem perda de precisao
