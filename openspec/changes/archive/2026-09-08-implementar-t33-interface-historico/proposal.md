## Why

O backend já oferece histórico paginado e filtrável, mas o investidor ainda não possui uma tela para consultar esses registros. A T33 é o próximo passo lógico após a T32 e torna o histórico utilizável pela interface sem criar uma segunda fonte de verdade.

## What Changes

- Criar uma rota privada para consulta do histórico da conta autenticada.
- Exibir movimentos com apresentação adequada aos tipos compra, venda, aporte e transferência.
- Adicionar filtros combináveis de intervalo, tipo, ticker, corretora e mercado.
- Preservar os filtros durante a paginação e mostrar no máximo 20 registros por página.
- Tratar carregamento, vazio, erro recuperável e sessão inválida sem expor detalhes técnicos.
- Manter o histórico somente leitura, sem ações de edição ou exclusão.

## Capabilities

### New Capabilities

### Modified Capabilities

- `historico-registro-patrimonial`: adicionar os requisitos observáveis da consulta do histórico pela interface.
- `interface-estados`: adicionar os estados, responsividade e comportamento de paginação da tela de histórico.

## Impact

- Frontend: rota, página, componentes de filtros, lista/tabela, paginação e serviço de histórico.
- API: consumo do contrato existente de `GET /api/history`, sem alteração do endpoint.
- Testes: cobertura de tipos de movimento, filtros, paginação, estados de interface, sessão inválida e responsividade.
