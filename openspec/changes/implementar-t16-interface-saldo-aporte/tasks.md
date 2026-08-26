## 1. Integração frontend com a API de carteira

- [x] 1.1 Criar o módulo frontend de carteira para consultar saldo e realizar aporte reutilizando `api/http.js`, cookies de sessão, tratamento de erros existente e CSRF nas operações mutáveis; verificar com testes do módulo que a consulta usa o endpoint da T15, o aporte envia o valor esperado com CSRF e respostas de erro são convertidas para o formato utilizado pela interface.

## 2. Página de carteira e consulta de saldo

- [x] 2.1 Criar a página de carteira e integrá-la à rota e navegação privadas existentes, carregando o saldo oficial da API e exibindo-o com o formatador BRL compartilhado; verificar com testes de componente que carregamento, saldo retornado, erro com nova tentativa e sessão inválida produzem os estados e a navegação esperados.

## 3. Fluxo de aporte e confirmação

- [x] 3.1 Implementar o formulário de aporte com validação básica de valor ausente, não numérico, zero, negativo ou inferior a R$ 10,00 e exigir confirmação simples na mesma tela antes do envio; verificar com testes que entradas inválidas não chegam à API, cancelar a confirmação não envia a operação e confirmar um valor válido dispara exatamente um aporte.

## 4. Estados da operação e atualização do saldo

- [x] 4.1 Integrar o envio do aporte aos estados de carregamento, sucesso e erro, impedir reenvios enquanto a solicitação estiver em andamento, preservar o contexto após falhas e atualizar o saldo somente a partir do resultado confirmado pelo backend ou de nova consulta; verificar com testes que cliques duplicados não geram aportes múltiplos, erros mantêm a operação recuperável e sucesso apresenta o novo saldo oficial.

## 5. Validação completa da T16

- [x] 5.1 Cobrir integralmente a T16 e a delta spec `interface-estados`, incluindo consulta de saldo, confirmação e cancelamento de aporte, prevenção de envio duplicado, tratamento de erro, sessão inválida, formatação monetária e comportamento responsivo; executar os testes focados, depois a suíte completa e o build do frontend, e finalizar com `openspec validate implementar-t16-interface-saldo-aporte --type change`, confirmando ausência de regressões.
