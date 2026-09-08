## Context

O endpoint de dashboard e as regras financeiras da T26 já existem e devem continuar sendo a fonte oficial dos valores. A T35 adiciona uma visão opcional por corretora e agregações derivadas das posições abertas, sem alterar o modelo persistido ou o saldo único da conta.

## Goals / Non-Goals

**Goals:**

- Reutilizar a consulta autenticada e os valores de mercado já consolidados.
- Validar a corretora no escopo da conta e aplicar o filtro antes das agregações.
- Produzir distribuições determinísticas em BRL por ativo, corretora e mercado.
- Preservar arredondamento financeiro, isolamento e compatibilidade com a visão geral.

**Non-Goals:**

- Criar tabelas, snapshots adicionais, endpoints separados ou cálculos no frontend.
- Alterar saldo, posições, movimentações, pontos patrimoniais ou regras de cotação.
- Adicionar gráficos ou telas; isso pertence à T37.

## Decisions

- **Filtro opcional no endpoint existente:** usar um parâmetro de corretora na consulta atual, mantendo a resposta geral quando ausente. Isso evita duplicação de contrato e preserva consumidores existentes; um endpoint separado teria maior custo e risco de divergência.
- **Agregação sobre posições consolidadas:** calcular cada parcela a partir do mesmo valor de mercado em BRL usado pelos indicadores. Isso evita divergência entre total e distribuições; consultas SQL independentes poderiam aplicar arredondamentos ou conversões diferentes.
- **Identidade opaca e autorização na consulta:** aceitar somente o identificador já exposto pelo contrato e confirmar que a corretora pertence à conta e está ativa antes de ler posições. Não aceitar CNPJ ou nome como autoridade, pois são dados de busca e podem ser ambíguos.
- **Saldo compartilhado fora do filtro:** manter o saldo da conta na resposta, mas restringir patrimônio, resultados e posições à visão selecionada. Assim a visão por corretora não sugere que o dinheiro disponível pertence a uma corretora específica.
- **Sem persistência de agregados:** derivar distribuições em cada leitura. A consistência imediata e a ausência de migração compensam o custo de agregação para o escopo local.

## Risks / Trade-offs

- **[Arredondamento pode fazer a soma visual divergir por centavos]** → arredondar parcelas com a política financeira existente e testar a reconciliação dentro da tolerância definida pelo domínio.
- **[Posições em mercados diferentes usam fontes de cotação com estados distintos]** → reutilizar os marcadores e instantes de desatualização do dashboard e não inventar valores quando não houver câmbio utilizável.
- **[Consulta agregada pode crescer com a carteira]** → filtrar conta/corretora antes de agrupar, manter a leitura sem efeitos colaterais e evitar introduzir otimizações estruturais fora do escopo.

## Migration Plan

Nenhuma migração de banco ou compatibilidade de dados é necessária. A alteração é reversível removendo o parâmetro opcional e a composição das distribuições; o comportamento geral do dashboard permanece como fallback.
