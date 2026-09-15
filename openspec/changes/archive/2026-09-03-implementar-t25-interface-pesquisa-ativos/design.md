## Context

O backend da T23 já é a fonte de verdade para catálogo, cotação, fallback, conversão USD/BRL e indicação de desatualização. O frontend possui roteamento privado, cliente HTTP comum, componentes de estados assíncronos e formatadores compartilhados; a nova tela deve reutilizar essas fundações e o contrato existente, sem duplicar regras financeiras.

See `proposal.md` - Why e `specs/ativos-cotacoes-cambio/spec.md` e `specs/interface-estados/spec.md`.

## Goals / Non-Goals

**Goals:**

- Integrar uma página privada de pesquisa à navegação da área autenticada.
- Encapsular a consulta de ativos em um serviço frontend que preserve o contrato e normalize somente a apresentação.
- Representar explicitamente os estados assíncronos e os avisos independentes de cotação e câmbio.
- Reutilizar formatação de moeda, data/hora e mensagens de erro já existentes.
- Cobrir com testes os mercados, conversão, limites de idade, falhas e responsividade relevante.

**Non-Goals:**

- Alterar endpoint, persistência, adapters ou scheduler do backend.
- Calcular cotação, conversão, idade ou resultado financeiro no frontend.
- Criar atualização manual, compra, venda, confirmação financeira ou resumo de operação.
- Adicionar dependência de dados externos ao navegador.

## Decisions

### 1. Página privada integrada ao layout existente

A pesquisa ficará em uma rota da área autenticada e será acessível pela navegação já usada pelas telas privadas. Isso mantém a regra de sessão centralizada no roteamento e evita renderizar dados de mercado dentro das páginas públicas. Uma tela pública foi rejeitada porque a T25 depende do fluxo privado definido para a carteira.

### 2. Serviço frontend dedicado sobre o cliente HTTP comum

Um módulo de mercado será responsável por enviar o ticker ao endpoint já disponibilizado pela T23 e devolver o resultado ou o erro normalizado ao componente. O componente não conhecerá detalhes de `fetch`, CSRF ou URL base. Criar chamadas diretas na página foi rejeitado por duplicar infraestrutura e dificultar os testes.

### 3. Estado explícito por consulta

A página manterá o ticker editável, o estado da solicitação e o último resultado somente enquanto a tela estiver ativa. Uma nova pesquisa substituirá o resultado apenas após resposta válida; durante a solicitação o controle de envio será desabilitado. Falhas manterão o contexto pesquisado para retry e não serão confundidas com estado vazio.

### 4. Apresentação orientada pelos metadados do backend

Os campos e flags retornados pela API serão exibidos diretamente: identificação, mercado, moeda, cotação, valores USD/BRL quando aplicáveis, instantes e avisos de atualidade. A interface não inferirá idade, recalculará BRL nem escolherá fallback. Assim, arredondamento e decisão sobre valor utilizável permanecem no backend.

### 5. Componentes compartilhados para estados e acessibilidade

Carregamento, vazio e erro reutilizarão os componentes comuns existentes, com mensagens específicas para pesquisa. O resultado e seus avisos terão associação semântica clara entre rótulo e valor, e a disposição será adaptada aos breakpoints já adotados pelo projeto. Isso evita uma implementação visual paralela e reduz regressões nas telas anteriores.

## Risks / Trade-offs

- **[Contrato de resposta divergente]** → Confirmar no código e nos testes da T23 os nomes e a forma dos campos antes de implementar o serviço; cobrir ausência de campos como erro funcional.
- **[Usuário interpretar cache como cotação atual]** → Exibir sempre o horário retornado e o aviso explícito de desatualização, sem ocultar o valor armazenado.
- **[Estado anterior aparecer após uma nova pesquisa]** → Associar resultado e erro à solicitação corrente e limpar ou substituir o resultado somente conforme a resposta correspondente.
- **[Layout estreito quebrar com valores USD/BRL]** → Usar composição responsiva e verificar pelo menos 320 px, tablet e desktop sem criar largura mínima que provoque rolagem horizontal.
- **[Falha 401 expor conteúdo parcial]** → Delegar a resposta ao mecanismo de rota/sessão existente e não renderizar dados novos enquanto a autenticação estiver sendo revalidada.

## Migration Plan

Não há migração de banco, endpoint ou dado. A entrega consiste em arquivos do frontend e seus testes; a reversão é a remoção da rota, componentes e serviço da T25, mantendo intactos o catálogo e o cache implementados pela T23.

## Open Questions

Nenhuma. A rota visual exata e a decomposição dos componentes podem ser escolhidas durante a implementação sem alterar o comportamento especificado.
