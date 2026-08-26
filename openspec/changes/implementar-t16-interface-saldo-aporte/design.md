## Context

O frontend já possui a fundação criada nas tarefas anteriores: rotas públicas e privadas, layout autenticado, contexto de autenticação, cliente HTTP com suporte a cookies e CSRF, componentes comuns para estados assíncronos e formatação compartilhada de valores monetários.

A T15 disponibilizou no backend os casos de uso autenticados de consulta de saldo e realização de aporte. A T16 deve apenas consumir esses contratos, sem duplicar regras financeiras ou alterar a fonte oficial do saldo.

A área privada atual possui uma página inicial simples e uma página de conta. Ainda não existe uma interface específica para carteira, saldo ou movimentações financeiras.

See `proposal.md` - Why e a delta spec `interface-estados` deste change.

## Goals / Non-Goals

**Goals:**

- Criar uma interface autenticada de carteira que apresente o saldo atual.
- Integrar o frontend diretamente aos endpoints disponibilizados pela T15.
- Permitir que o investidor informe e confirme um aporte sem sair da tela.
- Atualizar o saldo exibido após uma operação concluída.
- Reutilizar o cliente HTTP, CSRF, autenticação, formatadores e componentes assíncronos existentes.
- Impedir múltiplos envios do mesmo aporte enquanto houver solicitação em andamento.
- Apresentar validações e erros funcionais sem perder desnecessariamente o contexto preenchido pelo usuário.
- Manter a experiência funcional em desktop, tablet e celular.

**Non-Goals:**

- Alterar regras de saldo ou aporte do backend.
- Recalcular saldo no frontend como fonte de verdade.
- Criar histórico de movimentações.
- Implementar compra, venda ou transferência.
- Implementar dashboards ou indicadores de rentabilidade.
- Criar gerenciamento global complexo de estado.
- Adicionar biblioteca de formulários ou nova dependência de UI.
- Alterar banco de dados ou APIs backend sem necessidade comprovada.

## Decisions

### 1. Criar uma área explícita de carteira dentro da navegação privada

A funcionalidade será apresentada em uma página de carteira dentro do layout autenticado existente.

A navegação privada receberá acesso para essa área sem criar um segundo layout ou fluxo de autenticação.

A página será responsável por coordenar:

- carregamento inicial do saldo;
- apresentação do saldo;
- formulário de aporte;
- confirmação;
- sucesso e erro da operação.

**Alternativa considerada:** incorporar todo o fluxo diretamente na página `HomePage`.

**Decisão:** usar uma página de carteira dedicada para manter funcionalidades financeiras separadas da página inicial e permitir que operações futuras utilizem a mesma área.

### 2. Centralizar chamadas da carteira em um módulo de API próprio

Será criado um módulo frontend específico para consumir os contratos disponibilizados pela T15.

Esse módulo deverá:

- usar o `API_BASE_URL` já configurado;
- enviar cookies com `credentials: 'include'`;
- obter e enviar token CSRF para o aporte;
- interpretar respostas de erro no formato já utilizado pelo frontend;
- retornar somente dados necessários à interface.

A página não deverá chamar `fetch` diretamente.

**Alternativa considerada:** colocar as chamadas em `api/accounts.js`.

**Decisão:** separar operações financeiras de operações de identidade/conta para manter responsabilidades claras e facilitar reutilização em tarefas futuras.

### 3. O backend continuará sendo a fonte oficial do saldo

A interface poderá atualizar visualmente o saldo somente a partir de resposta confirmada da API.

O frontend não deverá calcular:

`saldo atual + aporte`

como resultado oficial da operação.

Depois de um aporte bem-sucedido, deverá utilizar o saldo retornado pelo contrato da T15 ou realizar nova consulta ao saldo, conforme o contrato existente no backend.

**Alternativa considerada:** aplicar atualização otimista imediatamente após o clique.

**Decisão:** evitar atualização otimista em valores financeiros para impedir divergência caso a operação seja rejeitada ou revertida no backend.

### 4. A validação frontend será apenas uma primeira barreira de experiência

Antes da confirmação, o formulário deverá rejeitar entradas evidentemente inválidas, incluindo:

- valor ausente;
- valor não numérico;
- zero;
- valor negativo;
- valor inferior a R$ 10,00.

Essa validação melhora a resposta ao usuário, mas não substitui as regras da T15.

Erros retornados pelo backend continuarão sendo tratados como autoridade final.

**Alternativa considerada:** depender exclusivamente da validação do backend.

**Decisão:** validar também no frontend para resposta imediata sem duplicar cálculos financeiros complexos.

### 5. A confirmação ocorrerá na mesma tela

Após preencher um aporte válido, a interface deverá apresentar uma confirmação simples antes do envio.

A confirmação deverá informar de forma clara o valor que será aportado e permitir:

- confirmar;
- cancelar.

O cancelamento não deverá disparar nenhuma requisição e deverá manter o usuário na mesma área.

Não será criada uma página intermediária de resumo.

**Alternativa considerada:** navegar para uma segunda página de confirmação.

**Decisão:** utilizar confirmação local à própria operação, conforme a spec `interface-estados`.

### 6. Estados de envio serão controlados explicitamente

A página manterá estados suficientes para distinguir:

- carregamento inicial;
- saldo carregado;
- erro ao consultar saldo;
- formulário disponível;
- confirmação pendente;
- aporte sendo enviado;
- aporte concluído;
- erro no aporte.

Durante o envio do aporte, os controles que poderiam disparar novamente a operação deverão permanecer desabilitados.

Além do estado visual, o manipulador da ação deverá impedir reentrada caso eventos duplicados ocorram rapidamente.

**Alternativa considerada:** confiar apenas no atributo `disabled` do botão.

**Decisão:** combinar estado visual e proteção lógica, seguindo o padrão já utilizado pelo frontend em operações sensíveis.

### 7. Reutilizar os componentes comuns de estados assíncronos

Os componentes existentes de carregamento, erro e mensagem deverão ser reutilizados quando adequados.

A consulta inicial utilizará:

- estado de carregamento enquanto aguarda a API;
- estado de erro funcional com opção de nova tentativa;
- conteúdo da carteira após sucesso.

O aporte deverá utilizar mensagem de sucesso ou erro sem substituir toda a tela da carteira.

**Alternativa considerada:** criar componentes exclusivos de loading e erro para a T16.

**Decisão:** reutilizar a fundação da T11 para manter consistência visual e reduzir código duplicado.

### 8. Reutilizar o formatador monetário existente

Todo saldo e valor apresentado ao usuário deverá utilizar o formatador compartilhado de BRL já existente no frontend.

O valor bruto retornado pela API deverá permanecer separado do texto formatado exibido.

**Alternativa considerada:** formatar manualmente com concatenação de `R$` e casas decimais.

**Decisão:** reutilizar `formatCurrency` para manter comportamento consistente entre telas.

### 9. Sessão inválida seguirá o fluxo global de autenticação

A carteira continuará dentro de uma `PrivateRoute`.

Caso a consulta ou o aporte indiquem que a sessão deixou de ser válida, a aplicação deverá limpar o contexto autenticado e encaminhar o usuário para o fluxo de login de maneira compatível com o comportamento já adotado pelo frontend.

Nenhum saldo anteriormente carregado deverá permanecer visível após perda da autenticação.

**Alternativa considerada:** apresentar somente uma mensagem de erro dentro da carteira.

**Decisão:** tratar perda de autenticação como mudança de sessão, não como erro funcional comum da operação.

### 10. Não adicionar dependências para formulário ou confirmação

O fluxo possui complexidade pequena e pode ser implementado com os recursos já usados pelo projeto.

Não serão introduzidos:

- biblioteca de gerenciamento de formulários;
- biblioteca de modal;
- gerenciamento global adicional;
- biblioteca monetária no frontend.

Componentes locais e estado React serão suficientes.

**Alternativa considerada:** introduzir biblioteca específica para formulário/modal.

**Decisão:** evitar custo e complexidade desnecessários para uma única operação simples.

## Risks / Trade-offs

- **[Saldo visual divergir do backend]** → Atualizar o saldo somente a partir de resposta confirmada ou nova consulta à API, sem atualização otimista.
- **[Aporte ser enviado duas vezes]** → Desabilitar controles durante a solicitação e aplicar proteção lógica contra reentrada.
- **[Regra de R$ 10,00 ficar duplicada entre frontend e backend]** → Tratar a validação frontend apenas como experiência; o backend permanece autoridade final.
- **[Erro apagar o valor digitado]** → Preservar o formulário quando a operação falhar, salvo quando houver motivo funcional para limpá-lo.
- **[Confirmação gerar navegação desnecessária]** → Manter confirmação na própria tela.
- **[Código duplicar cliente HTTP/CSRF]** → Reutilizar as funções e padrões já existentes em `api/http.js`.
- **[Sessão expirada deixar saldo privado visível]** → Limpar contexto/dados da carteira ao receber resposta de não autenticação e direcionar para login.
- **[T16 crescer para dashboard ou histórico]** → Limitar a página a saldo e aporte; demais informações serão implementadas nas tarefas específicas.
- **[Responsividade quebrar com formulário e confirmação]** → Reutilizar estilos e padrões responsivos existentes e testar viewports de desktop, tablet e celular.

## Migration Plan

1. Criar o módulo frontend de API da carteira consumindo os endpoints existentes da T15.
2. Criar a página/componentes de carteira com consulta do saldo.
3. Integrar a página à rota e navegação privada existentes.
4. Implementar formulário e validação básica do aporte.
5. Implementar confirmação simples antes da execução.
6. Integrar envio com CSRF e bloqueio contra solicitações duplicadas.
7. Atualizar o saldo após sucesso e apresentar mensagens funcionais.
8. Cobrir carregamento, erro, retry, confirmação, cancelamento e sessão inválida em testes.
9. Verificar responsividade e executar a suíte frontend completa.

Nenhuma migration de banco ou alteração estrutural do backend é prevista. O rollback consiste em remover a rota, a integração frontend e os componentes adicionados, preservando integralmente a API implementada na T15.