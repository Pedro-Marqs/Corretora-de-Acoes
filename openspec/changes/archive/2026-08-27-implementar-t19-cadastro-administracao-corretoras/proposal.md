## Why

As integrações externas necessárias para validar corretoras já estão isoladas e testáveis, mas ainda não existe o caso de uso que permita ao investidor pesquisar, associar e administrar corretoras da própria conta. A T19 implementa esse fluxo garantindo que somente instituições válidas, ativas e autorizadas como `CTVM` sejam utilizadas e que remoções ou recadastros preservem o histórico.

## What Changes

- Disponibilizar pesquisa de corretora exclusivamente por CNPJ.
- Consolidar dados cadastrais, endereço estruturado e situação regulatória utilizando os adapters implementados na T18.
- Aceitar para associação somente CNPJ cadastralmente ativo e identificado na CVM conforme a categoria `CTVM` exigida pelo projeto.
- Apresentar os dados consolidados antes da confirmação da associação.
- Criar ou reutilizar o cadastro institucional da corretora sem duplicar registros desnecessariamente.
- Associar a corretora exclusivamente à conta autenticada.
- Impedir mais de uma associação ativa da mesma corretora na mesma conta.
- Permitir consulta das corretoras ativas pertencentes à própria conta.
- Implementar remoção lógica da associação quando não existirem posições abertas vinculadas à corretora.
- Bloquear a remoção quando houver posição com quantidade maior que zero.
- Permitir recadastro/reativação de associação removida, preservando seu histórico anterior.
- Atualizar dados cadastrais válidos de corretoras já conhecidas quando novas consultas retornarem informações mais atuais.
- Preservar valores válidos anteriormente armazenados quando uma nova resposta externa vier incompleta.
- Garantir isolamento entre contas, impedindo consulta ou alteração de associações pertencentes a outro investidor.
- Mapear indisponibilidade ou resposta inválida das fontes externas para erros funcionais sem persistir alterações parciais.
- Não implementar a interface React de corretoras nesta tarefa; isso pertence à T20.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `corretoras`: completar o contrato de pesquisa, validação e administração de corretoras, incluindo exigência de CNPJ ativo e categoria `CTVM`, isolamento por conta, remoção lógica condicionada à ausência de posições abertas, recadastro com preservação histórica e proteção de dados válidos diante de respostas externas incompletas.

## Impact

- Backend Spring Boot responsável por corretoras.
- Controller e service de corretoras.
- Entidades e repositories de `Broker` e associação entre conta e corretora.
- Integração com os ports/adapters de CNPJ, CEP e CVM implementados na T18.
- Consultas de posição necessárias para autorizar ou bloquear remoções.
- Endpoints autenticados de pesquisa, associação, listagem e remoção de corretoras.
- Regras de autorização por conta autenticada.
- Persistência e atualização dos dados institucionais da corretora.
- Tratamento centralizado de erros de negócio e dependências externas.
- Testes de integração, autorização, duplicidade, remoção, recadastro e preservação histórica.
- Main spec `corretoras`, que receberá delta para refletir integralmente os requisitos já definidos em `docs/spec/corretoras.md`.
- Nenhuma implementação de interface frontend; a experiência React será tratada na T20.