## ADDED Requirements

### Requirement: Apresentação de pesquisa de ativos
A interface SHALL permitir que um investidor autenticado pesquise um ativo exclusivamente por ticker e SHALL apresentar, quando houver resultado válido, ticker, nome, mercado, moeda, cotação e horário da cotação fornecidos pelo backend. Para um ativo norte-americano, SHALL apresentar tanto o valor em USD quanto o valor correspondente em BRL e SHALL preservar a identificação temporal da cotação e do USD/BRL utilizados.

#### Scenario: Resultado brasileiro
- **WHEN** o investidor autenticado pesquisar um ticker brasileiro e a API retornar um resultado válido
- **THEN** a interface SHALL exibir ticker, nome, mercado, moeda, cotação e horário da cotação sem alterar os valores recebidos

#### Scenario: Resultado norte-americano convertido
- **WHEN** o investidor autenticado pesquisar um ticker norte-americano com cotação e USD/BRL utilizáveis
- **THEN** a interface SHALL exibir ticker, nome, mercado, moeda, valor em USD, valor correspondente em BRL e os horários dos dados utilizados

#### Scenario: Dados antigos
- **WHEN** a resposta indicar cotação com mais de 24 horas ou USD/BRL com mais de sete dias
- **THEN** a interface SHALL manter os valores exibidos, identificar claramente o dado desatualizado e mostrar o horário original correspondente

#### Scenario: Pesquisa sem valor utilizável
- **WHEN** a API rejeitar o ticker, indicar mercado não suportado, informar resposta incompleta ou não possuir cache necessário
- **THEN** a interface SHALL exibir uma mensagem funcional compreensível, SHALL NOT apresentar um valor inventado e SHALL preservar o contexto para nova tentativa quando aplicável

#### Scenario: Cotação determinada pelo backend
- **WHEN** o investidor consultar um ativo
- **THEN** a interface SHALL somente apresentar os valores retornados pela API e SHALL NOT oferecer campo, controle ou ação para informar, editar ou atualizar manualmente a cotação ou o câmbio
