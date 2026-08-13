# Spec — Saldo e aportes

## Objetivo

Administrar o saldo fictício único da conta e permitir aportes sem tratá-los como rendimento.

## Usuários envolvidos

- Investidor.

## Pré-condições

- Conta ativa e sessão válida;
- Saldo inicial criado no cadastro.

## Entradas

- Valor do aporte em reais.

## Validações

- O aporte deve ser numérico e igual ou superior a R$ 10,00.
- Valores são tratados com duas casas decimais e arredondamento `HALF_UP`.
- A conta usada deve ser a associada à sessão.

## Fluxo principal

1. O investidor consulta o saldo compartilhado por suas corretoras.
2. Informa um aporte válido.
3. Confirma a operação.
4. O sistema aumenta o saldo e registra a movimentação e o ponto patrimonial.

## Fluxos alternativos

- Não existe valor máximo de aporte.
- Uma conta sem corretoras também pode receber aporte.

## Situações de erro

- Valor ausente, não numérico, negativo ou menor que R$ 10,00;
- Falha ao gravar saldo, histórico ou ponto patrimonial;
- Tentativa de operar conta de outro usuário.

## Regras de autorização

- Somente o investidor autenticado pode consultar ou aportar na própria conta.

## Resultado esperado

Saldo, histórico e ponto patrimonial são atualizados atomicamente; o aporte não aumenta indicadores de rendimento.

## Critérios de aceitação

### CA01 — Saldo inicial

**Dado** uma conta recém-criada  
**Quando** o investidor consultar o saldo  
**Então** deve visualizar R$ 10.000,00.

### CA02 — Aporte válido

**Dado** saldo de R$ 10.000,00  
**Quando** o investidor aportar R$ 500,00  
**Então** o saldo deve passar a R$ 10.500,00 e uma movimentação de aporte deve ser registrada.

### CA03 — Aporte abaixo do mínimo

**Dado** um aporte inferior a R$ 10,00  
**Quando** o investidor tentar confirmá-lo  
**Então** saldo, histórico e patrimônio devem permanecer inalterados.

### CA04 — Aporte não é lucro

**Dado** uma conta sem resultado de investimentos  
**Quando** um aporte for concluído  
**Então** saldo e patrimônio devem aumentar, mas lucro, prejuízo e valorização devem permanecer inalterados.

### CA05 — Atomicidade

**Dado** uma falha durante a gravação do aporte  
**Quando** a transação não puder ser concluída  
**Então** nenhuma alteração de saldo, histórico ou ponto patrimonial deve permanecer.

## Requisitos relacionados

- RF03 e RF15–RF18;
- RN01, RN02, RN06, RN07, RN24 e RN28;
- RNF04–RNF08 e RNF14;
- HU05;
- CE07, CE20 e CE21.
