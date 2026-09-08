## 1. Serviço, rota e contrato de consulta

- [x] 1.1 Criar o serviço frontend para `GET /api/history`, mapeando página zero-based, filtros permitidos e metadados sem enviar tamanho controlável; verificar com testes de serviço que os parâmetros combinados são serializados corretamente e erros são normalizados
- [x] 1.2 Adicionar a rota privada e o destino de navegação da tela de histórico, reutilizando proteção de sessão e layout existentes; verificar redirecionamento para login em sessão ausente ou inválida

## 2. Tela de histórico e filtros

- [x] 2.1 Implementar filtros de intervalo, tipo, ticker, corretora e mercado com rótulos acessíveis e validação básica; verificar que aplicar ou alterar filtro reinicia a página e preserva os demais valores
- [x] 2.2 Implementar lista/tabela responsiva com apresentação condicional de compra, venda, aporte e transferência, datas e valores formatados pelos utilitários existentes; verificar que nenhum controle de edição ou exclusão aparece
- [x] 2.3 Implementar paginação usando os metadados oficiais, mantendo filtros ao mudar de página e descartando respostas obsoletas; verificar navegação com mais de uma página e limite máximo de 20 itens exibidos

## 3. Estados, acessibilidade e validação

- [x] 3.1 Cobrir carregamento, vazio, erro recuperável, nova tentativa e HTTP 401, preservando filtros e ocultando dados privados quando necessário; verificar mensagens sem detalhes técnicos e bloqueio de solicitações duplicadas
- [x] 3.2 Verificar a tela em 320 px, tablet e desktop, incluindo foco, associação de rótulos/erros e ausência de rolagem horizontal; executar testes frontend focados, ESLint, build Vite e `git diff --check`
