## MODIFIED Requirements

### Requirement: Associar corretora a conta
O sistema SHALL permitir que a conta autenticada associe corretoras válidas e ativas, SHALL impedir mais de uma associação ativa da mesma corretora na mesma conta e MUST limitar consulta e administração às associações pertencentes ao próprio investidor. Uma compra SHALL aceitar somente uma associação ativa pertencente à conta autenticada.

#### Scenario: Associacao valida
- **WHEN** o investidor autenticado confirmar a associação de uma corretora válida sem associação ativa equivalente
- **THEN** o sistema SHALL criar ou reativar a associação para a própria conta

#### Scenario: Associacao duplicada
- **WHEN** a conta tentar associar novamente a mesma corretora que já possui associação ativa
- **THEN** o sistema SHALL rejeitar a duplicidade sem criar nova associação

#### Scenario: Consultar corretoras ativas
- **WHEN** o investidor autenticado consultar suas corretoras disponíveis
- **THEN** o sistema SHALL retornar somente as associações ativas pertencentes à própria conta

#### Scenario: Compra com corretora não elegível
- **WHEN** o investidor tentar comprar usando corretora inativa, inexistente ou pertencente a outra conta
- **THEN** o sistema SHALL rejeitar a operação sem expor ou modificar dados da corretora

#### Scenario: Isolamento entre contas
- **WHEN** o investidor tentar consultar ou alterar uma associação pertencente a outra conta
- **THEN** o sistema MUST rejeitar a operação sem expor ou modificar os dados da outra conta
