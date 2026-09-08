## 1. Contrato e seleção do período

- [x] 1.1 Estender `GET /api/dashboard` com o período histórico opcional e a coleção de pontos patrimoniais, preservando o contrato e os indicadores atuais; verificar compatibilidade da consulta sem período.
- [x] 1.2 Validar os valores `4W`, `3M`, `6M`, `1Y`, `5Y` e `MAX`, rejeitando período ausente quando exigido, valor inválido e ausência de autenticação com o contrato uniforme; verificar que nenhuma conta ou ponto é exposto.

## 2. Consulta da série patrimonial

- [x] 2.1 Implementar o cálculo dos limites usando o relógio de Brasília, criação da conta para `MAX` e primeiro ponto disponível para cobertura parcial; verificar cada período com datas controladas.
- [x] 2.2 Consultar pontos somente da conta autenticada no intervalo, ordenar cronologicamente e retornar instante e valor patrimonial em BRL sem interpolação; verificar isolamento, série vazia e ordenação.

## 3. Validação integrada

- [x] 3.1 Comprovar que atualizações isoladas de cotação não geram pontos e que a leitura da série não altera movimentações, patrimônio ou saldo; verificar conta sem pontos e períodos sem cobertura.
- [x] 3.2 Executar testes focados de dashboard, suíte backend aplicável, compilação/empacotamento Maven, `git diff --check` e `openspec validate implementar-t36-serie-historica-patrimonial --strict`, corrigindo somente falhas do escopo. Verificado: testes focados 24/24, suíte backend 307 testes sem falhas, `test package` aprovado, Reviewer aprovado, `git diff --check` aprovado e `openspec validate --strict` aprovado.
