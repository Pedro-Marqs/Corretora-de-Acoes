## Why

Os casos de uso de pesquisa, associação, listagem e remoção de corretoras já estão disponíveis no backend, mas o investidor ainda não consegue utilizá-los pela área autenticada da aplicação. A T20 completa esse fluxo no frontend, conectando a interface às APIs da T19 e apresentando adequadamente resultados, bloqueios e falhas das integrações externas.

## What Changes

- Criar uma área autenticada de corretoras no frontend.
- Adicionar acesso à área de corretoras na navegação privada existente.
- Criar pesquisa de corretora exclusivamente por CNPJ.
- Validar o formato básico do CNPJ no frontend antes da chamada à API.
- Exibir os dados consolidados retornados pelo backend antes da confirmação da associação.
- Permitir confirmar a associação da corretora pesquisada.
- Listar somente as corretoras ativas retornadas para a conta autenticada.
- Permitir solicitar a remoção de uma associação ativa.
- Exibir mensagem funcional quando a remoção for bloqueada por posição aberta.
- Exibir estados distintos para carregamento, lista vazia, sucesso e erro.
- Exibir de forma compreensível indisponibilidade das fontes externas utilizada pelo backend.
- Impedir pesquisas, associações ou remoções duplicadas enquanto a respectiva solicitação estiver em andamento.
- Atualizar a lista de corretoras após associação ou remoção concluída com sucesso.
- Preservar o contexto da tela quando uma operação recuperável falhar.
- Reutilizar autenticação, cliente HTTP, CSRF, componentes assíncronos e padrões visuais já existentes.
- Manter a interface funcional em desktop, tablet e celular.
- Não alterar regras de negócio, persistência ou integrações externas implementadas nas T18 e T19.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. A T20 implementa no frontend comportamentos já definidos pelas capabilities `corretoras` e `interface-estados`, sem introduzir novos requisitos funcionais.

## Impact

- Frontend React da área autenticada.
- Página e componentes de corretoras.
- Serviço frontend responsável pelas chamadas da API de corretoras.
- Navegação e rotas privadas.
- Formulário de pesquisa por CNPJ.
- Apresentação do resultado consolidado e confirmação de associação.
- Listagem e remoção de corretoras ativas.
- Componentes compartilhados de carregamento, vazio, sucesso e erro.
- Tratamento frontend dos erros funcionais e de indisponibilidade retornados pela T19.
- Testes de componentes e integração frontend/API.
- Nenhuma alteração de banco de dados ou regra de negócio do backend.