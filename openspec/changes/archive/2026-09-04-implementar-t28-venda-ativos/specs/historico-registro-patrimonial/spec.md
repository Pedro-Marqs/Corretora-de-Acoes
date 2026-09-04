## MODIFIED Requirements

### Requirement: Historico de movimentacoes
O sistema SHALL registrar operações, aportes, transferências e ajustes com conta, instante, valores e origem. Uma venda concluída SHALL registrar ao menos o tipo da operação, conta, ativo, corretora, quantidade, preço determinado pelo backend, valor financeiro, resultado realizado e moeda/conversão aplicável. Atualizações automáticas de cotações ou câmbio, sem operação financeira, SHALL NOT criar movimentações.

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
- **WHEN** o investidor autenticado consultar seu historico
- **THEN** o sistema SHALL retornar registros ordenados e pertencentes somente à sua conta

#### Scenario: Atualizacao de mercado sem movimentacao
- **WHEN** um ciclo automatico atualizar cotacoes ou cambio
- **THEN** o sistema SHALL manter o historico de movimentacoes inalterado

### Requirement: Registro patrimonial
O sistema SHALL registrar pontos patrimoniais consistentes com saldo e posições em instante controlado. Uma venda concluída SHALL gerar o ponto com o saldo creditado, as posições resultantes, o câmbio utilizado quando aplicável e o valor total calculado no mesmo processamento. Atualizações isoladas de cotação ou câmbio SHALL NOT gerar ponto patrimonial.

#### Scenario: Gerar ponto patrimonial após venda
- **WHEN** ocorrer o processamento bem-sucedido de uma venda
- **THEN** o sistema SHALL persistir um ponto patrimonial consistente com o novo saldo e as posições resultantes

#### Scenario: Venda com falha de registro
- **WHEN** falhar o registro da movimentação ou do ponto patrimonial durante uma venda
- **THEN** o sistema SHALL reverter também o crédito e a alteração da posição

#### Scenario: Gerar ponto patrimonial após compra
- **WHEN** ocorrer o processamento bem-sucedido de uma compra
- **THEN** o sistema SHALL persistir um ponto patrimonial consistente com o novo saldo e as posições resultantes

#### Scenario: Gerar ponto patrimonial
- **WHEN** ocorrer o processamento de um ponto patrimonial
- **THEN** o sistema SHALL persistir saldo, posicoes, cambio e valor total usados no calculo

#### Scenario: Compra com falha de registro
- **WHEN** falhar o registro da movimentação ou do ponto patrimonial durante uma compra
- **THEN** o sistema SHALL reverter também o débito e a alteração da posição

#### Scenario: Atualizacao de mercado sem ponto patrimonial
- **WHEN** um ciclo automatico atualizar somente cotacoes ou cambio
- **THEN** o sistema SHALL deixar inalterados os pontos patrimoniais persistidos
