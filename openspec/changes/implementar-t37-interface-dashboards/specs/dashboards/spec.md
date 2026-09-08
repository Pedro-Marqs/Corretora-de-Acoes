## ADDED Requirements

### Requirement: Apresentar dashboard consolidado

A área privada SHALL apresentar os indicadores, posições e valores retornados por `GET /api/dashboard`, incluindo saldo compartilhado, patrimônio, preço médio, resultado realizado, valorização não realizada e resultado total. A interface SHALL exibir os valores monetários com duas casas e SHALL NOT recalcular os valores oficiais no frontend.

#### Scenario: Dashboard com carteira

- **WHEN** a consulta autenticada retornar saldo, posições e indicadores
- **THEN** a tela SHALL apresentar os dados correspondentes em seções identificáveis, usando exatamente os valores confirmados pela API

#### Scenario: Dashboard sem posições

- **WHEN** a consulta retornar indicadores sem posições abertas
- **THEN** a tela SHALL exibir saldo e resultados zerados conforme a resposta e SHALL apresentar um estado vazio para a carteira

### Requirement: Filtrar dashboard por visão

A interface SHALL permitir selecionar a visão geral ou uma corretora própria ativa e SHALL consultar novamente o dashboard com a seleção. O saldo SHALL continuar identificado como compartilhado pela conta, mesmo na visão de uma corretora.

#### Scenario: Selecionar corretora

- **WHEN** o investidor escolher uma corretora própria ativa
- **THEN** a interface SHALL solicitar a visão filtrada e apresentar somente os dados retornados para aquela corretora, mantendo o saldo compartilhado

#### Scenario: Trocar para visão geral

- **WHEN** o investidor selecionar a visão geral
- **THEN** a interface SHALL consultar e apresentar novamente os dados consolidados de todas as corretoras da conta

### Requirement: Apresentar distribuicoes da carteira

A interface SHALL apresentar as distribuições por ativo, corretora e mercado retornadas pelo dashboard, incluindo o valor numérico em BRL de cada parcela. SHALL identificar a dimensão de cada distribuição e SHALL NOT substituir os valores por percentuais calculados localmente.

#### Scenario: Distribuições com posições

- **WHEN** a resposta possuir parcelas por ativo, corretora ou mercado
- **THEN** a tela SHALL apresentar cada dimensão com suas parcelas e valores em BRL

#### Scenario: Distribuições vazias

- **WHEN** a resposta não possuir posições ou distribuições
- **THEN** a interface SHALL apresentar estado vazio para as distribuições sem criar parcelas artificiais

### Requirement: Apresentar serie historica do patrimonio

A interface SHALL apresentar a série patrimonial retornada pelo dashboard em ordem cronológica e SHALL permitir selecionar `4W`, `3M`, `6M`, `1Y`, `5Y` ou `MAX`. SHALL exibir os valores em BRL e os instantes conforme o padrão de Brasília, sem interpolar ou inventar pontos.

#### Scenario: Trocar periodo

- **WHEN** o investidor escolher um período suportado
- **THEN** a interface SHALL consultar o dashboard com esse período e atualizar o gráfico somente com os pontos retornados

#### Scenario: Serie sem pontos

- **WHEN** o período selecionado não possuir pontos patrimoniais
- **THEN** a tela SHALL exibir estado vazio do histórico sem desenhar valores sintéticos

#### Scenario: Cobertura parcial

- **WHEN** a API retornar uma série iniciada após o limite selecionado
- **THEN** a interface SHALL apresentar somente os pontos retornados e SHALL NOT preencher a lacuna visualmente

### Requirement: Exibir avisos do dashboard

A interface SHALL tornar visíveis os avisos de cotação ou câmbio desatualizados e seus instantes quando fornecidos pela API, sem bloquear a leitura dos demais dados válidos.

#### Scenario: Cotacao desatualizada

- **WHEN** a resposta indicar cotação ou câmbio desatualizado
- **THEN** a tela SHALL exibir aviso associado ao dado afetado e preservar o valor confirmado pelo backend

#### Scenario: Dados atuais

- **WHEN** a resposta não indicar desatualização
- **THEN** a interface SHALL apresentar os dados sem aviso de desatualização indevido
