## Why

O projeto atende autenticação por sessão JDBC, mas a Aula 13 exige autenticação JWT com Bearer. A migração precisa preservar BCrypt, a identidade da própria conta em todas as operações financeiras, a proteção contra CSRF/CORS e a experiência de login sem expor tokens no armazenamento persistente do navegador.

## What Changes

- **BREAKING** Substituir o transporte final de autenticação de sessão JDBC por access tokens JWT Bearer para investidores.
- Introduzir emissão e validação de JWT assinado, expirável e sem dados sensíveis.
- Identificar o JWT de investidor como `principal_type=INVESTOR`, audience de investidores e papel funcional `USER`, sem permitir seu uso na área administrativa.
- Manter a conta derivada da identidade autenticada; nenhum `accountId` enviado pelo cliente poderá autorizar outra conta.
- Introduzir refresh token opaco, rotativo e revogável em cookie `HttpOnly`, com o access token mantido somente em memória no frontend.
- Definir logout, expiração, revogação por conta e migração controlada da sessão legada.
- Atualizar CORS, CSRF, contratos de erro e cliente HTTP para o fluxo Bearer.
- Cobrir token ausente, inválido, adulterado, expirado, válido, logout e revogação.
- Não alterar regras financeiras, contratos de compra/venda ou o workflow do GitHub Actions.

## Capabilities

### New Capabilities

- `autenticacao-jwt`: ciclo de vida, claims, expiração, refresh, revogação e transporte seguro dos tokens.

### Modified Capabilities

- `cadastro-autenticacao-sessoes`: substituir a autoridade final da sessão por JWT, preservando cadastro, BCrypt, isolamento por conta e migração compatível.

## Impact

Afeta a cadeia Spring Security, o serviço e controlador de autenticação, configuração de secrets e CORS/CSRF, tabelas de refresh tokens, frontend de sessão e todos os testes que atualmente criam cookies `SESSION`. Também afeta o contrato de login/logout e a restauração de autenticação após recarregar a página. A sessão JDBC será mantida apenas durante a janela de migração e removida somente após validação e decisão de corte.
