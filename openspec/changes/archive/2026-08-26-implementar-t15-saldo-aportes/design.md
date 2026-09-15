## Context

A T14 já implementou a infraestrutura compartilhada para histórico e patrimônio. `FinancialHistoryService` registra movimentação e ponto patrimonial dentro de uma transação chamadora e utiliza o estado financeiro resultante da operação para calcular o patrimônio. `Movement` também já possui criação específica para movimentações do tipo aporte.

O cadastro de conta já utiliza essa infraestrutura para registrar o saldo inicial, portanto a T15 não deve criar um segundo mecanismo de histórico, patrimônio ou cálculo.

A entidade `Account` mantém o saldo único em BRL, mas atualmente não possui uma operação de domínio para realizar aportes. Também ainda não existem endpoints específicos de carteira para consultar saldo ou registrar aporte.

A arquitetura do projeto prevê controllers pequenos, regras financeiras na camada de aplicação/domínio e identificação da conta pela sessão autenticada.

See `proposal.md` - Why e a delta spec `saldo-aportes` deste change.

## Goals / Non-Goals

**Goals:**

- Expor consulta do saldo atual da conta autenticada.
- Implementar aporte mínimo de R$ 10,00 sobre o saldo único da conta.
- Centralizar a alteração de saldo em uma operação explícita do domínio.
- Tratar valores monetários com `BigDecimal`, duas casas decimais e `HALF_UP`.
- Atualizar saldo, registrar `DEPOSIT` e criar ponto patrimonial em uma única transação.
- Reutilizar `FinancialHistoryService` e `Movement.deposit(...)` implementados na T14.
- Garantir que somente a conta associada à sessão possa ser consultada ou alterada.
- Manter aportes classificados como entrada de capital e não como resultado de investimento.
- Permitir aportes em contas sem corretoras ou posições.

**Non-Goals:**

- Implementar a interface React de saldo e aporte; isso pertence à T16.
- Implementar compra, venda ou transferência.
- Criar consulta completa de histórico.
- Implementar dashboards, lucro, prejuízo ou valorização.
- Alterar o cálculo patrimonial criado na T14.
- Criar novo mecanismo de histórico financeiro.
- Adicionar novas dependências.
- Alterar o schema do banco sem necessidade.

## Decisions

### 1. Criar um caso de uso de carteira dedicado a saldo e aporte

A T15 terá um service de carteira responsável pelos casos de uso de consulta de saldo e aporte.

O controller deverá permanecer limitado a:

- obter a conta da sessão autenticada;
- validar a estrutura da requisição;
- chamar o service;
- devolver a resposta HTTP.

A operação financeira, validação de regras de negócio, atualização do saldo e integração com histórico permanecerão fora do controller.

**Alternativa considerada:** adicionar toda a lógica ao `AccountManagementService`.

**Decisão:** manter saldo e movimentações financeiras em um caso de uso de carteira separado das operações de identidade e credenciais da conta, seguindo a divisão prevista na arquitetura.

### 2. A conta será identificada exclusivamente pela sessão

Os endpoints de saldo e aporte não receberão `accountId` informado pelo cliente.

O controller obterá o identificador através do `AccountPrincipal`, seguindo o padrão já utilizado pelos endpoints privados existentes.

O service carregará a conta correspondente e deverá operar apenas sobre conta ativa.

**Alternativa considerada:** receber o identificador da conta pela URL ou pelo corpo da requisição.

**Decisão:** não aceitar identificador fornecido pelo cliente, reduzindo a superfície para acesso indevido ao saldo de outra conta.

### 3. Criar operação explícita de aporte em `Account`

`Account` receberá uma operação de domínio responsável por aumentar seu saldo.

A operação utilizará `BigDecimal` e manterá o saldo normalizado em duas casas decimais com `RoundingMode.HALF_UP`.

O service não deverá alterar o campo `balance` diretamente nem depender de reflection, setters genéricos ou manipulação pelo repository.

O valor mínimo de R$ 10,00 será validado como regra do caso de uso antes da alteração do saldo.

**Alternativa considerada:** calcular o novo saldo no service e atribuí-lo diretamente à entidade.

**Decisão:** encapsular a alteração na própria conta para preservar suas invariantes e permitir que operações financeiras futuras usem métodos de domínio apropriados.

### 4. Validar o valor antes de iniciar qualquer alteração financeira

A requisição de aporte utilizará `BigDecimal`.

O fluxo deverá rejeitar:

- valor ausente;
- valor menor que R$ 10,00;
- zero;
- valor negativo.

Depois da validação do mínimo, o valor aceito será normalizado para duas casas decimais com `HALF_UP`.

Nenhuma alteração de saldo, histórico ou patrimônio poderá ocorrer antes da validação ser concluída.

**Alternativa considerada:** depender apenas de validações HTTP.

**Decisão:** usar validação de entrada para erros estruturais e manter a regra financeira também protegida no caso de uso/domínio, de forma que ela não dependa exclusivamente do controller.

### 5. A operação de aporte será o limite transacional

O método de aporte do service de carteira será `@Transactional`.

A sequência conceitual será:

1. carregar a conta autenticada;
2. validar o aporte;
3. atualizar o saldo da conta;
4. chamar `FinancialHistoryService`;
5. criar `Movement.deposit(...)` com o valor aportado e o saldo resultante;
6. calcular e persistir o ponto patrimonial;
7. confirmar a transação.

`FinancialHistoryService` continuará utilizando `Propagation.MANDATORY`, portanto não criará uma transação independente.

Se qualquer etapa falhar, a alteração no saldo, a movimentação e o ponto patrimonial deverão ser revertidos juntos.

**Alternativa considerada:** salvar primeiro o saldo e registrar histórico posteriormente em outra transação.

**Decisão:** manter toda a operação atômica para impedir divergência entre saldo, histórico e patrimônio.

### 6. Calcular o ponto patrimonial depois da alteração do saldo

A conta deverá ter seu saldo atualizado antes da chamada ao `FinancialHistoryService`.

Assim, o `PatrimonyCalculator` criado na T14 enxergará o estado resultante da operação e o novo ponto patrimonial refletirá:

- novo saldo;
- posições já existentes, se houver;
- cotações/câmbio aplicáveis;
- patrimônio posterior ao aporte.

Uma conta sem corretoras ou posições terá patrimônio calculado normalmente apenas a partir do saldo.

**Alternativa considerada:** calcular o ponto antes de atualizar a conta.

**Decisão:** o histórico patrimonial representa o estado posterior à movimentação, conforme definido pelas specs do projeto.

### 7. Aporte será registrado exclusivamente como `DEPOSIT`

Todo aporte concluído utilizará `Movement.deposit(...)`.

O registro deverá preservar:

- tipo `DEPOSIT`;
- valor aportado;
- moeda BRL;
- instante da operação;
- saldo restante após o aporte.

Campos relacionados a ticker, mercado, quantidade, corretora e resultado realizado permanecerão não aplicáveis.

Isso diferencia entrada de capital de compra, venda e demais resultados de investimento.

**Alternativa considerada:** tratar aporte apenas como alteração de saldo sem histórico específico.

**Decisão:** usar `DEPOSIT`, pois a classificação é necessária para auditoria e para impedir que cálculos de rendimento confundam capital aportado com retorno.

### 8. Não criar cálculo de lucro ou valorização antecipadamente

A T15 deve garantir semanticamente que um aporte não represente rendimento através da classificação `DEPOSIT` e da ausência de resultado realizado nessa movimentação.

Como os dashboards e indicadores completos ainda não pertencem a esta tarefa, não será criado um novo mecanismo de lucro/prejuízo apenas para satisfazer a T15.

Quando esses cálculos forem implementados pelas tarefas correspondentes, deverão distinguir entradas de capital das movimentações que efetivamente produzem resultado.

Os testes da T15 deverão garantir que:

- o aporte seja registrado como `DEPOSIT`;
- não exista resultado realizado associado ao aporte;
- saldo e patrimônio aumentem pelo aporte;
- nenhuma operação de investimento seja criada como consequência.

**Alternativa considerada:** implementar antecipadamente os indicadores de rendimento.

**Decisão:** preservar a informação necessária para o cálculo correto sem antecipar funcionalidades de dashboards fora do escopo.

### 9. Criar endpoints específicos de carteira

Será criado um controller de carteira sob uma rota própria da API, mantendo conta e autenticação separadas das operações financeiras.

A API deverá oferecer:

- uma operação `GET` para consultar o saldo atual;
- uma operação `POST` para realizar aporte.

A resposta deverá fornecer o saldo em formato numérico consistente, permitindo que a T16 consuma a API sem recalcular valores no frontend.

A operação de aporte deverá estar sujeita às mesmas proteções de sessão e CSRF aplicadas às demais operações autenticadas que alteram estado.

**Alternativa considerada:** adicionar endpoints de saldo ao `AccountController`.

**Decisão:** usar controller de carteira porque a arquitetura do projeto separa identidade/conta das funcionalidades financeiras e as próximas operações também pertencerão a esse domínio.

### 10. Não introduzir migration para a T15 sem necessidade comprovada

O schema atual já possui `account.balance`, `movement` e `patrimonial_point`, e a T14 já realizou a evolução necessária do ponto patrimonial.

Portanto, a implementação da T15 deverá reutilizar o schema existente.

Uma nova migration só deverá ser criada caso a implementação revele uma necessidade incompatível com o modelo já aprovado, situação que deverá ser tratada como revisão de design e não como mudança automática de escopo.

## Risks / Trade-offs

- **[Saldo atualizado sem histórico correspondente]** → Manter aporte, `FinancialHistoryService` e ponto patrimonial na mesma transação.
- **[Histórico salvo com saldo anterior ao aporte]** → Alterar o saldo antes de chamar o registro financeiro.
- **[Valor monetário com escala ou arredondamento inconsistente]** → Normalizar valores com duas casas e `HALF_UP` na fronteira de domínio apropriada.
- **[Aporte abaixo do mínimo produzir efeitos parciais]** → Validar completamente o valor antes de modificar a conta.
- **[Usuário operar saldo de outra conta]** → Derivar a conta exclusivamente do `AccountPrincipal`; nunca aceitar `accountId` do cliente.
- **[Aporte ser contabilizado futuramente como lucro]** → Persistir explicitamente como `DEPOSIT` e manter resultado realizado não aplicável.
- **[Duplicação da infraestrutura da T14]** → Reutilizar obrigatoriamente `FinancialHistoryService`, `PatrimonyCalculator` e `Movement.deposit(...)`.
- **[Conta sem corretora impedir aporte]** → O fluxo não dependerá de `AccountBroker`; patrimônio sem posições será baseado no saldo.
- **[Implementação antecipar dashboards e métricas]** → Limitar a T15 à classificação correta da entrada de capital e deixar apresentação/agregações para as tarefas próprias.

## Migration Plan

1. Adicionar à entidade `Account` uma operação explícita para creditar aporte mantendo precisão monetária.
2. Criar os DTOs mínimos para consulta de saldo e entrada de aporte.
3. Criar o service de carteira com consulta do saldo e aporte transacional.
4. Integrar o aporte ao `FinancialHistoryService` através de `Movement.deposit(...)`.
5. Criar o controller autenticado de carteira.
6. Adicionar testes de saldo inicial, aporte mínimo, aporte válido, autorização e atomicidade.
7. Adicionar teste garantindo que aporte seja registrado como `DEPOSIT` sem resultado de investimento.
8. Executar a suíte existente para garantir ausência de regressões na T14 e nos fluxos de conta.

Nenhuma migration de banco é prevista. O rollback da T15 consiste em reverter as alterações de código e endpoints, preservando o schema e a infraestrutura da T14.