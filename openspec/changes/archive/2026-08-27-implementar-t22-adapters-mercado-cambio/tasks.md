## 1. Preparar contratos e infraestrutura compartilhada

- [x] 1.1 Criar os modelos internos imutáveis de cotação de mercado e câmbio e os ports separados para mercado brasileiro, mercado norte-americano e USD/BRL; evoluir `ExternalIntegrationProperties` para Brapi, Twelve Data e AwesomeAPI e ampliar `ExternalHttpTransport` de forma retrocompatível para aceitar headers opcionais sem quebrar BrasilAPI, ViaCEP ou CVM; verificar com testes que `domain/port` não depende de `infra/client/dto`, que URLs, tokens, chaves e timeouts são configuráveis, que credenciais não aparecem em mensagens/logs e que os testes existentes das integrações da T18 continuam passando.

## 2. Implementar adapter da Brapi para mercado brasileiro

- [x] 2.1 Implementar client, DTO externo e adapter da Brapi reutilizando a infraestrutura HTTP existente, normalizando ticker e convertendo somente respostas válidas para o modelo interno com `Market.BR`, `BRL`, nome válido, preço positivo em `BigDecimal` com duas casas `HALF_UP`, `regularMarketTime` como `quotedAt`, instante de coleta separado e fonte identificada; verificar com fixtures offline os casos de sucesso, fallback de nome, ativo inexistente, resposta incompleta, moeda/preço/timestamp inválidos, `429`, `5xx`, timeout, falha de transporte e JSON inválido, sem persistir `Asset`, `Quote` ou qualquer outro dado.

## 3. Implementar adapter da AwesomeAPI para USD/BRL

- [x] 3.1 Implementar client, DTO externo e adapter da AwesomeAPI limitado exclusivamente ao par `USD-BRL`, adotando `bid` como cotação interna única, validando `code=USD`, `codein=BRL`, valor positivo e timestamp autoritativo, convertendo o resultado para `BigDecimal` com duas casas `HALF_UP` e mantendo `quotedAt`, `collectedAt` e fonte separados; verificar com fixtures offline o caso válido, arredondamento, par incorreto, campo obrigatório ausente, `429`, `5xx`, timeout, falha de transporte e JSON inválido, confirmando que `ask` não é utilizado como valor oficial e que nenhuma persistência ou retry automático é executado.

## 4. Validar o gate e implementar adapter da Twelve Data

- [x] 4.1 Executar explicitamente o smoke test externo da T21 com `T21_EXTERNAL_SMOKE=true` e uma `TWELVE_DATA_API_KEY` local válida, confirmando AAPL/NASDAQ, MSFT/NASDAQ e KO/NYSE; considerar `Market.US` pela bolsa/MIC da listagem independentemente da nacionalidade da empresa; não exigir acesso real a bolsas não-US que não façam parte da cobertura do plano, validando deterministicamente por fixtures que exchange/MIC fora da allowlist norte-americana é rejeitado; com o gate US satisfeito, concluir client, DTO e adapter Twelve Data e verificar offline os casos de sucesso NASDAQ/NYSE, mercado não-US, autenticação inválida, símbolo inexistente, resposta incompleta, `429`, `5xx`, timeout e JSON inválido.

## 5. Validar integralmente a T22

- [x] 5.1 Consolidar os testes dos três adapters e da infraestrutura compartilhada, verificando normalização de ticker, precisão com `BigDecimal`, `HALF_UP` em duas casas, separação entre `quotedAt` e `collectedAt`, identificação da fonte, preservação da taxonomia `ExternalDataFailure`, ausência de retries automáticos, ausência de acesso a repositories ou `@Transactional`, ausência de DTOs externos nos ports e funcionamento totalmente offline da suíte padrão; executar os testes focados durante o desenvolvimento, depois `.\mvnw.cmd test` uma única vez ao final e finalizar com `openspec validate implementar-t22-adapters-mercado-cambio --type change`, confirmando que persistência, cache, scheduler, catálogo, controllers e interface da T23+ não foram antecipados.
