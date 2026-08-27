## 1. Contratos internos e configuração

- [x] 1.1 Criar os ports e modelos internos mínimos para consulta cadastral por CNPJ, endereço por CEP e situação regulatória na CVM, além de externalizar URLs e timeouts das três fontes; verificar com testes e inspeção de dependências que `domain/port` não expõe tipos de `infra/client/dto`, que CNPJ e CEP são normalizados nas fronteiras e que nenhuma nova dependência de produção é adicionada sem necessidade.

## 2. Adapters de BrasilAPI e ViaCEP

- [x] 2.1 Implementar clients, DTOs externos e adapters da BrasilAPI e ViaCEP, convertendo respostas válidas para os modelos internos e distinguindo dado inexistente, resposta incompleta, `429`, `5xx`, timeout e falha de transporte; verificar com testes determinísticos usando respostas simuladas que os campos obrigatórios são validados, DTOs externos não chegam ao service e nenhuma chamada real à internet é necessária na suíte padrão.

## 3. Adapter e snapshot da CVM

- [x] 3.1 Implementar a obtenção e o processamento de produção do dataset oficial da CVM conforme o formato validado na T17, construindo snapshot imutável indexado por CNPJ, com validade configurável, atualização única para requisições concorrentes e substituição atômica somente após processamento completo; verificar com fixtures locais que CNPJ conhecido pode ser localizado, categoria regulatória é interpretada corretamente, refresh concorrente não dispara downloads duplicados e falha durante atualização não publica snapshot parcial.

## 4. Consistência de falhas e isolamento das integrações

- [x] 4.1 Consolidar o mapeamento das falhas externas das três fontes para erros internos consistentes, preservando a diferença entre dado inexistente, resposta inválida e dependência indisponível e mantendo chamadas externas fora de transações de persistência; verificar com testes que `404` ou ausência de registro não é tratado como timeout/`5xx`, `429` não provoca retry automático, falha de atualização da CVM não é interpretada como reprovação regulatória e nenhum adapter possui responsabilidade de cadastro ou persistência de corretora.

## 5. Validação completa da T18

- [x] 5.1 Cobrir os adapters de CNPJ, CEP e CVM com testes de sucesso, campos obrigatórios ausentes, inexistência, `429`, `5xx`, timeout, conteúdo inválido e snapshot anterior preservado, confirmar que a suíte normal permanece totalmente offline e que nenhum controller, cadastro, associação ou persistência de corretora foi antecipado; executar os testes focados, depois `.\mvnw.cmd test` e finalizar com `openspec validate implementar-t18-adapters-cnpj-cep-cvm --type change`, confirmando ausência de regressões.
