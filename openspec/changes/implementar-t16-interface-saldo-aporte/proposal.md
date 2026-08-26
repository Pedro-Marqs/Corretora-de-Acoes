## Why

A API de saldo e aportes já está disponível, mas o investidor ainda não possui uma interface para visualizar seu saldo e realizar aportes. A T16 conecta o frontend à funcionalidade implementada na T15 e completa o fluxo do usuário com validação, confirmação e tratamento adequado dos estados da operação.

## What Changes

- Criar uma área de carteira para exibir o saldo atual da conta autenticada.
- Criar formulário de aporte em reais consumindo a API implementada na T15.
- Validar no frontend os requisitos básicos do aporte e apresentar erros funcionais retornados pelo backend.
- Exigir confirmação simples antes de enviar o aporte, sem criar uma página separada de resumo.
- Impedir envios duplicados enquanto o aporte estiver em andamento.
- Atualizar o saldo exibido após um aporte concluído com sucesso.
- Exibir estados de carregamento, sucesso e erro utilizando os componentes comuns já existentes.
- Formatar o saldo e os valores monetários com duas casas decimais.
- Manter a tela utilizável em desktop, tablet e celular.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `interface-estados`: completar o contrato de interface para ações financeiras, especificando confirmação simples antes da execução e prevenção de reenvio enquanto uma solicitação estiver em andamento.

## Impact

- Frontend React da área autenticada.
- Página ou componentes de carteira.
- Formulário de aporte.
- Serviço frontend responsável pelas chamadas da API de carteira.
- Navegação da área privada.
- Componentes compartilhados de carregamento, mensagens e formatação.
- Testes de componentes e integração frontend/API.
- Main spec `interface-estados`, que receberá delta para comportamentos de interação já previstos na documentação original.
- Nenhuma alteração de banco ou regra financeira do backend é necessária.