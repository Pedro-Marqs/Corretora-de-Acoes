## Context

As entidades de posição, movimentação e cotação já existem no modelo da aplicação, e `FinancialAmount`/o relógio financeiro foram estabelecidos anteriormente. A T26 deve fornecer uma regra única para consumo posterior por compra, venda, transferência e dashboards, mantendo os cálculos fora de controllers, repositories, integrações externas e frontend.

## Goals / Non-Goals

**Goals:**

- Encapsular operações de posição como transformações determinísticas de valores normalizados.
- Garantir precisão, escala e arredondamento uniformes em BRL e na conversão USD/BRL.
- Produzir dados suficientes para custo, médias, resultados e patrimônio sem depender de estado persistido.
- Tornar cada exemplo das specs reproduzível por testes unitários rápidos.

**Non-Goals:**

- Não implementar endpoints, autenticação, transações de compra/venda, transferência ou dashboard.
- Não alterar esquema, entidades persistidas, histórico ou pontos patrimoniais.
- Não consultar provedores de mercado nem decidir quando uma cotação está desatualizada.

## Decisions

### Núcleo puro de cálculo

As regras serão representadas por objetos/serviços de domínio sem efeitos colaterais: recebem quantidade, custo/preço e cotação já validados e devolvem uma posição ou resultado calculado. Isso permite que os casos de uso futuros revalidem saldo e propriedade sem misturar persistência ao cálculo. A alternativa de colocar fórmulas nos services transacionais foi rejeitada por duplicar regras e dificultar os testes isolados.

### Custo e média ponderada

O custo total será a soma dos custos de aquisição em moeda consolidada. Compras sobre posição aberta usam `(custo anterior + custo da compra) / quantidade total`; vendas reduzem o custo por `preço médio vigente × quantidade vendida`. Ao chegar a quantidade zero, custo e média são zerados, e uma nova compra inicia uma base independente. A transferência move quantidade e custo proporcional à média da origem; no destino, o custo recebido é combinado com o custo existente.

### Resultado e patrimônio

O resultado realizado de cada venda é `(preço de venda − preço médio vigente) × quantidade`. Para uma posição aberta, a valorização não realizada é `valor de mercado − custo total`; o resultado total é realizado acumulado mais valorização não realizada. Patrimônio é saldo mais valor de mercado das posições. Movimentações de capital (saldo inicial e aporte) ficam fora dos resultados.

### Precisão e moeda

Todos os valores financeiros usarão `BigDecimal` através da convenção financeira existente: duas casas e `HALF_UP` nos valores expostos/consolidados. Quantidades permanecem inteiras. Valores USD serão convertidos por multiplicação direta pela cotação USD/BRL, sem taxa. A alternativa de `double` foi rejeitada por perda de precisão e divergência nos limites de arredondamento.

### Contrato de teste

Os testes unitários cobrirão primeiras compras, médias, vendas parciais/totais, recompra, transferências para destinos vazios/existentes, resultados positivo/negativo, patrimônio, aportes excluídos e conversão internacional, incluindo valores na terceira casa decimal. Os testes não usarão banco, HTTP, relógio real ou frontend.

## Risks / Trade-offs

- **[Arredondamento em etapas diferentes]** → arredondar em pontos de saída definidos e reutilizar a mesma abstração financeira, com casos-limite explícitos nos testes.
- **[Divergência entre custo persistido e cálculo futuro]** → manter o contrato de custo/média como única fonte para os casos de uso posteriores e testar invariantes de conservação na transferência.
- **[Cotação USD/BRL indisponível]** → deixar a decisão de disponibilidade e fallback para T23/T27/T28; este núcleo não inventa cotação.
- **[Acúmulo de resultado realizado]** → retornar o resultado de cada venda de forma determinística para que o dashboard agregue somente movimentações concluídas, sem incluir aportes.

## Migration Plan

Não há migração de banco ou mudança de contrato HTTP nesta etapa. A implementação será adicionada de modo compatível e consumida pelas tarefas T27, T28, T30 e T34; em caso de rollback, remover o uso do núcleo pelos consumidores sem alteração de dados persistidos.

## Open Questions

Nenhuma. As decisões necessárias para o escopo da T26 estão definidas nas specs e nas decisões funcionais de continuidade.
