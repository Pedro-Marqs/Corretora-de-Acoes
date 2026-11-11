## ADDED Requirements

### Requirement: Registrar retirada e instante informado

Uma retirada concluída SHALL ser registrada como movimentação imutável da conta com valor, saldo resultante e instante efetivo da operação. Compras e vendas SHALL preservar o instante de operação informado pelo investidor quando válido, usando o momento atual de Brasília quando nenhum instante for informado; o ponto patrimonial correspondente SHALL usar o mesmo instante efetivo.

#### Scenario: Registrar retirada
- **WHEN** uma retirada for concluída
- **THEN** o histórico SHALL conter a retirada e o ponto patrimonial SHALL refletir o saldo após o débito

#### Scenario: Data padrão da operação
- **WHEN** uma compra ou venda for confirmada sem data/hora informada
- **THEN** o sistema SHALL registrar o momento atual no horário de Brasília

#### Scenario: Data/hora informada
- **WHEN** uma compra ou venda for confirmada com data/hora válida permitida
- **THEN** o histórico e o ponto patrimonial SHALL usar o instante informado e exibi-lo conforme o horário de Brasília
