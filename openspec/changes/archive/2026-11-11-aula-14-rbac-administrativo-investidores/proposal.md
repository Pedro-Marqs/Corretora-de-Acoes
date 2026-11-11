## Why

O projeto possui somente a identidade financeira `Account`, adequada para investidores, mas ainda não possui uma fronteira administrativa independente. A Aula 14 exige RBAC sem transformar papel administrativo em autorização para movimentar ou consultar indiscriminadamente carteiras individuais.

## What Changes

- Criar a identidade administrativa independente `AdminUser`, sem saldo, carteira, posição ou relação financeira com `Account`.
- Criar login administrativo JWT com audience, tipo de principal e chave/configuração separados dos tokens de investidores.
- Formalizar claims `iss`, `aud`, `sub`, `iat`, `exp`, `jti`, `principal_type=ADMIN` e `role=ADMIN`, além das variáveis e TTLs administrativos.
- Definir somente duas identidades funcionais: investidor `USER` em `Account` e administrador `ADMIN` em `AdminUser`.
- Impedir cadastro público e definir bootstrap seguro do primeiro administrador.
- Proteger endpoints administrativos com autorização em nível de método para `ADMIN`.
- Criar consulta, ativação e desativação paginadas de investidores, sem operações financeiras administrativas.
- Criar dashboard agregado com métricas de contas, negociações, volumes, séries temporais e ranking de ativos.
- Tratar moedas sem somar valores incompatíveis e registrar conversões quando houver equivalente em BRL.
- Registrar auditoria de ativação, desativação, bootstrap, login administrativo e tentativas negadas sensíveis.
- Impedir acesso cruzado entre tokens de investidores e endpoints administrativos.

## Evolução futura

Um operador administrativo somente leitura poderá ser introduzido em change posterior, com papel e matriz de permissões próprios. Essa possibilidade não faz parte desta versão e não possui tarefas neste change.

## Capabilities

### New Capabilities

- `identidade-administrativa`: identidade, login, papéis, bootstrap, gestão limitada de investidores e auditoria.
- `dashboard-administrativo`: métricas agregadas e ranking operacional sem exposição de carteiras individuais.

### Modified Capabilities

- Nenhuma. A identidade `Account` e a autorização horizontal do investidor permanecem contratos existentes.

## Impact

Afeta persistência e migrações administrativas, configuração JWT, cadeia de segurança, method security, endpoints e serviços administrativos, consultas agregadas de movimentos e contas, frontend separado do painel e testes de isolamento. Depende do contrato JWT definido em `aula-13-migrar-autenticacao-jwt` e não altera endpoints financeiros existentes.

## Contratos propostos para validação antes da Etapa 1

- Claims administrativas: `iss`, `aud`, `sub`, `iat`, `exp`, `jti`, `principal_type=ADMIN` e `role=ADMIN`, sem claims adicionais.
- Configuração: `APP_ADMIN_JWT_ENABLED=false` por padrão, `APP_ADMIN_JWT_ISSUER=corretora-admin`, `APP_ADMIN_JWT_AUDIENCE=corretora-admin-api`, `APP_ADMIN_JWT_SECRET` externo obrigatório quando habilitado, `APP_ADMIN_JWT_ACCESS_TOKEN_TTL=PT10M` e `APP_ADMIN_JWT_REFRESH_TOKEN_TTL=PT8H`; configuração ausente ou insegura deve falhar explicitamente.
- Login administrativo por e-mail normalizado de forma determinística (trim e minúsculas com locale neutro), com unicidade case-insensitive aplicada ao valor normalizado.
- Endpoints separados: `POST /api/admin/auth/login`, `/refresh` e `/logout`.
- Cookie `ADMIN_REFRESH_TOKEN` com `HttpOnly`, `SameSite=Lax`, `Path=/api/admin/auth`, `Secure` conforme o ambiente e proteção CSRF pelo `XSRF-TOKEN` existente.
- Bootstrap somente por comando CLI explícito e idempotente, com leitura segura da senha; `APP_ADMIN_BOOTSTRAP_PASSWORD` fica reservado à automação de provisionamento e nunca deve ser exposto ou persistido.
- Desativação do último administrador ativo rejeitada transacionalmente com `409`; a Aula 14 não cria cadastro público de administradores.
- Dashboard administrativo: `totalAccounts`, `activeAccounts` e `inactiveAccounts` representam o estado atual de contas `ACTIVE`/`INACTIVE`, independentemente do período; `newAccounts` representa contas criadas no período; compras, vendas e ações negociadas permanecem limitadas ao período. Contas `DELETED` não entram nas métricas de contas.

O contrato detalhado de HTTP, persistência, auditoria e dashboard está em `design.md` e nas duas specs deste change. As decisões acima fecham os contratos para a implementação; as tasks permanecem pendentes até haver código e evidências.
