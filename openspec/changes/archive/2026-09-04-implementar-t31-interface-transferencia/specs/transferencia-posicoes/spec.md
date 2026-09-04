## ADDED Requirements

### Requirement: Iniciar transferencia pela interface

A interface privada SHALL permitir que o investidor selecione uma posicao aberta, uma corretora de destino ativa e uma quantidade inteira positiva para iniciar uma transferencia autenticada. A origem SHALL ser a corretora da posicao selecionada, o destino SHALL ser diferente da origem e a interface MUST NOT enviar saldo, preco, custo ou conta como autoridade financeira.

#### Scenario: Selecionar posicao e destino validos

- **WHEN** o investidor selecionar uma posicao aberta, uma corretora propria ativa diferente e uma quantidade inteira dentro da disponibilidade
- **THEN** a interface SHALL apresentar a operacao pronta para confirmacao, mantendo a origem derivada da posicao e os valores financeiros como somente leitura

#### Scenario: Destino igual a origem

- **WHEN** o investidor tentar escolher como destino a mesma corretora da origem
- **THEN** a interface SHALL impedir a selecao ou o envio e SHALL exibir uma orientacao funcional sem chamar a API

#### Scenario: Quantidade invalida ou excessiva

- **WHEN** a quantidade estiver vazia, nao for inteira positiva ou exceder a quantidade disponivel
- **THEN** a interface SHALL rejeitar o envio, informar o campo invalido e SHALL NOT executar a transferencia

#### Scenario: Confirmacao cancelada

- **WHEN** o investidor cancelar a confirmacao simples da transferencia
- **THEN** a interface MUST NOT enviar a operacao e SHALL preservar origem, destino e quantidade para edicao ou nova confirmacao

#### Scenario: Transferencia concluida

- **WHEN** o backend aceitar a transferencia
- **THEN** a interface SHALL substituir os dados de posicao pela resposta da operacao ou por nova leitura autenticada, SHALL manter o saldo exibido inalterado e SHALL apresentar uma mensagem de sucesso

#### Scenario: Erro funcional da transferencia

- **WHEN** o backend rejeitar a transferencia por propriedade, atividade, quantidade ou estado financeiro desatualizado
- **THEN** a interface SHALL apresentar a mensagem funcional retornada, incluindo solicitado/disponivel quando fornecido, SHALL preservar contexto recuperavel e SHALL NOT declarar sucesso local
