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

Já foram realizadas dezesseis rodadas completas de perguntas. A décima sétima rodada deverá esclarecer as pendências restantes.

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
- O CPF deverá ser validado quanto ao formato e aos dígitos verificadores antes do cadastro.
- O e-mail deverá ter seu formato validado no cadastro, embora não seja necessária a confirmação do endereço.
- O login será realizado por e-mail e senha.
- Não será necessário confirmar o e-mail durante a criação da conta.
- A senha deverá possuir no mínimo oito caracteres, incluindo ao menos uma letra minúscula, uma letra maiúscula, um número e um caractere especial.
- O usuário poderá alterar apenas o e-mail e a senha, além de excluir sua conta na área de configurações. Nome e CPF não poderão ser alterados.
- Ao alterar o e-mail, o sistema deverá validar seu formato e verificar se ele já está vinculado a outra conta ativa.
- Para alterar o e-mail ou a senha, o usuário deverá informar a senha atual antes de confirmar a alteração.
- A recuperação de senha será simples: ao selecionar “Esqueci minha senha”, o usuário receberá um e-mail para definir uma nova senha.
- O link de recuperação de senha não terá prazo de expiração.
- Não haverá bloqueio temporário da conta após tentativas incorretas de login.
- Após a alteração do e-mail ou da senha, todas as sessões abertas da conta serão encerradas e será necessário realizar novo login.
- Para excluir a conta, o usuário deverá confirmar o e-mail e a senha e escrever a palavra `Excluir`.
- A exclusão será lógica: todos os dados serão mantidos no banco de dados e a conta será marcada como inativa.
- Uma conta inativa poderá ser reativada.
- Durante a criação de uma conta, se o CPF estiver vinculado a uma conta inativa, o sistema deverá oferecer duas opções: reativar a conta anterior ou criar uma nova conta.
- Se o usuário escolher reativar, o sistema enviará um e-mail para recuperação de senha e restaurará o acesso à conta com seus dados, saldo, carteira e histórico anteriores.
- Se o usuário não quiser reativar, poderá criar uma nova conta normalmente, mesmo com o CPF vinculado à conta inativa.
- O mesmo e-mail poderá ser reutilizado na nova conta.
- Quando uma nova conta for criada no lugar da reativação, o estado da conta anterior mudará de inativa para excluída.
- A nova conta começará com saldo próprio de R$ 10.000,00 e não dará acesso aos dados da conta anterior.
- Uma conta excluída não poderá ser acessada nem reativada, mas seus dados permanecerão armazenados no banco para preservação do histórico.
- Não poderá haver mais de uma conta ativa por e-mail.
- O e-mail de uma conta excluída poderá ser reutilizado em uma nova conta, desde que não esteja vinculado a outra conta ativa.
- O sistema deverá oferecer logout para encerrar a sessão do usuário.
- A sessão permanecerá ativa até que o usuário realize logout; não haverá expiração automática por inatividade.

### Saldo e custódia

- Cada conta começará com saldo de R$ 10.000,00.
- O saldo em dinheiro pertence à conta do usuário, e não a uma corretora.
- Todas as corretoras cadastradas pelo usuário utilizam o mesmo saldo disponível.
- As ações ficam vinculadas diretamente à corretora em que foram compradas ou para a qual foram transferidas.
- O saldo e a posição da carteira devem variar a cada movimentação.
- O usuário poderá adicionar saldo à conta informando um valor, que será somado diretamente ao saldo total.
- A adição de saldo terá valor mínimo de R$ 10,00 e não terá valor máximo.
- Cada adição de saldo será registrada no histórico com data, hora, valor adicionado e saldo resultante.
- A adição de saldo será registrada com o tipo de movimentação `APORTE`.
- O aporte aumenta o saldo e o patrimônio da conta, mas não será contabilizado como lucro nem como valorização.
- Valores adicionados poderão conter centavos, limitados a duas casas decimais.
- Valores negativos ou com mais de duas casas decimais deverão ser rejeitados; valores inferiores a R$ 10,00 também serão rejeitados pela regra de valor mínimo.

### Compra e venda

- Compras e vendas serão simulações instantâneas a preço de mercado.
- Não haverá criação ou gerenciamento de ordens de compra.
- Não será possível vender mais ações do que a quantidade disponível.
- As operações aceitarão somente quantidades inteiras de ações.
- O valor mínimo de uma compra será o valor de uma ação, e a quantidade máxima ficará limitada pelo saldo disponível da conta.
- Compras e vendas poderão ser realizadas a qualquer dia e horário, inclusive com o mercado fechado, por se tratar de uma simulação.
- Não haverá corretagem, impostos ou outras taxas nas operações.
- O sistema deverá calcular lucro e prejuízo.
- O sistema deverá apresentar um campo de valorização ou lucro.
- O professor informou que o preço médio também deve ser atualizado na venda “da mesma forma do imposto de renda”. O método exato ainda precisa ser esclarecido com o professor.

### Corretoras

- A busca de corretoras poderá ser feita por CNPJ ou nome.
- A busca deverá retornar todas as corretoras compatíveis com o termo informado.
- O usuário selecionará a corretora desejada entre os resultados, podendo distingui-las pelo CNPJ quando houver nomes semelhantes.
- Deverão ser armazenados CNPJ, razão social, nome fantasia, situação cadastral, endereço/CEP e a informação de autorização no mercado financeiro.
- O endereço deverá ser armazenado em campos separados: CEP, logradouro, número, complemento, bairro, cidade e UF.
- E-mail e telefone da corretora não serão armazenados, mesmo quando estiverem disponíveis na API.
- Se houver divergência entre o endereço retornado pela Brasil API e pelo ViaCEP, prevalecerão os dados da Brasil API.
- Uma instituição somente poderá ser cadastrada como corretora se estiver autorizada ou credenciada para atuar no mercado financeiro; não será permitido cadastrar qualquer empresa apenas por possuir um CNPJ válido.
- O cadastro também será impedido se a situação cadastral do CNPJ não estiver ativa, mesmo que a instituição conste como autorizada pela CVM.
- Se as APIs necessárias para validar CNPJ, endereço ou autorização no mercado financeiro estiverem indisponíveis e ainda não houver dados válidos armazenados, o cadastro da corretora será bloqueado até que todas as validações possam ser concluídas.
- Uma corretora somente poderá ser removida da conta se não possuir ações vinculadas.
- Se houver ações vinculadas, o sistema deverá orientar o usuário a vendê-las ou transferi-las antes da remoção.
- Ao ser removida, a corretora deixará de aparecer entre as corretoras ativas, mas seus dados e suas movimentações anteriores continuarão preservados e visíveis no histórico.
- Uma corretora removida poderá ser cadastrada novamente. Nesse caso, voltará a constar como ativa e seu histórico anterior continuará preservado.
- O mesmo CNPJ de corretora não poderá ser cadastrado mais de uma vez na mesma conta.
- Nos resultados da pesquisa, uma corretora já cadastrada deverá aparecer esmaecida e identificada pelo texto `Já cadastrada`, sem permitir novo cadastro.

### Cadastro de ativos e posições

- O ativo será cadastrado automaticamente quando o usuário informar o ticker e o mercado, desde que o ticker seja validado na API correspondente.
- A identidade única do ativo será composta por ticker e mercado.
- O ticker será digitado pelo usuário, enquanto o mercado será selecionado em uma combobox.
- A combobox de mercado terá as opções `Brasil/B3` e `Estados Unidos`. Essa divisão atende ao enunciado, que exige ações brasileiras e americanas e a identificação do mercado, sem exigir a separação entre bolsas norte-americanas.
- Se o ativo ainda não possuir cotação armazenada e a API estiver indisponível, sua compra será bloqueada.
- Se o mesmo ticker já estiver em uma corretora e o usuário quiser mantê-lo em outra, deverá realizar uma nova compra pela outra corretora.
- As posições do mesmo ativo em corretoras diferentes serão independentes, sem vínculo entre si, ressalvadas as transferências solicitadas pelo usuário.
- Quando a quantidade de uma posição chegar a zero por venda ou transferência, ela deixará de aparecer na carteira e permanecerá apenas no histórico.
- Se uma posição zerada for comprada novamente na mesma corretora, seu preço médio começará do zero e considerará somente a nova compra e as operações posteriores.

### Ações internacionais e câmbio

- A cotação de uma ação internacional deverá ser exibida em dólar e também convertida para real.
- A compra será debitada diretamente do saldo em reais pelo valor convertido.
- Não haverá taxa de câmbio.
- Poderá ser utilizada qualquer fonte adequada para a cotação USD/BRL; a fonte específica ainda será escolhida.
- Se a fonte de câmbio estiver indisponível, deverá ser utilizada a última cotação USD/BRL armazenada.
- A cotação USD/BRL deverá ser atualizada diariamente.
- Se a cotação cambial armazenada tiver mais de uma semana, o sistema deverá exibir um aviso de desatualização.
- Se a atualização diária do USD/BRL falhar, a próxima tentativa automática ocorrerá somente no dia seguinte, mantendo-se a última cotação armazenada.
- Não é necessário registrar a cotação cambial no histórico da movimentação.

### Cotações e indisponibilidade

- Se a API de cotação estiver indisponível, deverá ser usada a última cotação armazenada.
- A operação continuará permitida mesmo com uma cotação antiga.
- Se a cotação armazenada tiver mais de um dia, o sistema deverá mostrar um aviso de que ela não está atualizada.
- Uma cotação com mais de um dia exigirá somente a exibição do aviso; não será necessária confirmação adicional para realizar a operação.
- As cotações deverão ser atualizadas automaticamente a cada cinco minutos.
- A atualização automática a cada cinco minutos abrangerá somente os ativos que estejam presentes nas carteiras.
- Nas buscas de ativos, o sistema sempre deverá tentar obter a cotação atualizada, mesmo que o ativo não esteja em uma carteira.
- Se a tentativa de atualização durante uma busca falhar e já existir uma cotação armazenada, o ativo poderá ser exibido com essa cotação e com o aviso de desatualização aplicável.

### Integrações externas

- A Brasil API será utilizada para consultar CNPJ e dados cadastrais das empresas.
- O ViaCEP será utilizado para consultar e validar endereços por CEP.
- Uma fonte pública da CVM será utilizada separadamente para validar se a instituição está autorizada a atuar no mercado financeiro.
- A Brapi será utilizada para consultar cotações de ações brasileiras.
- Para ações norte-americanas, será escolhida uma API entre Alpha Vantage e Twelve Data após a comparação de limites e disponibilidade.
- As integrações e suas limitações de autenticação, quantidade de requisições e disponibilidade deverão ser documentadas.
- Se uma atualização automática falhar, o sistema continuará usando a última cotação armazenada e realizará uma nova tentativa no ciclo seguinte, após cinco minutos.

### Transferências

- A transferência ocorrerá entre corretoras cadastradas na conta do usuário.
- A transferência deverá preservar quantidade, preço e histórico do ativo.
- Ela deverá constar no histórico de movimentações, indicando corretora de origem e corretora de destino.
- Não haverá taxa, data de liquidação ou outras restrições por se tratar de uma simulação.
- Não será permitido transferir uma quantidade maior que a disponível na corretora de origem.
- Será permitido transferir apenas parte da quantidade disponível de um ativo.
- A posição que receber as ações deverá atualizar seu preço médio considerando o preço médio da posição de origem.
- Se a corretora de destino já possuir o mesmo ativo, o preço médio resultante será calculado pela média ponderada entre a posição existente no destino e as ações transferidas, valoradas pelo preço médio da origem.
- Quando todas as ações forem transferidas, a posição desaparecerá imediatamente da carteira da corretora de origem, permanecerá na corretora de destino e continuará registrada no histórico.

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
- Deverá oferecer filtros por período, tipo de movimentação, ação, corretora e mercado.
- O histórico e as listagens de corretoras e ações serão paginados.
- O tamanho inicial será de 20 registros por página, e o usuário poderá escolher entre 10, 20, 50 ou 100 registros por página.
- A ordenação padrão será pelos registros mais recentes.
- O usuário também poderá escolher outras ordenações pertinentes, incluindo corretoras por nome e ações por ticker.
- As opções adicionais de ordenação incluirão data crescente ou decrescente, nome da corretora, ticker, tipo de movimentação e valor da operação.
- Os pontos do gráfico histórico de patrimônio serão registrados somente quando ocorrer uma movimentação, e não a cada atualização periódica de cotação.
- Um aporte criará um novo ponto no gráfico, elevando o patrimônio, mas continuará excluído dos indicadores de lucro e valorização.
- Em cada ponto histórico, o patrimônio será calculado usando o saldo disponível e as cotações mais recentes armazenadas de todas as ações da carteira naquele momento.

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
- O resultado dos investimentos deverá apresentar separadamente a valorização não realizada das ações mantidas, o lucro ou prejuízo realizado nas vendas e o resultado total que reúne ambos.
- Os gráficos históricos deverão permitir acompanhar a evolução do patrimônio total, calculado como saldo disponível mais o valor das ações, ao longo do período selecionado.
- A visão inicial também prevê um dashboard geral de todas as corretoras e um dashboard específico da corretora selecionada.

### Limitações já declaradas

- Não haverá versão mobile nesta etapa.
- Não haverá vínculo real com contas bancárias.

## Pendências conhecidas

- Confirmar com o professor como o preço médio deve ser tratado após uma venda.
- Escolher a fonte específica da cotação USD/BRL.
- Escolher entre Alpha Vantage e Twelve Data para as ações norte-americanas.
- Escolher a fonte específica da cotação USD/BRL, que poderá coincidir com uma das APIs de cotação adotadas.
- Revisar o tamanho do escopo, especialmente dashboards, histórico completo, autenticação, recuperação por e-mail e múltiplas integrações externas.
- Consolidar os requisitos levantados em `docs/03-requisitos.md` somente depois da entrevista.

## Próximo passo

Continuar com a décima sétima rodada da entrevista, detalhando confirmações das movimentações, precisão monetária, data e hora e mensagens de falha.

## Última atualização

- Data: 12 de agosto de 2026;
- Etapa: décima sexta rodada da entrevista concluída; preparar a décima sétima rodada.
