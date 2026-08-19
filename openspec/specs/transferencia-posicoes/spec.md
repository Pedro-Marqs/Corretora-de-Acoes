# Transferencia de posicoes Specification

## Purpose

Permitir transferir posicoes entre corretoras associadas a mesma conta, preservando quantidade e historico.

## Requirements

### Requirement: Transferir posicao

O sistema SHALL validar origem, destino, ativo e quantidade antes de efetivar uma transferencia.

#### Scenario: Transferencia valida

- **WHEN** o investidor transferir quantidade disponivel entre corretoras validas
- **THEN** o sistema SHALL reduzir a origem, creditar o destino e registrar um evento atomico

#### Scenario: Transferencia invalida

- **WHEN** origem, destino, ativo ou quantidade forem invalidos
- **THEN** o sistema SHALL rejeitar a transferencia sem alterar as posicoes

### Requirement: Isolamento da conta

O sistema SHALL permitir transferencia apenas entre corretoras pertencentes a conta autenticada.

#### Scenario: Corretora de outra conta

- **WHEN** uma das corretoras nao pertencer a conta do investidor
- **THEN** o sistema SHALL negar a operacao sem expor dados de terceiros
