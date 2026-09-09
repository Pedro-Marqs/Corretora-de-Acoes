## ADDED Requirements

### Requirement: Administrar corretoras dentro do Banco

A área de corretoras SHALL ser acessível a partir de Banco e SHALL preservar pesquisa exclusivamente por CNPJ, associação, listagem de corretoras ativas, remoção lógica e bloqueios funcionais já definidos. Somente corretoras pertencentes à conta autenticada SHALL ser apresentadas como disponíveis.

#### Scenario: Abrir corretoras pelo Banco
- **WHEN** o investidor selecionar o cadastro de corretoras na seção Banco
- **THEN** a interface SHALL mostrar as corretoras próprias ativas e a ação de pesquisar/associar por CNPJ

#### Scenario: Remover corretora com posição
- **WHEN** o investidor tentar remover uma corretora que possua posição aberta
- **THEN** a interface SHALL mostrar o bloqueio funcional retornado e SHALL manter a corretora ativa
