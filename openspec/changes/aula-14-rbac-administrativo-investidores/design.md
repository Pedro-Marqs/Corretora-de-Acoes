## Context

`Account` representa uma conta financeira e suas operações já derivam do principal autenticado, inclusive compras e vendas. O change da Aula 13 fornece JWT de investidores; esta mudança adiciona uma segunda identidade, com audience, tipo de principal e configuração próprios, sem reutilizar `Account` para representar operador administrativo.

## Goals / Non-Goals

**Goals:**

- Isolar tabelas, credenciais, principal e endpoints administrativos.
- Implementar somente o administrador `ADMIN` com consulta de indicadores e ativação/desativação controladas; o investidor `USER` permanece em `Account`.
- Entregar consultas agregadas eficientes e monetariamente coerentes.
- Garantir autorização em nível HTTP e método, além de testes de isolamento entre as áreas.
- Auditar ações administrativas sensíveis sem armazenar segredos.

**Non-Goals:**

- Não criar cadastro público, saldo, carteira, posição ou operação financeira para `AdminUser`.
- Não permitir que administrador consulte carteiras ou históricos individuais por padrão.
- Não alterar `Account`, `PurchaseService`, `SaleService`, `TransferService` ou cálculos financeiros para acomodar permissões administrativas.
- Não alterar o workflow do GitHub Actions nem introduzir serviço externo de analytics.

## Decisions

- **`AdminUser` separado de `Account`:** evita que saldo e identidade operacional compartilhem ciclo de vida, DTO ou repositório. Alternativa rejeitada: adicionar `role` em `Account`, porque isso misturaria investidor e operador e ampliaria o risco de acesso cruzado.
- **Duas identidades funcionais:** `Account` representa o investidor `USER` e `AdminUser` representa o administrador `ADMIN`. Não haverá operador administrativo somente leitura nesta versão; essa evolução será planejada separadamente. O papel nunca substitui audience nem tipo de principal.
- **Audience e chaves separadas:** tokens de investidor usam audience de investidor e tokens administrativos usam audience administrativa; configurações e secrets podem ser distintos. Um token válido em uma área não é válido na outra.
- **Autorização em duas camadas:** filtro JWT valida identidade/audience; `@PreAuthorize` ou equivalente valida papel no método; serviços mantêm regras de domínio e não confiam em campos do request.
- **Bootstrap único controlado:** um comando/profile de inicialização aceita credenciais por variáveis externas, cria somente se não houver `AdminUser` e não fica exposto por HTTP. Depois do primeiro bootstrap, a capacidade é desabilitada.
- **Dashboard por projeções agregadas:** consultas SQL/JPA retornarão contagens, somas por moeda, séries e ranking, sem carregar entidades de carteira individual. Períodos usarão o relógio de Brasília e limites explícitos.
- **Auditoria transacional:** ativação/desativação e seu evento de auditoria serão confirmados no mesmo fluxo transacional; falha em qualquer parte não deixa estado administrativo divergente.

## Risks / Trade-offs

- [AdminUser ser usado para acessar API de investidor] → audience, principal type e chaves separados, com testes de token cruzado.
- [Role ADMIN virar atalho para dados financeiros] → endpoints administrativos não chamarão serviços de operação; testes deverão provar saldo/posição/histórico inalterados.
- [Métricas misturarem BRL e USD] → retornar moeda nativa separada e conversão explícita com taxa/instante ou estado indisponível.
- [Desativação administrativa invalidar credencial errada] → revogar somente credenciais da conta alvo e registrar auditoria; nunca revogar por e-mail ou papel sem UUID validado.
- [Bootstrap deixar credencial em log] → aceitar somente secret externo, mascarar entradas e testar ausência em respostas e logs.

## Migration Plan

1. Implementar schema e repositório `AdminUser`/auditoria sem tocar tabelas financeiras.
2. Integrar a infraestrutura JWT da Aula 13 com audience e principal administrativos separados.
3. Criar bootstrap controlado e login administrativo; validar tokens cruzados.
4. Criar serviços e endpoints de gestão e dashboard com projeções agregadas.
5. Criar shell/rotas administrativas no frontend sem compartilhar estado financeiro de investidor.
6. Executar regressões, revisar Graphify e validar reviewer/tester antes de liberar a área administrativa.
