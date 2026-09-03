## 1. Modelo de posição e operações

- [x] 1.1 Criar os objetos de domínio imutáveis para quantidade, custo total e preço médio, com invariantes para posição aberta, zerada e recompra; verificar com testes unitários de primeira compra, venda total e recompra.
- [x] 1.2 Implementar compra, venda parcial e venda total usando média ponderada, redução de custo e resultado realizado; verificar os exemplos CA01–CA05 de `compra-venda-posicoes` e os cenários de resultado positivo e negativo.

## 2. Transferência e conversão financeira

- [x] 2.1 Implementar transferência de quantidade e custo entre posições, incluindo destino vazio, destino existente, transferência parcial/total e conservação do custo; verificar CA01–CA05 de `transferencia-posicoes` sem dependência de banco.
- [x] 2.2 Implementar conversão USD/BRL e arredondamento financeiro compartilhado para operações e avaliações; verificar o exemplo CA06 de `compra-venda-posicoes` e casos de terceira casa com `HALF_UP`.

## 3. Resultados consolidados

- [x] 3.1 Implementar os cálculos puros de valor de mercado, patrimônio, valorização não realizada e resultado total, agregando resultado realizado e excluindo saldo inicial/aportes; verificar CA01–CA02 de `dashboards` e os cenários de `saldo-aportes`.
- [x] 3.2 Completar a suíte unitária com combinações de posições brasileiras e norte-americanas, múltiplas corretoras, resultados acumulados e invariantes de conservação; verificar que todos os testes financeiros passam sem banco, HTTP, frontend ou relógio real.

## 4. Verificação do change

- [x] 4.1 Revisar as regras implementadas contra os quatro deltas e o design, executar a suíte focada de domínio e confirmar que nenhum cálculo depende de repository, integração externa ou valor enviado pelo frontend.
