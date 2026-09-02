## Why

A T22 criou os adapters necessários para obter dados válidos de mercado e câmbio, mas esses dados ainda não são persistidos nem disponibilizados aos fluxos da aplicação. A T23 implementa o catálogo e o cache necessários para pesquisar ativos, reutilizar a última cotação válida em falhas externas e indicar quando os dados estão desatualizados.

## What Changes

- Permitir pesquisa de ativos brasileiros por ticker utilizando o adapter Brapi da T22.
- Disponibilizar ativos norte-americanos a partir das cotações armazenadas do ciclo diário.
- Persistir somente ativos, cotações e câmbio completos e válidos.
- Registrar fonte e instante dos dados persistidos.
- Nunca substituir um valor válido armazenado por resposta externa inválida ou incompleta.
- Utilizar a última cotação armazenada quando o provedor estiver indisponível.
- Utilizar o último USD/BRL armazenado quando a consulta atual estiver indisponível.
- Indicar como desatualizada uma cotação de ativo com mais de 24 horas.
- Indicar como desatualizado um USD/BRL com mais de sete dias.
- Permitir o uso de cotação antiga quando existir valor armazenado válido, mantendo visível seu instante e estado de desatualização.
- Bloquear fluxos financeiros que dependam de uma cotação ou câmbio quando nenhum valor utilizável estiver disponível.
- Para ativos norte-americanos, disponibilizar cotação em USD e correspondente em BRL usando o USD/BRL armazenado.
- Rejeitar ativos de mercados não suportados e respostas sem os campos obrigatórios.
- Garantir que somente o backend determine a cotação usada financeiramente.
- Não implementar scheduler nesta tarefa; os ciclos automáticos permanecem para a T24.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `ativos-cotacoes-cambio`: detalhar pesquisa e persistência de ativos, uso de cache/fallback, exibição USD/BRL, indicação de dados desatualizados e comportamento quando não existe cotação ou câmbio utilizável.

## Impact

- Services e controllers responsáveis por pesquisa e consulta de ativos.
- Repositories e entidades `Asset`, `Quote` e `ExchangeRate`.
- Adapters de mercado e câmbio implementados na T22.
- Persistência e consulta da última cotação válida por ativo.
- Persistência e consulta do último USD/BRL válido.
- Respostas da API de ativos com cotação, moeda, instante e estado de desatualização.
- Testes de pesquisa, cache, fallback, idade dos dados e isolamento entre contas.
- Possível evolução do esquema existente para armazenar metadados necessários, como fonte da cotação/câmbio.
- Nenhuma alteração de frontend ou scheduler nesta tarefa.