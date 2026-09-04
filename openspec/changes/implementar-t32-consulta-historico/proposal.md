## Why

O histórico imutável já é produzido pelas operações, mas ainda não pode ser consultado de forma controlada pela conta autenticada. A T32 completa esse contrato antes da interface do histórico, permitindo localizar movimentações sem expor dados de outra conta nem retornar páginas maiores que o limite funcional.

## What Changes

- Expor consulta autenticada e somente leitura do histórico da própria conta.
- Ordenar movimentações da mais recente para a mais antiga e limitar cada página a 20 registros.
- Aceitar filtros combináveis por intervalo, tipo, ticker, corretora e mercado.
- Retornar registros aplicáveis e metadados de paginação de forma estável.
- Manter histórico imutável, sem endpoints de edição ou exclusão.

## Capabilities

### New Capabilities

### Modified Capabilities

- `historico-registro-patrimonial`: definir a consulta paginada, filtrável e somente leitura do histórico próprio.

## Impact

- Backend: controller, service, repository e projeções de movimentação.
- API: novo contrato de consulta autenticada do histórico, incluindo filtros e metadados de paginação.
- Persistência: consultas indexadas sobre movimentações existentes; nenhuma alteração deve permitir editar ou apagar registros.
- Testes: cobertura de paginação, filtros, ordenação, isolamento entre contas e imutabilidade.
