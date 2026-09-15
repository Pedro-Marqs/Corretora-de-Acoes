## Context

O modelo persistido já contém conta, associação de corretora, ativo, cotação, câmbio, posição, movimentação e ponto patrimonial. A T14 fornece registro financeiro obrigatório dentro da transação chamadora, a T23 fornece dados de mercado/cache e a T26 fornece transformações puras de posição e conversão. A compra precisa compor esses serviços sem transportar DTO externo ou cálculo oficial para o controller.

## Goals / Non-Goals

**Goals:**

- Expor um caso de uso autenticado de compra que valide e revalide todos os vínculos da conta.
- Fazer cotação externa/cache antes da transação e executar débito, posição, histórico e patrimônio numa única transação curta.
- Proteger concorrência sobre o saldo e a posição, mantendo saldo não negativo e evitando movimentação duplicada por corrida.
- Retornar ao consumidor somente valores calculados pelo backend e metadados temporais necessários.

**Non-Goals:**

- Não implementar venda, transferência, tela frontend, consulta de posições ou histórico paginado.
- Não aceitar preço, câmbio ou resultado calculado pelo cliente.
- Não criar novos provedores, atualizar cotações durante a transação ou registrar ponto em atualização isolada de mercado.

## Decisions

### Fluxo em duas fases

O caso de uso valida formato e resolve ativo, cotação/câmbio e corretora antes de abrir a transação. Dentro da transação, recarrega a conta com bloqueio apropriado, confirma que a conta está ativa, confirma a associação ativa pertencente à conta e revalida saldo e existência/estado da posição. Isso segue a decisão de chamadas externas antes da transação, mas evita usar um saldo ou vínculo obsoleto. Manter a chamada externa dentro da transação foi rejeitado por aumentar locks e duração; confiar somente na pré-validação foi rejeitado por permitir corrida.

### Fonte financeira e conversão

O serviço de mercado devolve o valor armazenado ou atual utilizável e seus instantes; o preço enviado no request não participa do fluxo. Para ativo norte-americano, o custo é calculado pelo núcleo da T26 com USD/BRL utilizável, sem tarifa ou spread. Dados antigos continuam utilizáveis conforme as regras de fallback, mas são marcados no resultado; ausência de qualquer valor necessário bloqueia antes de alterar o banco.

### Atualização atômica

O débito usa o valor total normalizado por `FinancialAmount`. A posição é criada ou atualizada pela operação de compra da T26. Somente depois de ambas as alterações válidas o registro interno da T14 cria movimentação e ponto patrimonial; qualquer exceção propaga rollback. O registro recebe o preço e valor efetivamente calculados, nunca o payload original.

### Concorrência e propriedade

A conta será carregada para atualização para serializar compras concorrentes que compartilham saldo. A posição existente será atualizada sob a proteção correspondente e a unicidade natural conta-corretora-ativo será respeitada. Se a revalidação falhar, a operação termina com erro funcional sem revelar dados de outra conta. Uma alternativa baseada em leitura sem lock foi rejeitada porque duas compras poderiam ultrapassar o saldo.

### Contrato HTTP e erros

O controller aceitará somente os campos necessários à operação (ativo, corretora e quantidade), delegará a sessão ao identificador autenticado e devolverá o saldo/posição resultantes e os dados de cotação usados conforme os contratos existentes. Preço livre, identificador de conta e valores financeiros derivados não serão autoridade; se enviados, serão ignorados ou rejeitados por contrato, sem persistência. Validações, saldo insuficiente, vínculos inválidos e indisponibilidade de mercado usarão as categorias de erro uniformes já existentes, incluindo solicitado/disponível quando aplicável.

### Verificação

Testes unitários cobrirão o cálculo e a composição do caso de uso com mocks. Testes de integração H2 comprovarão primeira compra, média ponderada, mercados BR/US, isolamento e rollback; o fluxo crítico PostgreSQL local opt-in será usado quando as credenciais de teste estiverem disponíveis, sem tocar no banco principal.

## Risks / Trade-offs

- **Cotação pode envelhecer entre consulta e confirmação** → revalidar a existência/identidade do ativo e persistir o instante usado; não prometer preço em tempo real além da política de validade existente.
- **Compras concorrentes podem disputar saldo ou posição** → bloquear a conta na transação e testar duas solicitações concorrentes, garantindo que nenhuma deixe saldo negativo.
- **Falha no histórico após débito** → manter o registro com propagação de falha e rollback transacional, cobrindo falha intermediária em teste.
- **Cliente tentar impor preço ou conta** → limitar o DTO à intenção da operação e derivar conta da sessão; confirmar em teste que campos manipulados não alteram o resultado.

## Migration Plan

Não há migração estrutural prevista. A implementação adicionará o caso de uso sobre tabelas e serviços existentes; implantação é compatível com dados atuais. Para rollback, remover a rota/uso do caso de uso sem alterar registros já persistidos, preservando movimentações e posições válidas.

## Open Questions

Nenhuma. O endpoint e os DTOs podem seguir as convenções existentes sem alterar o comportamento especificado.
