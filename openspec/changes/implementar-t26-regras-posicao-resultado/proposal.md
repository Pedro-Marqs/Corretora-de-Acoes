## Why

As operações de carteira e os dashboards ainda não possuem um núcleo financeiro determinístico para calcular custo, preço médio, resultados e patrimônio. A T26 é necessária agora para que as próximas tarefas de compra, venda, transferência e dashboard usem a mesma regra, sem cálculos divergentes no frontend ou em serviços diferentes.

## What Changes

- Criar regras de domínio, independentes de banco, HTTP e frontend, para custo total, preço médio ponderado, venda parcial/total, recompra e transferência.
- Calcular resultado realizado, valorização não realizada, patrimônio e resultado total em reais.
- Converter posições norte-americanas de USD para BRL exclusivamente pela cotação USD/BRL, com arredondamento financeiro padronizado.
- Garantir que aporte e saldo inicial sejam capital, não lucro, prejuízo ou valorização.
- Cobrir os exemplos numéricos e casos-limite das especificações com testes unitários determinísticos.

## Capabilities

### New Capabilities

Nenhuma. A mudança consolida comportamento financeiro nas capacidades existentes.

### Modified Capabilities

- `compra-venda-posicoes`: especificar custo/preço médio, resultado realizado, venda parcial, zeragem e recompra.
- `transferencia-posicoes`: especificar preservação do custo transferido e média ponderada no destino.
- `dashboards`: especificar patrimônio, resultado realizado, valorização não realizada, resultado total e conversão USD/BRL.
- `saldo-aportes`: explicitar que saldo inicial e aportes não compõem lucro, prejuízo ou valorização, inclusive no cálculo consolidado.

## Impact

- Afeta os modelos e serviços de domínio financeiro e seus testes unitários.
- Define o contrato que será consumido pelas futuras implementações de compra, venda, transferência e dashboards, sem criar endpoints ou alterar persistência nesta mudança.
- Não adiciona dependências externas nem chamadas HTTP; os cálculos recebem dados já normalizados e não dependem de repositories.
