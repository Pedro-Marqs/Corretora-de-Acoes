## 1. Shell privado e navegação

- [x] 1.1 Reorganizar o layout privado para exibir somente `Investimentos`, `Banco`, `Bolsa` e o nome do usuário, com menu acessível de `Configurações`/`Sair`, guards, logout e mapeamento das páginas existentes; verificar sessão inválida, foco, teclado, clique externo, prevenção de logout duplicado e ausência de links mortos.

## 2. Contexto Investimentos

- [x] 2.1 Compor Investimentos na ordem total de patrimônio, saldo, distribuição nacional/internacional/saldo e carteira, e abrir listas filtradas ao selecionar cada mercado com ação `Investir`; verificar respostas oficiais, conta sem posições, avisos, isolamento de mercado, conversão USD/BRL e ausência de cálculos financeiros no cliente.

## 3. Bolsa, detalhe e negociação

- [x] 3.1 Reposicionar a pesquisa de ticker na Bolsa, conectar resultados ao detalhe e deste à negociação com quantidade, preço oficial somente leitura e Comprar/Vender, reutilizando assetId, corretora, confirmação e contratos atuais; verificar ativos BR/US, estados de pesquisa, desatualização, rejeições, prevenção de duplicidade e atualização somente pela API.

## 4. Contexto Banco

- [x] 4.1 Compor Banco com saldo compartilhado, aporte e administração de corretoras, e representar retirada de forma segura conforme o contrato disponível; verificar aporte, erros, corretoras próprias, bloqueio de remoção com posição e, na ausência de endpoint de retirada, indisponibilidade sem alteração ou operação fictícia.

## 5. Estados, responsividade e validação

- [x] 5.1 Aplicar carregamento, vazio, erro, sessão inválida, desatualização, acessibilidade e preservação de contexto a todas as composições, mantendo uso sem rolagem horizontal desde 320 px; verificar respostas simuladas, foco e ausência de dados técnicos/sensíveis.
- [x] 5.2 Validar o fluxo header → Investimentos → categoria → ativo → negociação e header → Banco/Bolsa em 320 px, tablet e desktop, executando testes frontend, ESLint, build Vite, `git diff --check` e `openspec validate implementar-t38-interface-investimentos-banco-bolsa --strict`.
