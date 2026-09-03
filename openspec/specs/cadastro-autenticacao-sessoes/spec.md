# Cadastro, autenticacao e sessoes Specification

## Purpose

Permitir a criacao de contas individuais e o acesso seguro as funcionalidades autenticadas.

## Requirements

### Requirement: Cadastro de conta

O sistema SHALL validar nome, CPF, e-mail e senha, criar uma conta ativa e registrar saldo inicial de R$ 10.000,00 uma unica vez.

#### Scenario: Cadastro valido

- **WHEN** um investidor nao autenticado informar CPF e e-mail nao usados e senha valida
- **THEN** o sistema SHALL criar a conta ativa, o saldo inicial e seu registro

#### Scenario: Cadastro invalido

- **WHEN** CPF, e-mail ou senha forem invalidos ou ja estiverem em uso por conta ativa
- **THEN** o sistema SHALL rejeitar o cadastro e indicar os campos invalidos

### Requirement: Autenticacao por sessao
O sistema SHALL autenticar credenciais validas usando sessao associada a conta ativa. Operações de compra SHALL exigir uma sessão autenticada associada à própria conta ativa e SHALL derivar a conta dessa sessão, sem aceitar identificador de conta fornecido pelo cliente como autoridade.

#### Scenario: Login valido
- **WHEN** uma conta ativa receber e-mail e senha corretos
- **THEN** o sistema SHALL criar uma sessao associada a conta

#### Scenario: Compra sem sessao
- **WHEN** um solicitante sem sessão autenticada enviar uma compra
- **THEN** o sistema SHALL rejeitar a solicitação como não autenticada e SHALL não alterar dados financeiros

#### Scenario: Compra de outra conta
- **WHEN** uma requisição tentar indicar conta diferente daquela associada à sessão
- **THEN** o sistema SHALL ignorar a indicação ou rejeitar a requisição sem acessar ou alterar a outra conta

#### Scenario: Credenciais invalidas
- **WHEN** e-mail, senha ou estado da conta impedirem o login
- **THEN** o sistema SHALL nao criar sessao nem revelar qual credencial falhou

### Requirement: Alteracao e isolamento de credenciais

O sistema SHALL exigir a senha atual para alterar e-mail ou senha, invalidar todas as sessoes e limitar o acesso a propria conta.

#### Scenario: Alterar credencial

- **WHEN** um investidor autenticado confirmar a senha atual e alterar e-mail ou senha
- **THEN** o sistema SHALL persistir a alteracao e invalidar todas as sessoes da conta

#### Scenario: Isolamento de conta

- **WHEN** o investidor tentar acessar registro de outra conta
- **THEN** o sistema SHALL negar o acesso sem expor os dados
