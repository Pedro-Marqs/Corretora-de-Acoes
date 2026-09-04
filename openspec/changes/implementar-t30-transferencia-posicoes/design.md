## Context

A aplicação já possui associações de corretora, posições, núcleo financeiro de transferência da T26 e operações transacionais de compra/venda. A transferência precisa usar a conta da sessão, preservar a precisão financeira existente e participar da mesma transação que o histórico e o ponto patrimonial.

## Goals / Non-Goals

**Goals:**

- Expor um caso de uso HTTP autenticado para transferir uma posição entre associações próprias ativas.
- Serializar alterações concorrentes da posição de origem e manter origem, destino, histórico e patrimônio consistentes.
- Reutilizar as regras financeiras existentes para custo e preço médio, sem cotação externa ou alteração de saldo.

**Non-Goals:**

- Interface frontend, consulta paginada do histórico ou novas regras de dashboard.
- Transferência entre contas, corretoras inativas, ativos diferentes, taxas, liquidação ou conversão cambial.
- Alteração do modelo de saldo ou criação de uma movimentação financeira de débito/crédito.

## Decisions

- **Caso de uso transacional único:** validar a sessão e os identificadores, bloquear/revalidar as associações e as posições e persistir as duas posições, a movimentação e o ponto patrimonial no mesmo limite transacional. Alternativas rejeitadas: duas transações compensatórias, que permitem estado parcial, e chamada assíncrona, que não é necessária para a operação local.
- **Ordenação determinística de bloqueios:** quando houver duas posições, adquirir os bloqueios em ordem estável de identificador, independentemente de origem/destino. Isso reduz deadlocks quando transferências concorrentes usam posições relacionadas; falhas de lock/conflito devem resultar em erro sem alterações parciais.
- **Revalidação sob lock:** a quantidade disponível e a propriedade/atividade das associações são verificadas novamente depois dos bloqueios. A validação inicial serve apenas para resposta rápida e não é fonte de verdade.
- **Sem cotação:** a transferência move quantidade e custo contábil pelo preço médio vigente; não consulta mercado, não calcula resultado e não altera saldo. O ponto patrimonial é calculado com o estado pós-transferência e os valores de mercado disponíveis já persistidos, conforme o serviço patrimonial existente.
- **Identificação da operação:** o payload aceita somente identificadores opacos de ativo, origem, destino e quantidade. Preço, custo, saldo e conta enviados pelo cliente não têm autoridade e não serão aceitos como entrada financeira.
- **Posição encerrada:** transferência total deixa a origem fechada/sem posição aberta; o destino vazio é criado ou reativado conforme o modelo atual, preservando histórico anterior quando aplicável.

## Risks / Trade-offs

- **[Concorrência com compra/venda]** operações simultâneas podem disputar a mesma posição → usar o mesmo mecanismo de bloqueio/revalidação das operações existentes e testar transferências cruzadas.
- **[Falha no registro posterior]** histórico ou patrimônio pode falhar após a alteração das posições → manter todos os writes no mesmo limite transacional e testar rollback forçado.
- **[Arredondamento acumulado]** divisão de custo transferido pode introduzir diferença → aplicar as regras financeiras centralizadas e verificar conservação do custo total com a precisão persistida.
- **[Resposta de recurso de outra conta]** identificadores podem ser usados para sondagem → consultar sempre com escopo da conta e retornar erro uniforme sem dados de terceiros.

## Migration Plan

Não é necessária migração de banco: o tipo de movimentação e as estruturas de posição/patrimônio existentes devem ser reutilizados. Implantar o caso de uso e seus testes; em rollback, remover a rota/serviço novo sem alterar dados existentes, pois nenhuma escrita parcial deve sobreviver a uma falha.
