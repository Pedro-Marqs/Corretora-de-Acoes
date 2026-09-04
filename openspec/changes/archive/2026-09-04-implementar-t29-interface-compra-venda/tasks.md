## 1. Busca com assetId (backend aditivo, suposição reversível)

- [x] 1.1 Expor o identificador opaco do ativo catalogado na resposta existente de `GET /api/assets/search`: adicionar `assetId` (UUID) ao recorte interno do cache, à view de serviço e ao response da busca, alimentado exclusivamente pelo identificador já persistido da entidade de catálogo nos dois caminhos (gravação da cotação recém-obtida e leitura do último valor armazenado), sem endpoint novo, sem migração, sem regra financeira nova e sem alterar os demais campos ou validadores existentes.
- [x] 1.2 Criar ou atualizar testes backend focados comprovando que a pesquisa BR e US, com cotação nova e com fallback de cache antigo, devolve o mesmo `assetId` do ativo persistido no catálogo (estável entre consultas do mesmo ticker/mercado) e que uma resposta de compra ou venda aceita esse identificador como `assetId` do payload conforme o contrato existente; comprovando ainda que, sem cotação válida utilizável (provedor falho e sem cache de ativo `ACTIVE`), a busca mantém a falha funcional/estado vazio existente sem publicar qualquer identificador, e executar os testes afetados.

## 2. Contrato frontend e acesso privado

- [x] 2.1 Inspecionar os contratos efetivos existentes de compra, venda, ativos, corretoras e dados de carteira e implementar o serviço frontend com payload mínimo de `assetId` retornado pela busca, corretora e quantidade, verificando por testes que o identificador é reenviado sem construção, deriva ou reutilização de outro resultado, que preço, câmbio, saldo, posição e resultado nunca são enviados, e que uma resposta de busca sem `assetId` válido é tratada como incompleta com erro funcional.
- [x] 2.2 Integrar a tela à rota e navegação privadas existentes, reutilizando autenticação, cookies, CSRF, layout e estados comuns, verificando que sessão ausente ou inválida direciona ao login sem exibir dados privados.

## 3. Seleção, cotação e confirmação

- [x] 3.1 Implementar em uma mesma tela a seleção de corretora ativa própria, ticker, compra/venda e quantidade inteira positiva, resolvendo o ativo pelo `assetId` devolvido pela busca da cotação exibida, usando apenas as fontes já disponíveis e validação de feedback, verificando compras e vendas preparadas sem listagem prévia obrigatória, simulação/preflight ou cálculo financeiro local e sem enviar operação quando a busca não tiver retornado identificador válido.
- [x] 3.2 Exibir cotação, disponibilidade, valores e horários retornados pelo backend, com avisos independentes para cotação e USD/BRL desatualizados e bloqueio somente quando não houver valor utilizável, verificando cenários BR, US, cache antigo e ausência de cache sem substituir valores por cálculo próprio.
- [x] 3.3 Implementar confirmação simples na mesma etapa para compra e venda, verificando confirmação explícita, cancelamento sem chamada de operação, preservação dos campos e ausência de página de resumo.

## 4. Execução e estados

- [x] 4.1 Conectar a confirmação aos endpoints existentes de compra e venda, impedir envios duplicados durante cada solicitação e atualizar saldo e posição somente pela resposta bem-sucedida do backend, verificando compra, venda e resposta de sucesso sem edição, cancelamento posterior ou estorno.
- [x] 4.2 Tratar erros funcionais retornados pelo backend para quantidade, ativo, corretora, saldo, posição, cotação, câmbio e indisponibilidade, exibindo solicitado/disponível quando presentes, preservando contexto e permitindo nova tentativa, verificando ausência de detalhes técnicos e tratamento de 401.

## 5. Responsividade e verificação

- [x] 5.1 Aplicar responsividade e formatação monetária de duas casas à tela, incluindo formulário, cotação, posição, confirmação, avisos e mensagens, verificando 320 px, tablet e desktop sem rolagem horizontal da página.
- [x] 5.2 Criar ou atualizar testes frontend para carregamento, vazio, sucesso, erro, desatualização, sessão inválida, compra, venda, confirmação, cancelamento, busca sem identificador utilizável e prevenção de duplicidade, verificando com a suíte frontend, ESLint e build Vite.
- [x] 5.3 Revisar o diff e a cobertura contra proposal, design e deltas, verificando que no backend apenas o campo aditivo `assetId` da resposta de busca e seus testes foram alterados, que o restante é frontend/testes/estilos da T29, que não há cálculo financeiro oficial ou preflight duplicado e que `git diff --check` e `openspec validate --strict` passam.

## 6. Contrato de leitura e redesign da carteira

- [x] 6.1 Implementar o contrato autenticado `GET /api/wallet/positions`, derivando a conta da sessão e retornando somente posições abertas da própria conta, saldo disponível e os campos de ativo, corretora, quantidade, preço médio, cotação/valor de mercado, lucro/perda não realizado e atualidade; verificar conta sem posições, isolamento entre contas, 401 e ausência de cotação utilizável sem inventar valores.
- [x] 6.2 Integrar o serviço frontend ao snapshot de posições e reestruturar a página para exibir somente ticker e bolsa/mercado no topo e, abaixo, a lista dos ativos possuídos, cobrindo carregamento, vazio, erro e seleção; verificar que clique em uma posição abre o contexto correto desde 320 px.
- [x] 6.3 Integrar a seleção por ticker digitado e por item da lista ao mesmo modal, preservando `assetId`, corretora e dados do snapshot; verificar que nenhuma cotação, saldo, posição, lucro/perda ou identificador é inventado quando a busca/leitura não retornar dados válidos.
- [x] 6.4 Implementar o modal acessível com preço fixo somente leitura, quantidade, saldo disponível, valor estimado, posição, quantidade, preço médio acumulado, lucro/perda e ações finais `Vender`/`Comprar`; verificar foco inicial/contido, cancelamento, Escape, retorno do foco e prevenção de envio duplicado, mantendo a leitura como informação e não preflight.
- [x] 6.5 Aplicar a nova direção visual inspirada nas referências e atualizar testes backend/frontend para posições com e sem cotação, compra, venda, média acumulada, estados e modal; verificar contraste, 320 px, tablet, desktop, ausência de rolagem horizontal, testes focados, ESLint, build Vite, `git diff --check` e `openspec validate --strict`.

## 7. Correções solicitadas no modal e no tema global

- [x] 7.1 Ajustar o modal para renderizar o bloco de posição somente quando o ativo selecionado tiver posição aberta correspondente na carteira; verificar que ativo sem posição não exibe quantidade, preço médio, lucro/perda nem placeholders, mas continua permitindo compra.
- [x] 7.2 Garantir que o saldo do modal e o limite informativo de compra usem sempre `availableBalance` do snapshot autenticado, inclusive para ativo sem posição; verificar que falha/ausência real do snapshot não seja mascarada e que a decisão final continue no backend.
- [x] 7.3 Aplicar a direção azul a tokens e estilos residuais de toda a aplicação, incluindo rotas públicas e privadas, preservando cores semânticas, contraste, foco visível, estados existentes e ausência de rolagem horizontal; verificar em 320 px, tablet e desktop.
- [x] 7.4 Acrescentar ou ajustar testes frontend de posição presente/ausente, saldo disponível, estados de snapshot e tema global; verificar suíte frontend, ESLint, build Vite, `git diff --check` e `openspec validate --strict`.
