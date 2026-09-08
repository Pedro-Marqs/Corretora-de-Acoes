## Context

O backend de dashboard já consolida saldo, posições, resultados, distribuições e pontos patrimoniais, e a fundação do frontend já fornece rotas privadas, cliente HTTP, formatação e estados comuns. A interface deve consumir esses valores sem duplicar regras financeiras.

## Goals / Non-Goals

**Goals:**

- Compor uma leitura única do dashboard e refletir seus dados oficiais nas seções visuais.
- Manter filtros de corretora e período coerentes com os parâmetros aceitos pela API.
- Garantir estados recuperáveis e uso em telas estreitas.

**Non-Goals:**

- Alterar endpoints, regras financeiras, snapshots ou persistência.
- Recalcular patrimônio, resultados, distribuições ou conversões no navegador.
- Criar edição de posições, movimentações ou atualização manual de cotações.

## Decisions

- **Uma consulta oficial por seleção:** o serviço frontend enviará os filtros atuais ao endpoint de dashboard e substituirá a apresentação somente pela resposta confirmada. Isso evita divergência entre cartões, distribuições e gráfico; chamadas separadas por seção foram descartadas por poderem exibir versões incompatíveis.
- **Filtros como estado explícito:** visão geral/corretora e período serão mantidos como estado da tela e reaplicados em nova consulta. Isso é preferível a filtrar os dados já recebidos, pois a autorização e a seleção de posições pertencem ao backend.
- **Componentes visuais sem matemática financeira:** cartões, tabelas/listas e gráficos apenas formatarão e exibirão campos da resposta. Bibliotecas de gráfico novas não são necessárias; uma visualização própria simples reduz dependências e mantém controle responsivo.
- **Estados por consulta:** carregamento, vazio, erro, sessão inválida e desatualização serão apresentados sem apagar filtros recuperáveis. A resposta 401 seguirá o redirecionamento privado já existente.

## Risks / Trade-offs

- **Resposta extensa em telas pequenas** → agrupar seções, permitir leitura vertical e assegurar largura mínima de 320 px sem overflow da página.
- **Períodos ou corretora alterados durante uma requisição** → bloquear a consulta equivalente enquanto aguarda resposta e aceitar apenas a resposta correspondente ao estado atual.
- **Interpretação visual de dados antigos** → preservar os avisos e instantes retornados pelo backend junto aos indicadores afetados.

## Migration Plan

Nenhuma migração de banco ou endpoint é necessária. A reversão consiste em remover a rota/componentes do dashboard, mantendo os contratos backend existentes.
