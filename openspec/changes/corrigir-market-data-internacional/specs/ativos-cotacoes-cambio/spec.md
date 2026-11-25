## ADDED Requirements

### Requirement: Autenticação específica e segura de provedores

As integrações SHALL enviar o método de autenticação exigido por cada provedor sem substituir um contrato por outro. Para a AwesomeAPI, quando `AWESOME_API_KEY` estiver configurada, a requisição SHALL enviar a chave no cabeçalho `x-api-key` e SHALL NOT enviar a mesma chave como `Authorization: Bearer`. A integração da Twelve Data SHALL continuar usando somente o método oficialmente suportado e SHALL validar uma consulta autenticada antes de qualquer conclusão operacional. Chaves, tokens, URLs com credenciais e corpos sensíveis SHALL NOT aparecer em logs, exceções públicas ou mensagens de diagnóstico.

#### Scenario: AwesomeAPI recebe o cabeçalho correto

- **WHEN** o sistema consultar USD/BRL com `AWESOME_API_KEY` configurada
- **THEN** a requisição SHALL conter `x-api-key` com a chave configurada, SHALL NOT conter `Authorization: Bearer` com essa chave e SHALL permitir que a resposta válida seja convertida em câmbio oficial

#### Scenario: Twelve Data autenticada é validada

- **WHEN** o sistema consultar uma ação norte-americana com `TWELVE_DATA_API_KEY` configurada e o provedor retornar uma cotação completa
- **THEN** o sistema SHALL aceitar a cotação oficial, preservar seu ticker, preço, moeda, fonte e instante, e SHALL registrar no teste a autenticação efetivamente utilizada sem expor a chave

#### Scenario: Credencial ausente ou rejeitada

- **WHEN** a credencial obrigatória estiver ausente, inválida ou for rejeitada pelo provedor
- **THEN** o sistema SHALL classificar a falha como autenticação/configuração, SHALL preservar qualquer dado válido já armazenado e SHALL NOT publicar a credencial nem uma URL que a contenha

### Requirement: Cache-first e consistência do câmbio USD/BRL

O sistema SHALL consultar primeiro o cache persistido de USD/BRL. Um valor dentro do TTL definido para câmbio SHALL ser retornado sem chamada externa. Um valor fora do TTL SHALL continuar disponível como fallback stale enquanto uma atualização controlada é tentada; se a atualização falhar, o sistema SHALL preservar o valor, seu `quotedAt`, sua fonte e o indicador de desatualização. O sistema SHALL NOT criar valor, timestamp ou cotação sintética e SHALL NOT alterar a persistência do histórico patrimonial diário.

#### Scenario: Cache cambial fresco evita chamada externa

- **WHEN** existir USD/BRL persistido dentro do TTL de câmbio
- **THEN** o serviço SHALL retornar esse valor oficial sem chamar a AwesomeAPI ou outra fonte externa e SHALL preservar sua fonte, instante e qualidade

#### Scenario: Cache cambial stale é preservado após falha

- **WHEN** existir USD/BRL persistido fora do TTL e a atualização externa falhar
- **THEN** o serviço SHALL retornar o último valor armazenado marcado como desatualizado, SHALL registrar a falha de forma segura e SHALL NOT sobrescrever o cache com valor inventado ou parcial

#### Scenario: Ausência de cache bloqueia dependência cambial

- **WHEN** não existir USD/BRL armazenado utilizável e a atualização externa falhar
- **THEN** o sistema SHALL preservar a ausência de valor e SHALL bloquear a operação dependente conforme o contrato existente, sem alterar saldo, posição, histórico ou patrimônio

### Requirement: Concorrência e rate limit dos provedores

O sistema SHALL impedir chamadas duplicadas concorrentes para a mesma atualização de USD/BRL e SHALL impedir ciclos do mesmo tipo de se sobreporem. HTTP 429 SHALL ser classificado como limitação do provedor; quando `Retry-After` for válido, uma nova tentativa controlada SHALL respeitar sua indicação dentro de um limite operacional definido. Sem `Retry-After`, o sistema SHALL usar backoff limitado e SHALL encerrar as tentativas sem tempestade de requisições. O fallback existente SHALL ser aplicado após o limite de tentativas, sem mascarar a causa operacional nos logs seguros.

#### Scenario: Requisições concorrentes compartilham uma atualização

- **WHEN** duas ou mais requisições solicitarem a mesma atualização cambial simultaneamente
- **THEN** o sistema SHALL executar no máximo uma chamada externa ativa para aquele recurso e SHALL entregar às demais o resultado persistido ou a falha controlada da mesma tentativa

#### Scenario: HTTP 429 respeita limite controlado

- **WHEN** um provedor responder HTTP 429 com ou sem `Retry-After`
- **THEN** o sistema SHALL registrar o provedor e a classificação de rate limit sem segredo, SHALL aguardar somente o atraso permitido pela política limitada, SHALL evitar retries ilimitados e SHALL preservar/retornar o fallback válido quando disponível

#### Scenario: Falhas de provedor são observáveis sem segredo

- **WHEN** uma chamada externa falhar por autenticação, rate limit, timeout, resposta inválida ou erro do servidor
- **THEN** o log SHALL identificar o provedor, a categoria da falha, o status HTTP quando disponível e a decisão de fallback, e SHALL excluir API keys, tokens, parâmetros sensíveis, payloads completos e stack traces de mensagens públicas

### Requirement: Regressão de mercados suportados

As consultas de ativos brasileiros e norte-americanos SHALL continuar usando os contratos oficiais e os dados retornados pelos provedores. Uma falha ou limitação de um provedor SHALL preservar o último valor válido conforme o TTL vigente ou bloquear a operação sem inventar preço, câmbio, saldo, posição ou timestamp.

#### Scenario: Cotação brasileira permanece funcional

- **WHEN** um ticker brasileiro válido for consultado e a fonte retornar uma resposta completa
- **THEN** o sistema SHALL retornar e preservar a cotação brasileira oficial com fonte e instante correspondentes

#### Scenario: Cotação norte-americana usa consulta autenticada

- **WHEN** um ticker norte-americano válido for consultado e a Twelve Data retornar uma resposta completa autenticada
- **THEN** o sistema SHALL retornar a cotação oficial em USD e SHALL manter a conversão para BRL dependente de USD/BRL utilizável, sem cálculo ou preço fictício

#### Scenario: Falhas não corrompem operações financeiras

- **WHEN** a cotação ou o câmbio necessários não estiverem utilizáveis após falha externa e fallback
- **THEN** o sistema SHALL bloquear a operação dependente sem mutar saldo, posição, preço médio, movimento ou patrimônio
