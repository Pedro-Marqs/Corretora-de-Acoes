## Why

A exclusão lógica e a reativação de contas já estão implementadas no backend, mas ainda precisam estar disponíveis na interface para completar o ciclo de vida da conta previsto na T13. Esta alteração implementa esses fluxos no frontend utilizando os requisitos e contratos já definidos no projeto.

## What Changes

- Adicionar à interface o fluxo de exclusão lógica de conta.
- Solicitar e-mail atual, senha atual e a confirmação exata `Excluir` antes da inativação.
- Encerrar o estado de autenticação local e redirecionar o usuário após a exclusão bem-sucedida.
- Criar uma página pública para reativação de conta.
- Apresentar as opções de reativar a conta existente ou criar uma nova conta quando aplicável.
- Informar claramente que a criação de uma nova conta não recupera o acesso à conta anterior.
- Tratar estados de carregamento, validação, erro e sucesso de acordo com as especificações existentes.
- Adicionar testes de frontend para os fluxos da T13.

## Capabilities

### New Capabilities

Nenhuma. O comportamento desta alteração já está definido pelas specs existentes.

### Modified Capabilities

Nenhuma. Esta alteração implementa no frontend requisitos existentes sem modificar seu comportamento especificado.

## Impact

- Frontend React em `src/main/front/`.
- Páginas e componentes relacionados às configurações da conta.
- Nova interface pública de reativação.
- Serviços frontend responsáveis pelas operações de conta.
- Contexto e estado de autenticação após a inativação.
- Rotas públicas e privadas do frontend.
- Testes de componentes e fluxos relacionados à conta.
- Integração com os endpoints de exclusão e reativação já existentes no backend.