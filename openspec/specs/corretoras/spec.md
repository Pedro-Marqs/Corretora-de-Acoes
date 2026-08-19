# Corretoras Specification

## Purpose

Permitir cadastrar e associar corretoras as contas de investidores.

## Requirements

### Requirement: Cadastro de corretora

O sistema SHALL manter nome, identificacao e status de corretoras sem duplicar registros ativos.

#### Scenario: Criar corretora

- **WHEN** uma corretora valida for cadastrada
- **THEN** o sistema SHALL persistir a corretora ativa

### Requirement: Associar corretora a conta

O sistema SHALL permitir que uma conta associe corretoras ativas e consulte somente suas associacoes.

#### Scenario: Associacao valida

- **WHEN** o investidor autenticado associar uma corretora ativa a sua conta
- **THEN** o sistema SHALL criar a associacao

#### Scenario: Associacao duplicada

- **WHEN** a conta tentar associar novamente a mesma corretora ativa
- **THEN** o sistema SHALL rejeitar a duplicidade sem criar novo registro
