# Spec — Exclusão, reativação e recriação de conta

## Objetivo

Permitir que uma conta seja inativada sem apagar seus dados e posteriormente reativada ou substituída por uma nova conta com o mesmo CPF.

## Usuários envolvidos

- Investidor.

## Pré-condições

- Exclusão exige sessão válida.
- Reativação exige a existência de conta inativa para o CPF informado.

## Entradas

- Exclusão: e-mail atual, senha atual e palavra `Excluir`;
- Reativação ou nova conta: CPF informado no fluxo de cadastro;
- Nova conta: nome, CPF, e-mail e senha válidos.

## Validações

- Exclusão somente ocorre se e-mail e senha corresponderem à conta e a palavra for exatamente `Excluir`.
- A conta a reativar deve estar inativa.
- O e-mail da conta antiga pode ser reutilizado em uma nova conta.
- CPF e e-mail devem permanecer únicos entre contas ativas.

## Fluxo principal

1. O investidor solicita a exclusão e informa as três confirmações.
2. O sistema valida os dados, inativa a conta e encerra suas sessões.
3. Saldo, corretoras, posições e histórico permanecem armazenados.
4. Posteriormente, ao informar CPF associado a conta inativa, o investidor não autenticado escolhe reativá-la.
5. Na primeira versão, o sistema reativa a conta sem exigir comprovação adicional de identidade.
6. Os dados preservados tornam-se acessíveis novamente pela conta reativada.

## Fluxos alternativos

- O investidor não autenticado escolhe criar uma nova conta em vez de reativar a anterior.
- A conta anterior permanece excluída e inacessível.
- A nova conta pode reutilizar o e-mail anterior e recebe novo saldo inicial de R$ 10.000,00.

## Situações de erro

- E-mail ou senha de confirmação incorretos;
- Palavra diferente de `Excluir`;
- Tentativa de reativar uma conta que não está inativa;
- Nova conta entra em conflito com outra conta ativa.

## Regras de autorização

- Somente o investidor autenticado pode excluir a própria conta.
- Na primeira versão, a reativação não exige comprovação de identidade; essa limitação deve ser tratada como risco conhecido e não concede acesso a outras contas ativas.
- Uma conta inativa não pode entrar pelo fluxo normal de login.

## Resultado esperado

Exclusões são lógicas, reativações restauram os dados preservados e a criação alternativa inicia uma conta independente sem tornar a anterior acessível.

## Critérios de aceitação

### CA01 — Exclusão confirmada

**Dado** um investidor autenticado com e-mail e senha corretos  
**Quando** ele escrever `Excluir` e confirmar a exclusão  
**Então** a conta deve ser inativada, suas sessões encerradas e seus dados preservados.

### CA02 — Exclusão inválida

**Dado** uma confirmação com e-mail, senha ou palavra incorreta  
**Quando** o investidor solicitar a exclusão  
**Então** a conta deve permanecer ativa e nenhum dado deve ser alterado.

### CA03 — Reativação

**Dado** um CPF associado a uma conta inativa  
**Quando** o investidor não autenticado escolher reativar essa conta  
**Então** ela deve voltar ao estado ativo com saldo, corretoras, posições e histórico preservados, sem comprovação adicional na primeira versão.

### CA04 — Nova conta no lugar da inativa

**Dado** um CPF associado somente a uma conta inativa  
**Quando** o investidor não autenticado escolher criar uma nova conta com dados válidos  
**Então** a conta anterior deve continuar inacessível e a nova deve iniciar com R$ 10.000,00.

### CA05 — Reutilização de e-mail

**Dado** que o e-mail pertence apenas a uma conta inativa  
**Quando** for usado no cadastro de uma nova conta válida  
**Então** o sistema deve permitir o cadastro.

## Requisitos relacionados

- RF10–RF14;
- RN03, RN05 e RN30;
- RNF07, RNF08 e RNF12;
- HU04;
- CE04–CE06 e CE21.
