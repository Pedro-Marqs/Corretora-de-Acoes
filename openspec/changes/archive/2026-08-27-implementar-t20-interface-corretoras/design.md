## Context

O backend implementado na T19 disponibiliza os casos de uso autenticados necessários para pesquisar, associar, listar e remover corretoras. A T20 deve apenas consumir esses contratos e representar seus resultados na interface.

O frontend já possui:

- `BrowserRouter` com área privada em `/app`;
- `AppLayout` compartilhado com navegação autenticada;
- página de carteira em `/app/carteira`;
- contexto de autenticação;
- tratamento de sessão expirada;
- cliente HTTP compartilhado com `API_BASE_URL`, cookies, parsing de JSON e CSRF;
- componentes reutilizáveis de carregamento, erro e mensagens;
- padrão de trava lógica com `useRef` para impedir mutações duplicadas;
- estilos e componentes responsivos usados pelas telas privadas.

A página de carteira já estabelece o padrão para uma tela privada que possui carregamento inicial, retry, sessão expirada, formulário, mutação assíncrona e atualização de dados após sucesso.

A T20 não possui delta de spec porque implementa no frontend comportamentos já definidos por `corretoras` e `interface-estados`.

Os nomes e caminhos exatos dos endpoints deverão ser obtidos dos contratos implementados localmente pela T19, sem inventar ou duplicar APIs.

See `proposal.md` - Why.

## Goals / Non-Goals

**Goals:**

- Criar uma página autenticada de corretoras integrada ao layout privado existente.
- Centralizar todas as chamadas da T19 em um módulo frontend próprio.
- Carregar e apresentar as corretoras ativas da conta.
- Permitir pesquisa exclusivamente por CNPJ.
- Exibir uma prévia consolidada antes da associação.
- Utilizar uma ação explícita de associação a partir dessa prévia.
- Atualizar a lista a partir do backend após associação ou remoção.
- Representar claramente lista vazia, loading, sucesso e erros recuperáveis.
- Apresentar bloqueio de remoção sem retirar a corretora da lista.
- Diferenciar sessão expirada dos demais erros.
- Impedir reenvio acidental de pesquisa, associação e remoção.
- Preservar dados relevantes da tela quando uma operação recuperável falhar.
- Manter comportamento utilizável em desktop, tablet e celular.
- Seguir os padrões já utilizados por `WalletPage` e demais telas privadas.

**Non-Goals:**

- Alterar endpoints ou regras de negócio da T19.
- Revalidar CNPJ ativo ou categoria `CTVM` no frontend.
- Consultar BrasilAPI, ViaCEP ou CVM diretamente pelo navegador.
- Permitir pesquisa por nome ou razão social.
- Permitir edição manual dos dados institucionais retornados.
- Exibir associações inativas ou histórico de recadastros.
- Implementar compra, venda ou transferência pela corretora.
- Criar gerenciamento global de estado.
- Adicionar biblioteca de formulários, modal, máscara ou gerenciamento de requisições.
- Implementar paginação se a API da T19 não a exigir.
- Criar nova regra de confirmação genérica para remoção que não esteja prevista no contrato atual.

## Decisions

### 1. Criar uma página dedicada de corretoras dentro de `/app`

A funcionalidade será apresentada em uma página privada própria, adicionada como rota filha do `AppLayout`.

A navegação privada receberá uma entrada `Corretoras`.

A página coordenará:

- carregamento da lista ativa;
- pesquisa por CNPJ;
- prévia da pesquisa;
- associação;
- remoção;
- mensagens funcionais.

Não será criado outro layout ou fluxo de autenticação.

**Alternativa considerada:** incorporar corretoras à `HomePage` ou `WalletPage`.

**Decisão:** corretoras possui fluxo próprio e será utilizada futuramente pelas operações financeiras, justificando uma área dedicada.

### 2. Criar um módulo frontend específico para a API de corretoras

As chamadas da T19 ficarão centralizadas em um módulo de API próprio, seguindo o padrão de `api/wallet.js`.

Esse módulo deverá:

- utilizar `API_BASE_URL`;
- enviar `credentials: 'include'`;
- utilizar CSRF nas operações mutáveis;
- utilizar `parseJson`;
- utilizar o agrupamento de erros de campos existente quando aplicável;
- converter falhas para um erro frontend próprio;
- validar minimamente a estrutura das respostas recebidas.

A página não realizará `fetch` diretamente.

Os endpoints exatos serão os implementados pela T19 no projeto local.

**Alternativa considerada:** espalhar chamadas HTTP dentro dos componentes.

**Decisão:** preservar a separação já adotada pela aplicação e evitar duplicação de cookies, CSRF e tratamento de erros.

### 3. Manter lista, pesquisa e mutações como estados independentes

A página não utilizará um único booleano global de carregamento.

Serão distinguidos conceitualmente:

**Lista**
- carregando;
- pronta;
- erro;
- sessão inválida.

**Pesquisa**
- vazia/ociosa;
- pesquisando;
- resultado disponível;
- erro.

**Associação**
- ociosa;
- enviando;
- sucesso;
- erro.

**Remoção**
- associação em processamento;
- sucesso;
- erro funcional.

Isso permite que, por exemplo, uma falha de pesquisa não substitua nem esconda a lista de corretoras já carregada.

**Alternativa considerada:** substituir toda a página por um loading ou erro sempre que qualquer chamada ocorrer.

**Decisão:** preservar o contexto já disponível e limitar cada estado à operação que o produziu.

### 4. Carregar a lista ativa ao entrar na página

Ao montar a página, o frontend consultará as corretoras ativas da conta autenticada.

Enquanto a primeira consulta estiver pendente, deverá ser exibido `LoadingState`.

Se a consulta falhar de maneira recuperável, deverá ser exibido `ErrorState` com retry.

Se a consulta retornar lista vazia, deverá ser utilizado `EmptyState`, sem tratar isso como erro.

A lista retornada pelo backend será considerada a fonte oficial do que está atualmente associado à conta.

### 5. Pesquisa aceitará somente CNPJ com validação estrutural no frontend

O campo aceitará entrada de CNPJ e poderá aplicar máscara visual.

Antes da chamada serão removidos caracteres de formatação.

A pesquisa somente será enviada quando houver exatamente 14 dígitos.

Essa validação existe apenas para feedback imediato.

O frontend não deverá decidir:

- se o CNPJ está ativo;
- se pertence a uma corretora;
- se consta na CVM;
- se é `CTVM`;
- se pode ser associado.

Essas decisões continuam exclusivamente no backend.

**Alternativa considerada:** reproduzir as regras da BrasilAPI/CVM no JavaScript.

**Decisão:** impedir duplicação de regras e divergência entre frontend e backend.

### 6. Alterar o CNPJ após uma pesquisa invalida a prévia anterior

Quando o investidor modificar o campo depois de uma pesquisa concluída, o resultado anterior deixará de ser considerado elegível para associação.

Isso evita situação em que a tela exiba um CNPJ no campo enquanto o botão de associação ainda represente outro resultado.

A associação utilizará o CNPJ pertencente à prévia validada, não uma combinação arbitrária do estado atual do formulário.

### 7. A prévia da pesquisa será a etapa de confirmação da associação

Uma pesquisa válida retornará os dados consolidados definidos pela T19.

A página exibirá, conforme disponibilizado pelo backend:

- CNPJ;
- razão social;
- nome fantasia;
- situação cadastral;
- categoria/regulação CVM;
- endereço estruturado.

A prévia apresentará uma ação explícita para associar aquela corretora.

Não será necessária uma página adicional de resumo nem um segundo modal de confirmação: visualizar a prévia e pressionar `Associar corretora` representa a confirmação do fluxo.

**Alternativa considerada:** associar automaticamente assim que a pesquisa retornar.

**Decisão:** a regra funcional exige apresentar os dados antes da confirmação da associação.

### 8. Dados da prévia serão somente leitura

Os dados retornados pela pesquisa não serão transformados em campos editáveis.

O formulário de associação não enviará ao backend razão social, endereço, situação ou categoria como fonte autoritativa.

A ação utilizará somente o identificador necessário pelo contrato da T19, principalmente o CNPJ pesquisado.

**Alternativa considerada:** permitir que o usuário corrija os dados externos antes de cadastrar.

**Decisão:** os dados institucionais vêm de fontes autoritativas e não do navegador.

### 9. Associação bem-sucedida será seguida de atualização da lista oficial

Após sucesso na associação, a interface não adicionará a corretora à lista apenas de forma otimista.

Será utilizado o resultado autoritativo da API quando ele representar integralmente a lista/associação ou, preferencialmente quando necessário, será executada nova consulta da lista ativa.

Depois do sucesso:

- a lista deverá refletir o estado oficial;
- a mensagem de sucesso deverá ser exibida;
- a prévia poderá ser encerrada;
- o campo de pesquisa poderá ser limpo.

**Alternativa considerada:** inserir localmente o objeto pesquisado na lista.

**Decisão:** a associação pode envolver atualização institucional ou reativação realizada pelo backend; recarregar evita divergência.

### 10. Remoção será feita diretamente a partir da associação listada

Cada item ativo disponibilizará a ação de remoção usando o identificador exigido pelo contrato da T19.

A tela não consultará novamente CNPJ, CEP ou CVM antes de remover.

Não será criada confirmação adicional obrigatória se ela não fizer parte do contrato existente.

Durante a solicitação:

- a mesma associação não poderá ser removida novamente;
- seu botão indicará estado pendente;
- o restante do conteúdo deverá permanecer visível.

**Alternativa considerada:** ocultar imediatamente a corretora através de remoção otimista.

**Decisão:** a operação pode ser bloqueada por posição aberta e somente o backend conhece o resultado autoritativo.

### 11. Remoção bloqueada preservará a corretora e mostrará o motivo no contexto

Quando o backend rejeitar a remoção por posição aberta:

- a corretora permanecerá na lista;
- nenhuma atualização otimista será aplicada;
- a mensagem funcional deverá aparecer próxima ao contexto da operação ou de forma claramente associada a ela;
- o botão deverá voltar a ficar disponível depois do término da requisição.

A interface não tentará descobrir por conta própria quais posições estão abertas.

### 12. Remoção bem-sucedida recarregará a lista ativa

Depois de remoção confirmada pelo backend, a lista será atualizada a partir da API.

Como o endpoint de listagem retorna somente associações ativas, a corretora removida deverá desaparecer naturalmente.

Se a última corretora for removida, a interface deverá passar para o estado vazio.

### 13. Usar travas lógicas além de botões desabilitados

Assim como em `WalletPage`, operações mutáveis possuirão trava lógica em `useRef` ou mecanismo equivalente.

Deverá existir proteção para:

- pesquisa duplicada do mesmo envio enquanto pendente;
- associação duplicada;
- remoção duplicada da mesma associação.

Desabilitar visualmente o botão não será a única proteção contra reentrada síncrona.

A trava de remoção deverá estar associada ao item em processamento ou representar de forma equivalente qual associação está pendente.

**Alternativa considerada:** depender somente da propriedade `disabled`.

**Decisão:** eventos repetidos podem ocorrer antes de uma nova renderização atualizar o botão.

### 14. Erros de operações não substituirão dados válidos já carregados

Se a lista já estiver disponível:

- falha na pesquisa não apagará a lista;
- falha na associação não apagará a prévia;
- falha na remoção não retirará a corretora;
- falha ao atualizar a lista depois de uma mutação deverá ser apresentada como erro de atualização, sem inventar um estado local como confirmado.

Quando aplicável, a interface oferecerá nova tentativa.

**Alternativa considerada:** retornar toda a página ao estado de erro para qualquer falha.

**Decisão:** `interface-estados` exige preservar contexto em falhas recuperáveis.

### 15. Não interpretar falhas externas diretamente pelo frontend

A T19 já converte as falhas da T18 em respostas funcionais.

O frontend deverá apresentar a mensagem/categoria disponibilizada pela API, sem tentar deduzir:

- BrasilAPI indisponível;
- ViaCEP indisponível;
- CVM indisponível;
- não CTVM;

a partir de regras próprias.

A interface apenas deverá garantir que uma indisponibilidade seja compreensível e não seja exibida como sucesso ou lista vazia.

**Alternativa considerada:** mapear cada fornecedor externo no React.

**Decisão:** o navegador não conhece nem deve conhecer a composição interna executada pelo backend.

### 16. Sessão expirada seguirá o mesmo fluxo das outras páginas privadas

Em qualquer chamada da página ou do módulo de corretoras, resposta de sessão inválida deverá:

1. interromper o fluxo atual;
2. limpar o contexto de autenticação;
3. redirecionar para `/login`;
4. apresentar a mensagem de sessão encerrada conforme o padrão existente.

Nenhum dado privado de corretoras deverá permanecer apresentado após esse estado.

A implementação poderá extrair lógica repetida somente se isso for uma pequena reutilização coerente; a T20 não deverá virar um refactor global de autenticação.

### 17. Reutilizar componentes comuns para os estados da interface

Serão reutilizados os componentes existentes, principalmente:

- `LoadingState`;
- `EmptyState`;
- `ErrorState`;
- `Message`.

Mensagens específicas de pesquisa, associação e remoção podem permanecer dentro da página quando forem altamente contextuais.

Não será criada uma segunda biblioteca de componentes assíncronos.

### 18. Não adicionar gerenciamento global ou nova biblioteca

A página utilizará estado local com hooks React.

Não será adicionado:

- Redux;
- Zustand;
- React Query/TanStack Query;
- biblioteca de formulários;
- biblioteca de máscaras;
- biblioteca de modal.

O fluxo é pequeno e já existe padrão suficiente no projeto para implementá-lo com React e utilitários atuais.

### 19. A responsividade seguirá a estrutura das telas privadas existentes

A tela deverá organizar pesquisa, prévia e lista sem depender de tabelas largas que causem rolagem horizontal da página.

Em viewport estreito:

- dados institucionais poderão ser apresentados em blocos ou cards;
- ações permanecerão acessíveis;
- CNPJ e textos longos deverão quebrar adequadamente;
- botões não poderão se sobrepor ao conteúdo.

A T20 deverá evoluir os estilos privados existentes em vez de criar um segundo sistema visual.

### 20. Testes de frontend utilizarão API simulada

Os testes deverão simular os contratos da T19 e cobrir pelo menos:

- carregamento inicial;
- lista vazia;
- listagem ativa;
- erro inicial e retry;
- CNPJ estruturalmente inválido sem chamada à API;
- pesquisa válida;
- instituição não elegível;
- indisponibilidade externa;
- prévia antes da associação;
- associação bem-sucedida;
- associação duplicada;
- prevenção de associação duplicada enquanto pendente;
- atualização da lista após associação;
- remoção bem-sucedida;
- remoção bloqueada por posição;
- prevenção de remoção duplicada;
- lista vazia depois da última remoção;
- sessão inválida;
- comportamento em viewport estreito.

Nenhum teste frontend deverá acessar BrasilAPI, ViaCEP ou CVM diretamente.

## Risks / Trade-offs

- **[Estado da página ficar complexo por possuir várias chamadas]** → Separar estados de lista, pesquisa, associação e remoção em vez de usar um único loading global.
- **[Resultado pesquisado ficar incompatível com o texto atual do campo]** → Invalidar a prévia sempre que o CNPJ for alterado.
- **[Dados externos serem adulterados antes da associação]** → Enviar apenas o identificador requerido pela T19 e deixar o backend revalidar os dados.
- **[Associação ser enviada mais de uma vez]** → Botão desabilitado mais trava lógica contra reentrada.
- **[Remoção ser enviada mais de uma vez]** → Controlar a associação em remoção e impedir segunda chamada para o mesmo item.
- **[Remoção otimista esconder corretora que possui posição aberta]** → Somente atualizar a lista depois de sucesso confirmado pelo backend.
- **[Erro de pesquisa apagar conteúdo útil]** → Manter lista e demais estados independentes.
- **[Falha externa parecer ausência de corretora]** → Exibir erro funcional retornado pelo backend, não `EmptyState`.
- **[Frontend duplicar regra CTVM]** → Limitar validação local ao formato do CNPJ.
- **[Sessão expirada deixar conteúdo privado visível]** → Limpar autenticação e sair da página privada imediatamente.
- **[T20 crescer para regra de negócio]** → Não acessar fontes externas nem modificar backend.
- **[Interface ficar larga por causa dos dados de endereço]** → Preferir layout responsivo em cards/blocos em vez de tabela rígida.

## Migration Plan

1. Criar módulo frontend de API para os contratos de corretoras da T19.
2. Criar página privada de corretoras.
3. Adicionar rota no agrupamento `/app`.
4. Adicionar `Corretoras` à navegação do `AppLayout`.
5. Implementar carregamento e estado vazio da lista ativa.
6. Implementar campo de pesquisa e validação estrutural de CNPJ.
7. Implementar prévia consolidada em modo somente leitura.
8. Implementar associação com proteção contra reenvio.
9. Atualizar a lista após associação.
10. Implementar remoção com estado por associação e mensagem de bloqueio.
11. Atualizar a lista após remoção.
12. Integrar tratamento de sessão inválida e demais erros funcionais.
13. Ajustar estilos responsivos.
14. Cobrir os fluxos com testes frontend e executar build completo.

Nenhuma migration de banco ou alteração backend é necessária.

O rollback consiste em remover a rota, entrada de navegação, página, módulo de API e estilos/testes adicionados pela T20. Os endpoints e dados criados pela T19 permanecem inalterados.