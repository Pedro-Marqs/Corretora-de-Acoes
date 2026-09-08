## Why

O dashboard geral já consolida a conta, mas ainda não permite analisar a composição da carteira por corretora, ativo e mercado. A T35 completa essa visão sem duplicar saldo compartilhado nem misturar posições de contas ou corretoras diferentes.

## What Changes

- Estender a consulta autenticada do dashboard para aceitar uma corretora própria ativa como visão opcional.
- Retornar indicadores e posições restritos à corretora selecionada, mantendo o saldo da conta identificado como compartilhado.
- Retornar distribuições numéricas em BRL por ativo, corretora e mercado.
- Validar propriedade e atividade da corretora, com erro uniforme para seleção inválida.
- Garantir que as parcelas agregadas sejam compatíveis com o valor total das posições da conta.

## Capabilities

### New Capabilities

### Modified Capabilities

- `dashboards`: adicionar visão por corretora e distribuições agregadas por ativo, corretora e mercado.

## Impact

- Endpoint e serviço de dashboard, consultas agregadas e projeções de posições/cotações.
- Contrato da especificação de dashboards e testes de isolamento, agregação e validação de corretora.
- Nenhuma migração, nova dependência externa ou alteração no saldo compartilhado.
