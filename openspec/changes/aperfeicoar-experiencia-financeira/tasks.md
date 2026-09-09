## 1. Contratos financeiros e persistência

- [x] 1.1 Implementar retirada autenticada, validação de saldo e atomicidade com movimentação/ponto patrimonial; verificar sucesso, saldo insuficiente, rollback e isolamento.
- [x] 1.2 Persistir e consultar tipo de retirada e instante efetivo de operações, com migração compatível, histórico paginado e ordenação.
- [x] 1.3 Ajustar compra/venda para aceitar preço unitário e data/hora válidos, com cotação e momento atual como padrões do cliente.
- [x] 1.4 Revalidar situação cadastral ativa da corretora na associação e na confirmação da operação; bloquear inatividade e indisponibilidade externa.

## 2. Mercado, dashboard e interface funcional

- [x] 2.1 Propagar cotação desatualizada como “preço de fechamento”, preservando o instante original em pesquisa, detalhe, operação e dashboard.
- [x] 2.2 Implementar retirada no Banco e negociação com preço/data editáveis, confirmação e estados recuperáveis.
- [x] 2.3 Renderizar gráficos patrimoniais e de distribuição com dados oficiais, filtros, acessibilidade e fallback textual.

## 3. Shell visual e validação transversal

- [x] 3.1 Fazer login direcionar para `/app` e reformular tela inicial, header e seções com fundo azul-marinho.
- [x] 3.2 Corrigir overflow horizontal e áreas vazias em telas estreitas, tablet e desktop.
- [x] 3.3 Executar testes backend/frontend, lint, build, `git diff --check` e `openspec validate --strict`.
