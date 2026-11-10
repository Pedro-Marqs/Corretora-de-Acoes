## 1. Identidade e persistência administrativa

- [x] 1.1 Definir `AdminUser` independente de `Account`, com UUID, e-mail normalizado único, hash BCrypt, estado `ACTIVE|INACTIVE`, papel fixo `ADMIN`, timestamps e DTOs mínimos; verificar ausência de senha/hash e de campos financeiros.
- [x] 1.2 Criar migrations para `admin_user`, `admin_refresh_token` e `admin_audit_event`, com chaves, índices, constraints de estado/papel e sem alterar tabelas financeiras; verificar banco vazio, banco existente e rollback controlado.
- [x] 1.3 Implementar comando CLI de bootstrap idempotente, com leitura segura de senha, BCrypt, sem endpoint HTTP, sem duplicação e sem segredos em logs; permitir `APP_ADMIN_BOOTSTRAP_PASSWORD` somente para provisionamento automatizado sem persistência indevida; verificar credencial ausente, primeira execução e execução repetida.

## 2. JWT e autorização da área administrativa

- [x] 2.1 Integrar JWT administrativo com claims exatas, `issuer=corretora-admin`, `audience=corretora-admin-api`, `APP_ADMIN_JWT_ENABLED=false` por padrão, secret externo obrigatório quando habilitado, TTL de access token de 10 minutos, TTL de refresh de 8 horas e converter separado do investidor; verificar claims adicionais, audience, issuer, assinatura, expiração, principal e tokens cruzados rejeitados.
- [x] 2.2 Configurar resolver/decoders separados, autorização HTTP e method security com default deny; verificar `401` para token ausente/inválido, `403` para principal sem autoridade, `USER` bloqueado e `ADMIN` permitido somente na área administrativa.
- [ ] 2.3 Implementar `POST /api/admin/auth/login|refresh|logout`, cookie próprio, CSRF, rotação, reutilização, logout, expiração e revogação por estado/credencial; verificar `204`, `400`, `401`, `403`, `409` e desativação segura do operador sem perder o último administrador ativo.

## 3. Gestão de investidores

- [x] 3.1 Implementar `GET /api/admin/investors` com status, busca, período, paginação limitada e ordenação whitelist; verificar DTO com somente id, nome, e-mail/CPF mascarados, estado e timestamps.
- [x] 3.2 Implementar `PATCH /api/admin/investors/{accountId}/status` para ativação/desativação somente por `ADMIN`, revogando refreshes da conta inativada e preservando histórico; verificar `400`, `401`, `403`, `404`, `409`, rollback e investidor sem acesso administrativo.
- [x] 3.3 Registrar auditoria de bootstrap, login, logout, listagem, ativação, desativação e negativas com ator nullable, papel, ação, alvo, instante, resultado e correlação; verificar ausência de token/senha/hash/cookie/payload.

## 4. Dashboard administrativo

- [ ] 4.1 Implementar `/api/admin/dashboard` com `totalAccounts`, `activeAccounts`, `inactiveAccounts`, `newAccounts`, `purchaseCount`, `saleCount`, `purchasedShares` e `soldShares`; verificar período em Brasília e métricas sem dados individuais.
- [ ] 4.2 Implementar séries diárias e ranking limitado por quantidade negociada, com desempate por ticker/mercado, default 10 e máximo 50; definir `MAX` do primeiro dado relevante disponível ao fim do período atual com agregação de custo controlado; verificar empates, limite, dados vazios e ausência de efeitos colaterais.
- [ ] 4.3 Implementar volumes nativos BRL/USD e equivalente em BRL somente com taxa válida, informando taxa/instante ou `UNAVAILABLE`; verificar arredondamento financeiro e ausência de soma incompatível.
- [ ] 4.4 Criar `AdminAuthContext`, cliente API, rotas e telas administrativas isoladas do `AuthContext`, carteira e histórico do investidor; verificar login, reload, logout, loading, erros e acesso cruzado.

## 5. Validação transversal

- [x] 5.1 Testar isolamento completo: `ADMIN` não movimenta recursos nem consulta saldo, posição, movimentação, patrimônio ou carteira individual; verificar dados financeiros inalterados e tokens/sessões cruzados rejeitados.
- [x] 5.2 Consultar Graphify para revisar dependências de segurança, contas, movimentos e frontend e executar `graphify update .` após o código; verificar grafo atualizado e somente relações do change.
- [x] 5.3 Executar testes backend/frontend focados, regressão financeira, lint/build aplicáveis e registrar riscos; deixar reviewer, tester e validação OpenSpec como gates finais.
