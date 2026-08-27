## Why

A implementação definitiva de corretoras depende de três fontes externas com formatos, limitações e comportamentos de falha próprios. Antes de criar os adapters da T18, a T17 deve comprovar que BrasilAPI, ViaCEP e uma fonte oficial processável da CVM fornecem os dados necessários de forma reproduzível, evitando construir a integração sobre premissas não validadas.

## What Changes

- Validar tecnicamente a consulta de dados cadastrais de CNPJ através da BrasilAPI.
- Validar tecnicamente a consulta e complementação de endereço através do ViaCEP.
- Identificar e validar uma fonte oficial processável da CVM para participantes intermediários.
- Demonstrar como localizar uma instituição pelo CNPJ e identificar sua categoria/autorização como `CTVM` sem automação de página, scraping de interface ou CAPTCHA.
- Mapear os campos necessários de cada fonte para os dados de corretora definidos pelo projeto.
- Registrar formatos de resposta, comportamento para dados ausentes, indisponibilidade e entradas inválidas.
- Registrar limitações relevantes de uso das fontes externas para orientar a implementação da T18.
- Criar somente protótipos descartáveis ou testes de contrato necessários para comprovar as integrações.
- Não criar cadastro funcional de corretora, endpoints de negócio, persistência de corretora ou associação com conta nesta tarefa.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. A T17 valida tecnicamente fontes externas necessárias para requisitos já definidos em `corretoras`; não altera o comportamento esperado da capability.

## Impact

- Protótipos descartáveis ou testes de contrato relacionados a integrações externas.
- Documentação técnica das fontes de CNPJ, CEP e CVM.
- Configuração local dos endpoints externos quando necessária para os testes.
- BrasilAPI como fonte de dados cadastrais de CNPJ.
- ViaCEP como fonte de endereço por CEP.
- Portal de Dados Abertos da CVM como fonte oficial para validação de participantes intermediários.
- Decisões técnicas que serão utilizadas na T18 para implementar portas, clientes e adapters definitivos.
- Nenhuma alteração funcional de API, frontend, banco de dados ou regras de corretoras.