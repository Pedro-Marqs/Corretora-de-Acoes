# Gestão de Ações e Corretoras

Aplicação acadêmica para simulação e acompanhamento de uma carteira de investimentos em ações brasileiras e norte-americanas.

O sistema permite registrar uma conta, controlar um saldo fictício em reais, cadastrar corretoras, pesquisar ativos, lançar compras, vendas e transferências e acompanhar a evolução patrimonial. **Não há movimentação de dinheiro real.**

## Sumário

- [Visão geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Configuração rápida com Docker](#configuração-rápida-com-docker)
- [Execução local](#execução-local)
- [Testes e verificações](#testes-e-verificações)
- [Banco de dados e Liquibase](#banco-de-dados-e-liquibase)
- [Configuração e integrações externas](#configuração-e-integrações-externas)
- [API principal](#api-principal)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Solução de problemas](#solução-de-problemas)
- [Documentação](#documentação)

## Visão geral

O projeto é organizado como um monólito modular, com uma API REST em Spring Boot e uma interface web em React. O PostgreSQL é o banco recomendado para execução da aplicação; os testes de integração usam H2 em memória com o mesmo changelog do banco principal.

O fluxo principal é:

```text
React/Vite ou Nginx
        │ HTTP + sessão + CSRF
        ▼
API Spring Boot
        │ JPA/Hibernate + Liquibase
        ▼
PostgreSQL
```

Em ambiente Docker Compose, o frontend é servido pelo Nginx, o backend se conecta ao serviço `postgres` e os dados do PostgreSQL ficam no volume nomeado `postgres-data`.

## Funcionalidades

- Cadastro, login, logout, reativação e exclusão da conta.
- Atualização de e-mail e senha.
- Saldo fictício em BRL, com depósitos e retiradas.
- Cadastro de corretoras por CNPJ, com consulta e validação de dados.
- Pesquisa de ativos por ticker e mercado, incluindo Brasil e Estados Unidos.
- Consulta de cotações, câmbio USD/BRL e informações auxiliares em APIs externas.
- Cache de cotações, com indicação de cotação desatualizada quando necessário.
- Registro de compras e vendas com preço e data informados pelo usuário.
- Transferência de posições entre corretoras.
- Carteira, posições, custo médio, resultado realizado e resultado não realizado.
- Histórico de movimentações e evolução patrimonial.
- Dashboard com distribuição da carteira e crescimento patrimonial.
- Exportação de dados para planilha XLSX.

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Backend | Java 17, Spring Boot 3.4, Spring Web, Spring Security, Spring Session JDBC, Spring Data JPA, Hibernate, Bean Validation |
| Persistência | PostgreSQL 17, H2 para testes, Liquibase |
| Frontend | React 19, React Router, JavaScript, Vite |
| Testes | JUnit, Spring Boot Test, MockMvc, Mockito, Vitest, Testing Library, JSDOM |
| Empacotamento | Maven Wrapper, npm, Docker, Docker Compose, Nginx |

## Arquitetura

O backend separa responsabilidades em camadas e módulos de negócio:

- `api`: controllers, requests, responses e tratamento de erros HTTP;
- `service`: casos de uso e regras transacionais;
- `domain`: entidades, enums e conceitos do domínio;
- `repository`: acesso aos dados com Spring Data JPA;
- `integration`: adaptadores para APIs externas;
- `security`: autenticação por sessão, CSRF, autorização e isolamento por conta;
- `scheduler`: tarefas periódicas, como atualização de cotações.

O frontend organiza as telas por fluxo de uso, com páginas públicas e páginas protegidas em `/app`. Em desenvolvimento, o Vite encaminha as chamadas `/api` para o backend; na imagem de produção, o Nginx faz esse proxy para o serviço `backend`.

## Pré-requisitos

Para desenvolvimento local:

- Git;
- Java 17;
- PostgreSQL 15 ou superior, ou Docker Desktop;
- Node.js `20.19+` ou `22.12+` e npm `10+`.

O Maven e o Vite não precisam ser instalados globalmente: o repositório fornece o Maven Wrapper e o frontend declara suas dependências em `package-lock.json`.

## Configuração rápida com Docker

Este é o caminho mais simples para executar a aplicação completa.

1. Na raiz do projeto, crie o arquivo local de ambiente:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Edite `.env` e troque, no mínimo, `POSTGRES_PASSWORD`. O arquivo `.env` é local, está no `.gitignore` e não deve ser versionado.

3. Valide a interpolação do Compose antes de iniciar os serviços:

   ```powershell
   docker compose config --quiet
   ```

4. Construa as imagens e suba a aplicação:

   ```powershell
   docker compose up --build
   ```

Acesse:

- Frontend: <http://localhost:3000>
- Backend: <http://localhost:8080>

O PostgreSQL não precisa ter uma porta publicada no host: dentro do Compose, o backend o acessa pelo hostname `postgres` na rede interna.

Comandos úteis:

```powershell
docker compose ps
docker compose logs -f backend
docker compose logs -f postgres
docker compose stop
docker compose down
```

`docker compose down` mantém o volume do banco. **`docker compose down -v` remove o volume `postgres-data` e apaga os dados persistidos; use-o somente quando essa perda for intencional.**

Os dois contextos de build possuem seu próprio `.dockerignore` (`backend/.dockerignore` e `frontend/.dockerignore`). Eles excluem dependências, artefatos de build, arquivos de IDE e configurações `.env` dos respectivos contextos.

## Execução local

### 1. Banco PostgreSQL

Crie um banco e um usuário, ou utilize valores equivalentes aos configurados no seu PostgreSQL. Um exemplo é:

```sql
CREATE USER gestao_acoes WITH PASSWORD 'troque-esta-senha';
CREATE DATABASE gestao_acoes OWNER gestao_acoes;
```

### 2. Variáveis do backend

O Spring Boot **não lê automaticamente** um arquivo `.env`. O arquivo serve como modelo e configuração do Compose; ao executar pela IDE ou pelo terminal, as variáveis precisam ser fornecidas ao processo.

No PowerShell, por exemplo:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:DB_HOST = "localhost"
$env:POSTGRES_PORT = "5432"
$env:POSTGRES_DB = "gestao_acoes"
$env:POSTGRES_USER = "gestao_acoes"
$env:POSTGRES_PASSWORD = "sua-senha"
```

Depois, em um terminal na raiz:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

No Linux/macOS, use `./mvnw spring-boot:run`.

### 3. Frontend

Em outro terminal:

```powershell
Set-Location frontend
npm ci
npm run dev
```

O frontend ficará disponível em <http://localhost:5173>. Por padrão, o Vite utiliza o proxy local para chamar o backend em `http://localhost:8080`. Para apontar para outro backend, copie `frontend/.env.example` para `frontend/.env` e configure `VITE_API_BASE_URL`.

## Testes e verificações

### Backend

```powershell
Set-Location backend
.\mvnw.cmd test
```

Para executar o ciclo Maven completo:

```powershell
.\mvnw.cmd clean verify
```

Os testes usam o perfil `test`, H2 em memória e Liquibase. Testes que dependem de serviços externos ou de infraestrutura adicional podem ser opt-in, conforme indicado no código de testes.

### Frontend

```powershell
Set-Location frontend
npm ci
npm test
npm run lint
npm run build
```

Na última verificação deste projeto, o backend passou com **311 testes, 0 falhas, 0 erros e 10 testes ignorados**; o frontend passou em testes, lint e build.

### Compose

Para conferir somente a configuração interpolada, sem iniciar os serviços:

```powershell
docker compose --env-file .env.example config --quiet
```

## Banco de dados e Liquibase

As alterações de schema são versionadas em:

```text
backend/src/main/resources/db/changelog/
```

O arquivo `db.changelog-master.xml` inclui os changesets em ordem. A aplicação inicia o Liquibase e o Hibernate trabalha com `ddl-auto=validate`: o Hibernate valida o modelo, mas não cria nem altera tabelas automaticamente.

O H2 usado nos testes aponta para o mesmo changelog principal. Isso reduz a diferença entre o banco de teste e o PostgreSQL.

Ao alterar o modelo:

1. crie um novo changeset numerado;
2. inclua-o no `db.changelog-master.xml`;
3. atualize as entidades JPA;
4. execute os testes;
5. nunca edite um changeset que já foi executado em um ambiente compartilhado.

O Liquibase controla as tabelas `DATABASECHANGELOG` e `DATABASECHANGELOGLOCK`. Em um banco existente que já possua estrutura e dados, faça um baseline/`changelog-sync` planejado antes de habilitar as migrações; não recrie o banco sem avaliar a preservação dos dados.

## Configuração e integrações externas

O arquivo `.env.example` documenta todas as variáveis disponíveis. As principais são:

| Variável | Uso |
| --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Credenciais usadas pelo PostgreSQL e pelo backend no Compose |
| `POSTGRES_PORT` | Porta do PostgreSQL na execução local; no Compose, o backend usa a porta interna `5432` |
| `SPRING_PROFILES_ACTIVE` | Perfil do Spring, normalmente `local` fora do Docker e `dev` no Compose |
| `DB_HOST` | Host do banco local; no Compose deve ser `postgres` |
| `BACKEND_PORT`, `FRONTEND_PORT` | Portas publicadas no host pelo Compose |
| `APP_CORS_ALLOWED_ORIGIN` | Origem permitida pelo backend |
| `APP_HTTP_CONNECT_TIMEOUT`, `APP_HTTP_READ_TIMEOUT` | Timeouts das integrações HTTP |
| `BRAPI_TOKEN`, `TWELVE_DATA_API_KEY`, `AWESOME_API_KEY` | Credenciais opcionais dos provedores que as exigirem |
| `T21_EXTERNAL_SMOKE` | Habilita o smoke test externo quando configurado para `true` |

As URLs-base configuráveis no `.env.example` correspondem a:

- BrasilAPI: dados auxiliares de empresas e instituições;
- ViaCEP: consulta de endereço;
- CVM: informações relacionadas ao mercado brasileiro;
- Brapi: ativos e cotações brasileiras;
- Twelve Data: cotações internacionais;
- AwesomeAPI: câmbio e dados auxiliares.

Não coloque tokens reais em código, logs, commits ou no README. Use variáveis de ambiente e mantenha `.env` fora do Git.

## API principal

A API usa sessão HTTP e CSRF. Os endpoints abaixo resumem os recursos disponíveis; os payloads e validações estão nos DTOs em `backend/src/main/java/com/projeto/gestao/api/controller`.

| Método | Rota | Acesso | Finalidade |
| --- | --- | --- | --- |
| `GET` | `/api/csrf` | Público | Obtém o token CSRF |
| `POST` | `/api/accounts` | Público | Cria uma conta |
| `POST` | `/api/auth/login` | Público | Inicia a sessão |
| `POST` | `/api/auth/logout` | Autenticado | Encerra a sessão |
| `GET` | `/api/accounts/me` | Autenticado | Consulta a conta atual |
| `GET` | `/api/dashboard` | Autenticado | Consulta os dados do dashboard |
| `GET` | `/api/assets/search` | Autenticado | Pesquisa ativos |
| `GET` | `/api/brokers/search` | Autenticado | Consulta uma corretora por CNPJ |
| `GET` | `/api/brokers` | Autenticado | Lista corretoras da conta |
| `POST` | `/api/brokers` | Autenticado | Associa uma corretora à conta |
| `DELETE` | `/api/brokers/{associationId}` | Autenticado | Remove uma associação |
| `GET` | `/api/wallet` | Autenticado | Consulta o saldo |
| `GET` | `/api/wallet/positions` | Autenticado | Consulta as posições |
| `POST` | `/api/wallet/deposits` | Autenticado | Registra depósito |
| `POST` | `/api/wallet/withdrawals` | Autenticado | Registra retirada |
| `POST` | `/api/wallet/purchases` | Autenticado | Registra compra |
| `POST` | `/api/wallet/sales` | Autenticado | Registra venda |
| `POST` | `/api/wallet/transfers` | Autenticado | Transfere uma posição |
| `GET` | `/api/history` | Autenticado | Consulta o histórico |

Todas as operações autenticadas devem respeitar o isolamento da conta logada. Para chamadas mutáveis pelo navegador, envie o token CSRF conforme a configuração do Spring Security.

## Estrutura do projeto

```text
.
├── backend/
│   ├── src/main/java/              # API, domínio, serviços, segurança e integrações
│   ├── src/main/resources/         # application*.properties e changelogs
│   ├── src/test/                   # testes unitários e de integração
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/api/                    # cliente HTTP
│   ├── src/components/             # componentes reutilizáveis
│   ├── src/pages/                  # telas públicas e privadas
│   ├── src/styles/                 # estilos globais e tema
│   ├── Dockerfile
│   └── package.json
├── docs/                           # visão, requisitos, arquitetura e continuidade
├── openspec/                       # specs e mudanças do OpenSpec
├── compose.yaml                    # PostgreSQL, backend e frontend
├── .env.example                    # modelo seguro de configuração
└── README.md
```

## Solução de problemas

### O Compose informa que uma variável não foi definida

Confirme que `.env` existe na raiz e contém `POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD`. Depois rode `docker compose config --quiet` novamente.

### O backend não conecta ao banco

- Fora do Docker, use `DB_HOST=localhost` e a porta publicada pelo PostgreSQL.
- Dentro do Compose, use `DB_HOST=postgres`; `localhost` aponta para o próprio container do backend.
- Verifique `docker compose ps` e aguarde o healthcheck do PostgreSQL ficar `healthy`.

### A porta 3000, 5173 ou 8080 já está ocupada

Altere `FRONTEND_PORT` ou `BACKEND_PORT` no `.env` para o Compose. Na execução local, encerre o processo que ocupa a porta ou altere a configuração do Vite/backend.

### O Liquibase acusa checksum ou lock

Não edite changesets já aplicados e não apague tabelas do banco como primeira tentativa. Verifique `DATABASECHANGELOG`, `DATABASECHANGELOGLOCK`, os logs do backend e o estado da transação; alterações de schema devem ser feitas em um novo changeset.

### Uma cotação externa não está disponível

Confira a URL-base e a chave do provedor no `.env`. A aplicação possui cache e pode sinalizar dados desatualizados, mas APIs externas continuam sujeitas a limites, indisponibilidade e mudanças de contrato.

## Documentação

Documentação do próprio projeto:

- [Visão do produto](docs/01-visao.md)
- [Requisitos](docs/03-requisitos.md)
- [Arquitetura](docs/04-arquitetura.md)
- [Tarefas](docs/05-tarefas.md)
- [Continuidade do projeto](docs/continuidade.md)
- [Specs e mudanças OpenSpec](openspec/)

Referências usadas na conferência de ambiente, migrações e containers:

- [Aula 10 — Variáveis de ambiente](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-10-VARIAVEIS-DE-AMBIENTE.md)
- [Aula 11 — Liquibase](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-11-LIQUIBASE.md)
- [Aula 12 — Docker e containers](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-12-DOCKER-E-CONTAINERS.md)

Este projeto segue as ideias dessas aulas, adaptadas à sua divisão em dois contextos de build (`backend` e `frontend`) e às integrações específicas da aplicação.
