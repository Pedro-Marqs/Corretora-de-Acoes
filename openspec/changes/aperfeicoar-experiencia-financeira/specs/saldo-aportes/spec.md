## ADDED Requirements

### Requirement: Retirada de saldo

O sistema SHALL permitir que o investidor autenticado retire valor positivo em reais, limitado ao saldo disponível da própria conta, mediante confirmação explícita. A retirada SHALL debitar o saldo e registrar movimentação e ponto patrimonial atomicamente, sem permitir saldo negativo.

#### Scenario: Retirada válida
- **WHEN** o investidor confirmar uma retirada positiva menor ou igual ao saldo disponível
- **THEN** o sistema SHALL debitar exatamente o valor solicitado, registrar a retirada e criar o ponto patrimonial na mesma transação

#### Scenario: Retirada acima do saldo
- **WHEN** o valor solicitado exceder o saldo disponível
- **THEN** o sistema SHALL rejeitar a retirada, informar solicitado e disponível conforme o contrato de erro e SHALL manter saldo, histórico e patrimônio inalterados

#### Scenario: Retirada inválida ou falha transacional
- **WHEN** o valor for ausente, zero, negativo ou uma etapa da retirada falhar
- **THEN** o sistema SHALL rejeitar ou reverter integralmente a operação sem alteração parcial

### Requirement: Apresentar retirada no Banco

A seção Banco SHALL disponibilizar uma ação de retirada e SHALL atualizar o saldo somente pela resposta oficial da operação. A interface SHALL indicar carregamento, confirmação, sucesso e erro sem permitir reenvio simultâneo.

#### Scenario: Abrir retirada
- **WHEN** o investidor autenticado selecionar retirada
- **THEN** a interface SHALL abrir formulário com saldo disponível como referência, sem alterar o saldo local

#### Scenario: Cancelar retirada
- **WHEN** o investidor cancelar a confirmação
- **THEN** a interface SHALL NOT enviar a retirada e SHALL preservar o contexto preenchido
