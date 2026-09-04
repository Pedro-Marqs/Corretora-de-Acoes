## 1. Contrato e resolução financeira

- [x] 1.1 Expor o contrato autenticado `POST /api/wallet/sales` com ativo, associação de corretora e quantidade inteira positiva; rejeitar corpo ausente, campos ausentes, quantidade fracionária, zero ou negativa pelo contrato uniforme, sem alterar dados. Verificar respostas HTTP, autenticação/CSRF e validações de entrada.
- [x] 1.2 Resolver a cotação do ativo antes da transação e o USD/BRL para ativos norte-americanos, aceitando dado atual ou cache utilizável, preservando fonte, instantes e indicação de desatualização. Verificar fallback de ativo e câmbio, ausência de cada dependência e bloqueio sem efeitos.

## 2. Autorização e venda transacional

- [x] 2.1 Derivar a conta da sessão e revalidar, sob bloqueio, conta ativa, associação própria ativa, ativo ativo e posição da combinação conta-corretora-ativo. Verificar acesso sem sessão, identificador de outra conta, corretora inativa/inexistente/de outra conta e isolamento.
- [x] 2.2 Aplicar o núcleo financeiro da T26 para reduzir o custo pela média vigente, preservar a média na venda parcial, zerar a posição na venda total e calcular resultado realizado positivo e negativo. Verificar venda parcial, total, acima da posição, posição zerada não operacional e recompra usando somente o novo custo.
- [x] 2.3 Creditar o saldo único pelo valor financeiro determinado no backend, converter ativos norte-americanos para BRL com `HALF_UP` e ignorar preço, saldo, câmbio ou resultado enviados pelo cliente. Verificar saldo antes/depois, conversão, arredondamento e tentativa de preço manipulado.
- [x] 2.4 Proteger vendas concorrentes da mesma posição e manter o saldo não negativo, fazendo no máximo uma venda consumir a quantidade disponível. Verificar concorrência com quantidade disputada e confirmar que a rejeição não produz crédito, redução ou registro parcial.

## 3. Histórico, patrimônio e consistência

- [x] 3.1 Registrar a movimentação imutável de venda com conta, ativo, corretora, quantidade, preço original, preço em BRL, valor creditado, resultado realizado, moeda/conversão e instantes efetivamente usados; gerar ponto patrimonial com saldo e posições resultantes. Verificar preservação do histórico anterior, conteúdo dos registros e ausência de registro em venda rejeitada.
- [x] 3.2 Manter saldo, posição, resultado, movimentação e patrimônio na mesma transação, revertendo tudo quando falhar qualquer etapa de persistência. Verificar falha injetada no saldo, posição, movimentação e patrimônio, comparando cada estado ao estado anterior.

## 4. Verificação integral

- [ ] 4.1 Executar a suíte focada de venda, os testes de regressão da compra, a compilação Maven e `git diff --check`; confirmar os critérios CA04, CA05, CA07 e CA08 aplicáveis à venda e todos os cenários dos deltas, sem alterar frontend ou banco principal.
