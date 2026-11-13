# autenticacao-jwt Specification

## Purpose
Fornecer autenticação JWT segura para investidores, com expiração, renovação e revogação compatíveis com a experiência atual sem armazenar tokens no localStorage.

## Requirements

### Requirement: Emissão e validação de JWT de investidor

O sistema SHALL emitir JWT assinado com issuer, audience de investidor, `principal_type=INVESTOR`, papel funcional `USER`, subject da conta, `iat`, `exp` e `jti`. O token SHALL conter somente identificadores e authorities necessárias, SHALL possuir expiração configurável e SHALL ser rejeitado quando a assinatura, issuer, audience, principal type, formato ou validade temporal forem inválidos. Ele SHALL ser distinto de qualquer JWT administrativo previsto para `AdminUser`.

#### Scenario: Token válido

- **WHEN** uma requisição de investidor apresentar `Authorization: Bearer` com JWT válido
- **THEN** o sistema SHALL autenticar a conta ativa correspondente como `USER` e permitir somente as operações autorizadas para essa conta

#### Scenario: Token ausente

- **WHEN** uma rota privada receber uma requisição sem Bearer token e sem sessão legada permitida
- **THEN** o sistema SHALL responder `401` sem executar a operação

#### Scenario: Token adulterado

- **WHEN** o payload ou a assinatura de um JWT for alterado
- **THEN** o sistema SHALL responder `401` sem acessar ou modificar dados financeiros

#### Scenario: Token expirado

- **WHEN** um JWT ultrapassar sua expiração
- **THEN** o sistema SHALL responder `401` e SHALL exigir renovação válida ou novo login

### Requirement: Renovação e revogação de credenciais

O sistema SHALL usar refresh tokens opacos, rotativos, armazenados somente em forma hash e associados à conta, à família de renovação, à expiração e ao estado de revogação. Logout, alteração de credencial, inativação da conta e detecção de reutilização SHALL revogar os refresh tokens aplicáveis.

#### Scenario: Renovação válida

- **WHEN** o cliente enviar refresh token válido em cookie seguro e não revogado
- **THEN** o sistema SHALL emitir novo access token, rotacionar o refresh token e invalidar o anterior

#### Scenario: Reutilização de refresh token

- **WHEN** um refresh token já rotacionado ou revogado for reapresentado
- **THEN** o sistema SHALL rejeitar a renovação e revogar a família correspondente

#### Scenario: Logout

- **WHEN** o investidor autenticado solicitar logout
- **THEN** o sistema SHALL revogar o refresh token aplicável, limpar o cookie e impedir novas renovações

### Requirement: Transporte seguro no navegador

O frontend SHALL manter o access token somente em memória. O refresh token SHALL ser enviado em cookie `HttpOnly`, `Secure` fora de ambientes locais, `SameSite` adequado e com escopo restrito. O sistema SHALL rejeitar o uso de localStorage para access ou refresh tokens.

#### Scenario: Recarregamento da aplicação

- **WHEN** o navegador recarregar a aplicação com refresh cookie válido
- **THEN** o frontend SHALL renovar a autenticação sem persistir o access token em localStorage ou sessionStorage

#### Scenario: Falha de renovação

- **WHEN** a renovação retornar `401`
- **THEN** o frontend SHALL limpar o access token em memória e conduzir o usuário ao login sem expor detalhes técnicos

### Requirement: CSRF, CORS e erros de autenticação

Requisições que dependam de cookies SHALL manter proteção CSRF. O CORS SHALL permitir somente as origens configuradas, com `Authorization` e credenciais apenas quando necessários. Falha de autenticação SHALL retornar `401` e falta de autorização SHALL retornar `403`, sem token, senha, hash ou stack trace na resposta ou nos logs.

#### Scenario: Mutação sem CSRF

- **WHEN** login, renovação ou logout com cookie for enviado sem CSRF válido
- **THEN** o sistema SHALL rejeitar a mutação sem alterar estado de autenticação

#### Scenario: Origem não permitida

- **WHEN** uma origem não configurada fizer preflight ou requisição autenticada
- **THEN** o sistema SHALL rejeitar a origem sem expor credenciais
