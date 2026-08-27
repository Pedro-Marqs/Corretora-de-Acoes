## 1. Domínio, persistência e invariantes de corretoras

- [x] 1.1 Evoluir `Broker`, `AccountBroker` e os repositories necessários para suportar criação e atualização segura de corretora, inativação e reativação da mesma associação histórica, busca por conta + corretora e bloqueio por posição aberta, além de criar migration Flyway com unicidade de `(account_id, broker_id)`; verificar com testes de domínio/repository que CNPJ continua único, uma conta não pode possuir duas associações da mesma corretora, associação inativa pode ser localizada para recadastro, posição com quantidade maior que zero é detectada e a migration é aplicada sem regressões.

## 2. Pesquisa e validação consolidada por CNPJ

- [x] 2.1 Implementar o caso de uso de pesquisa exclusivamente por CNPJ orquestrando `CompanyRegistryPort`, `PostalAddressPort` e `RegulatoryRegistryPort`, validando CNPJ ativo, endereço necessário e categoria `CTVM` e produzindo uma resposta consolidada sem criar associação automaticamente; verificar com testes usando doubles dos ports que CTVM válida retorna os dados esperados, CNPJ inativo e instituição não CTVM são rejeitados funcionalmente, indisponibilidade externa permanece distinta de reprovação regulatória e nenhuma alteração parcial é persistida quando a validação falha.

## 3. Associação, atualização cadastral e recadastro

- [x] 3.1 Implementar a confirmação de associação refazendo a validação externa pelo CNPJ, criando ou atualizando o `Broker` por merge campo a campo e criando, rejeitando ou reativando `AccountBroker` conforme seu estado; verificar com testes que primeira associação cria um único vínculo ativo, associação ativa duplicada é rejeitada, recadastro reutiliza a mesma associação histórica, dados válidos mais recentes são atualizados, campos ausentes não apagam valores anteriores e duas requisições concorrentes não resultam em duplicidade persistida.

## 4. Listagem, remoção lógica e isolamento entre contas

- [x] 4.1 Implementar listagem das corretoras ativas da conta autenticada e remoção lógica da associação, filtrando todas as operações pela conta da sessão e bloqueando a remoção somente quando existir posição com quantidade maior que zero naquela associação; verificar com testes que somente associações ativas da própria conta são retornadas, posição zerada permite remoção, posição positiva bloqueia remoção, associação removida deixa de aparecer para uso e tentativa de consultar ou alterar associação de outra conta não expõe nem modifica dados.

## 5. API e validação completa da T19

- [x] 5.1 Expor os endpoints autenticados necessários para pesquisar por CNPJ, confirmar associação, listar corretoras ativas e remover associação, integrando validação de entrada e tratamento centralizado de erros sem aceitar `accountId` ou dados institucionais do frontend como fonte autoritativa; verificar os seis critérios da spec `corretoras` e os cenários adicionais deste change com testes de service/API, executar os testes focados, depois `.\mvnw.cmd test` e finalizar com `openspec validate implementar-t19-cadastro-administracao-corretoras --type change`, confirmando que nenhuma interface React da T20 foi antecipada.
