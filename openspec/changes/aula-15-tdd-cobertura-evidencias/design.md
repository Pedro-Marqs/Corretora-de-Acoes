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
- **Três ciclos distribuídos:** ciclo 1 para autenticação/token inválido na Aula 13, ciclo 2 para isolamento `Account`/`USER` versus `AdminUser`/`ADMIN` na Aula 14 e ciclo 3 para fronteira financeira ou moeda. Cada ciclo terá teste focalizado, falha registrada, implementação mínima e refatoração.
- **Mutação manual controlada:** inverter temporariamente uma condição de autorização ou arredondamento, executar o teste que deve detectar, registrar a saída resumida e restaurar o código. Alternativa de PIT fica fora até existir justificativa de custo e compatibilidade.
- **Matriz de mocks:** mockar somente rede, relógio quando necessário, encoder/serviço externo em testes unitários e falhas deliberadamente injetadas; manter banco, transação, controllers e serviços reais em testes de integração apropriados.
- **Cobertura backend e frontend separadas:** JaCoCo continua sendo o gate backend; `npm run test:coverage` gera a verificação frontend localmente quando o escopo incluir interface. Nenhum workflow será modificado neste planejamento.
- **Comparação de baseline/final:** usar os mesmos comandos e ambiente sempre que possível, explicando diferenças de testes ignorados, PostgreSQL local e variáveis opt-in.

## Risks / Trade-offs

- [Branches próximos do limite podem falhar após mudanças legítimas] → adicionar testes de comportamento, nunca reduzir o gate, e revisar relatório por pacote/classe.
- [Evidência Red ser inventada depois] → capturar saída no momento de cada ciclo e associar teste, arquivo e decisão ao change correspondente.
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
