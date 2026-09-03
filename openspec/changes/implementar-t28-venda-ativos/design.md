## Context

A T27 já fornece a operação de compra transacional, o núcleo financeiro da T26, posições persistidas por conta/corretora e o registro interno de movimentações e patrimônio. A venda deve reutilizar esses contratos, mantendo o saldo único em reais, a sessão como autoridade da conta e as cotações armazenadas como fonte financeira.

## Goals / Non-Goals

**Goals:**

- Implementar um caso de uso de venda parcial ou total com cálculo financeiro determinístico.
- Garantir revalidação concorrente da posição e atomicidade entre saldo, posição, histórico e patrimônio.
- Cobrir ativos brasileiros e norte-americanos, inclusive fallback e conversão USD/BRL.

**Non-Goals:**

- Criar ordem, preço definido pelo usuário, taxa, imposto, comissão ou estorno.
- Implementar interface frontend, transferência, consulta de histórico ou dashboards.
- Alterar as regras financeiras já sincronizadas ou criar nova migração sem necessidade comprovada.

## Decisions

- **Cotação antes da transação:** resolver a cotação do ativo e o câmbio necessário antes de abrir a transação curta, como na compra. A operação usa o valor atual ou o cache utilizável e transporta os instantes originais; falha sem valor utilizável bloqueia a venda.
- **Autoridade da sessão e propriedade:** derivar a conta do principal autenticado e buscar a posição por conta, associação ativa da corretora e ativo. Identificadores enviados pelo cliente são apenas seleção e não autorizam acesso cruzado.
- **Revalidação sob bloqueio:** dentro da transação, revalidar conta ativa, associação, posição e quantidade disponível antes de creditar e reduzir. O bloqueio impede duas vendas concorrentes de consumirem a mesma quantidade.
- **Regra financeira única:** delegar quantidade restante, custo remanescente, preço médio e resultado realizado ao núcleo puro da T26. A venda reduz o custo pelo preço médio vigente; venda total encerra a posição aberta, sem apagar seu histórico.
- **Registro atômico:** atualizar saldo e posição e chamar o registro de movimentação/ponto patrimonial na mesma unidade transacional. Qualquer falha propaga erro e desfaz crédito, posição e registros.
- **Contrato financeiro do backend:** o preço, valor em moeda original, conversão e valor em reais são derivados da cotação resolvida no servidor. Payloads com preço não são fonte financeira nem são persistidos.

Alternativas rejeitadas: reservar posição fora da transação (aumentaria estados intermediários), confiar na quantidade retornada ao cliente (permitiria venda obsoleta) ou recalcular resultado no frontend (quebraria a autoridade financeira do backend).

## Risks / Trade-offs

- **[Concorrência entre vendas]** → bloquear e revalidar a posição na transação; testar vendas simultâneas e garantir que uma seja rejeitada sem efeitos parciais.
- **[Cotação externa indisponível]** → usar somente cache válido conforme o contrato e retornar erro funcional quando não houver valor utilizável.
- **[Falha de histórico ou patrimônio]** → manter os registros no chamador transacional e testar rollback após cada etapa de persistência.
- **[Posição zerada ainda persistida]** → manter a linha histórica sem quantidade aberta e garantir que consultas operacionais filtrem quantidade maior que zero.

## Migration Plan

Não há migração de banco prevista: a venda usa tabelas, constraints e registros existentes. A implantação consiste em disponibilizar o novo caso de uso e seus testes; rollback é remover/desabilitar a rota e o serviço sem modificar dados já persistidos.
