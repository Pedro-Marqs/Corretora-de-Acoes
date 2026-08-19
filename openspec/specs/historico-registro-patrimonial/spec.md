# Historico e registro patrimonial Specification

## Purpose

Manter historico imutavel de movimentacoes e pontos patrimoniais para consulta e auditoria.

## Requirements

### Requirement: Historico de movimentacoes

O sistema SHALL registrar operacoes, aportes, transferencias e ajustes com conta, instante, valores e origem.

#### Scenario: Consultar historico proprio

- **WHEN** o investidor autenticado consultar seu historico
- **THEN** o sistema SHALL retornar registros ordenados e pertencentes somente a sua conta

### Requirement: Registro patrimonial

O sistema SHALL registrar pontos patrimoniais consistentes com saldo e posicoes em instante controlado.

#### Scenario: Gerar ponto patrimonial

- **WHEN** ocorrer o processamento de um ponto patrimonial
- **THEN** o sistema SHALL persistir saldo, posicoes, cambio e valor total usados no calculo

### Requirement: Imutabilidade

O sistema SHALL impedir alteracao silenciosa de registros historicos ja persistidos.

#### Scenario: Tentativa de alterar historico

- **WHEN** uma operacao tentar modificar um registro historico
- **THEN** o sistema SHALL rejeitar a alteracao ou criar um novo evento auditavel
