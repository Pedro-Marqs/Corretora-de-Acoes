## MODIFIED Requirements

### Requirement: Navegação privada por seções

A área autenticada SHALL apresentar no header exatamente as seções principais `Investimentos`, `Banco` e `Bolsa`, com indicação clara da seção ativa. Após login válido, a interface SHALL navegar para a tela inicial privada `/app`, que SHALL apresentar um resumo visual coerente com a linguagem da tela de login e acessos às três seções. A navegação SHALL manter acessíveis as funcionalidades existentes que pertençam a cada contexto, sem expor rotas privadas a uma sessão inválida.

#### Scenario: Destino após login
- **WHEN** o login for concluído com sucesso
- **THEN** a interface SHALL navegar para `/app` e apresentar a tela inicial privada, não o formulário de login

#### Scenario: Tela inicial privada
- **WHEN** o investidor autenticado acessar `/app`
- **THEN** SHALL visualizar resumo e acessos às seções `Investimentos`, `Banco` e `Bolsa`, com linguagem visual consistente com o login

#### Scenario: Header autenticado
- **WHEN** o investidor autenticado acessar qualquer página privada
- **THEN** o header SHALL mostrar as três seções principais e SHALL indicar a seção correspondente à página atual

#### Scenario: Sessão inválida
- **WHEN** a sessão deixar de ser válida ao navegar ou carregar uma seção
- **THEN** a interface SHALL ocultar dados privados e SHALL direcionar o investidor ao login conforme a proteção existente
