# Interface e estados Specification

## Purpose

Oferecer uma interface responsiva e explicita sobre estados de carregamento, vazio, sucesso, erro e dados desatualizados.

## Requirements

### Requirement: Estados de interface

A interface SHALL apresentar estados de carregamento, vazio, sucesso, erro e desatualizado de forma distinguivel e MUST impedir o reenvio acidental de uma mesma acao enquanto sua solicitacao estiver em andamento.

#### Scenario: Falha de requisicao

- **WHEN** uma requisicao falhar
- **THEN** a interface SHALL exibir mensagem funcional, preservar contexto e permitir nova tentativa quando aplicavel

#### Scenario: Nenhum resultado

- **WHEN** uma consulta valida nao retornar dados
- **THEN** a interface SHALL exibir estado vazio sem tratar como erro

#### Scenario: Solicitacao em andamento

- **WHEN** uma acao estiver aguardando resposta da API
- **THEN** a interface SHALL indicar o estado de carregamento e MUST impedir novo envio da mesma acao ate a solicitacao terminar

### Requirement: Responsividade e formatacao

A interface SHALL funcionar em desktop, tablet e celular sem rolagem horizontal e exibir dinheiro com duas casas.

#### Scenario: Visualizacao em celular

- **WHEN** o investidor acessar uma tela em viewport estreito
- **THEN** o conteudo SHALL permanecer utilizavel sem sobreposicao ou rolagem horizontal da pagina

### Requirement: Confirmacao de movimentacao financeira

A interface MUST solicitar confirmacao simples antes de executar uma movimentacao financeira iniciada pelo investidor, sem exigir uma pagina separada de resumo.

#### Scenario: Confirmar aporte

- **WHEN** o investidor preencher um aporte valido e solicitar sua execucao
- **THEN** a interface SHALL apresentar uma confirmacao simples antes de enviar a operacao

#### Scenario: Cancelar confirmacao

- **WHEN** o investidor cancelar a confirmacao de uma movimentacao
- **THEN** a interface MUST NOT enviar a operacao e SHALL preservar o estado necessario para que o usuario possa continuar na mesma tela

### Requirement: Estados da pesquisa de ativos

A interface SHALL apresentar a pesquisa de ativos como uma tela privada, com estados visualmente distinguíveis de carregamento, resultado, vazio, erro e dados desatualizados. Enquanto a consulta estiver em andamento, SHALL impedir novo envio da mesma pesquisa; em falhas recuperáveis, SHALL manter o ticker informado e permitir nova tentativa.

#### Scenario: Consulta em andamento
- **WHEN** o investidor enviar um ticker e a API ainda não tiver respondido
- **THEN** a interface SHALL indicar carregamento e SHALL impedir reenvio acidental da mesma consulta até a resposta terminar

#### Scenario: Consulta sem resultado
- **WHEN** uma pesquisa válida não retornar um ativo
- **THEN** a interface SHALL exibir um estado vazio compreensível e SHALL NOT tratar a ausência de resultado como sucesso com dados ou como erro técnico

#### Scenario: Erro funcional ou indisponibilidade
- **WHEN** a API retornar ativo inválido, mercado rejeitado, resposta incompleta, ausência de cache ou falha de conexão
- **THEN** a interface SHALL exibir mensagem funcional sem stack trace, classe interna, SQL, credencial ou corpo técnico e SHALL permitir nova tentativa quando aplicável

#### Scenario: Sessão inválida
- **WHEN** a consulta ocorrer sem sessão válida ou a API responder que a sessão foi encerrada
- **THEN** a interface SHALL evitar exibir dados privados de mercado associados à área autenticada e SHALL direcionar o investidor ao login conforme a proteção de rotas existente

#### Scenario: Pesquisa responsiva
- **WHEN** o investidor acessar a tela em viewport de desktop, tablet ou celular
- **THEN** o formulário, resultado, avisos e mensagens SHALL permanecer utilizáveis sem rolagem horizontal da página e os valores monetários SHALL usar duas casas decimais

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

### Requirement: Estados da interface de histórico

A tela privada de histórico SHALL apresentar estados distinguíveis de carregamento, resultados, vazio e erro recuperável. A tela SHALL preservar o contexto de filtros quando aplicável, impedir consultas duplicadas enquanto uma solicitação equivalente estiver em andamento e direcionar o investidor ao login quando a sessão deixar de ser válida.

#### Scenario: Carregar histórico
- **WHEN** a tela solicitar uma página do histórico
- **THEN** SHALL indicar carregamento, impedir nova solicitação duplicada da mesma consulta e evitar apresentar como atuais os resultados ainda não confirmados

#### Scenario: Nenhum registro
- **WHEN** uma consulta válida não retornar movimentações
- **THEN** SHALL exibir estado vazio compreensível, sem tratá-lo como erro e sem apresentar paginação de páginas inexistentes

#### Scenario: Erro recuperável
- **WHEN** a API retornar falha de rede, erro funcional ou erro de servidor
- **THEN** SHALL exibir mensagem funcional sem stack trace, SQL, corpo técnico ou dados sensíveis, preservar os filtros aplicáveis e permitir nova tentativa

#### Scenario: Sessão inválida
- **WHEN** a consulta retornar HTTP 401
- **THEN** SHALL remover ou ocultar os dados privados do histórico e direcionar o investidor ao login conforme a proteção de rotas existente

#### Scenario: Histórico em viewport estreito
- **WHEN** o investidor acessar a tela em 320 px, tablet ou desktop
- **THEN** filtros, registros, mensagens e paginação SHALL permanecer utilizáveis sem rolagem horizontal da página, com datas e valores formatados conforme os padrões da aplicação

### Requirement: Estados da interface do dashboard

A tela privada do dashboard SHALL distinguir carregamento, sucesso, vazio, erro recuperável, sessão inválida e dados desatualizados. Enquanto uma consulta equivalente estiver em andamento, SHALL impedir novo envio duplicado e SHALL preservar os filtros selecionados quando a falha permitir nova tentativa.

#### Scenario: Carregamento do dashboard

- **WHEN** a tela solicitar indicadores, distribuições ou série histórica
- **THEN** SHALL indicar carregamento e SHALL evitar apresentar a resposta anterior como se fosse o resultado confirmado da nova seleção

#### Scenario: Erro recuperável

- **WHEN** a consulta falhar por rede, erro funcional ou erro de servidor
- **THEN** SHALL exibir mensagem funcional sem detalhes técnicos, preservar os filtros aplicáveis e permitir nova tentativa

#### Scenario: Sessão inválida

- **WHEN** a API responder HTTP 401
- **THEN** SHALL ocultar os dados privados do dashboard e direcionar o investidor ao login conforme a proteção de rotas existente

#### Scenario: Consulta sem dados

- **WHEN** a consulta válida retornar posições, distribuições ou histórico vazios
- **THEN** SHALL apresentar estado vazio específico da seção sem tratar a ausência como erro

### Requirement: Responsividade do dashboard

A tela de dashboard SHALL funcionar em viewport de 320 px, tablet e desktop sem rolagem horizontal da página. Cartões, filtros, distribuições, avisos e histórico SHALL permanecer acessíveis e os valores monetários SHALL usar duas casas decimais.

#### Scenario: Dashboard em celular

- **WHEN** o investidor acessar o dashboard em viewport estreito
- **THEN** SHALL conseguir ler e operar os filtros e seções sem sobreposição ou rolagem horizontal da página

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
