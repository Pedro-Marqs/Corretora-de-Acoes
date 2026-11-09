## 1. Contrato e configuração segura

- [ ] 1.1 Definir o contrato final de login, refresh, logout e erro `401`/`403`, incluindo issuer, audience, claims permitidas e ausência de dados sensíveis; verificar a especificação e uma matriz de casos.
- [x] 1.2 Configurar secrets, expiração, audience, issuer e modo de migração por ambiente sem valores sensíveis versionados; verificar propriedades de desenvolvimento, teste e produção simulada.
- [x] 1.3 Criar armazenamento de refresh tokens por hash, família, expiração e revogação sem alterar tabelas financeiras; verificar migração Liquibase e rollback em banco de teste.

## 2. Autenticação JWT e migração

- [x] 2.1 Implementar emissão JWT de investidor usando o encoder BCrypt existente e claims mínimas; verificar login válido, conta inativa e credenciais neutras.
- [ ] 2.2 Implementar validação Bearer com issuer, audience, `principal_type=INVESTOR`, papel `USER`, assinatura, expiração e principal da conta; verificar token ausente, inválido, adulterado, expirado, válido e rejeição na área administrativa.
- [x] 2.3 Implementar refresh rotativo, detecção de reutilização, logout e revogação por alteração de senha, e-mail e inativação; verificar que o refresh anterior não volta a funcionar.
- [x] 2.4 Implementar compatibilidade de sessão atrás de configuração explícita e documentar o corte; verificar que sessões legadas não acessam outra conta e que o modo final rejeita sessão.

## 3. Frontend e proteção de transporte

- [x] 3.1 Migrar o cliente HTTP para access token em memória, refresh cookie HttpOnly e restauração após recarregamento; verificar ausência de localStorage/sessionStorage e uma única tentativa de refresh.
- [ ] 3.2 Ajustar CSRF, CORS, cookies e tratamento de `401`/`403` sem expor tokens ou detalhes técnicos; verificar preflight, mutação sem CSRF, logout e origem não permitida.

## 4. Regressão e entrega

- [ ] 4.1 Atualizar testes de controllers, serviços e frontend que dependem de `SESSION`, mantendo testes de propriedade, compras, vendas, transferências, dashboard e histórico; verificar suíte focada sem alterar regras financeiras.
- [x] 4.2 Executar revisão Graphify dos consumidores de autenticação e `graphify update .` após as alterações de código; verificar que o grafo e o diff refletem somente o change.
- [ ] 4.3 Executar validação focalizada e registrar comandos, resultados, riscos restantes e decisão de corte; deixar a validação final para reviewer, tester e OpenSpec.
