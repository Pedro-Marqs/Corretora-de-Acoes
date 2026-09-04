## ADDED Requirements

### Requirement: Registrar transferência e patrimônio atomicamente

Uma transferência concluída SHALL criar exatamente uma movimentação imutável do tipo transferência, vinculada à conta, ativo, origem, destino, quantidade, custo total transferido, preço médio de origem e instante da operação. SHALL criar também um ponto patrimonial pós-transferência consistente com o saldo inalterado e as posições resultantes.

#### Scenario: Transferência concluída

- **WHEN** origem e destino forem atualizados com sucesso
- **THEN** o histórico SHALL conter os dados efetivamente transferidos, o ponto patrimonial SHALL refletir a nova distribuição e nenhum resultado realizado SHALL ser criado

#### Scenario: Transferência rejeitada

- **WHEN** qualquer validação da transferência falhar
- **THEN** o sistema SHALL não criar movimentação nem ponto patrimonial

#### Scenario: Falha ao registrar transferência

- **WHEN** falhar a persistência da movimentação ou do ponto patrimonial
- **THEN** o sistema SHALL reverter também as alterações das posições e não SHALL deixar evento parcial
