## MODIFIED Requirements

### Requirement: Transferir posicao

O sistema SHALL validar origem, destino, ativo e quantidade antes de efetivar uma transferencia. A transferencia SHALL reduzir da origem a quantidade transferida e seu custo pelo preco medio vigente, e SHALL creditar no destino a mesma quantidade e o mesmo custo total, sem alterar o saldo.

#### Scenario: Transferencia valida

- **WHEN** o investidor transferir quantidade disponivel entre corretoras validas
- **THEN** o sistema SHALL reduzir a origem, creditar o destino, preservar o custo transferido e registrar um evento atomico

#### Scenario: Transferencia para destino com posicao

- **WHEN** o destino possuir 5 unidades a R$ 30,00 e receber 5 unidades com custo unitario de R$ 20,00
- **THEN** o destino SHALL conter 10 unidades a preco medio de R$ 25,00

#### Scenario: Transferencia total

- **WHEN** toda a quantidade da origem for transferida
- **THEN** a origem SHALL ficar sem posicao aberta e o custo total creditado no destino SHALL ser igual ao custo retirado da origem

#### Scenario: Transferencia invalida

- **WHEN** origem, destino, ativo ou quantidade forem invalidos
- **THEN** o sistema SHALL rejeitar a transferencia sem alterar as posicoes, o saldo ou o custo registrado

## ADDED Requirements

### Requirement: Preservar resultado na transferencia

O sistema SHALL tratar a transferencia como movimentacao de custo entre corretoras, sem gerar resultado realizado, valorizacao ou lucro por si só.

#### Scenario: Transferencia sem resultado

- **WHEN** uma posicao for transferida entre corretoras sem mudança de quantidade ou custo total
- **THEN** os indicadores de resultado SHALL permanecer inalterados e somente a distribuição por corretora SHALL refletir a nova localização
