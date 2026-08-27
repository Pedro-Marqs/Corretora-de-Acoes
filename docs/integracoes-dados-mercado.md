# Prova técnica T21 — Brapi, Twelve Data e AwesomeAPI

Verificação realizada em **27/08/2026**. Este documento separa o que foi observado em chamadas
reais do que foi obtido da documentação oficial. Planos, limites e formatos externos podem mudar;
eles devem ser revistos antes da T22 e nunca tratados como invariantes de domínio.

## Resultado executivo

| Fonte | Uso | Resultado para T22 | Motivo |
|---|---|---|---|
| Brapi | ativos e cotações BR | **APROVADO COM LIMITAÇÃO** | Contrato mínimo e sandbox foram confirmados; plano Gratuito atualiza ações em aproximadamente 30 minutos e aceita apenas um ticker por chamada. |
| Twelve Data | ativos e cotações US | **APROVADO COM LIMITAÇÃO** | AAPL/NASDAQ, MSFT/NASDAQ e KO/NYSE foram confirmados com chave local válida. SHOP/TSX não está disponível no plano; mercados fora da allowlist US são rejeitados deterministicamente. |
| AwesomeAPI | USD/BRL | **APROVADO COM LIMITAÇÃO** | Contrato público confirmado; sem chave há cache de um minuto. A T22 deve usar um único lado da cotação e preservar o timestamp numérico. |

O gate US da Twelve Data foi satisfeito com `T21_EXTERNAL_SMOKE=true` e chave local válida. A
limitação de cobertura observada para SHOP/TSX não altera a fonte nem amplia os mercados aceitos;
classificações US continuam dependendo de exchange/MIC pertencente à allowlist documentada.

## Matriz técnica

| Aspecto | Brapi | Twelve Data | AwesomeAPI |
|---|---|---|---|
| Finalidade | ação brasileira | ação norte-americana | conversão direta USD/BRL |
| Endpoint mínimo | `GET https://brapi.dev/api/v2/stocks/quote?symbols=PETR4` | `GET https://api.twelvedata.com/quote?symbol=AAPL` | `GET https://economia.awesomeapi.com.br/json/last/USD-BRL` |
| Autenticação | `Authorization: Bearer <token>` em produção; PETR4, MGLU3, VALE3 e ITUB4 funcionam no sandbox sem token | `apikey`, fornecida somente por `TWELVE_DATA_API_KEY` no smoke test | pública com cache de 1 minuto; chave opcional por `AWESOME_API_KEY` para evitar o cache |
| Identificação | `requestedSymbol`, `symbol`; nome principal `data.longName`, fallback `data.shortName` | `symbol`, `name`, `exchange`, `mic_code` | `code=USD`, `codein=BRL` |
| Preço | `data.regularMarketPrice` | `close` do snapshot retornado por `/quote` | `bid` e `ask`; a T22 usará **`bid`** |
| Timestamp | `data.regularMarketTime`, e não `requestedAt` | `timestamp`; `datetime` é diagnóstico | `timestamp` Unix; `create_date` é diagnóstico em UTC-3 |
| Mercado | `BR` pela fonte/endpoint restrito à B3, nunca apenas por `BRL` | `US` por MIC/exchange aceito (`XNGS`, `XNMS`, `XNCM`, `XNAS`, `XNYS`, `XASE`, `ARCX`, `BATS`), nunca apenas por `USD` | não se aplica |
| Limite observado/documentado | Gratuito: 15.000 req/ciclo, 1 ticker/chamada, atraso aproximado de 30 min; Startup: 150.000, 10, 15 min; Pro: 500.000, 20, 5 min | `/quote`: 1 crédito por símbolo; Basic: 800 créditos/dia, reinício 00:00 UTC; pesos são por endpoint e símbolo | documentação anuncia até 100.000 requisições gratuitas com chave; sem chave há cache de 1 minuto |
| Falhas reproduzidas | resultado vazio, campo ausente, `429`, `5xx`, JSON inválido | erro estruturado, mercado não aceito, campo ausente, `429`, `5xx`, JSON inválido | `404`, par ausente, campo ausente, `429`, `5xx`, JSON inválido |
| Fixtures | `src/test/resources/t21/brapi-*.json` | `src/test/resources/t21/twelve-*.json` | `src/test/resources/t21/awesome-*.json` |
| Prova offline | `MarketDataProbeTests` | `MarketDataProbeTests` | `MarketDataProbeTests` |
| Prova real opt-in | `T21ExternalSmokeTests.verifiesBrapiSandboxQuote` | `T21ExternalSmokeTests.verifiesTwelveDataRepresentativeUsQuotesAndRejectedMarket` | `T21ExternalSmokeTests.verifiesAwesomeApiUsdBrl` |

Fontes oficiais consultadas: [documentação Brapi](https://brapi.dev/docs),
[limites Brapi](https://brapi.dev/faq/quais-as-limitacoes),
[frequência Brapi](https://brapi.dev/faq/qual-a-frequencia),
[documentação Twelve Data `/quote`](https://twelvedata.com/docs/market-data/quote),
[créditos Twelve Data](https://support.twelvedata.com/en/articles/5615854-credits) e
[API de moedas AwesomeAPI](https://docs.awesomeapi.com.br/api-de-moedas).

## Evidência observada em 27/08/2026

- Brapi sandbox respondeu `200` para PETR4 sem token com `requestedSymbol=PETR4`,
  `symbol=PETR4`, nomes, `currency=BRL`, `regularMarketPrice=41.36` e
  `regularMarketTime=2026-08-27T14:52:30Z`. `requestedAt` ocorreu depois e não representa o
  instante do mercado.
- Twelve Data respondeu `200` para AAPL com a chave pública `demo`: `name=Apple Inc.`,
  `exchange=NASDAQ`, `mic_code=XNGS`, `currency=USD`, `close=314.58` e timestamps. MSFT/NASDAQ
  e SHOP/TSX responderam `401` com essa chave; portanto cobertura representativa e rejeição real
  de outro mercado continuam pendentes de credencial local.
- AwesomeAPI respondeu `200` para USD-BRL sem chave, com `bid=5.1691`, `ask=5.1715`,
  `timestamp=1787843133` e `create_date=2026-08-27 12:05:33`.

As fixtures são mínimas e preservam a precisão recebida. Elas comprovam parsing e tratamento
determinístico do formato, não disponibilidade atual do fornecedor.

## Política sustentável para atualização brasileira

A frequência futura deve ser calculada por **quota e recência**, não configurada como polling fixo
de cinco minutos. Consultar novamente antes de o plano produzir dado mais recente apenas consome
quota e tende a devolver o mesmo snapshot.

Para cada ciclo de cobrança:

```text
lotes = ceil(tickersBrUnicos / tickersPorChamadaDoPlano)
orcamentoSeguro = floor(limiteMensal * 0,80)
ciclosPossiveis = floor(orcamentoSeguro / lotes)
intervaloPorQuota = ceil(minutosAtivosNoCiclo / ciclosPossiveis)
intervaloBase = max(recenciaDoPlano, intervaloPorQuota)
```

A margem de 20% cobre pesquisas avulsas, retries controlados, variação de dias úteis e concorrência.
O scheduler da T22 deve recalcular o intervalo quando mudar o número de tickers únicos, o plano,
a quota restante ou a janela ativa. Deve agrupar tickers até o limite do plano, compartilhar cache
entre contas, deduplicar símbolos e não repetir uma chamada enquanto o snapshot estiver dentro da
recência esperada. Se o consumo projetado ultrapassar o orçamento seguro, deve aumentar o intervalo
automaticamente até `projecaoMensal <= orcamentoSeguro`. Um `429` reduz a cadência; nunca se apaga
o último dado válido.

Exemplos para 600 minutos/dia, 22 dias úteis e margem de 20%:

| Plano/carteira | Lotes/ciclo | Intervalo derivado | Projeção mensal | Orçamento seguro |
|---|---:|---:|---:|---:|
| Gratuito, 10 tickers | 10 | 30 min (recência domina) | 4.400 | 12.000 |
| Gratuito, 50 tickers | 50 | 60 min (quota domina) | 11.000 | 12.000 |
| Startup, 50 tickers | 5 | 15 min (recência domina) | 4.400 | 120.000 |

Assim, o intervalo original de cinco minutos só é sustentável e útil quando o plano efetivamente
entrega essa recência e a quota comporta os lotes. No plano Gratuito, cinco minutos para 10 tickers
consumiriam 26.400 requisições no cenário acima e seriam inviáveis; 30 minutos consomem 4.400.

## Consumo diário da Twelve Data

O custo documentado de `/quote` é um crédito por símbolo. Deduplicando os símbolos e executando um
ciclo diário, o consumo é linear:

| Ativos US únicos | Créditos/dia | Situação no Basic (800/dia) |
|---:|---:|---|
| 10 | 10 | compatível |
| 100 | 100 | compatível |
| 500 | 500 | compatível com margem de 300 |
| 640 | 640 | teto operacional recomendado com margem de 20% |
| 800 | 800 | teto nominal, sem margem para descoberta ou repetição |

O ciclo deve observar também o limite por minuto exposto pelo plano e os headers
`api-credits-used`/`api-credits-left`. Batch não reduz o peso total por símbolo. A listagem de
instrumentos deve ser armazenada conforme a recomendação do fornecedor, evitando descoberta diária.

## Decisão para USD/BRL

A T22 usará exclusivamente **`bid`** como a cotação direta USD→BRL. Na legenda oficial, `bid` é o
preço de compra do dólar: é o lado aplicável ao converter um patrimônio denominado em USD para o
valor em BRL que seria recebido. A escolha é conservadora e evita superavaliar patrimônio.

Como a primeira versão não modela spread ou taxa cambial e RN27 exige uma conversão direta única,
o mesmo `bid` deve ser usado em compra, venda, patrimônio e dashboards. Usar `ask` em um fluxo e
`bid` em outro introduziria uma regra financeira não especificada. A limitação deve permanecer
visível: isso é uma referência uniforme, não uma simulação completa de liquidação cambial.

Uma chamada diária custa uma requisição por dia (cerca de 30/31 por mês), muito abaixo do limite
publicado. O timestamp numérico une preço e instante no mesmo snapshot; `create_date` serve apenas
para diagnóstico e conferência de fuso.

## Execução reproduzível e segurança

A suíte normal usa somente fixtures e não requer internet ou credenciais. Para repetir as chamadas
reais conscientemente:

```powershell
$env:T21_EXTERNAL_SMOKE = "true"
$env:TWELVE_DATA_API_KEY = "<chave-local>"
# Opcionais para sair do sandbox/cache público:
$env:BRAPI_TOKEN = "<token-local>"
$env:AWESOME_API_KEY = "<chave-local>"
.\mvnw.cmd -Dtest=T21ExternalSmokeTests test
```

Sem `T21_EXTERNAL_SMOKE=true`, os testes externos são abortados por assumption antes de criar o
cliente HTTP. Nenhuma chave, header ou resposta com credencial é salva em fixture ou arquivo de
configuração. Os nomes das variáveis são documentação; seus valores permanecem apenas no ambiente
local.

## Orientações obrigatórias para a T22

- Não iniciar o adapter Twelve Data enquanto a amostra AAPL/MSFT (NASDAQ), KO (NYSE) e SHOP (TSX,
  rejeitado) não passar com uma chave local e limites do plano forem conferidos no dashboard.
- Não inferir mercado por moeda. Brapi define BR pelo universo do endpoint; Twelve Data exige MIC
  pertencente à allowlist US.
- Validar identificação, nome, moeda, preço e timestamp antes de substituir cache persistido.
- Tratar `404`/erro estruturado, resposta incompleta, `429`, `5xx`, timeout e JSON inválido sem apagar
  o último valor válido.
- Manter preço e timestamp do mesmo snapshot e preservar a precisão externa até a normalização da
  T22.
- Implementar quotas, cache, scheduler, ports e adapters somente na T22/T23; nenhum desses
  componentes faz parte desta prova.

## Estado do gate T22

Em **27/08/2026**, durante a implementação da T22, o ambiente não continha a variável
`TWELVE_DATA_API_KEY`. O gate obrigatório AAPL/NASDAQ, MSFT/NASDAQ, KO/NYSE e rejeição de SHOP/TSX
não pôde ser executado com uma chave local válida. Por isso, a tarefa Twelve Data permanece
explicitamente bloqueada e nenhum client, DTO ou adapter de produção desse fornecedor foi criado.

Na retomada do mesmo dia, uma chave local válida foi disponibilizada e o smoke obrigatório foi
executado explicitamente. AAPL/NASDAQ, MSFT/NASDAQ e KO/NYSE retornaram contratos válidos e foram
aceitos como mercado US. Entretanto, a consulta `SHOP` com `exchange=TSX` retornou símbolo não
encontrado (`NOT_FOUND`), e não um instrumento canadense válido que pudesse ser rejeitado como
`UNSUPPORTED_MARKET`.

Após a correção explícita dos artefatos OpenSpec, o gate passou a exigir somente a cobertura US:
AAPL/NASDAQ, MSFT/NASDAQ e KO/NYSE. Em **27/08/2026**, o smoke foi reexecutado com
`T21_EXTERNAL_SMOKE=true` e os três casos passaram. SHOP/TSX permanece como evidência de listagem
indisponível no plano e não bloqueia a T22. A rejeição de mercado não-US é comprovada offline por
fixture `TSX/XTSE`, fora da allowlist validada `NASDAQ/XNGS` e `NYSE/XNYS`.
