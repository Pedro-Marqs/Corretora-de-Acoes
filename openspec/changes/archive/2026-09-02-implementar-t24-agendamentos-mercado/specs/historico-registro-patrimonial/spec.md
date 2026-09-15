## MODIFIED Requirements

### Requirement: Historico de movimentacoes
O sistema SHALL registrar operacoes, aportes, transferencias e ajustes com conta, instante, valores e origem. Atualizacoes automaticas de cotacoes ou cambio, sem operacao financeira, SHALL NOT criar movimentacoes.

#### Scenario: Consultar historico proprio
- **WHEN** o investidor autenticado consultar seu historico
- **THEN** o sistema SHALL retornar registros ordenados e pertencentes somente a sua conta

#### Scenario: Atualizacao de mercado sem movimentacao
- **WHEN** um ciclo automatico atualizar cotacoes ou cambio
- **THEN** o sistema SHALL manter o historico de movimentacoes inalterado

### Requirement: Registro patrimonial
O sistema SHALL registrar pontos patrimoniais consistentes com saldo e posicoes em instante controlado. Atualizacoes isoladas de cotacao ou cambio SHALL NOT gerar ponto patrimonial.

#### Scenario: Gerar ponto patrimonial
- **WHEN** ocorrer o processamento de um ponto patrimonial
- **THEN** o sistema SHALL persistir saldo, posicoes, cambio e valor total usados no calculo

#### Scenario: Atualizacao de mercado sem ponto patrimonial
- **WHEN** um ciclo automatico atualizar somente cotacoes ou cambio
- **THEN** o sistema SHALL deixar inalterados os pontos patrimoniais persistidos

### Requirement: Imutabilidade
O sistema SHALL impedir alteracao silenciosa de registros historicos ja persistidos.

#### Scenario: Tentativa de alterar historico
- **WHEN** uma operacao tentar modificar um registro historico
- **THEN** o sistema SHALL rejeitar a alteracao ou criar um novo evento auditavel
