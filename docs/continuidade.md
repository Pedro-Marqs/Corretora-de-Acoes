# Continuidade do projeto

## Finalidade

Este arquivo registra o ponto atual do trabalho para permitir sua continuação em outra máquina. Os documentos de referência são, nesta ordem:

1. `docs/01-visao.md` — escopo e decisões de produto confirmadas;
2. `docs/02-pesquisa.md` — pesquisa e recomendações técnicas;
3. enunciado acadêmico — prevalece quando houver requisito obrigatório.

Em caso de divergência, a decisão mais recente registrada nesta seção deve ser usada e o documento divergente deve ser corrigido antes de avançar.

Este arquivo deve ser atualizado sempre que houver uma decisão relevante, alteração de escopo, conclusão de etapa, criação de documento ou mudança no próximo passo do projeto.

## Estado atual

- A entrevista de visão foi encerrada.
- A visão consolidada está em `docs/01-visao.md`.
- A pesquisa técnica está em `docs/02-pesquisa.md`.
- A pesquisa foi revisada uma última vez e alinhada às decisões mais recentes.
- A T01 foi concluída: a base própria do backend Spring Boot e do frontend React/Vite foi criada, sem copiar ou incorporar código do repositório de referência.
- Os requisitos foram consolidados em `docs/03-requisitos.md`, e as cinco dúvidas levantadas nessa etapa foram resolvidas.
- Dez especificações funcionais foram criadas em `docs/spec/`, uma por conjunto de funcionalidades.
- O único tipo de usuário é o `Investidor`; os documentos distinguem somente seu estado autenticado ou não autenticado quando o fluxo exigir.
- A arquitetura foi proposta em `docs/04-arquitetura.md`, e sua estrutura inicial foi materializada pela T01.
- O projeto foi dividido em 41 tarefas pequenas e ordenadas em `docs/05-tarefas.md`, incluindo provas técnicas, backend, frontend, testes, documentação e demonstração.
- A T01 passou em build limpo, testes iniciais, análise estática e auditoria de dependências, e recebeu revisão final do Atlas sem achados restantes.
- A T02 foi implementada: perfis `local`/`dev` com PostgreSQL e `test` com H2, configurações externas por ambiente, Compose local e instruções de inicialização.
- A validação automatizada da T02 passou com cinco testes e build limpo; a conexão com uma instância PostgreSQL real ainda depende de Docker ou PostgreSQL disponível na máquina.
- A T03 foi implementada e aprovada tecnicamente pelo Revisor: migrações Flyway, nove entidades JPA, repositories, tabelas Spring Session e constraints do esquema foram criados.
- Na T03, 14 testes executáveis passaram; os dois testes PostgreSQL/Testcontainers foram ignorados por indisponibilidade de Docker, mantendo pendente a validação integral no banco principal.
- A execução local foi validada no PostgreSQL 9.4.26 instalado na máquina: a aplicação conectou, o Flyway aplicou as migrações V1 e V2, o Hibernate validou o modelo JPA e o backend iniciou com sucesso. Foram criadas 12 tabelas no esquema.
- O PostgreSQL 9.4 pode ser usado no desenvolvimento local, mas Flyway e Hibernate emitiram avisos de versão sem suporte oficial.
- Docker não será utilizado em nenhuma etapa do projeto. O banco de desenvolvimento e das validações de integração será sempre o PostgreSQL instalado localmente e administrado pelo pgAdmin.
- Os dois testes atuais baseados em Testcontainers deverão ser substituídos, em tarefa própria, por testes de integração opt-in contra um banco PostgreSQL local exclusivo para testes. Esses testes nunca poderão apagar, recriar ou limpar automaticamente o banco principal `gestao_acoes`.
- A T04 foi concluída: `FinancialAmount` centraliza valores com `BigDecimal`, escala de duas casas e `HALF_UP`, incluindo soma, multiplicação e conversão USD/BRL; `TimeConfiguration` fornece um `Clock` injetável no fuso `America/Sao_Paulo`.
- A validação final da T04 passou em `mvnw clean verify`: 34 testes sem falhas, sendo 32 executados e os mesmos dois testes Testcontainers ignorados por Docker indisponível. O build e a análise do compilador com `-Xlint:all -Werror` também passaram. O agente Revisor não encontrou problemas funcionais; as lacunas de cobertura apontadas por ele foram implementadas pelo agente Programador e revalidadas pelo Orquestrador.
- A T05 foi concluída: a API possui contrato uniforme de erro com `errorId`, `code`, `message`, `fieldErrors` e `timestamp`, manipulador global e as categorias validação, autenticação, autorização, conflito, regra de negócio, dependência externa e erro interno.
- Exceções funcionais não aceitam mensagens públicas arbitrárias. Mensagens específicas usam entradas tipadas e controladas, como saldo solicitado/disponível com `FinancialAmount` e conflito de corretora duplicada. JSON malformado ou incompatível retorna validação HTTP 400.
- Respostas e logs não expõem stack trace, SQL, classes internas, CPF, e-mail, senha, cookie, autorização ou chaves. Cada resposta recebe um UUID `errorId`; ocorrências registradas usam o mesmo identificador no log, junto somente do código e da classe técnica controlada.
- A T05 foi aprovada pelo agente Revisor após três ciclos. A validação final passou com 45 testes backend sem falhas (43 executados e dois Testcontainers ignorados por Docker indisponível), compilação `-Xlint:all -Werror`, JAR, teste frontend, ESLint e build Vite. O `npm audit` executado pelo `npm ci` não encontrou vulnerabilidades.
- A T06 foi concluída e aprovada pelo agente Revisor: a API usa Spring Security com sessão opaca persistida pelo Spring Session JDBC, CSRF, CORS restrito à origem configurada, bcrypt e respostas 401/403 no contrato uniforme da T05.
- As rotas públicas foram limitadas, por método e caminho, a cadastro, login, reativação e obtenção do token CSRF. Todas as demais rotas exigem autenticação. A T06 não implementou os fluxos funcionais dessas rotas.
- O cookie `SESSION` é `HttpOnly`, `SameSite=Lax` e seguro por padrão; somente os perfis locais HTTP e de teste desativam `Secure`. O cookie técnico `XSRF-TOKEN` é legível pelo React, usa `SameSite=Lax` e segue a mesma política de `Secure`.
- A criação automática do usuário e da senha temporários do Spring Boot foi desativada sem remover a cadeia de segurança nem o codificador bcrypt. A autenticação real será implementada nas tarefas posteriores.
- Foi criado, sem apagar ou modificar o banco principal, o banco local isolado `gestao_acoes_test`. O teste opt-in confirmou no PostgreSQL local a gravação e a recuperação de uma sessão JDBC.
- A validação final da T06 passou com 60 testes backend, sem falhas ou erros: 58 foram executados, incluindo o teste no PostgreSQL local, e dois testes Testcontainers antigos permaneceram ignorados. Compilação com `-Xlint:all -Werror`, empacotamento JAR, teste frontend, ESLint e build Vite também passaram.
- A proteção contra fixação de sessão será comprovada na T08, quando existir o fluxo real de login; não foi criado um login artificial apenas para testá-la na T06.
- A T07 foi concluída e aprovada pelo agente Revisor: `POST /api/accounts` cria uma conta `ACTIVE`, com saldo de R$ 10.000,00, movimentação `INITIAL_BALANCE` e primeiro ponto patrimonial na mesma transação.
- O cadastro valida os campos obrigatórios, CPF nos formatos de 11 dígitos ou `ddd.ddd.ddd-dd`, e-mail e a composição confirmada da senha. CPF e e-mail são normalizados, e a unicidade é aplicada somente entre contas ativas.
- O cadastro retorna HTTP 201 com identificador, nome, saldo e estado. CPF, e-mail, senha e hash não são retornados; o cadastro não autentica automaticamente e continua exigindo CSRF.
- Para aceitar senhas que cumpram RN04 mesmo acima do limite de entrada do bcrypt, o `PasswordEncoder` aplica SHA-256 em UTF-8 antes do bcrypt. Somente o hash bcrypt salgado é persistido. O futuro login deve usar o mesmo encoder.
- A validação final da T07 passou com 70 testes backend, sem falhas ou erros. Os testes no PostgreSQL local `gestao_acoes_test` comprovaram o cadastro completo, os três registros correlatos e a constraint de CPF ativo, com rollback automático e sem dados residuais. Dois testes Testcontainers antigos permaneceram ignorados; Docker não foi usado.
- Teste frontend, ESLint e build Vite também passaram no fechamento da T07.
- A pedido do usuário, foi criada antecipadamente uma interface mínima para testar o cadastro da T07, sem implementar a fundação completa da T11 nem os demais fluxos da T12. A tela não inclui login, área da conta, rotas privadas ou dashboard.
- A tela pública obtém o token em `GET /api/csrf`, envia `POST /api/accounts` com cookie e cabeçalho CSRF, bloqueia reenvios, mostra erros gerais e por campo e confirma o saldo inicial sem exibir CPF, e-mail, senha ou identificador técnico.
- O frontend de cadastro é responsivo, usa o breakpoint de 860px para evitar rolagem horizontal, executa em `http://localhost:5173` com porta estrita e aceita `VITE_API_BASE_URL` configurada em `src/main/front/.env` a partir do exemplo local.
- A validação da interface passou com nove testes, ESLint sem avisos e build Vite. O agente Revisor aprovou a entrega após a correção do breakpoint responsivo.
- Após um cadastro concluído, o frontend agora troca o formulário por uma tela inicial transitória da conta, exibindo somente o nome, o saldo devolvido pela API e o estado `Ativa`. A tela não persiste dados e volta ao cadastro se a página for recarregada.
- Essa tela inicial ainda não representa uma sessão autenticada: login, dados privados, carteira, corretoras, ativos e histórico continuam aguardando suas tarefas próprias. O aviso na interface informa explicitamente essa limitação.
- A tela inicial pós-cadastro foi aprovada pelo agente Revisor após ajuste específico para 320px. A validação passou com 11 testes frontend, ESLint e build Vite.
- Na execução local desta máquina, o Maven pode resolver incorretamente o repositório para `C:\.m2\repository` e falhar por permissão. Quando isso ocorrer, iniciar o backend com `-Dmaven.repo.local=C:\Users\Arklok\.m2\repository`. Esse parâmetro corrige somente o caminho do cache e não altera a aplicação.
- A T08 foi concluída e aprovada pelo agente Revisor: `POST /api/auth/login` autentica por e-mail e senha somente contas ativas, e `POST /api/auth/logout` encerra apenas a sessão atual. Ambos retornam HTTP 204 e exigem CSRF.
- Senha incorreta, e-mail inexistente e contas `INACTIVE` ou `DELETED` retornam o mesmo erro neutro HTTP 401. A busca ausente também executa uma comparação com hash dummy para reduzir diferenças observáveis de processamento.
- A sessão armazena um `AccountPrincipal` serializável contendo somente o UUID da conta; nome, CPF, e-mail, saldo, senha, hash e entidade JPA não são serializados no contexto de segurança.
- O login troca o identificador de uma sessão preexistente para impedir fixação e persiste explicitamente o contexto no Spring Session JDBC. O logout invalida o cookie e a linha da sessão atual, preservando outras sessões válidas da mesma conta.
- A validação final da T08 passou com 77 testes backend, sem falhas ou erros, incluindo autenticação e sessão no PostgreSQL local `gestao_acoes_test`; dois testes Testcontainers antigos permaneceram ignorados. O frontend existente também passou em 11 testes, ESLint e build Vite.
- A T08 não criou interface de login nem consulta da conta. Esses comportamentos continuam nas tarefas de frontend e na T09, respectivamente.
- A pedido do usuário, foi adicionada antecipadamente a interface mínima de login e logout da T08. O frontend alterna entre cadastro e login, usa CSRF e cookie de sessão, limpa credenciais ao trocar de tela e apresenta somente um estado transitório de sessão iniciada, sem inventar dados privados.
- O logout bem-sucedido volta ao login. Se o backend responder 401, a interface também remove o estado visual autenticado; falhas ambíguas de rede, 403 ou 5xx mantêm a tela da sessão para permitir nova tentativa.
- Como ainda não existe endpoint de consulta da conta, recarregar a página não restaura o estado visual da sessão, embora o cookie do backend possa continuar válido. Essa limitação será resolvida pelas tarefas de consulta da conta e fundação completa do frontend.
- A interface de login/logout foi aprovada pelo agente Revisor e passou com 21 testes frontend, ESLint e build Vite.
- O backend local foi reiniciado após a interface de login e está executando a versão da T08 na porta 8080; os logs dessa execução ficam em `target/backend-local.log` e `target/backend-local-error.log`.
- A T09 foi concluída e aprovada pelo agente Revisor: `GET /api/accounts/me` consulta somente a conta derivada do UUID da sessão e retorna nome, CPF mascarado como `529.***.***-25` e e-mail como `m***@domínio`, sem identificador ou credenciais.
- `PATCH /api/accounts/me/email` e `PATCH /api/accounts/me/password` exigem senha atual, CSRF e conta ativa. E-mail é normalizado e único entre contas ativas; a nova senha segue exatamente RN04. Nome e CPF não possuem endpoints nem métodos de alteração.
- Após alteração válida de e-mail ou senha, todas as sessões da conta são revogadas, inclusive a atual. A revogação usa um único `DELETE` parametrizado por `PRINCIPAL_NAME` na mesma transação da alteração, com remoção dos atributos por `ON DELETE CASCADE`.
- O rollback foi comprovado após a execução real da revogação: se ocorrer falha, a credencial volta ao valor anterior e duas sessões simultâneas permanecem existentes e utilizáveis. Erros de validação, conflito, senha incorreta ou CSRF também não revogam sessões.
- A validação final da T09 passou com 85 testes backend, sem falhas ou erros, incluindo PostgreSQL local `gestao_acoes_test`; dois testes Testcontainers antigos permaneceram ignorados. O frontend existente passou com 21 testes, ESLint e build Vite.
- A T09 não implementou sua interface de consulta/alteração; isso permanece na tarefa de frontend correspondente.
- A T10 foi concluída e aprovada pelo agente Revisor: `DELETE /api/accounts/me` exige e-mail atual, senha atual e a palavra exata `Excluir`, altera a conta de `ACTIVE` para `INACTIVE`, registra o horário de Brasília e revoga atomicamente todas as sessões.
- A exclusão da T10 é somente lógica. Saldo, nome, CPF, e-mail, hash, criação, corretoras, posições, movimentações e pontos patrimoniais permanecem associados ao mesmo UUID; nenhuma movimentação ou ponto novo é criado.
- `POST /api/accounts/reactivation/check` informa apenas a disponibilidade por CPF e `POST /api/accounts/reactivation` reativa uma única conta `INACTIVE`, sem criar sessão. O estado `DELETED` ficou reservado como terminal e não é produzido nem reativado pela T10.
- Se o CPF tiver nenhuma ou mais de uma conta inativa, se já existir conta ativa com o CPF ou e-mail, ou se a candidata estiver `DELETED`, a reativação é rejeitada sem expor dados pessoais. A existência de múltiplas inativas não é resolvida por escolha arbitrária.
- A reativação restaura a mesma conta e limpa `inactivated_at`, preservando todos os dados. O cadastro alternativo continua podendo criar uma nova conta com CPF/e-mail usados somente por conta inativa, sem alterar a antiga.
- A validação final da T10 passou com 93 testes backend sem falhas ou erros, incluindo PostgreSQL local `gestao_acoes_test`; dois testes Testcontainers antigos permaneceram ignorados. O frontend existente passou com 21 testes, ESLint e build Vite.
- A T10 não implementou telas de exclusão e reativação; elas permanecem na tarefa de frontend correspondente.

## Decisões funcionais confirmadas

- O sistema é um simulador acadêmico para pessoa física, com um único tipo de usuário: `Investidor`. Cadastro, login e reativação são realizados pelo investidor ainda não autenticado; as demais funções exigem autenticação.
- Cada usuário terá conta individual, sem vínculo ou compartilhamento com outras contas.
- Cadastro: nome, CPF, e-mail e senha. Não haverá confirmação de e-mail na primeira versão.
- O saldo inicial será de R$ 10.000,00 e pertence à conta, sendo compartilhado por todas as corretoras cadastradas.
- As posições em ativos pertencem a uma corretora específica.
- A corretora será pesquisada somente por CNPJ e aceita apenas se constar na CVM como `CTVM`.
- O mesmo CNPJ não poderá ser cadastrado duas vezes na mesma conta.
- Compra e venda serão simulações instantâneas a preço de mercado, sem livro de ordens ou preço definido pelo usuário.
- Entradas inválidas, saldo insuficiente, venda ou transferência acima da posição serão rejeitados sem alteração parcial dos dados.
- Transferências entre corretoras preservam quantidade, custo e histórico; não alteram saldo e não têm taxa ou liquidação.
- O histórico é imutável e registra somente movimentações concluídas com sucesso.
- O histórico terá 20 registros por página.
- Pontos patrimoniais serão registrados somente após saldo inicial, aporte, compra, venda e transferência; atualizações isoladas de cotação não criarão pontos.
- Ativos internacionais serão exibidos em dólar e em real, com conversão direta e sem taxa cambial.
- Na documentação e nos resumos, usar `cotação USD/BRL` para o fator de conversão e evitar chamá-lo de `taxa cambial`. A primeira versão não cobra tarifa de câmbio, spread, comissão ou qualquer outro custo; a conversão é exclusivamente `valor em USD × cotação USD/BRL`.
- A cotação USD/BRL será consultada diariamente na AwesomeAPI por HTTP REST; falhas usam o último valor. Após uma semana, será exibido aviso sem bloquear operações.
- As cotações brasileiras em carteira serão atualizadas a cada cinco minutos; as norte-americanas, uma vez ao dia pela Twelve Data, enquanto backend e internet estiverem disponíveis.
- Pesquisas e confirmações de compra ou venda brasileiras tentarão obter cotação atual antes do cache; operações norte-americanas usarão a cotação diária armazenada.
- Compras adicionais recalculam o preço médio por média ponderada; vendas parciais mantêm o preço médio unitário restante.
- Valores externos serão arredondados para duas casas por `HALF_UP`.
- Falhas de API usam a última cotação armazenada; sem cotação armazenada, a operação será bloqueada.
- O dashboard terá saldo, posição, preço médio, lucro/prejuízo, gráficos históricos, distribuições por ação, corretora e mercado, e filtros de 4 semanas, 3 meses, 6 meses, 1 ano, 5 anos e máximo.
- A interface React será responsiva e inspirada visualmente em Investidor10 e Rico, sem copiar identidade visual.
- Datas e horas usarão o horário de Brasília; valores monetários terão duas casas decimais.
- A primeira versão rodará somente localmente, mas precisará de internet para consultar serviços externos.

## Decisões técnicas confirmadas

- Referência exclusivamente estrutural: `https://github.com/Os-Tops/Corretora-Acoes-Apiv2`, branch `dev` considerada na pesquisa. O código não será copiado, incorporado nem reproduzido.
- Frontend: React com JavaScript e Vite.
- Backend: Java 17, Spring Boot 3.4.0 como ponto de partida e Maven Wrapper.
- Arquitetura: monólito em camadas; portas/adapters apenas nas integrações externas.
- Estrutura interna própria, apenas inspirada na organização observada no repositório de referência, usando os pacotes `api`, `config`, `domain`, `infra`, `repository`, `service` e `scheduler`; o React permanece diretamente em `src/main/front` com `components`, `pages`, `services` e `styles`.
- Um único processo Spring Boot e um único PostgreSQL; sem microsserviços, Redis, mensageria ou gateway.
- Banco principal: PostgreSQL.
- Antes de iniciar ou validar o backend localmente, sempre verificar a conexão com o PostgreSQL. Se o banco `gestao_acoes` não existir, criá-lo antes de executar a aplicação; se já existir, preservá-lo e apenas aplicar as migrações Flyway pendentes. Nunca recriar, apagar ou sobrescrever um banco existente automaticamente.
- Para o ambiente local atual, usar o usuário exclusivo `gestao_acoes`; credenciais devem permanecer em variáveis de ambiente ou no arquivo `.env` não versionado.
- Banco para testes automatizados rápidos: H2.
- MySQL não será suportado na primeira versão.
- Persistência: Spring Data JPA/Hibernate; valores exatos com `BigDecimal` e `NUMERIC/DECIMAL`.
- Migrações: Flyway antes da consolidação do esquema.
- Autenticação: Spring Security, sessão opaca em cookie seguro e senhas com bcrypt.
- Cliente HTTP: Spring Cloud OpenFeign.
- Cotações brasileiras: Brapi.
- Cotações norte-americanas: Twelve Data, uma vez ao dia, sujeita a prova técnica de cobertura e campos.
- USD/BRL: AwesomeAPI por HTTP REST, uma vez ao dia.
- As atualizações diárias de ativos norte-americanos e USD/BRL ocorrerão às 10h no horário de Brasília.
- Cache de cotações: PostgreSQL, sem Redis.
- Movimentações financeiras serão transações curtas e atômicas; chamadas externas ocorrerão antes da transação, seguidas de revalidação do estado.
- Controllers tratarão apenas HTTP e delegarão regras aos casos de uso e ao domínio.
- O frontend não será fonte de verdade para preços, saldo, preço médio ou resultados.
- Dependências herdadas sem uso serão removidas, em especial Thymeleaf e Alpha Vantage.
- Testes: JUnit/Mockito, Spring Boot Test, H2 para testes rápidos, PostgreSQL/Testcontainers nos fluxos críticos e mocks para APIs externas.

## Fora do escopo da primeira versão

- Hospedagem ou implantação em nuvem.
- Recuperação de senha e qualquer envio de e-mail.
- Verificação de e-mail no cadastro.
- Importação de carteira real, Open Finance, integração com B3 ou execução de ordens reais.
- Livro de ofertas, ordens limitadas, taxas, impostos, dividendos, desdobramentos e grupamentos.
- Relatório ou exportação para Excel.
- Resumo prévio da operação; haverá apenas confirmação simples.
- Pesquisa de corretora por nome e validação adicional pelo Banco Central.
- Revalidação periódica da situação da corretora após o cadastro.
- Identificação do tipo do ativo; bastam ticker, nome, mercado, moeda e cotação.
- Atualização manual solicitada pelo usuário.
- Comprovação de identidade adicional para reativação de conta; na primeira versão, a reativação não a exigirá.

## Validações técnicas futuras

- Validar na Twelve Data os campos mínimos e a cobertura necessária antes da integração completa.
- Priorizar dados abertos da CVM processáveis pelo backend e validar a fonte/formato antes do cadastro completo de corretora, evitando automação de páginas ou CAPTCHA.
- A base com Spring Boot 3.4.0 e Java 17 foi validada pelo build e pelo teste inicial na T01; novas dependências continuarão sendo validadas em suas respectivas tarefas.
- Para dúvidas não críticas futuras, será adotada a alternativa mais simples e registrada como suposição reversível.

## Próximo passo

Implementar somente a T11 — fundação do frontend. O PostgreSQL local está funcional e deve ter sua conexão verificada antes das execuções; criar o banco `gestao_acoes` somente se ele ainda não existir. Não usar Docker ou Testcontainers nas próximas validações; testes de integração devem utilizar PostgreSQL local e o banco separado `gestao_acoes_test` quando precisarem alterar dados.

A implementação deverá ocorrer estritamente uma tarefa por vez. O repositório de referência poderá orientar somente a estrutura; nenhuma tarefa poderá copiar código, importar a branch ou tentar reproduzir o projeto de referência.

O trabalho deve ser coordenado pelo agente Orquestrador, usando os perfis disponíveis em `C:\Users\Arklok\.codex\agents`: Planejador para análise, Programador para backend e testes, Front-end Designer para interface e Revisor de código para auditoria independente. Delegações devem ter escopo específico, agentes não devem editar simultaneamente os mesmos arquivos e o Orquestrador deve consolidar e validar toda entrega.

Ao concluir cada tarefa, o Orquestrador deve sempre apresentar ao usuário um resumo contendo: objetivo da tarefa, comportamento implementado, arquivos criados ou alterados, verificações e testes executados, comparação com os critérios de conclusão e limitações ou pendências restantes.
