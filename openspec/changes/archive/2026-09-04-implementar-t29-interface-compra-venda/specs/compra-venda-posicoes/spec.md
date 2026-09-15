## ADDED Requirements

### Requirement: Consultar posições próprias para operações
O sistema SHALL disponibilizar `GET /api/wallet/positions` somente para investidor autenticado. A resposta SHALL ser derivada da conta da sessão e SHALL retornar o saldo disponível da conta e somente posições abertas pertencentes às associações de corretora dessa conta. Cada posição SHALL incluir identificador opaco do ativo, ticker, nome, mercado, moeda, identificador e nome da corretora, quantidade, preço médio unitário, cotação utilizável quando existente, valor de mercado, lucro ou perda não realizado e os instantes/indicadores de atualidade aplicáveis. Valores dependentes de cotação ou câmbio sem valor utilizável SHALL ser omitidos ou explicitamente indisponíveis, sem cálculo pelo cliente. A interface SHALL usar esse saldo como limite informativo de compra e SHALL exibir dados de posição no modal somente quando existir posição aberta correspondente ao ativo selecionado; não SHALL renderizar placeholders de posição para ativo sem posição.

#### Scenario: Consultar carteira com posição
- **WHEN** o investidor autenticado solicitar suas posições
- **THEN** o sistema SHALL retornar HTTP 200 com o saldo disponível e os dados completos de cada posição aberta da própria conta, incluindo quantidade, preço médio e resultado não realizado quando houver dados de mercado utilizáveis

#### Scenario: Conta sem posições
- **WHEN** o investidor autenticado não possuir posições abertas
- **THEN** o sistema SHALL retornar HTTP 200 com saldo disponível e lista de posições vazia

#### Scenario: Posição com cotação indisponível
- **WHEN** uma posição aberta não possuir cotação ou câmbio utilizável
- **THEN** o sistema SHALL manter a posição e seus dados de custo na resposta, indicar a indisponibilidade aplicável e SHALL NOT inventar valor de mercado ou lucro/perda

#### Scenario: Sessão ausente
- **WHEN** a solicitação ocorrer sem sessão autenticada
- **THEN** o sistema SHALL rejeitar com o contrato uniforme de autenticação e SHALL NOT retornar saldo ou posições

#### Scenario: Isolamento de conta
- **WHEN** uma conta consultar suas posições
- **THEN** a resposta SHALL excluir posições, corretoras, ativos ou saldos pertencentes a qualquer outra conta

#### Scenario: Ativo sem posição selecionado para compra
- **WHEN** o investidor selecionar por pesquisa um ativo que não esteja entre as posições abertas
- **THEN** o modal SHALL continuar exibindo o saldo disponível recebido no snapshot como limite informativo para compra, SHALL NOT exibir quantidade, preço médio ou resultado como placeholders de posição e SHALL permitir a validação final da compra pelo backend
