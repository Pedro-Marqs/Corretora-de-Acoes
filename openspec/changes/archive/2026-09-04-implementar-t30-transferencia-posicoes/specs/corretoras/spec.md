## ADDED Requirements

### Requirement: Restringir corretoras da transferência

Uma transferência SHALL aceitar somente duas associações ativas, distintas e pertencentes à conta autenticada; a situação usada para decidir a operação SHALL ser revalidada no processamento.

#### Scenario: Duas associações próprias ativas

- **WHEN** origem e destino forem associações ativas da conta autenticada e diferentes
- **THEN** o sistema SHALL permitir a validação da transferência somente para a posição vinculada à origem e ao ativo solicitado

#### Scenario: Associação removida durante a tentativa

- **WHEN** uma associação deixar de estar ativa antes da confirmação transacional
- **THEN** o sistema SHALL rejeitar a transferência sem modificar posições, saldo ou histórico
