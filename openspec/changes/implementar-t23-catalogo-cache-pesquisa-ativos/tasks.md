## 1. Evoluir schema e persistência do catálogo

- [x] 1.1 Criar migration Flyway para adicionar tipo/status em `asset`, fonte em `quote` e `exchange_rate`, unicidade por `(ticker, market)` e backfill seguro dos registros existentes; evoluir `Asset`, `Quote`, `ExchangeRate` e repositories correspondentes e verificar com testes de migration/persistência que dados existentes são preservados, constraints são aplicadas e nenhuma duplicidade de ativo por mercado é aceita.

## 2. Implementar cache e regras temporais

- [x] 2.1 Implementar persistência do último snapshot válido de cotação e USD/BRL, preservando o valor anterior em falhas externas e impedindo que snapshot com `quotedAt` mais antigo sobrescreva um mais novo; implementar cálculo de desatualização com `Clock` usando mais de 24 horas para ativos e mais de 7 dias para câmbio e verificar com testes determinísticos os limites exatos, concorrência, fallback e ausência de sobrescrita por resposta inválida/incompleta.

## 3. Implementar pesquisa brasileira e consulta US

- [x] 3.1 Implementar pesquisa de ticker brasileiro com fluxo online-first via `BrazilMarketDataPort`, persistindo somente resposta válida e usando cache em falha externa; implementar consulta de ativos US exclusivamente pelo cache persistido, sem chamada à Twelve Data durante a consulta normal, e verificar com testes pesquisa BR válida, ticker/mercado rejeitado, falha com e sem cache, preservação do cache e ausência de consumo externo na consulta US.

## 4. Implementar câmbio e resposta de ativos

- [x] 4.1 Implementar resolução/persistência de USD/BRL via port da T22, fallback para último câmbio válido e conversão de ativo US para BRL com `BigDecimal` e `HALF_UP`, retornando preço original, valor convertido, fonte, `quotedAt` e estado de desatualização de cotação e câmbio; criar controller/DTO de pesquisa e consulta sem expor entidades JPA, permitir preço somente definido pelo backend e verificar com testes os cenários de USD/BRL válido, stale, ausência de câmbio e bloqueio de dependência financeira sem valor utilizável.

## 5. Validar integralmente a T23

- [x] 5.1 Confirmar que chamadas externas ocorrem fora de transações, que catálogo/cache são globais e não duplicados por conta, que não existe scheduler, retry, endpoint manual de refresh ou frontend antecipado e que os serviços ficam reutilizáveis pela T24; executar testes focados durante o desenvolvimento, depois `.\mvnw.cmd test` uma única vez ao final e `openspec validate implementar-t23-catalogo-cache-pesquisa-ativos --type change`, corrigindo regressões e deixando todas as tarefas marcadas como concluídas.
