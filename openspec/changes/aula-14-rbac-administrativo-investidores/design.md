## Context

`Account` representa uma conta financeira e suas operações já derivam do principal autenticado, inclusive compras e vendas. O change da Aula 13 fornece JWT de investidores; esta mudança adiciona uma segunda identidade, com audience, tipo de principal e configuração próprios, sem reutilizar `Account` para representar operador administrativo.

## Goals / Non-Goals

**Goals:**

- Isolar tabelas, credenciais, principal e endpoints administrativos.
- Implementar somente o administrador `ADMIN` com consulta de indicadores e ativação/desativação controladas; o investidor `USER` permanece em `Account`.
- Entregar consultas agregadas eficientes e monetariamente coerentes.
- Garantir autorização em nível HTTP e método, além de testes de isolamento entre as áreas.
- Auditar ações administrativas sensíveis sem armazenar segredos.

**Non-Goals:**

- Não criar cadastro público, saldo, carteira, posição ou operação financeira para `AdminUser`.
- Não permitir que administrador consulte carteiras ou históricos individuais por padrão.
- Não alterar `Account`, `PurchaseService`, `SaleService`, `TransferService` ou cálculos financeiros para acomodar permissões administrativas.
- Não alterar o workflow do GitHub Actions nem introduzir serviço externo de analytics.

## Decisions

- **`AdminUser` separado de `Account`:** evita que saldo e identidade operacional compartilhem ciclo de vida, DTO ou repositório. Alternativa rejeitada: adicionar `role` em `Account`, porque isso misturaria investidor e operador e ampliaria o risco de acesso cruzado.
- **Duas identidades funcionais:** `Account` representa o investidor `USER` e `AdminUser` representa o administrador `ADMIN`. Não haverá operador administrativo somente leitura nesta versão; essa evolução será planejada separadamente. O papel nunca substitui audience nem tipo de principal.
- **Audience e chaves separadas:** tokens de investidor usam audience de investidor e tokens administrativos usam audience administrativa; configurações e secrets podem ser distintos. Um token válido em uma área não é válido na outra.
- **Autorização em duas camadas:** filtro JWT valida identidade/audience; `@PreAuthorize` ou equivalente valida papel no método; serviços mantêm regras de domínio e não confiam em campos do request.
- **Bootstrap único controlado:** um comando CLI explícito e idempotente lê o e-mail normalizado e a senha por entrada segura, cria somente se não houver `AdminUser` e não fica exposto por HTTP. `APP_ADMIN_BOOTSTRAP_PASSWORD` fica restrito à automação de provisionamento, sem exposição ou persistência indevida.
- **Dashboard por projeções agregadas:** consultas SQL/JPA retornarão contagens, somas por moeda, séries e ranking, sem carregar entidades de carteira individual. Períodos usarão o relógio de Brasília e limites explícitos.
- **Auditoria transacional:** ativação/desativação e seu evento de auditoria serão confirmados no mesmo fluxo transacional; falha em qualquer parte não deixa estado administrativo divergente.

## Risks / Trade-offs

- [AdminUser ser usado para acessar API de investidor] → audience, principal type e chaves separados, com testes de token cruzado.
- [Role ADMIN virar atalho para dados financeiros] → endpoints administrativos não chamarão serviços de operação; testes deverão provar saldo/posição/histórico inalterados.
- [Métricas misturarem BRL e USD] → retornar moeda nativa separada e conversão explícita com taxa/instante ou estado indisponível.
- [Desativação administrativa invalidar credencial errada] → revogar somente credenciais da conta alvo e registrar auditoria; nunca revogar por e-mail ou papel sem UUID validado.
- [Bootstrap deixar credencial em log] → aceitar somente secret externo, mascarar entradas e testar ausência em respostas e logs.

## Migration Plan

1. Implementar schema e repositório `AdminUser`/auditoria sem tocar tabelas financeiras.
2. Integrar a infraestrutura JWT da Aula 13 com audience e principal administrativos separados.
3. Criar bootstrap controlado e login administrativo; validar tokens cruzados.
4. Criar serviços e endpoints de gestão e dashboard com projeções agregadas.
5. Criar shell/rotas administrativas no frontend sem compartilhar estado financeiro de investidor.
6. Executar regressões, revisar Graphify e validar reviewer/tester antes de liberar a área administrativa.

## Contratos técnicos fechados para implementação

### Claims, propriedades e status HTTP

O token administrativo conterá somente `iss`, `aud`, `sub`, `iat`, `exp`, `jti`, `principal_type=ADMIN` e `role=ADMIN`. O `sub` será o UUID de `AdminUser`; `aud` deverá conter exatamente `corretora-admin-api`; `iss` deverá ser `corretora-admin`; `iat` não poderá estar no futuro; `exp` deverá ser posterior a `iat`; e `jti` deverá ser UUID. Claims adicionais serão rejeitadas.

As propriedades administrativas serão `APP_ADMIN_JWT_ENABLED=false` por padrão, `APP_ADMIN_JWT_ISSUER=corretora-admin`, `APP_ADMIN_JWT_AUDIENCE=corretora-admin-api`, `APP_ADMIN_JWT_SECRET`, `APP_ADMIN_JWT_ACCESS_TOKEN_TTL=PT10M` e `APP_ADMIN_JWT_REFRESH_TOKEN_TTL=PT8H`. Quando habilitado, o secret externo será obrigatório, Base64 com pelo menos 32 bytes decodificados; configuração ausente, vazia ou insegura deverá falhar explicitamente. As propriedades da Aula 13 permanecem inalteradas.

`POST /api/admin/auth/login` recebe JSON com `email` e `password`; o e-mail é normalizado por trim e minúsculas com locale neutro antes da busca e da unicidade. Com credenciais válidas retorna `204`, `Authorization: Bearer` e `Set-Cookie: ADMIN_REFRESH_TOKEN`. JSON inválido retorna `400`; credenciais, estado ou contrato de autenticação inválidos retornam `401`; CSRF ausente ou inválido retorna `403`.

`POST /api/admin/auth/refresh` não recebe body, exige cookie de refresh e CSRF, retorna `204` com novo Bearer e novo cookie. Cookie ausente, inválido, expirado, revogado ou reutilizado retorna `401`; CSRF inválido retorna `403`. `POST /api/admin/auth/logout` exige CSRF, revoga o refresh apresentado quando houver, expira o cookie e retorna `204`; ausência de refresh não revela estado e continua idempotente.

Falhas de validação de query, paginação, período ou JSON retornam `400`. Ausência de autenticação retorna `401`; identidade autenticada sem a autoridade do método retorna `403`; conflito de ciclo de vida, como desativação do último administrador, retorna `409`.

### Cookies e compatibilidade

`ADMIN_REFRESH_TOKEN` será `HttpOnly`, `SameSite=Lax`, `Path=/api/admin/auth`, `Secure` conforme `app.security.session-cookie-secure` e max-age igual ao TTL administrativo. `XSRF-TOKEN` continuará legível pelo frontend, `Path=/`, `SameSite=Lax`, `Secure` conforme o ambiente, usando `X-XSRF-TOKEN`.

O access token administrativo permanecerá somente em memória. Consultas Bearer-only não enviarão cookies. Login, refresh e logout enviarão cookies e exigirão CSRF. A sessão JDBC legada, quando `APP_JWT_SESSION_COMPATIBILITY=true`, permanece restrita à área financeira; nenhum `SESSION` será aceito para autenticar `/api/admin/**`.

### Persistência e integridade

As tabelas administrativas serão criadas em migration própria, sem `ALTER` ou foreign key para tabelas financeiras além de uma referência opcional de auditoria ao identificador da conta-alvo. `admin_user` terá UUID primário, e-mail normalizado único, hash BCrypt, estado, papel fixo `ADMIN`, timestamps de criação/inativação e índice por estado. `admin_refresh_token` terá hash único, administrador, família, criação, expiração, revogação, substituição e índices por administrador/família. `admin_audit_event` terá ator nullable, papel, ação, alvo nullable, instante, resultado e correlation id.

Constraints impedirão papel diferente de `ADMIN`, estados desconhecidos, hash vazio e token bruto persistido. A operação que inativaria o último `AdminUser` ativo será executada com bloqueio transacional e rejeitada com `409`.

### Bootstrap

O bootstrap será executado somente por comando CLI explícito e idempotente, lendo o e-mail normalizado e a senha por entrada segura, sem endpoint público. Com nenhum administrador, cria um único usuário ativo; se já houver qualquer administrador, não cria outro e termina sem alterar credenciais. `APP_ADMIN_BOOTSTRAP_PASSWORD` poderá ser usado somente como alternativa de provisionamento automatizado; não poderá ser exposto, persistido ou incluído em mensagens, exceções ou auditoria. A ausência de credencial obrigatória deverá falhar sem criar usuário.

### Separação de segurança

O resolver de autenticação selecionará explicitamente a cadeia administrativa para `/api/admin/**` e a cadeia de investidor para endpoints financeiros. Cada cadeia terá decoder, converter, principal, issuer, audience e secret próprios. O principal administrativo nunca será convertido em `AccountPrincipal`; o principal de investidor nunca será aceito como `AdminPrincipal`. Incompatibilidade de principal será rejeitada pela segurança e não poderá resultar em `500` por erro de conversão.

Um token ausente, adulterado, expirado, com assinatura inválida ou incompatível com a cadeia selecionada resulta em `401`. Um token reconhecido na cadeia correta, mas sem a autoridade exigida pelo método, resulta em `403`. O acesso cruzado entre `ADMIN` e `USER` será rejeitado antes de consultar controllers ou serviços da outra área.

### Contrato administrativo de investidores

`GET /api/admin/investors` aceita `status=ACTIVE|INACTIVE|ALL`, busca limitada, datas de criação/inativação, `page` zero-based, `size` de 1 a 100 e ordenação apenas por campos permitidos. O DTO contém `id`, `name`, `emailMasked`, `cpfMasked`, `status`, `createdAt` e `inactivatedAt`. Não contém senha, hash, saldo, posição, patrimônio, corretora ou histórico.

`PATCH /api/admin/investors/{accountId}/status` aceita somente `{ "status": "ACTIVE"|"INACTIVE" }`. Retorna `204` em sucesso, `400` para JSON/parâmetros inválidos, `401` sem autenticação, `403` sem `ADMIN`, `404` para conta inexistente e `409` para conflito de ciclo de vida. Desativação revoga os refresh tokens da conta e impede novos usos do access token pelo validador de conta ativa, sem apagar dados históricos.

### Dashboard administrativo

`GET /api/admin/dashboard?period=4W|3M|6M|1Y|5Y|MAX&limit=1..50` exige `ADMIN`. `period` é obrigatório, `limit` tem default 10 e o período é calculado em `America/Sao_Paulo`. Para `MAX`, a consulta cobre do primeiro dado relevante disponível até o fim do período atual, usando agregação diária e consultas limitadas para não carregar movimentos individuais desnecessariamente. O retorno contém exatamente estas oito métricas escalares: `totalAccounts`, `activeAccounts`, `inactiveAccounts`, `newAccounts`, `purchaseCount`, `saleCount`, `purchasedShares` e `soldShares`.

As métricas de conta têm semânticas distintas: `totalAccounts`, `activeAccounts` e `inactiveAccounts` são fotografias do estado atual das contas `ACTIVE` e `INACTIVE`, independentes do período e sempre coerentes com `totalAccounts = activeAccounts + inactiveAccounts`; contas `DELETED` ficam excluídas. `newAccounts` conta contas `ACTIVE` ou `INACTIVE` criadas dentro do período selecionado. As métricas de operações (`purchaseCount`, `saleCount`, `purchasedShares` e `soldShares`) permanecem limitadas ao período.

O mesmo retorno contém volumes de compra/venda separados por `BRL` e `USD`, sem soma direta entre moedas; equivalente em BRL somente aparece com taxa `USD/BRL` válida e inclui valor da taxa e instante da cotação. Sem taxa válida, o equivalente é nulo e o estado é `UNAVAILABLE`, enquanto os valores nativos permanecem disponíveis.

As séries são diárias e contêm apenas data, novas contas, compras e vendas. O ranking retorna no máximo `limit` ativos, ordenado por quantidade negociada decrescente, com desempate por ticker e mercado. Nenhuma projeção retorna identificador de investidor, saldo, patrimônio, posição, CPF completo ou e-mail completo.
