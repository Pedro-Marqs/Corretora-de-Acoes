## ADDED Requirements

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
