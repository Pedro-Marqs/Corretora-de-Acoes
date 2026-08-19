# Ativos, cotacoes e cambio Specification

## Purpose

Disponibilizar ativos, cotacoes e cambio para suportar consultas e operacoes de investimento.

## Requirements

### Requirement: Catalogo de ativos

O sistema SHALL manter ativos brasileiros e norte-americanos com identificacao, tipo, moeda e status.

#### Scenario: Consultar ativo ativo

- **WHEN** o investidor consultar um ativo cadastrado e ativo
- **THEN** o sistema SHALL retornar seus dados publicos

### Requirement: Cotacoes e cambio

O sistema SHALL registrar cotacoes e cambio com fonte, instante, moeda e indicacao de desatualizacao.

#### Scenario: Cotacao indisponivel ou desatualizada

- **WHEN** a fonte externa falhar ou a cotacao exceder sua validade
- **THEN** o sistema SHALL informar o estado desatualizado sem inventar um valor

### Requirement: Precisao financeira

O sistema SHALL usar valores decimais e conversao USD/BRL com arredondamento HALF_UP em duas casas.

#### Scenario: Conversao monetaria

- **WHEN** um valor em USD for convertido para BRL
- **THEN** o sistema SHALL aplicar a cotacao de cambio e arredondar o resultado em duas casas
