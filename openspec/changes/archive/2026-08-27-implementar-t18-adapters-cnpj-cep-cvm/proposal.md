## Why

A T17 comprovou que BrasilAPI, ViaCEP e os dados abertos oficiais da CVM fornecem as informações necessárias para validar corretoras, mas essas fontes ainda não estão isoladas por contratos internos reutilizáveis. A T18 transforma essa prova técnica em integrações de produção testáveis, preparando a implementação do cadastro de corretoras sem acoplar as regras de negócio aos formatos e falhas específicas de serviços externos.

## What Changes

- Definir contratos internos mínimos para consulta de dados cadastrais de CNPJ, endereço por CEP e situação regulatória na CVM.
- Implementar integração de produção com a BrasilAPI para obtenção dos dados empresariais necessários.
- Implementar integração de produção com o ViaCEP para obtenção de endereço estruturado.
- Implementar integração de produção com a fonte oficial de Participantes Intermediários da CVM validada na T17.
- Converter respostas e formatos externos em modelos internos próprios, impedindo que DTOs de fornecedores cheguem à camada de serviço.
- Normalizar identificadores, especialmente CNPJ e CEP, nas fronteiras das integrações.
- Configurar timeout para chamadas externas.
- Mapear respostas incompletas, entradas inexistentes, limite de requisições, erros de servidor, timeout e indisponibilidade para erros internos consistentes.
- Criar testes determinísticos para os adapters sem tornar a suíte normal dependente da disponibilidade das fontes externas.
- Reutilizar as conclusões, formatos e fixtures produzidos pela T17, sem repetir a investigação técnica já concluída.
- Não implementar cadastro, associação, remoção ou interface de corretoras nesta tarefa.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. A T18 implementa infraestrutura interna necessária para requisitos já definidos em `corretoras`, sem introduzir ou alterar comportamento funcional exposto ao investidor.

## Impact

- `domain/port`, com os contratos internos das fontes externas.
- `infra/client` e `infra/client/dto`, com clientes e representações específicas de BrasilAPI, ViaCEP e CVM.
- `infra/adapter`, com conversão das respostas externas para modelos internos.
- Configuração de endpoints e timeouts das integrações externas.
- Tratamento padronizado de falhas externas compatível com a infraestrutura de erros existente.
- Testes unitários e de integração dos clients/adapters usando respostas controladas.
- BrasilAPI como fonte cadastral de CNPJ.
- ViaCEP como fonte de endereço.
- Dados Abertos da CVM como autoridade regulatória para identificação de participantes e categoria `CTVM`.
- Base técnica para a T19 implementar os casos de uso de cadastro e administração de corretoras.
- Nenhuma alteração de banco de dados, frontend ou endpoint funcional nesta tarefa.