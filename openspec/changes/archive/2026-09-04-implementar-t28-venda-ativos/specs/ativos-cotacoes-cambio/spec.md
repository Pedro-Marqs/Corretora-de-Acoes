## MODIFIED Requirements

### Requirement: Cotacoes e cambio
 O sistema SHALL registrar cotações e câmbio válidos com fonte, instante, moeda e indicação de desatualização, utilizando o último valor válido armazenado quando uma atualização externa não puder fornecer um valor utilizável. O sistema SHALL atualizar cotações brasileiras a cada cinco minutos e cotações norte-americanas e USD/BRL uma vez por dia às 10h no horário de Brasília. Uma falha da fonte externa SHALL preservar o último valor válido armazenado e sinalizar sua desatualização. Operações de compra ou venda SHALL usar a cotação/câmbio atual ou o último valor armazenado utilizável, informando o instante original; sem valor utilizável, SHALL bloquear a operação.

#### Scenario: Fallback usado na venda
- **WHEN** a consulta externa falhar e existir cotação válida armazenada para o ativo e, se necessário, USD/BRL válido
- **THEN** o sistema SHALL usar os últimos valores armazenados, informar seus instantes originais e permitir o cálculo da venda

#### Scenario: Ausência de cotação utilizável na venda
- **WHEN** uma venda depender de uma cotação e não existir valor atual nem valor válido armazenado
- **THEN** o sistema SHALL bloquear a venda sem alterar saldo, posição, histórico ou patrimônio

#### Scenario: Ausência de câmbio utilizável na venda
- **WHEN** uma venda de ativo norte-americano exigir conversão para BRL e não existir USD/BRL utilizável
- **THEN** o sistema SHALL bloquear a venda sem alterar saldo, posição, histórico ou patrimônio

#### Scenario: Cotação antiga usada na venda
- **WHEN** a cotação utilizada tiver mais de 24 horas ou o USD/BRL tiver mais de sete dias
- **THEN** o sistema SHALL manter o valor armazenado disponível e SHALL indicar o dado como desatualizado

#### Scenario: Fallback usado na compra
- **WHEN** a consulta externa falhar e existir cotação válida armazenada para o ativo e, se necessário, USD/BRL válido
- **THEN** o sistema SHALL usar os últimos valores armazenados, informar seus instantes originais e permitir o cálculo da compra

#### Scenario: Fallback de cotacao
- **WHEN** uma consulta externa falhar e existir uma cotação válida armazenada para o ativo
- **THEN** o sistema SHALL utilizar a última cotação válida e SHALL informar seu instante original

#### Scenario: Fallback de cambio
- **WHEN** a consulta de USD/BRL falhar e existir uma cotação de câmbio válida armazenada
- **THEN** o sistema SHALL utilizar o último USD/BRL válido e SHALL informar seu instante original

#### Scenario: Cotacao indisponivel ou desatualizada
- **WHEN** a fonte externa falhar ou a cotação exceder sua validade
- **THEN** o sistema SHALL informar o estado desatualizado sem inventar um valor

#### Scenario: Ausencia de cotacao utilizavel
- **WHEN** uma compra depender de uma cotação e não existir valor atual nem valor válido armazenado
- **THEN** o sistema SHALL bloquear a compra dependente sem alterar saldo, posição, histórico ou patrimônio

#### Scenario: Ausencia de cambio utilizavel
- **WHEN** uma compra de ativo norte-americano exigir conversão para BRL e não existir USD/BRL utilizável
- **THEN** o sistema SHALL bloquear a compra dependente sem alterar saldo, posição, histórico ou patrimônio

#### Scenario: Cotacao de ativo antiga
- **WHEN** a cotação utilizada tiver mais de 24 horas
- **THEN** o sistema SHALL indicá-la como desatualizada e SHALL manter disponível o valor armazenado

#### Scenario: Cambio antigo
- **WHEN** o USD/BRL utilizado tiver mais de sete dias
- **THEN** o sistema SHALL indicá-lo como desatualizado e SHALL manter disponível o valor armazenado

#### Scenario: Exibir ativo norte-americano
- **WHEN** um ativo norte-americano possuir cotação armazenada em USD e existir USD/BRL utilizável
- **THEN** o sistema SHALL retornar a cotação em USD e seu valor correspondente em BRL com os respectivos dados temporais necessários para identificar sua atualidade

#### Scenario: Ciclo diario no horario definido
- **WHEN** o relogio atingir 10h no horario de Brasilia em um novo dia
- **THEN** o sistema SHALL executar no maximo uma atualizacao diaria para cotacoes norte-americanas e USD/BRL

#### Scenario: Falha preserva cache
- **WHEN** uma consulta automatica de cotacao ou cambio falhar e houver valor armazenado
- **THEN** o sistema SHALL manter o valor anterior e marcar o dado como desatualizado

#### Scenario: Ciclos nao se sobrepoem
- **WHEN** um ciclo do mesmo tipo ja estiver em execucao
- **THEN** o sistema SHALL rejeitar ou ignorar a nova execucao sem iniciar processamento concorrente
