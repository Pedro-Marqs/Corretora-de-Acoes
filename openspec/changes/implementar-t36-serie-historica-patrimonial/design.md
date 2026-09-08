## Context

O dashboard já consolida a conta, a carteira e suas distribuições, enquanto os pontos patrimoniais são gravados pelas operações financeiras. A série deve ser uma leitura desses registros existentes, respeitando a conta autenticada e o relógio configurado da aplicação.

## Goals / Non-Goals

**Goals:**

- Acrescentar a série histórica ao contrato existente do dashboard.
- Resolver os seis períodos com limites determinísticos e ordenação cronológica.
- Preservar o modelo de pontos patrimoniais e a ausência de efeitos colaterais na consulta.

**Non-Goals:**

- Criar snapshots, pontos interpolados ou novo endpoint.
- Recalcular patrimônio a partir de cotações atuais.
- Alterar o registro de movimentações, aportes, compras, vendas ou transferências.
- Criar a interface e os gráficos, que pertencem à T37.

## Decisions

- **Parâmetro no endpoint existente:** a série será solicitada como parte de `GET /api/dashboard`, mantendo autenticação, contrato de erros e contexto da conta em uma única leitura. Um endpoint separado duplicaria autorização e composição do dashboard.
- **Consulta por intervalo no repositório de patrimônio:** filtrar por conta e instante entre o limite calculado e o instante atual, ordenando ascendentemente. A alternativa de carregar todos os pontos e filtrar em memória não escala com o histórico e aumenta o risco de isolamento incorreto.
- **Períodos representados por valores fechados:** aceitar somente `4W`, `3M`, `6M`, `1Y`, `5Y` e `MAX`; o backend calcula os limites usando o relógio injetável. Isso evita que o frontend defina datas arbitrárias ou regras divergentes.
- **Cobertura parcial sem preenchimento:** retornar o primeiro ponto existente quando não houver cobertura desde o limite. Interpolação ou repetição do saldo inicial criaria dados não registrados e poderia sugerir rentabilidade inexistente.
- **Sem recálculo por cotação:** usar o valor patrimonial persistido no ponto. Reavaliar posições com cotações atuais mudaria a semântica histórica e violaria a regra de que atualizações isoladas não criam pontos.

## Risks / Trade-offs

- **[Histórico grande pode aumentar o custo da resposta]** → filtrar por conta e intervalo na consulta, ordenar no banco e não introduzir agregações ou snapshots fora do escopo.
- **[Diferenças de calendário em períodos mensais e anuais]** → centralizar o cálculo dos limites no relógio da aplicação e cobrir cada período com testes de datas controladas.
- **[Consumidores existentes podem não esperar a série]** → manter os campos atuais e acrescentar a coleção histórica sem alterar o significado dos indicadores já publicados.

## Migration Plan

Nenhuma migração de banco é necessária. A alteração é reversível removendo o parâmetro/período e o campo da série; os pontos patrimoniais existentes permanecem intactos.
