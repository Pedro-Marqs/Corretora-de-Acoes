## Context

A T17 antecede a implementação dos adapters definitivos da T18 e existe para reduzir o risco das integrações externas necessárias ao cadastro de corretoras.

A spec de corretoras exige combinar três responsabilidades distintas:

- situação cadastral e dados empresariais do CNPJ;
- endereço estruturado;
- comprovação oficial de que a instituição consta na CVM na categoria exigida pelo projeto.

Atualmente:

- a BrasilAPI disponibiliza consulta individual por CNPJ;
- o ViaCEP disponibiliza consulta individual de endereço por CEP;
- a CVM publica o conjunto oficial `Participantes Intermediários: Informação Cadastral`, com dados referentes ao último dia útil, atualização diária, arquivo ZIP de dados e ZIP separado com o dicionário das colunas.

A BrasilAPI também possui uma API própria de corretoras baseada em dados da CVM, mas ela não substituirá a fonte oficial da CVM para a decisão regulatória do projeto.

Esta tarefa não possui delta de spec porque valida premissas técnicas para requisitos já existentes em `corretoras`.

See `proposal.md` - Why.

## Goals / Non-Goals

**Goals:**

- Demonstrar que um CNPJ pode ser consultado individualmente na BrasilAPI.
- Identificar os campos da BrasilAPI úteis ao modelo de corretora.
- Demonstrar que o CEP obtido pode ser consultado no ViaCEP e convertido em endereço estruturado.
- Baixar e inspecionar programaticamente o dataset oficial de Participantes Intermediários da CVM.
- Utilizar o dicionário oficial da CVM para identificar as colunas necessárias à validação regulatória.
- Demonstrar como localizar uma instituição no dataset da CVM pelo CNPJ.
- Demonstrar como distinguir uma instituição que satisfaz a categoria `CTVM` de outra que não satisfaz.
- Registrar formatos, campos obrigatórios, respostas inválidas, indisponibilidade e limitações conhecidas de cada fonte.
- Produzir evidência reproduzível suficiente para orientar a implementação dos adapters da T18.
- Manter testes dependentes da internet separados da suíte normal do projeto.

**Non-Goals:**

- Implementar os adapters definitivos da T18.
- Criar portas de domínio definitivas para integrações externas.
- Criar endpoint de cadastro ou consulta de corretoras.
- Persistir corretoras ou associações com contas.
- Implementar cache definitivo.
- Implementar sincronização periódica da CVM.
- Implementar retry, circuit breaker ou estratégia definitiva de resiliência.
- Criar parser de produção definitivo para os arquivos da CVM.
- Utilizar scraping, automação de navegador ou CAPTCHA.
- Utilizar a API de corretoras da BrasilAPI como autoridade regulatória no lugar da CVM.
- Fazer varreduras em massa da BrasilAPI ou ViaCEP.

## Decisions

### 1. Separar validação ao vivo de testes reproduzíveis

A prova técnica terá dois tipos de verificação:

1. consultas ao vivo às fontes externas para comprovar disponibilidade e contrato observado;
2. testes locais usando respostas representativas para validar parsing e mapeamento sem depender da internet.

Testes que acessam serviços reais deverão ser explicitamente identificados como testes externos e não deverão fazer parte da suíte normal executada a cada build.

A execução ao vivo deverá ser opt-in e possuir comando documentado.

**Alternativa considerada:** executar consultas reais sempre que a suíte de testes rodar.

**Decisão:** não tornar o build dependente da disponibilidade, latência ou mudanças temporárias de terceiros.

### 2. Usar BrasilAPI somente para dados cadastrais de CNPJ

A prova deverá validar a consulta individual de CNPJ através do contrato documentado pela BrasilAPI.

Deverão ser identificados pelo menos os campos necessários para:

- CNPJ;
- razão social;
- nome fantasia, quando disponível;
- situação cadastral;
- CEP;
- dados de endereço disponíveis.

Também deverão ser registrados os comportamentos observados para:

- consulta válida;
- CNPJ inválido ou malformado;
- CNPJ não encontrado;
- resposta indisponível ou incompleta.

A situação cadastral empresarial será responsabilidade dessa fonte, enquanto a autorização regulatória permanecerá responsabilidade da CVM.

**Alternativa considerada:** obter todos os dados exclusivamente através da API de corretoras da BrasilAPI.

**Decisão:** preservar separação entre fonte cadastral e fonte regulatória e manter a CVM como autoridade para o requisito `CTVM`.

### 3. Usar ViaCEP para estruturar e complementar o endereço

O CEP obtido durante a consulta cadastral será normalizado para oito dígitos antes de acessar o ViaCEP.

A prova deverá mapear pelo menos:

- CEP;
- logradouro;
- complemento, quando disponível;
- bairro;
- município;
- UF.

Deverá ser distinguido:

- CEP em formato inválido;
- CEP válido e encontrado;
- CEP válido porém inexistente;
- resposta indisponível.

O ViaCEP será utilizado para consulta pontual, nunca para varredura ou validação em massa de uma base local.

**Alternativa considerada:** utilizar somente o endereço retornado pela BrasilAPI.

**Decisão:** validar o ViaCEP porque o desenho funcional do projeto prevê fonte própria de CEP e porque isso permite obter endereço estruturado independentemente do formato cadastral da fonte de CNPJ.

### 4. Utilizar diretamente o conjunto oficial de Participantes Intermediários da CVM

A fonte regulatória da T17 será o conjunto oficial:

`Participantes Intermediários: Informação Cadastral`

A prova deverá utilizar os recursos oficiais disponibilizados pela CVM:

- arquivo de dados `cad_intermed.zip`;
- dicionário de dados `meta_cad_intermed.zip`.

O protótipo deverá comprovar que é possível:

1. obter o arquivo;
2. descompactá-lo programaticamente;
3. identificar o arquivo cadastral relevante;
4. interpretar seu cabeçalho/formato;
5. localizar um participante pelo CNPJ;
6. identificar, através das colunas documentadas pela própria CVM, a informação necessária para determinar se a instituição satisfaz a categoria `CTVM`.

O nome exato das colunas e valores utilizados deverá ser registrado somente depois da inspeção do dicionário oficial, evitando codificar uma suposição sobre o formato.

**Alternativa considerada:** automatizar a página de consulta da CVM.

**Decisão:** utilizar dados abertos estruturados, reproduzíveis e oficialmente publicados, eliminando dependência de interface web, navegador ou CAPTCHA.

### 5. Não usar a API de corretoras da BrasilAPI como fonte regulatória

A BrasilAPI atualmente também expõe dados de corretoras originados da CVM e pode ser utilizada apenas como comparação diagnóstica durante a prova.

O resultado funcional do teste de autorização não deverá depender dela.

Uma instituição somente será considerada validada para o propósito da prova quando sua classificação puder ser demonstrada diretamente a partir dos dados oficiais da CVM.

**Alternativa considerada:** consumir o endpoint pronto de corretoras da BrasilAPI e dispensar processamento do arquivo da CVM.

**Decisão:** a spec exige dados oficiais da CVM e a T17 existe justamente para demonstrar que essa fonte oficial é processável antes da implementação definitiva.

### 6. Normalizar CNPJ antes de cruzar fontes

Toda comparação entre BrasilAPI e CVM utilizará representação canônica do CNPJ, removendo pontuação e preservando os 14 caracteres correspondentes.

A mesma estratégia será documentada para a T18.

A prova não deverá depender de uma fonte retornar CNPJ com a mesma máscara utilizada por outra.

**Alternativa considerada:** comparar os valores textuais exatamente como recebidos.

**Decisão:** normalização evita falsos negativos decorrentes apenas de diferenças de formatação.

### 7. Validar pelo menos um caso CTVM e um caso negativo

A prova utilizará ao menos:

- um CNPJ conhecido que satisfaça o critério CTVM;
- um CNPJ válido e ativo que não satisfaça esse critério.

O caso positivo deverá passar pelas três fontes quando aplicável:

1. BrasilAPI para cadastro;
2. ViaCEP para endereço;
3. CVM para autorização/categoria.

O caso negativo deverá demonstrar que possuir CNPJ empresarial válido e ativo não é suficiente para ser aceito como CTVM.

Os CNPJs efetivamente utilizados e a data da verificação deverão ser documentados para tornar o resultado auditável.

**Alternativa considerada:** validar apenas o caminho positivo.

**Decisão:** o caso negativo é necessário para comprovar que o cruzamento com a CVM realmente exerce uma regra e não apenas confirma a existência do CNPJ.

### 8. Tratar o parsing da CVM como protótipo, não como adapter de produção

A T17 deverá implementar apenas o processamento mínimo necessário para comprovar o formato observado.

Não deverá antecipar:

- abstrações definitivas;
- cache;
- atualização automática;
- modelo de domínio final;
- política de retry;
- mecanismo definitivo de parsing.

Se for necessário código específico para interpretar o arquivo durante a prova, ele deverá permanecer claramente identificado como teste, protótipo ou fixture.

A T18 poderá então substituir esse código pela implementação definitiva baseada nas conclusões documentadas.

**Alternativa considerada:** aproveitar a T17 para já criar o cliente CVM definitivo.

**Decisão:** manter a fronteira entre descoberta técnica e implementação evita consolidar uma arquitetura antes da validação do formato real.

### 9. Não adicionar dependências de runtime apenas para a prova técnica

A T17 deverá priorizar recursos já disponíveis no projeto e na JDK para:

- requisições HTTP;
- leitura de streams;
- descompactação de ZIP.

Se a inspeção do formato exigir alguma ferramenta auxiliar, ela deverá permanecer restrita ao escopo da prova e não se tornar dependência de produção automaticamente.

Qualquer biblioteca considerada necessária para o adapter definitivo será decidida na T18 com base no formato confirmado.

**Alternativa considerada:** adicionar antecipadamente bibliotecas de HTTP, CSV ou resiliência ao projeto principal.

**Decisão:** evitar aumentar a superfície de produção antes de saber exatamente o que a integração exige.

### 10. Documentar um contrato observado para orientar a T18

A T17 produzirá documentação técnica versionada em `docs/` contendo, para cada fonte:

- finalidade;
- endpoint ou recurso utilizado;
- método/formato;
- campos consumidos;
- exemplo de resultado relevante;
- comportamento para entrada inválida;
- comportamento para ausência de dados;
- comportamento observado em indisponibilidade;
- limitações de uso;
- data em que a prova foi executada.

Para a CVM também deverão ser documentados:

- URL do conjunto;
- URL ou padrão do arquivo de dados;
- dicionário utilizado;
- formato interno encontrado;
- colunas utilizadas para CNPJ e classificação;
- valor ou regra que representa `CTVM`;
- periodicidade observada.

Esse documento será a referência técnica inicial da T18, evitando que o próximo change repita toda a investigação.

**Alternativa considerada:** deixar as descobertas somente nos testes ou na saída do terminal.

**Decisão:** integrações externas mudam e precisam de uma evidência versionada que explique de onde vieram as decisões.

### 11. Não definir estratégia definitiva de atualização do arquivo CVM na T17

A fonte da CVM é publicada como dataset atualizado diariamente, mas a T17 não decidirá ainda se a aplicação definitiva deverá:

- baixar o arquivo em cada consulta;
- usar cache;
- atualizar periodicamente;
- manter snapshot local.

A prova deverá apenas registrar tamanho aproximado, frequência de publicação, custo de processamento e características observadas suficientes para que a T18 escolha a estratégia.

**Alternativa considerada:** implementar imediatamente atualização agendada.

**Decisão:** primeiro medir e compreender a fonte; a estratégia operacional pertence ao adapter definitivo.

## Risks / Trade-offs

- **[Serviço externo indisponível tornar os testes instáveis]** → Manter verificações ao vivo fora da suíte padrão e utilizar respostas locais para testes determinísticos.
- **[BrasilAPI ou ViaCEP mudarem seus contratos]** → Documentar campos realmente utilizados e validar explicitamente ausência de campos obrigatórios.
- **[Formato do arquivo CVM mudar]** → Consultar o dicionário oficial e evitar assumir nomes de colunas antes da inspeção.
- **[CNPJ possuir máscaras diferentes entre fontes]** → Normalizar para representação canônica antes do cruzamento.
- **[BrasilAPI informar corretora enquanto a fonte CVM diverge]** → Utilizar a CVM como autoridade regulatória.
- **[Uso excessivo de APIs públicas provocar bloqueio]** → Fazer somente consultas pontuais necessárias à prova e nunca executar crawling ou full scan.
- **[Prova técnica virar implementação definitiva antecipada]** → Manter código experimental em escopo de teste/protótipo e deixar abstrações de produção para a T18.
- **[Dataset CVM ser atualizado após a execução e alterar um caso de teste]** → Registrar data da consulta e manter fixtures reproduzíveis separadas do smoke test ao vivo.
- **[Endereço divergir entre CNPJ e CEP]** → Registrar a divergência e as responsabilidades de cada fonte sem criar nesta tarefa uma regra de persistência definitiva.
- **[Caso positivo ou negativo mudar de situação no futuro]** → Utilizar os casos para smoke test atual e fixtures estáveis para testes determinísticos.

## Migration Plan

1. Criar a estrutura mínima de prova/teste externo sem alterar código funcional de corretoras.
2. Validar consulta de um CNPJ conhecido pela BrasilAPI e registrar contrato/campos observados.
3. Consultar o CEP correspondente no ViaCEP e registrar seu contrato e casos de erro.
4. Baixar o dicionário e o dataset de Participantes Intermediários da CVM.
5. Inspecionar e documentar formato, arquivos internos e colunas relevantes.
6. Demonstrar busca por CNPJ e identificação da categoria `CTVM`.
7. Executar um caso positivo CTVM e um caso negativo.
8. Criar fixtures mínimas para tornar parsing e mapeamento reproduzíveis sem internet.
9. Documentar endpoints, recursos, campos, limitações, falhas e resultados da prova.
10. Executar a suíte normal confirmando que os testes externos não introduzem dependência de rede no build.

Não existe migration de banco ou deploy funcional na T17.

O rollback consiste em remover protótipos, fixtures e documentação adicionados pelo change. Nenhuma API, tabela ou comportamento de negócio existente deverá precisar ser revertido.