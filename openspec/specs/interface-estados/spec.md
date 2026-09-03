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

### Requirement: Estados da pesquisa de ativos

A interface SHALL apresentar a pesquisa de ativos como uma tela privada, com estados visualmente distinguíveis de carregamento, resultado, vazio, erro e dados desatualizados. Enquanto a consulta estiver em andamento, SHALL impedir novo envio da mesma pesquisa; em falhas recuperáveis, SHALL manter o ticker informado e permitir nova tentativa.

#### Scenario: Consulta em andamento
- **WHEN** o investidor enviar um ticker e a API ainda não tiver respondido
- **THEN** a interface SHALL indicar carregamento e SHALL impedir reenvio acidental da mesma consulta até a resposta terminar

#### Scenario: Consulta sem resultado
- **WHEN** uma pesquisa válida não retornar um ativo
- **THEN** a interface SHALL exibir um estado vazio compreensível e SHALL NOT tratar a ausência de resultado como sucesso com dados ou como erro técnico

#### Scenario: Erro funcional ou indisponibilidade
- **WHEN** a API retornar ativo inválido, mercado rejeitado, resposta incompleta, ausência de cache ou falha de conexão
- **THEN** a interface SHALL exibir mensagem funcional sem stack trace, classe interna, SQL, credencial ou corpo técnico e SHALL permitir nova tentativa quando aplicável

#### Scenario: Sessão inválida
- **WHEN** a consulta ocorrer sem sessão válida ou a API responder que a sessão foi encerrada
- **THEN** a interface SHALL evitar exibir dados privados de mercado associados à área autenticada e SHALL direcionar o investidor ao login conforme a proteção de rotas existente

#### Scenario: Pesquisa responsiva
- **WHEN** o investidor acessar a tela em viewport de desktop, tablet ou celular
- **THEN** o formulário, resultado, avisos e mensagens SHALL permanecer utilizáveis sem rolagem horizontal da página e os valores monetários SHALL usar duas casas decimais
