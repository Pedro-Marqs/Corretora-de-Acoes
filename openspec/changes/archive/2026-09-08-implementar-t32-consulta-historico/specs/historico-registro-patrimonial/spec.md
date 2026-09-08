## MODIFIED Requirements

### Requirement: Historico de movimentacoes
O sistema SHALL registrar operações, aportes, transferências e ajustes com conta, instante, valores e origem. Uma venda concluída SHALL registrar ao menos o tipo da operação, conta, ativo, corretora, quantidade, preço determinado pelo backend, valor financeiro, resultado realizado e moeda/conversão aplicável. Atualizações automáticas de cotações ou câmbio, sem operação financeira, SHALL NOT criar movimentações. O sistema SHALL disponibilizar a consulta somente leitura do histórico da conta autenticada por `GET /api/history`, ordenando os registros do mais recente para o mais antigo e limitando cada página a 20 registros. A consulta SHALL aceitar filtros combináveis de intervalo temporal, tipo de movimentação, ticker, corretora e mercado, além de página solicitada, e SHALL retornar metadados suficientes para navegação, incluindo página atual, tamanho efetivo, total de registros e total de páginas.

#### Scenario: Registrar venda concluída
- **WHEN** uma venda for efetivada
- **THEN** o sistema SHALL persistir uma movimentação imutável vinculada à conta autenticada, contendo os dados financeiros efetivamente usados e o resultado realizado

#### Scenario: Venda rejeitada
- **WHEN** uma venda for rejeitada antes da conclusão
- **THEN** o sistema SHALL não criar movimentação

#### Scenario: Registrar compra concluída
- **WHEN** uma compra for efetivada
- **THEN** o sistema SHALL persistir uma movimentação imutável vinculada à conta autenticada, contendo os dados financeiros efetivamente usados

#### Scenario: Compra rejeitada
- **WHEN** uma compra for rejeitada antes da conclusão
- **THEN** o sistema SHALL não criar movimentação

#### Scenario: Consultar historico proprio
- **WHEN** o investidor autenticado consultar `GET /api/history`
- **THEN** o sistema SHALL retornar somente movimentações pertencentes à sua conta, em ordem decrescente de instante, com no máximo 20 registros na página solicitada e metadados de paginação

#### Scenario: Filtrar historico
- **WHEN** o investidor autenticado consultar o histórico com um ou mais filtros válidos de intervalo, tipo, ticker, corretora ou mercado
- **THEN** o sistema SHALL aplicar todos os filtros simultaneamente antes da paginação e SHALL retornar somente registros que atendam a todos eles

#### Scenario: Pagina inexistente
- **WHEN** o investidor autenticado solicitar uma página válida sem registros correspondentes
- **THEN** o sistema SHALL retornar uma lista vazia com metadados consistentes, sem tratar a consulta como erro

#### Scenario: Parametro de consulta invalido
- **WHEN** a consulta receber página, intervalo, tipo, ticker, corretora ou mercado em formato inválido ou fora das restrições definidas
- **THEN** o sistema SHALL rejeitar a requisição com o contrato uniforme de erro de validação e SHALL NOT alterar qualquer registro

#### Scenario: Consulta sem autenticacao
- **WHEN** uma requisição para `GET /api/history` não possuir sessão autenticada válida
- **THEN** o sistema SHALL rejeitar a consulta com o comportamento uniforme de autenticação e SHALL NOT revelar registros ou metadados da conta

#### Scenario: Atualizacao de mercado sem movimentacao
- **WHEN** um ciclo automatico atualizar cotacoes ou cambio
- **THEN** o sistema SHALL manter o historico de movimentacoes inalterado
