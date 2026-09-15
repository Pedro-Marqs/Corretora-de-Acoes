## Why

A T21 confirmou os contratos técnicos de Brapi e AwesomeAPI e identificou as condições necessárias para validar definitivamente a cobertura da Twelve Data. A T22 transforma essas evidências em portas e adapters de produção substituíveis, evitando que os casos de uso futuros dependam diretamente de DTOs, formatos, autenticação ou falhas específicas dos fornecedores externos.

## What Changes

- Criar contratos internos independentes para consulta de ativos/cotações brasileiras, ativos/cotações norte-americanas e câmbio USD/BRL.
- Criar modelos internos mínimos para representar identificação do ativo, nome, mercado, moeda, preço e instante da cotação.
- Implementar client, DTOs externos e adapter de produção para Brapi conforme o contrato validado na T21.
- Mapear ativos retornados pela Brapi para mercado brasileiro sem inferir mercado apenas pela moeda.
- Utilizar `regularMarketTime` como instante da cotação da Brapi e rejeitar respostas sem os campos obrigatórios.
- Implementar client, DTOs externos e adapter de produção para AwesomeAPI.
- Utilizar exclusivamente `bid` como cotação interna USD/BRL, conforme a decisão técnica registrada na T21.
- Utilizar o `timestamp` numérico da AwesomeAPI como instante autoritativo do câmbio.
- Implementar o adapter da Twelve Data somente depois que o gate técnico definido pela T21 for satisfeito com uma chave local válida e a amostra obrigatória de cobertura for confirmada.
- Quando liberada, mapear ativos da Twelve Data para mercado norte-americano somente por MIC/exchange aceito, nunca apenas por `currency=USD`.
- Impedir que instrumento de mercado não suportado seja convertido em ativo operacional norte-americano.
- Manter DTOs e formatos específicos de Brapi, Twelve Data e AwesomeAPI restritos à camada de infraestrutura.
- Normalizar ticker, nome, mercado, moeda, preço e timestamp para modelos internos comuns.
- Representar preços e câmbio com tipos decimais adequados, sem `float` ou `double` para os valores financeiros.
- Aplicar a normalização/arredondamento externo exigido pelo projeto no limite definido para os modelos internos, utilizando `HALF_UP` quando aplicável.
- Externalizar URLs, tokens, API keys e timeouts por configuração.
- Nunca registrar credenciais em código, fixtures, logs ou arquivos versionados.
- Mapear de forma consistente símbolo inexistente, mercado rejeitado, resposta incompleta, `429`, `5xx`, timeout, falha de transporte e conteúdo inválido.
- Não realizar retry automático nesta tarefa.
- Criar testes determinísticos com fixtures da T21 para sucesso e falhas dos três fornecedores.
- Manter a suíte padrão totalmente independente de internet e credenciais.
- Não implementar persistência de catálogo/cotações/câmbio, cache, fallback, controle de quota, scheduler, controller ou interface nesta tarefa.
- Preservar para as tarefas posteriores as limitações de quota e recência identificadas na T21, sem introduzir polling fixo de cinco minutos nos adapters.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. A T22 implementa infraestrutura interna para comportamentos já definidos em `ativos-cotacoes-cambio`, sem alterar requisitos funcionais observáveis do sistema.

## Impact

- `domain/port` para contratos internos de mercado e câmbio.
- Modelos internos necessários para ativo, cotação e USD/BRL.
- `infra/client` para Brapi, Twelve Data e AwesomeAPI.
- `infra/client/dto` com contratos específicos dos fornecedores.
- `infra/adapter` convertendo respostas externas para os modelos internos.
- Configuração externa de URLs, credenciais e timeouts.
- Tratamento comum de falhas de integrações externas.
- Fixtures produzidas pela T21 reutilizadas nos testes determinísticos.
- Testes unitários e de integração dos clients/adapters sem dependência obrigatória da internet.
- Documentação técnica da T21 como fonte das decisões de campos, timestamps, mercados e limitações.
- Nenhuma alteração de banco de dados, frontend ou API REST nesta tarefa.
- Nenhuma implementação de cache ou agendamento; essas responsabilidades permanecem para as tarefas posteriores.
- A implementação Twelve Data permanece condicionada ao gate técnico pendente da T21 e não deverá ser construída a partir de campos ou cobertura não comprovados.