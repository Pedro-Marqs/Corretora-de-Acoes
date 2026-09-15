## Why

As cotações e o câmbio agora são armazenados em cache, o sistema precisa atualizar esses dados automaticamente nos horários definidos pelo produto. Sem agendamentos controlados, posições ficam desatualizadas e falhas externas podem interromper a continuidade do cache.

## What Changes

- Atualizar cotações brasileiras de ativos que estejam em posições a cada cinco minutos.
- Atualizar cotações norte-americanas e USD/BRL diariamente às 10h no horário de Brasília.
- Impedir sobreposição do mesmo ciclo e limitar o ciclo diário a uma execução por dia.
- Preservar o último cache válido quando uma consulta externa falhar.
- Garantir que atualizações de mercado não criem movimentações nem pontos patrimoniais.

## Capabilities

### New Capabilities

### Modified Capabilities

- `ativos-cotacoes-cambio`: define a execução automática dos ciclos de atualização e a seleção de ativos em posições.
- `historico-registro-patrimonial`: explicita que atualizações isoladas de cotação/câmbio não geram registros históricos ou patrimoniais.

## Impact

- Serviços, repositories e adapters de mercado existentes no backend.
- Novo ou aprimorado componente em `scheduler/`, com relógio e coordenação de execução testáveis.
- Nenhuma nova rota HTTP ou alteração de contrato frontend.
