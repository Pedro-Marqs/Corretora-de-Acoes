## MODIFIED Requirements

### Requirement: Catalogo de ativos
O sistema SHALL manter ativos brasileiros e norte-americanos com identificação, tipo, moeda e status, permitindo pesquisa de ativos suportados e persistindo somente dados completos e válidos. Consultas de ativos SHALL considerar somente ativos ativos. Ciclos automáticos de cotação SHALL atualizar somente ativos ativos que possuam posição em alguma carteira.

#### Scenario: Consultar ativo ativo
- **WHEN** o investidor consultar um ativo cadastrado e ativo
- **THEN** o sistema SHALL retornar seus dados publicos

#### Scenario: Pesquisar ativo brasileiro
- **WHEN** o investidor pesquisar um ticker brasileiro válido e o provedor retornar dados completos
- **THEN** o sistema SHALL retornar o ativo com ticker, nome, mercado, moeda, cotação e instante e SHALL preservar os dados válidos para consultas posteriores

#### Scenario: Ativo ou mercado não suportado
- **WHEN** a pesquisa retornar um ativo pertencente a mercado não suportado
- **THEN** o sistema SHALL rejeitar o ativo e SHALL NOT persistir seus dados como ativo operacional

#### Scenario: Resposta incompleta
- **WHEN** uma fonte externa retornar dados sem identificação, nome, mercado, moeda, cotação ou instante obrigatório
- **THEN** o sistema SHALL rejeitar a nova resposta e SHALL preservar qualquer dado válido anteriormente armazenado

#### Scenario: Atualizar automaticamente somente ativos em posicao
- **WHEN** o ciclo automatico de cotacoes brasileiras executar
- **THEN** o sistema SHALL consultar somente ativos ativos que estejam presentes em alguma posicao

### Requirement: Cotacoes e cambio
O sistema SHALL registrar cotações e câmbio válidos com fonte, instante, moeda e indicação de desatualização, utilizando o último valor válido armazenado quando uma atualização externa não puder fornecer um valor utilizável. O sistema SHALL atualizar cotações brasileiras a cada cinco minutos e cotações norte-americanas e USD/BRL uma vez por dia às 10h no horário de Brasília. Uma falha da fonte externa SHALL preservar o último valor válido armazenado e sinalizar sua desatualização.

#### Scenario: Cotacao indisponivel ou desatualizada
- **WHEN** a fonte externa falhar ou a cotacao exceder sua validade
- **THEN** o sistema SHALL informar o estado desatualizado sem inventar um valor

#### Scenario: Fallback de cotacao
- **WHEN** uma consulta externa falhar e existir uma cotação válida armazenada para o ativo
- **THEN** o sistema SHALL utilizar a última cotação válida e SHALL informar seu instante original

#### Scenario: Ausencia de cotacao utilizavel
- **WHEN** uma funcionalidade financeira depender de uma cotação e não existir valor atual nem valor válido armazenado
- **THEN** o sistema SHALL bloquear a operação dependente sem inventar uma cotação

#### Scenario: Cotacao de ativo antiga
- **WHEN** a cotação utilizada tiver mais de 24 horas
- **THEN** o sistema SHALL indicá-la como desatualizada e SHALL manter disponível o valor armazenado

#### Scenario: Fallback de cambio
- **WHEN** a consulta de USD/BRL falhar e existir uma cotação de câmbio válida armazenada
- **THEN** o sistema SHALL utilizar o último USD/BRL válido e SHALL informar seu instante original

#### Scenario: Cambio antigo
- **WHEN** o USD/BRL utilizado tiver mais de sete dias
- **THEN** o sistema SHALL indicá-lo como desatualizado e SHALL manter disponível o valor armazenado

#### Scenario: Ausencia de cambio utilizavel
- **WHEN** uma funcionalidade financeira em ativo norte-americano exigir conversão para BRL e não existir USD/BRL utilizável
- **THEN** o sistema SHALL bloquear a operação dependente sem inventar uma taxa de câmbio

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
