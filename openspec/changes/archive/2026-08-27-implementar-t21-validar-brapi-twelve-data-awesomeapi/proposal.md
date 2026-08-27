## Why

A integração de mercado depende de três provedores externos com contratos, cobertura e limites distintos, e implementar os adapters definitivos sem validar esses detalhes criaria dependência de suposições frágeis. A T21 comprova previamente quais campos e fluxos de Brapi, Twelve Data e AwesomeAPI são adequados ao projeto e registra suas limitações para orientar a T22.

## What Changes

- Validar tecnicamente a Brapi como fonte de ativos e cotações do mercado brasileiro.
- Confirmar na Brapi os campos necessários para ticker, nome, mercado, moeda, preço e instante da cotação.
- Validar tecnicamente a Twelve Data como fonte de ativos e cotações do mercado norte-americano.
- Confirmar na Twelve Data os campos necessários para ticker, nome, mercado, moeda, preço e instante da cotação.
- Verificar a cobertura necessária da Twelve Data para ativos norte-americanos e documentar eventual limitação que impeça seu uso conforme planejado.
- Validar a AwesomeAPI como fonte do par cambial USD/BRL.
- Confirmar os campos necessários para valor do câmbio e instante da cotação.
- Verificar como cada provedor representa ativo inexistente, mercado não suportado, resposta incompleta, limite de uso e indisponibilidade.
- Medir ou estimar o consumo necessário para o ciclo de atualização brasileiro e para o ciclo diário de dados norte-americanos e câmbio.
- Registrar limites relevantes de requisição e restrições do plano/API utilizado.
- Criar provas reproduzíveis ou testes de contrato que permitam validar os formatos observados.
- Utilizar fixtures mínimas para reproduzir offline os formatos relevantes nas etapas posteriores.
- Manter qualquer smoke test que acesse serviços reais separado da suíte normal de testes.
- Permitir configuração local de credenciais quando necessária sem registrar tokens ou chaves no repositório.
- Documentar endpoints, parâmetros, campos, formatos, exemplos, limites, falhas observadas e data das verificações.
- Não criar os adapters de produção, catálogo persistente, cache, scheduler ou endpoints funcionais de ativos nesta tarefa.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. A T21 é uma prova técnica das fontes externas que serão utilizadas para requisitos já definidos em `ativos-cotacoes-cambio`, sem introduzir ou alterar comportamento funcional do sistema.

## Impact

- Protótipos descartáveis ou testes de contrato relacionados a Brapi, Twelve Data e AwesomeAPI.
- `infra/client` apenas quando necessário para manter as provas técnicas organizadas e separadas da implementação definitiva.
- Fixtures mínimas dos formatos externos confirmados.
- Configuração local para credenciais da Twelve Data ou de outro provedor quando necessária.
- Documentação técnica em `docs/` com resultados e limitações das três fontes.
- Evidências sobre cobertura de ativos brasileiros e norte-americanos.
- Evidências sobre obtenção do câmbio USD/BRL.
- Evidências de preço, moeda e timestamps retornados pelos provedores.
- Evidências sobre limites e consumo esperado das APIs.
- Base técnica para a T22 implementar portas, clients, DTOs e adapters sem depender de suposições.
- Nenhuma alteração de banco de dados, frontend, endpoint funcional, scheduler ou regra de negócio nesta tarefa.