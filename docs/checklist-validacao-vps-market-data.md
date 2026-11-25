# Checklist seguro de validação manual na VPS

Este roteiro deve ser executado pelo operador com acesso à VPS, depois de um deploy autorizado. O Codex não executou estes comandos e não tem acesso à produção.

## Cuidados

- Execute uma chamada por vez e respeite as quotas dos provedores.
- Não use `set -x`, `curl -v`, `docker inspect` ou comandos que imprimam o ambiente completo.
- Não cole chaves, respostas completas, cookies ou logs sem revisão/redação.
- Não exclua cache, altere tabelas, reinicie volumes ou force uma cotação para produzir um cache miss.

## 1. Presença das credenciais sem revelar valores

No diretório do Compose:

```sh
docker compose exec -T backend sh -c '
for name in TWELVE_DATA_API_KEY BRAPI_TOKEN AWESOME_API_KEY; do
  value=$(printenv "$name" 2>/dev/null || true)
  if [ -n "$value" ]; then echo "$name=present"; else echo "$name=missing"; fi
done
'
```

Resultado esperado: as três variáveis aparecem como `present`. Nenhum valor deve aparecer na saída.

## 2. AwesomeAPI autenticada

Use a mesma chave do container somente dentro do processo `curl`; não a copie para o terminal:

```sh
docker compose exec -T backend sh -c '
status=$(curl -sS -o /tmp/awesome-usd-brl.json -w "%{http_code}" \
  -H "x-api-key: ${AWESOME_API_KEY}" \
  "https://economia.awesomeapi.com.br/json/last/USD-BRL")
echo "AwesomeAPI HTTP $status"
test "$status" = 200
rm -f /tmp/awesome-usd-brl.json
'
```

Resultado esperado: `AwesomeAPI HTTP 200`. Não repetir chamadas sem necessidade.

## 3. Twelve Data autenticada

Use AAPL e, se necessário, outro ticker internacional já suportado pela aplicação. A consulta abaixo usa o método de query documentado; ela não deve imprimir o JSON:

```sh
docker compose exec -T backend sh -c '
status=$(curl -sS -o /tmp/twelve-aapl.json -w "%{http_code}" \
  "https://api.twelvedata.com/quote?symbol=AAPL&apikey=${TWELVE_DATA_API_KEY}")
echo "Twelve Data AAPL HTTP $status"
test "$status" = 200
rm -f /tmp/twelve-aapl.json
'
```

Resultado esperado: HTTP 200. Uma resposta 401/403 deve ser investigada como autenticação/permissão, sem registrar a chave.

## 4. Fluxo pela aplicação

1. Acesse a aplicação com uma conta de teste autorizada.
2. Abra a seção Bolsa e consulte `AAPL`; depois consulte outro ativo internacional suportado, como `MSFT` ou `KO`.
3. Confirme que a tela exibe somente preço, moeda, timestamp e avisos retornados pelo backend.
4. Confirme USD e BRL quando o backend fornecer USD/BRL utilizável.
5. Consulte um ticker brasileiro e confirme que a regressão BR continua funcional.
6. Não informe preço, câmbio ou timestamp manualmente.

## 5. Cache hit/miss sem alterar produção

- **Miss/refresh controlado:** observe a primeira consulta após o backend iniciar ou após a janela de atualização prevista, sem apagar registros.
- **Hit:** repita a mesma consulta dentro do TTL; o backend deve registrar `decision=CACHE_HIT` para USD/BRL e não gerar nova chamada externa.
- **Stale:** somente se ocorrer naturalmente após o TTL ou falha real do provedor, confirme que o valor mantém `quotedAt` original e aparece explicitamente como desatualizado.

Para inspeção redigida:

```sh
docker compose logs --since 15m backend | grep -E 'market-data provider=|AwesomeAPI|Twelve Data'
```

Compartilhe apenas linhas revisadas. Não compartilhe logs completos antes de verificar que não há segredos.

## 6. Ausência de HTTP 503 no fluxo afetado

- Confirme que as buscas de AAPL e do segundo ativo internacional concluem sem mensagem HTTP 503.
- Verifique no Network do navegador somente status, rota e tempo; redija cookies e headers de autenticação.
- Verifique nos logs que falhas, se houver, aparecem com `provider`, `reason`, `status` e `decision`, sem chave, query string sensível ou payload completo.
- Se o provedor responder 429, não repita manualmente em loop: confirme o fallback/cache e aguarde a janela de quota.

## Evidência mínima a registrar

- Data/hora e timezone da verificação.
- Resultado `present/missing` das três variáveis.
- Status HTTP da AwesomeAPI e Twelve Data, sem corpos com dados sensíveis.
- Tickers consultados pela aplicação e status observado.
- Evidência de `CACHE_HIT`, refresh ou fallback stale.
- Trechos de log redigidos e ausência de HTTP 503.
