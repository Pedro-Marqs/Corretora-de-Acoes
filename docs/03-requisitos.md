# Requisitos do projeto

## 1. Objetivo e origem

Este documento especifica a primeira versão da plataforma acadêmica de simulação de investimentos. Os requisitos foram derivados de `docs/01-visao.md` e `docs/02-pesquisa.md`. Decisões ainda não confirmadas aparecem somente na seção 9.

## 2. Atores do sistema

### AT01 — Visitante

Pessoa não autenticada que pode criar uma conta, entrar em uma conta ativa ou iniciar a reativação de uma conta inativa.

### AT02 — Investidor

Pessoa física autenticada que administra exclusivamente sua própria conta, saldo fictício, corretoras, posições, movimentações e dashboards. Este é o único perfil autenticado da primeira versão.

### AT03 — Agendador interno

Componente do backend que inicia atualizações periódicas de cotações e câmbio sem intervenção do investidor.

### AT04 — Serviços externos

Sistemas consultados pelo backend para obter dados de CNPJ, CEP, registro CTVM na CVM, ativos, cotações e câmbio. Não são usuários da interface e não alteram diretamente os dados financeiros.

## 3. Requisitos funcionais

### 3.1. Conta e autenticação

- **RF01:** O sistema deve permitir ao visitante criar uma conta informando nome, CPF, e-mail e senha.
- **RF02:** O sistema deve rejeitar o cadastro quando faltar qualquer campo de RF01, o CPF for inválido, o e-mail tiver formato inválido ou a senha não cumprir RN04.
- **RF03:** Ao concluir um cadastro válido, o sistema deve criar a conta com saldo de R$ 10.000,00 e registrar esse saldo inicial no histórico.
- **RF04:** O sistema deve permitir login por e-mail e senha somente em conta ativa.
- **RF05:** O sistema deve permitir ao investidor encerrar a sessão por logout.
- **RF06:** O sistema deve permitir ao investidor alterar o e-mail após confirmar a senha atual.
- **RF07:** O sistema deve permitir ao investidor alterar a senha após confirmar a senha atual.
- **RF08:** Após alteração de e-mail ou senha, o sistema deve invalidar todas as sessões pertencentes à conta, inclusive a sessão que realizou a alteração.
- **RF09:** O sistema deve exibir nome, CPF parcialmente ocultado e e-mail parcialmente ocultado na área da conta.
- **RF10:** O sistema deve permitir a exclusão lógica da conta somente após o investidor informar o e-mail atual, a senha atual e a palavra exata `Excluir`.
- **RF11:** O sistema deve impedir login em conta logicamente excluída pelo fluxo normal de autenticação.
- **RF12:** O sistema deve permitir reativar uma conta inativa, recuperando o saldo, as corretoras, as posições e o histórico preservados.
- **RF13:** Quando um CPF informado no cadastro pertencer a uma conta inativa, o sistema deve oferecer as alternativas de reativar essa conta ou criar uma nova conta.
- **RF14:** Se o visitante criar nova conta usando CPF de conta inativa, o sistema deve manter a conta anterior excluída e inacessível e iniciar a nova conta conforme RF03.

### 3.2. Saldo e aportes

- **RF15:** O sistema deve exibir ao investidor o saldo em reais disponível na conta.
- **RF16:** O sistema deve permitir ao investidor registrar aporte fictício de valor igual ou superior a R$ 10,00.
- **RF17:** Ao concluir um aporte, o sistema deve aumentar o saldo pelo valor informado e gravar uma movimentação de aporte com data/hora e saldo resultante.
- **RF18:** O sistema deve rejeitar aporte menor que R$ 10,00 sem alterar saldo ou histórico.

### 3.3. Corretoras

- **RF19:** O sistema deve permitir pesquisar uma corretora exclusivamente por CNPJ.
- **RF20:** Ao pesquisar um CNPJ, o backend deve consultar a situação cadastral, os dados de endereço e o registro da instituição na CVM.
- **RF21:** O resultado válido da pesquisa deve apresentar CNPJ, razão social, nome fantasia, situação cadastral, autorização e endereço estruturado.
- **RF22:** O sistema deve permitir cadastrar a corretora na conta somente quando o CNPJ estiver ativo e a instituição constar na CVM na categoria `CTVM`.
- **RF23:** O sistema deve rejeitar o cadastro de CNPJ que já esteja associado como corretora ativa à mesma conta.
- **RF24:** O sistema deve listar apenas as corretoras ativas da conta nas opções de compra, venda e transferência.
- **RF25:** O sistema deve permitir remover logicamente uma corretora somente quando ela não possuir posição com quantidade maior que zero.
- **RF26:** O sistema deve preservar o cadastro e as movimentações históricas de uma corretora removida.
- **RF27:** O sistema deve permitir cadastrar novamente uma corretora removida e manter associado seu histórico anterior.
- **RF28:** Ao consultar novamente uma corretora já conhecida, o sistema deve atualizar nome e endereço com os dados válidos retornados pela fonte externa, preservando os dados anteriores se a resposta vier incompleta.

### 3.4. Ativos e cotações

- **RF29:** O sistema deve permitir pesquisar ativos dos mercados brasileiro e norte-americano pelo ticker.
- **RF30:** O sistema deve aceitar um ativo somente se a integração retornar ticker, nome, mercado, moeda e cotação.
- **RF31:** O sistema deve rejeitar criptomoedas, moedas e ativos de mercados diferentes do brasileiro e do norte-americano.
- **RF32:** O resultado da pesquisa deve exibir ticker, nome, mercado, cotação, moeda e horário da cotação.
- **RF33:** Para ativo norte-americano, o sistema deve exibir a cotação em dólares e seu valor convertido em reais.
- **RF34:** O backend deve tentar obter cotação atual ao pesquisar um ativo brasileiro e ao confirmar sua compra ou venda; para ativo norte-americano, deve usar a cotação do ciclo diário ou a última armazenada.
- **RF35:** O agendador deve tentar atualizar, a cada cinco minutos, as cotações dos ativos brasileiros que possuam posições e, uma vez ao dia, as cotações dos ativos norte-americanos que possuam posições, enquanto o backend estiver em execução.
- **RF36:** Quando uma consulta de cotação falhar ou atingir o limite do provedor, o sistema deve usar a última cotação armazenada para o ativo, quando existente.
- **RF37:** Quando a última cotação de um ativo tiver mais de 24 horas, o sistema deve exibir aviso de desatualização e permitir a operação com esse valor.
- **RF38:** Quando não houver cotação atual nem armazenada para o ativo, o sistema deve bloquear a operação dependente da cotação.
- **RF39:** O sistema deve permitir vender pela última cotação armazenada um ativo em carteira que deixou de ser retornado pela API.
- **RF40:** O agendador deve tentar atualizar a cotação USD/BRL uma vez por dia por HTTP REST na AwesomeAPI e armazenar o último valor obtido.
- **RF41:** Se a atualização USD/BRL falhar, o sistema deve manter o último câmbio armazenado.
- **RF42:** Quando o câmbio armazenado tiver mais de sete dias, o sistema deve exibir aviso e continuar permitindo operações que utilizem esse câmbio.

### 3.5. Compra e venda

- **RF43:** O sistema deve permitir comprar uma quantidade inteira positiva de um ativo para uma corretora ativa da conta.
- **RF44:** Antes de concluir a compra, o sistema deve solicitar uma confirmação simples, sem apresentar uma tela separada de resumo.
- **RF45:** Ao concluir uma compra brasileira, o sistema deve debitar do saldo o produto da quantidade pela cotação em reais usada pelo backend.
- **RF46:** Ao concluir uma compra norte-americana, o sistema deve converter o valor total em dólares pelo câmbio USD/BRL usado pelo backend e debitar o resultado do saldo em reais.
- **RF47:** Ao concluir uma compra, o sistema deve criar ou atualizar a posição do ativo na corretora e gravar a movimentação no histórico.
- **RF48:** O sistema deve rejeitar compra cujo valor em reais seja superior ao saldo disponível, informando o valor da compra e o saldo disponível.
- **RF49:** O sistema deve permitir vender uma quantidade inteira positiva de uma posição pertencente à corretora selecionada.
- **RF50:** Antes de concluir a venda, o sistema deve solicitar uma confirmação simples, sem apresentar uma tela separada de resumo.
- **RF51:** Ao concluir uma venda, o sistema deve reduzir a quantidade da posição, acrescentar imediatamente ao saldo o valor em reais da venda e gravar a movimentação.
- **RF52:** O sistema deve rejeitar venda de quantidade superior à posição disponível na corretora, informando as quantidades solicitada e disponível.
- **RF53:** Quando uma venda zerar a posição, o sistema deve ocultá-la da carteira sem remover suas movimentações históricas.
- **RF54:** O sistema não deve oferecer edição, cancelamento ou estorno de compra ou venda concluída.

### 3.6. Transferências

- **RF55:** O sistema deve permitir transferir quantidade inteira positiva, total ou parcial, de uma posição entre duas corretoras ativas da mesma conta.
- **RF56:** O sistema deve rejeitar transferência quando origem e destino forem a mesma corretora ou quando a quantidade superar a posição disponível, informando a quantidade disponível.
- **RF57:** Antes de concluir a transferência, o sistema deve solicitar uma confirmação simples, sem apresentar uma tela separada de resumo.
- **RF58:** Ao concluir a transferência, o sistema deve reduzir a posição de origem, criar ou atualizar a posição de destino e manter o saldo inalterado.
- **RF59:** A transferência deve preservar o custo da quantidade transferida e registrar origem e destino no histórico.

### 3.7. Histórico e dashboards

- **RF60:** O sistema deve registrar no histórico somente saldo inicial, aportes, compras, vendas e transferências concluídos com sucesso.
- **RF61:** Cada registro histórico deve conter, quando aplicável ao tipo, ticker, cotação, quantidade, valor total, moeda, corretora, corretora de origem, corretora de destino, data/hora e saldo restante.
- **RF62:** O sistema deve impedir que o investidor edite ou exclua registros históricos.
- **RF63:** O sistema deve permitir filtrar o histórico por intervalo de datas, tipo de movimentação, ticker, corretora e mercado.
- **RF64:** O sistema deve apresentar o histórico paginado e, por padrão, do registro mais recente para o mais antigo.
- **RF65:** O dashboard geral deve apresentar saldo, patrimônio, posições, preço médio, lucro/prejuízo realizado, valorização não realizada e resultado total da conta.
- **RF66:** O sistema deve apresentar um dashboard restrito às posições e movimentações da corretora selecionada.
- **RF67:** O dashboard deve apresentar distribuições dos valores da carteira por ativo, por corretora e por mercado, informando também os valores numéricos de cada parcela.
- **RF68:** O dashboard deve apresentar gráfico da evolução histórica do patrimônio.
- **RF69:** O gráfico histórico deve oferecer os períodos de quatro semanas, três meses, seis meses, um ano, cinco anos e máximo.
- **RF70:** O período máximo deve abranger os dados existentes desde a criação da conta; nos demais períodos, se não houver dados desde o início solicitado, o gráfico deve começar no primeiro dado disponível.

## 4. Requisitos não funcionais

- **RNF01 — Plataforma:** A primeira versão deve executar localmente, com frontend React em JavaScript/Vite, backend Java 17/Spring Boot 3.4.0 e API REST em JSON.
- **RNF02 — Arquitetura:** O backend deve ser um monólito dividido em camadas de API, casos de uso, domínio e persistência; integrações externas devem ser acessadas por adapters substituíveis.
- **RNF03 — Persistência:** Dados da aplicação devem ser persistidos em PostgreSQL; H2 deve ser utilizado apenas em testes rápidos e não deve ser tratado como banco de produção.
- **RNF04 — Precisão:** Valores monetários, preços e resultados devem usar `BigDecimal` no backend e `NUMERIC/DECIMAL` no banco, sem `float` ou `double` nos cálculos financeiros.
- **RNF05 — Exibição monetária:** Todo valor monetário exibido deve ter exatamente duas casas decimais.
- **RNF06 — Tempo:** Datas e horas persistidas ou exibidas segundo regras de negócio devem ser interpretadas no fuso `America/Sao_Paulo`.
- **RNF07 — Atomicidade:** Aporte, compra, venda e transferência devem ser transacionais; uma falha em qualquer gravação deve deixar saldo, posições e histórico no estado anterior à tentativa.
- **RNF08 — Isolamento:** Toda consulta ou alteração autenticada deve derivar a conta da sessão e rejeitar acesso a registro pertencente a outro usuário, mesmo que seu identificador seja informado diretamente.
- **RNF09 — Senhas:** Senhas devem ser armazenadas somente como hash bcrypt e nunca devem ser registradas em logs ou retornadas pela API.
- **RNF10 — Sessão:** A autenticação deve usar sessão opaca persistida pelo Spring Session JDBC, transmitida em cookie `HttpOnly` e `SameSite`; `Secure` deve ser ativado em perfil com HTTPS e desativado apenas no perfil HTTP local.
- **RNF11 — Proteção web:** Requisições que alterem estado devem exigir proteção CSRF, e o CORS deve aceitar somente a origem configurada para o frontend local.
- **RNF12 — Privacidade:** CPF e e-mail devem aparecer parcialmente ocultados na interface e não devem ser escritos integralmente em logs de aplicação.
- **RNF13 — Segredos:** Chaves de APIs e credenciais do PostgreSQL devem vir de configuração externa ao código e não podem ser versionadas no Git.
- **RNF14 — Tratamento de erros:** Erros apresentados ao usuário devem conter mensagem explicativa em linguagem funcional e não devem expor stack trace, classe Java, consulta SQL ou credencial.
- **RNF15 — Responsividade:** As funções da interface devem permanecer utilizáveis sem rolagem horizontal da página em visualizações de desktop, tablet e celular usadas na verificação manual.
- **RNF16 — Atualização de mercado:** Enquanto backend e internet estiverem disponíveis, o intervalo entre atualizações automáticas deve ser de cinco minutos para ativos brasileiros e de um dia para ativos norte-americanos e USD/BRL.
- **RNF17 — Resiliência externa:** Timeout, resposta incompleta, HTTP `429` ou erro `5xx` de integração não deve apagar uma cotação ou dado cadastral válido já armazenado.
- **RNF18 — Testes:** As regras financeiras devem possuir testes unitários; autenticação, isolamento, rollback e persistência devem possuir testes de integração; os fluxos financeiros e constraints críticos devem ser testados contra PostgreSQL.
- **RNF19 — Documentação:** As APIs externas, suas credenciais, campos utilizados, limites conhecidos e comportamento de fallback devem ser documentados antes da apresentação.
- **RNF20 — API:** Os endpoints REST devem possuir documentação suficiente para identificar método HTTP, rota, autenticação, entrada, resposta de sucesso e respostas de erro.
- **RNF21 — Migrações:** O esquema PostgreSQL da entrega deve ser criado e atualizado por migrações versionadas com Flyway.
- **RNF22 — Dependências:** A aplicação não deve depender de Thymeleaf, Alpha Vantage, Redis, MySQL ou serviço de hospedagem na primeira versão.

## 5. Regras de negócio

- **RN01:** Cada conta ativa possui um único saldo em reais, compartilhado por todas as corretoras da conta.
- **RN02:** Uma nova conta sempre inicia com saldo de R$ 10.000,00.
- **RN03:** CPF e e-mail identificam credenciais de acesso e devem ser únicos entre contas ativas.
- **RN04:** A senha deve conter no mínimo oito caracteres, incluindo ao menos uma letra minúscula, uma maiúscula, um número e um caractere especial.
- **RN05:** Nome e CPF não podem ser alterados após o cadastro.
- **RN06:** Aporte mínimo é R$ 10,00 e não possui limite máximo definido.
- **RN07:** Aporte aumenta saldo e patrimônio, mas não integra lucro, prejuízo ou valorização.
- **RN08:** Uma corretora só é válida quando o CNPJ está ativo e a CVM a classifica como `CTVM`.
- **RN09:** Uma conta não pode possuir duas associações ativas com o mesmo CNPJ de corretora.
- **RN10:** Uma corretora com qualquer posição maior que zero não pode ser removida.
- **RN11:** As posições são independentes por combinação de conta, corretora e ativo.
- **RN12:** Quantidade de ativo em compra, venda ou transferência deve ser um número inteiro maior que zero.
- **RN13:** Compra, venda e transferência podem ocorrer em qualquer dia e horário e são concluídas imediatamente após a confirmação.
- **RN14:** Não existem corretagem, impostos, taxas cambiais ou taxa de transferência na primeira versão.
- **RN15:** O preço de uma operação é definido pelo backend a partir da cotação atual ou do fallback permitido; o preço enviado pelo frontend não determina o valor financeiro.
- **RN16:** Uma compra não pode produzir saldo negativo.
- **RN17:** Uma venda ou transferência não pode usar quantidade superior à posição na corretora de origem.
- **RN18:** O preço médio após compra é a média ponderada entre o custo da posição existente e o custo da nova compra.
- **RN19:** Uma venda parcial mantém o preço médio unitário da posição restante e reduz quantidade e custo total proporcionalmente.
- **RN20:** Lucro ou prejuízo realizado de uma venda é `(preço de venda − preço médio antes da venda) × quantidade vendida`.
- **RN21:** Se uma posição zerada for comprada novamente, o cálculo do preço médio começa sem aproveitar o custo da posição encerrada.
- **RN22:** Na transferência, o custo unitário da parcela transferida é o preço médio da posição de origem.
- **RN23:** Se o destino já possuir o ativo, seu preço médio é recalculado pela média ponderada do custo existente com o custo transferido.
- **RN24:** Patrimônio é composto pelo saldo em reais mais o valor convertido em reais das posições nas últimas cotações utilizáveis.
- **RN25:** Valorização não realizada de uma posição é `(cotação atual utilizável − preço médio) × quantidade`, convertida em reais quando o ativo estiver em dólar.
- **RN26:** Resultado total deve considerar lucro/prejuízo realizado e valorização não realizada, sem classificar aportes como rendimento.
- **RN27:** Ativo norte-americano é comprado e vendido em reais pela conversão direta entre sua cotação em USD e o USD/BRL, sem taxa de câmbio.
- **RN28:** Valores externos com mais de duas casas decimais devem ser arredondados para duas casas pelo modo `HALF_UP`: terceira casa de 0 a 4 arredonda para baixo e de 5 a 9 arredonda para cima.
- **RN29:** Somente movimentações concluídas com sucesso integram o histórico.
- **RN30:** Exclusões de conta e corretora são lógicas e não removem seus dados históricos do banco.

## 6. Histórias de usuário

- **HU01:** Como visitante, quero criar uma conta com meus dados para iniciar uma simulação com R$ 10.000,00.
- **HU02:** Como investidor, quero entrar e sair da minha conta para controlar o acesso aos meus dados.
- **HU03:** Como investidor, quero alterar meu e-mail ou senha para manter minhas credenciais atualizadas.
- **HU04:** Como investidor, quero excluir logicamente e reativar minha conta para interromper ou retomar o uso sem perder o histórico.
- **HU05:** Como investidor, quero fazer aportes fictícios para aumentar o saldo disponível sem que isso seja tratado como lucro.
- **HU06:** Como investidor, quero pesquisar uma instituição pelo CNPJ e cadastrar somente uma CTVM válida para organizar posições por corretora.
- **HU07:** Como investidor, quero pesquisar ativos brasileiros e norte-americanos para consultar dados e cotações antes de operar.
- **HU08:** Como investidor, quero comprar ativos com meu saldo fictício para formar posições associadas a uma corretora.
- **HU09:** Como investidor, quero vender parte ou toda uma posição para realizar resultado e recuperar saldo.
- **HU10:** Como investidor, quero transferir uma posição entre minhas corretoras sem alterar o saldo ou perder seu custo histórico.
- **HU11:** Como investidor, quero consultar um histórico imutável e filtrável para auditar as movimentações concluídas.
- **HU12:** Como investidor, quero visualizar a carteira consolidada e por corretora para acompanhar saldo, patrimônio, posição e resultados.
- **HU13:** Como investidor, quero ver gráficos por período e distribuições da carteira para analisar sua evolução e composição.
- **HU14:** Como investidor, quero saber quando cotação ou câmbio estão desatualizados para interpretar corretamente uma simulação feita com cache.

## 7. Casos de erro

| ID | Situação | Resposta esperada |
|---|---|---|
| CE01 | Cadastro com CPF inválido | Rejeitar o cadastro e indicar o campo inválido. |
| CE02 | Cadastro ou alteração com e-mail inválido | Rejeitar a solicitação e indicar o campo inválido. |
| CE03 | Senha fora de RN04 | Rejeitar e informar cada regra de composição não atendida. |
| CE04 | Login com credenciais inválidas ou conta inativa | Não criar sessão e apresentar mensagem explicativa sem revelar se e-mail ou senha foi o elemento incorreto. |
| CE05 | Alteração/exclusão com senha atual incorreta | Rejeitar sem alterar a conta. |
| CE06 | Exclusão sem e-mail atual ou palavra `Excluir` exata | Rejeitar sem inativar a conta. |
| CE07 | Aporte inferior a R$ 10,00 | Rejeitar sem alterar saldo ou histórico. |
| CE08 | CNPJ inválido, inativo, ausente na CVM ou não classificado como CTVM | Impedir o cadastro e informar o motivo identificável. |
| CE09 | Corretora duplicada | Impedir nova associação sem alterar a já existente. |
| CE10 | Remoção de corretora com posição | Rejeitar e manter a corretora ativa. |
| CE11 | Ativo sem algum campo obrigatório | Bloquear sua utilização e preservar dados válidos já armazenados. |
| CE12 | Ativo de mercado não aceito | Rejeitar pesquisa operacional/compra e informar que o mercado não é suportado. |
| CE13 | Quantidade nula, negativa, decimal ou não numérica | Rejeitar a movimentação antes de alterar dados. |
| CE14 | Compra acima do saldo | Rejeitar e informar valor da compra e saldo disponível. |
| CE15 | Venda acima da posição | Rejeitar e informar quantidade solicitada e disponível. |
| CE16 | Transferência acima da posição ou para a própria corretora | Rejeitar sem alterar posições, saldo ou histórico. |
| CE17 | API de cotação indisponível com cache existente | Usar o cache, mostrar data/hora e aplicar aviso quando ultrapassar 24 horas. |
| CE18 | API de cotação indisponível sem cache | Bloquear a operação e explicar que não existe cotação utilizável. |
| CE19 | Câmbio indisponível com cache existente | Usar o último câmbio e avisar quando tiver mais de sete dias. |
| CE20 | Falha durante gravação de movimentação | Reverter todas as alterações da tentativa e não criar registro histórico de sucesso. |
| CE21 | Usuário tenta acessar identificador de outra conta | Negar acesso sem retornar os dados do registro. |
| CE22 | Serviço externo retorna resposta incompleta | Não substituir dados válidos armazenados por valores ausentes. |

## 8. Dependências e restrições

### 8.1. Dependências externas

- Brasil API para consulta de CNPJ;
- ViaCEP para dados de endereço;
- fonte oficial da CVM para validação da categoria CTVM;
- Brapi para ativos e cotações do mercado brasileiro;
- Twelve Data para ativos e cotações norte-americanos, sujeita à prova técnica de cobertura e campos;
- AwesomeAPI por HTTP REST para a cotação USD/BRL;
- internet e chaves válidas para as integrações que exigirem autenticação.

### 8.2. Restrições técnicas e acadêmicas

- Backend Java com Spring Boot, API REST JSON e arquitetura em camadas;
- frontend React responsivo;
- PostgreSQL como banco principal e H2 somente em testes rápidos;
- integração com pelo menos três serviços externos reais;
- documentação dos endpoints e das limitações das APIs;
- testes e demonstração prática;
- execução somente local na primeira versão;
- ausência de envio de e-mail, recuperação de senha, hospedagem, negociação real e exportação Excel;
- ausência de MySQL, Redis, Thymeleaf e Alpha Vantage na solução da primeira versão.

## 9. Dúvidas ainda não resolvidas

Não há dúvidas funcionais pendentes. Antes das integrações completas, ainda devem ser validados tecnicamente:

- os campos e a cobertura de ativos norte-americanos disponíveis na Twelve Data;
- a fonte e o formato de dados abertos da CVM que permitam validar a categoria CTVM sem automação de páginas ou CAPTCHA.

## 10. Critério de qualidade desta especificação

Cada requisito acima descreve uma entrada, ação, estado ou resultado observável que pode ser verificado por teste automatizado ou procedimento de aceitação. Nenhuma dúvida da seção 9 deve ser tratada como decisão definitiva sem atualização explícita deste documento e da visão.
