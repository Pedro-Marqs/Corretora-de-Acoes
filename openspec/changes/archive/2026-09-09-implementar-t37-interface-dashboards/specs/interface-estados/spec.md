## ADDED Requirements

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
