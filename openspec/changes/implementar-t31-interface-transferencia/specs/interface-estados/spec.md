## ADDED Requirements

### Requirement: Estados do fluxo de transferencia

A interface privada de transferencia SHALL apresentar estados distinguiveis de carregamento, lista de posicoes, vazio, sucesso e erro. Enquanto a leitura de posicoes ou o envio estiverem em andamento, MUST impedir a solicitacao duplicada correspondente; falhas recuperaveis SHALL preservar contexto e permitir nova tentativa quando aplicavel.

#### Scenario: Carregamento da carteira para transferencia

- **WHEN** a interface consultar as posicoes e corretoras da conta autenticada
- **THEN** SHALL indicar carregamento e SHALL impedir novo carregamento duplicado ate a resposta terminar

#### Scenario: Carteira sem posicoes abertas

- **WHEN** a consulta autenticada retornar nenhuma posicao aberta
- **THEN** SHALL exibir estado vazio compreensivel, sem apresentar uma transferencia pronta ou inventar quantidade

#### Scenario: Falha recuperavel na leitura

- **WHEN** a consulta de posicoes ou corretoras falhar de forma recuperavel
- **THEN** SHALL exibir mensagem funcional sem stack trace, SQL ou dados sensiveis, preservar filtros ou selecao aplicavel e permitir nova tentativa

#### Scenario: Envio em andamento

- **WHEN** o investidor confirmar uma transferencia e a API ainda nao tiver respondido
- **THEN** SHALL indicar envio em andamento e MUST impedir novo envio da mesma operacao

#### Scenario: Sessao invalida

- **WHEN** a leitura ou o envio retornar HTTP 401
- **THEN** SHALL remover dados privados apresentados e direcionar o investidor ao login conforme a protecao de rotas existente

### Requirement: Responsividade da transferencia

A tela de transferencia SHALL permanecer utilizavel em desktop, tablet e celular, sem sobreposicao ou rolagem horizontal da pagina, e SHALL exibir quantidades e valores monetarios no formato definido pela interface.

#### Scenario: Transferencia em viewport estreito

- **WHEN** o investidor acessar o fluxo em viewport de 320 px ou maior
- **THEN** formulário, seletores, confirmacao, mensagens e lista SHALL permanecer acessiveis sem rolagem horizontal da pagina
