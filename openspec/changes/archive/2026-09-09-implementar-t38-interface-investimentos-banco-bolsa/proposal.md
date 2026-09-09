## Why

A área privada já possui fluxos funcionais, mas sua navegação está fragmentada e não acompanha a leitura mental do investidor: patrimônio, banco e bolsa ficam misturados. Esta mudança organiza a experiência em três seções principais, mantendo os contratos financeiros existentes e tornando o caminho da carteira até a negociação explícito.

## What Changes

- Reorganizar o header privado em `Investimentos`, `Banco`, `Bolsa` e um menu com o nome do usuário.
- Fazer o menu do usuário expandir para `Configurações` e `Sair`.
- Compor Investimentos com total de patrimônio, saldo, distribuição entre ações nacionais, ações internacionais e saldo em conta, além da carteira.
- Permitir navegar da categoria nacional/internacional para a lista de ativos, do ativo para seus dados e, então, para a tela de negociação.
- Reutilizar a cotação, saldo, posição, corretora e regras de compra/venda fornecidos pelos contratos existentes, sem cálculo financeiro no frontend.
- Compor Banco somente com saldo em dinheiro, aporte/retirada e administração das corretoras.
- Compor Bolsa com a pesquisa normal de ativos e seus estados de cotação.
- Preservar histórico, transferências, conta, autenticação, avisos de dados antigos e responsividade mínima de 320 px.

## Capabilities

### New Capabilities

- `navegacao-interface-financeira`: estrutura observável do shell privado, menu do usuário e organização das seções Investimentos, Banco e Bolsa.

### Modified Capabilities

- `dashboards`: apresentar a visão de investimentos com patrimônio, saldo, categorias e carteira, incluindo navegação para ativos.
- `ativos-cotacoes-cambio`: apresentar a lista, detalhe e pesquisa de ativos dentro dos novos contextos de Investimentos e Bolsa.
- `compra-venda-posicoes`: conduzir da tela de detalhe para negociação com quantidade, preço oficial e compra/venda.
- `saldo-aportes`: apresentar saldo e aporte/retirada na seção Banco, preservando o saldo único da conta.
- `corretoras`: apresentar o cadastro e a administração das corretoras na seção Banco.
- `interface-estados`: aplicar estados, acessibilidade, confirmação e responsividade ao novo shell e aos fluxos reorganizados.

## Impact

- Rotas privadas, layout/header, páginas, componentes, serviços e estilos do frontend React.
- Composição visual dos contratos existentes de dashboard, ativos, carteira, saldo, corretoras e autenticação; não há alteração de autoridade financeira no cliente.
- Testes de navegação, hierarquia de telas, estados, acessibilidade e responsividade.
- Nenhuma migração de banco, nova API ou alteração das regras financeiras é necessária; a retirada reutiliza o fluxo de saída de dinheiro existente quando disponível e não inventa um contrato backend ausente.
