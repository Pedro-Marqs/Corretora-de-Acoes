## MODIFIED Requirements

### Requirement: Consultar dashboard

O sistema SHALL exibir dados consolidados da propria conta, com valores monetarios em duas casas e datas no horario de Brasilia. O patrimonio SHALL ser o saldo em reais acrescido do valor de mercado das posicoes; a valorizacao nao realizada SHALL ser o valor de mercado das posicoes menos seu custo total; o resultado total SHALL ser a soma do resultado realizado e da valorizacao nao realizada. Aportes e saldo inicial SHALL ficar fora desses resultados.

#### Scenario: Dashboard com dados

- **WHEN** o investidor autenticado consultar o dashboard
- **THEN** o sistema SHALL retornar saldo, posicoes, patrimonio, resultado realizado, valorizacao nao realizada e resultado total calculados pelas mesmas regras financeiras

#### Scenario: Dashboard sem movimentacoes

- **WHEN** a conta ainda nao possuir operacoes
- **THEN** o sistema SHALL exibir estado vazio com saldo inicial correto e resultados de investimento iguais a zero

#### Scenario: Patrimonio com posicoes

- **WHEN** o saldo for R$ 1.000,00 e as posicoes forem avaliadas em R$ 2.500,00
- **THEN** o patrimonio SHALL ser R$ 3.500,00

## ADDED Requirements

### Requirement: Consolidar valores internacionais

O sistema SHALL converter o valor de mercado e o custo de ativos norte-americanos para BRL usando a cotacao USD/BRL aplicavel, sem taxa cambial, antes de consolidar patrimonio, valorizacao e distribuicoes.

#### Scenario: Posicao USD no dashboard

- **WHEN** uma posicao valer USD 20,00 e a cotacao USD/BRL for R$ 5,00
- **THEN** seu valor consolidado SHALL ser R$ 100,00 e o dashboard SHALL retornar a cotacao usada

### Requirement: Exibir resultado sem aporte

O sistema SHALL calcular lucro, prejuizo e valorizacao somente a partir das operacoes e da variacao das posicoes, excluindo saldo inicial e aportes.

#### Scenario: Aporte sem variacao de investimento

- **WHEN** uma conta sem variacao de precos receber um aporte
- **THEN** saldo e patrimonio SHALL aumentar pelo aporte, enquanto resultado realizado, valorizacao nao realizada e resultado total SHALL permanecer inalterados
