# Compra, venda e posicoes Specification

## Purpose

Registrar operacoes de compra e venda e manter posicoes financeiras consistentes.

## Requirements

### Requirement: Executar operacao
O sistema SHALL validar saldo, quantidade inteira positiva, ativo ativo, corretora ativa pertencente à conta autenticada e cotação utilizável antes de registrar uma compra ou venda. Na compra ou venda, o preço unitário e o valor financeiro SHALL ser determinados exclusivamente pelo backend a partir da cotação aplicável; qualquer preço informado pelo cliente SHALL ser ignorado e SHALL NOT influenciar o resultado.

#### Scenario: Venda valida
- **WHEN** o investidor autenticado enviar uma venda com quantidade disponível, ativo operacional, corretora própria ativa e cotação utilizável
- **THEN** o sistema SHALL registrar a movimentação, creditar o saldo e reduzir a posição pela regra financeira vigente, sem usar preço do cliente

#### Scenario: Venda internacional válida
- **WHEN** o investidor vender ativo norte-americano com cotação em USD e USD/BRL utilizável
- **THEN** o sistema SHALL calcular o crédito em reais multiplicando o valor em USD pela cotação USD/BRL e arredondando para duas casas com `HALF_UP`

#### Scenario: Compra valida
- **WHEN** o investidor autenticado enviar uma compra com saldo suficiente, quantidade válida, ativo operacional, corretora própria ativa e cotação utilizável
- **THEN** o sistema SHALL registrar a movimentação, debitar o saldo e criar ou atualizar a posição pela regra financeira vigente, sem usar preço do cliente

#### Scenario: Compra internacional válida
- **WHEN** o investidor comprar ativo norte-americano com cotação em USD e USD/BRL utilizável
- **THEN** o sistema SHALL calcular o débito em reais multiplicando o valor em USD pela cotação USD/BRL e arredondando para duas casas com `HALF_UP`

#### Scenario: Venda valida
- **WHEN** o investidor vender quantidade disponivel
- **THEN** o sistema SHALL registrar a movimentacao, creditar o saldo e reduzir a posicao

### Requirement: Rejeitar operacao inconsistente
O sistema SHALL rejeitar quantidade, ativo, corretora, posição ou cotação inválidos sem alterar o estado financeiro. A venda SHALL ser rejeitada quando a quantidade exceder a posição disponível ou não houver cotação utilizável ou, para ativo norte-americano, USD/BRL utilizável; dados de preço enviados pelo cliente SHALL ser desconsiderados, não validados como fonte financeira e não persistidos.

#### Scenario: Posicao insuficiente
- **WHEN** uma venda não tiver quantidade suficiente na posição da corretora
- **THEN** o sistema SHALL rejeitar a operação sem movimentação parcial

#### Scenario: Cotação ausente ou câmbio ausente na venda
- **WHEN** uma venda depender de cotação ou USD/BRL e não existir valor utilizável
- **THEN** o sistema SHALL rejeitar a operação sem creditar saldo, alterar posição ou registrar histórico/patrimônio

#### Scenario: Saldo ou posicao insuficiente
- **WHEN** uma compra não tiver saldo ou uma venda não tiver quantidade suficiente
- **THEN** o sistema SHALL rejeitar a operação sem movimentação parcial

#### Scenario: Preço manipulado pelo cliente
- **WHEN** o cliente enviar preço diferente da cotação determinada pelo backend
- **THEN** o sistema SHALL concluir a operação com o valor do backend, sem persistir ou usar o preço enviado

#### Scenario: Cotação ausente ou câmbio ausente
- **WHEN** uma compra depender de cotação ou USD/BRL e não existir valor utilizável
- **THEN** o sistema SHALL rejeitar a operação sem debitar saldo, alterar posição ou registrar histórico/patrimônio

### Requirement: Calcular posicao
O sistema SHALL manter quantidade, custo total, preco medio unitario e valor de mercado da posicao com precisao decimal e historico auditavel. Ao comprar unidades de uma posicao existente, SHALL calcular o novo preco medio pela media ponderada entre o custo anterior e o custo da compra. Ao vender, SHALL reduzir o custo pelo preco medio vigente e preservar esse preco medio na quantidade restante; quando a quantidade for zerada, SHALL encerrar a posição aberta.

#### Scenario: Venda parcial preserva media
- **WHEN** uma posição de 20 unidades a R$ 25,00 vender 5 unidades a R$ 30,00
- **THEN** SHALL restar 15 unidades a preco medio de R$ 25,00 e o custo restante SHALL ser R$ 375,00

#### Scenario: Venda total encerra posicao
- **WHEN** a quantidade vendida for igual à quantidade da posição
- **THEN** a quantidade, custo total e valor de mercado da posição aberta SHALL ser zero e a posição não SHALL ser apresentada como aberta

#### Scenario: Recompra apos zeragem
- **WHEN** um ativo cuja posição foi zerada for comprado novamente
- **THEN** o novo preco medio SHALL usar somente o custo da recompra, preservando a posição anterior no histórico

#### Scenario: Atualizar preco medio ponderado
- **WHEN** uma posição de 10 unidades a R$ 20,00 receber compra de 10 unidades a R$ 30,00
- **THEN** a posição SHALL conter 20 unidades, custo total de R$ 500,00 e preco medio de R$ 25,00, sem usar float ou double

#### Scenario: Atualizar preco medio
- **WHEN** uma compra for efetivada para um ativo ja mantido
- **THEN** o sistema SHALL recalcular o preco medio pela media ponderada sem usar float ou double

### Requirement: Calcular resultado de compra e venda
O sistema SHALL calcular o resultado realizado de uma venda como (preco unitario de venda menos preco medio unitario vigente) multiplicado pela quantidade vendida, sem taxas, impostos ou custos adicionais.

#### Scenario: Resultado realizado positivo
- **WHEN** 5 unidades com preco medio de R$ 25,00 forem vendidas a R$ 30,00
- **THEN** o resultado realizado SHALL aumentar em R$ 25,00

#### Scenario: Resultado realizado negativo
- **WHEN** unidades forem vendidas abaixo do preco medio vigente
- **THEN** o sistema SHALL registrar resultado realizado negativo pela mesma formula, sem alterar artificialmente o custo restante

### Requirement: Converter operacao internacional
O sistema SHALL converter valores de ativos norte-americanos para reais multiplicando o valor em USD pela cotacao USD/BRL fornecida, sem taxa cambial, e SHALL arredondar valores financeiros para duas casas com `HALF_UP`.

#### Scenario: Venda internacional convertida
- **WHEN** um ativo cotado a USD 10,00 for vendido em quantidade 2 com cotacao USD/BRL de R$ 5,00
- **THEN** o valor financeiro creditado em reais SHALL ser R$ 100,00

#### Scenario: Compra internacional convertida
- **WHEN** um ativo cotado a USD 10,00 for operado em quantidade 2 com cotacao USD/BRL de R$ 5,00
- **THEN** o valor financeiro em reais SHALL ser R$ 100,00

### Requirement: Consultar posições próprias para operações
O sistema SHALL disponibilizar `GET /api/wallet/positions` somente para investidor autenticado. A resposta SHALL ser derivada da conta da sessão e SHALL retornar o saldo disponível da conta e somente posições abertas pertencentes às associações de corretora dessa conta. Cada posição SHALL incluir identificador opaco do ativo, ticker, nome, mercado, moeda, identificador e nome da corretora, quantidade, preço médio unitário, cotação utilizável quando existente, valor de mercado, lucro ou perda não realizado e os instantes/indicadores de atualidade aplicáveis. Valores dependentes de cotação ou câmbio sem valor utilizável SHALL ser omitidos ou explicitamente indisponíveis, sem cálculo pelo cliente. A interface SHALL usar esse saldo como limite informativo de compra e SHALL exibir dados de posição no modal somente quando existir posição aberta correspondente ao ativo selecionado; não SHALL renderizar placeholders de posição para ativo sem posição.

#### Scenario: Consultar carteira com posição
- **WHEN** o investidor autenticado solicitar suas posições
- **THEN** o sistema SHALL retornar HTTP 200 com o saldo disponível e os dados completos de cada posição aberta da própria conta, incluindo quantidade, preço médio e resultado não realizado quando houver dados de mercado utilizáveis

#### Scenario: Conta sem posições
- **WHEN** o investidor autenticado não possuir posições abertas
- **THEN** o sistema SHALL retornar HTTP 200 com saldo disponível e lista de posições vazia

#### Scenario: Posição com cotação indisponível
- **WHEN** uma posição aberta não possuir cotação ou câmbio utilizável
- **THEN** o sistema SHALL manter a posição e seus dados de custo na resposta, indicar a indisponibilidade aplicável e SHALL NOT inventar valor de mercado ou lucro/perda

#### Scenario: Sessão ausente
- **WHEN** a solicitação ocorrer sem sessão autenticada
- **THEN** o sistema SHALL rejeitar com o contrato uniforme de autenticação e SHALL NOT retornar saldo ou posições

#### Scenario: Isolamento de conta
- **WHEN** uma conta consultar suas posições
- **THEN** a resposta SHALL excluir posições, corretoras, ativos ou saldos pertencentes a qualquer outra conta

#### Scenario: Ativo sem posição selecionado para compra
- **WHEN** o investidor selecionar por pesquisa um ativo que não esteja entre as posições abertas
- **THEN** o modal SHALL continuar exibindo o saldo disponível recebido no snapshot como limite informativo para compra, SHALL NOT exibir quantidade, preço médio ou resultado como placeholders de posição e SHALL permitir a validação final da compra pelo backend

### Requirement: Coordenar transferência com operações da posição

O sistema SHALL coordenar transferência, compra e venda concorrentes sobre a mesma posição de modo que cada operação observe estado confirmado e nenhuma quantidade ou custo seja perdido ou duplicado.

#### Scenario: Venda concorrente após transferência total

- **WHEN** uma venda e uma transferência total disputarem simultaneamente a posição da mesma corretora
- **THEN** somente a operação que observar quantidade disponível poderá consumir cada unidade, e a outra SHALL ser concluída sobre o estado serializado ou rejeitada sem alteração parcial

#### Scenario: Compra concorrente no destino

- **WHEN** uma compra e uma transferência creditarem simultaneamente o mesmo ativo e destino
- **THEN** a posição final SHALL refletir ambas as operações efetivadas com custo total e preço médio calculados pela regra financeira, sem sobrescrever a contribuição da outra operação

### Requirement: Abrir tela de negociação a partir do ativo

Ao acionar `Negociar` no detalhe do ativo, a interface SHALL mostrar somente a entrada de quantidade e as ações `Comprar` e `Vender`, além do preço fornecido pela API como somente leitura. A corretora e demais dados exigidos pelo contrato SHALL ser selecionáveis ou exibidos conforme o contexto autenticado existente, sem permitir preço informado pelo cliente.

#### Scenario: Negociação de ativo
- **WHEN** o investidor selecionar `Negociar`
- **THEN** a interface SHALL exibir quantidade, preço oficial e as opções de compra e venda, sem campo editável de preço

#### Scenario: Confirmar compra ou venda
- **WHEN** o investidor informar quantidade válida e escolher compra ou venda
- **THEN** a interface SHALL solicitar confirmação simples e SHALL enviar somente os dados autorizados pelo contrato existente

#### Scenario: Cotação indisponível
- **WHEN** não houver cotação ou câmbio utilizável
- **THEN** a interface SHALL bloquear a tentativa ou apresentar o erro funcional retornado, sem inventar preço ou alterar saldo/posição localmente

### Requirement: Atualizar negociação pela resposta oficial

Após operação concluída, a interface SHALL substituir saldo, posição e mensagens pelos dados devolvidos pela API ou por nova leitura autenticada. Falhas SHALL preservar o contexto recuperável e não declarar sucesso local.

#### Scenario: Operação concluída
- **WHEN** a compra ou venda for confirmada pelo backend
- **THEN** a interface SHALL apresentar o resultado oficial e permitir retornar à carteira atualizada

#### Scenario: Operação rejeitada
- **WHEN** o backend rejeitar saldo, posição, corretora, quantidade ou cotação
- **THEN** a interface SHALL exibir a mensagem funcional, preservar a entrada recuperável e SHALL NOT alterar valores exibidos por cálculo local
