## 1. Design system e componentes-base

- [x] 1.1 Congelar o inventário das rotas, layouts, componentes, clientes de API, estados e testes descrito em `design.md`, registrando também os campos oficiais usados por cada tela; verificar o artefato contra `frontend/src/App.jsx`, `frontend/src/api/` e os testes existentes sem alterar código de produção.
- [x] 1.2 Criar a camada de tokens visuais e a base tipográfica/responsiva em `frontend/src/styles/`, com tema escuro, esmeralda, superfícies, estados semânticos, foco, espaçamento, raios e sombras; verificar com teste de estilos, `npm run lint` e `npm run build`.
- [x] 1.3 Implementar primitivas reutilizáveis em `frontend/src/components/ui/` para shell, superfícies, botões, campos, badges, métricas, tabelas, filtros, paginação, mensagens e diálogos; verificar cada primitiva com Testing Library para estados normais, disabled, foco e teclado.
- [x] 1.4 Extrair primitivas de visualização acessíveis em `frontend/src/components/data-viz/` a partir dos SVG/HTML existentes, mantendo pontos oficiais, unidade, moeda, período e alternativa textual/tabular; verificar com dados reais de teste dos clientes, sem mocks de domínio ou dependência de cor/hover.
- [x] 1.5 Consolidar `LoadingState`, `EmptyState`, `ErrorState`, confirmação, conflito e atualização pós-mutação sem alterar contratos de API; verificar reenvio bloqueado, erro recuperável, `aria-live` e foco de diálogo nos testes de componentes.

## 2. Navegação, layouts e autenticação

- [ ] 2.1 Migrar cadastro, login, reativação, sessão transitória e página 404 para as primitivas do design system; verificar preservação de validações, mensagens, dados não persistidos e responsividade a partir de 320px nos testes das páginas públicas.
- [ ] 2.2 Reestruturar `AppLayout` e a navegação privada do investidor com shell, busca, menu de conta, seções Investimentos/Banco/Bolsa e configuração; verificar seção ativa, menu por teclado/Escape, logout, busca de ativos e redirecionamento de sessão inválida.
- [ ] 2.3 Reestruturar `AdminLayout` e o fluxo visual de `/admin/login` sem compartilhar contexto ou token com o investidor; verificar login, refresh, logout, erro 401/403, foco e encaminhamento para a rota administrativa correta.
- [ ] 2.4 Executar regressão das rotas em `App.jsx` e dos guards públicos, privados e administrativos depois da migração; verificar que todas as rotas listadas no design continuam acessíveis somente ao papel autorizado e que nenhum conteúdo privado aparece durante loading/erro.

## 3. Dashboard e gráficos do investidor

- [ ] 3.1 Migrar `DashboardPage` para cards de métricas, superfícies e filtros usando somente o snapshot de `/api/dashboard`; verificar saldo disponível, patrimônio, posições, resultados e avisos sem placeholders de posição ou cálculo financeiro local.
- [ ] 3.2 Migrar gráfico patrimonial, série de investimento, distribuição por ativo/corretora/mercado e estados de câmbio; verificar `patrimonyHistory` separado de `investmentHistory`, BRL/USD identificados, período oficial e ausência de pontos inventados.
- [ ] 3.3 Adaptar o dashboard do investidor para desktop, tablet e mobile; verificar ordem dos cards, legibilidade dos gráficos, ausência de overflow global e consulta acessível dos pontos em 320px, viewport intermediária e desktop.
- [ ] 3.4 Cobrir os estados do dashboard durante troca de período/corretora, resposta vazia, cotação/câmbio stale, erro de API, 401 e retry; verificar que resultados antigos não são misturados e que a sessão é encaminhada ao login correto.

## 4. Demais telas do investidor

- [ ] 4.1 Migrar `HomePage`, `AccountPage`, `ReactivationPage` e componentes de perfil/sessão; verificar dados mascarados, alterações de credencial, confirmações destrutivas, revogação visual após resposta oficial e mensagens seguras.
- [ ] 4.2 Migrar `BankPage`, `WalletPage` e `TransfersPage` com cards de saldo, aportes, retiradas e transferências; verificar confirmação, CSRF/cliente existente, bloqueio de duplicidade e atualização somente pela resposta oficial da API.
- [ ] 4.3 Migrar `AssetsPage`, `AssetDetailPage`, `OperationsPage` e `PortfolioPage`; verificar busca real, mercados BR/US, moeda correspondente, preço oficial somente leitura, compra/venda, posição e rejeições sem falso sucesso.
- [ ] 4.4 Migrar `HistoryPage` e `BrokersPage` com tabelas/filtros responsivos; verificar limite de 20 itens, filtros combinados e paginação oficiais, corretoras próprias por CNPJ, remoção/recadastro e estados vazios/erro.
- [ ] 4.5 Executar testes focados de todas as telas do investidor e dos clientes de API alterados apenas para apresentação; verificar sucesso, loading, vazio, erro, autorização e rejeição de mutação sem alterar regras financeiras ou endpoints.

## 5. Dashboard e telas administrativas

- [ ] 5.1 Migrar `AdminLoginPage` e `AdminLayout` para as primitivas compartilhadas, preservando a identidade de área restrita e a separação do investidor; verificar token somente em memória, refresh single-flight, logout e sessão revogada nos testes existentes.
- [ ] 5.2 Migrar `AdminDashboardPage` para cards, séries, ranking e visualizações agregadas; verificar que `totalAccounts`, `activeAccounts` e `inactiveAccounts` são atuais, que `newAccounts` e operações respeitam o período e que BRL/USD/conversão exibem `NOT_REQUIRED` e indisponibilidade corretamente.
- [ ] 5.3 Migrar a tabela de `AdminInvestorsPage` com filtros, busca, ordenação, paginação, dados mascarados, status e confirmação; verificar atualização após sucesso, conflito 409 sem falso sucesso e ausência de CPF/e-mail não mascarados ou dados financeiros individuais.
- [ ] 5.4 Cobrir as telas administrativas em loading, vazio, erro, 401/403, conflito e viewport estreita; verificar que o dashboard não expõe investidores individuais e que nenhuma ação administrativa altera regras de segurança ou contexto do investidor.

## 6. Responsividade, acessibilidade e regressão final

- [ ] 6.1 Auditar todas as rotas públicas, de investidor e administrativas em 320px, tablet e desktop, corrigindo composição, overflow, tabelas, diálogos e navegação; verificar checklist visual por tela sem alterar os dados exibidos.
- [ ] 6.2 Executar auditoria de teclado e semântica para navegação, formulários, tabelas, filtros, gráficos, mensagens, menus e confirmações; verificar foco visível, labels, `aria-live`, ordem lógica, Escape e alternativa textual para visualizações.
- [ ] 6.3 Remover estilos duplicados ou conflitantes somente após a migração das superfícies correspondentes; verificar que a identidade do investidor e do administrador continua única e que `git diff --check` não aponta problemas.
- [ ] 6.4 Executar a suíte frontend completa com `npm test`, `npm run lint` e `npm run build`; executar `npm run test:coverage` e comprovar statements, branches, functions e lines iguais ou superiores a 80%, sem skips tratados como aprovação e sem alterar thresholds.
- [ ] 6.5 Executar validação OpenSpec strict, atualizar Graphify após as alterações de código e revisar o diff final; verificar que somente frontend, testes e artefatos permitidos do change foram modificados e que backend, migrations, CI, Aulas 13/14/15 e changes arquivados permanecem intactos.
