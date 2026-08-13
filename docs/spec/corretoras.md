# Spec — Cadastro e administração de corretoras

## Objetivo

Permitir que o investidor associe à conta somente corretoras com CNPJ ativo e registro CVM na categoria CTVM.

## Usuários envolvidos

- Investidor;
- Serviços externos de CNPJ, CEP e CVM.

## Pré-condições

- Conta ativa e sessão válida;
- Internet disponível para nova validação;
- Fonte processável de dados abertos da CVM validada antes da implementação completa.

## Entradas

- CNPJ da corretora.

## Validações

- A busca aceita somente CNPJ.
- O CNPJ deve ser válido e estar ativo.
- A instituição deve constar na CVM como `CTVM`.
- O mesmo CNPJ não pode ter duas associações ativas na mesma conta.
- Dados obrigatórios: CNPJ, razão social, nome fantasia, situação, autorização e endereço estruturado.

## Fluxo principal

1. O investidor informa um CNPJ.
2. O sistema consulta situação cadastral, endereço e dados oficiais da CVM.
3. O sistema apresenta os dados consolidados.
4. O investidor confirma o cadastro.
5. O sistema associa a corretora à conta.

## Fluxos alternativos

- Ao consultar corretora conhecida, nome e endereço válidos são atualizados.
- Uma corretora removida pode ser reativada, preservando o histórico anterior.
- O investidor pode remover logicamente uma corretora sem posições abertas.

## Situações de erro

- CNPJ inválido ou inativo;
- Instituição ausente na CVM ou sem categoria CTVM;
- CNPJ duplicado na conta;
- Resposta externa indisponível ou incompleta;
- Tentativa de remover corretora com posição maior que zero.

## Regras de autorização

- Somente o investidor autenticado pode administrar corretoras da própria conta.
- Corretoras de outra conta não podem ser consultadas ou alteradas por identificador interno.

## Resultado esperado

A conta passa a possuir uma associação ativa com uma CTVM válida, sem duplicidade e com histórico preservado após remoção ou recadastro.

## Critérios de aceitação

### CA01 — Cadastro válido

**Dado** um CNPJ ativo que consta na CVM como CTVM  
**Quando** o investidor confirmar seu cadastro  
**Então** a corretora deve ser associada à conta com os dados obrigatórios.

### CA02 — Instituição não autorizada

**Dado** um CNPJ ativo que não consta na CVM como CTVM  
**Quando** o investidor tentar cadastrá-lo  
**Então** o cadastro deve ser rejeitado com o motivo identificado.

### CA03 — Duplicidade

**Dado** uma corretora já ativa na conta  
**Quando** seu CNPJ for cadastrado novamente  
**Então** nenhuma segunda associação deve ser criada.

### CA04 — Remoção permitida

**Dado** uma corretora sem posições abertas  
**Quando** o investidor removê-la  
**Então** ela deve ficar inativa, deixar as opções operacionais e permanecer no histórico.

### CA05 — Remoção bloqueada

**Dado** uma corretora com posição maior que zero  
**Quando** o investidor tentar removê-la  
**Então** a ação deve ser rejeitada e a corretora deve permanecer ativa.

### CA06 — Resposta incompleta

**Dado** dados válidos já armazenados  
**Quando** uma nova consulta retornar campos ausentes  
**Então** os valores válidos anteriores não devem ser apagados.

## Requisitos relacionados

- RF19–RF28;
- RN08–RN10 e RN30;
- RNF08, RNF14, RNF17 e RNF19;
- HU06;
- CE08–CE10, CE21 e CE22.
