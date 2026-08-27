## 1. Integração frontend e navegação privada

- [x] 1.1 Criar o módulo frontend de API de corretoras consumindo exatamente os endpoints implementados pela T19, reutilizando `api/http.js`, cookies de sessão, CSRF nas operações mutáveis, parsing e tratamento de erros existentes, e adicionar a página de corretoras à rota e navegação privadas; verificar com testes do módulo e de roteamento que pesquisa, associação, listagem e remoção usam os contratos reais da API, que respostas inválidas são rejeitadas adequadamente e que `/app/corretoras` permanece protegida pela autenticação existente.

## 2. Listagem das corretoras ativas

- [x] 2.1 Implementar o carregamento inicial e a apresentação das corretoras ativas da conta, reutilizando `LoadingState`, `EmptyState`, `ErrorState` e `Message`, sem exibir associações inativas; verificar com testes de componente que carregamento, lista vazia, lista preenchida, erro com nova tentativa e sessão expirada produzem os estados corretos sem apagar ou expor dados privados indevidamente.

## 3. Pesquisa por CNPJ e prévia da associação

- [x] 3.1 Implementar pesquisa exclusivamente por CNPJ com validação estrutural local de 14 dígitos, máscara visual quando conveniente e invalidação da prévia ao alterar o CNPJ após uma pesquisa, exibindo em modo somente leitura os dados consolidados retornados pela T19 antes da associação; verificar com testes que CNPJ inválido não chama a API, pesquisa válida apresenta a prévia, erros funcionais e indisponibilidade externa preservam a lista existente e nenhuma regra de CNPJ ativo ou categoria `CTVM` é reproduzida no frontend.

## 4. Associação, remoção e atualização autoritativa

- [x] 4.1 Implementar associação a partir da prévia pesquisada e remoção a partir das associações ativas, usando travas lógicas além de botões desabilitados para impedir reenvios, sem atualização otimista e recarregando a lista oficial após sucesso; verificar com testes que associação dispara somente uma requisição, duplicidade e falha preservam o contexto, remoção bloqueada por posição mantém a corretora visível com mensagem funcional, remoção válida atualiza a lista e cliques duplicados não geram operações múltiplas.

## 5. Validação completa da T20

- [x] 5.1 Cobrir integralmente os fluxos da T20 com API simulada, incluindo carregamento inicial, estado vazio, pesquisa válida e inválida, prévia, associação, duplicidade, indisponibilidade externa, remoção válida e bloqueada, prevenção de reenvio, sessão expirada e comportamento responsivo; executar os testes focados, depois a suíte frontend completa e o build, e finalizar com `openspec validate implementar-t20-interface-corretoras --type change`, confirmando que nenhuma regra de negócio, integração externa direta ou alteração backend foi introduzida.
