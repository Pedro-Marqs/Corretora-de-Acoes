## 1. Contrato e domínio

- [x] 1.1 Mapear o contrato HTTP autenticado e os erros uniformes da transferência, aceitando apenas origem, destino, ativo e quantidade, e verificar por testes de controller que sessão/CSRF, campos inválidos e recursos de outra conta não expõem dados nem alteram estado.
- [x] 1.2 Integrar o núcleo financeiro de transferência ao caso de uso persistente, cobrindo destino vazio/existente, transferência parcial/total, conservação de custo e média ponderada com testes unitários determinísticos.

## 2. Persistência transacional

- [x] 2.1 Implementar a atualização atômica de origem e destino com associações próprias ativas, revalidação sob lock e ordenação determinística dos bloqueios; verificar no teste de integração que propriedade, atividade, quantidade e mesma corretora são rejeitadas sem alterações.
- [x] 2.2 Registrar movimentação de transferência e ponto patrimonial pós-operação sem resultado ou alteração de saldo; verificar que a falha injetada em cada etapa produz rollback integral e nenhum evento parcial.

## 3. Concorrência e regressões

- [x] 3.1 Criar testes concorrentes para transferências cruzadas e para transferência simultânea com compra/venda, verificando que não há consumo duplicado, perda de custo, deadlock não tratado ou saldo alterado.
- [x] 3.2 Executar a suíte focada de carteira/histórico/saldo, compilação e validações do backend, revisar o diff e confirmar `git diff --check` e `openspec validate --strict` para o change.
