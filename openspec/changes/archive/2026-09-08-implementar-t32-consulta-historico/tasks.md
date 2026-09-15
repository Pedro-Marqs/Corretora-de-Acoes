## 1. Contrato e validação da consulta

- [x] 1.1 Definir o contrato de `GET /api/history`, incluindo `page` zero-based, limite fixo de 20, filtros `from` inclusivo, `to` exclusivo, tipo, ticker, corretora e mercado, e metadados `content`, `page`, `size`, `totalElements` e `totalPages`; verificar que o contrato cobre todos os campos observáveis da spec sem expor credenciais ou entidades internas
- [x] 1.2 Implementar a validação dos parâmetros e o mapeamento dos valores de filtro para o domínio existente; verificar rejeição uniforme de página negativa, datas inválidas, intervalo invertido, enumerações desconhecidas e identificadores malformados

## 2. Consulta persistente somente leitura

- [x] 2.1 Criar a projeção de leitura das movimentações e a consulta do repositório restrita à conta autenticada, com filtros combináveis por AND, ordenação por instante decrescente e identificador decrescente como desempate; verificar que a consulta não altera movimentações nem pontos patrimoniais
- [x] 2.2 Aplicar paginação fixa de 20 registros e calcular metadados consistentes para resultados completos, parciais e páginas vazias; verificar consulta com mais de 20 registros e filtros que não encontram correspondência

## 3. Endpoint e regras de acesso

- [x] 3.1 Implementar o service e o controller autenticados para `GET /api/history`, retornando somente a projeção da conta da sessão e mantendo a resposta de página vazia em HTTP 200; verificar que a corretora ou o identificador de outra conta nunca amplia o escopo da consulta
- [x] 3.2 Integrar validações e falhas ao contrato uniforme da API, sem criar endpoints de edição ou exclusão; verificar resposta de sessão ausente/expirada, parâmetros inválidos e ausência de qualquer mutação durante a consulta

## 4. Cobertura funcional e de isolamento

- [x] 4.1 Criar testes para ordenação, paginação, página vazia e cada filtro isolado, além de combinações de filtros; verificar que somente movimentações concluídas aparecem e que transferências preservam seus campos históricos na leitura
- [x] 4.2 Criar testes de isolamento entre duas contas e de imutabilidade após consultas repetidas; verificar que uma conta não recebe registros ou metadados de outra e que nenhum dado persistido é alterado

## 5. Validação integrada da entrega

- [x] 5.1 Executar a suíte focada do histórico e os testes backend relacionados, incluindo compilação e análise estática aplicáveis; verificar todos os cenários da delta spec e corrigir regressões sem ampliar o escopo para a interface T33
- [x] 5.2 Executar `git diff --check` e `openspec validate implementar-t32-consulta-historico --strict`; verificar que proposal, delta spec, design e tasks permanecem coerentes e que o change fica pronto para implementação
