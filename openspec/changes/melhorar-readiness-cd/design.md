## Context

O workflow `.github/workflows/cd.yml` valida a configuração, conecta à rede privada, faz checkout do SHA aprovado, executa `docker compose config`, build e `docker compose up --wait --wait-timeout 120`. Em seguida, usa um único `curl --fail` para cada endpoint local. A evidência da VPS mostra que essa sequência pode ocorrer antes de o servidor HTTP estar pronto, embora os containers estejam Healthy.

O commit `ab00211` e os serviços da VPS não serão alterados nesta tarefa. A mudança será limitada ao workflow e a um script versionado executado no host remoto após a subida do Compose.

## Goals / Non-Goals

**Goals:**

- Tolerar a janela transitória entre healthcheck do container e prontidão HTTP.
- Ter limites explícitos de tentativas, timeout por requisição, intervalo e prazo total.
- Produzir logs úteis para diagnosticar HTTP 4xx/5xx, conexão encerrada e outros erros de transporte.
- Preservar uma referência válida para rollback em retries do mesmo SHA ou após falha de readiness.
- Validar sucesso tardio e falha persistente sem acessar a VPS real.

**Non-Goals:**

- Alterar endpoints, respostas da aplicação, Dockerfiles, `compose.yaml`, `compose.prod.yaml` ou configuração da VPS.
- Reiniciar, publicar, fazer rollback ou executar o deploy durante esta implementação.
- Alterar regras financeiras, banco, autenticação, CI ou dependências da aplicação.
- Mascarar falhas do Compose ou transformar status HTTP não 2xx em sucesso.

## Decisions

### Script de readiness separado

Adicionar `.github/scripts/check-http-readiness.sh`, chamado explicitamente pelo workflow com os endpoints `frontend=http://127.0.0.1:3000/` e `backend=http://127.0.0.1:8080/api/csrf`. O script aceitará parâmetros de prazo total, máximo de tentativas, intervalo e timeout de requisição.

Foi escolhido um script separado em vez de um loop inline no YAML para permitir testes determinísticos e reduzir a complexidade do bloco remoto. Não será adicionada dependência externa ou framework de testes de shell.

### Classificação das respostas

O script usará `curl --write-out '%{http_code}'` sem `--fail`, separando status HTTP de código de transporte. Somente status 2xx será sucesso. Erro de transporte será registrado com código seguro, incluindo o caso de resposta vazia, e status não 2xx será registrado como falha HTTP.

### Limites e cronologia

O workflow usará limites finitos documentados no próprio comando. O script verificará prazo total e máximo de tentativas, usando o menor timeout entre a configuração da requisição e o tempo restante. O intervalo será aplicado apenas quando ainda houver prazo e tentativas disponíveis.

### Estado de rollback

O workflow manterá `previous-sha` como referência de rollback e adicionará `last-known-good-sha` como confirmação da última versão que passou pela prontidão. Os arquivos serão gravados por arquivo temporário no mesmo diretório e `mv`, preservando atomicidade.

- Em uma nova versão quando o checkout atual é explicitamente a última versão conhecida como boa, `previous-sha` é atualizado para o checkout atual.
- Em retry do mesmo SHA ou quando ainda não há `last-known-good-sha`, um `previous-sha` existente não é sobrescrito; se o arquivo ainda não existir na primeira implantação, o checkout atual é usado apenas como bootstrap da referência.
- A implantação falhará se o rollback necessário não existir ou não for um commit válido.
- `last-known-good-sha` só será atualizado depois de frontend e backend passarem.

Essa política evita que um retry sobre um checkout já malsucedido destrua a única referência da versão anterior.

### Testes sem produção

Adicionar `tests/check-http-readiness-test.sh`, que substitui `curl`, relógio e `sleep` por doubles locais. Os cenários serão: sucesso imediato, sucesso após falhas de transporte, status HTTP persistente não 2xx, erro persistente, limite de tentativas, intervalo limitado pelo prazo restante, sucesso fora do prazo e redaction de query/userinfo. Os testes não abrem portas, não usam credenciais reais e não executam SSH ou Docker Compose.

## Risks / Trade-offs

- [Janela maior de CD] → O prazo total será explícito e finito; cada espera será limitada ao tempo restante, respostas concluídas fora do prazo serão rejeitadas e o workflow continuará falhando após o limite.
- [Logs excessivos em falha] → Cada tentativa terá uma linha compacta, sem payloads, headers, query strings ou userinfo de URL, inclusive nos caminhos de erro de argumentos e transporte.
- [Estado antigo sem `last-known-good-sha`] → A primeira execução mantém compatibilidade usando `previous-sha`; retries sem referência válida falham de forma segura.
- [Readiness 2xx insuficiente para validação funcional] → O objetivo deste change é eliminar o falso negativo de disponibilidade HTTP; healthchecks do Compose e gates existentes permanecem obrigatórios.

## Migration Plan

1. Validar o script localmente com os doubles e executar lint/shellcheck disponível, sem instalar ferramentas.
2. Validar OpenSpec, Graphify e diff.
3. Criar commit e PR somente após autorização explícita posterior.
4. Após aprovação e merge, o CD passará a usar o script na próxima implantação autorizada; nenhuma implantação será disparada por esta tarefa.
5. Validar na VPS, após deploy autorizado, os logs de retry, a referência de rollback e os dois endpoints.

## Open Questions

Nenhuma questão bloqueante. Os valores iniciais dos limites ficam documentados no workflow e podem ser ajustados em mudança posterior com evidência operacional.
