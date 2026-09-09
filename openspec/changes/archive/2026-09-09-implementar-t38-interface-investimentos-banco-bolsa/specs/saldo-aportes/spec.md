## ADDED Requirements

### Requirement: Compor seção Banco

A seção Banco SHALL apresentar o saldo em dinheiro da conta como valor compartilhado, uma ação para aporte, uma ação para retirada quando o contrato de retirada estiver disponível, e a área de cadastro/administração das corretoras. A interface SHALL manter o saldo como somente leitura e usar os fluxos financeiros oficiais para qualquer alteração.

#### Scenario: Banco com saldo
- **WHEN** o investidor autenticado abrir Banco
- **THEN** a interface SHALL mostrar somente o saldo em dinheiro da própria conta como indicador financeiro principal e SHALL disponibilizar os acessos de aporte, retirada aplicável e corretoras

#### Scenario: Aporte no Banco
- **WHEN** o investidor iniciar um aporte
- **THEN** a interface SHALL abrir o formulário existente, validar o mínimo definido, confirmar a ação e atualizar o saldo somente pela resposta oficial

#### Scenario: Retirada não suportada pelo contrato atual
- **WHEN** o investidor selecionar retirada e não existir contrato backend habilitado para essa operação
- **THEN** a interface SHALL informar que a operação não está disponível, SHALL NOT simular débito nem oferecer valor fictício e SHALL preservar o saldo
