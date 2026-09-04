## 1. Dados e composição da transferência

- [x] 1.1 Integrar a leitura autenticada de posições abertas e corretoras próprias ativas ao fluxo privado e verificar por testes de serviço/componente os estados de carregamento, lista, vazio e erro recuperável.
- [x] 1.2 Criar o formulário de transferência com origem derivada da posição, destino distinto, quantidade inteira positiva e payload mínimo; verificar por testes que entradas inválidas não chamam a API e que dados financeiros não são enviados pelo cliente.

## 2. Confirmação e estados da operação

- [x] 2.1 Integrar confirmação simples, cancelamento preservando contexto e bloqueio de reenvio durante o envio; verificar por testes que apenas uma solicitação é feita e que o cancelamento não altera o estado.
- [x] 2.2 Tratar sucesso, erros funcionais, resposta 401 e falhas técnicas conforme os estados existentes, atualizando posições somente pela resposta/nova leitura e mantendo o saldo inalterado; verificar todos os cenários com API simulada.

## 3. Integração visual e validação final

- [x] 3.1 Adicionar a rota/navegação privada e a apresentação responsiva da transferência, verificando uso em 320 px, tablet e desktop sem rolagem horizontal nem perda de foco acessível.
- [x] 3.2 Executar a suíte frontend focada e completa, ESLint, build Vite, `git diff --check` e `openspec validate implementar-t31-interface-transferencia --strict`, corrigindo regressões antes de concluir o change.
