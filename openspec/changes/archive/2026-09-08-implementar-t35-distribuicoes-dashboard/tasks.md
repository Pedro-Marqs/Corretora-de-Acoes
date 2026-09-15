## 1. Contrato e escopo da consulta

- [x] 1.1 Estender `GET /api/dashboard` para aceitar o filtro opcional de corretora e retornar visão geral ou visão filtrada sem quebrar o contrato existente; verificar resposta HTTP, saldo compartilhado e compatibilidade da consulta sem filtro.
- [x] 1.2 Validar que a corretora informada pertence à conta autenticada e está ativa, rejeitando seleção de outra conta ou corretora inativa com o contrato uniforme; verificar autenticação, isolamento e ausência de vazamento.

## 2. Indicadores e distribuições

- [x] 2.1 Restringir posições, resultados e valores de mercado à visão selecionada antes da composição dos indicadores, reutilizando conversão USD/BRL e estados de desatualização existentes; verificar duas corretoras com o mesmo ativo e saldo inalterado.
- [x] 2.2 Implementar agregações numéricas em BRL por ativo, corretora e mercado a partir das posições abertas consolidadas; verificar parcelas separadas, posições USD e soma das parcelas compatível com o total dentro do arredondamento financeiro.

## 3. Validação integrada

- [x] 3.1 Cobrir conta sem posições, conta com múltiplas corretoras, seleção inválida, duas contas e ausência de câmbio utilizável; verificar estados vazios, erros funcionais e nenhum efeito em movimentações ou pontos patrimoniais.
- [x] 3.2 Executar testes focados do dashboard, suíte backend aplicável, compilação/empacotamento Maven, `git diff --check` e `openspec validate implementar-t35-distribuicoes-dashboard --strict`, corrigindo falhas sem ampliar o escopo. Verificado: `DashboardControllerTests` 13/13, suíte backend 296 testes sem falhas, `test package` com sucesso, reviewer aprovado, `git diff --check` e validação OpenSpec estrita aprovados.
