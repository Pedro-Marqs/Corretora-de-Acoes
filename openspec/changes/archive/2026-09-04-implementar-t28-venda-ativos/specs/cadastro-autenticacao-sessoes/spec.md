## MODIFIED Requirements

### Requirement: Autenticacao por sessao
O sistema SHALL autenticar credenciais válidas usando sessão associada a conta ativa. Operações de compra ou venda SHALL exigir uma sessão autenticada associada à própria conta ativa e SHALL derivar a conta dessa sessão, sem aceitar identificador de conta fornecido pelo cliente como autoridade.

#### Scenario: Venda sem sessão
- **WHEN** um solicitante sem sessão autenticada enviar uma venda
- **THEN** o sistema SHALL rejeitar a solicitação como não autenticada e SHALL não alterar dados financeiros

#### Scenario: Venda de outra conta
- **WHEN** uma requisição de venda tentar indicar conta diferente daquela associada à sessão
- **THEN** o sistema SHALL ignorar a indicação ou rejeitar a requisição sem acessar ou alterar a outra conta

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
