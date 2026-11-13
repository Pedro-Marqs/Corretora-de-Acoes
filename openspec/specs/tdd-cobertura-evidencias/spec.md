# tdd-cobertura-evidencias Specification

## Purpose
Tornar demonstrável e reproduzível a qualidade das alterações de autenticação, autorização e operações financeiras por meio de TDD, cobertura preservada e evidências auditáveis.

## Requirements

### Requirement: Linha de base de testes e cobertura

Antes de implementar os changes das Aulas 13 e 14, o projeto SHALL registrar versão do código, comandos executados, quantidade de testes, falhas, erros, ignorados, cobertura de linhas/branches e limites configurados. O relatório SHALL distinguir artefato gerado de conclusão validada.

#### Scenario: Baseline reproduzível

- **WHEN** a linha de base for criada em ambiente configurado
- **THEN** outra pessoa SHALL conseguir repetir os comandos e comparar os resultados sem credenciais ou dados sensíveis

### Requirement: Três ciclos Red Green Refactor

O desenvolvimento SHALL registrar pelo menos três ciclos reais, cada um contendo comportamento escolhido, teste inicialmente falhando, saída ou evidência da falha, implementação mínima, teste passando e refatoração posterior. A seleção dos ciclos SHALL seguir riscos e cobertura observados e NÃO SHALL exigir que cada ciclo seja especificamente sobre audience ADMIN/INVESTOR ou fronteiras financeiras. Pelo menos um ciclo SHALL tratar uma fronteira ou valor-limite relevante, e os testes obrigatórios de audience, RBAC, propriedade de conta e finanças SHALL ser mantidos como regressão quando já existirem.

#### Scenario: Ciclo completo

- **WHEN** um ciclo Red Green Refactor for concluído
- **THEN** a evidência SHALL mostrar a falha antes da implementação, a passagem após a implementação e a suíte ainda verde após a refatoração

#### Scenario: Ciclo sem regressão

- **WHEN** a refatoração alterar estrutura sem alterar comportamento
- **THEN** os testes focados e a regressão relacionada SHALL permanecer aprovados

### Requirement: Regressão de identidade e domínio financeiro

O change SHALL preservar e registrar testes de regressão para a separação `Account`/`USER` e `AdminUser`/`ADMIN`, bloqueio de acesso cruzado, respostas `401`/`403`, propriedade de conta, saldo, arredondamento, moedas, câmbio e ausência de mutação após operações rejeitadas. Esses testes SHALL ser classificados separadamente dos ciclos TDD e não SHALL receber uma cronologia RED retroativa quando o comportamento já estiver implementado.

#### Scenario: Regressão existente sem RED histórico

- **WHEN** a implementação já estiver correta e os testes de audience ou financeiros passarem
- **THEN** a evidência SHALL registrar o comportamento coberto, o comando e o resultado, mantendo a task de regressão concluída quando os critérios forem atendidos, sem declarar um ciclo TDD inexistente

### Requirement: Cobertura e fronteiras preservadas

As alterações SHALL manter ou superar os gates atuais de 80% de linhas e 80% de branches no JaCoCo backend e os limiares configurados no frontend. Os testes SHALL cobrir sucesso, falha, ausência, valores-limite, autenticação, autorização, propriedade de conta e operações financeiras relevantes.

#### Scenario: Gate preservado

- **WHEN** `mvn clean verify` e a cobertura frontend configurada forem executados
- **THEN** os gates SHALL passar sem redução de limite, exclusão artificial ou remoção de teste relevante

#### Scenario: Fronteira financeira

- **WHEN** uma entrada atingir limite de quantidade, saldo, moeda, câmbio, data ou autorização
- **THEN** o sistema SHALL rejeitar ou calcular conforme o contrato sem mutação financeira indevida

### Requirement: Mutação controlada e justificativa de mocks

O change SHALL demonstrar pelo menos uma mutação controlada em regra de segurança ou financeira que seja detectada por teste. A evidência SHALL explicar quais dependências externas foram mockadas, por quê, e por que o sistema sob teste não foi substituído por mocks.

#### Scenario: Mutação detectada

- **WHEN** uma condição relevante for temporariamente invertida ou removida em ambiente controlado
- **THEN** pelo menos um teste previsto SHALL falhar e a implementação original SHALL ser restaurada antes da validação final

#### Scenario: Mock justificado

- **WHEN** um teste utilizar mock
- **THEN** o registro SHALL identificar a dependência isolada, o risco de mock excessivo e a razão para manter domínio, serviço ou integração relevante real

### Requirement: Evidências seguras e validação final

Os relatórios SHALL conter comandos, resultados, arquivos/testes envolvidos, cobertura antes/depois, decisões e riscos restantes, sem senhas, tokens, hashes, chaves ou dados pessoais. Reviewer, tester e validação OpenSpec SHALL ser gates distintos da conclusão.

#### Scenario: Evidência sem segredo

- **WHEN** os artefatos de TDD e cobertura forem revisados
- **THEN** eles SHALL permitir auditoria técnica sem conter credenciais ou tokens utilizáveis

#### Scenario: Validação final

- **WHEN** todas as tarefas das Aulas 13, 14 e 15 estiverem implementadas
- **THEN** a conclusão SHALL exigir cobertura aprovada, testes aprovados, reviewer aprovado, tester aprovado e OpenSpec validado

### Requirement: Correspondência com a atividade da Aula 15

O relatório SHALL mapear a seção 21 do material original para evidências do change: baseline JaCoCo, lista de comportamentos e valores de fronteira, pelo menos três ciclos TDD, evolução de branches, justificativa de dublês e mutação detectada. A matriz SHALL distinguir TDD, regressão, caracterização e mutação, e SHALL registrar limitações quando algum item não puder ser comprovado.

#### Scenario: Matriz alinhada ao enunciado

- **WHEN** o relatório da Aula 15 for revisado
- **THEN** cada critério da seção 21 SHALL apontar para uma evidência verificável ou para uma limitação explicitamente registrada, sem promover regressão ou mutação a ciclo TDD

### Requirement: Redefinição operacional segura de senha administrativa

O sistema SHALL oferecer uma redefinição de senha administrativa somente por comando CLI explícito, destinada a um administrador já existente, sem criar, substituir ou reativar contas automaticamente.

#### Scenario: Redefinição usa console e preserva o escopo operacional

- **WHEN** o operador executa explicitamente o comando de redefinição para um e-mail existente e informa a nova senha pelo console interativo
- **THEN** somente o hash BCrypt do administrador identificado SHALL ser atualizado
- **AND** nenhum endpoint HTTP, argumento de processo ou senha permanente de `.env` SHALL participar da operação
- **AND** investidores, contas financeiras e outros administradores SHALL permanecer inalterados

#### Scenario: Sessões administrativas são revogadas e a operação é auditada

- **WHEN** a redefinição de senha é concluída
- **THEN** todos os refresh tokens administrativos ativos do administrador alvo SHALL ser revogados
- **AND** a operação SHALL gerar auditoria com ator `SYSTEM`, alvo administrativo, resultado e sem senha, token ou hash
- **AND** access tokens JWT já emitidos SHALL permanecer válidos até sua expiração natural quando não houver mecanismo separado de revogação imediata

#### Scenario: Entradas inválidas não alteram o estado

- **WHEN** o comando recebe senha ausente, vazia ou um administrador inexistente
- **THEN** a operação SHALL falhar explicitamente
- **AND** nenhum administrador, refresh token ou conta financeira SHALL ser alterado
- **AND** nenhuma credencial sensível SHALL ser registrada

#### Scenario: Configuração administrativa ausente falha explicitamente

- **WHEN** o operador solicita explicitamente a redefinição e o JWT administrativo está desativado
- **THEN** o comando SHALL falhar com erro controlado e explicativo antes de solicitar a senha
- **AND** nenhum administrador, refresh token, auditoria ou conta financeira SHALL ser alterado
- **WHEN** nenhum argumento de redefinição é informado
- **THEN** a inicialização normal SHALL continuar sem exigir o componente de redefinição
