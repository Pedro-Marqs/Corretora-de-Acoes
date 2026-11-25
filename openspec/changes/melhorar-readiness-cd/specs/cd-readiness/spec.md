## Purpose

Define uma verificação de prontidão pós-implantação que diferencie containers saudáveis de serviços HTTP efetivamente disponíveis, mantendo falha segura e rollback recuperável.

## ADDED Requirements

### Requirement: Verificação HTTP limitada e observável

O processo de CD SHALL verificar os endpoints locais do frontend e do backend após a espera saudável do Docker Compose, usando tentativas limitadas, timeout por requisição e prazo total configuráveis e finitos.

#### Scenario: Serviços ficam disponíveis após uma tentativa inicial

- **WHEN** uma ou mais tentativas iniciais retornam erro de transporte ou status HTTP não 2xx e ambos os endpoints retornam 2xx antes do prazo total
- **THEN** a verificação SHALL registrar as falhas e o sucesso tardio e SHALL terminar com código zero

#### Scenario: Serviço permanece indisponível

- **WHEN** qualquer endpoint não retorna 2xx até o limite de tentativas ou o prazo total
- **THEN** a verificação SHALL registrar o endpoint, a última causa observada e SHALL terminar com código diferente de zero

#### Scenario: Resposta HTTP não 2xx

- **WHEN** o servidor aceita a conexão mas responde 4xx ou 5xx
- **THEN** a tentativa SHALL ser registrada como falha HTTP com o status observado e SHALL ser elegível a novas tentativas somente dentro dos limites configurados

#### Scenario: Conexão encerrada sem resposta

- **WHEN** o cliente HTTP recebe erro de transporte, incluindo resposta vazia
- **THEN** a tentativa SHALL registrar o código ou motivo seguro do transporte e SHALL continuar conforme o prazo restante, sem considerar o endpoint pronto

### Requirement: Logs seguros de prontidão

Cada tentativa SHALL registrar serviço, endpoint sem query string ou fragmento, número da tentativa, status HTTP quando disponível, motivo seguro e tempo decorrido, sem imprimir credenciais, cabeçalhos sensíveis ou payloads.

#### Scenario: Falha transitória registrada

- **WHEN** uma tentativa falha antes de uma tentativa posterior bem-sucedida
- **THEN** o log SHALL distinguir tentativa, falha transitória e recuperação, sem expor segredos

### Requirement: Preservação da referência de rollback

O CD SHALL gravar a referência da versão anterior de forma atômica antes de trocar o checkout e SHALL preservar essa referência quando uma nova tentativa iniciar sobre uma versão que não foi confirmada como implantada com sucesso.

#### Scenario: Retry do mesmo commit após falha de readiness

- **WHEN** o checkout atual corresponde ao commit alvo, mas a implantação anterior não registrou sucesso
- **THEN** o CD SHALL reutilizar a referência de rollback existente e SHALL falhar se ela não existir ou não apontar para um commit válido

#### Scenario: Novo commit após implantação malsucedida

- **WHEN** o checkout atual é uma versão cuja prontidão não foi confirmada e um novo commit alvo é recebido
- **THEN** o CD SHALL preservar a última referência conhecida antes da falha até que o novo commit conclua com sucesso

#### Scenario: Implantação concluída

- **WHEN** ambos os endpoints passam na verificação de prontidão
- **THEN** o CD SHALL registrar o commit alvo como a última versão confirmada e SHALL manter a referência anterior disponível para rollback

### Requirement: Preservação dos gates existentes

O CD SHALL manter a validação de configuração e segurança, a conexão Tailscale, a verificação de árvore limpa e arquivos proibidos, a validação do Compose, o build e a espera saudável dos serviços antes da verificação HTTP.

#### Scenario: Gate anterior falha

- **WHEN** qualquer gate de configuração, segurança, Compose, build ou healthcheck falha
- **THEN** o CD SHALL falhar sem executar a verificação HTTP como substituta do gate
