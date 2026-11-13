# interface-fintech-global Specification

## Purpose
Estabelecer uma linguagem visual e de interação única, premium e acessível para todas as áreas existentes da aplicação, preservando os contratos funcionais, financeiros e de segurança já consolidados.

## Requirements

### Requirement: Design system visual compartilhado

A aplicação SHALL apresentar as áreas de investidor e administrador com o mesmo design system, baseado em tema escuro elegante, verde-esmeralda como cor primária, superfícies em camadas, tipografia legível, espaçamento consistente, bordas, sombras, estados de foco e semântica visual documentados. A identidade SHALL permanecer própria e MUST NOT copiar marca, textos, dados, imagens ou elementos proprietários da referência visual.

#### Scenario: Alternar entre áreas da aplicação

- **WHEN** o usuário navegar entre uma tela do investidor e uma tela administrativa autorizada
- **THEN** a estrutura visual, os tokens, os componentes de interação e os estados de foco SHALL permanecer reconhecíveis como uma única identidade, com hierarquia e navegação específicas para cada papel

#### Scenario: Uso de marca de referência

- **WHEN** uma tela for implementada com base na imagem de referência
- **THEN** a interface SHALL reutilizar somente princípios de composição, contraste, densidade e organização, sem reproduzir a marca, os textos ou os dados apresentados na imagem

### Requirement: Cobertura visual de todas as rotas existentes

Todas as rotas públicas, privadas do investidor e administrativas existentes SHALL adotar o design system, incluindo cadastro, login, reativação, dashboard, início, banco, ativos, detalhe de ativo, carteira, negociação, transferências, histórico, corretoras, conta/perfil, login administrativo, dashboard administrativo, gestão de investidores e página não encontrada. Componentes compartilhados SHALL cobrir formulários, tabelas, filtros, confirmações, carregamento, vazio, erro, mensagens e navegação sem alterar a autorização das rotas.

#### Scenario: Abrir uma tela pública

- **WHEN** o visitante acessar cadastro, login, reativação ou página não encontrada em viewport desktop, tablet ou mobile
- **THEN** a tela SHALL usar a mesma fundação visual, SHALL manter seus campos e mensagens funcionais e SHALL continuar sem exibir dados privados

#### Scenario: Abrir uma tela privada do investidor

- **WHEN** um investidor autenticado acessar qualquer rota sob `/app`
- **THEN** a tela SHALL usar o layout privado compartilhado, SHALL indicar a seção ativa e SHALL preservar a proteção que redireciona sessões inválidas sem renderizar conteúdo privado

#### Scenario: Abrir uma tela administrativa

- **WHEN** um administrador autenticado acessar `/admin` ou `/admin/investors`
- **THEN** a tela SHALL usar o layout administrativo do mesmo design system, SHALL manter a separação visual e técnica do fluxo do investidor e SHALL exibir somente dados autorizados pela API administrativa

### Requirement: Fidelidade aos dados financeiros oficiais

As visualizações SHALL usar exclusivamente valores, séries, cotações, câmbio, posições, movimentos e métricas recebidos dos contratos reais da API ou de uma nova consulta autenticada. A interface MUST NOT inventar rentabilidade, pontos históricos, preços, saldos, posições, resultados ou agregações locais que possam contradizer o backend. BRL e USD SHALL permanecer identificados e separados, e movimentação financeira SHALL NOT ser apresentada como desempenho patrimonial.

#### Scenario: Dashboard do investidor com patrimônio e movimentos

- **WHEN** `GET /api/dashboard` retornar saldo, posições, `patrimonyHistory` e `investmentHistory`
- **THEN** a interface SHALL representar o patrimônio com `patrimonyHistory`, SHALL distinguir a série de investimento quando exibida e SHALL rotular moeda e resultado conforme os campos oficiais

#### Scenario: Série histórica ausente ou incompleta

- **WHEN** a API não retornar pontos suficientes para uma visualização
- **THEN** a interface SHALL mostrar estado vazio ou informação indisponível, SHALL manter os valores oficiais disponíveis e SHALL NOT preencher a série com dados estimados, zero artificial ou interpolação local; quando a API fornecer uma âncora oficial do início do período, SHALL renderizá-la como primeiro ponto

#### Scenario: Dashboard administrativo por período

- **WHEN** o administrador mudar o período do dashboard
- **THEN** `totalAccounts`, `activeAccounts` e `inactiveAccounts` SHALL continuar representando o estado atual, `newAccounts` SHALL representar contas criadas no período e contagens, ações negociadas, volumes e séries operacionais SHALL respeitar o período retornado pela API

#### Scenario: Volume administrativo com moedas distintas

- **WHEN** o dashboard administrativo retornar volumes BRL/USD com conversão disponível, indisponível ou não aplicável
- **THEN** a interface SHALL exibir BRL e USD separadamente, SHALL comunicar a situação da conversão sem fabricar equivalente e SHALL manter o ranking agregado sem expor dados individuais

### Requirement: Visualizações, tabelas e filtros acessíveis

Gráficos, cards de métricas, tabelas e filtros SHALL ter hierarquia visual consistente, rótulos compreensíveis, estados de carregamento e erro, alternativa textual ou tabular para dados relevantes, foco visível e operação completa por teclado. Tabelas SHALL preservar identificação de colunas, ordenação, paginação e filtros oficiais quando o contrato os fornecer; gráficos SHALL comunicar unidade, período, moeda e ausência de dados.

#### Scenario: Consultar gráfico patrimonial

- **WHEN** o investidor visualizar o gráfico de patrimônio
- **THEN** o gráfico SHALL informar que representa evolução patrimonial oficial, SHALL indicar o período e a moeda e SHALL oferecer uma forma acessível de consultar os pontos sem depender exclusivamente de cor ou posição visual

#### Scenario: Operar tabela com teclado

- **WHEN** o usuário navegar por uma tabela, filtro, paginação ou ação de confirmação usando teclado
- **THEN** a ordem de foco SHALL ser lógica, o foco SHALL permanecer visível, os controles SHALL possuir nome acessível e nenhuma ação SHALL depender somente de hover, arraste ou cor

#### Scenario: Filtrar histórico ou investidores

- **WHEN** o usuário aplicar, remover ou combinar filtros em histórico ou gestão de investidores
- **THEN** os filtros SHALL manter rótulos e valores compreensíveis, SHALL indicar carregamento da nova consulta, SHALL preservar paginação coerente com a resposta oficial e SHALL comunicar lista vazia ou erro recuperável

### Requirement: Responsividade sem perda de função

As telas SHALL funcionar em desktop, tablet e mobile a partir de 320 CSS pixels, reorganizando navegação, cards, gráficos, tabelas, formulários, modais e ações sem ocultar informação essencial, criar rolagem horizontal involuntária ou exigir gestos não acessíveis. A densidade visual SHALL ser adaptada ao espaço disponível sem alterar os dados ou contratos enviados.

#### Scenario: Dashboard em mobile

- **WHEN** o investidor ou administrador abrir um dashboard em viewport estreita
- **THEN** os indicadores SHALL refluír para uma ordem legível, os gráficos SHALL permanecer consultáveis, os controles SHALL ser acionáveis e nenhuma métrica essencial SHALL ser cortada ou substituída por placeholder

#### Scenario: Tabela larga em mobile

- **WHEN** uma tabela possuir mais colunas que o espaço disponível
- **THEN** a interface SHALL aplicar uma estratégia responsiva documentada, como reordenação, composição por cartões ou rolagem contida e acessível, sem deslocar o layout inteiro da página nem remover os campos funcionais

### Requirement: Estados e confirmações consistentes

Toda consulta ou mutação visual SHALL apresentar estados distintos de carregamento, sucesso, vazio, erro recuperável, erro de autorização e conflito quando aplicável. A interface SHALL impedir reenvio acidental, preservar entradas recuperáveis após rejeição e somente declarar sucesso com base na resposta oficial, sem alterar localmente saldo, posição, status de investidor ou sessão antes da confirmação do backend.

#### Scenario: Mutação financeira rejeitada

- **WHEN** compra, venda, aporte, retirada ou transferência for rejeitada pela API
- **THEN** a interface SHALL exibir a mensagem funcional segura, manter o contexto preenchido quando possível e SHALL NOT apresentar saldo, posição ou histórico como se a operação tivesse sido concluída

#### Scenario: Desativação administrativa rejeitada ou conflitante

- **WHEN** a alteração de status de um investidor retornar erro de autorização, conflito ou regra de negócio
- **THEN** a interface SHALL comunicar o resultado sem indicar alteração efetivada, SHALL atualizar a lista somente após sucesso confirmado e SHALL manter a sessão administrativa protegida

#### Scenario: Sessão expirada

- **WHEN** a API rejeitar uma requisição por sessão inválida ou token revogado
- **THEN** a interface SHALL limpar o contexto correspondente, ocultar dados privados e encaminhar o usuário ao login correto, sem misturar sessão de investidor com sessão administrativa
