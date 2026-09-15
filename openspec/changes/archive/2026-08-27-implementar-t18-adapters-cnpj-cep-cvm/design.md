## Context

A T17 validou tecnicamente as três fontes necessárias ao domínio de corretoras e registrou seus contratos observados, campos relevantes, casos de erro e limitações.

A T18 deve transformar essas conclusões em infraestrutura de produção, mantendo as fontes externas isoladas do restante da aplicação.

A arquitetura do projeto já define que:

- integrações externas são expostas ao service através de `domain/port`;
- formatos específicos dos fornecedores permanecem em `infra/client/dto`;
- `infra/adapter` converte respostas externas para modelos internos;
- integrações devem possuir timeout e interpretar falhas;
- respostas sem campos obrigatórios devem ser rejeitadas;
- DTOs externos não podem chegar ao service ou domínio;
- chamadas externas não devem permanecer dentro de transações longas de banco.

A aplicação utiliza Spring Boot Web e já possui infraestrutura HTTP suficiente para implementar clientes síncronos sem adicionar uma nova stack reativa.

A T18 não possui delta de spec porque implementa infraestrutura interna para requisitos já existentes em `corretoras`.

See `proposal.md` - Why e a documentação técnica produzida pela T17.

## Goals / Non-Goals

**Goals:**

- Criar contratos internos independentes para CNPJ, CEP e situação regulatória CVM.
- Isolar completamente os formatos de BrasilAPI, ViaCEP e CVM.
- Converter os três provedores para modelos internos estáveis.
- Normalizar CNPJ e CEP nas fronteiras das integrações.
- Configurar URLs e timeouts externamente.
- Tratar respostas inexistentes, incompletas, `429`, `5xx`, timeout e indisponibilidade de maneira consistente.
- Implementar processamento de produção do dataset oficial da CVM.
- Evitar download repetido do dataset da CVM para cada consulta.
- Manter os adapters testáveis sem acesso real obrigatório à internet.
- Não adicionar dependências de produção sem necessidade.
- Preparar contratos que a T19 possa consumir sem conhecer detalhes dos fornecedores.

**Non-Goals:**

- Implementar cadastro ou associação de corretoras.
- Criar controller ou endpoint de corretoras.
- Persistir ou atualizar entidade `Broker`.
- Implementar autorização do investidor sobre corretoras.
- Criar interface React de corretoras.
- Implementar remoção ou reativação de associação.
- Criar retry automático complexo ou circuit breaker.
- Criar sincronização agendada da CVM.
- Criar cache distribuído ou persistente.
- Utilizar a API de corretoras da BrasilAPI como autoridade regulatória.
- Executar chamadas externas reais na suíte normal de testes.

## Decisions

### 1. Criar três ports independentes

Serão definidos contratos internos separados para:

- consulta cadastral de empresa por CNPJ;
- consulta de endereço por CEP;
- consulta da situação regulatória de intermediário na CVM.

Cada port deverá retornar apenas modelos internos necessários ao domínio.

O service futuro da T19 poderá combinar essas informações sem conhecer BrasilAPI, ViaCEP, ZIP, CSV ou qualquer DTO externo.

Os nomes exatos das interfaces podem seguir as convenções existentes do projeto, mas suas responsabilidades deverão permanecer separadas.

**Alternativa considerada:** criar um único port de `BrokerLookup` que consulte todas as fontes.

**Decisão:** manter três contratos evita acoplar fornecedores diferentes e permite testar, substituir ou evoluir cada integração independentemente.

### 2. Utilizar modelos internos próprios entre adapters e service

Cada adapter deverá converter os formatos externos para representações internas mínimas.

A consulta cadastral deverá disponibilizar somente informações necessárias como:

- CNPJ normalizado;
- razão social;
- nome fantasia quando disponível;
- situação cadastral;
- CEP e dados cadastrais relevantes.

O endereço interno deverá representar somente os componentes utilizados pelo domínio.

A consulta CVM deverá retornar informação suficiente para determinar identificação e categoria/autorização do participante.

Objetos de `infra/client/dto` MUST NOT ser retornados pelas interfaces de `domain/port`.

**Alternativa considerada:** retornar diretamente os DTOs da BrasilAPI, ViaCEP e CVM.

**Decisão:** impedir que mudanças externas contaminem service e domínio.

### 3. Utilizar cliente HTTP síncrono já disponível no Spring Web

BrasilAPI, ViaCEP e download do recurso CVM utilizarão o mecanismo HTTP síncrono disponível no stack Spring Web existente.

Não será adicionada dependência WebFlux apenas para utilizar `WebClient`.

Os clientes deverão possuir configuração própria de:

- URL base;
- recurso utilizado;
- timeout de conexão;
- timeout de leitura/resposta quando aplicável.

Esses valores deverão ser externalizados em propriedades da aplicação.

**Alternativa considerada:** adicionar nova biblioteca HTTP ou stack reativa.

**Decisão:** os casos de uso são síncronos e a aplicação é um monólito local; adicionar outra stack aumentaria complexidade sem benefício concreto nesta versão.

### 4. Normalizar identificadores na fronteira do adapter

CNPJ será convertido para representação canônica de 14 dígitos antes da consulta ou comparação.

CEP será convertido para representação canônica de oito dígitos.

O domínio e os services não deverão depender de pontuação específica retornada por algum fornecedor.

Entradas estruturalmente impossíveis deverão ser rejeitadas antes da chamada externa.

**Alternativa considerada:** enviar e comparar os valores exatamente como recebidos.

**Decisão:** as fontes podem utilizar máscaras diferentes e essa diferença não possui significado de domínio.

### 5. BrasilAPI será responsável somente pelos dados cadastrais

O client da BrasilAPI conhecerá o formato HTTP e seu DTO específico.

O adapter será responsável por:

1. normalizar o CNPJ;
2. executar a consulta;
3. validar a presença dos campos obrigatórios definidos pela T17;
4. converter a resposta para o modelo interno;
5. mapear erros externos para erros internos.

A situação regulatória de CTVM não será derivada dessa integração.

Mesmo que a BrasilAPI possua outro endpoint relacionado a corretoras, esse endpoint não será utilizado como autoridade regulatória.

**Alternativa considerada:** utilizar somente a BrasilAPI para cadastro e autorização.

**Decisão:** preservar a CVM como fonte oficial do requisito regulatório.

### 6. ViaCEP será responsável pelo endereço estruturado

O adapter de CEP receberá CEP normalizado e retornará um modelo interno de endereço.

A distinção observada na T17 entre:

- formato inválido;
- CEP inexistente;
- resposta válida;
- resposta incompleta;
- indisponibilidade;

deverá ser preservada pelo adapter através dos erros internos adequados.

Campos opcionais ausentes poderão permanecer vazios, mas ausência de campos considerados obrigatórios para o domínio deverá produzir resposta externa inválida/incompleta.

**Alternativa considerada:** depender somente do endereço recebido na consulta de CNPJ.

**Decisão:** manter a fonte específica de CEP prevista pela arquitetura e pelas specs.

### 7. O adapter CVM utilizará snapshot imutável em memória

O dataset oficial da CVM não deverá ser baixado e processado novamente para cada CNPJ consultado.

O adapter manterá um snapshot imutável em memória indexado pelo CNPJ normalizado.

O fluxo será:

1. verificar se existe snapshot válido;
2. caso inexistente ou expirado, obter o dataset oficial;
3. descompactar e interpretar o formato validado na T17;
4. validar estrutura e campos essenciais;
5. construir um novo índice completo em memória;
6. somente depois da leitura bem-sucedida substituir atomicamente o snapshot anterior;
7. consultar o CNPJ no índice.

O período de validade será configurável e terá valor padrão compatível com a atualização diária da fonte, inicialmente 24 horas.

O snapshot antigo nunca deverá ser substituído parcialmente durante uma atualização.

**Alternativa considerada:** baixar o ZIP da CVM para cada consulta.

**Decisão:** o dataset representa uma coleção completa e possui atualização periódica; baixar e processar o arquivo para cada CNPJ desperdiçaria rede e CPU.

### 8. Falha na atualização da CVM não destruirá snapshot válido anterior

Se uma atualização falhar durante download, descompactação, parsing ou validação, o snapshot anterior permanecerá intacto em memória.

A falha deverá ser reportada como erro da dependência externa quando a operação exigir atualização válida.

O adapter não deverá silenciosamente transformar uma falha de atualização em resultado regulatório negativo, pois:

`não foi possível consultar`

é semanticamente diferente de:

`CNPJ não consta como CTVM`.

**Alternativa considerada:** retornar participante inexistente quando o dataset não puder ser carregado.

**Decisão:** indisponibilidade externa não pode ser confundida com reprovação regulatória.

### 9. Atualizações concorrentes do snapshot CVM serão consolidadas

Caso múltiplas requisições encontrem o snapshot expirado simultaneamente, apenas uma deverá efetivamente realizar a atualização.

As demais não deverão iniciar downloads paralelos idênticos.

A troca do snapshot deverá ocorrer de forma atômica depois de sua construção completa.

Não será introduzido cache distribuído porque a primeira versão executa uma única instância do backend.

**Alternativa considerada:** permitir que cada thread faça seu próprio refresh.

**Decisão:** evitar carga externa e processamento duplicados sem introduzir infraestrutura desnecessária.

### 10. Centralizar a classificação das falhas externas

As três integrações deverão diferenciar pelo menos:

- entrada inválida antes da chamada;
- recurso não encontrado ou dado inexistente;
- resposta com campo obrigatório ausente;
- `429` / limite externo;
- `5xx`;
- timeout;
- erro de transporte;
- conteúdo externo inválido ou impossível de interpretar.

Essas situações serão convertidas em exceções ou resultados internos compatíveis com o tratamento centralizado já criado no projeto.

O service não deverá interpretar códigos HTTP dos fornecedores.

Uma resposta `404` ou equivalente não deverá ser automaticamente tratada da mesma forma que timeout ou `500`.

**Alternativa considerada:** converter qualquer falha externa em uma exceção genérica.

**Decisão:** a T19 precisará distinguir uma instituição inexistente/não autorizada de uma fonte temporariamente indisponível.

### 11. Não adicionar retry automático nesta tarefa

Os adapters terão timeout, porém a T18 não implementará retry automático.

Em particular, uma resposta `429` não deverá gerar repetição imediata da mesma solicitação.

A estratégia futura de retry poderá ser introduzida somente se houver necessidade observada e com política explícita de backoff.

**Alternativa considerada:** repetir automaticamente chamadas que falharem.

**Decisão:** retries ingênuos podem ampliar indisponibilidade, aumentar latência e agravar rate limiting.

### 12. Chamadas externas deverão ocorrer fora de transações longas de persistência

Os ports e adapters não controlarão transações de banco.

A T19 deverá realizar a obtenção e validação dos dados externos antes do trecho transacional responsável por persistir ou associar uma corretora.

A T18 não deverá introduzir `@Transactional` nos adapters.

**Alternativa considerada:** permitir que o futuro cadastro abra uma transação e execute as três fontes dentro dela.

**Decisão:** manter transações de banco curtas e não prendê-las à latência de terceiros, conforme a arquitetura do projeto.

### 13. A suíte padrão será totalmente determinística e offline

Testes dos clients HTTP deverão usar servidor/respostas simuladas com a infraestrutura de teste já disponível.

Os testes deverão cobrir:

- sucesso;
- dado inexistente;
- campos obrigatórios ausentes;
- `429`;
- `5xx`;
- timeout ou falha equivalente de transporte.

O parser CVM utilizará fixtures pequenas contendo o mesmo formato validado na T17, incluindo ZIP e conteúdo interno representativo.

Nenhum teste executado normalmente por `mvn test` deverá depender de BrasilAPI, ViaCEP ou CVM reais.

Os smoke tests reais realizados pela T17 permanecem separados da suíte determinística.

**Alternativa considerada:** verificar os adapters contra os serviços reais em todo build.

**Decisão:** disponibilidade externa não determina a correção do código do projeto.

### 14. Não adicionar novas dependências sem necessidade comprovada

O projeto já possui Spring Web e recursos da JDK suficientes para:

- realizar HTTP;
- trabalhar com streams;
- ler ZIP;
- manter estruturas em memória.

O parser do formato CVM deverá priorizar os recursos existentes e a solução mínima compatível com o formato efetivamente observado pela T17.

Uma dependência adicional somente poderá ser introduzida se o formato real não puder ser tratado adequadamente com a stack existente e o benefício for documentado.

**Alternativa considerada:** adicionar antecipadamente bibliotecas HTTP, cache e resiliência.

**Decisão:** manter pequena a superfície tecnológica da primeira versão.

### 15. A T18 não criará um agregador de corretora

Não será criado nesta tarefa um serviço que execute:

BrasilAPI → ViaCEP → CVM → cadastro.

Cada adapter deverá poder ser utilizado individualmente.

A composição dessas três fontes, suas regras de precedência e a decisão final de aceitar ou rejeitar uma corretora pertencem ao caso de uso da T19.

**Alternativa considerada:** finalizar já na T18 todo o fluxo de pesquisa da corretora.

**Decisão:** preservar a divisão planejada entre infraestrutura externa e regra de negócio.

## Risks / Trade-offs

- **[Mudança no contrato da BrasilAPI ou ViaCEP]** → Isolar DTOs externos e validar campos obrigatórios antes da conversão.
- **[Mudança no formato do dataset CVM]** → Concentrar parsing em um componente específico e cobrir o formato validado com fixtures.
- **[Download da CVM em toda consulta]** → Utilizar snapshot indexado em memória com validade configurável.
- **[Snapshot parcialmente construído ser utilizado]** → Construir nova versão isoladamente e substituir a referência somente após sucesso completo.
- **[Múltiplas threads atualizarem a CVM simultaneamente]** → Serializar o refresh e publicar o snapshot de forma atômica.
- **[Dados CVM antigos serem confundidos com dados atuais]** → Registrar instante do snapshot e exigir refresh após o período configurado.
- **[Falha externa ser interpretada como corretora inválida]** → Manter erros de indisponibilidade distintos de resultados negativos de negócio.
- **[DTO externo contaminar o service]** → Fazer `domain/port` retornar exclusivamente modelos internos.
- **[429 provocar cascata de retries]** → Não implementar retry automático na T18.
- **[Teste ficar instável por depender da internet]** → Usar respostas simuladas e fixtures na suíte padrão.
- **[Transação de banco ficar aberta durante acesso externo]** → Adapters não serão transacionais e a T19 deverá buscar dados antes da persistência.
- **[T18 avançar para regra de negócio da T19]** → Não criar controller, service agregador, persistência ou associação de corretora.
- **[Cache em memória não funcionar entre múltiplas instâncias]** → Aceitar essa limitação porque a primeira versão executa uma única instância local.

## Migration Plan

1. Definir os três contratos internos em `domain/port` e seus modelos internos mínimos.
2. Externalizar URLs e timeouts das fontes.
3. Implementar client e DTOs da BrasilAPI.
4. Implementar seu adapter e mapeamento de erros.
5. Implementar client e DTOs do ViaCEP.
6. Implementar seu adapter e mapeamento de erros.
7. Implementar obtenção e parsing do dataset CVM conforme o formato comprovado na T17.
8. Criar índice por CNPJ e gerenciamento do snapshot em memória.
9. Implementar adapter regulatório da CVM e seus erros.
10. Adicionar testes determinísticos para sucesso, resposta incompleta, inexistência, `429`, `5xx`, timeout e parsing inválido.
11. Confirmar que nenhum DTO externo é exposto pelos ports.
12. Executar a suíte completa e validar que nenhuma funcionalidade de cadastro de corretora foi antecipada.

Nenhuma migration de banco é necessária.

O rollback consiste em remover ports, clients, DTOs, adapters e configurações adicionados pela T18. Como nenhum endpoint ou dado persistido será criado nesta tarefa, o rollback não exige migração ou correção de dados.