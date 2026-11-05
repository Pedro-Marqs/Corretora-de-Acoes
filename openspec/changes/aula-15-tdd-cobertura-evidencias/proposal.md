## Why

A Corretora já possui uma suíte extensa e gates de cobertura, mas a atividade da Aula 15 exige evidência reproduzível de TDD, análise de fronteiras, justificativa de mocks e detecção controlada de mutação. Este change organiza a auditoria e os registros sem reduzir os limites atuais nem alterar o workflow do GitHub Actions.

## What Changes

- Registrar uma linha de base de testes e cobertura antes dos changes de autenticação e RBAC.
- Executar três ciclos reais Red → Green → Refactor em comportamentos de segurança e domínio financeiro.
- Preservar os limites JaCoCo atuais de 80% para linhas e branches e os limites V8 existentes no frontend.
- Criar testes de fronteira para JWT, audience/papel, propriedade de conta, moedas, períodos, paginação e operações financeiras.
- Registrar evidência de cada teste inicialmente falhando e da correção que o faz passar.
- Demonstrar detecção de uma mutação controlada sem introduzir ferramenta adicional sem justificativa.
- Documentar por que cada dependência externa é mockada e por que o sistema sob teste permanece real quando necessário.
- Produzir relatórios antes/depois e registrar comandos, resultados, testes ignorados e riscos.
- Não alterar `.github/workflows/`, limites de cobertura, regras financeiras ou código de produção neste change isoladamente.

## Capabilities

### New Capabilities

- `tdd-cobertura-evidencias`: processo verificável para linha de base, ciclos TDD, mutação, cobertura e documentação de validação.

### Modified Capabilities

- Nenhuma.

## Impact

Afeta a organização dos testes backend/frontend e os documentos de evidência dos changes das Aulas 13 e 14. Pode exigir novos testes, pequenos ajustes de produção descobertos pelos ciclos TDD e relatórios locais; não altera o workflow atual nem reduz gates. A Aula 15 é transversal: o baseline precede a Aula 13, seus ciclos acompanham as implementações e o relatório final depende da conclusão da Aula 14.
