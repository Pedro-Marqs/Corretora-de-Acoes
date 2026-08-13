# Spec — Ativos, cotações, câmbio e cache

## Objetivo

Fornecer dados utilizáveis de ativos brasileiros e norte-americanos, com atualização, conversão cambial, cache e indicação de desatualização.

## Usuários envolvidos

- Investidor;
- Agendador interno;
- Brapi;
- Twelve Data;
- AwesomeAPI.

## Pré-condições

- Sessão válida para pesquisas na carteira;
- Internet para consultas atuais;
- Credenciais válidas quando exigidas;
- Cobertura e campos da Twelve Data validados antes da integração completa.

## Entradas

- Ticker pesquisado;
- Respostas dos provedores com ticker, nome, mercado, moeda, cotação e horário;
- Cotação USD/BRL da AwesomeAPI.

## Validações

- Somente mercados brasileiro e norte-americano são aceitos.
- Criptomoedas e moedas não são ativos operáveis.
- Ticker, nome, mercado, moeda e cotação são obrigatórios.
- Valores são arredondados para duas casas por `HALF_UP` antes dos cálculos.
- O horário da cotação deve ser preservado para determinar sua idade.

## Fluxo principal

1. O investidor pesquisa um ticker.
2. Para ativo brasileiro, o backend tenta consultar uma cotação atual.
3. Para ativo norte-americano, o backend usa a cotação do ciclo diário armazenada.
4. O sistema valida e apresenta os campos obrigatórios e o horário da cotação.
5. Para ativo norte-americano, apresenta USD e o valor convertido em BRL.
6. O agendador atualiza posições brasileiras a cada cinco minutos.
7. Às 10h de Brasília, atualiza uma vez ao dia posições norte-americanas e USD/BRL.

## Fluxos alternativos

- Se o provedor falhar, o sistema usa a última cotação armazenada.
- Cotação de ativo com mais de 24 horas gera aviso, mas permanece operável.
- USD/BRL com mais de sete dias gera aviso, mas permanece utilizável.
- Ativo que deixou de ser retornado pode ser vendido pela última cotação armazenada.

## Situações de erro

- Ativo de mercado não aceito;
- Resposta sem campo obrigatório;
- Timeout, limite de requisições, HTTP `429` ou `5xx`;
- Ausência simultânea de cotação atual e armazenada;
- Ausência de USD/BRL necessário para converter operação norte-americana.

## Regras de autorização

- O investidor consulta dados de mercado para uso em sua própria conta.
- Somente o backend define a cotação usada financeiramente.
- O usuário não pode iniciar atualização manual.

## Resultado esperado

Dados válidos são apresentados com fonte temporal identificável; falhas usam cache sem apagar dados válidos e bloqueiam somente quando não existe valor necessário.

## Critérios de aceitação

### CA01 — Pesquisa brasileira

**Dado** um ticker brasileiro válido retornado com todos os campos  
**Quando** o investidor pesquisá-lo  
**Então** o sistema deve apresentar ticker, nome, mercado, moeda, cotação e horário.

### CA02 — Exibição norte-americana

**Dado** uma cotação diária em USD e USD/BRL armazenado  
**Quando** um ativo norte-americano for exibido  
**Então** devem ser apresentados o valor em USD e o correspondente em BRL.

### CA03 — Ciclo brasileiro

**Dado** posições brasileiras e backend com internet  
**Quando** transcorrer o intervalo de cinco minutos  
**Então** o agendador deve tentar atualizar suas cotações.

### CA04 — Ciclos diários

**Dado** posições norte-americanas e backend com internet às 10h de Brasília  
**Quando** iniciar o ciclo diário  
**Então** o sistema deve tentar atualizar suas cotações e o USD/BRL uma vez naquele dia.

### CA05 — Fallback

**Dado** uma falha do provedor e uma cotação armazenada  
**Quando** o valor for necessário  
**Então** o sistema deve usar o valor armazenado e mostrar seu horário.

### CA06 — Cotação antiga

**Dado** uma cotação de ativo com mais de 24 horas  
**Quando** ela for exibida ou usada  
**Então** o sistema deve mostrar aviso e permitir a operação.

### CA07 — Ausência de cotação

**Dado** falha externa sem cotação armazenada  
**Quando** uma operação depender do valor  
**Então** a operação deve ser bloqueada sem alteração financeira.

### CA08 — Dados incompletos

**Dado** uma resposta sem nome, ticker, mercado, moeda ou cotação  
**Quando** ela for recebida  
**Então** o ativo não deve ser aceito e dados válidos anteriores devem ser preservados.

## Requisitos relacionados

- RF29–RF42;
- RN15, RN24, RN25, RN27 e RN28;
- RNF04–RNF06, RNF13, RNF16, RNF17 e RNF19;
- HU07 e HU14;
- CE11, CE12, CE17–CE19 e CE22.
