# Pesquisa técnica do projeto

> Pesquisa realizada em 13 de agosto de 2026. Planos gratuitos, limites de APIs e versões de ferramentas podem mudar; devem ser conferidos novamente antes da implementação e da apresentação.

## 1. Como ler este documento

As conclusões usam três classificações:

- **Fato:** informação confirmada pela visão do projeto ou por documentação oficial referenciada;
- **Recomendação:** escolha técnica sugerida para este projeto acadêmico;
- **Suposição:** hipótese reversível adotada quando ainda não existe decisão definitiva.

## 2. Resumo executivo

### Recomendação principal

Adotar uma aplicação web com:

- Frontend SPA em **React + JavaScript + Vite**, seguindo o repositório-base;
- Backend em **Java + Spring Boot**, usando Spring MVC, Spring Security e Spring Data JPA;
- Build do backend com **Maven Wrapper**;
- Banco principal **PostgreSQL local**;
- **H2 opcional** para testes rápidos, sem substituir os testes finais com PostgreSQL;
- Arquitetura de **monólito em camadas**, preservando `domain/port` e `infra/adapter` do repositório-base para APIs externas;
- Autenticação por sessão em **cookie `HttpOnly`, `Secure` e `SameSite`**, evitando tokens no armazenamento web;
- Migrações de banco com **Flyway** antes da entrega final;
- Testes com JUnit, Mockito, Spring Boot Test e React Testing Library; Testcontainers e Playwright ficam restritos aos cenários críticos;
- Execução exclusivamente local na primeira versão, sem serviços de hospedagem.

Essa combinação atende às exigências acadêmicas, mantém baixa complexidade operacional e permite demonstrar separação de camadas, segurança, persistência relacional, integração externa e testes.

### 2.1. Repositório-base

**Decisão:** a evolução será baseada no repositório [Os-Tops/Corretora-Acoes-Apiv2](https://github.com/Os-Tops/Corretora-Acoes-Apiv2), considerando sua branch padrão `dev` na data desta pesquisa.

**Fato:** o repositório-base utiliza Maven Wrapper, Java 17, Spring Boot 3.4.0, Spring Data JPA, H2, PostgreSQL, Spring Cloud OpenFeign e testes do Spring Boot. Sua estrutura já separa controllers, services, repositories, portas de domínio, clientes HTTP e adapters. Fontes: [`pom.xml`](https://github.com/Os-Tops/Corretora-Acoes-Apiv2/blob/dev/pom.xml) e [código Java](https://github.com/Os-Tops/Corretora-Acoes-Apiv2/tree/dev/src/main/java/com/projeto/gestao).

**Fato:** o frontend existente usa React com JavaScript e Vite dentro de `src/main/front`. Fonte: [`package.json`](https://github.com/Os-Tops/Corretora-Acoes-Apiv2/blob/dev/src/main/front/package.json).

**Recomendação:** reutilizar a organização e os componentes úteis, mas não copiar automaticamente decisões incompatíveis com a nova visão. A nova versão precisará acrescentar autenticação, usuários, validação CVM/CTVM, saldo, movimentações, transferências, histórico e dashboards completos.

**Decisão:** manter Java 17 e Spring Boot 3.4.0 como ponto de partida do repositório-base. Antes da implementação, remover dependências e código herdados que não atendem à nova visão, especialmente Thymeleaf e a integração Alpha Vantage; a compatibilidade das versões restantes deverá ser confirmada pelo build e pelos testes.

## 3. Soluções semelhantes

### 3.1. Kinvo

**Fato:** o Kinvo apresenta uma visão consolidada de patrimônio, desempenho, distribuição, evolução histórica, patrimônio por instituição e ganho de capital. Também informa que limitações das instituições conectadas podem afetar dados históricos e indicadores. Fontes: [Resumo da Carteira — Kinvo](https://suporte.kinvo.com.br/open-finance/articles/resumo-da-carteira) e [Primeiros passos — Kinvo](https://consolidador.kinvo.com.br/lp-primeiros-passos-kinvo/).

**Lições aplicáveis:**

- Separar patrimônio, desempenho e distribuição;
- Exibir a instituição associada à posição;
- Informar claramente quando dados externos estiverem incompletos ou desatualizados;
- Não confundir aporte com rentabilidade.

### 3.2. Gorila

**Fato:** o Gorila se apresenta como plataforma de consolidação e visualização de múltiplas carteiras, com métricas de posição, alocação, rentabilidade, períodos de análise e atualização de ativos brasileiros e internacionais. Fontes: [Gorila View](https://gorila.com.br/view) e [Gorila para investidores](https://gorila.com.br/para-investidores/).

**Lições aplicáveis:**

- Dashboard geral e visão por carteira/corretora;
- Distribuição visual e evolução temporal;
- Distinção entre consolidação de dados e execução real de ordens.

### 3.3. Rico

**Fato:** a Rico combina acompanhamento de investimentos com execução real, home broker e diversos produtos. Isso a torna referência visual, mas funcionalmente muito maior e mais regulada que este simulador. A própria documentação alerta que falhas de terceiros podem prejudicar o recebimento de informações atualizadas. Fontes: [Rico](https://www.rico.com.vc/) e [Vantagens Rico](https://www.rico.com.vc/vantagens-rico/).

**Recomendação:** usar Rico e Investidor10 apenas como inspiração de hierarquia visual — resumo no topo, cartões de indicadores, tabelas e gráficos — sem reproduzir home broker, recomendações, produtos bancários ou negociação real.

### 3.4. Diferença do projeto

**Fato:** este projeto é um simulador acadêmico manual. Ele não importará posições reais via Open Finance/B3 e não executará ordens.

**Recomendação:** deixar permanentemente visível a indicação de “ambiente simulado” e a data/hora da cotação. Isso reduz a possibilidade de o usuário interpretar os valores como recomendação ou dado em tempo real garantido.

## 4. Alternativas de tecnologias

### 4.1. Backend

| Alternativa | Vantagens acadêmicas | Desvantagens | Avaliação |
|-------------|----------------------|--------------|-----------|
| Spring Boot + Spring MVC | Exigido pelo trabalho; ecossistema integrado para REST, validação, segurança, JPA, agendamento e testes | Mais configuração e conceitos que soluções JavaScript simples | **Recomendado** |
| Spring WebFlux | Cliente/servidor não bloqueante e bom para alta concorrência | Introduz programação reativa e aumenta a curva de aprendizado | Não recomendado para o escopo atual |
| Jakarta EE/Quarkus | Boa arquitetura Java e baixo consumo | Diverge da exigência explícita de Spring Boot | Rejeitado |

**Fato:** Spring Boot fornece recursos comuns de produção, como configuração externa, segurança, métricas e health checks, e suporta aplicações executáveis independentes. Fonte: [Spring Boot](https://docs.spring.io/spring-boot/index.html).

**Recomendação:** usar Spring MVC tradicional. O volume acadêmico não justifica tornar todo o sistema reativo.

### 4.2. Cliente para APIs externas

| Alternativa | Característica | Uso sugerido |
|-------------|----------------|--------------|
| `RestClient` | Síncrono, moderno e simples | **Recomendado** para chamadas pontuais |
| HTTP Service Client | Interfaces declarativas sobre cliente HTTP | Boa alternativa para reduzir código repetitivo |
| `WebClient` | Não bloqueante, reativo e adequado a streaming/alta concorrência | Útil apenas se o grupo dominar Reactor |
| `RestTemplate` | Cliente síncrono antigo | Evitar em projeto novo |
| OpenFeign | Declarativo e já utilizado pelo repositório-base | **Recomendado para preservar a base existente** |

**Fato:** a documentação atual do Spring apresenta `RestClient`, `WebClient` e HTTP Service Clients; `RestTemplate` está depreciado no Spring Framework 7 em favor de `RestClient`. Fonte: [REST Clients — Spring Framework](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html).

**Decisão:** manter OpenFeign porque ele já está configurado e utilizado no repositório-base. Cada cliente Feign continuará encapsulado por uma porta de domínio e um adapter, evitando que services dependam diretamente dos contratos externos.

**Recomendação:** não misturar OpenFeign, `RestClient` e `WebClient` na primeira versão.

### 4.3. Frontend

| Alternativa | Vantagens | Desvantagens | Avaliação |
|-------------|-----------|--------------|-----------|
| React + JavaScript + Vite | Já utilizado pelo repositório-base e não exige migração | Não oferece verificação estática completa | **Recomendado** |
| React + TypeScript + Vite | Acrescenta verificação estática aos contratos do frontend | Exige migrar o frontend existente | Alternativa futura |
| React com framework full-stack | Roteamento e recursos integrados | Sobreposição com o backend Spring e maior complexidade | Desnecessário |
| Thymeleaf | Integração direta com Spring e deploy único | Não atende à decisão confirmada de React | Rejeitado |

**Fato:** React cria interfaces por composição de componentes e não prescreve sozinho roteamento ou busca de dados. O Create React App foi descontinuado. Fontes: [React](https://react.dev/) e [Instalação do React](https://react.dev/learn/installation).

**Fato:** TypeScript adiciona verificação estática de tipos ao JavaScript, e o Vite oferece um template oficial `react-ts`. Fontes: [TypeScript](https://www.typescriptlang.org/docs/) e [Vite](https://vite.dev/guide/).

**Recomendação:** manter React com JavaScript e Vite, como no repositório-base, evitando uma migração para TypeScript durante a ampliação funcional. Usar React Router para navegação e não adicionar uma biblioteca de estado/cache na primeira etapa. Fonte: [React Router](https://reactrouter.com/).

### 4.4. Gráficos e componentes visuais

**Recomendação:** usar uma única biblioteca React de gráficos, como Recharts, por reduzir esforço na construção de linhas e gráficos de distribuição. Para o restante, usar CSS próprio e evitar introduzir um design system na primeira versão. Fonte: [Recharts](https://recharts.github.io/).

## 5. Banco de dados

### 5.1. Comparação

| Banco | Pontos fortes | Limitações no projeto | Avaliação |
|-------|---------------|-----------------------|-----------|
| PostgreSQL | Tipos numéricos exatos, transações, constraints e índices | Requer instalação local ou container | **Principal recomendado** |
| MySQL | Popular e compatível com JPA | Manter três bancos aumentaria configuração e testes sem benefício para a primeira versão | Alternativa considerada e rejeitada |
| H2 | Inicialização rápida, memória e testes simples | Seus modos de compatibilidade não substituem testes no banco principal | Apenas desenvolvimento/testes rápidos |

**Fato:** PostgreSQL oferece `numeric/decimal` de precisão selecionável, enquanto `real` e `double precision` são inexatos. Fonte: [Numeric Types — PostgreSQL](https://www.postgresql.org/docs/current/datatype-numeric.html).

**Fato:** H2 oferece modos de compatibilidade para diferentes bancos, mas documenta diferenças e limitações próprias. Fonte: [H2 Features](https://h2database.com/html/features.html).

**Recomendação:** persistir dinheiro e preços como `NUMERIC/DECIMAL`, nunca `float` ou `double`. No Java, usar `BigDecimal`. Quantidades podem ser inteiras.

**Fato:** PostgreSQL suporta diferentes níveis de isolamento transacional; `Serializable` pode exigir que a aplicação repita uma transação abortada. Fonte: [Transaction Isolation — PostgreSQL](https://www.postgresql.org/docs/current/transaction-iso.html).

**Recomendação:** começar com transações padrão e constraints de banco. Não adicionar estratégia avançada de concorrência na primeira versão, pois o uso será local e concorrência entre dispositivos está fora do escopo.

### 5.2. Modelo relacional provável

**Suposição:** o modelo deverá conter, no mínimo:

- Usuário/conta;
- Sessão;
- Corretora global validada;
- Associação entre usuário e corretora, com estado ativo/removido;
- Ativo global;
- Cotação do ativo;
- Cotação cambial;
- Posição por conta, corretora e ativo;
- Movimentação imutável;
- Ponto histórico de patrimônio.

**Recomendação:** separar a corretora global validada da associação do usuário. Assim, vários usuários podem cadastrar o mesmo CNPJ sem duplicar dados oficiais, mas cada conta mantém seu próprio estado e histórico.

### 5.3. Migrações

**Fato:** Flyway aplica migrações versionadas em ordem e mantém uma tabela de histórico do esquema. Fonte: [Migrações Flyway](https://documentation.red-gate.com/fd/migrations-271585107.html).

**Recomendação:** durante o protótipo inicial, a criação automática do Hibernate pode ser usada apenas no perfil de testes. Antes de manter dados locais e preparar a entrega, usar Flyway para versionar o esquema.

## 6. Autenticação e segurança de sessão

### 6.1. Necessidade

**Fato:** autenticação é obrigatória porque o sistema armazena CPF, e-mail, saldo, posições e histórico individuais.

**Fato:** Spring Boot integra Spring Security e permite personalizar regras por uma cadeia de filtros. Fonte: [Spring Security no Spring Boot](https://docs.spring.io/spring-boot/reference/web/spring-security.html).

### 6.2. Sessão versus JWT

| Opção | Vantagens | Desvantagens | Avaliação |
|-------|-----------|--------------|-----------|
| Sessão opaca em cookie | Revogação simples, logout real, menor exposição ao JavaScript | Exige estado de sessão no servidor/banco | **Recomendada** |
| JWT em cookie | API pode validar sem consultar sessão | Revogação e encerramento de todas as sessões ficam mais complexos | Alternativa válida |
| JWT em `localStorage` | Implementação comum em tutoriais | Token acessível a JavaScript e exposto em caso de XSS | Não recomendado |

**Fato:** a OWASP recomenda não guardar tokens, identificadores de sessão, JWTs ou credenciais em `localStorage`/`sessionStorage`, preferindo cookies `HttpOnly`, `Secure` e `SameSite` ou padrão BFF. Fonte: [Session Management Cheat Sheet — OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).

**Decisão:** acrescentar Spring Security e Spring Session JDBC ao repositório-base. O projeto atual ainda não contém autenticação, e Spring Session JDBC permite persistir `HttpSession` e localizar sessões por usuário sem desenvolver tokens próprios. Fonte: [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html).

**Recomendação:** usar cookie `HttpOnly` e `SameSite`, proteção CSRF e CORS limitado ao frontend local. O atributo `Secure` será ativado quando houver HTTPS; em desenvolvimento HTTP local, ficará desativado por perfil.

### 6.3. Senhas

**Fato:** a OWASP recomenda algoritmos lentos de hash, como Argon2id, bcrypt ou PBKDF2, e nunca armazenamento em texto puro. Fonte: [Password Storage Cheat Sheet — OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).

**Recomendação:** usar bcrypt por ser a alternativa mais simples integrada ao Spring Security e suficiente para o projeto acadêmico. Recuperação de senha e envio de e-mail ficam fora da primeira versão.

## 7. Arquitetura apropriada

### 7.1. Estilo recomendado

**Recomendação:** manter o monólito em camadas e o recorte já existente no repositório-base. Portas e adapters serão usados somente nas integrações externas; repositories JPA continuarão diretos, sem portas adicionais.

```text
React SPA
    |
API REST / controllers / DTOs
    |
Casos de uso e transações
    |
Domínio e regras financeiras
    |
Repositories JPA e portas de serviços externos
    |
JPA/PostgreSQL + adaptadores Brasil API, ViaCEP, CVM, Brapi e provedor EUA/USD
```

### 7.2. Por que não microsserviços

**Recomendação:** não usar microsserviços. O grupo teria de resolver comunicação distribuída, autenticação entre serviços, consistência, observabilidade e múltiplos processos sem benefício acadêmico proporcional. Um monólito em camadas demonstra separação de responsabilidades e é muito mais fácil de testar e apresentar.

### 7.3. Áreas funcionais sugeridas

**Suposição:** pacotes internos por capacidade:

- `identity`: cadastro, sessão, alteração de credenciais e exclusão;
- `brokers`: pesquisa, validação e associação de corretoras;
- `marketdata`: ativos, cotações, câmbio e agendamentos;
- `portfolio`: saldo, posições, compra, venda e transferência;
- `history`: movimentações e patrimônio histórico;
- `dashboard`: consultas consolidadas;
- `shared`: erros, tempo, dinheiro e infraestrutura comum.

### 7.4. Transações e chamadas externas

**Fato:** Spring oferece gerenciamento declarativo de transações e regras de rollback. Fonte: [Declarative Transaction Management — Spring](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html).

**Recomendação:** não manter transação de banco aberta enquanto aguarda API externa. Obter a cotação antes da transação; depois executar uma transação curta que valida saldo/quantidade e grava posição, saldo, movimentação e ponto patrimonial.

### 7.5. Cache e agendamentos

**Recomendação:** usar tabelas de última cotação como cache persistente, sem Redis. Enquanto o backend estiver em execução, um agendamento do Spring atualizará a cada cinco minutos somente os tickers presentes em posições, agrupando símbolos quando a API permitir. Pesquisas e confirmações de compra ou venda também tentarão consultar a cotação atual antes de usar o cache.

## 8. Integrações externas

### 8.1. Brasil

| Necessidade | Opção | Avaliação |
|------------|-------|-----------|
| CNPJ | Brasil API | Consulta somente pelo CNPJ informado |
| CEP | ViaCEP | Serviço gratuito e simples |
| Participante registrado | Cadastro/Dados Abertos da CVM | Único critério regulatório adotado na primeira versão |
| Ativos brasileiros | Brapi | Boa aderência a ações, FIIs, BDRs e ETFs brasileiros |

**Fato:** ViaCEP oferece webservice gratuito de consulta de CEP. Fonte: [ViaCEP](https://viacep.com.br/).

**Fato:** a CVM permite consultar participantes registrados e disponibiliza dados para download/dados abertos. Fontes: [Consultar Participantes — CVM](https://www.gov.br/pt-br/servicos/consultar-participantes-cvm) e [Dados sobre regulados — CVM](https://www.gov.br/cvm/pt-br/acesso-a-informacao-cvm/perguntas-frequentes-da-cvm/como-acessar-dados-informacoes-sobre-regulados).

**Fato:** Brapi fornece dados em JSON de ativos brasileiros e requer token para acesso geral, mantendo apenas símbolos específicos para testes sem token. Fonte: [Documentação Brapi](https://brapi.dev/docs).

**Decisão:** a pesquisa de corretora aceitará somente CNPJ. A instituição será considerada válida apenas quando constar na CVM com a categoria `CTVM`. Não haverá busca por nome nem cruzamento adicional com dados do Banco Central.

**Risco:** a consulta web da CVM pode não ser uma API REST conveniente. Preferir download de dados abertos processado localmente pelo backend; evitar automação frágil de páginas HTML/CAPTCHA.

### 8.2. Estados Unidos

| Provedor | Plano gratuito verificado | Adequação ao projeto |
|----------|---------------------------|----------------------|
| Alpha Vantage | Até 25 requisições/dia; dados dos EUA em tempo real ou com 15 minutos de atraso são premium | Limite diário muito baixo |
| Twelve Data | Plano Basic informa 8 créditos por minuto e 800 por dia; consumo depende do endpoint e do número de símbolos | Pode atender a atualização quase em tempo real apenas com agrupamento, cache e controle rigoroso de consumo |

Fontes: [Suporte Alpha Vantage](https://www.alphavantage.co/support/) e [Créditos Twelve Data](https://support.twelvedata.com/en/articles/5615854-credits).

**Decisão:** usar Twelve Data na primeira versão e buscar atualização dos ativos em carteira a cada cinco minutos, além de atualizar sob demanda em pesquisas e confirmações de operações. Encapsular o provedor por uma interface para permitir troca futura. Antes da implementação completa, uma prova técnica deverá validar os campos exigidos, o caráter em tempo real ou quase em tempo real dos dados e o consumo de créditos para consultas agrupadas. Fonte adicional: [Preços Twelve Data](https://twelvedata.com/pricing).

### 8.3. USD/BRL

**Recomendação:** preferir uma fonte já adotada que ofereça USD/BRL, reduzindo o número de credenciais, desde que seu limite suporte a atualização diária. Manter um adaptador independente porque a fonte ainda é uma decisão reversível.

## 9. Riscos técnicos e de segurança

| Risco | Impacto | Mitigação recomendada |
|-------|---------|------------------------|
| Limites de APIs externas | Cotações falham ou bloqueiam a chave | Ciclo de cinco minutos somente para posições, consulta agrupada, cache persistente e tratamento de `429` |
| Cotação desatualizada | Operação distante do mercado | Mostrar data/hora e aviso destacado |
| Manipulação do preço pelo frontend | Saldo e resultado incorretos | Backend obtém e define o preço; nunca aceita preço enviado livremente pelo cliente |
| IDOR/acesso entre contas | Vazamento de CPF, carteira e histórico | Derivar conta da sessão, filtrar toda consulta por proprietário e testar identificadores de outra conta |
| Arredondamento monetário | Divergência de saldo/preço médio | `BigDecimal`/`NUMERIC`, escala e arredondamento centralizados; nunca `double` |
| Atualização perdida de saldo/posição | Saldo negativo ou venda acima da posição | Transação, constraints e bloqueio/versionamento otimista |
| XSS e furto de sessão | Tomada de conta | React sem HTML arbitrário, CSP, cookie HttpOnly/Secure/SameSite e validação de entrada |
| CSRF em autenticação por cookie | Operação não autorizada | Token CSRF e política CORS restrita |
| CPF/e-mail em logs | Violação de privacidade | Mascaramento e política explícita de logging |
| Segredos no Git | Comprometimento de APIs/banco | Variáveis de ambiente, arquivo local ignorado e rotação das chaves |
| Dependência de dados da CVM em formato instável | Cadastro de corretora indisponível | Adaptador isolado, importação de dados oficiais e testes de contrato |
| Exclusão lógica sem política de retenção | Acúmulo e risco de privacidade | Documentar finalidade acadêmica e restringir acesso; revisar antes de uso real |
| Aplicação local ou internet indisponível | Atualização em tempo real não ocorre | Exibir horário/aviso, usar a última cotação armazenada e retomar o ciclo quando o serviço voltar |

## 10. Estratégia de testes

### 10.1. Backend

**Fato:** `spring-boot-starter-test` reúne suporte do Spring Boot, JUnit Jupiter, AssertJ e outras ferramentas. Fonte: [Testing — Spring Boot](https://docs.spring.io/spring-boot/reference/testing/).

**Recomendação:**

1. **Testes unitários:** preço médio, lucro/prejuízo, conversão, arredondamento, aporte e transferência com JUnit e Mockito;
2. **Testes de integração:** movimentações, rollback, segurança e persistência com Spring Boot Test;
3. **Teste com PostgreSQL real:** usar Testcontainers somente nos fluxos financeiros e constraints mais críticos;
4. **Mocks das APIs externas:** cobrir sucesso, dados ausentes, timeout, `429` e erro `5xx`;
5. **Teste do agendamento:** verificar o ciclo de cinco minutos, agrupamento de símbolos, prevenção de execuções sobrepostas e retomada após reinício.

**Fato:** Spring Boot oferece integração oficial com Testcontainers, inclusive conexões a bancos JDBC e Flyway. Fonte: [Testcontainers — Spring Boot](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html).

**Recomendação:** não considerar testes apenas com H2 suficientes. O conjunto de integração deve executar em PostgreSQL para detectar diferenças reais de tipos, constraints e transações.

### 10.2. Frontend

**Recomendação:**

- Vitest e React Testing Library para funções, componentes e comportamento visível ao usuário;
- Mocks simples das chamadas REST durante os testes de componentes;
- Playwright somente para um fluxo principal completo, da criação da conta até compra e venda;
- Verificação manual da responsividade em desktop, tablet e celular.

Fontes: [Vitest](https://vitest.dev/), [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/) e [Playwright](https://playwright.dev/).

### 10.3. Casos críticos mínimos

- Compra não deixa saldo negativo;
- Venda/transferência não supera a posição;
- Falha no meio da movimentação não persiste efeito parcial;
- Usuário A não acessa registros do usuário B;
- Compra norte-americana usa preço e câmbio definidos pelo backend;
- Cotação antiga exibe aviso correto;
- Limite de API usa cache sem apagar dados válidos;
- Venda parcial mantém preço médio conforme a suposição vigente;
- Posição zerada some da carteira e permanece no histórico;
- Alteração de senha encerra as sessões.

## 11. Execução local

**Decisão:** a primeira versão não será implantada em nuvem nem publicada na internet.

### 11.1. Componentes locais

| Componente | Execução recomendada |
|------------|----------------------|
| Frontend React | Servidor de desenvolvimento do Vite |
| Backend Spring Boot | Processo Java local |
| PostgreSQL | Container Docker Compose ou instalação nativa |

### 11.2. PostgreSQL local

| Alternativa | Vantagem | Desvantagem |
|-------------|----------|-------------|
| Docker Compose | Ambiente reproduzível para os três integrantes | Exige Docker Desktop e mais memória |
| Instalação nativa | Menor camada de ferramentas | Configuração pode variar entre máquinas |

**Recomendação:** usar Docker Compose se todas as máquinas suportarem Docker; caso contrário, documentar a instalação nativa e padronizar versão, porta e nome do banco.

### 11.3. Consequências

- As APIs externas continuam exigindo internet e chaves válidas;
- Frontend e backend usarão portas locais distintas, exigindo CORS restrito ao endereço do frontend;
- Cookies seguros precisam de configuração própria para desenvolvimento em `localhost`;
- A atualização automática a cada cinco minutos só ocorrerá enquanto o backend estiver em execução e houver internet;
- A implantação fica fora da primeira versão e poderá ser pesquisada novamente futuramente.

## 12. Decisões técnicas recomendadas

| Tema | Escolha recomendada | Confiança |
|------|---------------------|-----------|
| Backend | Spring Boot + Spring MVC | Alta; exigência acadêmica |
| Frontend | React + JavaScript + Vite | Alta; já adotado no repositório-base |
| Banco principal | PostgreSQL | Alta |
| Banco de testes rápidos | H2 | Alta; não substitui PostgreSQL nos fluxos críticos |
| ORM | Spring Data JPA/Hibernate | Alta |
| Migrações | Flyway | Alta |
| Build | Maven Wrapper | Alta; já adotado no repositório-base |
| Java/Spring | Java 17 e Spring Boot 3.4.0 como ponto de partida | Alta; versões do repositório-base |
| Cliente HTTP | Spring Cloud OpenFeign | Alta; já adotado no repositório-base |
| Arquitetura | Monólito em camadas + adapters somente para APIs | Alta |
| Autenticação | Sessão opaca em cookie seguro + CSRF | Média-alta |
| Hash de senha | bcrypt | Alta |
| API brasileira | Brapi | Alta para escopo acadêmico |
| API norte-americana | Twelve Data, atualização a cada cinco minutos e sob demanda | Média; confirmar cobertura, latência e limite do plano |
| USD/BRL | Adaptador configurável, preferindo provedor já usado | Média |
| Cache | Banco relacional, sem Redis inicialmente | Alta |
| Teste de banco | Testcontainers apenas nos fluxos críticos | Média-alta |
| Execução | Exclusivamente local | Alta; decisão do projeto |

## 13. Suposições técnicas reversíveis

- Twelve Data oferecerá no plano disponível os ativos e campos mínimos exigidos; isso deverá ser validado por uma prova técnica antes de consolidar a escolha.
- Consultas agrupadas e cache permitirão cumprir a atualização quase em tempo real sem exceder o plano disponível; se a prova técnica refutar isso, o provedor ou o intervalo precisará ser revisto com o professor.
- O cache persistente no PostgreSQL será suficiente para o volume acadêmico, sem Redis.
- Uma única instância do backend executará os agendamentos durante a primeira versão.
- A configuração de cookie e CORS em `localhost` será suficiente para a primeira versão.
- Dados oficiais da CVM poderão ser importados/consultados sem automação de páginas protegidas por CAPTCHA.

## 14. Conclusão

A solução mais equilibrada é um monólito Spring Boot em camadas, com PostgreSQL local e frontend React. Ela atende diretamente ao enunciado, permite transações atômicas para saldo e posições e mantém as APIs externas substituíveis sem introduzir infraestrutura desnecessária.

Os maiores riscos não estão na renderização dos dashboards, mas em três áreas:

1. Consumo e disponibilidade das APIs de cotação;
2. Segurança e isolamento dos dados de cada usuário;
3. Consistência financeira entre saldo, posição e histórico.

Antes de desenvolver todas as telas, a primeira prova técnica deve validar Brapi, Twelve Data, CVM e USD/BRL com as contas gratuitas reais. Em paralelo, as regras financeiras devem receber testes unitários antes de serem conectadas à interface.

## 15. Referências principais

- [Spring Boot — documentação oficial](https://docs.spring.io/spring-boot/index.html)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa/)
- [Spring Security no Spring Boot](https://docs.spring.io/spring-boot/reference/web/spring-security.html)
- [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html)
- [REST Clients — Spring Framework](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
- [Transações declarativas — Spring Framework](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html)
- [React — documentação oficial](https://react.dev/)
- [TypeScript — documentação oficial](https://www.typescriptlang.org/docs/)
- [Vite — documentação oficial](https://vite.dev/guide/)
- [React Router — documentação oficial](https://reactrouter.com/)
- [Recharts — documentação oficial](https://recharts.github.io/)
- [Flyway — migrações](https://documentation.red-gate.com/fd/migrations-271585107.html)
- [PostgreSQL — tipos numéricos](https://www.postgresql.org/docs/current/datatype-numeric.html)
- [PostgreSQL — isolamento transacional](https://www.postgresql.org/docs/current/transaction-iso.html)
- [H2 — recursos e compatibilidade](https://h2database.com/html/features.html)
- [OWASP — armazenamento de senhas](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [OWASP — gerenciamento de sessão](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
- [Brapi — documentação](https://brapi.dev/docs)
- [ViaCEP](https://viacep.com.br/)
- [CVM — consulta de participantes](https://www.gov.br/pt-br/servicos/consultar-participantes-cvm)
- [Alpha Vantage — suporte e limites](https://www.alphavantage.co/support/)
- [Twelve Data — créditos](https://support.twelvedata.com/en/articles/5615854-credits)
- [Spring Boot — testes](https://docs.spring.io/spring-boot/reference/testing/)
- [Spring Boot — Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Vitest](https://vitest.dev/)
- [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/)
- [Playwright](https://playwright.dev/)
- [Repositório-base](https://github.com/Os-Tops/Corretora-Acoes-Apiv2)
- [`pom.xml` do repositório-base](https://github.com/Os-Tops/Corretora-Acoes-Apiv2/blob/dev/pom.xml)
