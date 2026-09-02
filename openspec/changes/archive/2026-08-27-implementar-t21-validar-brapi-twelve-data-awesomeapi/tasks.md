## 1. Validar Brapi e dimensionar atualização sustentável do mercado brasileiro

- [x] 1.1 Criar a prova técnica da Brapi com pelo menos um ativo brasileiro válido e casos controlados de símbolo inexistente, mercado não aceito e resposta incompleta, confirmando ticker, nome, moeda, cotação, timestamp, autenticação, atraso dos dados e limites do plano utilizado; calcular uma política sustentável de atualização considerando quantidade de tickers únicos, tickers permitidos por chamada, limite mensal, frequência real de atualização dos dados, cache e margem de segurança, sem assumir o intervalo original de cinco minutos como obrigatório, e verificar/documentar que a estratégia proposta consegue permanecer dentro da quota mesmo aumentando automaticamente o intervalo quando necessário.

## 2. Validar Twelve Data e cobertura do mercado norte-americano

- [x] 2.1 Criar a prova técnica da Twelve Data com ativos norte-americanos representativos e um instrumento de mercado não aceito, confirmando ticker, nome, exchange/mercado, moeda, cotação, timestamp, autenticação, cobertura e consumo de créditos; verificar que mercado `US` pode ser identificado sem inferi-lo apenas pela moeda, calcular o consumo do ciclo diário para diferentes quantidades de ativos e classificar a fonte como aprovada, aprovada com limitação ou bloqueada para a T22, mantendo chamadas reais opt-in e fixtures reproduzíveis offline.

## 3. Validar AwesomeAPI e contrato USD/BRL

- [x] 3.1 Criar a prova técnica da AwesomeAPI para `USD-BRL`, confirmando moeda base e destino, preços disponíveis, timestamp, formato, cache/autenticação e comportamento diante de resposta inválida ou incompleta; documentar qual campo único de cotação deverá ser utilizado pela T22 para a conversão direta USD/BRL e justificar a escolha de forma consistente com compra, venda, patrimônio e dashboards, além de confirmar que uma atualização diária possui consumo compatível com o projeto.

## 4. Consolidar consumo, fixtures e prontidão para a T22

- [x] 4.1 Consolidar em `docs/` a matriz técnica de Brapi, Twelve Data e AwesomeAPI com endpoints, autenticação, campos, timestamps, mercados, limites, data da verificação, consumo projetado, falhas, fixtures e conclusão de prontidão; registrar explicitamente que a frequência brasileira futura deverá ser controlada por quota e recência dos dados, atualizando dinamicamente o intervalo em vez de consumir requisições com polling fixo desnecessário, manter todos os smoke tests externos desabilitados por padrão e verificar que a suíte normal executa sem internet ou credenciais, finalizando com `mvnw.cmd test` e validação OpenSpec. Comprovação: a matriz está em `docs/integracoes-dados-mercado.md`, a suíte passou com 219 testes (9 ignorados por integrações opcionais) e os smoke tests T21 foram ignorados sem `T21_EXTERNAL_SMOKE=true`.
