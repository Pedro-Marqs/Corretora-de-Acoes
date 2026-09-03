## Why

A compra da T27 já cria posições e debita o saldo, mas a carteira ainda não consegue realizar o ciclo financeiro completo. A T28 adiciona a venda simulada com preço de mercado determinado pelo backend, preservando a posição e registrando o resultado realizado de forma atômica.

## What Changes

- Criar o fluxo autenticado de venda por corretora e quantidade inteira positiva.
- Resolver cotação e, quando aplicável, USD/BRL utilizáveis antes da transação, usando o cache permitido.
- Revalidar conta, propriedade da corretora, ativo e quantidade da posição dentro da transação.
- Creditar o saldo único, reduzir ou zerar a posição e calcular o resultado realizado pela média vigente.
- Registrar a venda e o ponto patrimonial com os valores efetivamente usados, atomicamente.
- Rejeitar venda inválida, acima da posição ou sem cotação/câmbio utilizável sem estado parcial.

## Capabilities

### New Capabilities

Nenhuma. A venda integra capacidades existentes de carteira, mercado, corretoras, identidade, saldo e histórico.

### Modified Capabilities

- `compra-venda-posicoes`: especificar venda parcial, total, recompra, resultado realizado e conversão internacional.
- `historico-registro-patrimonial`: registrar venda e ponto patrimonial e garantir rollback da venda.
- `ativos-cotacoes-cambio`: permitir que a venda use cotação e câmbio atuais ou armazenados utilizáveis.
- `corretoras`: exigir associação ativa da própria conta para vender.
- `cadastro-autenticacao-sessoes`: exigir sessão ativa e conta derivada da sessão para venda.
- `saldo-aportes`: creditar o valor da venda no saldo único e manter atomicidade.

## Impact

- Afeta o controller/service de carteira e os repositories de conta, posição, ativo, corretora, movimentação e patrimônio.
- Adiciona o contrato HTTP autenticado de venda e testes de posição, cotação, conversão, isolamento e rollback.
- Reutiliza o núcleo financeiro da T26, o cache/adapters de mercado e o registro interno de histórico/patrimônio.
- Não implementa interface frontend, transferência, consulta paginada do histórico ou dashboards.
