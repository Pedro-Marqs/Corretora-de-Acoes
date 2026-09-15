## ADDED Requirements

### Requirement: Apresentar snapshot de posições na operação
A interface privada de compra e venda SHALL consultar o snapshot autenticado de saldo e posições antes de apresentar a carteira. SHALL exibir estados distinguíveis de carregamento, lista, vazio e erro; cada item aberto SHALL apresentar, quando fornecidos, ativo, corretora, quantidade, preço médio e lucro/perda, sem recalcular valores oficiais. A seleção de um item SHALL abrir o mesmo contexto de operação usado pela pesquisa de ticker.

#### Scenario: Lista de posições carregada
- **WHEN** a consulta autenticada retornar posições abertas
- **THEN** a interface SHALL listar os itens com os valores recebidos pelo backend e SHALL permitir selecionar um item para abrir o modal correspondente

#### Scenario: Carteira vazia
- **WHEN** a consulta retornar lista vazia
- **THEN** a interface SHALL exibir estado vazio e SHALL manter disponível a pesquisa de ticker para iniciar uma operação

#### Scenario: Erro ao carregar posições
- **WHEN** a consulta falhar de forma recuperável
- **THEN** a interface SHALL exibir mensagem funcional, não expor detalhes técnicos e permitir nova tentativa sem inventar posições ou saldo

#### Scenario: Sessão inválida na leitura
- **WHEN** a consulta de posições retornar 401
- **THEN** a interface SHALL remover a apresentação de dados privados e direcionar o investidor ao login conforme a proteção de rotas

### Requirement: Operar com dados de posição somente leitura
A interface SHALL apresentar no modal o saldo e a cotação recebidos pelos contratos e SHALL apresentar quantidade, preço médio e lucro/perda somente quando houver posição aberta do ativo selecionado na carteira. Para ativo sem posição, SHALL omitir esses campos em vez de renderizar placeholders. SHALL manter preço, saldo, posição, preço médio e resultado fora da autoridade do usuário e SHALL NOT usar o snapshot como simulação, reserva ou garantia de aceite. Após uma operação bem-sucedida, SHALL substituir os dados pelo retorno da operação ou por nova leitura autenticada.

#### Scenario: Snapshot selecionado
- **WHEN** o investidor selecionar uma posição da lista
- **THEN** o modal SHALL exibir o ativo e a corretora correspondentes, mantendo os valores financeiros como somente leitura

#### Scenario: Dados alterados no backend
- **WHEN** a operação for rejeitada porque saldo, posição, ativo, corretora ou cotação mudou após a leitura
- **THEN** a interface SHALL exibir o erro funcional retornado, preservar contexto recuperável e SHALL NOT declarar alteração local bem-sucedida

#### Scenario: Ativo sem posição não cria bloco de posição
- **WHEN** o investidor pesquisar um ativo que não possua posição aberta na carteira
- **THEN** o modal SHALL exibir o saldo disponível do snapshot como limite informativo para compra, SHALL NOT exibir campos de quantidade, preço médio ou lucro/perda da posição e SHALL manter as ações e a validação final existentes

### Requirement: Aplicar direção visual azul global
A interface SHALL usar uma direção visual azul consistente em páginas públicas e privadas, incluindo fundos, superfícies, navegação, campos, botões, cartões e modais. A mudança SHALL preservar contraste legível, responsividade sem rolagem horizontal, estados de carregamento/vazio/erro/alerta/sucesso e as cores semânticas necessárias para ganho, perda e ações destrutivas.

#### Scenario: Rotas públicas e privadas com tema azul
- **WHEN** o investidor navegar entre cadastro, login, conta, carteira, ativos e operações
- **THEN** os elementos recorrentes SHALL apresentar a paleta azul definida para a aplicação, sem permanecerem verdes por herança de estilos anteriores

#### Scenario: Estados semânticos preservados
- **WHEN** a interface exibir erro, alerta, sucesso, ganho, perda ou ação destrutiva sob o tema azul
- **THEN** o estado SHALL continuar distinguível por cor, texto e/ou estrutura, com contraste e foco visível adequados

#### Scenario: Tema responsivo
- **WHEN** qualquer rota for visualizada em 320 px, tablet ou desktop
- **THEN** o tema SHALL permanecer utilizável, sem sobreposição ou rolagem horizontal e sem quebrar os fluxos existentes
