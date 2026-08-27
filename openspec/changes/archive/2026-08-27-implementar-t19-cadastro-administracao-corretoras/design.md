## Context

A T18 disponibilizou portas internas independentes para consulta cadastral por CNPJ, endereço por CEP e registro regulatório da CVM. A T19 deve compor essas integrações em casos de uso de corretoras sem permitir que detalhes das fontes externas cheguem aos controllers ou à persistência.

O modelo atual já possui:

- `Broker`, identificado unicamente por CNPJ;
- `AccountBroker`, relacionando uma conta a uma corretora e contendo estado ativo/inativo;
- `Position`, vinculada a uma associação de corretora;
- repositories para corretora, associação e posição;
- autenticação por conta/sessão já utilizada pelos demais casos de uso privados.

`Broker` já possui os campos institucionais necessários para razão social, nome fantasia, situação cadastral, categoria CVM e endereço.

`AccountBroker` já possui estado, data de associação e data de remoção, porém o esquema atual ainda não garante unicidade de `(account_id, broker_id)`. Para preservar o histórico por meio de reativação da mesma associação, a T19 deverá reforçar essa invariável também no banco.

As chamadas externas podem possuir latência e falhar independentemente do PostgreSQL. Elas não deverão permanecer dentro de transações longas.

See `proposal.md` - Why e a delta spec `corretoras` deste change.

## Goals / Non-Goals

**Goals:**

- Criar um caso de uso de pesquisa de corretora exclusivamente por CNPJ.
- Consolidar os resultados dos três ports da T18 em uma projeção interna única.
- Diferenciar claramente instituição inválida de dependência externa indisponível.
- Permitir associação somente após nova validação autoritativa do CNPJ.
- Manter um único registro institucional `Broker` por CNPJ.
- Manter uma única associação histórica entre conta e corretora.
- Reativar associação inativa em vez de criar nova linha.
- Impedir duplicidade inclusive em cenários concorrentes.
- Listar somente corretoras ativas pertencentes à conta autenticada.
- Remover logicamente uma associação somente quando não houver posição aberta.
- Atualizar dados institucionais sem apagar valores válidos anteriores por respostas incompletas.
- Manter acesso externo fora das transações de persistência.
- Expor erros funcionais adequados para a futura interface T20.

**Non-Goals:**

- Criar a interface React de corretoras.
- Permitir pesquisa por nome, razão social ou identificador interno.
- Persistir dados recebidos do frontend como fonte de verdade.
- Permitir ao frontend escolher categoria CVM ou situação cadastral.
- Implementar compra, venda ou transferência.
- Criar histórico financeiro por associação ou remoção de corretora.
- Alterar saldo, patrimônio ou movimentações.
- Criar cache adicional para as fontes externas.
- Alterar a implementação interna dos adapters da T18.
- Permitir remoção física de `Broker` ou `AccountBroker`.

## Decisions

### 1. Criar um serviço de aplicação que orquestre os três ports

A lógica de corretoras será coordenada por um service próprio.

O fluxo de consolidação será:

1. normalizar e validar estruturalmente o CNPJ;
2. consultar `CompanyRegistryPort`;
3. verificar se o cadastro empresarial está ativo;
4. obter o CEP retornado pelo cadastro;
5. consultar `PostalAddressPort`;
6. consultar `RegulatoryRegistryPort` pelo mesmo CNPJ;
7. verificar se o registro regulatório satisfaz a categoria `CTVM`;
8. produzir uma projeção interna consolidada.

O controller não deverá chamar os três ports diretamente.

**Alternativa considerada:** colocar a composição das fontes no controller.

**Decisão:** a composição representa regra do caso de uso e precisa ser reutilizada pela pesquisa e pela confirmação de associação.

### 2. Pesquisa e associação serão operações distintas

A pesquisa deverá retornar uma prévia consolidada sem depender de dados enviados posteriormente pelo cliente.

A associação será uma operação separada e receberá essencialmente o CNPJ a ser associado.

No momento da associação, o backend deverá executar novamente a validação necessária nas fontes externas em vez de confiar nos dados exibidos anteriormente ou reenviados pelo frontend.

Isso garante que situação cadastral e autorização regulatória utilizadas para persistir a associação sejam obtidas de fontes autoritativas.

**Alternativa considerada:** pesquisar uma vez e receber do frontend todos os dados consolidados no POST de associação.

**Decisão:** dados externos exibidos no navegador não são uma fonte confiável para persistência.

### 3. Não manter transação de banco aberta durante as consultas externas

A composição BrasilAPI → ViaCEP → CVM ocorrerá fora de transação de persistência.

Somente depois que os dados estiverem integralmente validados será iniciado o trecho transacional que:

- cria ou atualiza `Broker`;
- cria ou reativa `AccountBroker`.

Nenhuma alteração deverá ser persistida se a validação externa obrigatória falhar.

**Alternativa considerada:** marcar todo o fluxo de associação como uma única transação envolvendo as três chamadas externas.

**Decisão:** não manter conexão/transação de banco presa à latência de terceiros.

### 4. CNPJ será a identidade institucional imutável da corretora

`Broker` continuará sendo compartilhado entre contas e identificado unicamente pelo CNPJ normalizado.

Ao associar uma corretora:

- se não existir `Broker` para o CNPJ, será criado;
- se já existir, seus dados institucionais poderão ser atualizados;
- uma nova linha de `Broker` nunca será criada apenas porque outra conta está associando a mesma instituição.

O UUID interno não será utilizado como entrada pública para pesquisa.

**Alternativa considerada:** criar um `Broker` separado por conta.

**Decisão:** a instituição é global; o vínculo com cada investidor pertence a `AccountBroker`.

### 5. Atualização de Broker utilizará merge campo a campo

As entidades deverão possuir operações de domínio explícitas para criação e atualização, evitando setters públicos indiscriminados.

Quando dados válidos mais recentes forem recebidos:

- valores presentes e válidos poderão substituir os existentes;
- valores ausentes ou inválidos não substituirão valores válidos já armazenados;
- o CNPJ nunca será alterado;
- `updatedAt` será atualizado somente quando o estado institucional for efetivamente persistido.

Campos obrigatórios continuam sendo exigidos para criação de um novo `Broker`.

Para um `Broker` já existente, uma resposta parcial poderá preservar campos válidos anteriores apenas quando ainda houver informação suficiente para concluir as validações obrigatórias de situação cadastral e CVM.

**Alternativa considerada:** substituir todos os campos do registro com cada resposta externa.

**Decisão:** evita apagar informação válida por degradação temporária de uma fonte.

### 6. Uma conta terá uma única associação histórica por corretora

A relação lógica será:

`Account + Broker -> exatamente um AccountBroker`

Estados possíveis:

- `ACTIVE`;
- `INACTIVE`.

Na primeira associação será criada uma linha.

Na remoção, a mesma linha será marcada como inativa e receberá `removedAt`.

No recadastro, a mesma linha será reativada:

- `status = ACTIVE`;
- `removedAt = null`;
- o histórico da associação continuará representado pelo mesmo identificador.

Não será criada uma segunda linha no recadastro.

**Alternativa considerada:** criar nova linha de associação a cada recadastro.

**Decisão:** a spec exige preservação histórica sem segunda associação ativa e o modelo atual já possui estado próprio para remoção lógica.

### 7. Reforçar a unicidade de associação no banco

Será adicionada migration Flyway criando constraint única sobre:

`(account_id, broker_id)`

A checagem pelo repository continuará sendo usada para fornecer erro funcional antecipado, mas a constraint será a proteção final contra duas requisições concorrentes.

A aplicação deverá converter uma violação concorrente dessa constraint para o mesmo resultado funcional de associação duplicada.

**Alternativa considerada:** depender apenas de `existsBy...` antes do `save`.

**Decisão:** uma verificação seguida de inserção não impede race condition entre requisições simultâneas.

### 8. Duplicidade ativa e recadastro terão comportamentos diferentes

Dentro da transação de associação:

- associação inexistente → criar ativa;
- associação existente `ACTIVE` → rejeitar duplicidade;
- associação existente `INACTIVE` → reativar.

A consulta da associação deverá considerar conta e corretora conjuntamente, não apenas o estado ativo.

O repository deverá ser estendido para localizar a associação histórica por `accountId + brokerId`.

**Alternativa considerada:** consultar somente associações ativas e criar uma nova quando nenhuma for encontrada.

**Decisão:** isso perderia a referência à associação removida e quebraria o recadastro histórico.

### 9. Remoção será feita pelo identificador da associação, sempre filtrado pela conta

A operação de remoção não deverá executar um `findById` sem validar propriedade posteriormente como caminho principal.

O repository deverá permitir localizar a associação pelo identificador combinado com a conta autenticada.

Se a associação não pertencer à conta:

- nenhum dado da outra conta será retornado;
- nenhuma alteração será feita;
- a resposta deverá seguir a estratégia do projeto para recurso privado inexistente/não acessível.

**Alternativa considerada:** buscar qualquer associação pelo UUID e depois comparar `accountId`.

**Decisão:** filtrar por propriedade na própria consulta reduz risco de exposição acidental.

### 10. Somente associações ativas serão listadas para uso

A listagem de corretoras da conta utilizará:

`accountId + status ACTIVE`

Corretoras removidas não aparecerão nas opções operacionais.

Elas permanecerão no banco para:

- preservação histórica;
- posições antigas com quantidade zero;
- futuro recadastro.

A T19 não criará endpoint público para listar associações inativas.

**Alternativa considerada:** retornar ativas e inativas com um campo de estado.

**Decisão:** a funcionalidade prevista para operações utiliza somente corretoras ativas.

### 11. Remoção será bloqueada por posição aberta na associação específica

Antes de inativar uma associação, o service deverá verificar posições daquela:

- conta;
- associação de corretora.

Qualquer posição com `quantity > 0` impedirá a remoção.

O repository de posições deverá receber uma consulta específica que responda eficientemente à pergunta:

`existe posição positiva para esta conta e esta associação?`

A remoção e a checagem final de posições deverão ocorrer dentro do trecho transacional necessário para evitar alteração parcial.

**Alternativa considerada:** carregar todas as posições da conta e filtrar em memória.

**Decisão:** a regra é específica por corretora e pode ser respondida diretamente pelo banco.

### 12. Posição com quantidade zero não bloqueia remoção

Registros de posição podem permanecer com quantidade zero para preservar estrutura ou histórico operacional.

Somente `quantity > 0` representa posição aberta para essa regra.

Não haverá exclusão de posições durante a remoção da corretora.

**Alternativa considerada:** impedir remoção se existir qualquer linha de `Position`.

**Decisão:** existência física de uma posição zerada não representa exposição financeira aberta.

### 13. Remoção não fará nova consulta às fontes externas

A validade cadastral/CVM é necessária para pesquisar e associar uma corretora, mas não para removê-la.

A remoção depende apenas de:

- conta autenticada;
- associação ativa pertencente a ela;
- ausência de posições abertas.

Assim, indisponibilidade da BrasilAPI, ViaCEP ou CVM não impedirá o investidor de remover uma associação elegível.

**Alternativa considerada:** revalidar a corretora antes da remoção.

**Decisão:** adicionaria dependência externa sem relação com a regra de remoção.

### 14. Recadastro exige nova validação externa completa

Uma associação inativa não será reativada apenas por já ter sido válida anteriormente.

O recadastro deverá passar novamente pelas validações atuais de:

- CNPJ ativo;
- endereço necessário;
- registro CVM;
- categoria `CTVM`.

Após a validação:

- dados institucionais válidos poderão ser atualizados;
- a associação histórica será reativada.

**Alternativa considerada:** reativar diretamente a associação antiga.

**Decisão:** situação empresarial ou autorização regulatória podem ter mudado desde a remoção.

### 15. Falhas externas terão resultado distinto de reprovação funcional

O service deverá preservar a classificação produzida pelos adapters da T18.

Exemplos:

- CNPJ inexistente → instituição não encontrada;
- CNPJ existente porém inativo → instituição não elegível;
- registro CVM consultado com sucesso mas não CTVM → instituição não autorizada;
- timeout/`429`/`5xx` → dependência externa indisponível;
- resposta obrigatória incompleta → não foi possível concluir a validação.

Uma falha técnica nunca será convertida para “não é CTVM”.

Nenhum `Broker` ou `AccountBroker` novo será persistido nesses casos.

### 16. Endpoints serão autenticados e orientados aos casos de uso

A API privada deverá fornecer operações equivalentes a:

- pesquisar corretora por CNPJ;
- associar/confirmar corretora;
- listar corretoras ativas da conta;
- remover uma associação.

Os contratos HTTP deverão usar apenas a conta identificada pela sessão.

Nenhum endpoint aceitará `accountId` fornecido pelo frontend para definir propriedade.

A confirmação da associação não receberá razão social, endereço, situação ou categoria CVM como dados autoritativos; o CNPJ será utilizado para refazer a validação no backend.

**Alternativa considerada:** permitir que o frontend envie `accountId` ou o objeto consolidado completo.

**Decisão:** propriedade vem da sessão e dados institucionais vêm das fontes confiáveis.

### 17. Pesquisa não cria associação implicitamente

Consultar um CNPJ não deverá associar automaticamente a corretora à conta.

A pesquisa retornará dados suficientes para a T20 apresentar a confirmação.

A persistência de uma nova associação ocorrerá somente após a ação explícita de confirmação.

Para um `Broker` institucional já existente, eventuais atualizações cadastrais decorrentes da pesquisa poderão ser persistidas em transação curta somente depois de a consulta externa ter sido validada, sem alterar qualquer `AccountBroker`.

**Alternativa considerada:** pesquisar e associar numa única chamada.

**Decisão:** a spec exige apresentação dos dados consolidados antes da confirmação.

### 18. Não confiar somente em checagens de aplicação para concorrência

As invariantes relevantes deverão existir em múltiplas camadas quando necessário:

- CNPJ único em `broker` já permanece protegido pelo banco;
- conta + corretora única será protegida por nova constraint;
- services fazem checagem funcional para mensagens adequadas;
- violações de constraint concorrentes serão traduzidas para erro funcional.

Isso também cobre duas associações simultâneas do mesmo CNPJ na mesma conta.

### 19. O fluxo não registra movimentação ou ponto patrimonial

Pesquisar, associar, remover ou recadastrar corretora não altera:

- saldo;
- quantidade de posições;
- custo;
- patrimônio financeiro.

Portanto, essas operações não deverão chamar `FinancialHistoryService` nem criar `Movement` ou `PatrimonialPoint`.

A existência de posições será apenas consultada para bloquear remoção.

### 20. Testes externos continuarão simulados na T19

Os services serão testados substituindo os três ports por doubles/mocks controlados.

Os testes da T19 não deverão chamar BrasilAPI, ViaCEP ou CVM reais.

Testes de repository e integração HTTP deverão utilizar a infraestrutura de banco/teste existente.

Devem ser cobertos pelo menos:

- CTVM válida;
- CNPJ inativo;
- instituição não CTVM;
- dependência externa indisponível;
- associação inicial;
- duplicidade;
- listagem somente da própria conta;
- remoção sem posição;
- remoção bloqueada por posição positiva;
- posição zerada permitindo remoção;
- recadastro;
- preservação da mesma associação no recadastro;
- atualização cadastral válida;
- resposta parcial preservando valores antigos;
- tentativa de acessar associação de outra conta;
- concorrência/constraint de duplicidade quando aplicável.

## Risks / Trade-offs

- **[Chamadas externas tornarem a associação lenta]** → Executar as consultas fora da transação de banco e iniciar persistência somente após validação completa.
- **[Frontend adulterar dados exibidos antes da confirmação]** → Confirmar por CNPJ e consultar novamente as fontes no backend.
- **[Duas requisições criarem associação duplicada]** → Constraint `UNIQUE (account_id, broker_id)` mais tradução da violação concorrente.
- **[Recadastro criar nova associação e perder continuidade histórica]** → Localizar também associações inativas e reativar a mesma entidade.
- **[Resposta externa parcial apagar cadastro válido]** → Aplicar merge campo a campo e nunca substituir valor válido por ausência.
- **[Falha da CVM ser confundida com instituição não autorizada]** → Preservar categorias distintas de falha técnica e reprovação regulatória.
- **[Remoção de corretora com posição aberta]** → Consultar existência de `quantity > 0` antes da inativação.
- **[Consulta de associação por UUID expor outra conta]** → Todas as operações privadas combinam identificador com a conta da sessão.
- **[Transação longa esperar serviços externos]** → Separar fase externa da fase transacional.
- **[Broker compartilhado entre contas receber atualização concorrente]** → CNPJ permanece único e atualizações são idempotentes/mergeadas por dados validados.
- **[Dados mudarem entre pesquisa e confirmação]** → Revalidar as fontes no momento da associação.
- **[T19 avançar para a interface T20]** → Limitar o change à API/backend e seus contratos.

## Migration Plan

1. Adicionar migration Flyway garantindo unicidade de `(account_id, broker_id)`.
2. Evoluir `Broker` com operações controladas de criação e atualização parcial segura.
3. Evoluir `AccountBroker` com criação, inativação e reativação explícitas.
4. Estender repositories para localizar associação histórica por conta/corretora e associação por conta/id.
5. Adicionar consulta eficiente de existência de posição positiva por conta e associação.
6. Criar projeções internas/API necessárias para pesquisa e listagem.
7. Implementar a composição dos três ports para pesquisa e validação de corretora.
8. Implementar associação transacional após validação externa completa.
9. Implementar listagem de associações ativas por conta.
10. Implementar remoção lógica protegida por posição aberta.
11. Implementar recadastro reutilizando a associação histórica.
12. Implementar endpoints autenticados e mapeamento de erros.
13. Cobrir os seis critérios da spec e os cenários adicionais da delta spec.
14. Executar testes focados, testes de persistência e a suíte completa.

A migration adiciona somente uma restrição de integridade e não remove dados.

Antes de aplicar a constraint em ambientes que já possuam dados, deverá ser verificado que não existem pares duplicados de `account_id + broker_id`. Na base atual do projeto, qualquer duplicidade encontrada deverá ser tratada explicitamente antes da criação da constraint, sem apagar histórico automaticamente.

O rollback de código poderá remover os novos casos de uso e endpoints. A constraint de unicidade não deverá ser removida em rollback normal porque representa uma invariável do domínio; se uma reversão estrutural for realmente necessária, deverá ocorrer por migration posterior, nunca alterando uma migration já aplicada.