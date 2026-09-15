## ADDED Requirements

### Requirement: Identificar ativo retornado pela pesquisa
A resposta válida de pesquisa de ativo SHALL incluir `assetId`, identificador opaco do ativo ativo do catálogo que possui a cotação retornada. O identificador SHALL ser estável para o mesmo ativo catalogado entre a cotação recém-obtida e o fallback de cache e SHALL ser utilizável como referência nos contratos de compra e venda. Pesquisa sem cotação utilizável SHALL NOT publicar `assetId`.

#### Scenario: Pesquisa com cotação válida
- **WHEN** a pesquisa retornar uma cotação válida para ativo brasileiro ou norte-americano
- **THEN** a resposta SHALL incluir o `assetId` persistido do ativo junto aos dados de mercado

#### Scenario: Pesquisa usando fallback
- **WHEN** a fonte externa falhar mas houver cotação utilizável armazenada
- **THEN** a resposta SHALL incluir o mesmo `assetId` do ativo catalogado e SHALL preservar os indicadores de desatualização

#### Scenario: Sem cotação utilizável
- **WHEN** a pesquisa não possuir resposta válida nem cache utilizável
- **THEN** o sistema SHALL manter o estado funcional/ vazio existente e SHALL NOT retornar identificador de ativo operacional
