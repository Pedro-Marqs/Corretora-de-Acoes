## MODIFIED Requirements

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
