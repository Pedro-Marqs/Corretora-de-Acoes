# Transferencia de posicoes Specification

## Purpose

Permitir transferir posicoes entre corretoras associadas a mesma conta, preservando quantidade e historico.

## Requirements

### Requirement: Transferir posicao

O sistema SHALL validar origem, destino, ativo e quantidade antes de efetivar uma transferencia. A transferencia SHALL reduzir da origem a quantidade transferida e seu custo pelo preco medio vigente, e SHALL creditar no destino a mesma quantidade e o mesmo custo total, sem alterar o saldo.

#### Scenario: Transferencia valida

- **WHEN** o investidor transferir quantidade disponivel entre corretoras validas
- **THEN** o sistema SHALL reduzir a origem, creditar o destino, preservar o custo transferido e registrar um evento atomico

#### Scenario: Transferencia para destino com posicao

- **WHEN** o destino possuir 5 unidades a R$ 30,00 e receber 5 unidades com custo unitario de R$ 20,00
- **THEN** o destino SHALL conter 10 unidades a preco medio de R$ 25,00

#### Scenario: Transferencia total

- **WHEN** toda a quantidade da origem for transferida
- **THEN** a origem SHALL ficar sem posicao aberta e o custo total creditado no destino SHALL ser igual ao custo retirado da origem

#### Scenario: Transferencia invalida

- **WHEN** origem, destino, ativo ou quantidade forem invalidos
- **THEN** o sistema SHALL rejeitar a transferencia sem alterar as posicoes, o saldo ou o custo registrado

### Requirement: Isolamento da conta

O sistema SHALL permitir transferencia apenas entre corretoras pertencentes a conta autenticada.

#### Scenario: Corretora de outra conta

- **WHEN** uma das corretoras nao pertencer a conta do investidor
- **THEN** o sistema SHALL negar a operacao sem expor dados de terceiros

### Requirement: Preservar resultado na transferencia

O sistema SHALL tratar a transferencia como movimentacao de custo entre corretoras, sem gerar resultado realizado, valorizacao ou lucro por si só.

#### Scenario: Transferencia sem resultado

- **WHEN** uma posicao for transferida entre corretoras sem mudança de quantidade ou custo total
- **THEN** os indicadores de resultado SHALL permanecer inalterados e somente a distribuição por corretora SHALL refletir a nova localização

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

### Requirement: Iniciar transferencia pela interface

A interface privada SHALL permitir que o investidor selecione uma posicao aberta, uma corretora de destino ativa e uma quantidade inteira positiva para iniciar uma transferencia autenticada. A origem SHALL ser a corretora da posicao selecionada, o destino SHALL ser diferente da origem e a interface MUST NOT enviar saldo, preco, custo ou conta como autoridade financeira.

#### Scenario: Selecionar posicao e destino validos

- **WHEN** o investidor selecionar uma posicao aberta, uma corretora propria ativa diferente e uma quantidade inteira dentro da disponibilidade
- **THEN** a interface SHALL apresentar a operacao pronta para confirmacao, mantendo a origem derivada da posicao e os valores financeiros como somente leitura

#### Scenario: Destino igual a origem

- **WHEN** o investidor tentar escolher como destino a mesma corretora da origem
- **THEN** a interface SHALL impedir a selecao ou o envio e SHALL exibir uma orientacao funcional sem chamar a API

#### Scenario: Quantidade invalida ou excessiva

- **WHEN** a quantidade estiver vazia, nao for inteira positiva ou exceder a quantidade disponivel
- **THEN** a interface SHALL rejeitar o envio, informar o campo invalido e SHALL NOT executar a transferencia

#### Scenario: Confirmacao cancelada

- **WHEN** o investidor cancelar a confirmacao simples da transferencia
- **THEN** a interface MUST NOT enviar a operacao e SHALL preservar origem, destino e quantidade para edicao ou nova confirmacao

#### Scenario: Transferencia concluida

- **WHEN** o backend aceitar a transferencia
- **THEN** a interface SHALL substituir os dados de posicao pela resposta da operacao ou por nova leitura autenticada, SHALL manter o saldo exibido inalterado e SHALL apresentar uma mensagem de sucesso

#### Scenario: Erro funcional da transferencia

- **WHEN** o backend rejeitar a transferencia por propriedade, atividade, quantidade ou estado financeiro desatualizado
- **THEN** a interface SHALL apresentar a mensagem funcional retornada, incluindo solicitado/disponivel quando fornecido, SHALL preservar contexto recuperavel e SHALL NOT declarar sucesso local
