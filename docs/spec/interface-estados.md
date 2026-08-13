# Spec — Interface integrada e tratamento de estados

## Objetivo

Padronizar a experiência das funcionalidades em React, incluindo navegação, confirmações, mensagens, estados de carregamento e responsividade.

## Usuários envolvidos

- Investidor.

## Pré-condições

- Frontend local conectado à API local;
- Para áreas privadas, sessão válida.

## Entradas

- Ações do usuário em formulários, filtros, confirmações e navegação;
- Respostas de sucesso e erro da API;
- Estados vazio, carregando, concluído, indisponível e desatualizado.

## Validações

- Formulários devem indicar campos obrigatórios e erros funcionais.
- Valores monetários devem exibir duas casas decimais.
- Datas e horários devem usar Brasília.
- CPF e e-mail devem aparecer parcialmente ocultados.
- Ações financeiras exigem confirmação simples, sem tela separada de resumo.

## Fluxo principal

1. O usuário acessa uma tela permitida para seu estado de autenticação.
2. A interface indica carregamento durante a consulta.
3. Exibe dados ou um estado vazio compreensível.
4. O usuário executa uma ação e recebe confirmação simples quando aplicável.
5. Após a resposta, a interface apresenta sucesso ou erro funcional e atualiza os dados afetados.

## Fluxos alternativos

- Cotações e câmbio antigos exibem aviso e horário.
- Listagens paginadas permitem navegar mantendo filtros.
- Em telas menores, conteúdos são reorganizados sem exigir rolagem horizontal da página.
- Sessão inválida direciona o usuário para autenticação.

## Situações de erro

- Falha de conexão com a API;
- Resposta funcional de validação;
- Sessão encerrada;
- Dados vazios ou parcialmente indisponíveis;
- Múltiplos acionamentos enquanto uma solicitação está em andamento.

## Regras de autorização

- Telas privadas não devem apresentar dados sem sessão válida.
- Elementos ocultos na interface não substituem a autorização do backend.
- Após logout ou revogação de sessão, dados privados deixam de estar acessíveis.

## Resultado esperado

Todas as funcionalidades apresentam estados compreensíveis, dados privados protegidos e comportamento utilizável em desktop, tablet e celular.

## Critérios de aceitação

### CA01 — Estado de carregamento

**Dado** uma consulta ainda em andamento  
**Quando** a tela aguardar a resposta  
**Então** deve indicar carregamento e impedir reenvio acidental da mesma ação.

### CA02 — Erro funcional

**Dado** uma solicitação rejeitada pela API  
**Quando** a resposta for apresentada  
**Então** a interface deve mostrar mensagem explicativa sem stack trace, classe, SQL ou credencial.

### CA03 — Confirmação de movimentação

**Dado** aporte, compra, venda ou transferência preenchida  
**Quando** o usuário solicitar a execução  
**Então** deve ser exibida uma confirmação simples na mesma etapa, sem página separada de resumo.

### CA04 — Dados sensíveis

**Dado** a área da conta  
**Quando** CPF e e-mail forem exibidos  
**Então** ambos devem aparecer parcialmente ocultados.

### CA05 — Valores e horários

**Dado** valores monetários e datas retornados pela API  
**Quando** forem exibidos  
**Então** os valores devem ter duas casas decimais e as datas devem representar o horário de Brasília.

### CA06 — Responsividade

**Dado** cada tela funcional em visualizações de desktop, tablet e celular usadas na verificação  
**Quando** o usuário navegar e executar suas ações  
**Então** todos os controles devem permanecer utilizáveis sem rolagem horizontal da página.

### CA07 — Sessão inválida

**Dado** uma sessão expirada ou revogada  
**Quando** uma área privada for acessada  
**Então** nenhum dado privado deve ser mostrado e o usuário deve ser direcionado ao login.

### CA08 — Aviso de desatualização

**Dado** cotação com mais de 24 horas ou câmbio com mais de sete dias  
**Quando** o valor for apresentado  
**Então** a interface deve mostrar aviso e o horário do dado utilizado.

## Requisitos relacionados

- RF09, RF15, RF21, RF24, RF32, RF33, RF37, RF42, RF44, RF48, RF50, RF52, RF57 e RF64–RF70;
- RNF05, RNF06, RNF12, RNF14 e RNF15;
- HU01–HU14;
- CE01–CE22.
