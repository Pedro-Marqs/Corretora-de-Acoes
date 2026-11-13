# identidade-administrativa Specification

## Purpose

Separar operadores administrativos da identidade financeira dos investidores, aplicando RBAC explícito sem conceder a administradores acesso automático a saldo, carteira, posições ou histórico individual.

## Requirements

### Requirement: Identidade administrativa independente

O sistema SHALL persistir `AdminUser` em estrutura própria, com credencial, estado, papel `ADMIN` e identificador administrativo, sem saldo, carteira, posição, corretora associada ou relacionamento financeiro com `Account`. A identidade investidora `Account` SHALL representar o papel funcional `USER` em sua própria área.

#### Scenario: Login administrativo válido

- **WHEN** um `AdminUser` ativo informar credenciais válidas
- **THEN** o sistema SHALL emitir JWT administrativo com audience e tipo de principal administrativos

#### Scenario: Token de investidor em área administrativa

- **WHEN** um JWT de investidor acessar endpoint administrativo
- **THEN** o sistema SHALL rejeitar a requisição sem consultar nem alterar dados administrativos

#### Scenario: Token administrativo em área financeira

- **WHEN** um JWT administrativo acessar endpoint de carteira, saldo, posição, compra, venda ou transferência
- **THEN** o sistema SHALL rejeitar a requisição e SHALL não criar contexto de conta investidora

### Requirement: Bootstrap e credenciais administrativas

O sistema SHALL impedir cadastro público de administradores. O primeiro administrador SHALL ser inicializado somente por comando CLI explícito e idempotente, com e-mail normalizado por trim e minúsculas com locale neutro, senha lida de forma segura, hash BCrypt e execução verificável. `APP_ADMIN_BOOTSTRAP_PASSWORD` SHALL ser aceito somente como alternativa de provisionamento automatizado, sem exposição ou persistência indevida.

#### Scenario: Cadastro público de administrador

- **WHEN** uma requisição pública tentar criar `AdminUser`
- **THEN** o sistema SHALL rejeitar a operação sem criar identidade administrativa

#### Scenario: Bootstrap sem segredo externo

- **WHEN** o comando CLI for iniciado sem credencial obrigatória
- **THEN** o sistema SHALL falhar sem criar administrador e SHALL informar o resultado operacional sem expor segredo

### Requirement: Separação de identidades e autorização administrativa

O sistema SHALL aceitar somente a identidade investidora `USER` na área financeira e a identidade administrativa `ADMIN` na área administrativa. O padrão SHALL ser negar e cada método administrativo SHALL declarar a exigência de `ADMIN`.

#### Scenario: Investidor USER consulta sua área

- **WHEN** um investidor autenticado com identidade `USER` acessar sua área financeira
- **THEN** o sistema SHALL permitir somente operações autorizadas para sua própria conta

#### Scenario: Investidor USER acessa área administrativa

- **WHEN** um investidor com identidade `USER` tentar consultar indicadores globais ou gerenciar investidores
- **THEN** o sistema SHALL responder `401` ou `403` conforme a falha de audience/autorização e SHALL não retornar dados administrativos

#### Scenario: Administrador ADMIN consulta e altera estado

- **WHEN** um administrador `ADMIN` consultar indicadores globais ou ativar/desativar um investidor válido
- **THEN** o sistema SHALL permitir somente essas ações administrativas, revogar autenticações afetadas quando aplicável e registrar auditoria

### Requirement: Contrato de endpoints de investidores

O sistema SHALL expor `GET /api/admin/investors` para consulta paginada e `PATCH /api/admin/investors/{accountId}/status` para ativação ou desativação. A listagem SHALL aceitar somente `status=ACTIVE|INACTIVE|ALL`, busca limitada, datas de criação/inativação, `page` zero-based, `size` de 1 a 100 e ordenação por campos permitidos. O DTO SHALL conter somente `id`, `name`, `emailMasked`, `cpfMasked`, `status`, `createdAt` e `inactivatedAt`.

#### Scenario: Listagem administrativa válida

- **WHEN** um `ADMIN` enviar filtros e paginação válidos
- **THEN** o sistema SHALL responder `200` com a página solicitada, totalizadores e somente os campos administrativos permitidos

#### Scenario: Listagem inválida ou sem autorização

- **WHEN** paginação, filtro, ordenação ou JSON forem inválidos, ou a identidade não for autorizada
- **THEN** o sistema SHALL responder `400` para validação, `401` sem autenticação ou `403` sem `ADMIN`, sem executar consulta financeira individual

#### Scenario: Alteração de estado do investidor

- **WHEN** um `ADMIN` enviar `PATCH /api/admin/investors/{accountId}/status` com `ACTIVE` ou `INACTIVE`
- **THEN** o sistema SHALL responder `204`, preservar o histórico, revogar refreshes da conta quando inativar e impedir novo access token para conta inativa

#### Scenario: Conflito de ciclo de vida do investidor

- **WHEN** o alvo não existir ou a transição não puder ser aplicada por conflito de estado
- **THEN** o sistema SHALL responder `404` ou `409`, sem alterar saldo, posição, movimento ou patrimônio

### Requirement: Gestão limitada de investidores

O sistema SHALL oferecer ao `ADMIN` consulta paginada de investidores com filtros seguros e ativação/desativação controladas. A gestão administrativa SHALL excluir alteração de senha, acesso a carteira individual, movimentação de saldo, compra, venda e transferência. A introdução futura de operador somente leitura fica fora deste change.

#### Scenario: Consulta paginada

- **WHEN** um administrador `ADMIN` consultar investidores com período, estado, busca ou paginação válidos
- **THEN** o sistema SHALL retornar somente campos administrativos mínimos, totalizadores e página solicitada

#### Scenario: Tentativa de movimentação financeira

- **WHEN** um administrador `ADMIN` tentar depositar, retirar, comprar, vender ou transferir recursos
- **THEN** o sistema SHALL rejeitar a operação e SHALL preservar saldo, posições, histórico e patrimônio

### Requirement: Auditoria administrativa

O sistema SHALL registrar bootstrap, login administrativo relevante, ativação, desativação e tentativas administrativas negadas com ator, papel, ação, alvo não sensível, instante controlado, resultado e identificador de correlação. O registro SHALL não conter senha, token, hash ou payload bruto.

#### Scenario: Ação sensível aprovada

- **WHEN** um `ADMIN` executar ativação ou desativação
- **THEN** o sistema SHALL persistir evento de auditoria associado ao resultado da operação

#### Scenario: Ação negada

- **WHEN** uma ação administrativa for negada por papel, audience ou estado
- **THEN** o sistema SHALL registrar a tentativa sem revelar dados protegidos na resposta

### Requirement: Campos obrigatórios de auditoria

Cada evento SHALL possuir `id`, `actor_admin_id` nullable para falhas sem identidade, `actor_role`, `action`, `target_account_id` e/ou `target_admin_id` quando aplicável, `occurred_at`, `outcome` e `correlation_id`. `outcome` SHALL distinguir pelo menos `SUCCESS`, `DENIED` e `FAILURE`. O evento SHALL referenciar apenas identificadores e códigos controlados, nunca payload bruto, cookie, JWT, senha ou hash.

#### Scenario: Auditoria de ação aprovada

- **WHEN** um `ADMIN` executar login, listagem, ativação, desativação ou logout
- **THEN** o sistema SHALL registrar ator, ação, alvo aplicável, instante, resultado e correlação sem dados secretos

#### Scenario: Auditoria sem identidade

- **WHEN** um login inválido ou tentativa negada ocorrer antes da criação do principal
- **THEN** o sistema SHALL registrar evento com ator nullable, resultado controlado e nenhum segredo ou payload bruto

### Requirement: Contrato JWT administrativo

O sistema SHALL emitir JWT administrativo somente para `AdminUser` ativo, com exatamente as claims `iss`, `aud`, `sub`, `iat`, `exp`, `jti`, `principal_type=ADMIN` e `role=ADMIN`. O issuer SHALL ser `corretora-admin`, a audience SHALL ser `corretora-admin-api`, `sub` SHALL ser o UUID do administrador e claims adicionais SHALL ser rejeitadas. `APP_ADMIN_JWT_ENABLED` SHALL ser `false` por padrão; quando habilitado, o secret externo SHALL ser obrigatório e configuração ausente ou insegura SHALL falhar explicitamente. Os TTLs SHALL ser 10 minutos para access token e 8 horas para refresh token. O secret, issuer, audience e TTLs administrativos SHALL ser independentes dos valores do JWT `INVESTOR/USER` da Aula 13.

#### Scenario: Claims administrativas válidas

- **WHEN** um administrador ativo informar credenciais válidas
- **THEN** o sistema SHALL retornar `204`, um Bearer administrativo e um refresh administrativo, sem incluir senha, hash ou dados financeiros

#### Scenario: Token de investidor na cadeia administrativa

- **WHEN** um JWT com `principal_type=INVESTOR`, `role=USER` ou audience de investidor for enviado a endpoint administrativo
- **THEN** o sistema SHALL responder `401` por incompatibilidade de autenticação e SHALL não criar `AdminPrincipal`

#### Scenario: Token administrativo na cadeia financeira

- **WHEN** um JWT com `principal_type=ADMIN` for enviado a endpoint de conta, carteira, saldo, posição, compra, venda ou transferência
- **THEN** o sistema SHALL rejeitar a solicitação antes do controller e SHALL não criar `AccountPrincipal` nem alterar dados financeiros

### Requirement: Sessão administrativa por cookie seguro

O sistema SHALL expor `POST /api/admin/auth/login`, `POST /api/admin/auth/refresh` e `POST /api/admin/auth/logout`. Login, refresh e logout SHALL exigir CSRF válido quando utilizarem cookie. O cookie `ADMIN_REFRESH_TOKEN` SHALL ser `HttpOnly`, `SameSite=Lax`, `Path=/api/admin/auth`, `Secure` conforme o ambiente e armazenar somente refresh opaco. Access tokens administrativos SHALL permanecer somente em memória no frontend.

#### Scenario: Login administrativo inválido

- **WHEN** o body for inválido, a credencial for inválida ou o administrador estiver inativo
- **THEN** o sistema SHALL responder `400` para validação ou `401` para autenticação e SHALL não emitir credencial

#### Scenario: Refresh administrativo válido

- **WHEN** o cliente enviar refresh válido em cookie com CSRF válido
- **THEN** o sistema SHALL responder `204`, emitir novo Bearer, rotacionar o cookie e invalidar o refresh anterior

#### Scenario: Refresh administrativo rejeitado

- **WHEN** o refresh estiver ausente, expirado, revogado, reutilizado ou o CSRF for inválido
- **THEN** o sistema SHALL responder `401` para a credencial ou `403` para CSRF e SHALL não emitir novo Bearer

#### Scenario: Logout administrativo

- **WHEN** o cliente enviar logout com CSRF válido
- **THEN** o sistema SHALL responder `204`, revogar o refresh apresentado quando houver e expirar `ADMIN_REFRESH_TOKEN` sem revelar se ele era válido

### Requirement: Bootstrap administrativo controlado

O sistema SHALL criar o primeiro administrador somente por comando CLI explícito e idempotente, com e-mail normalizado e senha lida de forma segura. `APP_ADMIN_BOOTSTRAP_PASSWORD` SHALL ser permitido somente como alternativa de provisionamento automatizado, sem exposição ou persistência indevida. O bootstrap SHALL não ser exposto por HTTP, SHALL usar BCrypt e SHALL não registrar os valores recebidos. Se já existir administrador, SHALL não criar outro; se faltar credencial obrigatória, SHALL falhar sem criar usuário.

#### Scenario: Primeiro bootstrap

- **WHEN** o comando CLI de bootstrap for executado, a tabela estiver vazia e as credenciais fornecidas de forma segura forem válidas
- **THEN** o sistema SHALL criar exatamente um `AdminUser` ativo com papel `ADMIN`

#### Scenario: Bootstrap repetido

- **WHEN** o bootstrap for executado novamente após existir administrador
- **THEN** o sistema SHALL permanecer idempotente, não criar duplicata e não expor credenciais

#### Scenario: Bootstrap sem segredo

- **WHEN** o comando CLI de bootstrap não receber uma credencial obrigatória
- **THEN** o sistema SHALL falhar sem criar administrador e SHALL não incluir o segredo em resposta, exceção ou log

### Requirement: Integridade do ciclo de vida administrativo

O sistema SHALL aceitar somente estados `ACTIVE` e `INACTIVE`, SHALL manter o papel fixo `ADMIN` e SHALL impedir transação que deixe zero administradores ativos. A tentativa de desativar o último administrador ativo SHALL responder `409` e SHALL preservar o estado anterior. A Aula 14 não SHALL criar cadastro público nem tela de gestão de operadores.

#### Scenario: Último administrador ativo

- **WHEN** uma operação controlada tentar inativar o único `AdminUser` ativo
- **THEN** o sistema SHALL responder `409`, SHALL manter o administrador ativo e SHALL registrar a tentativa negada

#### Scenario: Administrador inativado

- **WHEN** um administrador deixar de estar ativo
- **THEN** novos logins e refreshes SHALL responder `401`, refreshes existentes SHALL ser revogados e nenhum dado financeiro SHALL ser criado ou alterado


