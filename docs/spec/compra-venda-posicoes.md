# Spec — Compra, venda e manutenção de posições

## Objetivo

Permitir compras e vendas simuladas, mantendo saldo, posição, preço médio, resultado e histórico consistentes.

## Usuários envolvidos

- Investidor;
- Serviços de cotação e câmbio.

## Pré-condições

- Conta ativa e sessão válida;
- Corretora ativa pertencente à conta;
- Ativo aceito e cotação utilizável;
- Para ativo norte-americano, USD/BRL armazenado;
- Para venda, posição na corretora selecionada.

## Entradas

- Tipo da operação;
- Corretora;
- Ticker;
- Quantidade inteira;
- Confirmação do investidor.

## Validações

- Quantidade deve ser inteira e maior que zero.
- Compra não pode superar o saldo.
- Venda não pode superar a posição na corretora.
- O backend define cotação e câmbio; valores enviados pelo cliente não determinam o preço.
- Não existem taxas, impostos ou quantidades fracionárias.

## Fluxo principal

1. O investidor escolhe corretora, ativo, operação e quantidade.
2. O sistema obtém a cotação aplicável e calcula o valor em reais.
3. O sistema solicita confirmação simples, sem tela separada de resumo.
4. Na compra, debita o saldo e cria ou atualiza a posição por média ponderada.
5. Na venda, reduz a posição, credita o saldo e calcula o resultado realizado.
6. O sistema grava histórico e ponto patrimonial na mesma transação.

## Fluxos alternativos

- Compra norte-americana converte USD para BRL sem taxa.
- Venda parcial mantém o preço médio unitário da posição restante.
- Venda total oculta a posição da carteira.
- Recompra após posição zerada inicia novo cálculo de preço médio.
- Cotação antiga armazenada pode ser usada com aviso.

## Situações de erro

- Quantidade inválida;
- Saldo insuficiente;
- Venda superior à posição;
- Corretora inativa ou de outra conta;
- Ativo inválido;
- Ausência de cotação ou câmbio utilizável;
- Falha durante gravação transacional.

## Regras de autorização

- Somente o investidor autenticado opera saldo, corretoras e posições da própria conta.
- O frontend não pode impor preço, câmbio, saldo ou preço médio.

## Resultado esperado

A operação confirmada atualiza atomicamente saldo, posição, resultado, histórico e ponto patrimonial; uma falha não deixa efeitos parciais.

## Critérios de aceitação

### CA01 — Primeira compra

**Dado** saldo de R$ 10.000,00 e ativo cotado a R$ 20,00  
**Quando** o investidor confirmar a compra de 10 unidades  
**Então** o saldo deve ficar em R$ 9.800,00 e a posição deve ter 10 unidades com preço médio de R$ 20,00.

### CA02 — Média ponderada

**Dado** posição de 10 unidades com preço médio de R$ 20,00  
**Quando** forem compradas 10 unidades a R$ 30,00  
**Então** a posição deve ter 20 unidades com preço médio de R$ 25,00.

### CA03 — Compra sem saldo

**Dado** uma compra cujo total supera o saldo  
**Quando** o investidor confirmá-la  
**Então** ela deve ser rejeitada, informando total e saldo disponível, sem alterar dados.

### CA04 — Venda parcial

**Dado** posição de 20 unidades com preço médio de R$ 25,00  
**Quando** forem vendidas 5 unidades a R$ 30,00  
**Então** devem restar 15 unidades com preço médio de R$ 25,00 e o resultado realizado deve aumentar R$ 25,00.

### CA05 — Venda total e recompra

**Dado** uma posição vendida integralmente  
**Quando** o mesmo ativo for comprado novamente  
**Então** a posição anterior deve permanecer no histórico e o novo preço médio deve usar somente a recompra.

### CA06 — Operação norte-americana

**Dado** ativo a USD 10,00, câmbio de R$ 5,00 e quantidade 2  
**Quando** a compra for confirmada  
**Então** o saldo deve ser debitado em R$ 100,00, sem taxa cambial.

### CA07 — Preço controlado pelo backend

**Dado** um preço diferente enviado pelo cliente  
**Quando** a operação for processada  
**Então** o valor financeiro deve usar somente a cotação determinada pelo backend.

### CA08 — Atomicidade

**Dado** uma falha após iniciar a movimentação  
**Quando** a transação não for concluída  
**Então** saldo, posição, resultado, histórico e patrimônio devem permanecer no estado anterior.

## Requisitos relacionados

- RF24, RF34, RF36–RF39 e RF43–RF54;
- RN01, RN11–RN21 e RN27–RN29;
- RNF04–RNF08, RNF14, RNF17 e RNF18;
- HU08, HU09 e HU14;
- CE11–CE15, CE17, CE18 e CE20–CE22.
