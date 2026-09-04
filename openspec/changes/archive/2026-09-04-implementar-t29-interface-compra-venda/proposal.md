## Why

A tela de operações já possui um modal e um snapshot de carteira, mas ainda apresenta placeholders de posição para ativos que o investidor não possui e pode tratar o saldo como indisponível mesmo quando ele existe. Além disso, a identidade visual permanece predominantemente verde/clara; este ajuste fecha essas lacunas sem alterar as regras financeiras ou os fluxos de compra e venda.

## What Changes

- Criar a interface privada de compra e venda com seleção de corretora ativa própria, ticker, operação e quantidade inteira positiva, usando como ativo o `assetId` devolvido pela busca existente.
- Substituir o layout operacional por uma carteira: no topo exibir somente os campos de ticker e bolsa/mercado; abaixo listar os ativos que o investidor possui, com seleção por clique ou por ticker digitado.
- Criar o contrato autenticado `GET /api/wallet/positions`, limitado à conta da sessão, retornando saldo disponível e posições abertas com ativo, corretora, quantidade, preço médio, cotação/valor de mercado e lucro ou perda não realizado.
- Abrir um modal de operação ao selecionar um ativo, exibindo preço fixo retornado pelo backend, quantidade, saldo disponível, valor estimado (preço de compra × quantidade), posição atual e ações finais de vender/comprar.
- Adicionar o campo `assetId` (UUID opaco do ativo catalogado dono da cotação retornada) à resposta já existente de `GET /api/assets/search`, como suposição reversível mínima: o identificador é sempre o da linha de catálogo já persistida pelo fluxo de cache, sem migração, sem endpoint novo e sem regra financeira nova.
- Consultar e exibir cotação, disponibilidade, valores e horários retornados pelo backend, incluindo avisos independentes de cotação e USD/BRL desatualizados.
- Solicitar confirmação simples na mesma etapa; cancelar não envia a operação nem perde o contexto.
- Enviar somente a intenção da operação aos endpoints existentes de compra e venda, sem preço, câmbio, saldo, posição ou resultado calculado pelo frontend.
- Atualizar saldo e posição exclusivamente pela resposta bem-sucedida do backend.
- Exibir erros funcionais retornados pelo backend, incluindo solicitado/disponível quando presentes, preservando contexto e permitindo nova tentativa.
- Tratar carregamento, vazio, sucesso, erro, sessão inválida, envio duplicado e responsividade desde 320 px.
- Aplicar direção visual inspirada nas referências fornecidas, com paleta escura azul-marinho, superfícies contrastantes e acentos semânticos de ganho/perda, sem copiar marca, imagem ou identidade proprietária.
- Exibir no modal os dados de posição somente quando houver posição aberta do ativo na carteira e não renderizar placeholders de quantidade, preço médio ou resultado para ativo sem posição.
- Exibir o saldo disponível sempre a partir do `availableBalance` do snapshot da carteira, inclusive no modal de ativo sem posição, como limite informativo para compra; a validação final permanece no backend.
- Estender a direção azul para toda a aplicação, substituindo tokens verdes/claros globais sem remover estados semânticos de erro, alerta, ganho e perda.
- Documentar e preservar o cálculo de preço médio acumulado: média ponderada acumulada das compras, manutenção nas vendas parciais e reinício após zeragem.
- Não criar simulação/preflight de disponibilidade, cálculo financeiro no frontend, edição, cancelamento posterior ou estorno. A leitura autenticada de posições é somente um snapshot para apresentação e não pré-aprova a operação.

## Capabilities

### New Capabilities

Nenhuma. A interface compõe capacidades existentes.

### Modified Capabilities

- `interface-estados`: definir o comportamento observável da interface privada de compra e venda, leitura do snapshot de posições, confirmação, estados, erros, responsividade e tema azul global, consumindo o `assetId` retornado pela busca.
- `ativos-cotacoes-cambio`: expor na pesquisa de ativos o identificador opaco do ativo catalogado dono da cotação retornada, mantendo a exigência de ativo ativo na execução de compra e venda.
- `compra-venda-posicoes`: definir a leitura autenticada do snapshot de posições e explicitar o cálculo de preço médio acumulado e sua apresentação na posição do modal.

## Impact

- Backend: adição do campo `assetId` ao contrato existente de `GET /api/assets/search` e criação de `GET /api/wallet/positions`, alimentado por consultas da conta autenticada; sem migração e sem alteração de regra financeira.
- Frontend React: lista de posições, modal de operação, confirmação, cotação, avisos, estados assíncronos e revisão global dos estilos para a direção azul.
- Serviço frontend de carteira, integrado aos endpoints existentes de compra e venda e aos dados já disponíveis de corretoras/mercado, enviando o `assetId` retornado pela busca sem inventar ou derivar identificadores.
- Testes backend focados na exposição do `assetId` e testes frontend e estilos responsivos.
- Critérios de aceitação: ativo sem posição não renderiza quantidade/preço médio/resultado de posição; o modal mostra sempre o saldo `availableBalance` do snapshot como limite informativo de compra; rotas públicas e privadas usam a direção azul sem perder contraste, estados semânticos, foco ou responsividade.
- Suposição reversível registrada no design: se surgir decisão futura que dispense o campo, ele pode ser removido da resposta sem impacto em dados persistidos.
