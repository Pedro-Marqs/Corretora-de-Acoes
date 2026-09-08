## ADDED Requirements

### Requirement: Apresentar histórico na interface

A interface privada SHALL permitir ao investidor consultar o histórico retornado por `GET /api/history`, exibindo somente registros da sessão autenticada e os campos aplicáveis ao tipo de movimentação. A interface SHALL apresentar os registros mais recentes primeiro, respeitar o limite de 20 itens por página e não oferecer ações de edição ou exclusão.

#### Scenario: Exibir tipos de movimentação
- **WHEN** a consulta retornar compras, vendas, aportes ou transferências
- **THEN** a interface SHALL identificar cada tipo e exibir somente os campos funcionais fornecidos para aquele movimento, sem expor entidades, credenciais ou identificadores internos desnecessários

#### Scenario: Consultar página do histórico
- **WHEN** o investidor abrir o histórico ou selecionar outra página
- **THEN** a interface SHALL solicitar a página correspondente ao backend, exibir no máximo 20 registros e mostrar controles de paginação coerentes com `page`, `totalElements` e `totalPages`

#### Scenario: Combinar filtros
- **WHEN** o investidor informar dois ou mais filtros de intervalo, tipo, ticker, corretora ou mercado
- **THEN** a interface SHALL enviar os filtros juntos, manter seus valores ao trocar de página e exibir somente os registros correspondentes ao resultado oficial da API

#### Scenario: Alterar filtros
- **WHEN** o investidor aplicar, remover ou alterar um filtro
- **THEN** a interface SHALL reiniciar a consulta na primeira página, preservar os demais filtros e não misturar resultados da consulta anterior com a nova

#### Scenario: Histórico somente leitura
- **WHEN** o investidor visualizar qualquer registro
- **THEN** a interface SHALL não apresentar controles de edição, exclusão, correção ou estorno
