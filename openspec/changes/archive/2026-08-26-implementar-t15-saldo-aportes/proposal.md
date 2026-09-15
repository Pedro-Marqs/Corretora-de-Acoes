## Why

A infraestrutura de histórico e patrimônio já permite registrar movimentações financeiras de forma consistente, mas o investidor ainda não possui operações próprias para consultar seu saldo e realizar aportes. A T15 implementa esse fluxo garantindo consistência monetária, atomicidade e separação entre aporte de capital e rendimento dos investimentos.

## What Changes

- Disponibilizar consulta do saldo único da conta autenticada.
- Implementar aporte fictício em reais com valor mínimo de R$ 10,00.
- Atualizar o saldo utilizando `BigDecimal`, duas casas decimais e arredondamento `HALF_UP`.
- Registrar o aporte e o ponto patrimonial na mesma transação utilizando a infraestrutura criada na T14.
- Garantir rollback integral caso saldo, histórico ou patrimônio não possam ser persistidos.
- Permitir aporte mesmo quando a conta ainda não possuir corretoras associadas.
- Garantir que valores aportados aumentem saldo e patrimônio, mas não sejam contabilizados como lucro, prejuízo ou valorização.
- Impedir consulta ou alteração do saldo de outra conta.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `saldo-aportes`: alinhar a capability permanente aos requisitos já definidos em `docs/spec/saldo-aportes.md`, especificando o aporte mínimo de R$ 10,00 e que aportes representam entrada de capital, não rendimento dos investimentos.

## Impact

- Backend Spring Boot relacionado a saldo e carteira.
- Novos endpoints autenticados para consulta de saldo e realização de aporte.
- Serviço responsável pela operação de aporte.
- Persistência do saldo da conta.
- Integração com a infraestrutura de histórico e patrimônio criada na T14.
- Cálculos futuros de lucro, prejuízo e valorização.
- Testes de API, serviço, transação e autorização.
- Main spec `saldo-aportes`, que receberá delta para refletir integralmente os requisitos documentais existentes.