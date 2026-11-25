## Context

Ver `proposal.md` para a motivação e `specs/ativos-cotacoes-cambio/spec.md` para o contrato observável. O Graphify localiza o fluxo em `AwesomeApiClient`, `TwelveDataClient`, seus adapters, `ExchangeRateService`, `MarketCachePersistenceService` e `MarketRefreshService`, além dos testes de adapters, cache e probes externos.

A evidência da VPS foi fornecida pelo usuário: sem autenticação e com `Authorization: Bearer`, a AwesomeAPI respondeu HTTP 429/`QuotaExceeded`; com `x-api-key`, respondeu HTTP 200. O Codex não acessou a VPS e não deve apresentar essa evidência como teste próprio. Uma consulta Twelve Data autenticada retornou HTTP 200 no ambiente local, mas a validação ponta a ponta da produção permanece um gate posterior.

O worktree contém alterações locais do change `readequacao-plataforma`. Este change altera somente seus próprios artefatos durante o planejamento e, na implementação futura, deverá modificar exclusivamente o escopo de market data, sem checkout destrutivo, stash, reset, clean ou alteração da arquitetura de histórico diário.

## Goals / Non-Goals

**Goals:**

- Corrigir a autenticação da AwesomeAPI sem alterar o contrato de Twelve Data sem evidência oficial.
- Fazer o serviço de USD/BRL aproveitar cache fresco antes de chamar a rede e preservar fallback stale sem fabricar dados.
- Controlar chamadas concorrentes, retries de 429 e ciclos de refresh.
- Produzir logs operacionais seguros, úteis para diferenciar autenticação, limite, timeout, resposta inválida e indisponibilidade.
- Comprovar regressões de ações brasileiras, ações norte-americanas, câmbio e operações dependentes.
- Registrar uma avaliação documental de fonte alternativa de câmbio, sem ativá-la.

**Non-Goals:**

- Não alterar o banco, migrations, dados de produção, histórico patrimonial diário ou snapshots financeiros.
- Não trocar a Twelve Data para header ou outro método sem documentação oficial aplicável e teste de contrato.
- Não adicionar SDK, cliente HTTP, biblioteca de retry ou dependência de observabilidade sem necessidade comprovada.
- Não fazer deploy, testar usando credenciais de produção dentro do repositório, nem registrar chaves em evidências.
- Não substituir preços, câmbio, saldos, posições, timestamps ou cálculos oficiais por valores do frontend.

## Decisions

### 1. Correção localizada da autenticação AwesomeAPI

O `AwesomeApiClient` passará a enviar `x-api-key` somente quando a chave não estiver vazia. O header `Authorization: Bearer` não será enviado como alternativa silenciosa, porque a evidência da VPS mostra que ele é interpretado como chamada sem quota válida. A ausência de chave continuará sendo uma falha controlada, não uma tentativa de descobrir credenciais.

Alternativa rejeitada: enviar os dois headers. Isso mantém o contrato incorreto, dificulta diagnóstico e pode expor a mesma credencial em caminhos de autenticação diferentes.

### 2. Twelve Data permanece com seu método atual até validação documental

O cliente Twelve Data continuará usando a forma de autenticação já adotada pelo projeto enquanto o teste autenticado e a documentação oficial confirmarem sua validade. A validação deverá capturar apenas método e resultado, nunca a chave ou uma URI completa. Uma resposta 401/403 será classificada como autenticação; 429 como rate limit.

Alternativa rejeitada: trocar a autenticação da Twelve Data por analogia com a AwesomeAPI. Provedores diferentes não compartilham contrato de headers e query parameters.

### 3. Cache-first com TTL e qualidade preservada

`ExchangeRateService.resolveUsdBrl()` fará leitura do cache persistido antes da chamada externa. Um valor fresco será retornado imediatamente. Para valor stale, a atualização poderá ser tentada de forma controlada; em erro, o valor anterior será retornado com seu `quotedAt`, fonte e flag de stale. A política usa o TTL de câmbio já existente no domínio, sem criar uma nova tabela ou mudar o histórico patrimonial.

Alternativa rejeitada: sempre consultar o provedor e usar o cache apenas no erro. Esse fluxo aumenta chamadas, piora HTTP 429 e não aproveita a qualidade já conhecida do cache.

### 4. Single-flight local e dupla verificação

Será usado um mecanismo leve por recurso (`USD/BRL`) no processo do backend, alinhado aos locks locais já existentes no serviço de cache. A primeira chamada adquire o lock, relê o banco e só chama o provedor se outro fluxo ainda não tiver persistido um valor utilizável. Chamadas concorrentes aguardam o resultado controlado e relêem o cache. O refresh agendado manterá sua proteção contra sobreposição.

Esse desenho cobre a concorrência dentro do container atual sem manter lock de banco durante uma chamada de rede. Uma futura topologia com múltiplas réplicas exigiria uma decisão própria de coordenação distribuída e fica fora deste change.

Alternativa rejeitada: segurar uma transação/lock PostgreSQL durante a chamada externa. Isso aumenta contenção e pode bloquear operações financeiras por indisponibilidade do provedor.

### 5. Retry limitado para HTTP 429

O mapper existente continuará classificando 429 como `RATE_LIMITED`, mas o fluxo de atualização terá no máximo uma nova tentativa por ciclo. Se `Retry-After` for um valor válido e estiver dentro do limite operacional configurado, o retry respeitará esse atraso; valores ausentes, inválidos ou acima do limite cairão em um backoff curto e limitado, seguido de fallback/erro. O teste usará relógio/espera injetável ou abstraída para não dormir de forma não determinística na suíte.

Alternativas rejeitadas: retry ilimitado, que pode agravar quota; ignorar `Retry-After`, que desrespeita o provedor; ou usar biblioteca de retry, que adicionaria dependência sem necessidade.

### 6. Logs estruturados e redigidos

Os clientes/serviços registrarão apenas campos controlados: `provider`, categoria (`AUTHENTICATION`, `RATE_LIMITED`, `TIMEOUT`, `INVALID_RESPONSE`, `SERVER_ERROR`, `TRANSPORT_ERROR`), status quando disponível, operação sem parâmetros sensíveis e decisão (`CACHE_HIT`, `REFRESHED`, `STALE_FALLBACK`, `BLOCKED`). Não serão registrados headers, query strings, payloads completos, stack traces em mensagens públicas ou valores de credencial. A exceção pública continuará genérica e compatível com o contrato existente.

### 7. Fonte alternativa somente como avaliação documental

A tarefa de avaliação comparará documentação, autenticação, limites, cobertura de USD/BRL, timestamps, SLA/atualidade, custo/licença e comportamento de falha de uma fonte alternativa. O resultado será um artefato sem segredo e sem código de integração. A AwesomeAPI permanece a fonte oficial do change até decisão OpenSpec futura.

### 8. Validação em camadas

Os testes unitários capturarão headers sem rede; testes de serviço verificarão cache, concorrência e retry; testes de adapters cobrirão respostas oficiais e falhas; regressões de compra/venda garantirão ausência de mutação. Um probe opt-in de ambiente documentará como validar a VPS após deploy autorizado, sem imprimir valores de chaves. Reviewer e tester serão executados em contextos separados e não serão substituídos pelos testes do implementador.

## Risks / Trade-offs

- [A resposta 429 pode persistir] → limitar retries, respeitar `Retry-After`, preservar cache stale e registrar o provedor/motivo.
- [Cache fresco pode ocultar uma atualização disponível] → manter o TTL e o ciclo diário já definido; não transformar uma consulta de leitura em refresh forçado.
- [Lock local não coordena múltiplas réplicas] → documentar a fronteira, evitar lock de banco durante rede e exigir mudança específica se a topologia crescer.
- [Logs podem vazar segredo por URI futura] → não logar URI/headers de clientes externos e testar mensagens com as chaves sentinela ausentes.
- [Ajuste de autenticação pode afetar apenas produção] → teste de header, probe opt-in no container e regressões brasileiras/norte-americanas antes do gate de deploy.
- [Alterações locais de outro change podem ser misturadas] → modificar apenas arquivos do novo change nesta etapa, revisar `git status`/diff por caminho e não executar operações destrutivas.

## Migration Plan

1. Implementar e testar o change em isolamento lógico dentro da branch atual, sem tocar nos artefatos/código não relacionados de `readequacao-plataforma`.
2. Executar suíte focada e regressões completas; revisar logs de teste para garantir ausência de chaves.
3. Após autorização separada de deploy, validar no container do backend a presença das variáveis sem imprimir seus valores e executar probes autenticados controlados da AwesomeAPI e Twelve Data.
4. Observar cache hit, refresh, fallback stale, HTTP 429 e ausência de 503 para ativos internacionais.
5. Em rollback, reverter somente o código deste change; não há migration, backfill ou alteração de dados para desfazer.

## Open Questions

Nenhuma decisão necessária está pendente. O limite operacional exato do atraso de `Retry-After` deverá ser definido como constante/configuração interna durante a implementação, preservando o requisito de retry único e limitado; se alterar contrato ou topologia, deverá gerar novo planejamento.
