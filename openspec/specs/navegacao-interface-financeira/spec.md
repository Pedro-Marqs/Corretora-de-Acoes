# navegacao-interface-financeira Specification

## Purpose
Organizar a área privada em uma navegação financeira simples, previsível e adequada ao uso móvel, conectando investimentos, banco, bolsa e as ações da conta sem duplicar regras de negócio.

## Requirements

### Requirement: Navegação privada por seções

A área autenticada SHALL apresentar no header exatamente as seções principais `Investimentos`, `Banco` e `Bolsa`, com indicação clara da seção ativa. A navegação SHALL manter acessíveis as funcionalidades existentes que pertençam a cada contexto, sem expor rotas privadas a uma sessão inválida.

#### Scenario: Header autenticado
- **WHEN** o investidor autenticado acessar qualquer página privada
- **THEN** o header SHALL mostrar as três seções principais e SHALL indicar a seção correspondente à página atual

#### Scenario: Sessão inválida
- **WHEN** a sessão deixar de ser válida ao navegar ou carregar uma seção
- **THEN** a interface SHALL ocultar dados privados e SHALL direcionar o investidor ao login conforme a proteção existente

### Requirement: Menu da conta do investidor

O header SHALL apresentar o nome do investidor como controle de conta. Ao ser acionado, o controle SHALL expandir um menu contendo somente as opções `Configurações` e `Sair`, SHALL permitir fechamento por nova ativação, clique externo ou teclado, e SHALL preservar foco visível.

#### Scenario: Abrir menu do usuário
- **WHEN** o investidor ativar seu nome no header
- **THEN** a interface SHALL expandir o menu com `Configurações` e `Sair`, sem criar opções de navegação não especificadas

#### Scenario: Sair da conta
- **WHEN** o investidor selecionar `Sair`
- **THEN** a interface SHALL executar o logout existente, impedir duplicidade durante a solicitação e conduzir ao fluxo público após confirmação de encerramento

### Requirement: Organização visual das seções

Investimentos SHALL ser o contexto da carteira e do patrimônio, Banco SHALL ser o contexto do dinheiro e corretoras, e Bolsa SHALL ser o contexto de pesquisa de ativos. A troca de seção SHALL preservar os contratos e dados oficiais já existentes, sem criar estado financeiro local como fonte de verdade.

#### Scenario: Troca de contexto
- **WHEN** o investidor selecionar outra seção do header
- **THEN** a interface SHALL abrir a página correspondente sem misturar controles de banco, carteira e pesquisa de ativos na mesma navegação principal
