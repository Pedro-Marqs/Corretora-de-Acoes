## Context

A T22 concluiu os ports e adapters para Brapi, Twelve Data e AwesomeAPI, retornando modelos internos validados de mercado e câmbio. A T23 passa a consumir esses contratos para criar o catálogo persistido e o cache utilizado pelas funcionalidades posteriores.

O banco já possui `asset`, `quote` e `exchange_rate`, porém o modelo atual ainda não armazena todos os metadados exigidos pela spec: `Asset` não possui tipo/status e `Quote`/`ExchangeRate` não persistem a fonte.

A tabela `quote` possui uma linha por ativo e `exchange_rate` uma linha por par, estrutura adequada para representar o último valor válido utilizado como cache.

See `proposal.md` - Why e `specs/ativos-cotacoes-cambio/spec.md`.

## Goals / Non-Goals

**Goals:**

- Reutilizar exclusivamente os ports criados na T22 para acesso aos fornecedores.
- Persistir somente snapshots externos completos e válidos.
- Manter uma única cotação válida atual por ativo e um único USD/BRL válido.
- Preservar o cache anterior quando uma atualização externa falhar.
- Separar consulta externa da transação de persistência.
- Calcular idade dos dados usando `quotedAt` e relógio injetável.
- Disponibilizar pesquisa brasileira e consulta de ativos US armazenados.
- Preparar serviços reutilizáveis pelo scheduler da T24.
- Manter dados de mercado globais e compartilhados entre contas.

**Non-Goals:**

- Criar scheduler.
- Implementar frequência adaptativa ou controle de quota.
- Executar chamadas externas dentro de transações.
- Criar histórico completo de todas as cotações recebidas.
- Implementar frontend.
- Implementar compra/venda.
- Permitir ao usuário informar ou substituir preços.
- Criar endpoint de atualização manual.
- Alterar os adapters concluídos na T22 sem necessidade de correção.

## Decisions

### 1. `Asset`, `Quote` e `ExchangeRate` serão o catálogo/cache persistido

A T23 utilizará as entidades já existentes em vez de criar novas tabelas paralelas.

A estrutura continuará representando:

```text
Asset
    -> identidade do ativo

Quote
    -> último snapshot válido do ativo

ExchangeRate
    -> último USD/BRL válido