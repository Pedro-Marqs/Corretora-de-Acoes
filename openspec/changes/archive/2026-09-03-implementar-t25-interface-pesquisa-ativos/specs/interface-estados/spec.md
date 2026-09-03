## ADDED Requirements

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
