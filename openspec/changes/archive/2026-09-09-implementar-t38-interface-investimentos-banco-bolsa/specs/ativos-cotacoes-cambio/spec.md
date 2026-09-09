## ADDED Requirements

### Requirement: Apresentar lista e detalhe do ativo

Ao selecionar `Investir` na lista de uma categoria, a interface SHALL abrir uma tela ou estado de detalhe do ativo com ticker, nome, mercado, moeda, cotação oficial, instante, dados disponíveis de posição e resumo visual. O preço e os avisos SHALL vir da API, sem edição pelo investidor.

#### Scenario: Abrir detalhe de ativo
- **WHEN** o investidor selecionar `Investir` para um ativo listado
- **THEN** a interface SHALL apresentar os dados oficiais do ativo e SHALL oferecer a ação `Negociar`

#### Scenario: Cotação desatualizada
- **WHEN** o ativo ou câmbio retornado estiver desatualizado
- **THEN** a interface SHALL manter o valor oficial, indicar a desatualização e mostrar o instante original sem permitir atualização manual

### Requirement: Pesquisa na seção Bolsa

A seção Bolsa SHALL manter a pesquisa normal de ativos exclusivamente por ticker, com os resultados brasileiros e norte-americanos e os estados de vazio, erro, carregamento e desatualização já definidos para a pesquisa.

#### Scenario: Pesquisar ativo na Bolsa
- **WHEN** o investidor autenticado enviar um ticker válido na seção Bolsa
- **THEN** a interface SHALL apresentar os campos oficiais do resultado e SHALL permitir iniciar o fluxo de detalhe/negociação sem solicitar preço ao usuário

#### Scenario: Pesquisa sem resultado
- **WHEN** a pesquisa válida não retornar ativo utilizável
- **THEN** a interface SHALL apresentar estado vazio ou erro funcional apropriado e SHALL preservar o ticker para nova tentativa
