## MODIFIED Requirements

### Requirement: Consistencia do saldo
O sistema SHALL calcular e persistir saldo usando BigDecimal, duas casas e arredondamento HALF_UP. Uma compra confirmada SHALL debitar integralmente o valor calculado pelo backend do saldo único da conta, e o saldo SHALL permanecer não negativo.

#### Scenario: Saldo apos movimentacao
- **WHEN** um aporte ou operacao for confirmado
- **THEN** o saldo SHALL refletir a movimentacao integralmente sem perda de precisao

#### Scenario: Compra com saldo suficiente
- **WHEN** o valor da compra for menor ou igual ao saldo disponível no momento da confirmação
- **THEN** o sistema SHALL debitar exatamente o valor calculado pelo backend e manter o saldo consistente com a posição e o histórico

#### Scenario: Compra com saldo insuficiente
- **WHEN** o valor da compra exceder o saldo disponível
- **THEN** o sistema SHALL rejeitar a operação, SHALL manter o saldo inalterado e SHALL informar os valores solicitado e disponível conforme o contrato de erro

#### Scenario: Falha durante a compra
- **WHEN** qualquer etapa de atualização do saldo, posição, histórico ou patrimônio falhar
- **THEN** o sistema SHALL reverter integralmente o débito e não manter alteração parcial
