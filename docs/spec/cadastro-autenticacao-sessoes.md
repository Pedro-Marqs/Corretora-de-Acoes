# Spec — Cadastro, autenticação e sessões

## Objetivo

Permitir a criação de contas individuais e o acesso seguro às funcionalidades autenticadas.

## Usuários envolvidos

- Investidor.

## Pré-condições

- Para cadastro, o investidor não precisa estar autenticado.
- Para alterar e-mail ou senha, o investidor deve possuir sessão válida.

## Entradas

- Cadastro: nome, CPF, e-mail e senha;
- Login: e-mail e senha;
- Alteração: novo e-mail ou nova senha e senha atual.

## Validações

- Todos os campos de cadastro são obrigatórios.
- CPF deve ter formato e dígitos verificadores válidos.
- E-mail deve ter formato válido e ser único entre contas ativas.
- Senha deve ter pelo menos oito caracteres, com minúscula, maiúscula, número e caractere especial.
- Nome e CPF não podem ser alterados.
- Senha atual deve ser válida para alterar e-mail ou senha.

## Fluxo principal

1. O investidor não autenticado informa os dados de cadastro.
2. O sistema valida os dados e cria a conta ativa.
3. O sistema cria saldo de R$ 10.000,00 e registra o saldo inicial.
4. O usuário informa e-mail e senha no login.
5. O sistema valida as credenciais e cria uma sessão.
6. O investidor acessa somente os dados da própria conta.
7. No logout, o sistema encerra a sessão atual.

## Fluxos alternativos

- O investidor altera e-mail ou senha após confirmar a senha atual.
- Após a alteração, todas as sessões da conta são encerradas.
- A área da conta exibe CPF e e-mail parcialmente ocultados.

## Situações de erro

- Campo obrigatório ausente;
- CPF ou e-mail inválido;
- Senha fora da composição exigida;
- CPF ou e-mail já usado por conta ativa;
- Credenciais inválidas ou conta inativa;
- Senha atual incorreta na alteração.

## Regras de autorização

- Investidores não autenticados podem cadastrar e autenticar contas.
- Somente o investidor autenticado pode consultar ou alterar a própria conta.
- Identificadores recebidos do cliente não podem conceder acesso a outra conta.

## Resultado esperado

Conta e saldo inicial são criados uma única vez, as credenciais permitem uma sessão válida e alterações de credenciais revogam todas as sessões.

## Critérios de aceitação

### CA01 — Cadastro válido

**Dado** um investidor não autenticado com CPF e e-mail não usados por conta ativa e senha válida  
**Quando** ele concluir o cadastro com todos os campos  
**Então** uma conta ativa deve ser criada com saldo de R$ 10.000,00 e registro de saldo inicial.

### CA02 — Cadastro inválido

**Dado** um cadastro com CPF inválido, e-mail inválido ou senha fora das regras  
**Quando** o investidor tentar concluí-lo  
**Então** nenhuma conta ou saldo deve ser criado e os campos inválidos devem ser indicados.

### CA03 — Login

**Dado** uma conta ativa  
**Quando** forem informados e-mail e senha corretos  
**Então** o sistema deve criar uma sessão associada à conta.

### CA04 — Credenciais inválidas

**Dado** e-mail ou senha incorretos, ou uma conta inativa  
**Quando** ocorrer uma tentativa de login  
**Então** nenhuma sessão deve ser criada e a mensagem não deve revelar qual credencial falhou.

### CA05 — Alteração de credencial

**Dado** um investidor autenticado que informou a senha atual correta  
**Quando** alterar e-mail ou senha  
**Então** a alteração deve ser persistida e todas as sessões da conta devem ser invalidadas.

### CA06 — Isolamento

**Dado** um investidor autenticado e um identificador pertencente a outra conta  
**Quando** ele tentar consultar ou alterar esse registro  
**Então** o acesso deve ser negado sem exposição dos dados.

## Requisitos relacionados

- RF01–RF09;
- RN03–RN05;
- RNF08–RNF14;
- HU01–HU03;
- CE01–CE05 e CE21.
