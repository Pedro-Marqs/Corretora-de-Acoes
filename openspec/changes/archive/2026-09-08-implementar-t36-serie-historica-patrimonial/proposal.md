## Why

O dashboard já apresenta o estado atual da carteira, mas ainda não permite acompanhar a evolução do patrimônio ao longo do tempo. A T36 adiciona essa série usando somente pontos patrimoniais reais, tornando os períodos comparáveis sem inventar valores entre movimentações.

## What Changes

- Estender `GET /api/dashboard` para aceitar um período histórico entre 4 semanas, 3 meses, 6 meses, 1 ano, 5 anos e máximo.
- Retornar pontos patrimoniais da conta autenticada em ordem cronológica.
- Usar a criação da conta como limite inicial do período máximo e iniciar no primeiro ponto disponível quando não houver cobertura completa.
- Preservar a ausência de pontos sintéticos para períodos sem movimentações ou atualizações isoladas de cotação.
- Validar período inválido e autenticação com o contrato uniforme existente.

## Capabilities

### New Capabilities

### Modified Capabilities

- `dashboards`: adicionar consulta de série histórica patrimonial por período.

## Impact

- Endpoint e serviço de dashboard, consulta de pontos patrimoniais e contrato da resposta histórica.
- Testes de período, ordenação, cobertura parcial, isolamento e ausência de pontos artificiais.
- Nenhuma migração, alteração de movimentações ou mudança nas regras de registro patrimonial.
