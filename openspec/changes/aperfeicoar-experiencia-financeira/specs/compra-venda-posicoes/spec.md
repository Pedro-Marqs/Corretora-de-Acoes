## MODIFIED Requirements

### Requirement: Executar operacao
O sistema SHALL validar saldo, quantidade inteira positiva, ativo ativo, corretora ativa pertencente à conta autenticada e cotação utilizável antes de registrar uma compra ou venda. O formulário SHALL iniciar o preço unitário com a cotação aplicável e permitir sua edição; na confirmação, o backend SHALL validar e usar o preço unitário enviado conforme as regras financeiras, sem aceitar valores ausentes, não positivos ou incompatíveis com a moeda do ativo.

#### Scenario: Venda válida com preço padrão
- **WHEN** o investidor enviar venda com quantidade disponível sem alterar o preço preenchido
- **THEN** o sistema SHALL registrar a operação usando a cotação aplicável exibida como padrão

#### Scenario: Venda valida
- **WHEN** o investidor autenticado enviar uma venda com quantidade disponível, ativo operacional, corretora própria ativa e cotação utilizável
- **THEN** o sistema SHALL registrar a movimentação, creditar o saldo e reduzir a posição pela regra financeira vigente, usando o preço confirmado

#### Scenario: Venda internacional válida
- **WHEN** o investidor vender ativo norte-americano com preço confirmado em USD e USD/BRL utilizável
- **THEN** o sistema SHALL calcular o crédito em reais multiplicando o valor em USD pela cotação USD/BRL e arredondando para duas casas com `HALF_UP`

#### Scenario: Compra válida com preço editado
- **WHEN** o investidor enviar compra com saldo suficiente e preço unitário positivo editado
- **THEN** o sistema SHALL validar o preço e registrar débito, posição e histórico usando o preço confirmado

#### Scenario: Compra valida
- **WHEN** o investidor autenticado enviar uma compra com saldo suficiente, quantidade válida, ativo operacional, corretora própria ativa e preço confirmado utilizável
- **THEN** o sistema SHALL registrar a movimentação, debitar o saldo e criar ou atualizar a posição pela regra financeira vigente

#### Scenario: Compra internacional válida
- **WHEN** o investidor comprar ativo norte-americano com preço confirmado em USD e USD/BRL utilizável
- **THEN** o sistema SHALL calcular o débito em reais multiplicando o valor em USD pela cotação USD/BRL e arredondando para duas casas com `HALF_UP`

#### Scenario: Venda valida
- **WHEN** o investidor vender quantidade disponível com preço confirmado utilizável
- **THEN** o sistema SHALL registrar a movimentação, creditar o saldo e reduzir a posição

#### Scenario: Preço inválido
- **WHEN** o preço estiver ausente, zero, negativo ou incompatível com o mercado do ativo
- **THEN** o sistema SHALL rejeitar a operação sem alterar saldo, posição, histórico ou patrimônio

#### Scenario: Corretora cadastralmente inativa
- **WHEN** a associação ou a situação cadastral da corretora não estiver ativa no momento da confirmação
- **THEN** o sistema SHALL rejeitar a operação sem expor ou alterar dados financeiros

### Requirement: Abrir tela de negociação a partir do ativo

Ao acionar `Negociar` no detalhe do ativo, a interface SHALL mostrar quantidade, preço unitário preenchido com a cotação oficial e editável, e as ações `Comprar` e `Vender`. A corretora e demais dados exigidos pelo contrato SHALL ser selecionáveis ou exibidos conforme o contexto autenticado.

#### Scenario: Negociação com preço editável
- **WHEN** o investidor selecionar `Negociar`
- **THEN** a interface SHALL exibir a cotação como valor inicial do campo de preço e SHALL permitir sua edição antes da confirmação

#### Scenario: Negociação de ativo
- **WHEN** o investidor selecionar `Negociar`
- **THEN** a interface SHALL exibir quantidade, preço oficial como valor inicial e as opções de compra e venda

#### Scenario: Confirmar compra ou venda
- **WHEN** o investidor informar quantidade válida e escolher compra ou venda
- **THEN** a interface SHALL solicitar confirmação simples e SHALL enviar somente os dados autorizados pelo contrato existente, incluindo preço e data/hora confirmados

#### Scenario: Cotação desatualizada
- **WHEN** a cotação inicial estiver desatualizada
- **THEN** a interface SHALL identificar o valor como preço de fechamento e mostrar seu instante original

#### Scenario: Cotação indisponível
- **WHEN** não houver cotação ou câmbio utilizável para preencher o preço
- **THEN** a interface SHALL bloquear a tentativa ou apresentar o erro funcional retornado, sem inventar preço ou alterar saldo/posição localmente

### Requirement: Informar data e hora da operação

Compra e venda SHALL aceitar data/hora de operação editável em formato válido e SHALL preencher o campo inicialmente com o momento atual de Brasília. O backend SHALL rejeitar formato inválido ou instante futuro e SHALL registrar o instante aceito no histórico e no ponto patrimonial.

#### Scenario: Data/hora padrão
- **WHEN** o investidor abrir a negociação
- **THEN** o campo de data/hora SHALL vir preenchido com o momento atual de Brasília e permanecer editável

#### Scenario: Data/hora válida editada
- **WHEN** o investidor alterar para um instante válido não futuro e confirmar
- **THEN** o sistema SHALL registrar a compra ou venda com o instante informado

#### Scenario: Data/hora inválida
- **WHEN** o instante estiver em formato inválido ou no futuro
- **THEN** o sistema SHALL rejeitar a operação sem alterar dados financeiros
