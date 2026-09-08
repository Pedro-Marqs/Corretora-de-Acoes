# Historico e registro patrimonial Specification

## Purpose

Manter historico imutavel de movimentacoes e pontos patrimoniais para consulta e auditoria.

## Requirements

### Requirement: Historico de movimentacoes
O sistema SHALL registrar operações, aportes, transferências e ajustes com conta, instante, valores e origem. Uma venda concluída SHALL registrar ao menos o tipo da operação, conta, ativo, corretora, quantidade, preço determinado pelo backend, valor financeiro, resultado realizado e moeda/conversão aplicável. Atualizações automáticas de cotações ou câmbio, sem operação financeira, SHALL NOT criar movimentações. O sistema SHALL disponibilizar a consulta somente leitura do histórico da conta autenticada por `GET /api/history`, ordenando os registros do mais recente para o mais antigo e limitando cada página a 20 registros. A consulta SHALL aceitar filtros combináveis de intervalo temporal, tipo de movimentação, ticker, corretora e mercado, além de página solicitada, e SHALL retornar metadados suficientes para navegação, incluindo página atual, tamanho efetivo, total de registros e total de páginas.

#### Scenario: Registrar venda concluída
- **WHEN** uma venda for efetivada
- **THEN** o sistema SHALL persistir uma movimentação imutável vinculada à conta autenticada, contendo os dados financeiros efetivamente usados e o resultado realizado

#### Scenario: Venda rejeitada
- **WHEN** uma venda for rejeitada antes da conclusão
- **THEN** o sistema SHALL não criar movimentação

#### Scenario: Registrar compra concluída
- **WHEN** uma compra for efetivada
- **THEN** o sistema SHALL persistir uma movimentação imutável vinculada à conta autenticada, contendo os dados financeiros efetivamente usados

#### Scenario: Compra rejeitada
- **WHEN** uma compra for rejeitada antes da conclusão
- **THEN** o sistema SHALL não criar movimentação

#### Scenario: Consultar historico proprio
- **WHEN** o investidor autenticado consultar `GET /api/history`
- **THEN** o sistema SHALL retornar somente movimentações pertencentes à sua conta, em ordem decrescente de instante, com no máximo 20 registros na página solicitada e metadados de paginação

#### Scenario: Filtrar historico
- **WHEN** o investidor autenticado consultar o histórico com um ou mais filtros válidos de intervalo, tipo, ticker, corretora ou mercado
- **THEN** o sistema SHALL aplicar todos os filtros simultaneamente antes da paginação e SHALL retornar somente registros que atendam a todos eles

#### Scenario: Pagina inexistente
- **WHEN** o investidor autenticado solicitar uma página válida sem registros correspondentes
- **THEN** o sistema SHALL retornar uma lista vazia com metadados consistentes, sem tratar a consulta como erro

#### Scenario: Parametro de consulta invalido
- **WHEN** a consulta receber página, intervalo, tipo, ticker, corretora ou mercado em formato inválido ou fora das restrições definidas
- **THEN** o sistema SHALL rejeitar a requisição com o contrato uniforme de erro de validação e SHALL NOT alterar qualquer registro

#### Scenario: Consulta sem autenticacao
- **WHEN** uma requisição para `GET /api/history` não possuir sessão autenticada válida
- **THEN** o sistema SHALL rejeitar a consulta com o comportamento uniforme de autenticação e SHALL NOT revelar registros ou metadados da conta

#### Scenario: Atualizacao de mercado sem movimentacao
- **WHEN** um ciclo automatico atualizar cotacoes ou cambio
- **THEN** o sistema SHALL manter o historico de movimentacoes inalterado

### Requirement: Registro patrimonial
O sistema SHALL registrar pontos patrimoniais consistentes com saldo e posições em instante controlado. Uma venda concluída SHALL gerar o ponto com o saldo creditado, as posições resultantes, o câmbio utilizado quando aplicável e o valor total calculado no mesmo processamento. Atualizações isoladas de cotação ou câmbio SHALL NOT gerar ponto patrimonial.

#### Scenario: Gerar ponto patrimonial após venda
- **WHEN** ocorrer o processamento bem-sucedido de uma venda
- **THEN** o sistema SHALL persistir um ponto patrimonial consistente com o novo saldo e as posições resultantes

#### Scenario: Venda com falha de registro
- **WHEN** falhar o registro da movimentação ou do ponto patrimonial durante uma venda
- **THEN** o sistema SHALL reverter também o crédito e a alteração da posição

#### Scenario: Gerar ponto patrimonial após compra
- **WHEN** ocorrer o processamento bem-sucedido de uma compra
- **THEN** o sistema SHALL persistir um ponto patrimonial consistente com o novo saldo e as posições resultantes

#### Scenario: Gerar ponto patrimonial
- **WHEN** ocorrer o processamento de um ponto patrimonial
- **THEN** o sistema SHALL persistir saldo, posicoes, cambio e valor total usados no calculo

#### Scenario: Compra com falha de registro
- **WHEN** falhar o registro da movimentação ou do ponto patrimonial durante uma compra
- **THEN** o sistema SHALL reverter também o débito e a alteração da posição

#### Scenario: Atualizacao de mercado sem ponto patrimonial
- **WHEN** um ciclo automatico atualizar somente cotacoes ou cambio
- **THEN** o sistema SHALL deixar inalterados os pontos patrimoniais persistidos

### Requirement: Imutabilidade
O sistema SHALL impedir alteracao silenciosa de registros historicos ja persistidos.

#### Scenario: Tentativa de alterar historico
- **WHEN** uma operacao tentar modificar um registro historico
- **THEN** o sistema SHALL rejeitar a alteracao ou criar um novo evento auditavel

### Requirement: Registrar transferência e patrimônio atomicamente

Uma transferência concluída SHALL criar exatamente uma movimentação imutável do tipo transferência, vinculada à conta, ativo, origem, destino, quantidade, custo total transferido, preço médio de origem e instante da operação. SHALL criar também um ponto patrimonial pós-transferência consistente com o saldo inalterado e as posições resultantes.

#### Scenario: Transferência concluída

- **WHEN** origem e destino forem atualizados com sucesso
- **THEN** o histórico SHALL conter os dados efetivamente transferidos, o ponto patrimonial SHALL refletir a nova distribuição e nenhum resultado realizado SHALL ser criado

#### Scenario: Transferência rejeitada

- **WHEN** qualquer validação da transferência falhar
- **THEN** o sistema SHALL não criar movimentação nem ponto patrimonial

#### Scenario: Falha ao registrar transferência

- **WHEN** falhar a persistência da movimentação ou do ponto patrimonial
- **THEN** o sistema SHALL reverter também as alterações das posições e não SHALL deixar evento parcial

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
