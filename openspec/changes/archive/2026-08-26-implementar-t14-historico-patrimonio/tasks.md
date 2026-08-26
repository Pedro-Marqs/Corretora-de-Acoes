## 1. Modelo e persistência do histórico

- [x] 1.1 Completar `Movement` e `PatrimonialPoint` para suportar criação validada dos cinco tipos de movimentação e preservar os dados relevantes do cálculo patrimonial, criando migration Flyway incremental quando necessário; verificar com testes de domínio/persistência que cada tipo aceita somente seus campos aplicáveis, registros inválidos são rejeitados e o schema atualizado funciona em H2/PostgreSQL conforme a infraestrutura existente.

## 2. Cálculo patrimonial

- [x] 2.1 Implementar o componente interno de cálculo do patrimônio após uma movimentação, considerando saldo, posições positivas, cotações e conversão USD/BRL quando aplicável, e persistindo os insumos utilizados no ponto patrimonial; verificar com testes que patrimônio exclusivamente em BRL, patrimônio com posições e conversão em USD e ausência de dados indispensáveis produzem os resultados esperados.

## 3. Registro transacional

- [x] 3.1 Implementar o serviço interno compartilhado que valida e registra `Movement` e seu `PatrimonialPoint` dentro da transação financeira chamadora, sem iniciar transação independente e sem disponibilizar operações funcionais de edição ou exclusão; verificar com testes de integração que movimentação e ponto são gravados juntos e que falha em qualquer etapa provoca rollback integral.

## 4. Integração e regras da T14

- [x] 4.1 Migrar o registro de saldo inicial de `AccountRegistrationService` para a nova infraestrutura e garantir que somente saldo inicial, aporte, compra, venda e transferência possam gerar histórico/ponto, sem permitir que atualização isolada de cotação ou câmbio invoque esse fluxo; verificar que o cadastro continua criando exatamente um movimento e um ponto e que atualização isolada de mercado não cria novos registros.

## 5. Validação completa

- [x] 5.1 Cobrir e validar integralmente a T14 com testes dos cinco tipos de movimentação, campos obrigatórios, imutabilidade, atomicidade, cálculo patrimonial e ausência de ponto para atualização isolada de cotação; executar a suíte relevante e depois a suíte completa do backend, validar o change com OpenSpec e confirmar que CA01–CA03 e CA06 de `historico-registro-patrimonial` e os critérios da T14 em `docs/05-tarefas.md` estão atendidos sem regressões.
