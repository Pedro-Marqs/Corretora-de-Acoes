## MODIFIED Requirements

### Requirement: Consultar dashboard

O sistema SHALL disponibilizar `GET /api/dashboard` para o investidor autenticado e SHALL retornar dados consolidados da propria conta, com valores monetarios em duas casas e datas no horario de Brasilia. A resposta SHALL conter saldo, posicoes, patrimonio, preco medio, resultado realizado, valorizacao nao realizada e resultado total calculados pelas mesmas regras financeiras das operacoes. O patrimonio SHALL ser o saldo em reais acrescido do valor de mercado das posicoes; a valorizacao nao realizada SHALL ser o valor de mercado das posicoes menos seu custo total; o resultado total SHALL ser a soma do resultado realizado e da valorizacao nao realizada. Aportes e saldo inicial SHALL ficar fora desses resultados. A resposta SHALL identificar cotacoes ou cambio antigos quando utilizados, incluindo o instante e o fator USD/BRL aplicados.

#### Scenario: Dashboard com dados

- **WHEN** o investidor autenticado consultar `GET /api/dashboard`
- **THEN** o sistema SHALL retornar saldo, posicoes, preco medio, patrimonio, resultado realizado, valorizacao nao realizada e resultado total calculados pelas mesmas regras financeiras

#### Scenario: Dashboard sem movimentacoes

- **WHEN** a conta ainda nao possuir operacoes
- **THEN** o sistema SHALL retornar estado vazio com saldo inicial correto e resultados de investimento iguais a zero

#### Scenario: Patrimonio com posicoes

- **WHEN** o saldo for R$ 1.000,00 e as posicoes forem avaliadas em R$ 2.500,00
- **THEN** o patrimonio SHALL ser R$ 3.500,00

#### Scenario: Dashboard sem autenticacao

- **WHEN** uma requisicao para `GET /api/dashboard` nao possuir sessao autenticada valida
- **THEN** o sistema SHALL rejeitar a consulta com o contrato uniforme de autenticacao e SHALL NOT revelar indicadores, posicoes ou metadados da conta

## ADDED Requirements

### Requirement: Consolidar indicadores internacionais

O sistema SHALL converter para BRL o valor de mercado e o custo de ativos norte-americanos usando a cotacao USD/BRL aplicavel, sem taxa cambial, antes de consolidar patrimonio, valorizacao e resultados. A resposta SHALL retornar a cotacao USD/BRL e o instante utilizados quando houver posicao internacional.

#### Scenario: Posicao USD no dashboard

- **WHEN** uma posicao valer USD 20,00 e a cotacao USD/BRL for R$ 5,00
- **THEN** seu valor consolidado SHALL ser R$ 100,00 e o dashboard SHALL retornar a cotacao usada

#### Scenario: Cambio indisponivel sem cache

- **WHEN** uma conta possuir ativo norte-americano e nao existir cotacao USD/BRL utilizavel
- **THEN** o sistema SHALL rejeitar a consulta com erro funcional padronizado, sem inventar conversao ou valor consolidado

### Requirement: Excluir aportes dos resultados

O sistema SHALL calcular lucro, prejuizo e valorizacao somente a partir das operacoes e da variacao das posicoes, excluindo saldo inicial e aportes.

#### Scenario: Aporte sem variacao de investimento

- **WHEN** uma conta sem variacao de precos receber um aporte
- **THEN** saldo e patrimonio SHALL aumentar pelo aporte, enquanto resultado realizado, valorizacao nao realizada e resultado total SHALL permanecer inalterados

### Requirement: Proteger consistencia e isolamento dos indicadores

O sistema SHALL retornar somente posicoes, movimentacoes, cotacoes e saldo pertencentes à conta autenticada, SHALL preservar o ultimo valor valido quando uma cotacao estiver desatualizada e SHALL indicar essa desatualizacao na resposta sem alterar dados financeiros.

#### Scenario: Cotacao antiga

- **WHEN** o dashboard usar uma cotacao de mercado ou USD/BRL acima do limite de atualizacao
- **THEN** o sistema SHALL retornar os indicadores calculados com o ultimo valor valido, o instante desse valor e um indicador de desatualizacao

#### Scenario: Isolamento entre contas

- **WHEN** duas contas autenticadas consultarem seus dashboards
- **THEN** cada resposta SHALL conter somente os indicadores e posicoes da propria conta
