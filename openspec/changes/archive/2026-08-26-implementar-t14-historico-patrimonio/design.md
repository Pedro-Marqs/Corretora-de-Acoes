## Context

A estrutura de persistência necessária para histórico e patrimônio já existe parcialmente no projeto. O domínio possui `Movement`, `MovementType` com os cinco tipos previstos, `PatrimonialPoint`, posições, cotações e câmbio, além dos respectivos repositories.

O cadastro de conta já demonstra o comportamento inicial esperado ao persistir conta, movimentação de saldo inicial e ponto patrimonial dentro de uma única transação. Entretanto, essa lógica ainda está acoplada ao serviço de cadastro e não existe um mecanismo interno compartilhado que possa ser reutilizado por aporte, compra, venda e transferência.

A T14 deve criar essa infraestrutura antes das operações financeiras seguintes, conforme `docs/05-tarefas.md`. Os requisitos permanecem definidos em `historico-registro-patrimonial`.

Existe também uma lacuna entre o modelo atual e a main spec OpenSpec: `PatrimonialPoint` armazena atualmente apenas o patrimônio total em BRL, enquanto a spec exige que o ponto preserve os valores utilizados no cálculo, incluindo saldo, posições e câmbio quando aplicável. O modelo deverá ser completado para preservar informação suficiente para reconstruir e auditar o cálculo sem depender de cotações futuras.

See `proposal.md` - Why.

## Goals / Non-Goals

**Goals:**

* Criar um serviço interno único para registrar movimentação e ponto patrimonial.
* Garantir que o registro participe obrigatoriamente da transação da operação financeira chamadora.
* Representar e validar corretamente os cinco tipos de movimentação já definidos no domínio.
* Aceitar somente os campos aplicáveis ao tipo registrado e rejeitar movimentações incompletas ou inconsistentes.
* Calcular o patrimônio utilizando o estado financeiro resultante da movimentação.
* Persistir os dados relevantes utilizados no cálculo patrimonial, além do valor patrimonial final.
* Reutilizar a infraestrutura no registro do saldo inicial já existente.
* Manter histórico e pontos patrimoniais imutáveis do ponto de vista das operações da aplicação.
* Garantir que alterações isoladas de cotação e câmbio não produzam registros históricos.
* Permitir que T15 e tarefas posteriores reutilizem a infraestrutura sem duplicar regras.

**Non-Goals:**

* Criar endpoints de consulta do histórico, filtros ou paginação; esses comportamentos poderão ser expostos nas tarefas específicas posteriores.
* Implementar aporte, compra, venda ou transferência completos.
* Implementar interface frontend.
* Alterar as regras financeiras específicas dessas operações.
* Criar atualização automática de cotações ou câmbio.
* Criar endpoint para alterar ou remover movimentações ou pontos patrimoniais.
* Adicionar novas dependências externas.

## Decisions

### 1. Reutilizar e completar os modelos existentes

`Movement`, `MovementType` e `PatrimonialPoint` continuarão sendo os modelos centrais da infraestrutura.

Não será criada uma segunda representação de histórico paralela às entidades atuais. Em vez disso, `Movement` será completada com formas controladas de criação para os tipos previstos e `PatrimonialPoint` será evoluído quando necessário para armazenar os dados utilizados no cálculo.

As entidades continuarão sem setters públicos de alteração, reduzindo a possibilidade de modificação acidental depois da criação.

**Alternativa considerada:** criar novas entidades específicas para cada tipo de movimentação.

**Decisão:** manter uma entidade `Movement` discriminada por `MovementType`, porque o schema e os documentos do projeto já foram estruturados dessa forma e os campos opcionais variam conforme o tipo.

### 2. Validar campos aplicáveis no momento da criação da movimentação

A criação de uma `Movement` deverá passar por uma API de domínio ou mecanismo interno que conheça o tipo de movimentação e valide os campos obrigatórios e não aplicáveis.

Os cinco tipos continuam sendo:

* saldo inicial;
* aporte;
* compra;
* venda;
* transferência.

Campos relacionados a ativo, mercado, cotação, quantidade e corretoras serão exigidos somente nos tipos em que forem aplicáveis, conforme os requisitos e specs existentes.

Uma movimentação inconsistente deverá ser rejeitada antes da persistência.

**Alternativa considerada:** permitir a criação genérica da entidade e depender apenas de campos `NULL` e constraints do banco.

**Decisão:** validar também na camada de domínio/serviço para detectar inconsistências antes de chegar à persistência e produzir erros previsíveis.

### 3. Criar um serviço interno de registro que obrigatoriamente participe da transação chamadora

Será criado um serviço interno responsável por:

1. receber os dados da movimentação já concluída;
2. validar e criar o registro histórico;
3. persistir a movimentação;
4. calcular o patrimônio resultante;
5. criar e persistir o ponto patrimonial correspondente.

Esse serviço não deverá iniciar uma operação financeira independente. Sua execução deve ocorrer dentro da transação aberta pelo caso de uso chamador.

A implementação deverá utilizar a infraestrutura transacional do Spring de forma que uma chamada sem transação ativa seja rejeitada, ou oferecer garantia equivalente.

Dessa forma, nas tarefas seguintes o fluxo será conceitualmente:

`alteração financeira → registro histórico → ponto patrimonial → commit`

Se qualquer uma dessas etapas falhar:

`rollback de toda a operação`

**Alternativa considerada:** abrir uma nova transação independente para histórico e patrimônio.

**Decisão:** não utilizar transação independente, pois poderia persistir a operação financeira sem o histórico correspondente ou vice-versa, contrariando a atomicidade exigida pela T14.

### 4. Centralizar o cálculo patrimonial

O serviço de registro utilizará um componente interno responsável pelo cálculo patrimonial da conta após a movimentação.

O cálculo deverá considerar:

* saldo atual da conta;
* posições com quantidade positiva;
* cotações utilizadas para valorar as posições;
* conversão USD/BRL quando houver exposição em dólar;
* valor total em BRL.

A obtenção das posições deverá reutilizar o repository existente, que já oferece consulta das posições da conta com quantidade positiva.

O cálculo utilizará o estado resultante da movimentação, e não o estado anterior.

Quando um dado indispensável para valorar uma posição estiver ausente, como cotação ou câmbio necessário, a operação deverá falhar em vez de gravar um ponto patrimonial estimado ou inconsistente.

**Alternativa considerada:** usar custo médio das posições como patrimônio.

**Decisão:** o ponto deve refletir a valoração patrimonial usando as informações de mercado disponíveis, enquanto custo médio permanece informação da posição e não substitui sua valoração.

### 5. Persistir os insumos relevantes do ponto patrimonial

`PatrimonialPoint` será completado para preservar informação suficiente sobre o cálculo realizado naquele instante.

O ponto deverá registrar, no mínimo:

* saldo utilizado no cálculo;
* valor das posições utilizado no cálculo, consolidado em BRL;
* câmbio USD/BRL utilizado quando necessário;
* patrimônio total em BRL;
* instante do registro;
* conta e movimentação que originaram o ponto.

O câmbio poderá não ser aplicável quando nenhuma posição exigir conversão de USD para BRL.

A alteração necessária no banco deverá ser feita por migration Flyway incremental, preservando o schema já existente.

**Alternativa considerada:** armazenar somente `patrimonyBrl` e recalcular os componentes posteriormente.

**Decisão:** não recalcular pontos históricos com cotações atuais, pois isso alteraria implicitamente o significado de um registro histórico e reduziria sua auditabilidade.

### 6. Um ponto patrimonial corresponde a uma movimentação concluída

Cada movimentação válida deverá produzir exatamente um ponto patrimonial correspondente.

A associação um-para-um já existente entre `PatrimonialPoint` e `Movement` será preservada.

Os pontos serão criados somente para:

* saldo inicial;
* aporte;
* compra;
* venda;
* transferência.

Atualização de `Quote` ou `ExchangeRate` isoladamente não chamará o serviço de registro.

**Alternativa considerada:** criar pontos sempre que uma cotação mudar para manter um gráfico continuamente atualizado.

**Decisão:** não fazer isso, pois a T14 e a spec determinam que alterações isoladas de cotação não criam pontos patrimoniais.

### 7. Migrar o saldo inicial para a nova infraestrutura

O fluxo de cadastro atualmente cria diretamente `Movement.initialBalance(...)` e `PatrimonialPoint.initial(...)`.

Após a criação da infraestrutura da T14, o cadastro deverá reutilizar o novo mecanismo para registrar o saldo inicial, mantendo a mesma transação que cria a conta.

Isso garante que saldo inicial, aporte, compra, venda e transferência usem uma única política de registro.

**Alternativa considerada:** deixar o cadastro com implementação própria e utilizar o novo serviço somente nas operações futuras.

**Decisão:** centralizar também o saldo inicial para evitar duas implementações da mesma regra e garantir consistência desde o primeiro registro da conta.

### 8. Imutabilidade será garantida pela API da aplicação e pelo modelo

Nenhum controller ou serviço de negócio oferecerá operação para atualizar ou excluir `Movement` ou `PatrimonialPoint`.

As entidades não receberão setters públicos para dados históricos.

Repositories permanecerão componentes internos de persistência; o fluxo funcional da aplicação somente utilizará operações de criação e consulta para histórico.

Não será criado endpoint `PUT`, `PATCH` ou `DELETE` para movimentações ou pontos patrimoniais.

**Alternativa considerada:** implementar edição com versionamento ou eventos compensatórios nesta tarefa.

**Decisão:** não implementar alteração de histórico. Caso uma correção auditável seja necessária no futuro, ela deverá ser representada por um novo evento conforme requisitos futuros, e não pela alteração silenciosa de registros existentes.

### 9. Utilizar o relógio configurado pelo projeto

Instantes de movimentação e ponto patrimonial deverão utilizar o `Clock` configurado pela aplicação, seguindo o padrão já empregado no cadastro.

Movimentação e ponto produzidos pela mesma operação deverão receber um instante coerente, evitando chamadas independentes ao relógio que possam gerar inconsistências artificiais.

Isso também mantém os testes determinísticos.

## Risks / Trade-offs

* **[Falha no ponto patrimonial depois da alteração financeira]** → Garantir que registro e cálculo participem obrigatoriamente da transação chamadora, provocando rollback integral.
* **[Movimentação persistida com combinação inválida de campos]** → Centralizar a criação e validação por tipo antes da persistência e manter constraints do banco como segunda camada de proteção.
* **[Ponto histórico mudar de significado após nova cotação]** → Persistir os valores relevantes utilizados no cálculo no instante da movimentação e nunca recalcular um ponto histórico com dados atuais.
* **[Cotação ou câmbio ausente durante uma operação]** → Falhar a operação de forma explícita, sem criar histórico ou ponto parcial.
* **[Duplicação entre saldo inicial e operações futuras]** → Migrar o cadastro para reutilizar o serviço interno da T14.
* **[Uso acidental de update/delete dos repositories]** → Não expor essas operações por serviços ou controllers e manter entidades históricas sem métodos públicos de mutação.
* **[Migration afetar registros de saldo inicial já existentes]** → Criar migration incremental compatível com registros atuais e preencher valores históricos que possam ser derivados com segurança; campos não aplicáveis devem permanecer explicitamente opcionais quando necessário.
* **[Cálculo patrimonial ganhar responsabilidades demais]** → Separar cálculo patrimonial da orquestração de persistência, mantendo o serviço de registro responsável apenas pelo fluxo transacional.

## Migration Plan

1. Evoluir o modelo de `Movement` para suportar criação validada dos cinco tipos previstos.
2. Evoluir `PatrimonialPoint` para armazenar os insumos relevantes do cálculo patrimonial.
3. Criar migration Flyway incremental para adequar `patrimonial_point` sem alterar migrations já aplicadas.
4. Criar o componente de cálculo patrimonial reutilizando conta, posições, cotações e câmbio existentes.
5. Criar o serviço interno transacional de registro de movimentação e ponto patrimonial.
6. Migrar `AccountRegistrationService` para utilizar a nova infraestrutura no saldo inicial.
7. Criar testes dos cinco tipos, validações, cálculo, atomicidade e ausência de registro em atualização isolada de cotação.
8. Executar a suíte existente para garantir que o cadastro e demais funcionalidades anteriores não sofram regressão.

Como rollback de código, a nova infraestrutura pode ser revertida antes de ser utilizada pelas tarefas financeiras seguintes. Migrations já aplicadas não devem ser alteradas ou removidas; qualquer correção de schema posterior deverá ser realizada por uma nova migration.
