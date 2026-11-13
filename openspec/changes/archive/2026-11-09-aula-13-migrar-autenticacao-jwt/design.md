## Context

O projeto usa Spring Session JDBC, cookie `SESSION`, CSRF, CORS restrito e BCrypt. `AccountPrincipal` contém apenas o UUID da conta e os controllers financeiros já derivam a conta autenticada do principal. A migração deve alterar o transporte sem alterar a autoridade por propriedade nem as transações de compra, venda, saldo, posição, histórico e patrimônio.

## Goals / Non-Goals

**Goals:**

- Adotar JWT Bearer para investidores com validação completa de issuer, audience, assinatura e tempo.
- Emitir o principal investidor como `INVESTOR`/`USER`, mantendo-o incompatível com a audience administrativa do change da Aula 14.
- Preservar BCrypt, `AccountPrincipal`, isolamento horizontal e o contrato seguro de erros.
- Preservar a experiência de recarregamento por meio de access token em memória e refresh token em cookie HttpOnly.
- Permitir rollout gradual e revogação explícita sem depender de logout apenas no cliente.

**Non-Goals:**

- Não alterar regras financeiras, cálculos, concorrência ou persistência de operações.
- Não colocar JWT em localStorage, sessionStorage ou payload com CPF, e-mail, senha, hash ou saldo.
- Não implementar autenticação administrativa neste change; ela depende do contrato JWT e fica no change da Aula 14.
- Não alterar `.github/workflows/`, branches ou introduzir gateway externo.

## Decisions

- **Access token em memória e refresh token HttpOnly:** evita exposição persistente ao JavaScript e mantém restauração após recarregamento. Alternativa rejeitada: localStorage, por aumentar impacto de XSS. Alternativa de access token apenas em cookie foi rejeitada porque não atende ao fluxo Bearer explícito.
- **Refresh token opaco persistido por hash:** o access token permanece stateless durante seu TTL, enquanto logout, rotação e revogação continuam controláveis. A tabela terá hash, conta, família, expiração, revogado-em e referência de substituição; nenhum token bruto será persistido.
- **Chaves e configuração por ambiente:** secrets, issuer, audience e tempos serão fornecidos por configuração externa, com tamanho mínimo e chaves separadas por ambiente. O JWT não carregará informações financeiras.
- **Fronteira entre identidades:** o token investidor terá `principal_type=INVESTOR` e papel `USER`; o token administrativo será definido no change da Aula 14 com `principal_type=ADMIN`, papel `ADMIN` e audience própria. Nenhum token será aceito na área oposta.
- **Migração em duas fases:** primeiro, backend com compatibilidade explicitamente configurável para sessões legadas e frontend migrado para Bearer; depois, desativação da sessão após validação. Não apagar tabelas nem sessões durante a primeira fase.
- **CSRF somente onde cookie participa:** rotas que usam refresh cookie mantêm CSRF; rotas protegidas exclusivamente por Authorization Bearer não usam cookie de autenticação, mas continuam obedecendo CORS e o contrato de erro.
- **Integração com o cliente HTTP comum:** interceptor adicionará Bearer em memória, tentará uma única renovação em caso de `401`, evitará loops e limpará o estado em falha. O backend continuará sendo autoridade sobre identidade e autorização.

## Risks / Trade-offs

- [Tokens antigos continuarem aceitos após alteração de credencial] → access tokens terão TTL curto e a revogação impedirá novas renovações; a invalidação imediata de access tokens será tratada pela política de expiração.
- [Rollout aceitar acidentalmente sessão fora da janela] → compatibilidade ficará atrás de configuração explícita, coberta por teste e removida na etapa de corte.
- [Rotação de refresh gerar corridas] → usar atualização transacional e estado revogado/substituído com restrição de uso único.
- [Mudança de cookie e CORS quebrar frontend] → manter contratos de origem, testar preflight, CSRF, renovação, logout e recarregamento antes de desativar sessão.
- [Alteração do principal afetar operações financeiras] → preservar `AccountPrincipal.accountId()` e executar regressões de compra, venda, transferência, dashboard e histórico.

## Migration Plan

1. Criar configuração externa e armazenamento de refresh tokens sem remover Spring Session.
2. Implementar emissão/validação JWT e testes isolados de token.
3. Habilitar aceitação dual somente em ambiente de migração, mantendo sessão como fallback controlado.
4. Migrar frontend para access token em memória, refresh cookie, CSRF e renovação única.
5. Executar testes focados e regressões financeiras; validar Graphify após o código ser alterado.
6. Publicar a fase de corte somente após revisão, tester e aprovação explícita; então desabilitar sessão legada e planejar remoção posterior das tabelas.
