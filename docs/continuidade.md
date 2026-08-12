# Continuidade do projeto

## Finalidade deste arquivo

Este documento registra o contexto consolidado das conversas sobre o projeto, as decisões já tomadas e o ponto em que o trabalho parou.

Ao retomar o projeto em outra conversa ou máquina, o assistente deverá:

1. Ler este arquivo;
2. Ler os documentos citados na seção **Documentos de referência**;
3. Não inventar respostas para as pendências;
4. Continuar a partir da seção **Próximo passo**;
5. Atualizar este arquivo sempre que novas decisões relevantes forem confirmadas.

## Documentos de referência

- `docs/01-visao.md`: visão inicial do projeto;
- `docs/01,1-trabalho.md`: enunciado e regras da faculdade;
- `docs/02-pesquisa.md`: pesquisa do projeto;
- `docs/03-requisitos.md`: requisitos a serem consolidados;
- `docs/04-arquitetura.md`: arquitetura a ser definida;
- `docs/05-specs.md`: especificações a serem produzidas;
- `docs/06-tarefas.md`: tarefas a serem planejadas.

## Estado atual

Está em andamento uma entrevista de levantamento de requisitos. O assistente deve atuar como analista de requisitos, fazendo no máximo cinco perguntas por rodada, sem escrever código, propor prematuramente uma solução ou inventar respostas.

Já foram realizadas três rodadas completas de perguntas. A quarta rodada foi apresentada e ainda não foi respondida.

## Visão consolidada

O projeto é um sistema acadêmico de simulação de investimentos em ações brasileiras e norte-americanas. Pessoas físicas poderão cadastrar corretoras, simular compras e vendas, transferir ações entre corretoras e acompanhar sua carteira por meio de históricos e dashboards.

As operações são apenas simuladas. Não há vínculo real com contas bancárias nem envio de ordens para bolsas ou corretoras.

## Decisões confirmadas

### Usuários e contas

- O sistema terá contas individuais.
- O usuário deverá se cadastrar antes de utilizar o sistema.
- Haverá somente um perfil: pessoa física investidora.
- Uma conta não terá vínculo ou compartilhamento de dados com outras contas.
- O cadastro solicitará nome, CPF, e-mail e senha.
- O usuário poderá alterar seus dados e excluir sua conta na área de configurações da conta.
- A recuperação de senha será simples: ao selecionar “Esqueci minha senha”, o usuário receberá um e-mail para definir uma nova senha.
- Para excluir a conta, o usuário deverá confirmar o e-mail e a senha e escrever a palavra `Excluir`.
- O histórico deverá continuar armazenado no banco de dados depois da exclusão da conta. Ainda é necessário definir como preservar esse histórico em relação aos dados pessoais.

### Saldo e custódia

- Cada conta começará com saldo de R$ 10.000,00.
- O saldo em dinheiro pertence à conta do usuário, e não a uma corretora.
- Todas as corretoras cadastradas pelo usuário utilizam o mesmo saldo disponível.
- As ações ficam vinculadas diretamente à corretora em que foram compradas ou para a qual foram transferidas.
- O saldo e a posição da carteira devem variar a cada movimentação.

### Compra e venda

- Compras e vendas serão simulações instantâneas a preço de mercado.
- Não haverá criação ou gerenciamento de ordens de compra.
- Não será possível vender mais ações do que a quantidade disponível.
- O sistema deverá calcular lucro e prejuízo.
- O professor informou que o preço médio também deve ser atualizado na venda “da mesma forma do imposto de renda”. O método exato ainda precisa ser esclarecido com o professor.

### Ações internacionais e câmbio

- A cotação de uma ação internacional deverá ser exibida em dólar e também convertida para real.
- A compra será debitada diretamente do saldo em reais pelo valor convertido.
- Não haverá taxa de câmbio.
- A fonte de câmbio USD/BRL ainda não foi escolhida.
- Não é necessário registrar a cotação cambial no histórico da movimentação.

### Cotações e indisponibilidade

- Se a API de cotação estiver indisponível, deverá ser usada a última cotação armazenada.
- A operação continuará permitida mesmo com uma cotação antiga.
- Se a cotação armazenada tiver mais de uma semana, o sistema deverá mostrar um aviso de que ela não está atualizada.
- Ainda não foi definido se o usuário precisará confirmar expressamente uma operação feita com cotação desatualizada.

### Transferências

- A transferência ocorrerá entre corretoras cadastradas na conta do usuário.
- A transferência deverá preservar quantidade, preço e histórico do ativo.
- Ela deverá constar no histórico de movimentações, indicando corretora de origem e corretora de destino.
- Não haverá taxa, data de liquidação ou outras restrições por se tratar de uma simulação.
- Não será permitido transferir uma quantidade maior que a disponível na corretora de origem.

### Histórico

- O histórico não poderá ser alterado pelo usuário.
- Deverá registrar:
  - Saldo inicial;
  - Tipo de movimentação: compra, venda ou transferência;
  - Cotação da ação;
  - Quantidade;
  - Valor total;
  - Moeda;
  - Corretora;
  - Corretoras de origem e destino, no caso de transferência;
  - Data e hora;
  - Saldo restante.

### Dashboards

- Os dashboards deverão exibir:
  - Saldo;
  - Posição;
  - Preço médio;
  - Lucro e prejuízo;
  - Gráficos históricos;
  - Distribuição por ação;
  - Distribuição por corretora;
  - Distribuição por mercado;
  - Filtros por período.
- A visão inicial também prevê um dashboard geral de todas as corretoras e um dashboard específico da corretora selecionada.

### Limitações já declaradas

- Não haverá versão mobile nesta etapa.
- Não haverá vínculo real com contas bancárias.

## Pendências conhecidas

- Confirmar com o professor como o preço médio deve ser tratado após uma venda.
- Definir a fonte da cotação USD/BRL e seu comportamento em caso de indisponibilidade.
- Definir se operações com cotações antigas exigirão confirmação do usuário.
- Definir como preservar o histórico após a exclusão da conta sem comprometer os dados pessoais.
- Revisar o tamanho do escopo, especialmente dashboards, histórico completo, autenticação, recuperação por e-mail e múltiplas integrações externas.
- Consolidar os requisitos levantados em `docs/03-requisitos.md` somente depois da entrevista.

## Próximo passo

Continuar a quarta rodada da entrevista. As seguintes perguntas foram apresentadas e ainda aguardam resposta:

1. Ao excluir uma conta e manter o histórico, os dados pessoais do usuário devem permanecer associados a ele ou devem ser anonimizados? Uma conta excluída poderá ser reativada ou o mesmo CPF/e-mail poderá ser cadastrado novamente?
2. No cadastro de corretoras, a busca poderá começar por CNPJ ou nome. Se houver várias empresas com nomes semelhantes, o usuário deverá selecionar uma delas antes da validação? Quais dados da corretora deverão ser armazenados?
3. As operações aceitarão somente quantidades inteiras de ações ou também ações fracionárias? Existem valores mínimos ou limites de quantidade por operação?
4. A simulação poderá comprar e vender a qualquer hora e em qualquer dia, inclusive com o mercado fechado, usando a cotação disponível, ou deverá respeitar o horário das bolsas?
5. Compra e venda terão corretagem, impostos ou outras taxas? No cálculo de lucro/prejuízo, devem aparecer separadamente o resultado já realizado nas vendas e a valorização ainda não realizada das ações mantidas?

## Última atualização

- Data: 12 de agosto de 2026;
- Etapa: entrevista de requisitos, aguardando respostas da quarta rodada.
