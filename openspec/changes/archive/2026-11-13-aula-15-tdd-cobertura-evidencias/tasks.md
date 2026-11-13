## 1. Linha de base

- [x] 1.1 Executar e registrar baseline backend com `mvn clean verify`, incluindo testes, falhas, erros, ignorados, JaCoCo de linhas/branches e limites atuais; verificar ausência de segredos no relatório.
- [x] 1.2 Executar e registrar baseline frontend com testes, lint/build e `npm run test:coverage` quando aplicável; verificar os quatro limiares V8 sem alterar CI.
- [x] 1.3 Criar matriz de comportamentos prioritários, fronteiras e testes existentes para JWT, `Account`/`USER`, `AdminUser`/`ADMIN`, propriedade, compras, vendas, transferências, moedas, câmbio, período e paginação.

## 2. Ciclos TDD

- [x] 2.1 Executar ciclo Red → Green → Refactor para token ausente, inválido, adulterado ou expirado; registrar falha inicial, implementação mínima, refatoração e comandos.
- [x] 2.2 Validar e registrar regressão da separação de audience entre `Account`/`USER` e `AdminUser`/`ADMIN`, com bloqueio de acesso cruzado e evidências de `401`/`403`; essa regressão não precisa ser reclassificada como ciclo TDD quando o comportamento já estava implementado.
- [x] 2.3 Validar e registrar regressão das fronteiras financeiras e monetárias, como propriedade da conta, moeda/câmbio, saldo, arredondamento e ausência de mutação indevida; essa regressão não precisa ser reclassificada como ciclo TDD quando o comportamento já estava implementado.
- [x] 2.4 Executar um terceiro ciclo TDD legítimo para redefinição operacional segura de senha administrativa por CLI, com RED antes das classes de produção, GREEN mínimo e REFACTOR verificável; localizar somente administrador existente, persistir BCrypt, revogar refresh tokens e auditar sem dados sensíveis, sem endpoint público, senha em argumento/.env ou alteração financeira.

## 3. Mutação e mocks

- [x] 3.1 Escolher uma condição relevante de segurança ou cálculo, executar mutação temporária e registrar o teste que falha; verificar restauração integral do código original.
- [x] 3.2 Documentar justificativa de cada mock novo ou alterado, distinguindo dependência externa, relógio, falha injetada e sistema sob teste real; verificar que não há mock do comportamento testado.

## 4. Relatórios finais

- [x] 4.1 Executar novamente os comandos de cobertura e comparar baseline/final por linhas, branches e limiares; verificar que nenhum gate foi reduzido ou artificialmente excluído.
- [x] 4.2 Consolidar evidências dos três ciclos, mutação, mocks, testes de fronteira, ignorados e riscos em Markdown seguro; verificar ausência de senhas, tokens, hashes e dados pessoais.
- [x] 4.3 Executar Graphify após os changes de código, revisar diff e submeter a trilha completa ao reviewer, tester e `openspec validate --strict`; verificar todos os gates antes de marcar tarefas.
