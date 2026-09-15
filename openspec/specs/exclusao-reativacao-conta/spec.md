# Exclusao e reativacao de conta Specification

## Purpose

Permitir a desativacao e a reativacao controladas de contas, preservando o historico necessario.

## Requirements

### Requirement: Excluir conta

O sistema SHALL permitir que o titular autenticado desative a propria conta com confirmacao e encerre suas sessoes.

#### Scenario: Exclusao confirmada

- **WHEN** o titular confirmar a exclusao da conta
- **THEN** o sistema SHALL marcar a conta como inativa e invalidar suas sessoes

#### Scenario: Exclusao sem confirmacao

- **WHEN** a confirmacao exigida nao for fornecida
- **THEN** o sistema SHALL manter a conta ativa

### Requirement: Reativar conta

O sistema SHALL permitir reativar uma conta elegivel usando o fluxo de autenticacao definido.

#### Scenario: Reativacao valida

- **WHEN** o titular comprovar sua identidade e solicitar reativacao
- **THEN** o sistema SHALL marcar a conta como ativa sem duplicar saldo ou historico
