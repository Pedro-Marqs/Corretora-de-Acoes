## 1. Regra de saldo e aporte

- [x] 1.1 Adicionar à entidade `Account` uma operação explícita para creditar aportes com `BigDecimal`, duas casas decimais e `HALF_UP`, mantendo a regra de valor mínimo de R$ 10,00 no caso de uso; verificar com testes que valores válidos atualizam corretamente o saldo e valores ausentes, zero, negativos ou inferiores ao mínimo não produzem alteração.

## 2. Caso de uso transacional

- [x] 2.1 Implementar o serviço de carteira para consultar o saldo da conta autenticada e realizar aporte em uma única transação, atualizando primeiro o saldo e reutilizando `FinancialHistoryService` com `Movement.deposit(...)`; verificar com testes que um aporte válido atualiza saldo, cria exatamente uma movimentação `DEPOSIT` e um ponto patrimonial com o estado posterior à operação.

## 3. API autenticada de carteira

- [x] 3.1 Criar os DTOs e endpoints autenticados para consulta do saldo e realização de aporte, derivando a conta exclusivamente de `AccountPrincipal` e mantendo proteção CSRF na operação que altera estado; verificar com testes de API que a própria conta pode consultar/aportar, requisições inválidas são rejeitadas e nenhuma operação aceita `accountId` fornecido pelo cliente.

## 4. Atomicidade e semântica financeira

- [x] 4.1 Garantir que falhas durante atualização de saldo, registro histórico ou criação do ponto patrimonial provoquem rollback integral e que aportes permaneçam classificados apenas como entrada de capital, sem resultado realizado ou criação de operação de investimento; verificar com testes de rollback que saldo, `Movement` e `PatrimonialPoint` permanecem inalterados após falha e que `DEPOSIT` não possui resultado de investimento.

## 5. Validação completa da T15

- [x] 5.1 Cobrir os critérios da T15 e da delta spec `saldo-aportes`, incluindo saldo inicial de R$ 10.000,00, aporte mínimo, aporte válido, conta sem corretora, isolamento por sessão, precisão monetária e atomicidade; executar os testes focados, depois `mvnw.cmd test` e `openspec validate implementar-t15-saldo-aportes --type change`, confirmando ausência de regressões.
