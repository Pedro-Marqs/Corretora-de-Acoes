## 1. Contrato e resolução financeira

- [ ] 1.1 Estender o contrato autenticado de carteira para receber ativo, corretora e quantidade de venda, rejeitando entradas ausentes, não inteiras ou não positivas; verificar com testes HTTP e do contrato de erro.
- [ ] 1.2 Resolver cotação do ativo e USD/BRL quando aplicável antes da transação, usando somente valores atuais ou cache utilizável e preservando os instantes usados; verificar com testes de fallback, ausência de cotação/câmbio e ativo norte-americano.

## 2. Caso de uso transacional

- [ ] 2.1 Implementar a venda derivando a conta da sessão e revalidando conta ativa, corretora própria ativa, ativo e posição sob bloqueio transacional; verificar isolamento entre contas, corretora inválida e concorrência de quantidade.
- [x] 2.2 Aplicar o núcleo financeiro da T26 para reduzir custo pela média vigente, preservar a média na venda parcial, zerar a posição na venda total e calcular resultado realizado positivo ou negativo; verificar cenários unitários de venda parcial, total e recompra.
- [ ] 2.3 Creditar o saldo único pelo valor backend da operação, convertendo USD para BRL com `HALF_UP` quando necessário e sem aceitar preço do cliente; verificar saldo, conversão internacional e tentativa de preço manipulado.

## 3. Histórico, patrimônio e consistência

- [ ] 3.1 Registrar a movimentação de venda com os dados efetivamente usados, resultado realizado e conversão aplicável, e gerar ponto patrimonial com saldo e posições resultantes; verificar preservação do histórico anterior e ausência de registro para venda rejeitada.
- [x] 3.2 Garantir rollback integral quando falhar atualização de saldo, posição, movimentação ou patrimônio; verificar com testes transacionais que nenhum estado parcial permanece.

## 4. Validação da entrega

- [ ] 4.1 Executar a suíte focada de venda, testes de regressão de compra e compilação Maven, corrigindo falhas causadas pela mudança; verificar também `git diff --check`.
