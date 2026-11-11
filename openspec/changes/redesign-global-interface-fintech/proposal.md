## Why

O frontend atual já cobre os fluxos de investidor e administrador, mas a experiência visual está distribuída entre um CSS global extenso, layouts com identidades diferentes e componentes de página pouco sistematizados. Isso dificulta a evolução consistente de telas, gráficos, tabelas, estados assíncronos e acessibilidade. Agora que as capacidades funcionais e administrativas estão consolidadas, é necessário planejar uma modernização visual única, premium e responsiva, inspirada na composição de dashboards fintech fornecida como referência, sem copiar marca, textos ou dados.

## What Changes

- Introduzir um design system visual único para as áreas de investidor e administrador, com tokens de cor, tipografia, espaçamento, superfícies, bordas, sombras, foco e estados.
- Reorganizar a navegação, os layouts e a hierarquia visual das rotas públicas, privadas do investidor e administrativas sem alterar seus contratos de segurança ou autorização.
- Modernizar dashboards, gráficos, cards de métricas, tabelas, filtros, formulários, confirmações, mensagens, estados vazios, carregamento e erro.
- Padronizar a apresentação de dados reais, mantendo separados BRL e USD, distinguindo movimentação financeira de desempenho patrimonial e preservando a semântica administrativa consolidada.
- Garantir responsividade desktop, tablet e mobile, navegação por teclado, foco visível, semântica HTML e suporte a tecnologias assistivas.
- Executar a implementação em seis entregas revisáveis: base visual; navegação/autenticação; dashboard do investidor; demais telas do investidor; telas administrativas; regressão, responsividade e acessibilidade.
- Não alterar APIs, regras financeiras, persistência, migrations, CI, sessões JWT, refresh, CSRF, RBAC, isolamento de dados ou changes arquivados.

## Capabilities

### New Capabilities

- `interface-fintech-global`: contrato visual e de interação compartilhado para todas as telas de investidor e administrador, incluindo design system, layouts, visualizações baseadas em dados oficiais, estados assíncronos, responsividade e acessibilidade.

### Modified Capabilities

Nenhuma. As especificações existentes continuam sendo os contratos funcionais e de segurança; esta capacidade acrescenta requisitos de apresentação e interação sem substituir regras de autenticação, finanças, navegação funcional ou dashboards consolidados.

## Impact

- Frontend React/Vite: `frontend/src/App.jsx`, layouts, páginas, componentes compartilhados, `frontend/src/styles/global.css`, formatadores e testes associados.
- Clientes de API existentes serão apenas consumidos pelos novos componentes; os contratos reais de `dashboard`, `wallet`, `history`, `market`, `brokers`, autenticação e administração permanecem a fonte dos dados exibidos.
- A configuração atual de testes Vitest/Testing Library e seus thresholds globais de 80% para statements, branches, functions e lines será preservada.
- A biblioteca de gráficos atual é SVG/React nativo. A adoção de uma dependência adicional só poderá ser decidida durante a implementação se houver necessidade demonstrada e sem substituir dados oficiais por mocks.
- Nenhum arquivo de backend, migration, CI ou change arquivado será modificado por este planejamento ou pelas etapas visuais previstas.
