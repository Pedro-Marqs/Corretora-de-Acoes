## Context

A T18 estabeleceu o padrão atual para integrações externas do projeto:

- contratos internos em `domain/port`;
- modelos internos independentes dos fornecedores;
- clients HTTP e DTOs específicos em `infra/client` e `infra/client/dto`;
- adapters em `infra/adapter`;
- `ExternalDataFailure` como taxonomia comum de falhas;
- `ExternalHttpTransport` e `JdkExternalHttpTransport` para transporte HTTP;
- configuração externa de URLs e timeouts por `ExternalIntegrationProperties`;
- testes determinísticos sem internet.

A T22 deve reutilizar esse padrão para dados de mercado em vez de criar uma segunda infraestrutura de integração.

O modelo atual também já possui entidades JPA `Asset`, `Quote` e `ExchangeRate`. Essas entidades representam persistência e não serão usadas diretamente como retorno dos ports externos. A T22 deverá produzir modelos internos imutáveis que a T23 poderá posteriormente validar, persistir e combinar com cache.

A T21 confirmou:

- Brapi como `APROVADO COM LIMITAÇÃO`;
- AwesomeAPI como `APROVADO COM LIMITAÇÃO`;
- Twelve Data teve a cobertura necessária do mercado norte-americano confirmada com chave local válida para AAPL/NASDAQ, MSFT/NASDAQ e KO/NYSE;
- SHOP/TSX retornou `NOT_FOUND`, evidenciando indisponibilidade dessa listagem no plano utilizado, sem invalidar a cobertura US;
- `regularMarketTime` como timestamp autoritativo da Brapi;
- mercado brasileiro derivado da fonte B3, não apenas de `BRL`;
- mercado norte-americano identificado por bolsa/MIC pertencente à allowlist validada;
- a nacionalidade da empresa não determina o mercado: uma empresa estrangeira listada em bolsa norte-americana pode ser classificada como `Market.US`;
- `bid` como valor único adotado para USD/BRL;
- timestamp numérico da AwesomeAPI como instante autoritativo;
- política de atualização brasileira baseada em quota e recência, não polling fixo de cinco minutos.

O gate da Twelve Data exige comprovação com chave local válida da cobertura necessária ao mercado norte-americano. Foram confirmados AAPL/NASDAQ, MSFT/NASDAQ e KO/NYSE.

A classificação `Market.US` é determinada pela bolsa/MIC da listagem consultada, independentemente do país de origem da empresa. Portanto, uma empresa estrangeira negociada em uma bolsa norte-americana pode ser aceita como `Market.US`.

A tentativa com SHOP/TSX retornou `NOT_FOUND` porque essa listagem não está disponível no plano utilizado. Esse resultado não bloqueia a T22: mercados não-US serão rejeitados deterministicamente por testes offline usando exchange/MIC fora da allowlist norte-americana.

A configuração base das três fontes já começou a ser declarada em `application.properties`, porém `ExternalIntegrationProperties` ainda representa somente BrasilAPI, ViaCEP e CVM e deverá ser evoluída.

See `proposal.md` - Why e `docs/integracoes-dados-mercado.md`.

## Goals / Non-Goals

**Goals:**

- Criar ports substituíveis para mercado brasileiro, mercado norte-americano e USD/BRL.
- Retornar modelos internos independentes de DTOs dos fornecedores.
- Reutilizar a infraestrutura HTTP e a taxonomia de falhas introduzidas na T18.
- Normalizar ticker, mercado, moeda, preço, fonte e timestamps.
- Aplicar `BigDecimal` e `HALF_UP` em duas casas na fronteira para os valores internos financeiros.
- Preservar separadamente o instante do dado de mercado e o instante de coleta.
- Validar todos os campos obrigatórios antes de entregar um resultado ao chamador.
- Implementar Brapi conforme as conclusões confirmadas pela T21.
- Implementar AwesomeAPI conforme as conclusões confirmadas pela T21.
- Implementar Twelve Data após comprovar com chave válida a cobertura das bolsas norte-americanas necessárias ao projeto.
- Impedir que ativos de mercados não suportados sejam mapeados como `US`.
- Classificar `Market.US` pela bolsa/MIC da listagem, e não pela nacionalidade da empresa ou somente pela moeda.
- Externalizar URLs, chaves, tokens e timeouts.
- Garantir que nenhuma credencial apareça em logs, mensagens de erro ou fixtures.
- Manter testes normais completamente offline.
- Preparar contratos que possam ser usados pela T23 sem acoplamento aos fornecedores.

**Non-Goals:**

- Persistir novos ativos.
- Atualizar `Asset`, `Quote` ou `ExchangeRate` no banco.
- Implementar catálogo.
- Implementar cache ou fallback.
- Implementar controle de quota.
- Implementar scheduler.
- Implementar política adaptativa de frequência.
- Criar controller ou endpoint REST de ativos.
- Criar interface React.
- Implementar compra ou venda.
- Executar conversão financeira de uma operação completa.
- Fazer retry automático.
- Introduzir circuit breaker.
- Alterar a fonte definida para qualquer mercado.
- Substituir a Twelve Data por outro fornecedor.
- Tornar testes reais obrigatórios na suíte padrão.

## Decisions

### 1. Criar três ports por responsabilidade de domínio

Serão utilizados contratos independentes para:

- mercado brasileiro;
- mercado norte-americano;
- USD/BRL.

Conceitualmente:

```text
BrazilMarketDataPort
UsMarketDataPort
ExchangeRatePort