## ADDED Requirements

### Requirement: Abrir tela de negociação a partir do ativo

Ao acionar `Negociar` no detalhe do ativo, a interface SHALL mostrar somente a entrada de quantidade e as ações `Comprar` e `Vender`, além do preço fornecido pela API como somente leitura. A corretora e demais dados exigidos pelo contrato SHALL ser selecionáveis ou exibidos conforme o contexto autenticado existente, sem permitir preço informado pelo cliente.

#### Scenario: Negociação de ativo
- **WHEN** o investidor selecionar `Negociar`
- **THEN** a interface SHALL exibir quantidade, preço oficial e as opções de compra e venda, sem campo editável de preço

#### Scenario: Confirmar compra ou venda
- **WHEN** o investidor informar quantidade válida e escolher compra ou venda
- **THEN** a interface SHALL solicitar confirmação simples e SHALL enviar somente os dados autorizados pelo contrato existente

#### Scenario: Cotação indisponível
- **WHEN** não houver cotação ou câmbio utilizável
- **THEN** a interface SHALL bloquear a tentativa ou apresentar o erro funcional retornado, sem inventar preço ou alterar saldo/posição localmente

### Requirement: Atualizar negociação pela resposta oficial

Após operação concluída, a interface SHALL substituir saldo, posição e mensagens pelos dados devolvidos pela API ou por nova leitura autenticada. Falhas SHALL preservar o contexto recuperável e não declarar sucesso local.

#### Scenario: Operação concluída
- **WHEN** a compra ou venda for confirmada pelo backend
- **THEN** a interface SHALL apresentar o resultado oficial e permitir retornar à carteira atualizada

#### Scenario: Operação rejeitada
- **WHEN** o backend rejeitar saldo, posição, corretora, quantidade ou cotação
- **THEN** a interface SHALL exibir a mensagem funcional, preservar a entrada recuperável e SHALL NOT alterar valores exibidos por cálculo local
