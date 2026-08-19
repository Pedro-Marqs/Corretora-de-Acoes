# Dashboards Specification

## Purpose

Apresentar uma visao consolidada de saldo, posicoes, rentabilidade e patrimonio do investidor.

## Requirements

### Requirement: Consultar dashboard

O sistema SHALL exibir dados consolidados da propria conta, com valores monetarios em duas casas e datas no horario de Brasilia.

#### Scenario: Dashboard com dados

- **WHEN** o investidor autenticado consultar o dashboard
- **THEN** o sistema SHALL retornar saldo, posicoes, patrimonio e indicadores calculados

#### Scenario: Dashboard sem movimentacoes

- **WHEN** a conta ainda nao possuir operacoes
- **THEN** o sistema SHALL exibir estado vazio com saldo inicial correto

### Requirement: Estado dos dados

O sistema SHALL indicar carregamento, erro, ausencia de dados e cotacoes desatualizadas sem ocultar inconsistencias.

#### Scenario: Fonte externa indisponivel

- **WHEN** dados de mercado nao puderem ser atualizados
- **THEN** o dashboard SHALL indicar a desatualizacao e preservar o ultimo estado valido
