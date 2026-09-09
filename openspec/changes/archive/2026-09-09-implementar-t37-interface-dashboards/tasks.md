## 1. Serviço e composição da tela

- [x] 1.1 Integrar o serviço frontend ao `GET /api/dashboard`, enviando visão de corretora e período selecionados e verificando por testes que a resposta oficial substitui o estado exibido sem recálculo local.
- [x] 1.2 Criar a rota/página privada do dashboard e compor indicadores, posições, saldo compartilhado, avisos e estado de dados; verificar renderização com respostas completas e sem posições.

## 2. Filtros e visualizações

- [x] 2.1 Implementar alternância entre visão geral e corretora própria ativa, preservando o saldo compartilhado; verificar nova consulta, isolamento visual e prevenção de envio duplicado.
- [x] 2.2 Implementar distribuições por ativo, corretora e mercado com valores em BRL; verificar parcelas vazias, valores formatados e ausência de percentuais divergentes.
- [x] 2.3 Implementar gráfico/lista da série patrimonial e seletor dos períodos `4W`, `3M`, `6M`, `1Y`, `5Y` e `MAX`; verificar ordem, datas em Brasília, série vazia e cobertura parcial sem interpolação.

## 3. Estados e validação final

- [x] 3.1 Tratar carregamento, erro recuperável, sessão 401, vazio e desatualização preservando contexto e mensagens seguras; verificar cada estado com respostas simuladas.
- [x] 3.2 Garantir responsividade desde 320 px, foco e acessibilidade dos filtros e seções, executar testes frontend, ESLint, build Vite, `git diff --check` e `openspec validate implementar-t37-interface-dashboards --strict`. Verificado: 25 arquivos e 260 testes frontend aprovados, ESLint sem avisos, build Vite aprovado, `git diff --check` aprovado e validação OpenSpec estrita aprovada.
