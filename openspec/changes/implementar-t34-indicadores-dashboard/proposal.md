## Why

As operações, posições e cotações já estão disponíveis, mas a conta ainda não expõe uma visão consolidada de saldo, patrimônio e rentabilidade. A T34 implementa essa primeira camada do dashboard no backend, usando as regras financeiras existentes como fonte única antes das telas e distribuições posteriores.

## What Changes

- Criar uma consulta autenticada de indicadores gerais do dashboard.
- Retornar saldo, posições, patrimônio, preço médio, resultado realizado, valorização não realizada e resultado total calculados no backend.
- Consolidar ativos norte-americanos em BRL com a cotação USD/BRL aplicável, retornando a cotação e avisos de dados desatualizados quando necessário.
- Excluir saldo inicial e aportes dos resultados de investimento.
- Tratar conta sem operações e ausência de cotação utilizável sem expor dados de outras contas.

## Capabilities

### New Capabilities

### Modified Capabilities

- `dashboards`: definir a consulta de indicadores gerais, seu contrato observável, conversão internacional, estados de dados e exclusão de aportes dos resultados.

## Impact

- Backend: controller, serviço, projeções/repositories de conta, posições, movimentações e cotações, reutilizando as regras financeiras da T26.
- API: novo endpoint autenticado de consulta geral do dashboard; nenhuma alteração nas operações existentes.
- Testes: cobertura de conta sem posições, carteira brasileira e mista, resultados, aportes, dados antigos, cotação USD/BRL e isolamento entre contas.
- Banco: somente consultas; sem migração prevista.
