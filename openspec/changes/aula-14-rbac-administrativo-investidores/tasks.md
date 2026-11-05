## 1. Identidade e persistência administrativa

- [ ] 1.1 Definir `AdminUser`, estado, papel único `ADMIN`, DTOs seguros e repositórios sem relação financeira com `Account`; verificar que nenhum DTO contém senha ou hash.
- [ ] 1.2 Criar migrações para administradores e auditoria sem alterar tabelas de saldo, posições, movimentos ou patrimônio; verificar schema vazio e schema existente.
- [ ] 1.3 Implementar bootstrap único controlado por segredo externo, com BCrypt e proteção contra repetição; verificar ausência de cadastro HTTP e de segredos em logs.

## 2. JWT e autorização da área administrativa

- [ ] 2.1 Integrar JWT administrativo com audience, principal type e configuração de assinatura separados do investidor; verificar token administrativo válido e tokens cruzados rejeitados.
- [ ] 2.2 Configurar autorização HTTP e em nível de método para `ADMIN`, usando default deny; verificar investidor `USER` bloqueado, acesso `ADMIN` permitido, `401` e `403` por audience e estado.
- [ ] 2.3 Implementar login/logout administrativo e revogação compatível com a infraestrutura JWT da Aula 13; verificar expiração, logout e desativação do operador.

## 3. Gestão de investidores

- [ ] 3.1 Implementar consulta paginada com filtros por estado, busca e período, retornando apenas campos administrativos mínimos; verificar paginação, ordenação e validação.
- [ ] 3.2 Implementar ativação e desativação somente para `ADMIN`, revogando credenciais da conta alvo quando aplicável e preservando dados históricos; verificar investidor `USER` bloqueado e rollback.
- [ ] 3.3 Registrar auditoria de bootstrap, login relevante, ações sensíveis e tentativas negadas; verificar ator, papel, alvo, instante, resultado e ausência de token/senha/payload.

## 4. Dashboard administrativo

- [ ] 4.1 Implementar métricas de contas e negociações por período, quantidades e volumes separados por moeda; verificar as oito categorias solicitadas e filtros temporais.
- [ ] 4.2 Implementar séries temporais e ranking determinístico dos ativos mais negociados; verificar empates, limites, dados vazios e ausência de efeitos colaterais.
- [ ] 4.3 Implementar conversão monetária somente com câmbio válido e informar taxa/instante ou indisponibilidade; verificar BRL, USD e arredondamento financeiro existente.
- [ ] 4.4 Criar área administrativa isolada no frontend, sem reutilizar carteira/histórico do investidor; verificar rotas, estados de erro e ausência de acesso cruzado.

## 5. Validação transversal

- [ ] 5.1 Testar que `ADMIN` não movimenta recursos nem consulta dados individuais de investidores; verificar saldo, posições, movimentações e patrimônio inalterados.
- [ ] 5.2 Consultar Graphify para revisar dependências de segurança, contas, movimentos e frontend e executar `graphify update .` após o código; verificar grafo atualizado.
- [ ] 5.3 Executar testes focados e registrar riscos; deixar reviewer, tester e validação OpenSpec como gates finais.
