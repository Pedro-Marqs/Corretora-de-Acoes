## Context

A T21 antecede a implementação dos adapters definitivos de mercado da T22. Seu objetivo é eliminar incertezas sobre contratos externos antes que eles sejam transformados em portas e modelos internos permanentes.

O projeto já define que:

- Brapi será a fonte dos ativos e cotações do mercado brasileiro;
- Twelve Data será a fonte dos ativos e cotações do mercado norte-americano, condicionada à validação prévia de cobertura e campos;
- AwesomeAPI será a fonte do USD/BRL;
- somente mercados brasileiro e norte-americano serão aceitos;
- ticker, nome, mercado, moeda, cotação e horário são dados obrigatórios;
- cotações brasileiras serão atualizadas em ciclos de cinco minutos;
- cotações norte-americanas e USD/BRL serão atualizadas uma vez ao dia;
- valores externos serão posteriormente normalizados e arredondados pelo backend;
- chaves de API não podem ser versionadas;
- falhas externas não podem apagar dados válidos previamente armazenados.

A main spec `ativos-cotacoes-cambio` já define o comportamento funcional esperado. A T21 não altera esse contrato e deverá produzir apenas evidências técnicas para a T22.

Os planos, limites, endpoints e formatos dos fornecedores podem mudar. Por isso, toda conclusão da T21 deverá registrar a data da verificação e distinguir informação observada em documentação de comportamento confirmado por chamada real.

See `proposal.md` - Why e `docs/spec/ativos-cotacoes-cambio.md`.

## Goals / Non-Goals

**Goals:**

- Identificar o conjunto mínimo de endpoints necessário em cada fornecedor.
- Confirmar os campos necessários para formar ativo, cotação e câmbio.
- Confirmar como determinar de maneira confiável se um ativo pertence ao mercado brasileiro ou norte-americano.
- Determinar quais timestamps representam efetivamente o instante do dado de mercado.
- Confirmar comportamento para símbolo inexistente, mercado não aceito e resposta incompleta.
- Confirmar autenticação e configuração mínima exigida por cada fornecedor.
- Medir os limites relevantes do plano disponível.
- Estimar o consumo dos ciclos previstos pelo projeto.
- Confirmar que a cobertura da Twelve Data atende aos ativos norte-americanos necessários.
- Produzir fixtures mínimas baseadas nos contratos observados.
- Tornar as evidências reproduzíveis por smoke tests explícitos.
- Manter a suíte normal de testes independente da internet.
- Registrar qualquer bloqueio que precise ser resolvido antes da T22.

**Non-Goals:**

- Criar ports definitivos de mercado.
- Criar DTOs de produção.
- Criar adapters definitivos.
- Persistir ativos, cotações ou câmbio.
- Implementar cache.
- Implementar scheduler.
- Criar endpoints de pesquisa de ativos.
- Implementar fallback de cotação.
- Criar operações de compra ou venda.
- Adicionar retry, circuit breaker ou política de resiliência definitiva.
- Escolher um plano pago apenas para fazer a prova técnica.
- Colocar credenciais reais em arquivos versionados.
- Fazer a suíte normal depender da disponibilidade dos provedores.

## Decisions

### 1. Separar documentação, prova externa e fixtures offline

Cada fornecedor será validado em três níveis:

1. documentação oficial atual;
2. chamada real controlada;
3. fixture mínima derivada do formato confirmado.

A documentação registra o contrato declarado pelo fornecedor.

A chamada real confirma que o contrato necessário funciona na prática com o plano/credencial disponível.

A fixture permite que o formato seja reproduzido posteriormente sem internet.

**Alternativa considerada:** confiar somente na documentação oficial.

**Decisão:** a própria finalidade da T21 é eliminar suposições antes da implementação definitiva.

### 2. Smoke tests externos serão opt-in

As provas que acessam Brapi, Twelve Data ou AwesomeAPI não serão executadas automaticamente pela suíte normal.

Deverá existir um mecanismo explícito de habilitação, como configuração ou variável de ambiente, para indicar que testes externos devem ser executados.

Sem essa habilitação:

- `mvn test` permanece determinístico;
- nenhuma credencial externa é exigida;
- indisponibilidade de fornecedor não quebra o build normal.

**Alternativa considerada:** executar os serviços reais em todos os testes.

**Decisão:** testes de contrato externo servem para investigação e verificação periódica, não para determinar a estabilidade do build local.

### 3. Não criar abstrações definitivas na T21

A prova técnica poderá utilizar código pequeno e descartável em escopo de teste ou estrutura claramente experimental.

Ela não deverá criar interfaces de domínio ou adapters que a T22 seja obrigada a preservar.

O código deverá ser suficiente apenas para:

- executar a chamada;
- capturar o contrato observado;
- validar campos;
- reproduzir falhas relevantes.

**Alternativa considerada:** aproveitar a T21 para começar os clients de produção.

**Decisão:** isso misturaria descoberta com implementação e poderia cristalizar suposições antes da validação.

### 4. Brapi será validada usando o contrato atual de cotação de ações

A prova deverá verificar o endpoint atual destinado à cotação de ativos brasileiros e confirmar pelo menos:

- ticker solicitado e ticker resolvido;
- nome curto e/ou nome completo;
- moeda;
- preço atual utilizável;
- instante da cotação;
- comportamento para símbolo inexistente ou não suportado.

O estudo deverá identificar qual campo de nome será usado como principal na T22 e se existe fallback seguro quando ele estiver ausente.

O instante utilizado para idade da cotação deverá representar o dado de mercado e não simplesmente o instante em que a requisição foi realizada.

A identificação interna de mercado `BR` deverá decorrer do fato de o adapter futuro utilizar uma fonte/endpoint limitado ao mercado brasileiro, e não apenas de `currency = BRL`.

**Alternativa considerada:** inferir mercado somente pela moeda.

**Decisão:** moeda não é identidade suficiente de mercado e essa inferência seria frágil.

### 5. A prova Brapi deve distinguir sandbox de uso normal autenticado

Ativos disponibilizados pelo fornecedor para teste sem token poderão ser usados para uma prova inicial.

A validação também deverá documentar:

- quando token é obrigatório;
- forma recomendada de autenticação;
- limites do plano utilizado;
- quantidade máxima de símbolos por chamada;
- eventual atraso de cotação associado ao plano.

Token real, quando utilizado, deverá vir exclusivamente de configuração local não versionada.

A ausência de token em um ambiente normal não deverá ser mascarada pelo fato de um ticker de demonstração funcionar sem autenticação.

### 6. O consumo da Brapi será avaliado contra o ciclo de cinco minutos

A T21 deverá calcular a viabilidade do ciclo brasileiro usando pelo menos:

- número de símbolos brasileiros únicos;
- frequência de atualização de cinco minutos;
- quantidade de símbolos permitida por chamada no plano;
- período efetivo em que o scheduler será executado;
- limite mensal do plano.

A análise deverá usar uma fórmula equivalente a:

`requisições = ciclos × ceil(símbolos únicos / símbolos por chamada)`

O resultado deverá ser documentado para mais de um tamanho representativo de carteira.

Não será assumido que um plano é adequado apenas porque uma única chamada manual funciona.

**Alternativa considerada:** ignorar limites até o scheduler ser implementado.

**Decisão:** o objetivo da T21 inclui comprovar que o desenho futuro não depende de uma taxa de chamadas impossível para o plano utilizado.

### 7. Twelve Data será validada com foco no contrato mínimo para ações dos EUA

A prova deverá confirmar um fluxo que consiga obter:

- ticker;
- nome;
- exchange ou informação equivalente;
- moeda;
- cotação;
- timestamp do dado.

O endpoint de cotação atual será o primeiro candidato porque concentra a maior parte desses campos.

Endpoints de descoberta/referência poderão ser usados somente se forem necessários para determinar corretamente mercado ou cobertura.

A T21 deverá procurar a menor combinação de endpoints que permita à T22 produzir o modelo interno completo.

**Alternativa considerada:** utilizar vários endpoints antecipadamente para obter informações adicionais.

**Decisão:** cada chamada adicional aumenta consumo de créditos e pontos de falha sem benefício quando seus dados não são necessários ao domínio.

### 8. Mercado norte-americano não será inferido apenas por USD

A Twelve Data cobre diversos países e mercados, portanto:

`currency = USD`

não será suficiente para classificar um ativo como mercado `US`.

A prova deverá verificar uma forma reproduzível de restringir ou identificar instrumentos norte-americanos, utilizando informação como país, exchange, MIC ou filtro equivalente oferecido pelo fornecedor.

Deverão ser testados:

- um ativo norte-americano conhecido;
- um instrumento de mercado não aceito pelo projeto.

A T22 somente poderá mapear `market = US` depois que esse critério estiver documentado.

**Alternativa considerada:** classificar qualquer ativo em USD como norte-americano.

**Decisão:** isso permitiria ativos de mercados não suportados.

### 9. Cobertura da Twelve Data é um gate para a T22

A prova não precisa verificar toda ação norte-americana existente.

Ela deverá, porém, verificar uma amostra suficiente para demonstrar o fluxo esperado, incluindo pelo menos ativos de exchanges relevantes ao projeto.

Também deverá confirmar:

- capacidade de descoberta;
- obtenção da cotação;
- moeda;
- identificação do mercado;
- timestamp.

Se o plano disponível ou a fonte não atender ao fluxo mínimo, o resultado será documentado como bloqueio técnico.

A T21 não deverá adaptar silenciosamente os requisitos ou substituir o fornecedor por outro.

**Alternativa considerada:** considerar AAPL funcionando como prova automática de cobertura completa.

**Decisão:** um único símbolo prova o contrato básico, mas não prova a cobertura necessária da fonte.

### 10. O consumo da Twelve Data será calculado em créditos, não apenas requisições HTTP

Como o fornecedor utiliza pesos/créditos por endpoint e símbolo, a análise deverá considerar:

- peso do endpoint escolhido;
- quantidade de símbolos norte-americanos únicos;
- limite por minuto;
- limite diário;
- possibilidade ou não de agrupamento/batch no plano utilizado.

Para o ciclo diário previsto pelo projeto deverá ser registrada a quantidade de créditos necessária para uma carteira representativa.

A estratégia da T22 deverá ser possível dentro desses limites sem depender de chamadas desnecessárias.

### 11. Credencial Twelve Data será sempre externa ao repositório

A chave utilizada pelos smoke tests deverá vir de variável de ambiente ou configuração local ignorada pelo Git.

A documentação poderá registrar:

- nome da configuração esperada;
- onde obter a chave;
- quais testes precisam dela;

mas nunca seu valor.

A prova deverá preferir o mecanismo de autenticação recomendado atualmente pelo fornecedor quando compatível com o cliente utilizado.

### 12. AwesomeAPI será validada especificamente para `USD-BRL`

A T21 não precisa investigar a API completa de moedas.

A prova deverá validar o endpoint de última cotação para `USD-BRL` e confirmar:

- moeda base `USD`;
- moeda de destino `BRL`;
- `bid`;
- `ask`;
- timestamp;
- comportamento para par inexistente;
- resposta incompleta;
- efeito de autenticação/cache quando relevante.

O timestamp numérico informado pelo fornecedor será preferido como referência técnica de horário por evitar dependência de parsing de texto localizado.

### 13. T21 deverá registrar explicitamente qual campo cambial será consumido pela T22

A AwesomeAPI fornece mais de um preço para o par.

Como o domínio da primeira versão trabalha com uma única conversão direta USD/BRL e não modela spread ou taxa cambial, a T21 deverá documentar qual campo único será adotado pelo adapter da T22 e por quê.

A decisão deverá respeitar:

- a semântica oficial de `bid` e `ask`;
- RN14, que não modela taxa cambial;
- RN27, que exige uma conversão direta única;
- consistência entre compra, venda, patrimônio e dashboards.

Essa escolha deverá ser registrada como conclusão da prova técnica antes do início da T22, evitando que cada serviço escolha um preço diferente.

**Alternativa considerada:** deixar `bid` e `ask` chegarem ao domínio e decidir em cada operação.

**Decisão:** o modelo atual prevê uma única cotação USD/BRL; expor os dois lados criaria comportamento não especificado.

### 14. Não utilizar `create_date` textual como fonte principal de tempo cambial

Quando o fornecedor disponibilizar timestamp numérico e data textual, a T21 deverá confirmar a equivalência entre ambos.

O adapter futuro deverá privilegiar uma representação temporal inequívoca e convertível para o modelo de tempo do projeto.

A data textual poderá ser mantida apenas como evidência diagnóstica da prova.

**Alternativa considerada:** persistir diretamente a string apresentada pelo fornecedor.

**Decisão:** o projeto precisa calcular idade do câmbio e opera com regra explícita de fuso horário.

### 15. Preço e timestamp devem representar o mesmo snapshot lógico

Em cada provedor será verificado que:

- o preço selecionado pertence à mesma resposta/snapshot do timestamp escolhido;
- horário de requisição não é confundido com horário de mercado;
- resposta antiga ainda pode ser identificada pela idade do timestamp.

Essas conclusões orientarão o mapeamento da T22 e posteriormente a regra de stale/cache.

### 16. Valores externos serão preservados na precisão original durante a prova

A T21 deverá registrar os valores como recebidos pelo provedor para permitir comparação e evidência.

Ela poderá demonstrar que são parseáveis como decimal, mas não deverá alterar o contrato observado para simular o arredondamento definitivo.

A T22 será responsável pela normalização e pelo uso de tipos internos adequados; as regras financeiras posteriores aplicarão `HALF_UP` conforme as specs.

**Alternativa considerada:** arredondar valores já nas fixtures da T21.

**Decisão:** fixtures de contrato devem refletir o fornecedor, não o modelo interno futuro.

### 17. Respostas incompletas serão produzidas deterministicamente

Não será necessário aguardar um fornecedor real apresentar uma resposta defeituosa.

A partir de uma resposta válida confirmada, serão criadas fixtures variantes removendo campos obrigatórios relevantes, permitindo demonstrar como a T22 deverá reconhecer contratos incompletos.

Pelo menos os campos relativos a:

- identificação;
- moeda;
- preço;
- horário;

deverão ser exercitados quando aplicáveis.

### 18. Rate limit e indisponibilidade serão simulados além da observação documental

A T21 não deverá consumir deliberadamente toda a quota de uma API para provocar bloqueio real.

O comportamento documentado de limite deverá ser registrado e a condição deverá ser representada de forma controlada em testes/provas locais.

O mesmo vale para:

- `429`;
- `5xx`;
- timeout;
- resposta não parseável.

**Alternativa considerada:** provocar rate limit real até o fornecedor bloquear a chave.

**Decisão:** isso desperdiça quota, pode gerar bloqueio temporário e não melhora a qualidade da prova.

### 19. Fixtures terão somente o conteúdo necessário para reproduzir o contrato

As respostas salvas não deverão ser dumps extensos de APIs.

Cada fixture deverá conter apenas dados suficientes para representar:

- sucesso;
- mercado rejeitado quando aplicável;
- resposta incompleta;
- erro estruturado quando necessário.

Credenciais, headers de autenticação, identificadores de conta ou informações irrelevantes não serão armazenados.

### 20. Documentação final da T21 terá uma matriz por fornecedor

Ao final deverá existir documentação contendo, para cada fonte:

| Aspecto | Evidência |
|---|---|
| Finalidade no projeto | ativo BR, ativo US ou USD/BRL |
| Endpoint validado | recurso mínimo confirmado |
| Autenticação | exigência observada |
| Campos utilizados | origem → significado interno futuro |
| Timestamp | campo e semântica |
| Mercado | regra para BR/US |
| Limites | plano/limite observado |
| Consumo projetado | ciclo do projeto |
| Falhas | inexistente, incompleto, limite, indisponibilidade |
| Fixture | caso reproduzível offline |
| Data da validação | quando a prova foi executada |
| Conclusão | aprovado ou bloqueado para T22 |

A documentação deverá diferenciar fatos confirmados de recomendações de implementação.

### 21. A T21 termina com decisão explícita de prontidão para a T22

Cada fornecedor receberá uma conclusão:

- `APROVADO`: contrato mínimo e consumo compatíveis com o projeto;
- `APROVADO COM LIMITAÇÃO`: utilizável, mas com restrição documentada que a T22 deve respeitar;
- `BLOQUEADO`: não foi possível comprovar requisito necessário.

A T22 não deverá depender de campo ou comportamento marcado como não comprovado.

Caso a Twelve Data fique bloqueada, a T21 será considerada tecnicamente concluída somente se o motivo e a consequência estiverem documentados; a escolha de substituir fornecedor ou alterar requisito exige decisão posterior explícita.

## Risks / Trade-offs

- **[Documentação do provedor divergir do comportamento real]** → Confirmar os campos críticos com chamada real controlada e registrar data da prova.
- **[Plano gratuito mudar depois da T21]** → Registrar plano e limites observados com data e manter limites como configuração/risco da integração, não como invariantes permanentes.
- **[Brapi não comportar o ciclo de cinco minutos para muitas posições no plano utilizado]** → Calcular consumo para diferentes quantidades de símbolos e documentar a capacidade antes da T22/scheduler.
- **[Twelve Data possuir cobertura parcial ou restrição de plano]** → Validar amostra representativa, descoberta e endpoints de cotação e marcar bloqueio quando necessário.
- **[Créditos da Twelve Data serem consumidos durante desenvolvimento]** → Manter smoke tests opt-in e usar fixtures para repetição local.
- **[Ticker em USD ser classificado incorretamente como mercado US]** → Confirmar país/exchange/MIC ou filtro equivalente; nunca usar moeda isoladamente.
- **[Timestamp de requisição ser confundido com timestamp de mercado]** → Documentar explicitamente o campo temporal escolhido de cada fornecedor.
- **[AwesomeAPI possuir `bid` e `ask` e diferentes partes do sistema escolherem campos diferentes]** → Registrar uma única decisão cambial antes da T22.
- **[Chave de API ser exposta em commit ou fixture]** → Ler somente de configuração externa e revisar fixtures/documentação antes do commit.
- **[Teste normal falhar porque serviço externo está fora do ar]** → Manter provas online desabilitadas por padrão.
- **[T21 evoluir acidentalmente para T22]** → Não criar portas, adapters, persistence ou contratos de produção.
- **[Fixtures envelhecerem]** → Utilizá-las como evidência de formato, não como prova permanente de disponibilidade atual; smoke test pode ser reexecutado quando necessário.

## Migration Plan

1. Criar estrutura de prova técnica separada da implementação de produção.
2. Definir mecanismo explícito para habilitar smoke tests externos.
3. Configurar nomes das credenciais externas sem registrar seus valores.
4. Validar documentação e contrato atual da Brapi.
5. Executar prova com ativo brasileiro conhecido.
6. Registrar campos, timestamp, erros, autenticação e limites da Brapi.
7. Calcular o consumo do ciclo brasileiro de cinco minutos.
8. Criar fixtures mínimas da Brapi.
9. Validar documentação e contrato atual da Twelve Data.
10. Executar prova com ativos norte-americanos representativos.
11. Confirmar a regra de identificação do mercado US.
12. Exercitar um mercado não aceito.
13. Registrar campos, timestamp, cobertura, créditos e limites da Twelve Data.
14. Calcular o consumo do ciclo diário norte-americano.
15. Criar fixtures mínimas da Twelve Data.
16. Validar `USD-BRL` na AwesomeAPI.
17. Confirmar campos de preço e timestamp e registrar a escolha cambial que será usada pela T22.
18. Documentar cache/autenticação e limites relevantes da AwesomeAPI.
19. Criar fixtures mínimas da AwesomeAPI.
20. Criar respostas incompletas e falhas simuladas para os três formatos.
21. Consolidar a matriz comparativa e classificar cada fornecedor como aprovado, aprovado com limitação ou bloqueado.
22. Confirmar que a suíte normal continua executando sem internet e sem credenciais.
23. Validar o change OpenSpec.

Nenhuma migration de banco, endpoint funcional ou alteração de frontend é necessária.

O rollback consiste em remover somente protótipos/testes exploratórios, fixtures e documentação da prova. Como a T21 não altera comportamento de produção nem persistência, não há migração de dados a desfazer.