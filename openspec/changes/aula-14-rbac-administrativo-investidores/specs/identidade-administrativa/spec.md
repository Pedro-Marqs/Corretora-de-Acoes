## Purpose

Separar operadores administrativos da identidade financeira dos investidores, aplicando RBAC explícito sem conceder a administradores acesso automático a saldo, carteira, posições ou histórico individual.

## ADDED Requirements

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

O sistema SHALL impedir cadastro público de administradores. O primeiro administrador SHALL ser inicializado somente por procedimento controlado de ambiente ou operação administrativa local, com segredo externo, hash BCrypt e execução única verificável.

#### Scenario: Cadastro público de administrador

- **WHEN** uma requisição pública tentar criar `AdminUser`
- **THEN** o sistema SHALL rejeitar a operação sem criar identidade administrativa

#### Scenario: Bootstrap sem segredo externo

- **WHEN** o bootstrap for iniciado sem credenciais externas obrigatórias ou quando já existir administrador
- **THEN** o sistema SHALL não criar outro administrador e SHALL informar falha operacional sem expor segredo

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
