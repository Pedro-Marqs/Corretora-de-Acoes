## Why

A Corretora já possui uma suíte extensa e gates de cobertura, mas a atividade da Aula 15 exige evidência reproduzível de TDD, análise de fronteiras, justificativa de mocks e detecção controlada de mutação. Este change organiza a auditoria e os registros sem reduzir os limites atuais nem alterar o workflow do GitHub Actions.

## What Changes

- Registrar uma linha de base de testes e cobertura antes dos changes de autenticação e RBAC.
- Executar pelo menos três ciclos reais Red → Green → Refactor em comportamentos relevantes escolhidos a partir dos riscos e da cobertura; os ciclos não ficam artificialmente vinculados às tasks de audience ou finanças.
- Preservar os limites JaCoCo atuais de 80% para linhas e branches e os limites V8 existentes no frontend.
- Criar testes de fronteira para JWT, audience/papel, propriedade de conta, moedas, períodos, paginação e operações financeiras.
- Registrar evidência de cada teste inicialmente falhando e da correção que o faz passar.
- Demonstrar detecção de uma mutação controlada sem introduzir ferramenta adicional sem justificativa.
- Documentar por que cada dependência externa é mockada e por que o sistema sob teste permanece real quando necessário.
- Produzir relatórios antes/depois e registrar comandos, resultados, testes ignorados e riscos.
- Preservar testes de regressão obrigatórios para audience ADMIN/INVESTOR, isolamento RBAC e fronteiras financeiras, mesmo quando esses testes não forem ciclos TDD.
- Adicionar, se comprovado por ciclo TDD autêntico, a redefinição operacional segura de senha administrativa exclusivamente por CLI, sem exposição HTTP ou credencial permanente.
- Alinhar explicitamente a atividade à seção 21 do material da Aula 15: lista de comportamentos e fronteiras, pelo menos três ciclos, evolução de branches, justificativa de mocks e mutação detectada.
- Não alterar `.github/workflows/`, limites de cobertura ou regras financeiras neste change; a única alteração de produção permitida é o requisito operacional de reset administrativo por CLI descrito na especificação.

## Decisão metodológica

Em 2026-11-13, o responsável pelo projeto autorizou nesta conversa a adequação da interpretação das tasks 2.2 e 2.3 à seção 21 do material original. A exigência original de três ciclos RED → GREEN → REFACTOR permanece preservada: os ciclos históricos de JWT e refresh token, documentados na Aula 13, e o ciclo contemporâneo de redefinição administrativa por CLI continuam distintos e auditáveis. As tasks 2.2 e 2.3 permanecem regressões obrigatórias de audience/RBAC e fronteiras financeiras, sem receber uma cronologia RED retroativa; a task 2.4 representa o terceiro ciclo TDD. Essa decisão é interna, registrada pelo responsável nesta conversa, e não constitui alegação de aprovação individual do professor.

## Capabilities

### New Capabilities

- `tdd-cobertura-evidencias`: processo verificável para linha de base, ciclos TDD, mutação, cobertura e documentação de validação.

### Modified Capabilities

- Nenhuma.

## Impact

Afeta a organização dos testes backend/frontend, a operação CLI administrativa e os documentos de evidência dos changes das Aulas 13 e 14. Pode exigir novos testes, pequenos ajustes de produção descobertos pelos ciclos TDD e relatórios locais; não altera o workflow atual nem reduz gates. A Aula 15 é transversal: o baseline precede a Aula 13, seus ciclos acompanham as implementações e o relatório final depende da conclusão da Aula 14.
