## ADDED Requirements

### Requirement: Estados do shell e dos fluxos reorganizados

O novo shell e suas páginas SHALL apresentar estados distinguíveis de carregamento, sucesso, vazio, erro recuperável, sessão inválida e dados desatualizados quando aplicáveis. Enquanto uma ação estiver em andamento, SHALL impedir duplicidade e SHALL preservar o contexto recuperável em falhas.

#### Scenario: Carregamento de uma seção
- **WHEN** Investimentos, Banco ou Bolsa aguardar dados da API
- **THEN** a interface SHALL indicar carregamento sem apresentar valores financeiros não confirmados

#### Scenario: Falha recuperável
- **WHEN** uma consulta ou ação falhar por erro funcional, rede ou servidor
- **THEN** a interface SHALL mostrar mensagem segura, preservar seleção/entrada aplicável e permitir nova tentativa quando possível

### Requirement: Responsividade e acessibilidade do novo fluxo

Header, menu do usuário, cartões, listas, detalhe e negociação SHALL permanecer utilizáveis a partir de 320 px, sem rolagem horizontal da página, com foco visível, nomes acessíveis e controles de menu operáveis por teclado.

#### Scenario: Visualização móvel
- **WHEN** o investidor acessar qualquer seção em viewport de 320 px ou maior
- **THEN** o conteúdo SHALL permanecer legível e acionável sem sobreposição ou rolagem horizontal da página

#### Scenario: Navegação por teclado
- **WHEN** o investidor navegar pelo header e pelo menu usando teclado
- **THEN** a ordem de foco SHALL ser compreensível, o menu SHALL poder ser aberto/fechado e nenhum controle SHALL ficar inacessível
