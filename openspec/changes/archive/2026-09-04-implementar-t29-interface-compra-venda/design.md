## Context

O frontend já possui autenticação privada, cliente HTTP com cookies e CSRF, tratamento uniforme de erros, layouts/estados compartilhados e telas de corretoras e ativos. As T27 e T28 fornecem os contratos existentes de compra e venda; o backend permanece autoridade para cotação, câmbio, saldo, posição e resultado. A implementação também precisa de uma leitura autenticada das posições: a busca de ativos não é limitada à carteira e não contém quantidade, preço médio ou resultado.

## Goals / Non-Goals

**Goals:**

- Compor uma única jornada privada para compra e venda com snapshot autenticado de saldo e posições, seleção, cotação, confirmação, execução e atualização.
- Reutilizar os serviços e estados assíncronos existentes, mantendo feedback funcional seguro e responsividade.
- Garantir que a interface transmita intenção mínima e trate a resposta do backend como única fonte de atualização.

**Non-Goals:**

- Alterar banco ou migrações, modificar regras financeiras ou integrações externas. As extensões de API ficam limitadas ao campo `assetId` da busca e ao contrato autenticado de leitura `GET /api/wallet/positions`.
- Usar a leitura de posições como simulação/preflight, reserva de saldo/quantidade ou garantia de aceite; a validação final continua nos endpoints de compra e venda.
- Adicionar preço, câmbio, saldo, posição ou resultado ao payload; criar resumo separado, edição, cancelamento posterior ou estorno.

## Decisions

- **`assetId` como campo aditivo na busca existente (suposição reversível):** a resposta de `GET /api/assets/search` passa a incluir o UUID opaco do ativo catalogado já persistido pelo fluxo de catálogo/cache. O identificador nunca é novo: `MarketCachePersistenceService` cria ou reutiliza a linha de catálogo por ticker e mercado e ambos os caminhos de resposta constroem o recorte a partir da entidade persistida. O snapshot de posições é separado porque precisa de escopo por conta e de dados de carteira; aceitar ticker no payload ou derivar UUID no frontend continuaria sendo rejeitado. Se uma decisão futura dispense o campo, basta removê-lo da resposta, sem impacto em dados persistidos.
- **Uma página com operação selecionável:** compra e venda compartilham formulário, confirmação e estados para evitar fluxos divergentes. Separar páginas duplicaria validações sem benefício observável.
- **Dados sob demanda pelos contratos existentes:** seleções e consultas usarão as fontes já disponíveis no frontend e as respostas efetivas dos contratos, incluindo o `assetId` devolvido pela busca e o snapshot autenticado de posições; a listagem não será pré-condição para enviar uma operação válida.
- **Snapshot autenticado de posições:** `GET /api/wallet/positions` será derivado exclusivamente da conta da sessão e retornará `availableBalance` e somente posições abertas. Cada item conterá `assetId`, ticker, nome, mercado, moeda, `brokerageId`, nome da corretora, quantidade, preço médio unitário, cotação utilizável quando houver, valor de mercado, lucro/perda não realizado e metadados de atualidade. Valores indisponíveis por falta de cotação permanecerão ausentes, nunca calculados pelo cliente; lista vazia será válida.
- **Leitura sem preflight:** o snapshot servirá para a lista e o contexto do modal. Uma posição alterada entre leitura e confirmação será tratada pela resposta da operação, sem reserva ou simulação local.
- **Payload mínimo por operação:** o serviço enviará apenas os identificadores de ativo/corretora e a quantidade exigidos por cada endpoint existente, usando o `assetId` retornado pela busca sem construí-lo, derivá-lo ou reutilizar um memorizado de outra consulta. Preço, câmbio e valores financeiros exibidos são somente leitura; não haverá cálculo de total, saldo ou disponibilidade no cliente.
- **Confirmação local:** solicitar execução abre confirmação na mesma etapa. Cancelar apenas fecha esse estado e preserva os campos; confirmar dispara uma única mutação.
- **Resposta como autoridade:** saldo e posição exibidos após sucesso serão substituídos pelos dados retornados pelo backend. Erros preservam a seleção e não simulam alteração local.
- **Bloqueio por solicitação:** pesquisa/preparação e execução terão controles próprios desabilitados enquanto pendentes, evitando duplicidade sem bloquear correções após erro.
- **Avisos não equivalem a cálculo:** cotação/USD-BRL utilizável e antigo será exibido com aviso e horário; ausência de valor utilizável impedirá confirmação conforme o estado retornado pelo backend.
- **Composição responsiva:** reutilizar tokens e breakpoints atuais, empilhando formulário, cotação, posição, confirmação e mensagens em telas estreitas, sem tabela larga ou rolagem horizontal da página.

## Risks / Trade-offs

- **Dados envelhecem entre consulta e confirmação** → exibir o horário/aviso, mas deixar a validação final ao backend e tratar sua resposta sem inferência local.
- **Resposta pode omitir campos opcionais de erro** → usar o normalizador existente e renderizar solicitado/disponível somente quando presentes.
- **Posição ou corretora pode mudar durante a tela** → aceitar a rejeição do backend, preservar contexto recuperável e não declarar sucesso local.
- **Interações rápidas podem duplicar requisições** → desabilitar controles relevantes e testar submissão por clique e teclado.
- **Tema parcialmente atualizado** → centralizar as cores recorrentes em tokens azulados e revisar seletores existentes por página, com verificação visual nas rotas públicas e privadas.
- **Resposta de busca sem `assetId` válido (regressão de contrato)** → o validador do serviço de mercado trata a resposta como incompleta com erro funcional, e a tela não envia operação com identificador inventado; testes backend e frontend cobrem o campo.

## Migration Plan

Não há migração: o identificador exposto já é persistido pelo fluxo atual de catálogo/cache. A entrega adiciona um campo aditivo à resposta de busca e a composição frontend sobre os contratos atuais. Para rollback, remover a rota/acesso da interface e desconsiderar ou retirar o campo da resposta, sem alterar dados já registrados no backend.

## Layout e interação da carteira

- A tela terá um cabeçalho operacional mínimo contendo apenas ticker e bolsa/mercado. Abaixo dele ficará a lista das posições do investidor, com estado vazio explícito quando não houver ativos.
- Digitar um ticker e selecionar um resultado, ou clicar em uma posição da lista, abrirá o mesmo modal de operação. A busca digitada continuará usando o `assetId` retornado pelo backend; a lista usará os identificadores devolvidos pelo snapshot autenticado.
- O modal concentrará preço fixo somente leitura, quantidade, saldo disponível, valor estimado da compra, seletor de corretora quando necessário e a posição da corretora selecionada. A posição exibirá quantidade, preço médio acumulado e lucro/perda, e os controles finais serão `Vender` e `Comprar`.
- O bloco de posição será condicional: só será renderizado quando o ativo selecionado tiver posição aberta na carteira (e, para a corretora escolhida, posição correspondente). Ativos sem posição continuarão podendo ser comprados, mas não receberão valores substitutos como “indisponível” para quantidade, preço médio ou resultado.
- O saldo mostrado no modal será sempre o `availableBalance` do snapshot autenticado da carteira. Ele é informação de limite para compra, não reserva nem pré-validação; mudanças concorrentes continuam sendo decididas pelo backend.
- O valor estimado será apresentado a partir dos dados financeiros retornados pelo backend ou da representação de cotação aplicável, sem tornar o cálculo do navegador fonte de verdade. O preço não será editável nem enviado no payload.
- O modal será um diálogo acessível: foco inicial controlado, foco contido enquanto aberto, fechamento por cancelamento/Escape quando permitido e retorno do foco ao acionador. Erros permanecerão associados aos campos e o contexto não será perdido.
- A direção visual usará tokens próprios de fundo azul-marinho, superfícies elevadas, texto claro, acento teal e cores semânticas para lucro/perda. Não serão incorporados assets, nomes ou componentes proprietários das imagens de referência.
- A direção azul será aplicada nos tokens compartilhados e nos estilos específicos de páginas, navegação, cartões, formulários, modais e estados. Erro, alerta, sucesso e resultado positivo/negativo conservarão diferenciação semântica e contraste acessível.

### Correção incremental do modal e do tema

As correções devem ser feitas sobre os contratos e tokens já adotados neste change, sem criar uma fonte paralela de carteira. O componente do modal deve localizar uma posição somente pela combinação do ativo selecionado e da corretora em contexto; ausência dessa combinação significa ausência de posição e remove o bloco inteiro de posição. O saldo exibido deve ser lido do `availableBalance` do snapshot atual, inclusive quando a posição estiver ausente, e nunca substituído por estado textual de indisponibilidade quando o campo existir.

O tema azul deve ser aplicado nos tokens compartilhados e nos estilos residuais de cada rota, preservando tokens semânticos separados para erro, alerta, sucesso, ganho, perda e destruição. A verificação deve cobrir rotas públicas e privadas, foco visível e larguras de 320 px, tablet e desktop; não haverá alteração de contratos financeiros ou de regras de operação.

## Decisão financeira documentada

O preço médio acumulado de uma posição após cada compra será a média ponderada de todo o custo ainda carregado: `(quantidade anterior × preço médio anterior + quantidade comprada × preço unitário da compra) ÷ (quantidade anterior + quantidade comprada)`, com valores financeiros arredondados conforme a regra vigente. Venda parcial reduz o custo pelo preço médio acumulado vigente e preserva esse preço médio na quantidade restante; venda total encerra a posição e uma recompra inicia uma nova média usando somente a recompra. A interface apenas apresenta o valor calculado pelo backend.
