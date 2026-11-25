## Why

As cotações de ações internacionais retornam HTTP 503 na produção embora funcionem localmente. As evidências fornecidas diretamente da VPS mostram que a AwesomeAPI responde HTTP 429 com ausência de autenticação ou `Authorization: Bearer`, mas HTTP 200 com `x-api-key`; o cliente atual usa o cabeçalho divergente. Além disso, o serviço cambial consulta a fonte externa antes de aproveitar um cache utilizável, aumentando chamadas desnecessárias e a exposição a limites de taxa.

## What Changes

- Corrigir o contrato de autenticação enviado à AwesomeAPI para usar `x-api-key`, preservando a chave fora de logs e mensagens públicas.
- Validar o comportamento autenticado da Twelve Data sem alterar seu método de autenticação sem confirmação documental e contratual.
- Definir cache-first para USD/BRL, com TTL, indicador de atualidade, fallback stale controlado e preservação do último valor válido.
- Evitar chamadas concorrentes duplicadas para a mesma atualização cambial e impedir ciclos de atualização sobrepostos.
- Tratar HTTP 429 de forma explícita, respeitando `Retry-After` quando fornecido e aplicando backoff limitado sem tempestade de retries.
- Adicionar observabilidade segura por provedor, motivo, status e decisão de fallback, sem chaves, tokens, URLs com credenciais ou payloads sensíveis.
- Adicionar regressões automatizadas para autenticação, cache, concorrência, rate limit, ações brasileiras, ações norte-americanas e consulta autenticada da Twelve Data.
- Avaliar documentalmente uma fonte alternativa de câmbio, sem adicionar integração ou alterar a fonte oficial automaticamente.
- Manter APIs, regras financeiras, persistência de histórico diário, autenticação, CI/CD e dados existentes inalterados.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `ativos-cotacoes-cambio`: tornar explícitos os requisitos de autenticação segura dos provedores, cache-first de USD/BRL, concorrência controlada, resposta a rate limit e diagnóstico operacional sem exposição de segredos.

## Impact

- Backend: clientes HTTP da AwesomeAPI e Twelve Data, adapters de mercado/câmbio, serviço e persistência de cache cambial, scheduler/refresh de mercado, mapeamento de falhas e logging.
- Testes: clientes, adapters, serviço de câmbio, refresh de mercado, cache e regressões de ativos BR/EUA.
- Operação: a produção continuará exigindo as credenciais já configuradas; não haverá alteração de secrets, banco, migrations, CI/CD ou deploy neste change.
- Provedores: serão respeitados os contratos de autenticação e limites documentados; a evidência da VPS será tratada como evidência externa fornecida pelo usuário, não como teste executado pelo Codex.
