# Interface e estados Specification

## Purpose

Oferecer uma interface responsiva e explicita sobre estados de carregamento, vazio, sucesso, erro e dados desatualizados.

## Requirements

### Requirement: Estados de interface

A interface SHALL apresentar estados de carregamento, vazio, sucesso, erro e desatualizado de forma distinguivel.

#### Scenario: Falha de requisicao

- **WHEN** uma requisicao falhar
- **THEN** a interface SHALL exibir mensagem funcional, preservar contexto e permitir nova tentativa quando aplicavel

#### Scenario: Nenhum resultado

- **WHEN** uma consulta valida nao retornar dados
- **THEN** a interface SHALL exibir estado vazio sem tratar como erro

### Requirement: Responsividade e formatacao

A interface SHALL funcionar em desktop, tablet e celular sem rolagem horizontal e exibir dinheiro com duas casas.

#### Scenario: Visualizacao em celular

- **WHEN** o investidor acessar uma tela em viewport estreito
- **THEN** o conteudo SHALL permanecer utilizavel sem sobreposicao ou rolagem horizontal da pagina
