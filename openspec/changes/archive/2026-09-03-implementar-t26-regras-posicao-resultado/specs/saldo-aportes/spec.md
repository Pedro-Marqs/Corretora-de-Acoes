## MODIFIED Requirements

### Requirement: Aporte não representa rendimento

O sistema SHALL tratar aportes e saldo inicial como entrada de capital do investidor e MUST NOT contabilizá-los como lucro, prejuízo, valorização não realizada ou resultado total dos investimentos.

#### Scenario: Aporte sem resultado de investimentos

- **WHEN** um aporte for concluído em uma conta sem resultado de investimentos
- **THEN** saldo e patrimônio SHALL aumentar pelo valor correspondente, enquanto lucro, prejuízo, valorização e resultado total SHALL permanecer inalterados

#### Scenario: Aporte com investimentos

- **WHEN** uma conta com posições receber um aporte sem alteração de preço ou quantidade
- **THEN** somente o saldo e o patrimônio SHALL aumentar pelo aporte, mantendo inalterados o resultado realizado, a valorização não realizada e o resultado total
