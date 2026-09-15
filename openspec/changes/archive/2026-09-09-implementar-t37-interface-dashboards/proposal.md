## Why

O backend já fornece indicadores, distribuições e série histórica patrimonial, mas a área privada ainda não os apresenta em uma visão consolidada. A T37 transforma esses dados oficiais em um dashboard utilizável para comparação geral ou por corretora.

## What Changes

- Criar a tela privada de dashboard com indicadores financeiros e posições recebidos da API.
- Permitir alternar entre visão geral e corretora própria ativa.
- Apresentar distribuições por ativo, corretora e mercado com seus valores numéricos.
- Apresentar a série histórica com seleção dos períodos `4W`, `3M`, `6M`, `1Y`, `5Y` e `MAX`.
- Tratar carregamento, vazio, erro, sessão inválida e cotações desatualizadas sem recalcular valores oficiais.
- Manter a tela responsiva desde 320 px, sem rolagem horizontal.

## Capabilities

### New Capabilities

### Modified Capabilities

- `dashboards`: adicionar os comportamentos observáveis da apresentação do dashboard e seus filtros visuais.
- `interface-estados`: adicionar estados, responsividade e regras de apresentação específicos do dashboard.

## Impact

- Página, componentes, estilos e serviço frontend do dashboard.
- Integração com `GET /api/dashboard`, incluindo filtro de corretora e período.
- Testes de componentes para indicadores, distribuições, série, estados e responsividade.
- Nenhuma alteração no cálculo financeiro, persistência ou contrato de autoridade do backend.
