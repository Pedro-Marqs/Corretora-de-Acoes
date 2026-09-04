## ADDED Requirements

### Requirement: Expor transferência autenticada

O sistema SHALL disponibilizar uma operação mutável autenticada para transferir uma posição da associação de corretora de origem para a associação de destino. A entrada SHALL conter somente identificadores opacos da origem, destino e ativo e uma quantidade inteira positiva; conta, saldo, custo, preço médio e preço de mercado SHALL ser derivados do estado persistido e da sessão.

#### Scenario: Solicitação autenticada válida

- **WHEN** o investidor autenticado enviar origem, destino, ativo e quantidade disponível válidos
- **THEN** o sistema SHALL retornar sucesso com o estado resultante da posição e SHALL NOT aceitar valores financeiros fornecidos pelo cliente como fonte de cálculo

#### Scenario: Sessão ausente

- **WHEN** a solicitação for enviada sem sessão autenticada ou sem a proteção CSRF exigida
- **THEN** o sistema SHALL rejeitar pelo contrato uniforme de autenticação/segurança e SHALL NOT alterar posição, saldo, histórico ou patrimônio

### Requirement: Rejeitar transferência inconsistente

O sistema SHALL rejeitar origem inexistente, destino inexistente, associação inativa, associações iguais, ativo incompatível, quantidade ausente, não inteira, não positiva ou superior à posição disponível. A rejeição SHALL ocorrer sem expor dados de outra conta e sem qualquer alteração persistida.

#### Scenario: Origem ou destino inválido

- **WHEN** uma associação não pertencer à conta autenticada, estiver inativa, não existir ou for igual à outra
- **THEN** o sistema SHALL retornar erro funcional uniforme sem revelar qual recurso falhou e SHALL manter todos os dados inalterados

#### Scenario: Quantidade inválida ou insuficiente

- **WHEN** a quantidade não for inteira positiva ou exceder a posição disponível na origem
- **THEN** o sistema SHALL rejeitar informando os campos aplicáveis pelo contrato de erro, sem reduzir, criar ou atualizar posição

### Requirement: Garantir atomicidade e concorrência

O sistema SHALL aplicar a transferência como uma unidade atômica, incluindo origem, destino, movimentação e ponto patrimonial. A operação SHALL revalidar propriedade, atividade e quantidade sob coordenação transacional e SHALL evitar que transferências ou compras/vendas concorrentes façam a mesma unidade ser consumida duas vezes.

#### Scenario: Falha intermediária

- **WHEN** falhar a atualização do destino, o registro da movimentação ou o ponto patrimonial
- **THEN** o sistema SHALL reverter origem, destino e todos os registros criados pela tentativa

#### Scenario: Operações concorrentes na mesma origem

- **WHEN** duas operações concorrentes tentarem transferir ou vender quantidade sobreposta da mesma posição
- **THEN** no máximo as quantidades disponíveis poderão ser efetivadas, cada operação rejeitada SHALL deixar o estado como antes da tentativa e o custo total SHALL permanecer conservado
