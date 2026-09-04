## MODIFIED Requirements

### Requirement: Consistencia do saldo
O sistema SHALL calcular e persistir saldo usando BigDecimal, duas casas e arredondamento HALF_UP. Uma compra confirmada SHALL debitar e uma venda confirmada SHALL creditar integralmente o valor calculado pelo backend no saldo único da conta, e o saldo SHALL permanecer não negativo.

#### Scenario: Venda com saldo creditado
- **WHEN** uma venda for confirmada
- **THEN** o saldo SHALL ser creditado exatamente pelo valor calculado pelo backend e permanecer consistente com a posição, o histórico e o patrimônio

#### Scenario: Falha durante a venda
- **WHEN** qualquer etapa de atualização do saldo, posição, histórico ou patrimônio falhar
- **THEN** o sistema SHALL reverter integralmente o crédito e não manter alteração parcial

#### Scenario: Saldo apos movimentacao
- **WHEN** um aporte ou operação for confirmado
- **THEN** o saldo SHALL refletir a movimentação integralmente sem perda de precisão

#### Scenario: Compra com saldo suficiente
- **WHEN** o valor da compra for menor ou igual ao saldo disponível no momento da confirmação
- **THEN** o sistema SHALL debitar exatamente o valor calculado pelo backend e manter o saldo consistente com a posição e o histórico

#### Scenario: Compra com saldo insuficiente
- **WHEN** o valor da compra exceder o saldo disponível
- **THEN** o sistema SHALL rejeitar a operação, SHALL manter o saldo inalterado e SHALL informar os valores solicitado e disponível conforme o contrato de erro

#### Scenario: Falha durante a compra
- **WHEN** qualquer etapa de atualização do saldo, posição, histórico ou patrimônio falhar
- **THEN** o sistema SHALL reverter integralmente o débito e não manter alteração parcial
