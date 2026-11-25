## 1. Planejamento e baseline

- [x] 1.1 Registrar que `origin/main` aponta para `ab00211`, revisar o workflow CD atual e confirmar que nenhum arquivo de produção, Compose ou banco será alterado; verificar `git status` limpo no worktree isolado. Baseline confirmada no worktree `corretora-cd-readiness`, sem alterações iniciais e sem tocar nos outros worktrees.
- [x] 1.2 Validar a delta spec e o design com `openspec validate melhorar-readiness-cd --strict` antes da implementação. Validação aprovada antes do código.

## 2. Verificação HTTP de prontidão

- [x] 2.1 Criar `.github/scripts/check-http-readiness.sh` com parâmetros finitos para prazo total, tentativas, intervalo e timeout por requisição; verificar sintaxe com `bash -n`. Sintaxe aprovada em container Linux.
- [x] 2.2 Implementar classificação separada de status 2xx, status HTTP não 2xx e erro de transporte, incluindo resposta vazia; verificar logs seguros por tentativa sem query string, credenciais, headers ou payloads. Implementado com status/código de transporte separados e redaction de query string.
- [x] 2.3 Implementar sucesso tardio e falha persistente com código de saída correto; verificar com doubles locais de `curl`, relógio e `sleep` sem usar rede, SSH ou Docker. Testes do script passaram para sucesso imediato, recuperação após erro 52, HTTP 503 persistente e erro 7 persistente.

## 3. Integração segura no workflow

- [x] 3.1 Substituir os dois `curl` únicos do `.github/workflows/cd.yml` pela chamada do script após `docker compose up --wait`; preservar configuração, Tailscale, checkout, validação do Compose, build e healthchecks. O workflow mantém todos os gates e chama o script com limites 120s/40 tentativas/3s/15s.
- [x] 3.2 Proteger `previous-sha` com gravação atômica e adicionar `last-known-good-sha`, preservando a referência durante retries de SHA malsucedido e atualizando-a somente após readiness completo; verificar com validação estática do bloco remoto e testes do estado quando aplicável. A política foi implementada com `write_state`, preservação em retry e validação de commit; o bloco remoto passou `bash -n`.
- [x] 3.3 Garantir que readiness não mascare gates anteriores nem converta falha persistente em sucesso; verificar que o workflow permanece com `set -Eeuo pipefail` e falha em código diferente de zero. O workflow preserva `set -Eeuo pipefail`, e o script retorna 1 para falha persistente.

## 4. Validação final

- [x] 4.1 Executar testes do script para sucesso imediato, sucesso tardio, HTTP não 2xx, erro de transporte, limite de tentativas e logs sem segredos; registrar aprovados, falhados e não aplicáveis. `check-http-readiness-test.sh` passou em container Linux; todos os cenários aprovados, sem falhas ou dados reais.
- [x] 4.2 Executar lint/build/testes existentes aplicáveis sem alterar CI, banco ou produção; confirmar que nenhuma alteração financeira, de API ou de infraestrutura foi introduzida. Não há código da aplicação, API, banco ou Compose neste change; `bash -n`, testes do script e validação estática do workflow foram executados em Linux.
- [x] 4.3 Executar `openspec validate melhorar-readiness-cd --strict`, `graphify update .` e `git diff --check`; revisar o diff completo e os arquivos não rastreados. Todos passaram; Graphify foi atualizado, com avisos preexistentes de dois testes Java parcialmente extraídos.
- [x] 4.4 Solicitar reviewer e tester independentes em contextos separados, sem editar arquivos, fazer staging, commit, push, PR, merge, deploy ou archive; registrar findings e corrigir somente problemas relevantes dentro deste change. Reviewer final aprovado. Tester final confirmou worktree/branch/HEAD, `git diff --check` e `git diff --cached --check` com exit 0, ausência de trailing whitespace nos arquivos não rastreados e harness aprovado via Bash do Git; nenhum teste foi ignorado.
