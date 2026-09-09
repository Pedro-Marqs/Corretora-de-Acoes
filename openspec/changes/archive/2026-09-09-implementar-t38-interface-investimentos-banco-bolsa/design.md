## Context

O frontend já possui rotas privadas e contratos para dashboard, pesquisa de ativos, operações, saldo, corretoras, conta e logout. O dashboard é a fonte dos indicadores e posições; operações continuam submetidas aos endpoints existentes. A motivação e o escopo observável estão em `proposal.md` e nas delta specs deste change.

## Goals / Non-Goals

**Goals:**

- Criar um shell privado único com três contextos principais e menu de conta.
- Compor Investimentos a partir dos dados oficiais existentes, com navegação progressiva categoria → ativo → negociação.
- Reorganizar saldo, aporte e corretoras sob Banco e pesquisa sob Bolsa.
- Preservar estados comuns, acessibilidade e responsividade sem alterar autoridade financeira.

**Non-Goals:**

- Criar endpoints, migrações, novas regras financeiras ou novos provedores de cotação.
- Recalcular patrimônio, distribuição, preço, saldo, câmbio ou resultados no navegador.
- Criar livro de ofertas, preço limite, taxas, retirada simulada ou qualquer operação não suportada pelo backend atual.
- Remover histórico, transferência ou configurações existentes; somente reposicioná-los no fluxo apropriado.

## Decisions

- **Shell compartilhado e rotas contextuais:** o layout privado será a única origem do header e do menu do usuário. As páginas existentes serão compostas sob os contextos Investimentos, Banco e Bolsa, em vez de manter uma segunda navegação paralela. Isso evita divergência de logout, sessão e foco.
- **Investimentos como composição do dashboard:** patrimônio, saldo, distribuições e posições serão renderizados a partir de uma leitura oficial do dashboard/posições. A categoria nacional ou internacional apenas seleciona o conjunto visual; não recalcula parcelas nem cria uma nova fonte de dados.
- **Fluxo progressivo de operação:** a seleção de uma posição abre detalhe e `Negociar` abre o formulário mínimo. A busca da Bolsa poderá iniciar o mesmo detalhe para um ativo sem posição. O contexto de ativo, corretora e `assetId` será preservado até a confirmação, enquanto preço e saldo permanecem somente leitura.
- **Banco como hub de dinheiro e corretoras:** a página reunirá o saldo único, o formulário de aporte e a interface de corretoras já existentes. Retirada não será inventada: quando não houver contrato backend, o controle apresentará indisponibilidade segura; nenhuma alteração de saldo será simulada.
- **Bolsa como pesquisa:** a pesquisa por ticker continuará usando o serviço e os estados de cotação existentes. A apresentação será movida para o contexto Bolsa, sem duplicar cliente HTTP ou regras de cache.
- **Dados e estados por consulta:** mudanças de seção ou seleção iniciarão as leituras necessárias e substituirão a apresentação somente por respostas correspondentes. Erros 401 seguirão o guard de rota; erros recuperáveis manterão filtros e contexto.
- **Alternativas descartadas:** um mega-dashboard com todos os controles foi descartado por misturar dinheiro, carteira e busca; uma API agregadora nova foi descartada por duplicar contratos e regras já implementados; a retirada end-to-end foi descartada neste change por não existir requisito/contrato financeiro correspondente.

## Risks / Trade-offs

- **Muitos níveis de navegação em telas pequenas** → usar leitura vertical, retorno claro entre lista/detalhe/negociação e header compacto sem overflow.
- **Respostas diferentes entre dashboard e posições** → atualizar a seção após mutações usando resposta oficial ou nova leitura autenticada, nunca mesclar cálculos locais.
- **Expectativa de retirada funcional** → deixar o acesso visual explícito somente quando aplicável e exibir indisponibilidade segura quando o backend não oferecer o contrato; registrar uma futura mudança separada se o produto confirmar a regra.
- **Rotas antigas usadas por links ou testes** → preservar compatibilidade das rotas existentes e fazer a nova navegação apontar para elas, salvo quando uma página já tiver sido absorvida sem perda de função.

## Migration Plan

Não há migração de banco nem alteração de API. Implementar o shell e a composição mantendo as páginas/serviços atuais, migrar os links para os três contextos e validar os fluxos existentes. A reversão consiste em restaurar a navegação anterior e remover apenas a composição visual, sem tocar nos dados persistidos.
