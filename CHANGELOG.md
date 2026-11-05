# Changelog

Este arquivo registra as mudanças relevantes do projeto. A seção abaixo prepara a primeira release e permanece como `Unreleased` até a publicação da tag e da release no GitHub.

## [1.0.0] - Unreleased

### Funcionalidades

- Cadastro, autenticação por sessão, logout, reativação e exclusão de conta.
- Atualização de e-mail e senha.
- Saldo fictício em reais, com depósitos e retiradas.
- Cadastro e associação de corretoras por CNPJ.
- Pesquisa de ativos, consulta de cotações e conversão USD/BRL por integrações externas.
- Cache de cotações, incluindo indicação de dados desatualizados.
- Registro de compras, vendas e transferências de posições.
- Carteira com posições, custo médio e resultados realizados e não realizados.
- Histórico de movimentações, evolução patrimonial e dashboard.
- Exportação de dados da carteira para planilha XLSX.

### Plataforma e operação

- Backend Java 17 com Spring Boot, JPA/Hibernate, Liquibase e PostgreSQL.
- Frontend React com Vite.
- Execução completa via Docker Compose, com PostgreSQL, backend e frontend.
- Suíte de testes automatizados no backend e frontend, com verificação de cobertura na CI.
