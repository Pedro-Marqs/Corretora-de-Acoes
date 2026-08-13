# Spec — Histórico e registro patrimonial

## Objetivo

Manter um registro imutável das movimentações concluídas e pontos patrimoniais que sustentem auditoria e gráficos.

## Usuários envolvidos

- Investidor;
- Funcionalidades de cadastro, aporte, compra, venda e transferência.

## Pré-condições

- Conta existente;
- Para consulta, sessão válida;
- Para gravação, uma movimentação sendo concluída com sucesso.

## Entradas

- Tipo de movimentação;
- Ticker, cotação, quantidade, valor total e moeda, quando aplicáveis;
- Corretora ou corretoras de origem e destino, quando aplicáveis;
- Data/hora e saldo restante;
- Valor patrimonial calculado após a movimentação.

## Validações

- Somente saldo inicial, aporte, compra, venda e transferência concluídos são registrados.
- Campos aplicáveis ao tipo devem estar presentes.
- Data/hora usa o horário de Brasília.
- Registros não podem ser editados nem excluídos pelo usuário.
- Cada página contém 20 registros.

## Fluxo principal

1. Uma movimentação é validada e concluída.
2. Na mesma transação, o sistema registra seus dados e o saldo resultante.
3. O sistema grava um ponto patrimonial posterior à movimentação.
4. O investidor consulta o histórico ordenado do mais recente para o mais antigo.
5. Pode aplicar filtros e navegar em páginas de 20 registros.

## Fluxos alternativos

- O saldo inicial produz o primeiro registro e o primeiro ponto patrimonial.
- Os filtros podem combinar intervalo de datas, tipo, ticker, corretora e mercado.
- Pontos patrimoniais são criados somente após saldo inicial, aporte, compra, venda ou transferência; atualizações de cotação isoladas não criam pontos.

## Situações de erro

- Tentativa de gravar movimentação incompleta;
- Falha parcial entre alteração financeira, histórico e ponto patrimonial;
- Tentativa de editar ou excluir registro;
- Consulta de histórico pertencente a outra conta.

## Regras de autorização

- Somente o investidor autenticado pode consultar o próprio histórico.
- Nenhum investidor pode editar ou excluir registros históricos.
- A gravação ocorre apenas como consequência interna de uma movimentação concluída.

## Resultado esperado

O histórico reflete exatamente as movimentações bem-sucedidas, possui pontos patrimoniais correspondentes e pode ser consultado em páginas de 20 itens.

## Critérios de aceitação

### CA01 — Registro de sucesso

**Dado** uma movimentação válida concluída  
**Quando** sua transação for confirmada  
**Então** devem ser gravados o registro histórico, o saldo restante e um ponto patrimonial.

### CA02 — Tentativa rejeitada

**Dado** uma movimentação inválida ou não concluída  
**Quando** ela for rejeitada  
**Então** nenhum registro de sucesso ou ponto patrimonial deve ser criado.

### CA03 — Imutabilidade

**Dado** um registro histórico existente  
**Quando** o investidor tentar editá-lo ou excluí-lo  
**Então** a ação deve ser negada e o registro deve permanecer inalterado.

### CA04 — Paginação

**Dado** mais de 20 movimentações  
**Quando** o histórico for consultado sem filtros  
**Então** a primeira página deve conter no máximo 20 itens, começando pelos mais recentes.

### CA05 — Filtros

**Dado** movimentações com datas, tipos, tickers, corretoras e mercados diferentes  
**Quando** filtros forem aplicados  
**Então** somente registros da própria conta que atendam a todos os filtros devem ser retornados.

### CA06 — Atualização de cotação isolada

**Dado** uma atualização automática de cotação sem movimentação  
**Quando** o preço armazenado mudar  
**Então** nenhum novo registro histórico ou ponto patrimonial deve ser criado.

## Requisitos relacionados

- RF03, RF17, RF47, RF51 e RF59–RF64;
- RN07, RN20, RN24–RN26, RN29 e RN30;
- RNF04–RNF08 e RNF18;
- HU11;
- CE20 e CE21.
