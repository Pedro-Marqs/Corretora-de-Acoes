## 1. Contrato e validação da compra

- [x] 1.1 Criar o contrato autenticado da compra com ativo, corretora e quantidade inteira positiva, derivando a conta da sessão e verificando com testes de validação, autenticação e isolamento entre contas
- [x] 1.2 Integrar a resolução de ativo ativo, associação de corretora própria ativa e cotação/câmbio utilizáveis, ignorando preço e identificador de conta enviados pelo cliente; verificar com testes de mercado ausente, ativo/corretora inválidos e preço manipulado

## 2. Caso de uso transacional

- [x] 2.1 Implementar o fluxo em duas fases, obtendo mercado antes da transação e revalidando conta ativa, propriedade, ativo, saldo e dados necessários dentro da transação; verificar com testes de saldo suficiente/insuficiente e concorrência
- [x] 2.2 Debitar o saldo compartilhado e criar ou atualizar a posição usando exclusivamente o núcleo financeiro da T26, cobrindo primeira compra, média ponderada, compra brasileira e compra norte-americana convertida para BRL; verificar que o saldo nunca fica negativo

## 3. Histórico, patrimônio e resposta

- [x] 3.1 Registrar a movimentação de compra e o ponto patrimonial com os valores efetivamente usados, instante, cotação/câmbio e posição resultante, mantendo a operação atomicamente transacional; verificar consistência e ausência de registro em rejeições
- [x] 3.2 Expor resposta de sucesso e erros pelo contrato uniforme, incluindo saldo/posição resultantes, origem temporal dos dados e solicitado/disponível quando houver saldo insuficiente; verificar que nenhum preço do cliente, credencial ou detalhe interno aparece

## 4. Verificação integral

- [x] 4.1 Completar testes de rollback forçando falha no histórico ou patrimônio e confirmar que saldo, posição, movimentação e pontos retornam ao estado anterior
- [x] 4.2 Executar a suíte focada do backend, testes de integração aplicáveis em H2 e PostgreSQL local opt-in quando configurado, além de `git diff --check` e `openspec validate --strict`; confirmar os critérios de compra das specs sem alterar frontend ou banco principal
