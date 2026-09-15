## ADDED Requirements

### Requirement: Coordenar transferência com operações da posição

O sistema SHALL coordenar transferência, compra e venda concorrentes sobre a mesma posição de modo que cada operação observe estado confirmado e nenhuma quantidade ou custo seja perdido ou duplicado.

#### Scenario: Venda concorrente após transferência total

- **WHEN** uma venda e uma transferência total disputarem simultaneamente a posição da mesma corretora
- **THEN** somente a operação que observar quantidade disponível poderá consumir cada unidade, e a outra SHALL ser concluída sobre o estado serializado ou rejeitada sem alteração parcial

#### Scenario: Compra concorrente no destino

- **WHEN** uma compra e uma transferência creditarem simultaneamente o mesmo ativo e destino
- **THEN** a posição final SHALL refletir ambas as operações efetivadas com custo total e preço médio calculados pela regra financeira, sem sobrescrever a contribuição da outra operação
