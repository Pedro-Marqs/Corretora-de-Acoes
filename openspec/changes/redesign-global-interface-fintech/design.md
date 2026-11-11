## Context

O frontend atual é React 19 com Vite e React Router, organizado por páginas, layouts, contextos de autenticação, clientes de API e um CSS global único. `App.jsx` expõe rotas públicas (`/cadastro`, `/login`, `/reativacao`, `/404`), rotas privadas do investidor sob `/app` e rotas administrativas em `/admin` e `/admin/investors`. `AppLayout` concentra sidebar, busca de ativos, menu da conta e logout; `AdminLayout` concentra a navegação administrativa e logout. Os estados comuns já existem em `components/common/AsyncStates.jsx`.

Os clientes de API fazem validação defensiva das respostas e cobrem, entre outros, `/api/dashboard`, `/api/wallet`, `/api/history`, `/api/brokers`, `/api/assets/search`, `/api/auth/*`, `/api/admin/auth/*`, `/api/admin/dashboard` e `/api/admin/investors`. O dashboard do investidor recebe saldo, posições, distribuições, câmbio, avisos, `patrimonyHistory` e `investmentHistory`; o dashboard administrativo recebe métricas atuais de contas, métricas operacionais por período, volumes BRL/USD, séries e ranking agregado. O backend e as especificações consolidadas são a fonte dos dados.

Hoje o pacote frontend não declara biblioteca de gráficos dedicada. Os gráficos existentes são componentes React/SVG e barras estilizadas em `DashboardPage.jsx`, `AdminDashboardPage.jsx` e `global.css`. O Vitest usa Testing Library/jsdom e exige 80% mínimo em statements, branches, functions e lines. O redesign deve respeitar esse baseline e não introduzir mocks de dados financeiros.

## Goals / Non-Goals

**Goals:**

- Criar uma fundação visual única, escura, premium e esmeralda para investidor e administrador.
- Transformar tokens e componentes compartilhados em contratos visuais reutilizáveis, reduzindo estilos ad hoc nas páginas.
- Padronizar cards de métricas, gráficos, tabelas, filtros, formulários, confirmações e estados assíncronos.
- Preservar a navegação funcional existente, a distinção de papéis e todos os dados oficiais, inclusive moedas e semântica temporal.
- Cobrir desktop, tablet, mobile a partir de 320px, foco visível, teclado, semântica e alternativas acessíveis para visualizações.
- Entregar a mudança em seis etapas pequenas, cada uma com testes focados, screenshots/manuais quando úteis e regressão automatizada.

**Non-Goals:**

- Alterar endpoints, DTOs, regras financeiras, cálculos de patrimônio, persistência, migrations, CI ou autenticação.
- Criar novas métricas, séries históricas, cotações, conversões ou recomendações financeiras.
- Copiar a marca ou os dados da imagem de referência, ou transformar a referência em requisito de conteúdo.
- Introduzir uma segunda identidade visual para administradores.
- Arquivar, reabrir ou alterar Aulas 13, 14 ou 15.

## Decisions

### 1. Uma fundação de tokens para os dois papéis

Adicionar tokens CSS nomeados em uma camada dedicada, provavelmente `frontend/src/styles/tokens.css`, importada antes da folha de composição. A paleta deverá reservar:

- superfícies escuras em níveis (`canvas`, `surface`, `surface-raised`, `surface-muted`);
- texto primário, secundário e de baixa ênfase com contraste verificável;
- esmeralda para ação positiva e destaque principal, com estados hover/active/focus;
- azul, âmbar, vermelho e roxo somente como semântica auxiliar de dados/estado, nunca como identidades concorrentes;
- tokens de `space-*`, `radius-*`, `shadow-*`, `font-*`, `control-*` e breakpoints documentados.

A tipografia será sans-serif para interface e dados, com escala fluida baseada em `clamp`, pesos limitados e tabular numerals para valores. Todas as cores de ganho, perda, aviso e erro terão significado textual além da cor.

Alternativa considerada: manter os valores espalhados em `global.css`. Foi rejeitada porque o arquivo já contém regras legadas e variações de tema difíceis de auditar entre layouts.

### 2. Componentes de apresentação com composição, não páginas monolíticas

Criar uma biblioteca local em `frontend/src/components/ui/` e, se necessário, `frontend/src/components/data-viz/`, com componentes sem regra financeira: `PageShell`, `SectionHeader`, `MetricCard`, `SurfaceCard`, `Badge`, `Button`, `TextField`, `SelectField`, `FilterBar`, `DataTable`, `Pagination`, `AsyncState`, `EmptyState`, `ErrorState`, `ConfirmDialog`, `StatusMessage`, `ResponsiveNav` e primitivas acessíveis de gráfico.

Os componentes receberão dados já interpretados pelos clientes de API e não calcularão rentabilidade, patrimônio, câmbio ou permissões. A decisão de atualização após uma mutação continuará nos hooks/páginas que chamam as APIs.

Alternativa considerada: instalar uma biblioteca de componentes completa. Foi rejeitada neste planejamento por aumentar o acoplamento visual, o bundle e a superfície de acessibilidade sem necessidade comprovada.

### 3. Manter SVG nativo como primeira opção para gráficos

Os gráficos atuais de patrimônio, distribuição, séries administrativas e indicadores deverão ser refatorados para primitivas SVG/HTML reutilizáveis, com `aria-labelledby`, `title/desc`, unidades, período, moeda e tabela/detalhes acessíveis. A implementação deverá renderizar apenas os pontos oficiais retornados pelos clientes de API.

Uma biblioteca como Recharts só será considerada durante a etapa de base visual se uma lacuna concreta de interação, resize ou acessibilidade não puder ser resolvida pelas primitivas existentes. Nesse caso, a decisão exigirá avaliação de bundle, cobertura, keyboard/fallback e ausência de transformação financeira. Não será adicionada por preferência estética.

Alternativa considerada: substituir imediatamente os SVG por uma biblioteca de gráficos. Foi rejeitada porque o projeto já tem gráficos funcionais, não declara dependência dedicada e a mudança poderia alterar escala, acessibilidade e interpretação dos dados sem benefício demonstrado.

### 4. Adaptadores finos para contratos reais

As páginas continuarão usando os módulos atuais em `frontend/src/api/`. A etapa de redesign poderá extrair formatadores e adaptadores de visualização, mas não deverá duplicar chamadas ou relaxar a validação de resposta. Cada visualização deverá declarar sua origem, por exemplo:

| Área | Fonte oficial | Restrições de apresentação |
| --- | --- | --- |
| Dashboard do investidor | `GET /api/dashboard` | patrimônio, investimento, saldo, posições, distribuições, câmbio e avisos; BRL/USD identificados |
| Carteira/posições | `/api/wallet/positions` e snapshot do dashboard | quantidade, preço médio, mercado e corretora conforme resposta; nenhum placeholder de posição |
| Ativos/negociação | `/api/assets/search`, `/api/wallet/purchases`, `/api/wallet/sales` | cotação oficial somente leitura; preço não editável; sucesso somente após resposta |
| Banco/transferências | `/api/wallet`, depósitos, retiradas e transferências | saldo e posição atualizados pela API; confirmação antes de mutar |
| Histórico | `GET /api/history` | no máximo 20 por página; filtros combinados oficiais; somente leitura |
| Corretoras | `/api/brokers` e pesquisa/associação por CNPJ | somente associações próprias ativas e erros funcionais preservados |
| Conta | `/api/accounts/me` e mutações de credencial/ciclo de vida | dados mascarados, sessão e confirmação protegidas |
| Dashboard administrativo | `/api/admin/dashboard` | contas atuais independentes do período; operações e séries limitadas ao período; volumes BRL/USD |
| Investidores administrativos | `/api/admin/investors` e status | dados pessoais mascarados, paginação/filtros oficiais, conflito sem falso sucesso |

### 5. Dois layouts, uma linguagem

`AppLayout` e `AdminLayout` continuarão separados para manter menus e contextos de sessão distintos, mas passarão a compor as mesmas primitivas de shell, cabeçalho, navegação, superfície e estado. O investidor manterá as seções funcionais de Investimentos, Banco e Bolsa e acesso a Conta; o administrador manterá Dashboard e Investidores. Nenhum layout poderá ler o contexto de autenticação do outro.

### 6. Estratégia responsiva e acessível explícita

Usar mobile-first com faixas de composição em torno de 320px, 640px, 900px e 1200px, ajustadas após inspeção visual. O layout deverá evitar overflow horizontal global; tabelas largas usarão composição responsiva documentada ou uma região de rolagem contida com cabeçalho e nome acessíveis. Menus e diálogos deverão suportar Escape, clique externo quando aplicável, foco visível e retorno de foco. A implementação deverá verificar contraste, labels, `aria-live` para estados e não depender de hover.

### 7. Organização da migração por entregas

As páginas existentes serão migradas por fatias verticais, mantendo a aplicação executável após cada etapa. Cada etapa terá testes de componentes e páginas afetadas, lint e build; a etapa final executará a suíte de cobertura com os thresholds existentes, além de inspeção em 320px, tablet e desktop. Não haverá migração em massa sem snapshots/asserções que comprovem estados e contratos.

## Risks / Trade-offs

- **[Risco]** Um CSS global grande contém regras de temas e páginas legadas que podem continuar vencendo os novos tokens. **Mitigação:** introduzir tokens e escopos de layout, migrar por superfície e remover somente regras substituídas após testes, sem apagar alterações não relacionadas.
- **[Risco]** Gráficos visualmente mais sofisticados podem sugerir desempenho não fornecido pela API. **Mitigação:** manter origem e semântica explícitas, usar apenas séries oficiais, oferecer tabela/detalhes e revisar textos contra as specs de dashboards e patrimônio.
- **[Risco]** A densidade de tabelas administrativas e histórico pode degradar em 320px. **Mitigação:** definir comportamento responsivo por tabela, testar teclado/leitor de tela e preservar todos os campos funcionais.
- **[Risco]** A nova estética pode parecer duas aplicações se as telas forem migradas em ritmos diferentes. **Mitigação:** concluir tokens e primitivas antes das páginas, proibir valores visuais ad hoc nas etapas seguintes e revisar ambos os layouts em cada entrega.
- **[Risco]** A cobertura global pode cair com novos componentes e estados defensivos. **Mitigação:** cada tarefa inclui testes para sucesso, loading, vazio, erro, conflito e autorização conforme aplicável; o limiar de 80% não será reduzido.
- **[Risco]** Dependência de uma imagem somente conversacional dificulta comparação pixel a pixel. **Mitigação:** usar a imagem fornecida na conversa como direção; antes da implementação visual, solicitar/registrar um arquivo local se forem necessários testes de referência ou inspeção automatizada.

## Migration Plan

1. Congelar o inventário de rotas, contratos e estados atuais; criar tokens, primitivas e regras de acessibilidade sem alterar chamadas de API.
2. Migrar shell, navegação, autenticação pública e proteção visual das rotas, mantendo os contextos JWT separados.
3. Migrar dashboard e visualizações do investidor, validando patrimônio, investimento, moeda, câmbio e estados sem dados.
4. Migrar carteira, ativos, negociação, banco, transferências, histórico, corretoras e conta/perfil.
5. Migrar login, dashboard e gestão de investidores administrativos, incluindo mascaramento, conflitos e métricas sem dados individuais.
6. Executar regressão visual e funcional em breakpoints, testar teclado/estados, rodar lint, build, suíte e coverage; corrigir apenas problemas introduzidos pelo change.

Rollback é por etapa: cada entrega deve ser revertível no conjunto de componentes/estilos da etapa, sem rollback de backend ou alteração de dados. Como o change não modifica contratos de API, a reversão visual não exige migration ou operação de banco.

## Open Questions

- A imagem de referência está disponível no histórico da conversa, mas não há ainda um arquivo binário local identificado no workspace. Antes de iniciar a implementação, decidir se a comparação visual exigirá que o usuário forneça a imagem como arquivo.
- Durante a etapa 1, confirmar por medição de acessibilidade se o SVG nativo atende todas as interações previstas; somente se não atender será reavaliada uma biblioteca de gráficos.
