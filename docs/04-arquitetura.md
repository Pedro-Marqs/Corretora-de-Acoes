# Arquitetura do projeto

## 1. Objetivo

Definir uma arquitetura simples, testável e adequada à primeira versão acadêmica do simulador de investimentos. Este documento deriva de:

- `docs/01-visao.md`;
- `docs/02-pesquisa.md`;
- `docs/03-requisitos.md`;
- especificações em `docs/spec/`.

A solução será uma aplicação web local formada por uma SPA React, uma API REST Spring Boot e um banco PostgreSQL. Não haverá microsserviços, mensageria, Redis, gateway de API ou infraestrutura de nuvem.

## 2. Visão geral

```text
Navegador
└── React + Vite
    └── HTTP/JSON + cookie de sessão + token CSRF
        └── API Spring Boot
            ├── Identidade e sessões
            ├── Corretoras
            ├── Dados de mercado
            ├── Carteira e movimentações
            ├── Histórico
            └── Dashboards
                ├── PostgreSQL
                └── APIs externas
                    ├── Brasil API
                    ├── ViaCEP
                    ├── dados abertos da CVM
                    ├── Brapi
                    ├── Twelve Data
                    └── AwesomeAPI
```

### Decisão arquitetural principal

Será adotado um **monólito modular em camadas**:

- um único processo Spring Boot;
- um único banco PostgreSQL;
- pacotes internos próprios por camada, inspirados apenas na organização geral da referência;
- comunicação direta por chamadas de método dentro do backend;
- adapters somente para sistemas externos;
- frontend React consumindo uma API REST JSON.

Essa divisão organiza o código sem introduzir os custos operacionais de sistemas distribuídos.

## 3. Componentes principais

### 3.1. Frontend React

Responsabilidades:

- apresentar cadastro, login e área da conta;
- apresentar saldo, corretoras, ativos, operações, histórico e dashboards;
- validar formato básico das entradas para resposta imediata;
- enviar comandos e consultas à API;
- manter apenas estado de interface, nunca o estado financeiro oficial;
- apresentar carregamento, vazio, sucesso, erro e dados desatualizados;
- exibir valores com duas casas decimais e datas em horário de Brasília;
- funcionar em desktop, tablet e celular sem rolagem horizontal da página.

O frontend não calcula o preço oficial da operação, não determina saldo, não recalcula preço médio como fonte de verdade e não acessa diretamente o banco ou APIs externas.

### 3.2. Camada de API

Responsabilidades:

- expor endpoints REST em JSON;
- receber e validar a estrutura das requisições;
- identificar a conta a partir da sessão;
- chamar um caso de uso;
- transformar resultados em respostas HTTP;
- encaminhar erros ao tratamento centralizado.

Controllers devem ser pequenos. Regras financeiras, autorização por propriedade e integração externa não devem ficar nessa camada.

### 3.3. Camada de aplicação

Responsabilidades:

- representar os casos de uso descritos nas specs;
- coordenar domínio, persistência e integrações;
- delimitar transações;
- garantir a ordem das alterações de saldo, posição, histórico e patrimônio;
- impedir que chamadas externas permaneçam dentro de transações de banco longas.

Exemplos de casos de uso:

- criar e autenticar conta;
- alterar credenciais;
- excluir ou reativar conta;
- registrar aporte;
- pesquisar e cadastrar corretora;
- pesquisar ativo;
- comprar, vender e transferir posição;
- consultar histórico e dashboards;
- atualizar cotações e câmbio.

### 3.4. Domínio financeiro

Responsabilidades:

- representar dinheiro, quantidade, posição e movimentação;
- aplicar `HALF_UP` para duas casas decimais;
- calcular preço médio ponderado;
- calcular lucro/prejuízo realizado;
- calcular valorização não realizada e patrimônio;
- validar saldo e quantidade;
- preservar custo em transferências;
- impedir estados financeiros inválidos.

As mesmas regras de cálculo devem ser reutilizadas por operações e dashboards para evitar resultados divergentes.

### 3.5. Persistência

Responsabilidades:

- armazenar contas, sessões, corretoras, ativos, cotações, câmbio, posições, movimentações e pontos patrimoniais;
- aplicar constraints e índices;
- executar consultas filtradas pela conta autenticada;
- paginar o histórico em 20 registros;
- garantir atomicidade com transações;
- evoluir o esquema por migrações Flyway.

PostgreSQL será o banco principal. H2 será usado somente em testes rápidos e não definirá o comportamento final do esquema.

### 3.6. Integrações externas

Cada serviço externo será acessado por uma interface interna e um adapter. Responsabilidades:

- montar a requisição externa;
- converter a resposta para um modelo interno;
- aplicar timeout e interpretar erros;
- rejeitar respostas sem campos obrigatórios;
- não apagar dados válidos armazenados quando a resposta vier incompleta;
- impedir que DTOs do provedor se espalhem pelo domínio.

### 3.7. Agendador

Responsabilidades:

- atualizar cotações brasileiras em carteira a cada cinco minutos;
- atualizar cotações norte-americanas e USD/BRL diariamente às 10h de Brasília;
- evitar execuções sobrepostas do mesmo ciclo;
- persistir valores válidos;
- manter o cache anterior em caso de falha;
- não criar movimentação ou ponto patrimonial somente por mudança de cotação.

Na primeira versão haverá uma única instância local do backend, portanto não é necessário coordenar agendadores distribuídos.

### 3.8. Segurança

Responsabilidades:

- autenticar e manter sessões;
- armazenar senhas com bcrypt;
- proteger alterações por CSRF;
- restringir CORS ao frontend local configurado;
- autorizar cada registro pela conta da sessão;
- mascarar dados pessoais e impedir seu registro integral em logs;
- revogar sessões após logout, alteração de credenciais ou exclusão da conta.

## 4. Organização das camadas

A organização será criada no novo projeto com os pacotes `api`, `config`, `domain`, `infra`, `repository` e `service`. A referência externa orienta apenas essa separação geral, sem cópia de classes ou implementações. As funcionalidades serão identificadas pelos nomes das classes dentro dessas camadas, sem criar um módulo de primeiro nível para cada capacidade.

| Pacote | Responsabilidade |
|---|---|
| `api/controller` | Endpoints REST de conta, corretora, ativo, carteira, histórico e dashboard |
| `api/exception` | Tratamento centralizado e respostas de erro |
| `config` | Segurança, CORS, sessão, relógio, agendamento e configurações por perfil |
| `domain/model` | Entidades e objetos do domínio |
| `domain/dto` | Modelos internos de entrada, saída e projeção que não pertencem a um cliente externo |
| `domain/port` | Contratos das integrações externas |
| `infra/client` | Clientes HTTP declarativos de cada provedor |
| `infra/client/dto` | Formatos específicos recebidos das APIs externas |
| `infra/adapter` | Implementações das portas e conversão de respostas externas |
| `repository` | Repositories JPA e consultas de persistência |
| `service` | Casos de uso, regras coordenadas e limites transacionais |
| `scheduler` | Disparo dos ciclos automáticos de cotação e câmbio |

Dependências internas recomendadas:

```text
api/controller
      |
      v
   service -----------------> repository
      |                            |
      v                            v
domain/model e domain/port     PostgreSQL
                    ^
                    |
              infra/adapter
                    |
              infra/client
                    |
               APIs externas
```

O pacote `service` concentra a coordenação das funcionalidades, mas cálculos financeiros reutilizáveis permanecem no domínio. O dashboard consulta repositories e serviços de cálculo, sem modificar saldo ou posição. O histórico é gravado pelos serviços de conta e carteira, nunca diretamente pelo frontend.

## 5. Estrutura de diretórios proposta

A estrutura é própria e apenas se inspira na separação geral observada no repositório de referência, sem replicar ou copiar seu código:

```text
meu-projeto/
├── docs/
│   ├── spec/
│   ├── 01-visao.md
│   ├── 02-pesquisa.md
│   ├── 03-requisitos.md
│   └── 04-arquitetura.md
├── src/
│   ├── main/
│   │   ├── java/com/projeto/gestao/
│   │   │   ├── api/
│   │   │   │   ├── controller/
│   │   │   │   └── exception/
│   │   │   ├── config/
│   │   │   ├── domain/
│   │   │   │   ├── dto/
│   │   │   │   ├── model/
│   │   │   │   └── port/
│   │   │   ├── infra/
│   │   │   │   ├── adapter/
│   │   │   │   └── client/
│   │   │   │       └── dto/
│   │   │   ├── repository/
│   │   │   ├── scheduler/
│   │   │   ├── service/
│   │   │   └── GestaoAcoesCorretorasApplication.java
│   │   ├── resources/
│   │   │   ├── db/migration/
│   │   │   ├── application.properties
│   │   │   ├── application-dev.properties
│   │   │   └── application-test.properties
│   │   └── front/
│   │       ├── components/
│   │       ├── pages/
│   │       ├── services/
│   │       ├── styles/
│   │       ├── App.jsx
│   │       ├── main.jsx
│   │       ├── index.html
│   │       └── package.json
│   └── test/
│       └── java/com/projeto/gestao/
│           ├── infra/adapter/
│           ├── repository/
│           └── service/
├── compose.yaml
├── pom.xml
├── mvnw
└── README.md
```

Essa estrutura preserva os nomes e a localização usados pelo projeto-base. Para evitar a duplicidade existente entre `api/controller` e um pacote `controller` na raiz, todos os novos controllers ficarão em `api/controller`.

No frontend, `components` conterá elementos reutilizáveis, `pages` conterá as telas, `services` centralizará as chamadas à API local e `styles` guardará os estilos. Não será acrescentada uma hierarquia `features` na primeira versão.

Os testes espelharão os pacotes produtivos relevantes. A distinção entre teste unitário e de integração será feita pelo tipo de teste e pela configuração utilizada, sem exigir diretórios artificiais `unit` e `integration`.

## 6. Modelo de dados

### 6.1. Entidades principais

| Entidade | Dados essenciais | Relações e restrições |
|---|---|---|
| `account` | id, nome, CPF, e-mail, hash da senha, saldo, estado, criação e inativação | CPF e e-mail únicos entre contas ativas; saldo em BRL |
| `broker` | id, CNPJ, razão social, nome fantasia, situação, categoria CVM e endereço | Cadastro institucional reutilizável |
| `account_broker` | id, conta, corretora, estado, datas de associação/remoção | Uma associação ativa por conta e CNPJ |
| `asset` | id, ticker, nome, mercado e moeda | Mercado BR ou US; moeda BRL ou USD |
| `quote` | ativo, preço, moeda, instante da cotação e instante da coleta | Mantém a última cotação válida por ativo |
| `exchange_rate` | par USD/BRL, valor, instante da cotação e coleta | Mantém o último câmbio válido |
| `position` | conta, corretora, ativo, quantidade, preço médio e custo total | Única por conta, corretora e ativo; quantidade inteira não negativa |
| `movement` | conta, tipo, ticker, cotação, quantidade, total, moeda, corretora/origem/destino, data/hora e saldo restante | Imutável; contém somente movimentação concluída |
| `patrimonial_point` | conta, movimentação, data/hora e patrimônio em BRL | Um ponto após cada movimentação prevista |
| tabelas de sessão | sessão, conta e validade técnica | Gerenciadas pelo Spring Session JDBC |

### 6.2. Relacionamentos

```text
account 1 ─── N account_broker N ─── 1 broker
account 1 ─── N position N ─── 1 asset
account_broker 1 ─── N position
asset 1 ─── 1 última quote
account 1 ─── N movement
movement 1 ─── 1 patrimonial_point
account 1 ─── N sessões
```

### 6.3. Decisões de modelagem

- Saldo fica na conta porque é compartilhado entre corretoras.
- Corretora institucional é separada da associação com a conta para permitir remoção lógica e recadastro sem duplicar seus dados básicos.
- Posição é separada por conta, corretora e ativo.
- Quantidade é inteira; preços, saldo, câmbio e resultados usam `NUMERIC/DECIMAL`.
- Movimentação guarda uma fotografia dos valores usados. Alterações futuras no nome da corretora ou no ativo não mudam o registro histórico.
- O histórico não precisa de uma tabela distinta por tipo de operação; campos não aplicáveis permanecem ausentes.
- Pontos patrimoniais são gravados somente após saldo inicial, aporte, compra, venda e transferência.
- Atualizações de cotação não alteram pontos históricos já gravados.
- Exclusão de conta e associação de corretora é lógica.
- Constraints do banco devem impedir saldo negativo, quantidade negativa e duplicidades ativas quando aplicável.

### 6.4. Índices mínimos

- conta ativa por CPF;
- conta ativa por e-mail;
- associação ativa por conta e corretora;
- posição por conta, corretora e ativo;
- movimentação por conta e data/hora;
- movimentação por conta combinada com filtros mais usados;
- ponto patrimonial por conta e data/hora;
- ativo por ticker e mercado.

Os índices devem ser confirmados pelas consultas reais. Não serão criados índices para todas as colunas antecipadamente.

## 7. Comunicação entre as partes

### 7.1. Frontend e backend

- protocolo HTTP local;
- conteúdo JSON;
- cookie de sessão enviado automaticamente;
- token CSRF nas operações que alteram estado;
- respostas de listagem com conteúdo, página atual, tamanho 20 e total de registros;
- datas em formato inequívoco com informação de fuso ou deslocamento.

O frontend e o backend podem usar portas diferentes durante o desenvolvimento. O CORS aceitará somente a origem local configurada.

### 7.2. Fluxo de consulta

```text
React → Controller → Caso de uso de consulta → Repository → PostgreSQL
                                      └──────→ Adapter externo, quando necessário
```

Consultas de dashboard devem usar projeções específicas, evitando carregar entidades completas e executar cálculos repetitivos no frontend.

### 7.3. Fluxo de movimentação

```text
1. Backend obtém cotação/câmbio utilizável fora da transação.
2. Inicia transação curta.
3. Revalida saldo, posição, corretora e propriedade.
4. Altera saldo e/ou posições.
5. Grava movimentação.
6. Calcula e grava ponto patrimonial.
7. Confirma a transação.
```

Se qualquer passo transacional falhar, todos os passos de 3 a 6 são revertidos.

### 7.4. Comunicação com APIs externas

- chamadas síncronas por HTTP REST através de OpenFeign;
- timeout explícito;
- DTO próprio para cada provedor;
- conversão para modelo interno antes de chegar ao domínio;
- cache persistente no PostgreSQL;
- tratamento específico para timeout, resposta inválida, `429` e `5xx`.

Não haverá fila ou retentativa infinita. Uma execução automática posterior fará uma nova tentativa.

## 8. Dependências externas

| Dependência | Finalidade | Frequência/uso | Fallback |
|---|---|---|---|
| Brasil API | Dados e situação do CNPJ | Pesquisa de corretora | Rejeitar nova validação se não houver resposta utilizável |
| ViaCEP | Endereço estruturado | Pesquisa de corretora | Preservar endereço válido anterior; bloquear cadastro se faltar dado obrigatório novo |
| Dados abertos CVM | Confirmar categoria CTVM | Cadastro de corretora | Bloquear cadastro sem confirmação |
| Brapi | Ativos e cotações brasileiras | Pesquisa/operação e ciclo de 5 minutos | Última cotação válida |
| Twelve Data | Ativos e cotações norte-americanas | Ciclo diário às 10h | Última cotação válida |
| AwesomeAPI | USD/BRL | Ciclo diário às 10h | Último câmbio válido |

Antes das integrações completas, devem ser validados:

- campos e cobertura da Twelve Data;
- fonte e formato processável dos dados abertos da CVM.

## 9. Tratamento de erros

### 9.1. Categorias

| Categoria | Exemplo | Resposta esperada |
|---|---|---|
| Validação | CPF inválido ou quantidade decimal | Indicar campos/regras não atendidos |
| Autenticação | Credenciais inválidas | Negar sem revelar qual credencial falhou |
| Autorização | Registro de outra conta | Negar sem expor o registro |
| Conflito | Corretora duplicada | Explicar o conflito sem alterar dados |
| Regra de negócio | Saldo ou posição insuficiente | Informar valores solicitado e disponível |
| Dependência externa | Timeout, `429` ou `5xx` | Usar cache quando permitido ou bloquear com mensagem funcional |
| Erro interno | Falha inesperada | Reverter transação e retornar mensagem genérica rastreável por log técnico |

### 9.2. Formato uniforme

As respostas de erro da API devem possuir, no mínimo:

- identificador funcional do erro;
- mensagem adequada ao usuário;
- lista de campos inválidos quando aplicável;
- instante do erro.

Não devem conter stack trace, classe Java, SQL, senha, chave de API ou detalhes internos.

### 9.3. Tratamento centralizado

Um manipulador global converterá exceções conhecidas em respostas consistentes. O domínio sinaliza a causa funcional; controllers não devem repetir blocos de tratamento.

Logs técnicos devem permitir diagnóstico, mas CPF e e-mail precisam estar mascarados. Senhas, cookies e chaves nunca serão registrados.

## 10. Segurança

### 10.1. Autenticação e sessão

- Spring Security e Spring Session JDBC;
- sessão opaca em cookie `HttpOnly` e `SameSite`;
- `Secure` habilitado quando houver HTTPS e desabilitado somente no perfil HTTP local;
- bcrypt para hash de senha;
- logout com invalidação da sessão;
- alteração de e-mail/senha e exclusão invalidam todas as sessões da conta;
- sem armazenamento de token no `localStorage`.

### 10.2. Autorização e isolamento

- há um único tipo de usuário: Investidor;
- o estado autenticado define acesso às funções privadas;
- a conta é obtida da sessão em todas as operações;
- repositories e consultas filtram pela conta;
- identificadores de corretora, posição e movimentação são validados contra essa conta;
- testes devem tentar acessar registros de outro usuário.

### 10.3. Proteção de dados

- CPF e e-mail parcialmente ocultados na interface;
- dados pessoais mascarados nos logs;
- senhas nunca recuperáveis em texto;
- credenciais externas fornecidas por configuração local não versionada;
- respostas não expõem campos internos desnecessários.

### 10.4. CSRF, CORS e XSS

- CSRF ativo nas requisições de alteração;
- CORS restrito ao endereço configurado do frontend;
- renderização React sem inserção de HTML arbitrário;
- validação de entrada no backend mesmo quando o frontend já validou.

### 10.5. Risco aceito na reativação

Na primeira versão, a reativação de conta não exige comprovação de identidade. Essa é uma decisão funcional confirmada, mas representa risco de tomada de conta. O fluxo deve ficar isolado para que uma verificação futura possa ser acrescentada sem alterar os demais módulos. A aplicação não deve apresentar essa limitação como segurança equivalente ao login.

## 11. Estratégia de testes

### 11.1. Testes unitários

Executados sem Spring quando possível:

- arredondamento `HALF_UP`;
- preço médio ponderado;
- venda parcial e venda total;
- lucro/prejuízo realizado;
- transferência e custo no destino;
- conversão USD/BRL;
- patrimônio e valorização;
- validações de quantidade e saldo.

### 11.2. Testes de aplicação

Com casos de uso e dependências simuladas:

- coordenação de cadastro e saldo inicial;
- aporte;
- compra, venda e transferência;
- fallback de cotação e câmbio;
- atualização de dados de corretora;
- criação de histórico e ponto patrimonial.

### 11.3. Testes de integração

Com Spring Boot:

- autenticação, sessão, CSRF e revogação;
- isolamento entre contas;
- repositories, filtros e paginação de 20 registros;
- rollback integral de movimentações;
- constraints e migrações;
- agendamentos sem sobreposição.

H2 pode acelerar testes simples. Fluxos financeiros, consultas específicas e constraints críticas devem rodar em PostgreSQL por Testcontainers.

### 11.4. Testes de adapters

Usar servidor HTTP simulado e respostas controladas para:

- sucesso;
- campo obrigatório ausente;
- timeout;
- HTTP `429`;
- HTTP `5xx`;
- preservação do cache anterior.

Poucos testes manuais ou de contrato podem consultar as APIs reais usando credenciais próprias, sem fazer a suíte comum depender da internet.

### 11.5. Testes do frontend

- Vitest para funções de formatação e cálculo apenas visual;
- React Testing Library para formulários, mensagens, confirmação e estados;
- mocks da API para componentes;
- um fluxo principal com Playwright: cadastro, login, corretora, aporte, compra e venda;
- verificação manual em desktop, tablet e celular.

### 11.6. Critério de cobertura funcional

Cada critério Dado/Quando/Então em `docs/spec/` deve corresponder a pelo menos um teste automatizado ou procedimento de aceitação identificado. A prioridade é cobrir regras financeiras, autorização e rollback, não atingir uma porcentagem artificial de linhas.

## 12. Configuração e execução local

### Perfis

- `local`: PostgreSQL local, frontend local e integrações reais configuradas;
- `test`: H2 para testes rápidos e adapters simulados;
- testes críticos: PostgreSQL efêmero por Testcontainers.

### Configurações externas

- URL e credenciais do PostgreSQL;
- origem permitida no CORS;
- chaves e URLs das APIs;
- timeouts;
- fuso `America/Sao_Paulo`;
- intervalos de atualização, mantendo os valores confirmados como padrão.

Valores sensíveis ficam fora do Git. Um arquivo de exemplo sem segredos deve documentar as variáveis necessárias.

## 13. Decisões rejeitadas

| Alternativa | Motivo da rejeição |
|---|---|
| Microsserviços | Aumentariam processos, comunicação, segurança e consistência sem benefício acadêmico proporcional |
| Redis | PostgreSQL é suficiente para o cache e volume local |
| Mensageria | Os fluxos são locais, síncronos e de baixo volume |
| JWT em `localStorage` | Aumenta exposição a XSS e complica revogação |
| Banco por módulo | Não há necessidade de isolamento físico no monólito |
| MySQL como segundo banco | Aumentaria variações de esquema e testes sem fazer parte da primeira versão |
| Frontend acessando APIs externas | Exporia chaves e duplicaria validações e fallbacks |
| Cálculos financeiros no frontend | Criaria duas fontes de verdade e permitiria manipulação do preço |
| Uma tabela por tipo de movimentação | Aumentaria consultas e duplicaria campos sem necessidade atual |

## 14. Justificativa das decisões

### Monólito modular

Permite demonstrar separação de responsabilidades, arquitetura em camadas, segurança, transações e integrações sem exigir implantação distribuída. É compatível com o tamanho do grupo, a execução local e o objetivo acadêmico.

### PostgreSQL como fonte de verdade

Oferece transações, constraints, índices e tipos decimais adequados aos cálculos financeiros. Também armazena sessões e cache, evitando componentes adicionais.

### Organização própria inspirada na referência

Criar os pacotes `api`, `config`, `domain`, `infra`, `repository` e `service`, além do frontend em `src/main/front`, oferece uma separação clara para o porte acadêmico. As funcionalidades continuam identificáveis pelos nomes de controllers, services, modelos e repositories. Nenhum código ou componente do repositório de referência será incorporado.

### Regras financeiras no backend

Garante uma única fonte de verdade para saldo, preço médio, resultado e patrimônio. O frontend fica responsável apenas pela apresentação.

### Transações curtas

Chamadas externas podem ser lentas ou falhar. Obter cotações antes da transação e revalidar o estado dentro dela reduz bloqueios sem abrir mão da atomicidade.

### Cache persistente

Permite continuar a simulação quando uma API falha e conserva os valores após reinício local. Redis seria infraestrutura adicional sem necessidade para o volume esperado.

### Adapters para integrações

APIs gratuitas podem mudar limites e formatos. Isolá-las permite trocar um provedor sem modificar regras financeiras ou controllers.

### Histórico e pontos patrimoniais separados

Movimentações servem à auditoria; pontos patrimoniais servem ao gráfico. A separação mantém consultas simples e respeita a decisão de criar pontos somente após movimentações.

## 15. Validações antes da implementação completa

Não são decisões funcionais pendentes, mas provas técnicas necessárias:

1. confirmar que a Twelve Data retorna ticker, nome, mercado, moeda e cotação para os ativos norte-americanos necessários;
2. identificar e validar o formato oficial de dados abertos da CVM usado para verificar CTVM;
3. validar o endpoint e os campos USD/BRL da AwesomeAPI;
4. criar a base própria com Java 17 e Spring Boot 3.4.0 e validar seu build sem dependências ou código herdados;
5. validar as migrações e os fluxos financeiros críticos em PostgreSQL.
