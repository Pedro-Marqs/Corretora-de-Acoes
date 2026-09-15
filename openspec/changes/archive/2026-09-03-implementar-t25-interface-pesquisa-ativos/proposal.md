## Why

A T23 disponibilizou no backend a pesquisa e o cache de ativos, mas o investidor ainda não consegue consultar esses dados pela aplicação. A T25 conecta esse contrato à interface para tornar visíveis cotações, conversões, horários e a atualidade dos valores antes das operações.

## What Changes

- Criar uma tela privada de ativos integrada à navegação existente.
- Permitir pesquisa exclusiva por ticker usando o serviço frontend de mercado.
- Apresentar ticker, nome, mercado, moeda, cotação e horário retornados pela API.
- Apresentar valores em USD e BRL para ativos norte-americanos, sem recalcular ou substituir valores oficiais no frontend.
- Identificar claramente cotações de ativos com mais de 24 horas e câmbio USD/BRL com mais de sete dias, preservando os horários utilizados.
- Tratar carregamento, resultado vazio, ativo inválido, resposta incompleta, mercado rejeitado, ausência de cache e indisponibilidade com mensagens funcionais e possibilidade de nova tentativa quando aplicável.
- Não criar atualização manual, operação financeira, página de resumo ou alteração no contrato de persistência do mercado.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `ativos-cotacoes-cambio`: explicitar a apresentação na interface dos dados de pesquisa e dos valores USD/BRL, incluindo fonte temporal e avisos de desatualização.
- `interface-estados`: definir o comportamento da tela de pesquisa de ativos nos estados de carregamento, vazio, sucesso, erro, indisponibilidade e dados desatualizados.

## Impact

- Frontend React em `src/main/front`, incluindo página, componentes, navegação, estilos e serviço de mercado.
- Contrato de consumo da API de ativos criado na T23, sem alterar a fonte de verdade financeira do backend.
- Testes de componentes e integração simulada para resultados brasileiros e norte-americanos, estados de erro e limites de idade.
- Nenhuma alteração de backend, banco, adapters, scheduler ou documentação de integração externa é necessária.
