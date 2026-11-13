## ADDED Requirements

### Requirement: Identificar preço de fechamento desatualizado

Quando a cotação do ativo exceder o limite de atualidade, o sistema SHALL manter o último valor válido e SHALL identificá-lo explicitamente como preço de fechamento, informando o instante original. A indicação SHALL aparecer em pesquisa, detalhe, operação e dashboard quando o dado for utilizado.

#### Scenario: Cotação antiga disponível
- **WHEN** a última cotação válida do ativo estiver desatualizada
- **THEN** a interface SHALL exibir o valor como preço de fechamento e mostrar sua data/hora original, sem apresentá-lo como cotação atual

#### Scenario: Cotação atual
- **WHEN** a cotação estiver dentro do limite de atualidade
- **THEN** a interface SHALL exibir a cotação normalmente e SHALL NOT mostrar aviso de preço de fechamento
