## Context

A T13 será implementada sobre a fundação de frontend criada nas tarefas anteriores e sobre os endpoints de gerenciamento de conta já disponíveis no backend.

O frontend já possui roteamento público e privado, contexto de autenticação, cliente HTTP com suporte a sessão e CSRF, tratamento padronizado de erros e a página de configurações da conta. A implementação deve reutilizar esses padrões, sem introduzir uma nova camada arquitetural ou novas dependências.

O backend já fornece operações para inativação da conta autenticada, consulta da possibilidade de reativação e reativação. A inativação invalida a sessão atual, portanto o frontend também precisa limpar seu estado local de autenticação após a operação.

Os requisitos funcionais permanecem definidos nas specs existentes `exclusao-reativacao-conta` e `interface-estados`. Este change não altera esses requisitos.

See `proposal.md` - Why.

## Goals / Non-Goals

**Goals:**

- Integrar a interface existente com os endpoints de exclusão e reativação já fornecidos pelo backend.
- Reutilizar o cliente HTTP, tratamento de CSRF e modelo de erros já adotados no frontend.
- Integrar a exclusão à área autenticada da conta.
- Disponibilizar a reativação por uma rota pública.
- Manter o estado de autenticação do frontend consistente com a invalidação da sessão realizada pelo backend.
- Representar explicitamente estados de envio, validação, erro e sucesso.
- Impedir envios duplicados enquanto uma operação estiver em andamento.
- Cobrir os novos fluxos com testes de frontend.

**Non-Goals:**

- Alterar as regras de negócio de exclusão ou reativação.
- Alterar os endpoints ou contratos existentes do backend, salvo se for identificado um defeito que impossibilite o cumprimento das specs.
- Criar mecanismo adicional de verificação de identidade para reativação.
- Excluir fisicamente contas ou seus dados históricos.
- Refatorar componentes, autenticação ou infraestrutura HTTP fora do necessário para a T13.
- Adicionar novas bibliotecas de frontend.

## Decisions

### 1. Estender o módulo de API de contas existente

As operações de exclusão e reativação serão adicionadas ao módulo de API de contas já utilizado pelo frontend, mantendo centralizados os contratos relacionados à conta.

As novas operações deverão reutilizar o mecanismo existente de obtenção de token CSRF, envio de cookies de sessão, parsing das respostas e conversão de erros da API.

**Alternativa considerada:** criar um módulo exclusivo para exclusão e reativação.

**Decisão:** não criar um novo módulo, pois essas operações pertencem ao mesmo domínio das demais operações de conta e a separação adicionaria complexidade sem benefício para o escopo atual.

### 2. Integrar a exclusão à área autenticada da conta

O fluxo de inativação será iniciado a partir da área de configurações da conta já existente.

A interface coletará e-mail atual, senha atual e a confirmação textual exata `Excluir`. O frontend poderá realizar validações básicas de presença e confirmação, mas o backend continuará sendo a autoridade para validar credenciais e regras de negócio.

Após uma exclusão bem-sucedida, o estado local de autenticação será limpo imediatamente e o usuário será direcionado para uma rota pública.

**Alternativa considerada:** criar uma página autenticada independente exclusivamente para exclusão.

**Decisão:** manter o fluxo dentro das configurações da conta, pois ele faz parte do gerenciamento do ciclo de vida da própria conta e evita criar navegação desnecessária.

### 3. Implementar a reativação como fluxo público independente

A reativação será disponibilizada em uma rota pública para que uma conta inativa, que não pode realizar login normalmente, consiga iniciar o processo.

O fluxo utilizará primeiro a operação de consulta de reativação existente no backend. A interface apresentará as alternativas previstas pela resposta da API e permitirá prosseguir com a reativação quando aplicável.

A rota deve continuar acessível sem sessão autenticada e utilizar os mesmos padrões visuais e de estados das demais páginas públicas.

**Alternativa considerada:** incorporar a reativação diretamente ao formulário de login.

**Decisão:** usar uma página pública própria. Isso mantém login e reativação como responsabilidades distintas e permite representar com clareza as alternativas oferecidas ao usuário.

### 4. Reutilizar o fluxo de cadastro para criação de nova conta

Quando o fluxo informar que o usuário pode optar por criar uma nova conta em vez de reativar a anterior, a interface deverá direcioná-lo ao cadastro já existente em vez de implementar um segundo formulário de criação de conta.

A interface deverá informar que essa escolha cria uma conta distinta e não recupera o acesso à conta anterior.

**Alternativa considerada:** duplicar o formulário de cadastro dentro da página de reativação.

**Decisão:** reutilizar a rota de cadastro existente para evitar duplicação de validações, chamadas HTTP e comportamento.

### 5. Manter o backend como fonte de verdade das regras de negócio

Validações de interface serão utilizadas para feedback imediato e prevenção de submissões obviamente inválidas, mas nenhuma regra crítica será considerada satisfeita apenas por validação no navegador.

Erros funcionais retornados pela API deverão ser apresentados de forma apropriada, incluindo erros associados a campos quando disponíveis.

Erros técnicos deverão utilizar o padrão de estado de erro já existente e permitir nova tentativa quando aplicável.

**Alternativa considerada:** replicar integralmente as regras do backend no frontend.

**Decisão:** evitar duplicação das regras de negócio para reduzir divergência entre cliente e servidor.

### 6. Tratar explicitamente a perda da sessão após exclusão

A exclusão invalida a sessão no backend. Após resposta bem-sucedida, o frontend limpará o contexto local de autenticação sem tentar reutilizar a sessão e redirecionará o usuário para uma área pública.

Respostas `401` inesperadas durante operações autenticadas deverão continuar seguindo o padrão já existente para sessão expirada.

**Alternativa considerada:** depender somente da próxima requisição para descobrir que a sessão deixou de existir.

**Decisão:** sincronizar imediatamente o estado local após o sucesso da exclusão, evitando que a interface permaneça temporariamente em estado autenticado.

### 7. Testar comportamento pelo ponto de vista do usuário

Os testes de frontend deverão cobrir os fluxos relevantes com a API simulada, incluindo:

- submissão válida e inválida de exclusão;
- confirmação textual incorreta;
- tratamento de erros de campos e erros técnicos;
- limpeza da autenticação e redirecionamento após exclusão;
- consulta de possibilidade de reativação;
- reativação bem-sucedida e falha;
- alternativa para criação de nova conta;
- prevenção de submissões duplicadas;
- proteção das rotas autenticadas após a inativação.

Os testes deverão validar comportamento observável em vez de depender de detalhes internos dos componentes.

## Risks / Trade-offs

- **[Estado local permanecer autenticado após exclusão]** → Limpar explicitamente o contexto de autenticação imediatamente após a resposta bem-sucedida e redirecionar para uma rota pública.
- **[Duplicação de regras entre frontend e backend]** → Limitar a validação do frontend a feedback de interface e manter o backend como autoridade das regras funcionais.
- **[Usuário interpretar criação de nova conta como recuperação da anterior]** → Exibir claramente que a nova conta é independente e que a conta anterior continuará inacessível.
- **[Submissões repetidas produzirem múltiplas requisições]** → Desabilitar ações enquanto a requisição correspondente estiver em andamento, seguindo o padrão já utilizado no frontend.
- **[Falhas de API deixarem a interface sem saída]** → Preservar o contexto preenchido quando seguro e apresentar estado de erro com nova tentativa quando aplicável.
- **[Alterações desnecessárias em código consolidado das tarefas anteriores]** → Limitar as mudanças aos componentes, rotas, serviços e testes necessários para completar a T13.

## Migration Plan

Não há migração de banco de dados, alteração de modelo persistente ou nova dependência.

A implementação pode ser integrada incrementalmente:

1. adicionar ao cliente de contas as operações necessárias;
2. implementar e testar o fluxo autenticado de exclusão;
3. implementar a rota e o fluxo público de reativação;
4. integrar a alternativa de criação de nova conta ao cadastro existente;
5. executar os testes do frontend e verificar regressões nos fluxos de cadastro, login e conta;
6. executar lint e build antes de considerar a T13 concluída.

Em caso de regressão, as alterações da T13 podem ser revertidas sem migração de dados, pois os contratos e o modelo persistente existentes não são modificados.