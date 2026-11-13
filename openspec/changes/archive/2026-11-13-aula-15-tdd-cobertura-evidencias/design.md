## Context

O backend já executa JaCoCo em `verify` com mínimo de 80% para linhas e branches; o frontend possui limiares V8 de 80% para linhas, branches, funções e statements. A suíte contém testes unitários, de controller, integração, rollback, concorrência, segurança e operações financeiras. O workflow atual não será alterado por este change.

## Goals / Non-Goals

**Goals:**

- Criar evidências reproduzíveis antes, durante e depois dos changes de segurança.
- Concentrar os ciclos TDD em comportamentos de alto risco: tokens, audience/papel, propriedade e fronteiras financeiras.
- Preservar os gates atuais e tornar explícitas lacunas de testes ignorados ou dependentes de ambiente.
- Demonstrar uma mutação detectada sem obrigar a adoção de PIT ou outra ferramenta nova.

**Non-Goals:**

- Não reduzir JaCoCo, excluir classes sem justificativa ou remover testes para fazer o gate passar.
- Não alterar `.github/workflows/ci.yml`.
- Não transformar relatório de cobertura em objetivo único nem testar detalhes internos sem comportamento observável.
- Não registrar credenciais, tokens, hashes, CPF ou e-mail completo nas evidências.

**Dependências:**

- O baseline da Aula 15 não depende de código novo e deve ser executado primeiro.
- Os ciclos de autenticação dependem da Aula 13.
- O ciclo de separação de identidades usa `Account`/`USER` versus `AdminUser`/`ADMIN` e depende da Aula 14.
- A consolidação final ocorre somente depois das duas mudanças de segurança.

## Decisions

- **Evidências versionáveis em Markdown:** registrar resumos e comandos em arquivos da mudança; não versionar `target/`, `coverage/`, HTML gerado ou tokens. Isso permite revisão sem transformar artefatos temporários em fonte de verdade.
- **Três ciclos independentes dos temas de regressão:** a seção 21 do material do professor exige pelo menos três ciclos reais, mas não exige que cada ciclo seja sobre audience ADMIN/INVESTOR ou finanças. Os ciclos podem ser escolhidos entre comportamentos relevantes de autenticação, refresh, validação, domínio ou integração, desde que cada um tenha RED, GREEN e REFACTOR verificáveis. Os testes de audience/RBAC e fronteiras financeiras continuam obrigatórios como regressão e caracterização, mesmo quando já implementados.
- **Mutação manual controlada:** inverter temporariamente uma condição de autorização ou arredondamento, executar o teste que deve detectar, registrar a saída resumida e restaurar o código. Alternativa de PIT fica fora até existir justificativa de custo e compatibilidade.
- **Matriz de mocks:** mockar somente rede, relógio quando necessário, encoder/serviço externo em testes unitários e falhas deliberadamente injetadas; manter banco, transação, controllers e serviços reais em testes de integração apropriados.
- **Cobertura backend e frontend separadas:** JaCoCo continua sendo o gate backend; `npm run test:coverage` gera a verificação frontend localmente quando o escopo incluir interface. Nenhum workflow será modificado neste planejamento.
- **Comparação de baseline/final:** usar os mesmos comandos e ambiente sempre que possível, explicando diferenças de testes ignorados, PostgreSQL local e variáveis opt-in.
- **Adequação ao enunciado:** a seção 21 será atendida por uma matriz que separa ciclos TDD, regressões, fronteiras, evolução de branches, mocks e mutação. As tasks de audience e finanças não serão usadas para inventar RED retrospectivo; sua evidência será a suíte comportamental existente e seus resultados reproduzíveis.

- **Terceiro ciclo: recuperação operacional administrativa:** a oportunidade aprovada é uma redefinição segura de senha administrativa por CLI. O comando será explícito e independente do bootstrap: aceitará apenas o e-mail de um administrador existente e lerá a nova senha por `Console.readPassword`, sem senha em argumento ou `.env` permanente. O serviço transacional localizará o administrador com lock, persistirá somente o hash BCrypt, revogará os refresh tokens administrativos do alvo e registrará auditoria sem dados sensíveis. O estado `ACTIVE`/`INACTIVE` não será alterado e access tokens já emitidos conservarão a validade residual documentada até a expiração.

### Decisão metodológica registrada em 2026-11-13

O responsável pelo projeto autorizou nesta conversa a adequação da leitura das tasks 2.2 e 2.3 à seção 21 do material original. O requisito original de pelo menos três ciclos RED → GREEN → REFACTOR permanece integral; os dois ciclos históricos autênticos da Aula 13 (JWT e refresh token) e o terceiro ciclo autêntico desta Aula 15 (reset administrativo por CLI) são mantidos. Audience ADMIN/INVESTOR, RBAC, propriedade e fronteiras financeiras continuam regressões obrigatórias nas tasks 2.2 e 2.3, mas não recebem uma cronologia RED retroativa. A task 2.4 representa o terceiro ciclo contemporâneo. Esta é uma decisão interna do projeto, registrada pelo responsável nesta conversa; não é apresentada como aprovação individual do professor nem como autorização acadêmica externa.

## Risks / Trade-offs

- [Branches próximos do limite podem falhar após mudanças legítimas] → adicionar testes de comportamento, nunca reduzir o gate, e revisar relatório por pacote/classe.
- [Evidência Red ser inventada depois] → capturar saída no momento de cada ciclo e associar teste, arquivo e decisão ao change correspondente; não reclassificar regressão ou mutação como TDD.
- [Mocks esconderem defeitos de integração] → combinar testes unitários com integração real de Spring, banco e segurança nos fluxos críticos.
- [Mutação temporária contaminar o working tree] → executar em cópia de trabalho controlada ou bloco isolado, registrar antes/depois e confirmar `git diff` limpo da mutação.
- [Dados sensíveis em logs de teste] → usar fixtures sintéticas, mascaramento e revisão dos artefatos antes do reviewer.

## Migration Plan

1. Executar baseline e registrar evidências sem modificar código de produção.
2. Durante a implementação da Aula 13, concluir o ciclo de token e atualizar evidências.
3. Durante a implementação da Aula 14, concluir o ciclo de RBAC/isolamento e o ciclo financeiro/moeda.
4. Executar a mutação controlada e restaurar integralmente o código.
5. Gerar relatórios finais backend/frontend, revisar diffs e anexar resultados resumidos.
6. Submeter reviewer, tester e validação OpenSpec; somente então considerar a trilha de evidências concluída.
