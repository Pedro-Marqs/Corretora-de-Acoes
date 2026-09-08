## 1. Contrato e consulta autenticada

- [x] 1.1 Definir DTOs/projeções e o endpoint autenticado `GET /api/dashboard`, com resposta de saldo, posições, indicadores, cotação/câmbio utilizados e avisos; verificar contrato HTTP, autenticação e ausência de entidades ou identificadores internos
- [x] 1.2 Implementar a consulta isolada pela conta da sessão, usando leitura consistente de saldo, posições, snapshots e movimentações sem efeitos colaterais; verificar isolamento entre duas contas e ausência de criação de movimentação/ponto patrimonial

## 2. Cálculo e estados financeiros

- [x] 2.1 Compor preço médio, valor de mercado, patrimônio, resultado realizado, valorização não realizada e resultado total reutilizando as regras financeiras da T26, excluindo saldo inicial e aportes; verificar exemplos numéricos de conta sem posições, carteira brasileira e carteira com operações
- [x] 2.2 Implementar conversão de ativos norte-americanos para BRL com a cotação USD/BRL aplicável, propagando valor e instante do câmbio; verificar conversão, arredondamento, ausência de câmbio utilizável e preservação do último valor válido com marcador de desatualização

## 3. Validação integrada

- [x] 3.1 Cobrir erros de sessão, conta sem dados, cotações antigas e respostas funcionais uniformes sem vazamento de dados; verificar cenários correspondentes da spec `dashboards` e que aportes não alteram os resultados de investimento
- [x] 3.2 Executar a suíte backend focada e a suíte completa aplicável, compilação/empacotamento Maven, `git diff --check` e `openspec validate implementar-t34-indicadores-dashboard --strict`, corrigindo falhas sem alterar o escopo
