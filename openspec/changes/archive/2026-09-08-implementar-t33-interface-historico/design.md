## Context

A T32 disponibiliza `GET /api/history` com paginação fixa, filtros combináveis, ordenação determinística e isolamento por sessão. A T33 deve consumir esse contrato existente no frontend, integrando-se às rotas privadas e aos estados compartilhados já criados, sem duplicar cálculos ou regras do backend.

## Goals / Non-Goals

**Goals:**

- Criar uma tela privada de histórico com filtros e paginação controlada pelo backend.
- Reutilizar o cliente HTTP, formatação, navegação e tratamento de sessão existentes.
- Apresentar uma leitura responsiva e somente leitura dos movimentos.

**Non-Goals:**

- Alterar o endpoint, a persistência ou as regras de paginação da T32.
- Calcular saldo, resultado, preço ou conversão no frontend.
- Criar edição, exclusão, exportação ou resumo financeiro adicional.

## Decisions

- **Rota e navegação:** adicionar a página à área privada e à navegação somente se a rota já possuir destino funcional; a proteção seguirá o mecanismo de sessão existente.
- **Estado da consulta:** manter filtros e página em estado local da tela, reiniciar `page` para zero quando filtros mudarem e enviar apenas parâmetros aceitos pelo contrato da T32. O tamanho da página não será enviado pelo cliente.
- **Apresentação:** usar uma tabela em larguras confortáveis e uma composição responsiva em viewport estreito, preservando legibilidade sem impor rolagem horizontal à página. Campos condicionais serão renderizados conforme o tipo e o payload devolvido.
- **Atualização de dados:** substituir a lista somente após resposta válida; após erro, preservar filtros e permitir nova tentativa. HTTP 401 usará o fluxo de sessão já existente, sem manter dados privados visíveis.
- **Acessibilidade e segurança:** associar rótulos aos filtros, indicar estados de carregamento e erro, manter foco utilizável e não renderizar mensagens técnicas nem identificadores internos.

Alternativas consideradas: recalcular ou agrupar o histórico no cliente foi rejeitado por duplicar a fonte de verdade; carregar todos os registros e paginar localmente foi rejeitado pelo limite e pelos metadados oficiais da API; criar uma tela de edição foi rejeitado pela imutabilidade do domínio.

## Risks / Trade-offs

- **[Risco]** Muitos campos tornam a tabela estreita em celular. → Usar apresentação responsiva por registro e manter apenas campos aplicáveis visíveis.
- **[Risco]** Respostas fora de ordem podem sobrescrever filtros mais recentes. → Associar a resposta à consulta vigente e ignorar respostas obsoletas.
- **[Risco]** Filtros inválidos podem gerar erro do backend. → Validar formatos básicos no formulário, preservar a mensagem funcional da API e permitir correção sem perder contexto.

## Migration Plan

Não há migração de banco nem alteração de API. Publicar a rota e a página após os testes frontend do contrato existente; em rollback, remover a navegação/página sem alterar o histórico persistido ou o endpoint T32.
