## Why

O workflow de CD pode declarar uma implantação inválida quando os containers já estão saudáveis, mas o processo HTTP ainda não aceitou a conexão. Na VPS, o commit `ab00211` estava funcional e os endpoints retornavam HTTP 200, porém o `curl` único recebeu `Empty reply from server` e interrompeu o deploy imediatamente.

## What Changes

- Adicionar uma verificação reutilizável de prontidão HTTP para frontend e backend, com tentativas limitadas, intervalo controlado e prazo máximo explícito.
- Registrar cada tentativa com serviço, endpoint, tentativa, status HTTP ou erro de transporte e tempo decorrido, sem segredos.
- Falhar o workflow quando o prazo total expirar, preservando o comportamento de falha do CD.
- Manter os healthchecks e a espera do Docker Compose antes das verificações HTTP.
- Gravar a referência de rollback antes da troca de checkout e preservar a referência anterior durante novas tentativas.
- Adicionar testes determinísticos do script para sucesso tardio, falha persistente, timeout e resposta HTTP inválida.

## Capabilities

### New Capabilities

- `cd-readiness`: verifica de forma limitada e observável a prontidão HTTP dos serviços após a subida do Compose, sem alterar a aplicação ou a infraestrutura da VPS.

### Modified Capabilities

Nenhuma capacidade de negócio, financeira, banco de dados ou API será alterada.

## Impact

- `.github/workflows/cd.yml` usará a verificação de prontidão após `docker compose up --wait`.
- Um script shell versionado e seus testes serão adicionados ao repositório.
- A execução remota continuará usando SSH, a mesma configuração de produção, o mesmo Compose e a mesma política de rollback.
- Não haverá deploy, reinício ou alteração de produção durante esta implementação.
