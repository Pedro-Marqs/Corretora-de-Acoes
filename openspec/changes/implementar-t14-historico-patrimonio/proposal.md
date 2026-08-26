## Why

As próximas operações financeiras do sistema precisam registrar movimentações e a evolução patrimonial de forma consistente, imutável e transacional. A T14 cria essa infraestrutura compartilhada antes da implementação de aportes, compras, vendas, transferências e demais operações que dependem desses registros.

## What Changes

* Implementar a infraestrutura interna para persistência de movimentações financeiras e pontos patrimoniais.
* Suportar os tipos de movimentação previstos pelo projeto, mantendo somente os dados aplicáveis a cada tipo.
* Criar um mecanismo compartilhado para registrar movimentação e ponto patrimonial de forma atômica junto à operação que os originou.
* Calcular e persistir o estado patrimonial utilizado no momento do registro.
* Garantir que registros históricos persistidos não possam ser editados ou removidos silenciosamente.
* Garantir que atualizações isoladas de cotação não criem movimentações ou pontos patrimoniais.
* Adicionar testes para tipos de movimentação, consistência patrimonial, imutabilidade e rollback transacional.

## Capabilities

### New Capabilities

Nenhuma. A infraestrutura implementa comportamentos já definidos pelas specs existentes do projeto.

### Modified Capabilities

Nenhuma. A T14 não altera requisitos da capability `historico-registro-patrimonial`; apenas implementa a infraestrutura necessária para atendê-los.

## Impact

* Camada de domínio relacionada a movimentações e patrimônio.
* Repositories de movimentação e ponto patrimonial.
* Serviços internos responsáveis pelo registro de histórico e patrimônio.
* Cálculo do patrimônio da conta.
* Integração transacional com operações financeiras implementadas nesta e nas próximas tarefas.
* Testes de domínio, persistência e transação.
* Nenhum novo endpoint público é necessário especificamente para esta infraestrutura.
