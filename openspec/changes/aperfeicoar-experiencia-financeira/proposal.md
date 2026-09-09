## Why

A experiência atual ainda não cobre retirada de saldo e não comunica claramente o contexto temporal das cotações, enquanto os fluxos de operação e a apresentação visual precisam ser mais úteis e coerentes. Esta mudança consolida esses ajustes funcionais e uma reformulação visual responsiva, sem perder a autoridade financeira do backend.

## What Changes

- Habilitar retirada de saldo com validação, confirmação, histórico e patrimônio atômicos.
- Garantir que a associação de corretora revalide situação cadastral ativa antes de concluir.
- Exibir gráficos úteis no dashboard e manter seus dados oficiais, filtros e estados.
- Identificar cotação antiga de ativo como preço de fechamento, com instante original.
- Pré-preencher preço de compra/venda com a cotação, mantendo o campo editável, e permitir data/hora editável com padrão no momento atual.
- Fazer o login conduzir à tela inicial privada, visualmente coerente com a tela de login.
- Corrigir overflow e áreas vazias causadas por arraste horizontal em todos os tamanhos suportados.
- Aplicar fundo azul-marinho e reformular o layout com referências visuais de Status Invest e Investidor10, sem copiar identidade, conteúdo ou elementos proprietários.

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- `saldo-aportes`: retirada de saldo e apresentação do Banco.
- `historico-registro-patrimonial`: registro de retirada e datas de operação.
- `ativos-cotacoes-cambio`: identificação de preço de fechamento para cotação desatualizada.
- `compra-venda-posicoes`: preço inicial editável e data/hora da operação.
- `dashboards`: gráficos visíveis no dashboard.
- `navegacao-interface-financeira`: destino pós-login e tela inicial privada.
- `interface-estados`: tema azul-marinho, referências visuais e correção de overflow.

## Impact

Afeta contratos de saldo, histórico, operações, mercado e dashboard, incluindo persistência de movimentações e timestamps. Afeta o shell e estilos do frontend, telas de login, Banco, Bolsa, Investimentos e dashboard; pode exigir migração de banco e não exige nova integração externa.
