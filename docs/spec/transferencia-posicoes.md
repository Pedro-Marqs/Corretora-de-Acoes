# Spec — Transferência de posições

## Objetivo

Transferir total ou parcialmente uma posição entre corretoras da mesma conta, preservando custo e mantendo o saldo inalterado.

## Usuários envolvidos

- Investidor.

## Pré-condições

- Conta ativa e sessão válida;
- Duas corretoras ativas distintas na conta;
- Posição maior que zero na corretora de origem.

## Entradas

- Corretora de origem;
- Corretora de destino;
- Ticker da posição;
- Quantidade inteira;
- Confirmação do investidor.

## Validações

- Origem e destino devem ser diferentes e pertencer à conta.
- Quantidade deve ser inteira, positiva e não superar a posição de origem.
- A transferência não possui taxa nem altera o saldo.

## Fluxo principal

1. O investidor escolhe posição, origem, destino e quantidade.
2. O sistema valida a disponibilidade.
3. Solicita confirmação simples.
4. Reduz a quantidade e o custo total na origem.
5. Cria ou atualiza a posição no destino com o custo transferido.
6. Mantém o saldo inalterado.
7. Registra transferência, origem, destino e ponto patrimonial.

## Fluxos alternativos

- Transferência total oculta a posição zerada na origem.
- Se o destino já possui o ativo, o preço médio é recalculado por média ponderada.

## Situações de erro

- Quantidade inválida ou superior à disponível;
- Origem igual ao destino;
- Corretora inativa ou pertencente a outra conta;
- Posição inexistente;
- Falha durante a transação.

## Regras de autorização

- Somente o investidor autenticado pode transferir entre corretoras da própria conta.
- Nenhuma posição de outra conta pode ser usada como origem ou destino.

## Resultado esperado

A quantidade muda de corretora com custo preservado, saldo inalterado e registro histórico atômico.

## Critérios de aceitação

### CA01 — Transferência para destino vazio

**Dado** 10 unidades com preço médio de R$ 20,00 na origem e nenhuma no destino  
**Quando** 4 unidades forem transferidas  
**Então** a origem deve ficar com 6 unidades, o destino com 4 a preço médio de R$ 20,00 e o saldo deve permanecer igual.

### CA02 — Destino com posição

**Dado** destino com 5 unidades a R$ 30,00  
**Quando** receber 5 unidades com custo unitário de R$ 20,00  
**Então** deve ficar com 10 unidades a preço médio de R$ 25,00.

### CA03 — Quantidade excessiva

**Dado** posição de 5 unidades  
**Quando** o investidor tentar transferir 6  
**Então** a operação deve ser rejeitada, informar a disponibilidade e não alterar dados.

### CA04 — Mesma corretora

**Dado** a mesma corretora como origem e destino  
**Quando** a transferência for confirmada  
**Então** ela deve ser rejeitada sem registrar movimentação.

### CA05 — Atomicidade

**Dado** uma falha ao atualizar qualquer posição ou histórico  
**Quando** a transação não for concluída  
**Então** origem, destino, saldo e histórico devem permanecer no estado anterior.

## Requisitos relacionados

- RF24 e RF55–RF59;
- RN11–RN14, RN17, RN22, RN23, RN28 e RN29;
- RNF04–RNF08, RNF14 e RNF18;
- HU10;
- CE13, CE16, CE20 e CE21.
