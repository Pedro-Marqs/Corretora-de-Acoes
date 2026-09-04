## ADDED Requirements

### Requirement: Manter saldo inalterado na transferência

A transferência de posição SHALL deixar exatamente inalterado o saldo único da conta, não SHALL criar débito, crédito, aporte ou movimentação de saldo e SHALL manter esse saldo no ponto patrimonial produzido pela operação.

#### Scenario: Transferência sem liquidação

- **WHEN** uma transferência parcial ou total for concluída
- **THEN** o saldo antes e depois SHALL ser igual, enquanto somente a localização, quantidade e custo das posições forem atualizados

#### Scenario: Falha na transferência

- **WHEN** a transferência for rejeitada ou sofrer rollback
- **THEN** o saldo SHALL permanecer igual ao saldo anterior e nenhum registro de saldo SHALL ser criado
