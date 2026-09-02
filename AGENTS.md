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
- usar subagentes para duplicar investigação ou implementação;
- executar `git push`;
- executar `git reset --hard`;
- descartar alterações não relacionadas do usuário;
- arquivar o change.

## Reviewer

O reviewer é externo ao Codex e executado pelo OpenCode.

O Codex não precisa criar ou chamar reviewer próprio.

## Tester

A suíte final é executada pelo agente tester do OpenCode.

O Codex deve executar apenas os testes focados necessários durante a implementação.

## Conclusão

Uma mudança só está concluída quando:

1. todas as tarefas OpenSpec estão implementadas;
2. reviewer aprova;
3. tester aprova;
4. OpenSpec valida;
5. o change é arquivado.

Se existir bloqueio real, interromper com uma descrição objetiva do bloqueio em vez de improvisar comportamento.
```
