# Interface e estados Specification

## Purpose

Oferecer uma interface responsiva e explicita sobre estados de carregamento, vazio, sucesso, erro e dados desatualizados.

## Requirements

### Requirement: Estados de interface

A interface SHALL apresentar estados de carregamento, vazio, sucesso, erro e desatualizado de forma distinguivel e MUST impedir o reenvio acidental de uma mesma acao enquanto sua solicitacao estiver em andamento.

#### Scenario: Falha de requisicao

- **WHEN** uma requisicao falhar
- **THEN** a interface SHALL exibir mensagem funcional, preservar contexto e permitir nova tentativa quando aplicavel

#### Scenario: Nenhum resultado

- **WHEN** uma consulta valida nao retornar dados
- **THEN** a interface SHALL exibir estado vazio sem tratar como erro

#### Scenario: Solicitacao em andamento

- **WHEN** uma acao estiver aguardando resposta da API
- **THEN** a interface SHALL indicar o estado de carregamento e MUST impedir novo envio da mesma acao ate a solicitacao terminar

### Requirement: Responsividade e formatacao

A interface SHALL funcionar em desktop, tablet e celular sem rolagem horizontal e exibir dinheiro com duas casas.

#### Scenario: Visualizacao em celular

- **WHEN** o investidor acessar uma tela em viewport estreito
- **THEN** o conteudo SHALL permanecer utilizavel sem sobreposicao ou rolagem horizontal da pagina

### Requirement: Confirmacao de movimentacao financeira

A interface MUST solicitar confirmacao simples antes de executar uma movimentacao financeira iniciada pelo investidor, sem exigir uma pagina separada de resumo.

#### Scenario: Confirmar aporte

- **WHEN** o investidor preencher um aporte valido e solicitar sua execucao
- **THEN** a interface SHALL apresentar uma confirmacao simples antes de enviar a operacao

#### Scenario: Cancelar confirmacao

- **WHEN** o investidor cancelar a confirmacao de uma movimentacao
- **THEN** a interface MUST NOT enviar a operacao e SHALL preservar o estado necessario para que o usuario possa continuar na mesma tela
