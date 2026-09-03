# Corretoras Specification

## Purpose

Permitir cadastrar e associar corretoras as contas de investidores.

## Requirements

### Requirement: Cadastro de corretora

O sistema SHALL permitir pesquisar corretoras exclusivamente por CNPJ e SHALL manter seus dados institucionais sem duplicar registros desnecessariamente. Uma corretora somente poderá ser considerada válida para associação quando possuir CNPJ cadastralmente ativo e constar na fonte oficial da CVM conforme a categoria `CTVM` exigida pelo projeto.

#### Scenario: Criar corretora

- **WHEN** uma corretora com CNPJ ativo e categoria `CTVM` for confirmada para cadastro
- **THEN** o sistema SHALL persistir ou atualizar seu cadastro institucional com os dados válidos consolidados

#### Scenario: Pesquisa por CNPJ

- **WHEN** o investidor autenticado pesquisar uma corretora
- **THEN** o sistema SHALL aceitar somente CNPJ como identificador de pesquisa e SHALL consolidar os dados cadastrais, de endereço e regulatórios disponíveis

#### Scenario: CNPJ inativo

- **WHEN** o CNPJ pesquisado existir mas estiver cadastralmente inativo
- **THEN** o sistema SHALL rejeitar a corretora como elegível para associação e informar o motivo

#### Scenario: Instituicao nao autorizada

- **WHEN** o CNPJ estiver ativo mas não satisfizer a categoria `CTVM` exigida na fonte oficial da CVM
- **THEN** o sistema SHALL rejeitar a corretora como elegível para associação e informar o motivo

#### Scenario: Fonte externa indisponivel

- **WHEN** uma fonte externa necessária à validação estiver indisponível e não for possível concluir a verificação obrigatória
- **THEN** o sistema SHALL rejeitar a conclusão da pesquisa ou associação sem tratar a indisponibilidade como reprovação cadastral ou regulatória

### Requirement: Associar corretora a conta
O sistema SHALL permitir que a conta autenticada associe corretoras válidas e ativas, SHALL impedir mais de uma associação ativa da mesma corretora na mesma conta e MUST limitar consulta e administração às associações pertencentes ao próprio investidor. Uma compra SHALL aceitar somente uma associação ativa pertencente à conta autenticada.

#### Scenario: Associacao valida
- **WHEN** o investidor autenticado confirmar a associação de uma corretora válida sem associação ativa equivalente
- **THEN** o sistema SHALL criar ou reativar a associação para a própria conta

#### Scenario: Associacao duplicada
- **WHEN** a conta tentar associar novamente a mesma corretora que já possui associação ativa
- **THEN** o sistema SHALL rejeitar a duplicidade sem criar nova associação

#### Scenario: Consultar corretoras ativas
- **WHEN** o investidor autenticado consultar suas corretoras disponíveis
- **THEN** o sistema SHALL retornar somente as associações ativas pertencentes à própria conta

#### Scenario: Compra com corretora não elegível
- **WHEN** o investidor tentar comprar usando corretora inativa, inexistente ou pertencente a outra conta
- **THEN** o sistema SHALL rejeitar a operação sem expor ou modificar dados da corretora

#### Scenario: Isolamento entre contas
- **WHEN** o investidor tentar consultar ou alterar uma associação pertencente a outra conta
- **THEN** o sistema MUST rejeitar a operação sem expor ou modificar os dados da outra conta

### Requirement: Remocao e recadastro de corretora

O sistema SHALL permitir remoção lógica de uma associação de corretora somente quando ela não possuir posições abertas e SHALL preservar seu histórico para permitir posterior recadastro.

#### Scenario: Remocao permitida

- **WHEN** o investidor remover uma corretora da própria conta que não possua posição com quantidade maior que zero
- **THEN** o sistema SHALL inativar logicamente a associação, removê-la das opções operacionais e preservar seu histórico

#### Scenario: Remocao bloqueada

- **WHEN** o investidor tentar remover uma corretora que possua posição com quantidade maior que zero
- **THEN** o sistema SHALL rejeitar a remoção e manter a associação ativa

#### Scenario: Recadastro de corretora

- **WHEN** o investidor associar novamente uma corretora anteriormente removida da própria conta e ela continuar válida
- **THEN** o sistema SHALL reativar a associação preservando o histórico anterior sem criar uma segunda associação ativa

### Requirement: Preservacao de dados cadastrais validos

O sistema SHALL atualizar dados cadastrais válidos de uma corretora conhecida quando novas informações válidas estiverem disponíveis e MUST NOT apagar valores válidos já armazenados apenas porque uma nova resposta externa estiver incompleta.

#### Scenario: Atualizacao com dados validos

- **WHEN** uma nova pesquisa de corretora conhecida retornar nome ou endereço válido mais atual
- **THEN** o sistema SHALL atualizar os respectivos dados institucionais preservando a identidade da corretora

#### Scenario: Resposta incompleta

- **WHEN** uma nova consulta externa de corretora conhecida retornar campos ausentes enquanto existirem valores válidos anteriormente armazenados
- **THEN** o sistema MUST preservar os valores válidos anteriores em vez de substituí-los por valores ausentes
