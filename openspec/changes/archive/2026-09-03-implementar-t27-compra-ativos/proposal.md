## Why

A carteira já possui regras financeiras determinísticas, mas ainda não há um caso de uso transacional que transforme uma cotação do backend em uma compra persistida. A T27 conecta mercado, conta, corretora, posição e histórico agora, estabelecendo a primeira operação financeira sem permitir preço manipulado pelo cliente ou estado parcial.

## What Changes

- Criar o fluxo autenticado de compra simulada de ativos por corretora e quantidade inteira positiva.
- Obter cotação e, quando necessário, USD/BRL utilizáveis antes da transação, usando fallback conforme o cache de mercado.
- Revalidar conta ativa, propriedade da corretora, ativo ativo e saldo dentro da transação antes de debitar.
- Debitar o saldo compartilhado e criar ou atualizar a posição pela média ponderada do núcleo financeiro da T26.
- Registrar a compra e o ponto patrimonial atomicamente com saldo e posição.
- Ignorar qualquer preço livre enviado pelo cliente e retornar erros uniformes para entradas e regras financeiras rejeitadas.

## Capabilities

### New Capabilities

Nenhuma. A compra integra capacidades existentes de carteira, mercado, corretoras, identidade, saldo e histórico.

### Modified Capabilities

- `compra-venda-posicoes`: definir o contrato observável da compra simulada, incluindo preço exclusivamente determinado pelo backend, conversão internacional e atomicidade.
- `historico-registro-patrimonial`: explicitar os dados registrados após uma compra e sua atomicidade com a operação.
- `ativos-cotacoes-cambio`: explicitar que a compra usa somente cotação/câmbio utilizáveis e bloqueia a operação quando ausentes.
- `corretoras`: explicitar que somente a associação ativa pertencente à conta pode ser usada na compra.
- `cadastro-autenticacao-sessoes`: explicitar que o caso de uso de compra exige sessão da própria conta ativa.
- `saldo-aportes`: explicitar o débito de compra no saldo único e sua consistência transacional.

## Impact

- Afeta controller/service de carteira e os repositories de conta, corretora, ativo, posição, movimentação e patrimônio.
- Adiciona o contrato HTTP autenticado da compra e testes unitários, de integração e de atomicidade com mocks de mercado.
- Reutiliza `FinancialAmount`, o relógio configurado, o núcleo financeiro da T26, o cache/adapters da T23 e o registro interno da T14; não cria nova dependência externa.
- Não implementa venda, transferência, interface frontend ou consulta paginada do histórico.
