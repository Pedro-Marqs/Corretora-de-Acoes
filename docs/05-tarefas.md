# Tarefas de implementação

## 1. Como usar este documento

As tarefas estão em ordem recomendada. Cada uma produz uma alteração pequena, testável e revisável. Uma tarefa só deve ser iniciada quando suas dependências estiverem concluídas.

Os caminhos citados seguem a arquitetura de `docs/04-arquitetura.md` e a estrutura do repositório-base. Nomes exatos de classes podem ser ajustados durante a implementação, sem mudar a responsabilidade indicada.

## T01 — Incorporar e sanear o repositório-base

### Objetivo

Usar o projeto `Corretora-Acoes-Apiv2` como base compilável, removendo somente dependências e integrações confirmadas como fora da nova versão.

### Arquivos ou componentes envolvidos

- `pom.xml`, Maven Wrapper e `.gitignore`;
- `src/main/java/com/projeto/gestao/`;
- `src/main/front/`;
- integração Alpha Vantage e Thymeleaf herdados.

### Dependências

- Nenhuma.

### Passos de implementação

- Incorporar a branch `dev` do repositório-base à estrutura atual.
- Preservar a organização `api`, `config`, `domain`, `infra`, `repository` e `service`.
- Remover Alpha Vantage, Thymeleaf e código sem uso associado.
- Manter Java 17, Spring Boot 3.4.0, Maven Wrapper e frontend React/Vite.
- Registrar qualquer incompatibilidade encontrada sem adicionar funcionalidade nova.

### Testes necessários

- Executar o build limpo do backend.
- Executar instalação e build do frontend.
- Executar os testes herdados ainda aplicáveis.

### Concluída quando

- Backend e frontend compilarem a partir de um checkout limpo.
- Não houver referência ativa a Alpha Vantage ou Thymeleaf.
- Nenhuma funcionalidade nova tiver sido implementada nesta tarefa.

### Requisitos e specs relacionados

- RNF01, RNF02 e RNF22;
- `docs/04-arquitetura.md`, seções 4, 5 e 15.

## T02 — Configurar execução local e perfis

### Objetivo

Padronizar a execução local com PostgreSQL no perfil de desenvolvimento e H2 somente no perfil de testes rápidos.

### Arquivos ou componentes envolvidos

- `compose.yaml`;
- `application.properties`;
- `application-dev.properties`;
- `application-test.properties`;
- arquivo de exemplo de variáveis locais.

### Dependências

- T01.

### Passos de implementação

- Configurar PostgreSQL local sem versionar credenciais reais.
- Configurar H2 apenas para testes rápidos.
- Definir `America/Sao_Paulo` como fuso da aplicação.
- Externalizar chaves, URLs de APIs, CORS e timeouts.
- Documentar os comandos mínimos de inicialização.

### Testes necessários

- Iniciar a aplicação com o perfil local e conectar ao PostgreSQL.
- Iniciar teste simples com o perfil de teste e H2.
- Verificar que a aplicação falha de forma compreensível quando configuração obrigatória está ausente.

### Concluída quando

- Os dois perfis iniciarem com os bancos corretos.
- Nenhum segredo real estiver versionado.
- O fuso configurado for verificável em teste.

### Requisitos e specs relacionados

- RNF01, RNF03, RNF06 e RNF13;
- arquitetura, seção 12.

## T03 — Criar migração inicial do banco

### Objetivo

Criar o esquema relacional inicial e suas restrições por Flyway.

### Arquivos ou componentes envolvidos

- `src/main/resources/db/migration/`;
- modelos JPA em `domain/model`;
- repositories básicos.

### Dependências

- T02.

### Passos de implementação

- Criar tabelas de conta, corretora, associação conta-corretora, ativo, cotação, câmbio, posição, movimentação e ponto patrimonial.
- Preparar as tabelas exigidas pelo Spring Session JDBC.
- Usar tipos decimais para dinheiro, preços e câmbio.
- Criar chaves estrangeiras, unicidades e checks definidos na arquitetura.
- Criar os índices mínimos para conta, posição, movimentação e patrimônio.

### Testes necessários

- Aplicar migrações em PostgreSQL vazio.
- Validar constraints de saldo, quantidade e duplicidade.
- Executar migração no perfil H2 quando compatível com os testes rápidos.

### Concluída quando

- Um banco vazio for criado integralmente por Flyway.
- As constraints críticas falharem para dados inválidos.
- O esquema não depender de criação automática destrutiva do ORM.

### Requisitos e specs relacionados

- RNF03, RNF04 e RNF21;
- RN01, RN03, RN09, RN11 e RN30;
- arquitetura, seção 6.

## T04 — Implementar tipos financeiros, relógio e arredondamento

### Objetivo

Centralizar precisão monetária, arredondamento e horário usados por todas as funcionalidades.

### Arquivos ou componentes envolvidos

- utilitários ou objetos de domínio em `domain/model`;
- configuração de relógio em `config`;
- testes em `src/test/java`.

### Dependências

- T01.

### Passos de implementação

- Definir operações monetárias com `BigDecimal`.
- Centralizar escala de duas casas e modo `HALF_UP`.
- Centralizar conversão USD/BRL.
- Injetar relógio configurado para Brasília em vez de consultar tempo diretamente.
- Criar formatação compartilhada apenas onde necessária.

### Testes necessários

- Testar terceira casa de 0 a 4 e de 5 a 9.
- Testar soma, multiplicação e conversão sem `double`.
- Testar instante conhecido no fuso de Brasília.

### Concluída quando

- Cálculos financeiros não usarem `float` ou `double`.
- Casos-limite de `HALF_UP` passarem.
- Testes puderem controlar o horário.

### Requisitos e specs relacionados

- RNF04–RNF06;
- RN28;
- todas as specs com valores ou datas.

## T05 — Padronizar erros da API

### Objetivo

Fornecer respostas de erro funcionais e uniformes sem expor detalhes internos.

### Arquivos ou componentes envolvidos

- `api/exception/`;
- exceções de domínio;
- DTO de erro;
- configuração de logs.

### Dependências

- T01 e T04.

### Passos de implementação

- Definir categorias de validação, autenticação, autorização, conflito, regra de negócio, dependência externa e erro interno.
- Criar manipulador global.
- Padronizar identificador, mensagem, campos inválidos e instante.
- Mascarar CPF/e-mail nos logs e excluir senhas, cookies e chaves.

### Testes necessários

- Testar uma resposta de cada categoria.
- Verificar ausência de stack trace, SQL e segredos.
- Verificar mascaramento nos eventos de log testáveis.

### Concluída quando

- Controllers não precisarem repetir tratamento de exceções.
- Todos os erros possuírem o formato documentado.
- Dados sensíveis não aparecerem nas respostas.

### Requisitos e specs relacionados

- RNF12–RNF14 e RNF20;
- CE01–CE22;
- spec `interface-estados.md`, CA02.

## T06 — Configurar segurança, sessão, CSRF e CORS

### Objetivo

Criar a infraestrutura de autenticação por sessão e a proteção das rotas privadas.

### Arquivos ou componentes envolvidos

- `config/SecurityConfig` e configuração CORS;
- Spring Security e Spring Session JDBC;
- tabelas de sessão;
- testes de segurança.

### Dependências

- T02, T03 e T05.

### Passos de implementação

- Configurar sessão opaca em cookie `HttpOnly` e `SameSite`.
- Configurar `Secure` conforme o perfil HTTP/HTTPS.
- Ativar CSRF para alterações de estado.
- Restringir CORS à origem local configurada.
- Definir rotas públicas de cadastro, login e reativação e proteger as demais.

### Testes necessários

- Acessar rota privada sem sessão.
- Testar alteração sem token CSRF.
- Testar origem CORS permitida e rejeitada.
- Verificar atributos do cookie por perfil.

### Concluída quando

- Rotas privadas rejeitarem usuário não autenticado.
- Requisições mutáveis sem CSRF forem rejeitadas.
- Sessões forem persistidas no banco configurado.

### Requisitos e specs relacionados

- RNF08–RNF11;
- spec `cadastro-autenticacao-sessoes.md`.

## T07 — Implementar cadastro de conta e saldo inicial

### Objetivo

Criar uma conta ativa válida com senha protegida, saldo inicial e primeiro registro patrimonial.

### Arquivos ou componentes envolvidos

- `api/controller` de conta;
- `service` de conta;
- modelos e repositories de conta, movimentação e patrimônio.

### Dependências

- T03–T06.

### Passos de implementação

- Validar nome, CPF, e-mail e composição da senha.
- Aplicar unicidade entre contas ativas.
- Armazenar somente hash bcrypt.
- Criar saldo de R$ 10.000,00.
- Gravar saldo inicial e ponto patrimonial na mesma transação.

### Testes necessários

- Cadastro válido.
- CPF, e-mail e senha inválidos.
- CPF/e-mail duplicados em conta ativa.
- Rollback se histórico ou patrimônio falhar.

### Concluída quando

- O cadastro válido produzir exatamente uma conta, saldo e registro inicial.
- Entrada inválida não persistir nenhum dado.
- A senha não puder ser obtida em texto legível.

### Requisitos e specs relacionados

- RF01–RF03;
- RN02–RN05;
- spec `cadastro-autenticacao-sessoes.md`, CA01 e CA02;
- spec `historico-registro-patrimonial.md`, CA01.

## T08 — Implementar login e logout

### Objetivo

Permitir entrada em conta ativa e encerramento da sessão atual.

### Arquivos ou componentes envolvidos

- controller e service de autenticação;
- configuração de segurança;
- persistência de sessão.

### Dependências

- T06 e T07.

### Passos de implementação

- Autenticar por e-mail e senha.
- Impedir login de conta inativa.
- Usar mensagem neutra para credencial inválida.
- Criar sessão após sucesso.
- Invalidar a sessão atual no logout.

### Testes necessários

- Login válido, senha incorreta, e-mail inexistente e conta inativa.
- Acesso privado antes e depois do logout.
- Ausência de enumeração de conta na mensagem.

### Concluída quando

- Login válido criar sessão funcional.
- Tentativas inválidas não criarem sessão.
- Logout impedir reutilização da sessão.

### Requisitos e specs relacionados

- RF04 e RF05;
- CE04;
- spec `cadastro-autenticacao-sessoes.md`, CA03 e CA04.

## T09 — Implementar consulta e alteração de credenciais

### Objetivo

Exibir dados mascarados e permitir alteração de e-mail ou senha com revogação total de sessões.

### Arquivos ou componentes envolvidos

- controller/service de conta;
- repository de conta e sessões;
- mascaramento de CPF/e-mail.

### Dependências

- T08.

### Passos de implementação

- Criar consulta da conta autenticada.
- Mascarar CPF e e-mail na resposta de exibição.
- Confirmar senha atual antes da alteração.
- Validar novo e-mail ou nova senha.
- Revogar todas as sessões após sucesso.

### Testes necessários

- Exibição mascarada.
- Senha atual correta e incorreta.
- Novo e-mail inválido ou duplicado.
- Revogação de duas sessões simultâneas.

### Concluída quando

- Nome e CPF não puderem ser alterados.
- Alteração válida persistir e encerrar todas as sessões.
- Dados de outra conta não puderem ser acessados.

### Requisitos e specs relacionados

- RF06–RF09;
- RN03–RN05;
- spec `cadastro-autenticacao-sessoes.md`, CA05 e CA06.

## T10 — Implementar exclusão lógica e reativação

### Objetivo

Completar o ciclo de vida da conta sem apagar dados históricos.

### Arquivos ou componentes envolvidos

- controller/service de conta;
- repositories de conta e sessão;
- fluxos públicos de reativação e novo cadastro.

### Dependências

- T09.

### Passos de implementação

- Exigir e-mail atual, senha atual e palavra exata `Excluir` para inativação.
- Revogar sessões ao inativar.
- Preservar saldo, posições, corretoras e histórico.
- Oferecer reativação sem comprovação adicional, conforme decisão da primeira versão.
- Permitir nova conta com CPF/e-mail da conta inativa, mantendo a anterior inacessível.

### Testes necessários

- Exclusão válida e confirmações inválidas.
- Login da conta inativa.
- Reativação com dados preservados.
- Nova conta reutilizando e-mail de conta inativa.

### Concluída quando

- Nenhuma exclusão física ocorrer.
- Sessões forem revogadas na inativação.
- Os cinco critérios da spec passarem.

### Requisitos e specs relacionados

- RF10–RF14;
- RN30;
- spec `exclusao-reativacao-conta.md`.

## T11 — Criar fundação do frontend

### Objetivo

Preparar navegação, cliente HTTP e componentes comuns sem implementar regras de domínio.

### Arquivos ou componentes envolvidos

- `src/main/front/App.jsx` e `main.jsx`;
- `components/`, `pages/`, `services/` e `styles/`.

### Dependências

- T01 e contratos de erro da T05.

### Passos de implementação

- Configurar rotas públicas e privadas.
- Criar cliente da API com cookie e CSRF.
- Criar layout, navegação, carregamento, estado vazio e mensagens.
- Criar formatação comum de moeda, data e hora.
- Impedir envios duplicados enquanto houver solicitação em andamento.

### Testes necessários

- Renderização de rotas.
- Estados de carregamento, vazio e erro.
- Inclusão de credenciais e CSRF nas requisições.
- Formatação de valores e horário.

### Concluída quando

- Uma página pública e uma privada puderem usar o mesmo cliente e layout.
- Estados comuns estiverem cobertos por testes de componentes.
- Não houver cálculo financeiro oficial no frontend.

### Requisitos e specs relacionados

- RNF05, RNF06, RNF14 e RNF15;
- spec `interface-estados.md`, CA01, CA02 e CA05–CA07.

## T12 — Criar telas de cadastro, login e conta

### Objetivo

Disponibilizar na interface os fluxos de identidade já implementados.

### Arquivos ou componentes envolvidos

- páginas e componentes de cadastro, login e conta;
- serviço frontend de autenticação.

### Dependências

- T08–T11.

### Passos de implementação

- Criar formulários de cadastro e login.
- Exibir validações por campo e mensagem neutra de login.
- Criar área da conta com dados mascarados.
- Implementar alteração de e-mail/senha e redirecionamento após revogação.
- Implementar logout.

### Testes necessários

- Validação dos formulários.
- Sucesso e erro com API simulada.
- Redirecionamento de rota privada.
- Exibição parcial de CPF/e-mail.

### Concluída quando

- Cadastro, login, consulta, alteração e logout funcionarem pela interface.
- Nenhum dado privado aparecer sem sessão.
- Os critérios correspondentes das specs passarem no frontend.

### Requisitos e specs relacionados

- RF01–RF09;
- HU01–HU03;
- specs `cadastro-autenticacao-sessoes.md` e `interface-estados.md`.

## T13 — Criar telas de exclusão e reativação

### Objetivo

Completar na interface o ciclo de inativação, reativação e criação alternativa de conta.

### Arquivos ou componentes envolvidos

- página de configurações da conta;
- página pública de reativação;
- serviço frontend de conta.

### Dependências

- T10–T12.

### Passos de implementação

- Criar confirmação com e-mail, senha e `Excluir`.
- Exibir as opções de reativar ou criar nova conta quando aplicável.
- Encerrar estado local após exclusão.
- Informar claramente que a conta anterior permanece inacessível ao criar outra.

### Testes necessários

- Confirmações corretas e incorretas.
- Reativação e nova conta com API simulada.
- Redirecionamento após inativação.

### Concluída quando

- Todos os fluxos da spec estiverem acessíveis e compreensíveis.
- A interface não alegar que reativação possui verificação de identidade.
- Os cinco critérios da spec passarem.

### Requisitos e specs relacionados

- RF10–RF14;
- HU04;
- specs `exclusao-reativacao-conta.md` e `interface-estados.md`.

## T14 — Implementar infraestrutura de histórico e patrimônio

### Objetivo

Criar o mecanismo interno imutável usado por todas as movimentações para registrar histórico e pontos patrimoniais.

### Arquivos ou componentes envolvidos

- modelos e repositories de movimentação e ponto patrimonial;
- service interno de registro;
- cálculo de patrimônio.

### Dependências

- T03, T04 e T07.

### Passos de implementação

- Representar os cinco tipos de movimentação.
- Exigir somente os campos aplicáveis a cada tipo.
- Criar registro e ponto patrimonial dentro da transação chamadora.
- Impedir operações de edição e exclusão.
- Garantir que atualização isolada de cotação não invoque o registro.

### Testes necessários

- Registro de cada tipo com seus campos.
- Rejeição de registro incompleto.
- Rollback junto à operação chamadora.
- Ausência de ponto para atualização de cotação.

### Concluída quando

- O serviço interno registrar movimentação e patrimônio atomicamente.
- Não existir endpoint de alteração ou exclusão.
- CA01–CA03 e CA06 da spec passarem.

### Requisitos e specs relacionados

- RF60–RF62;
- RN24, RN29 e RN30;
- spec `historico-registro-patrimonial.md`.

## T15 — Implementar saldo e aportes

### Objetivo

Permitir consulta do saldo único da conta e aportes fictícios transacionais.

### Arquivos ou componentes envolvidos

- controller/service de carteira;
- repository de conta;
- histórico e patrimônio.

### Dependências

- T09 e T14.

### Passos de implementação

- Criar consulta do saldo da conta autenticada.
- Validar aporte mínimo de R$ 10,00.
- Somar o valor ao saldo.
- Registrar aporte e ponto patrimonial na mesma transação.
- Excluir aporte dos cálculos de lucro e valorização.

### Testes necessários

- Saldo inicial.
- Aporte mínimo, acima do mínimo e inválido.
- Aporte não contabilizado como lucro.
- Rollback integral em falha de registro.

### Concluída quando

- Saldo e histórico permanecerem consistentes.
- Entrada inválida não alterar dados.
- Os cinco critérios da spec passarem.

### Requisitos e specs relacionados

- RF15–RF18;
- RN01, RN06 e RN07;
- spec `saldo-aportes.md`.

## T16 — Criar interface de saldo e aporte

### Objetivo

Exibir o saldo compartilhado e permitir aporte pela interface.

### Arquivos ou componentes envolvidos

- página ou componente de carteira;
- formulário de aporte;
- serviço frontend de carteira.

### Dependências

- T11, T12 e T15.

### Passos de implementação

- Exibir saldo com duas casas decimais.
- Criar formulário com validação do mínimo.
- Solicitar confirmação simples na mesma etapa.
- Atualizar saldo após sucesso e apresentar erro sem duplicar envio.

### Testes necessários

- Renderização do saldo.
- Aporte válido e inválido com API simulada.
- Estado durante envio e mensagem de erro.

### Concluída quando

- Aporte puder ser realizado de ponta a ponta pela interface.
- Saldo refletir o valor retornado pelo backend.
- A interface não classificar aporte como lucro.

### Requisitos e specs relacionados

- RF15–RF18;
- HU05;
- specs `saldo-aportes.md` e `interface-estados.md`, CA03.

## T17 — Validar fontes de CNPJ, CEP e CVM

### Objetivo

Realizar a prova técnica das fontes de corretora antes de implementar o cadastro definitivo.

### Arquivos ou componentes envolvidos

- protótipos descartáveis ou testes de contrato em `infra/client`;
- documentação da integração;
- configuração local de endpoints.

### Dependências

- T02.

### Passos de implementação

- Confirmar campos usados da Brasil API e ViaCEP.
- Identificar fonte oficial processável da CVM.
- Demonstrar como localizar CNPJ e categoria `CTVM` sem automação de página ou CAPTCHA.
- Registrar formatos, limites, falhas e dados obrigatórios.
- Não criar cadastro funcional nesta tarefa.

### Testes necessários

- Consultar ao menos um CNPJ CTVM conhecido.
- Consultar um CNPJ que não atenda à regra.
- Exercitar resposta incompleta e indisponibilidade de forma controlada.

### Concluída quando

- A fonte/formato CVM estiver identificado e reproduzível.
- Os campos necessários puderem ser mapeados.
- As limitações estiverem documentadas.

### Requisitos e specs relacionados

- RF19–RF22;
- RN08;
- RNF17 e RNF19;
- spec `corretoras.md`.

## T18 — Implementar adapters de CNPJ, CEP e CVM

### Objetivo

Isolar as três fontes externas atrás de portas internas testáveis.

### Arquivos ou componentes envolvidos

- `domain/port`;
- `infra/client` e `infra/client/dto`;
- `infra/adapter`.

### Dependências

- T05 e T17.

### Passos de implementação

- Definir contratos internos mínimos.
- Criar clientes HTTP e DTOs próprios por fonte.
- Converter respostas para modelo interno de corretora.
- Configurar timeout.
- Mapear resposta incompleta, `429`, `5xx` e indisponibilidade.

### Testes necessários

- Adapter com resposta válida.
- Campo obrigatório ausente.
- Timeout, `429` e `5xx` simulados.
- Garantir que DTO externo não chegue ao service.

### Concluída quando

- Services dependerem somente das portas internas.
- Todos os cenários externos forem reproduzíveis sem internet nos testes.
- Erros forem convertidos para categorias padronizadas.

### Requisitos e specs relacionados

- RF20 e RF21;
- RNF02, RNF17 e RNF19;
- CE08 e CE22;
- spec `corretoras.md`.

## T19 — Implementar cadastro e administração de corretoras

### Objetivo

Permitir pesquisar, associar, remover e recadastrar corretoras válidas na conta.

### Arquivos ou componentes envolvidos

- controller/service de corretora;
- modelos e repositories de corretora e associação;
- adapters da T18.

### Dependências

- T09, T14 e T18.

### Passos de implementação

- Pesquisar exclusivamente por CNPJ.
- Consolidar cadastro, endereço e categoria CVM.
- Permitir associação apenas para CNPJ ativo e CTVM.
- Impedir duplicidade ativa por conta.
- Implementar remoção lógica sem posição e recadastro.
- Atualizar nome/endereço sem apagar valor anterior por resposta incompleta.

### Testes necessários

- CTVM válida e instituição não autorizada.
- Duplicidade.
- Remoção com e sem posição.
- Recadastro e preservação histórica.
- Isolamento entre contas.

### Concluída quando

- Os seis critérios da spec passarem.
- Somente corretoras ativas aparecerem para operações.
- Nenhum dado de outra conta for exposto.

### Requisitos e specs relacionados

- RF19–RF28;
- RN08–RN10 e RN30;
- spec `corretoras.md`.

## T20 — Criar interface de corretoras

### Objetivo

Disponibilizar pesquisa por CNPJ, associação, listagem e remoção na interface.

### Arquivos ou componentes envolvidos

- página de corretoras;
- componentes de pesquisa e resultado;
- serviço frontend de corretoras.

### Dependências

- T11, T12 e T19.

### Passos de implementação

- Criar busca exclusiva por CNPJ.
- Exibir os campos consolidados antes da associação.
- Listar corretoras ativas.
- Criar ação de remoção com mensagens de bloqueio.
- Exibir estados de indisponibilidade externa.

### Testes necessários

- Pesquisa válida e inválida com API simulada.
- Associação, duplicidade e remoção bloqueada.
- Estado vazio e carregamento.

### Concluída quando

- Os fluxos principais da spec funcionarem pela interface.
- Apenas corretoras ativas forem apresentadas para uso.
- Erros externos forem compreensíveis.

### Requisitos e specs relacionados

- RF19–RF28;
- HU06;
- specs `corretoras.md` e `interface-estados.md`.

## T21 — Validar Brapi, Twelve Data e AwesomeAPI

### Objetivo

Comprovar campos, cobertura e limites antes de construir a integração definitiva de mercado.

### Arquivos ou componentes envolvidos

- testes de contrato ou protótipos em `infra/client`;
- documentação das APIs;
- configuração local de credenciais.

### Dependências

- T02.

### Passos de implementação

- Validar ticker, nome, mercado, moeda, cotação e horário na Brapi.
- Validar esses campos e cobertura de ativos norte-americanos na Twelve Data.
- Validar endpoint e campos USD/BRL da AwesomeAPI.
- Medir consumo necessário para ciclo brasileiro e diário norte-americano.
- Documentar limites e exemplos sem salvar credenciais.

### Testes necessários

- Um ativo brasileiro válido e um mercado rejeitado.
- Um ativo norte-americano válido.
- Uma cotação USD/BRL válida.
- Respostas sem campo obrigatório e limite de uso simulados.

### Concluída quando

- Cada provedor tiver um mapeamento mínimo confirmado.
- A cobertura da Twelve Data estiver aceita ou o bloqueio documentado.
- Nenhuma integração definitiva depender de suposição de campo.

### Requisitos e specs relacionados

- RF29–RF42;
- RNF16, RNF17 e RNF19;
- spec `ativos-cotacoes-cambio.md`.

## T22 — Implementar adapters de mercado e câmbio

### Objetivo

Criar portas e adapters substituíveis para Brapi, Twelve Data e AwesomeAPI.

### Arquivos ou componentes envolvidos

- `domain/port`;
- `infra/client`, `infra/client/dto` e `infra/adapter`;
- modelos internos de ativo, cotação e câmbio.

### Dependências

- T04, T05 e T21.

### Passos de implementação

- Definir contratos internos de pesquisa, cotação e câmbio.
- Criar um cliente e DTOs por provedor.
- Validar campos obrigatórios e mercados aceitos.
- Normalizar moeda, mercado, preço e horário.
- Mapear timeout, `429`, `5xx` e resposta incompleta.

### Testes necessários

- Sucesso de cada adapter.
- Mercado rejeitado e campo ausente.
- Timeout, `429` e `5xx`.
- Arredondamento de valores externos.

### Concluída quando

- O domínio não conhecer DTOs externos.
- Os adapters puderem ser testados sem internet.
- Todos os erros seguirem o formato comum.

### Requisitos e specs relacionados

- RF29–RF33;
- RN27 e RN28;
- RNF02, RNF17 e RNF19;
- spec `ativos-cotacoes-cambio.md`.

## T23 — Implementar catálogo, cache e pesquisa de ativos

### Objetivo

Persistir dados válidos de mercado, pesquisar ativos e aplicar fallback de cotação/câmbio.

### Arquivos ou componentes envolvidos

- controller/service de ativos;
- repositories de ativo, cotação e câmbio;
- adapters da T22.

### Dependências

- T03, T09 e T22.

### Passos de implementação

- Pesquisar ticker brasileiro no provedor atual.
- Usar cotação diária armazenada para ativo norte-americano.
- Persistir somente respostas completas e válidas.
- Preservar valor anterior quando a consulta falhar.
- Calcular idade e marcar cotação acima de 24 horas ou câmbio acima de sete dias.
- Bloquear dependência financeira quando não houver valor utilizável.

### Testes necessários

- Pesquisa brasileira e exibição norte-americana em USD/BRL.
- Ativo/mercado rejeitado.
- Cache existente e inexistente em falha.
- Avisos de idade no limite e após o limite.
- Preservação de dados em resposta incompleta.

### Concluída quando

- CA01, CA02 e CA05–CA08 da spec passarem.
- O horário do dado usado for retornado.
- O frontend não puder impor cotação.

### Requisitos e specs relacionados

- RF29–RF34 e RF36–RF42;
- RN15, RN27 e RN28;
- spec `ativos-cotacoes-cambio.md`.

## T24 — Implementar agendamentos de mercado

### Objetivo

Executar os ciclos automáticos confirmados sem sobreposição e sem criar pontos patrimoniais.

### Arquivos ou componentes envolvidos

- `scheduler/`;
- services e repositories de cotação/câmbio;
- adapters da T22.

### Dependências

- T23.

### Passos de implementação

- Atualizar ativos brasileiros em posições a cada cinco minutos.
- Atualizar ativos norte-americanos e USD/BRL às 10h de Brasília, uma vez ao dia.
- Evitar execução simultânea do mesmo ciclo.
- Manter cache anterior após falha.
- Não gravar movimentação ou ponto patrimonial.

### Testes necessários

- Relógio controlado para cada frequência.
- Seleção somente de ativos em posições.
- Prevenção de sobreposição.
- Falha externa com preservação do cache.
- Ausência de histórico/ponto após atualização.

### Concluída quando

- CA03, CA04 e CA06 da spec de mercado passarem.
- O ciclo diário executar no máximo uma vez por dia.
- Nenhuma atualização isolada alterar patrimônio histórico.

### Requisitos e specs relacionados

- RF35 e RF40;
- RNF16 e RNF17;
- specs `ativos-cotacoes-cambio.md` e `historico-registro-patrimonial.md`, CA06.

## T25 — Criar interface de pesquisa de ativos

### Objetivo

Permitir pesquisar ativos e visualizar valores, moedas, horários e avisos.

### Arquivos ou componentes envolvidos

- página de ativos;
- componentes de resultado e aviso;
- serviço frontend de mercado.

### Dependências

- T11, T12 e T23.

### Passos de implementação

- Criar pesquisa por ticker.
- Exibir ticker, nome, mercado, moeda, cotação e horário.
- Exibir USD e BRL para ativo norte-americano.
- Exibir avisos de cotação/câmbio antigo.
- Tratar ativo inválido, ausência de cache e indisponibilidade.

### Testes necessários

- Resultados brasileiro e norte-americano.
- Estado de carregamento e vazio.
- Avisos nos limites de idade.
- Erros de campo ausente e mercado rejeitado.

### Concluída quando

- A pesquisa apresentar exatamente os campos obrigatórios.
- Dados antigos estiverem claramente identificados.
- Nenhuma opção de atualização manual existir.

### Requisitos e specs relacionados

- RF29–RF42;
- HU07 e HU14;
- specs `ativos-cotacoes-cambio.md` e `interface-estados.md`, CA08.

## T26 — Implementar regras de posição e resultado

### Objetivo

Criar e testar isoladamente os cálculos financeiros usados por compra, venda, transferência e dashboards.

### Arquivos ou componentes envolvidos

- modelos/serviços de domínio financeiro;
- testes unitários sem banco.

### Dependências

- T04.

### Passos de implementação

- Calcular custo e preço médio ponderado de compra.
- Manter preço médio na venda parcial.
- Calcular resultado realizado.
- Zerar posição e reiniciar média na recompra.
- Calcular custo transferido e média no destino.
- Calcular patrimônio, valorização e resultado total sem considerar aporte como lucro.

### Testes necessários

- Exemplos numéricos de todas as specs financeiras.
- Venda parcial, total e recompra.
- Transferência para destino vazio e existente.
- Ativo USD convertido em BRL.
- Casos de arredondamento.

### Concluída quando

- Todos os cálculos forem determinísticos e independentes do frontend.
- Os exemplos das specs produzirem os valores esperados.
- Nenhuma regra financeira depender de repository ou HTTP.

### Requisitos e specs relacionados

- RN11–RN28;
- specs `compra-venda-posicoes.md`, `transferencia-posicoes.md` e `dashboards.md`.

## T27 — Implementar compra de ativos

### Objetivo

Executar compra simulada usando preço do backend e atualização atômica da carteira.

### Arquivos ou componentes envolvidos

- controller/service de carteira;
- repositories de conta, corretora, ativo e posição;
- histórico e mercado.

### Dependências

- T14, T19, T23 e T26.

### Passos de implementação

- Validar corretora ativa, ativo e quantidade inteira positiva.
- Obter cotação/câmbio utilizável antes da transação.
- Revalidar saldo e propriedade dentro da transação.
- Debitar saldo e criar/atualizar posição.
- Gravar compra e ponto patrimonial.
- Ignorar preço livre enviado pelo cliente.

### Testes necessários

- Primeira compra e média ponderada.
- Compra brasileira e norte-americana.
- Saldo insuficiente com valores na mensagem.
- Cotação ausente e cotação antiga.
- Preço manipulado pelo cliente.
- Rollback integral.

### Concluída quando

- CA01–CA03 e CA06–CA08 da spec de compra/venda passarem para compra.
- Saldo nunca ficar negativo.
- Saldo, posição, histórico e patrimônio permanecerem consistentes.

### Requisitos e specs relacionados

- RF43–RF48;
- RN12–RN18, RN27–RN29;
- spec `compra-venda-posicoes.md`.

## T28 — Implementar venda de ativos

### Objetivo

Executar venda parcial ou total com resultado realizado e preservação correta da posição.

### Arquivos ou componentes envolvidos

- controller/service de carteira;
- repositories de conta e posição;
- histórico e mercado.

### Dependências

- T27.

### Passos de implementação

- Validar posição na corretora e quantidade inteira positiva.
- Obter cotação utilizável definida pelo backend.
- Revalidar quantidade dentro da transação.
- Creditar saldo e reduzir ou zerar a posição.
- Manter preço médio na venda parcial e calcular resultado realizado.
- Gravar venda e ponto patrimonial.

### Testes necessários

- Venda parcial, total e acima da posição.
- Recompra após posição zerada.
- Venda por cache quando ativo deixa de ser retornado.
- Venda norte-americana convertida.
- Rollback integral.

### Concluída quando

- CA04, CA05, CA07 e CA08 da spec passarem para venda.
- Posição zerada não aparecer como aberta.
- Histórico anterior permanecer preservado.

### Requisitos e specs relacionados

- RF49–RF54;
- RN15, RN17 e RN19–RN21;
- spec `compra-venda-posicoes.md`.

## T29 — Criar interface de compra e venda

### Objetivo

Permitir operações simuladas pela interface com confirmação simples e mensagens de disponibilidade.

### Arquivos ou componentes envolvidos

- páginas/componentes de operação e posição;
- serviço frontend de carteira;
- componentes de confirmação e aviso.

### Dependências

- T16, T20, T25, T27 e T28.

### Passos de implementação

- Selecionar corretora ativa, ticker, operação e quantidade.
- Exibir a cotação e aviso aplicáveis.
- Solicitar confirmação na mesma etapa, sem página de resumo.
- Impedir novo envio enquanto a operação estiver em andamento.
- Atualizar saldo e posição após sucesso.
- Exibir valores solicitado/disponível nos erros correspondentes.

### Testes necessários

- Compra e venda válidas com API simulada.
- Quantidade inválida, saldo e posição insuficientes.
- Confirmação cancelada.
- Cotação/câmbio desatualizado.

### Concluída quando

- Compra e venda funcionarem de ponta a ponta.
- A interface não enviar preço como fonte financeira.
- Não existir edição, cancelamento posterior ou estorno.

### Requisitos e specs relacionados

- RF43–RF54;
- HU08, HU09 e HU14;
- specs `compra-venda-posicoes.md` e `interface-estados.md`, CA03.

## T30 — Implementar transferência de posições

### Objetivo

Transferir posição entre corretoras da conta sem alterar saldo e preservando custo.

### Arquivos ou componentes envolvidos

- controller/service de carteira;
- repository de posição;
- histórico e patrimônio.

### Dependências

- T19, T26 e T28.

### Passos de implementação

- Validar origem e destino ativos e distintos.
- Validar quantidade inteira disponível.
- Reduzir origem e criar/atualizar destino na mesma transação.
- Recalcular média ponderada no destino quando necessário.
- Manter saldo inalterado.
- Registrar origem, destino e ponto patrimonial.

### Testes necessários

- Destino vazio e com posição.
- Transferência parcial e total.
- Quantidade excessiva e mesma corretora.
- Corretora de outra conta.
- Rollback integral.

### Concluída quando

- Os cinco critérios da spec passarem.
- O custo total entre origem e destino for preservado.
- O saldo não mudar.

### Requisitos e specs relacionados

- RF55–RF59;
- RN17, RN22, RN23, RN28 e RN29;
- spec `transferencia-posicoes.md`.

## T31 — Criar interface de transferência

### Objetivo

Permitir transferência total ou parcial pela interface.

### Arquivos ou componentes envolvidos

- componente/página de transferência;
- serviço frontend de carteira;
- seletores de corretora e posição.

### Dependências

- T20, T29 e T30.

### Passos de implementação

- Listar apenas corretoras ativas da conta.
- Preencher origem a partir da posição escolhida.
- Impedir destino igual à origem.
- Validar quantidade e solicitar confirmação simples.
- Atualizar as posições após sucesso e exibir disponibilidade no erro.

### Testes necessários

- Transferência válida.
- Mesma corretora e quantidade excessiva.
- Confirmação cancelada e envio duplicado.

### Concluída quando

- Transferência puder ser concluída pela interface.
- Saldo exibido permanecer inalterado.
- Estados de erro forem compreensíveis.

### Requisitos e specs relacionados

- RF55–RF59;
- HU10;
- specs `transferencia-posicoes.md` e `interface-estados.md`, CA03.

## T32 — Implementar consulta paginada do histórico

### Objetivo

Expor o histórico imutável com filtros, ordenação e páginas de 20 registros.

### Arquivos ou componentes envolvidos

- controller/service de histórico;
- repository e projeções de movimentação.

### Dependências

- T14, T15, T27, T28 e T30.

### Passos de implementação

- Criar consulta da conta autenticada.
- Ordenar do mais recente para o mais antigo.
- Fixar 20 registros por página.
- Implementar filtros combináveis de intervalo, tipo, ticker, corretora e mercado.
- Retornar metadados de paginação.

### Testes necessários

- Mais de 20 registros e navegação de páginas.
- Cada filtro isolado e combinação de filtros.
- Ordem padrão.
- Isolamento entre contas.
- Ausência de endpoints de edição/exclusão.

### Concluída quando

- CA03–CA05 da spec passarem.
- Toda página possuir no máximo 20 registros.
- Somente movimentos concluídos da conta forem retornados.

### Requisitos e specs relacionados

- RF60–RF64;
- RN29 e RN30;
- spec `historico-registro-patrimonial.md`.

## T33 — Criar interface do histórico

### Objetivo

Exibir e filtrar o histórico em páginas de 20 registros.

### Arquivos ou componentes envolvidos

- página de histórico;
- componentes de filtros, tabela/lista e paginação;
- serviço frontend de histórico.

### Dependências

- T11, T20, T25 e T32.

### Passos de implementação

- Exibir as colunas aplicáveis por tipo de movimento.
- Criar filtros de intervalo, tipo, ticker, corretora e mercado.
- Manter filtros ao mudar de página.
- Mostrar registros mais recentes primeiro.
- Criar apresentação responsiva para telas menores.

### Testes necessários

- Renderização de tipos diferentes.
- Aplicação e combinação de filtros.
- Navegação entre páginas.
- Estado vazio e erro.
- Ausência de ações de edição/exclusão.

### Concluída quando

- Histórico puder ser consultado e filtrado integralmente.
- Cada página exibir no máximo 20 itens.
- A tela permanecer utilizável nos tamanhos definidos.

### Requisitos e specs relacionados

- RF60–RF64;
- HU11;
- specs `historico-registro-patrimonial.md` e `interface-estados.md`.

## T34 — Implementar indicadores do dashboard geral

### Objetivo

Calcular e expor os indicadores consolidados da conta usando uma única regra financeira.

### Arquivos ou componentes envolvidos

- controller/service de dashboard;
- repositories/projeções de conta, posição e movimentação;
- regras financeiras da T26.

### Dependências

- T23, T26, T28, T30 e T32.

### Passos de implementação

- Calcular saldo, patrimônio e posições.
- Calcular preço médio, realizado, não realizado e resultado total.
- Converter posições USD para BRL.
- Excluir aportes do rendimento.
- Retornar aviso e horário quando usar cotação/câmbio antigo.

### Testes necessários

- Conta somente com saldo.
- Carteira brasileira e mista.
- Aporte sem alteração do lucro.
- Resultados realizados e não realizados.
- Cache antigo e isolamento entre contas.

### Concluída quando

- CA01, CA02 e CA07 da spec de dashboards passarem.
- Os valores coincidirem com as regras da T26.
- Nenhum cálculo oficial for delegado ao frontend.

### Requisitos e specs relacionados

- RF65;
- RN24–RN28;
- spec `dashboards.md`.

## T35 — Implementar dashboard por corretora e distribuições

### Objetivo

Expor a visão de uma corretora e as distribuições por ativo, corretora e mercado.

### Arquivos ou componentes envolvidos

- service/controller de dashboard;
- consultas agregadas e projeções.

### Dependências

- T34.

### Passos de implementação

- Restringir posições e resultados à corretora selecionada.
- Manter o saldo identificado como compartilhado pela conta.
- Agregar valores em BRL por ativo, corretora e mercado.
- Retornar valor numérico de cada parcela.
- Validar propriedade da corretora.

### Testes necessários

- Duas corretoras com o mesmo ativo.
- Distribuições por cada dimensão.
- Corretora de outra conta.
- Soma das parcelas comparada ao total das posições.

### Concluída quando

- CA03 e CA04 da spec passarem.
- A visão específica não misturar posições de outra corretora.
- Os valores agregados forem reproduzíveis pelos dados de posição.

### Requisitos e specs relacionados

- RF66 e RF67;
- RN01, RN11 e RN24–RN27;
- spec `dashboards.md`.

## T36 — Implementar série histórica patrimonial

### Objetivo

Expor a evolução do patrimônio nos seis períodos definidos usando apenas pontos gravados após movimentações.

### Arquivos ou componentes envolvidos

- service/controller de dashboard;
- repository de ponto patrimonial.

### Dependências

- T32 e T34.

### Passos de implementação

- Implementar períodos de 4 semanas, 3 meses, 6 meses, 1 ano, 5 anos e máximo.
- Usar a criação da conta como limite do período máximo.
- Iniciar no primeiro ponto disponível quando não houver cobertura completa.
- Ordenar cronologicamente os pontos.
- Não inventar pontos entre movimentações.

### Testes necessários

- Cada período com relógio controlado.
- Período máximo.
- Cobertura parcial e conta com um único ponto.
- Ausência de pontos por atualização isolada de cotação.

### Concluída quando

- CA05 e CA06 da spec passarem.
- Todos os pontos retornados pertencerem ao período e à conta.
- Nenhum valor sintético não especificado for criado.

### Requisitos e specs relacionados

- RF68–RF70;
- spec `dashboards.md`;
- spec `historico-registro-patrimonial.md`, CA06.

## T37 — Criar interface dos dashboards

### Objetivo

Apresentar indicadores, distribuições e gráfico histórico no dashboard geral e por corretora.

### Arquivos ou componentes envolvidos

- página de dashboard;
- componentes de indicadores, gráficos, filtros e avisos;
- serviço frontend de dashboard.

### Dependências

- T11, T20 e T34–T36.

### Passos de implementação

- Exibir todos os indicadores definidos.
- Criar seleção de visão geral ou corretora.
- Criar gráficos de distribuição com valores numéricos.
- Criar gráfico histórico e seletor dos seis períodos.
- Exibir estados vazios e avisos de dados antigos.
- Adaptar visualização a desktop, tablet e celular.

### Testes necessários

- Dashboard com e sem posições.
- Troca de corretora e período.
- Renderização das três distribuições.
- Dados parcialmente cobertos e antigos.
- Testes de componentes responsivos e verificação manual.

### Concluída quando

- Os sete critérios da spec de dashboards puderem ser aceitos pela interface.
- Valores exibidos vierem da API, sem recálculo divergente.
- A tela não exigir rolagem horizontal da página.

### Requisitos e specs relacionados

- RF65–RF70;
- HU12–HU14;
- specs `dashboards.md` e `interface-estados.md`.

## T38 — Revisar responsividade e estados transversais

### Objetivo

Aplicar de forma consistente os critérios de interface a todas as páginas concluídas.

### Arquivos ou componentes envolvidos

- todos os componentes, páginas e estilos do frontend;
- mensagens e contratos de erro da API.

### Dependências

- T13, T16, T20, T25, T29, T31, T33 e T37.

### Passos de implementação

- Revisar carregamento, vazio, sucesso, erro e desatualização em cada tela.
- Revisar bloqueio de múltiplos envios.
- Confirmar máscaras, moedas e horários.
- Ajustar navegação e layout para desktop, tablet e celular.
- Confirmar redirecionamento após sessão revogada.

### Testes necessários

- Executar os oito critérios da spec de interface em cada fluxo aplicável.
- Testar navegação por teclado nos formulários principais.
- Realizar verificação manual nos três tamanhos definidos.

### Concluída quando

- Nenhuma tela funcional apresentar estado sem tratamento.
- Não houver rolagem horizontal da página nos tamanhos verificados.
- Mensagens técnicas ou dados sensíveis não aparecerem ao usuário.

### Requisitos e specs relacionados

- RNF05, RNF06, RNF12, RNF14 e RNF15;
- spec `interface-estados.md`.

## T39 — Criar testes integrados dos fluxos críticos

### Objetivo

Validar segurança, persistência e atomicidade no conjunto real de componentes.

### Arquivos ou componentes envolvidos

- testes Spring Boot;
- Testcontainers PostgreSQL;
- servidor HTTP simulado para APIs;
- Playwright para um fluxo principal.

### Dependências

- T24, T28, T30, T32 e T38.

### Passos de implementação

- Criar testes PostgreSQL de cadastro, aporte, compra, venda e transferência.
- Forçar falha intermediária e verificar rollback.
- Testar acesso cruzado entre duas contas em todos os recursos principais.
- Testar revogação de sessões e CSRF.
- Criar fluxo de interface: cadastro, login, corretora, aporte, compra e venda.

### Testes necessários

- A própria tarefa consiste na suíte integrada descrita.
- Executar também migrações em banco vazio.

### Concluída quando

- Todos os fluxos críticos passarem em PostgreSQL.
- Falhas não deixarem estado parcial.
- Usuário A não acessar nenhum recurso do usuário B.
- O fluxo Playwright principal passar de forma reproduzível.

### Requisitos e specs relacionados

- RNF07, RNF08 e RNF18;
- CE20 e CE21;
- critérios críticos de todas as specs.

## T40 — Documentar API e integrações

### Objetivo

Produzir os entregáveis acadêmicos de uso da API e das dependências externas.

### Arquivos ou componentes envolvidos

- documentação de endpoints;
- coleção Postman ou equivalente;
- README;
- documentação das APIs externas.

### Dependências

- T32, T35 e T36.

### Passos de implementação

- Documentar método, rota, autenticação, entrada, sucesso e erros de cada endpoint.
- Atualizar coleção de requisições.
- Documentar configuração local e variáveis sem segredos.
- Registrar campos, limites e fallback de Brasil API, ViaCEP, CVM, Brapi, Twelve Data e AwesomeAPI.
- Explicar que operações são simulações.

### Testes necessários

- Executar a coleção contra ambiente local limpo.
- Seguir o README em uma configuração nova.
- Verificar que nenhum segredo aparece nos arquivos.

### Concluída quando

- Todos os endpoints implementados estiverem documentados.
- A coleção reproduzir os fluxos principais.
- Uma pessoa do grupo conseguir iniciar o projeto apenas com o README.

### Requisitos e specs relacionados

- RNF19 e RNF20;
- restrições acadêmicas de `docs/01,1-trabalho.md`.

## T41 — Executar validação final e preparar demonstração

### Objetivo

Confirmar que a primeira versão atende aos documentos aprovados e pode ser demonstrada localmente.

### Arquivos ou componentes envolvidos

- aplicação completa;
- testes;
- documentação;
- roteiro de demonstração.

### Dependências

- T39 e T40.

### Passos de implementação

- Mapear RF01–RF70 para teste ou procedimento de aceitação.
- Executar build e suítes do backend e frontend.
- Executar todos os critérios Dado/Quando/Então das dez specs.
- Verificar funcionalidades explicitamente fora do escopo.
- Preparar base e roteiro reproduzíveis para demonstração local.
- Registrar limitações conhecidas, incluindo reativação sem comprovação de identidade.

### Testes necessários

- Suíte completa automatizada.
- Testes reais controlados das integrações.
- Teste manual responsivo.
- Ensaio do roteiro de demonstração em ambiente limpo.

### Concluída quando

- Todos os requisitos tiverem evidência de atendimento ou limitação registrada.
- Builds e testes passarem.
- A demonstração puder ser repetida sem ajuste manual do banco.
- Nenhuma funcionalidade fora do escopo for necessária para concluir o roteiro.

### Requisitos e specs relacionados

- RF01–RF70;
- RNF01–RNF22;
- RN01–RN30;
- todas as specs de `docs/spec/`;
- entregáveis de `docs/01,1-trabalho.md`.

## 2. Resumo da ordem de execução

| Faixa | Entrega |
|---|---|
| T01–T06 | Base, banco, padrões e segurança |
| T07–T13 | Conta, autenticação e respectivas telas |
| T14–T16 | Histórico interno, saldo e aportes |
| T17–T20 | Corretoras e suas integrações |
| T21–T25 | Ativos, cotações, câmbio, cache e agendamentos |
| T26–T31 | Regras financeiras, compra, venda e transferência |
| T32–T33 | Histórico consultável |
| T34–T37 | Dashboards e gráficos |
| T38–T41 | Qualidade transversal, testes, documentação e demonstração |

## 3. Regras para execução das tarefas

- Não agrupar tarefas apenas para reduzir o número de entregas.
- Cada tarefa deve ser revisada antes de iniciar outra que dependa dela.
- Testes definidos na tarefa fazem parte da implementação, não são trabalho opcional posterior.
- Alterações de requisito ou spec devem atualizar primeiro os documentos e `docs/continuidade.md`.
- Uma tarefa não está concluída apenas porque compila; todos os critérios objetivos indicados devem ser atendidos.
- Provas técnicas T17 e T21 devem ocorrer antes das integrações definitivas para impedir implementação baseada em suposições.
