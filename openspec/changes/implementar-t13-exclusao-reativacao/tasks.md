## 1. Cliente de API de contas

- [x] 1.1 Adicionar ao módulo `src/main/front/api/accounts.js` a operação autenticada de exclusão de conta usando `DELETE /api/accounts/me`, reutilizando CSRF, `credentials: 'include'`, parsing e `AccountApiError`; verificar com teste do módulo ou teste de componente que payload, método e tratamento de erro estão corretos.
- [ ] 1.2 Adicionar ao módulo de contas a operação pública de consulta de reativação usando `POST /api/accounts/reactivation/check`; verificar que a resposta da API é retornada ao chamador e erros funcionais/técnicos seguem o padrão existente.
- [ ] 1.3 Adicionar ao módulo de contas a operação pública de reativação usando `POST /api/accounts/reactivation`; verificar que sucesso e respostas de erro são tratados corretamente.
- [ ] 1.4 Executar os testes existentes relacionados ao cliente HTTP e às operações de conta e verificar que nenhuma funcionalidade anterior foi quebrada.

## 2. Fluxo autenticado de exclusão

- [ ] 2.1 Adicionar à área de configurações da conta uma seção de exclusão que solicite e-mail atual, senha atual e a confirmação textual exata `Excluir`; verificar pela interface que os três campos são exigidos antes do envio.
- [ ] 2.2 Implementar validação de interface para impedir envio quando a confirmação não for exatamente `Excluir`, mantendo o backend como autoridade sobre credenciais e regras de negócio; verificar com teste de componente o caso correto e casos de confirmação inválida.
- [ ] 2.3 Integrar o formulário de exclusão à nova operação da API e apresentar erros de campo ou mensagem funcional retornados pelo backend; verificar com API simulada respostas de sucesso e falha.
- [ ] 2.4 Impedir submissões duplicadas durante a exclusão, desabilitando a ação enquanto a requisição estiver em andamento; verificar com teste que múltiplos acionamentos não geram múltiplas chamadas.
- [ ] 2.5 Após exclusão bem-sucedida, limpar imediatamente o contexto local de autenticação e redirecionar para uma rota pública; verificar que uma rota privada deixa de permanecer acessível após a operação.
- [ ] 2.6 Tratar perda inesperada de sessão durante o fluxo de exclusão seguindo o padrão já usado pela página de conta; verificar comportamento para resposta `401`.

## 3. Fluxo público de reativação

- [ ] 3.1 Criar uma página pública de reativação e registrar sua rota no roteamento existente sem exigir sessão autenticada; verificar que a página pode ser acessada diretamente quando o usuário está deslogado.
- [ ] 3.2 Criar o formulário inicial de reativação conforme o contrato já fornecido pelo backend e integrar a consulta de possibilidade de reativação; verificar com API simulada os resultados esperados da consulta.
- [ ] 3.3 Exibir claramente as alternativas retornadas pelo fluxo de reativação, incluindo a possibilidade de reativar a conta existente ou seguir para a criação de uma nova conta quando aplicável; verificar cada estado em teste de componente.
- [ ] 3.4 Implementar a confirmação da reativação chamando o endpoint correspondente e apresentar estado de sucesso ou erro; verificar reativação bem-sucedida e falha com API simulada.
- [ ] 3.5 Após reativação bem-sucedida, direcionar o usuário para o fluxo de login sem criar sessão automaticamente; verificar o redirecionamento e garantir que a rota privada ainda exija autenticação.
- [ ] 3.6 Impedir múltiplos envios simultâneos nos passos de consulta e reativação; verificar que cada ação gera no máximo uma requisição enquanto estiver pendente.

## 4. Alternativa de nova conta

- [ ] 4.1 Integrar a opção de criar uma nova conta à rota de cadastro já existente, sem duplicar o formulário de cadastro; verificar que a navegação chega ao fluxo existente.
- [ ] 4.2 Exibir antes da navegação uma mensagem clara de que a nova conta será independente e que a conta anterior permanecerá inacessível; verificar o texto e o comportamento em teste de interface.
- [ ] 4.3 Verificar que o cadastro existente continua funcionando normalmente quando acessado pelo fluxo padrão e pelo fluxo vindo da reativação.

## 5. Estados de interface e tratamento de erros

- [ ] 5.1 Garantir que exclusão e reativação apresentem estados distinguíveis de carregamento, erro e sucesso conforme os padrões existentes do frontend; verificar os estados por testes de componente.
- [ ] 5.2 Preservar os dados preenchidos quando ocorrer erro recuperável e permitir nova tentativa quando aplicável; verificar o comportamento com falha simulada de API.
- [ ] 5.3 Garantir que erros técnicos não exponham detalhes internos e utilizem as mensagens funcionais já padronizadas pelo cliente HTTP; verificar com respostas simuladas inválidas e falha de conexão.
- [ ] 5.4 Verificar que as novas telas permanecem utilizáveis em viewport estreito sem sobreposição ou rolagem horizontal causada pelos novos componentes.

## 6. Testes e validação da T13

- [ ] 6.1 Criar ou atualizar testes do fluxo de exclusão cobrindo confirmações corretas e incorretas, erros de API, prevenção de envio duplicado, limpeza da autenticação e redirecionamento após inativação; verificar que todos os testes passam.
- [ ] 6.2 Criar testes do fluxo de reativação cobrindo consulta, reativação, falhas, opção de nova conta e redirecionamentos; verificar que todos os testes passam.
- [ ] 6.3 Executar a suíte completa de testes do frontend e corrigir somente regressões causadas pela T13; verificar que não existem testes removidos ou ignorados para obter sucesso.
- [ ] 6.4 Executar lint do frontend e verificar ausência de novos erros.
- [ ] 6.5 Executar o build de produção do frontend e verificar conclusão sem erros.
- [ ] 6.6 Fazer uma verificação integrada manual dos fluxos de exclusão, reativação e criação alternativa de conta contra o backend local e confirmar que os critérios de conclusão da T13 em `docs/05-tarefas.md` foram atendidos.
