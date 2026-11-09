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

Durante a migração, o sistema SHALL aceitar a sessão legada somente quando o modo de compatibilidade estiver explicitamente habilitado. No estado final, o sistema SHALL autenticar investidores por JWT Bearer associado a uma conta ativa e SHALL derivar a conta exclusivamente da identidade autenticada, sem aceitar identificador de conta fornecido pelo cliente como autoridade.

#### Scenario: Venda sem sessão

- **WHEN** um solicitante sem sessão legada compatível ou JWT válido enviar uma venda
- **THEN** o sistema SHALL rejeitar a solicitação como não autenticada e SHALL não alterar dados financeiros

#### Scenario: Venda de outra conta

- **WHEN** uma requisição de venda tentar indicar conta diferente daquela associada à identidade autenticada
- **THEN** o sistema SHALL ignorar a indicação ou rejeitar a requisição sem acessar ou alterar a outra conta

#### Scenario: Login valido

- **WHEN** uma conta ativa receber e-mail e senha corretos
- **THEN** o sistema SHALL emitir um JWT de investidor e um mecanismo seguro de renovação sem expor a senha ou seu hash

#### Scenario: Compra sem sessao

- **WHEN** um solicitante sem sessão legada compatível ou JWT válido enviar uma compra
- **THEN** o sistema SHALL rejeitar a solicitação como não autenticada e SHALL não alterar dados financeiros

#### Scenario: Compra de outra conta

- **WHEN** uma requisição tentar indicar conta diferente daquela associada ao JWT
- **THEN** o sistema SHALL ignorar a indicação ou rejeitar a requisição sem acessar ou alterar a outra conta

#### Scenario: Credenciais invalidas

- **WHEN** e-mail, senha ou estado da conta impedirem o login
- **THEN** o sistema SHALL não emitir access token nem refresh token e SHALL não revelar qual credencial falhou

#### Scenario: Migração de sessão compatível

- **WHEN** o modo de migração estiver habilitado e uma sessão JDBC válida acessar uma rota de investidor
- **THEN** o sistema SHALL preservar o comportamento funcional da rota, SHALL impedir acesso a contas diferentes e SHALL permitir a troca controlada para JWT

### Requirement: Alteracao e isolamento de credenciais

O sistema SHALL exigir a senha atual para alterar e-mail ou senha, invalidar todas as sessoes e limitar o acesso a propria conta.

#### Scenario: Alterar credencial

- **WHEN** um investidor autenticado confirmar a senha atual e alterar e-mail ou senha
- **THEN** o sistema SHALL persistir a alteracao e invalidar todas as sessoes da conta

#### Scenario: Isolamento de conta

- **WHEN** o investidor tentar acessar registro de outra conta
- **THEN** o sistema SHALL negar o acesso sem expor os dados
