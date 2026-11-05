## Purpose

Tornar demonstrável e reproduzível a qualidade das alterações de autenticação, autorização e operações financeiras por meio de TDD, cobertura preservada e evidências auditáveis.

## ADDED Requirements

### Requirement: Linha de base de testes e cobertura

Antes de implementar os changes das Aulas 13 e 14, o projeto SHALL registrar versão do código, comandos executados, quantidade de testes, falhas, erros, ignorados, cobertura de linhas/branches e limites configurados. O relatório SHALL distinguir artefato gerado de conclusão validada.

#### Scenario: Baseline reproduzível

- **WHEN** a linha de base for criada em ambiente configurado
- **THEN** outra pessoa SHALL conseguir repetir os comandos e comparar os resultados sem credenciais ou dados sensíveis

### Requirement: Três ciclos Red Green Refactor

O desenvolvimento SHALL registrar pelo menos três ciclos reais, cada um contendo comportamento escolhido, teste inicialmente falhando, saída ou evidência da falha, implementação mínima, teste passando e refatoração posterior. Os ciclos SHALL cobrir segurança/autorização e ao menos uma fronteira financeira ou de moeda.

#### Scenario: Ciclo completo

- **WHEN** um ciclo Red Green Refactor for concluído
- **THEN** a evidência SHALL mostrar a falha antes da implementação, a passagem após a implementação e a suíte ainda verde após a refatoração

#### Scenario: Ciclo sem regressão

- **WHEN** a refatoração alterar estrutura sem alterar comportamento
- **THEN** os testes focados e a regressão relacionada SHALL permanecer aprovados

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
