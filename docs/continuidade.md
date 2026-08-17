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

Implementar somente a T07 — cadastro de conta e saldo inicial. O PostgreSQL local está funcional e deve ter sua conexão verificada antes das execuções; criar o banco `gestao_acoes` somente se ele ainda não existir. Não usar Docker ou Testcontainers nas próximas validações; testes de integração devem utilizar PostgreSQL local e o banco separado `gestao_acoes_test` quando precisarem alterar dados.

A implementação deverá ocorrer estritamente uma tarefa por vez. O repositório de referência poderá orientar somente a estrutura; nenhuma tarefa poderá copiar código, importar a branch ou tentar reproduzir o projeto de referência.

O trabalho deve ser coordenado pelo agente Orquestrador, usando os perfis disponíveis em `C:\Users\Arklok\.codex\agents`: Planejador para análise, Programador para backend e testes, Front-end Designer para interface e Revisor de código para auditoria independente. Delegações devem ter escopo específico, agentes não devem editar simultaneamente os mesmos arquivos e o Orquestrador deve consolidar e validar toda entrega.

Ao concluir cada tarefa, o Orquestrador deve sempre apresentar ao usuário um resumo contendo: objetivo da tarefa, comportamento implementado, arquivos criados ou alterados, verificações e testes executados, comparação com os critérios de conclusão e limitações ou pendências restantes.
