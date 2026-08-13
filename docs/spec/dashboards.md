# Spec — Dashboards da carteira e da corretora

## Objetivo

Apresentar a situação consolidada da conta e a visão de uma corretora, com indicadores, distribuições e evolução patrimonial.

## Usuários envolvidos

- Investidor.

## Pré-condições

- Conta ativa e sessão válida;
- Saldo inicial existente;
- Para visão específica, corretora pertencente à conta.

## Entradas

- Escopo geral ou corretora selecionada;
- Período: quatro semanas, três meses, seis meses, um ano, cinco anos ou máximo;
- Saldo, posições, resultados, últimas cotações, câmbio e pontos patrimoniais armazenados.

## Validações

- Todos os dados devem pertencer à conta autenticada.
- Valores em USD devem ser convertidos para BRL para consolidação.
- Aportes não podem ser classificados como lucro ou valorização.
- O período máximo começa na criação da conta.
- Sem dados no início do período, o gráfico começa no primeiro ponto disponível.

## Fluxo principal

1. O investidor abre o dashboard geral.
2. O sistema calcula saldo, patrimônio, posições, preço médio, resultado realizado, valorização não realizada e resultado total.
3. Apresenta distribuições por ativo, corretora e mercado, com valores numéricos.
4. Apresenta a evolução patrimonial no período escolhido.
5. O investidor pode selecionar uma corretora para restringir a visão.

## Fluxos alternativos

- Conta sem posições apresenta saldo e estados vazios nas distribuições.
- Cotação ou câmbio desatualizado é usado com aviso visível.
- Um período sem cobertura completa apresenta apenas o intervalo disponível.

## Situações de erro

- Corretora inexistente ou pertencente a outra conta;
- Ausência de cotação necessária sem cache;
- Dados inconsistentes entre posição e histórico;
- Falha ao carregar indicadores ou gráfico.

## Regras de autorização

- O investidor só pode visualizar o próprio dashboard e suas corretoras.
- O escopo da conta deve ser derivado da sessão, não de identificador fornecido isoladamente.

## Resultado esperado

Os indicadores e gráficos apresentam valores coerentes com saldo, posições, histórico e últimas cotações utilizáveis no escopo selecionado.

## Critérios de aceitação

### CA01 — Patrimônio

**Dado** saldo de R$ 1.000,00 e posições avaliadas em R$ 2.500,00  
**Quando** o dashboard geral for carregado  
**Então** o patrimônio deve ser R$ 3.500,00.

### CA02 — Aporte fora do resultado

**Dado** uma conta sem variação de preços que recebeu um aporte  
**Quando** os resultados forem calculados  
**Então** o aporte deve aumentar saldo e patrimônio sem aumentar lucro ou valorização.

### CA03 — Visão por corretora

**Dado** posições em duas corretoras  
**Quando** uma delas for selecionada  
**Então** posições, resultados e distribuições específicas devem considerar somente essa corretora, mantendo o saldo da conta identificado como compartilhado.

### CA04 — Distribuições

**Dado** posições em ativos, corretoras e mercados diferentes  
**Quando** o dashboard for exibido  
**Então** cada distribuição deve apresentar as parcelas e seus valores numéricos em reais.

### CA05 — Período máximo

**Dado** pontos patrimoniais desde a criação da conta  
**Quando** o período máximo for selecionado  
**Então** o gráfico deve abranger do primeiro ao último ponto existente.

### CA06 — Período parcialmente coberto

**Dado** um período selecionado anterior ao primeiro ponto disponível  
**Quando** o gráfico for carregado  
**Então** deve começar no primeiro ponto existente sem criar valores inexistentes.

### CA07 — Cotação desatualizada

**Dado** indicador calculado com cotação de ativo superior a 24 horas  
**Quando** o dashboard for exibido  
**Então** o valor deve ser apresentado junto de aviso de desatualização.

## Requisitos relacionados

- RF15, RF33, RF37, RF42 e RF65–RF70;
- RN01, RN07, RN11 e RN18–RN28;
- RNF04–RNF06, RNF08 e RNF15–RNF18;
- HU12–HU14;
- CE17, CE19 e CE21.
