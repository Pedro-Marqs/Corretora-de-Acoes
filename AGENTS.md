# Projeto

Este projeto utiliza OpenSpec para especificação e Codex CLI para implementação.

## Fonte de verdade

- Requisitos originais: `docs/`
- Specs atuais: `openspec/specs/`
- Mudanças em andamento: `openspec/changes/`
- Estado da implementação: `tasks.md` da mudança atual
- Continuidade: `docs/continuidade.md`

## Responsabilidades

### OpenSpec

Responsável por:

- entender a próxima tarefa;
- criar e revisar proposal;
- criar delta specs quando necessário;
- criar design quando necessário;
- criar tasks;
- validar a mudança;
- não implementar código da aplicação.

### Codex

Responsável exclusivamente pela implementação.

Deve:

1. Ler este arquivo.
2. Ler `docs/continuidade.md`.
3. Ler proposal, specs, design e tasks da mudança atual.
4. Investigar o código relacionado.
5. Implementar todas as tarefas pendentes que pertençam à unidade lógica atual.
6. Não parar após cada micro-subtarefa.
7. Executar testes focados durante a implementação.
8. Corrigir falhas causadas pela implementação.
9. Atualizar `tasks.md` conforme concluir trabalho comprovado.
10. Atualizar `docs/continuidade.md` somente quando houver decisão ou estado relevante a registrar.
11. Revisar o próprio diff antes de terminar.

Não deve:

- criar outra especificação para substituir o OpenSpec;
- alterar requisitos apenas para acomodar a implementação;
- implementar trabalho fora do change;
- usar subagentes para duplicar investigação ou implementação; revisões e validações independentes só podem usar os agentes personalizados descritos abaixo, em escopo explícito e sem alterar o produto;
- executar `git push`;
- executar `git reset --hard`;
- descartar alterações não relacionadas do usuário;
- arquivar o change.

## Reviewer

O reviewer pode ser executado como subagente interno personalizado do Codex, em contexto separado do implementador, quando o usuário solicitar explicitamente. Ele também pode ser executado pelo fluxo externo do OpenCode.

- Não edita arquivos, não executa testes e não faz staging, commit, push ou archive.
- Inspeciona diretamente o código rastreado e não rastreado, as specs, as tasks e as evidências.
- Emite findings com severidade, arquivo/localização, evidência, impacto e correção recomendada.
- O implementador não pode aprovar o próprio trabalho; o parecer deve ser independente e não fabricado.
- Quando interno, sua natureza deve ser registrada; não se deve alegar que a revisão foi externa ao Codex.

## Tester

O tester pode ser executado como subagente interno personalizado do Codex, em contexto separado do implementador, quando o usuário solicitar explicitamente. Ele também pode ser executado pelo fluxo externo do OpenCode.

- Executa validações reproduzíveis, testes e cobertura sem editar código-fonte ou especificações.
- Não usa contas ou bancos de desenvolvimento reais, nem faz staging, commit, push ou archive.
- Não trata testes ignorados como aprovados e não fabrica evidências.
- Emite relatório com comandos, resultados, limitações e regressões.
- O implementador não pode aprovar o próprio trabalho.

## Conclusão

Uma mudança só está concluída quando:

1. todas as tarefas OpenSpec estão implementadas;
2. reviewer aprova;
3. tester aprova;
4. OpenSpec valida;
5. o change é arquivado.

Se existir bloqueio real, interromper com uma descrição objetiva do bloqueio em vez de improvisar comportamento.

Reviewer e tester internos são mecanismos de apoio do Codex e não equivalem a revisão externa. A task 4.3 só pode ser concluída após pareceres reais de contextos separados e aprovação explícita; problemas encontrados retornam ao implementador para correção.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.

## Fluxo padrão de desenvolvimento

Use as ferramentas conforme o tipo de mudança:

- Correções triviais, documentação, ajustes de instruções ou correções locais sem alteração de comportamento não exigem um change OpenSpec. Ainda assim, devem respeitar este arquivo e receber a validação proporcional ao risco.
- Funcionalidades, alterações de comportamento, contratos, persistência, concorrência, integrações ou mudanças arquiteturais exigem planejamento no OpenSpec antes da implementação.
- Antes de uma alteração com impacto arquitetural, consulte o grafo existente com `graphify query`, usando `graphify path` e `graphify explain` para delimitar dependências, consumidores, testes e possíveis efeitos colaterais.
- Use os resultados do Graphify como evidência técnica para o proposal, design e tasks; eles não substituem requisitos, specs ou decisões do OpenSpec.
- Depois de modificar código, execute `graphify update .` e revise as relações relevantes. Não execute rebuild forçado nem instale ferramentas apenas para atualizar o grafo sem necessidade explícita.

Para mudanças que exigem OpenSpec, siga esta ordem:

1. Ler `AGENTS.md`, `docs/continuidade.md`, as specs relacionadas e os artefatos `proposal.md`, `design.md` e `tasks.md` do change atual.
2. Consultar o Graphify quando a mudança puder afetar arquitetura, contratos, persistência ou integrações.
3. Implementar somente as tasks aprovadas no change.
4. Executar testes focados e corrigir regressões introduzidas pela implementação.
5. Atualizar o grafo após as alterações de código e revisar o diff completo.
6. Deixar a validação final, a aprovação do reviewer/tester e o archive para o fluxo previsto pelo OpenSpec/OpenCode.

Regras operacionais adicionais:

- Não modificar automaticamente `.github/workflows/`, gatilhos de branch ou a configuração atual da CI como parte deste fluxo. Mudanças nesses arquivos exigem escopo explícito.
- Preservar a branch atual e as alterações não relacionadas do usuário. Não executar `git commit`, `git push`, `git reset --hard` ou descarte automático de alterações.
- Não instalar Graphify, OpenSpec ou outras ferramentas se a capacidade necessária já estiver disponível. Se uma ferramenta obrigatória não estiver disponível, relatar o bloqueio antes de alterar o ambiente.
