## Why

A transferência de posições já funciona no backend da T30, mas o investidor ainda não consegue executá-la pela aplicação. A T31 completa esse fluxo com uma interface privada que reduz erros de seleção e mantém a confirmação e os estados de operação coerentes.

## What Changes

- Adicionar a interface privada de transferência entre corretoras próprias ativas.
- Listar posições e corretoras disponíveis da conta autenticada, preenchendo a origem a partir da posição escolhida.
- Permitir selecionar destino e quantidade parcial ou total, impedindo a mesma corretora e entradas inválidas.
- Solicitar confirmação simples antes do envio, bloquear reenvio duplicado e atualizar a visão somente com a resposta do backend.
- Exibir estados de carregamento, vazio, sucesso e erro funcional, incluindo quantidade disponível quando fornecida, sem expor detalhes técnicos.
- Preservar saldo inalterado na apresentação pós-sucesso e conduzir sessão inválida ao login.

## Capabilities

### New Capabilities

### Modified Capabilities

- `transferencia-posicoes`: adicionar o comportamento observável da interface para iniciar uma transferência autenticada.
- `interface-estados`: adicionar estados, confirmação, responsividade e proteção contra reenvio específicos da transferência.

## Impact

Frontend React/Vite: página ou componente de transferência, seleção de posições e corretoras, cliente HTTP e testes de interface. Não altera endpoints, regras financeiras, persistência, saldo ou integrações externas do backend.
