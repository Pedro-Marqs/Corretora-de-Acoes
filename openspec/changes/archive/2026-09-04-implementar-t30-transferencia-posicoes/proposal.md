## Why

As posições já podem ser compradas e vendidas, mas ainda não há uma operação backend para reorganizá-las entre corretoras próprias sem transformar a transferência em venda e recompra. A T30 implementa esse fluxo agora para manter quantidade, custo médio acumulado e histórico consistentes, sem movimentar o saldo.

## What Changes

- Adicionar uma operação autenticada de transferência parcial ou total de posição entre duas associações próprias, ativas e distintas.
- Validar origem, destino, ativo, quantidade inteira positiva, disponibilidade da posição e pertencimento à conta autenticada.
- Atualizar origem e destino atomicamente, preservando o custo transferido e recalculando a média ponderada quando o destino já possuir posição.
- Registrar uma movimentação imutável de transferência e um ponto patrimonial, sem resultado realizado e sem alteração do saldo.
- Garantir rollback integral e comportamento seguro sob concorrência entre transferências e outras operações da mesma posição.
- Cobrir contrato HTTP, validações, isolamento, persistência, concorrência e falhas intermediárias com testes focados.

## Capabilities

### New Capabilities

### Modified Capabilities

- `transferencia-posicoes`: detalhar o endpoint autenticado, entradas, respostas, erros, atomicidade e concorrência da transferência.
- `compra-venda-posicoes`: explicitar a consistência concorrente das posições quando a transferência ocorrer junto de operações de carteira.
- `corretoras`: exigir associações próprias ativas e distintas também para transferências.
- `historico-registro-patrimonial`: detalhar o evento imutável e o ponto patrimonial produzidos por uma transferência bem-sucedida.
- `saldo-aportes`: explicitar que a transferência não altera o saldo nem cria movimentação de saldo.

## Impact

Backend de carteira, posições e movimentações; contrato HTTP autenticado e tratamento uniforme de erros; persistência transacional de posição, histórico e patrimônio; testes unitários, de integração H2 e de concorrência. Não há nova integração externa, alteração de frontend ou mudança no saldo contábil.
