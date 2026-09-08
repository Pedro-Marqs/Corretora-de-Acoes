## Context

As operações de compra, venda e transferência já persistem posições, movimentações e patrimônio, e a T26 fornece cálculos financeiros puros. A T23/T24 mantém cotações e câmbio com fallback; a nova consulta deve compor esses dados sem criar uma segunda regra financeira ou alterar os registros.

## Goals / Non-Goals

**Goals:**

- Expor `GET /api/dashboard` para a conta autenticada.
- Compor saldo, posições e resultados usando os objetos financeiros da T26.
- Aplicar conversão USD/BRL, arredondamento e metadados de cotação conforme os contratos existentes.
- Isolar a consulta por conta e representar explicitamente estado vazio, fallback e dados desatualizados.

**Non-Goals:**

- Criar distribuições, série histórica ou visão por corretora das T35/T36.
- Alterar compra, venda, transferência, agendamentos ou persistência.
- Calcular indicadores no frontend ou criar pontos patrimoniais durante a consulta.

## Decisions

- **Contrato dedicado de leitura:** usar `GET /api/dashboard`, protegido pela sessão, com uma projeção de resposta própria para indicadores, posições e avisos. Isso evita sobrecarregar o histórico ou expor entidades JPA; criar vários endpoints menores foi rejeitado por aumentar chamadas e permitir leituras inconsistentes.
- **Composição no serviço de dashboard:** carregar somente dados da conta autenticada e delegar médias, custos e resultados ao núcleo financeiro da T26. Recalcular fórmulas no service foi rejeitado porque duplicaria a fonte de verdade; delegar ao frontend foi rejeitado por permitir divergência.
- **Cotação oficial e fallback:** selecionar o snapshot utilizável conforme mercado, usando a cotação brasileira armazenada/atualizada e o câmbio USD/BRL aplicável. A resposta carrega valor, instante e marcador de desatualização; ausência de valor necessário para conversão gera erro funcional em vez de estimativa.
- **Consultas sem efeitos colaterais:** usar repositories/projeções somente de leitura e não registrar movimentação ou patrimônio. Atualizar cache durante a consulta foi rejeitado para preservar transações curtas e o comportamento dos agendamentos.
- **Aportes fora da rentabilidade:** derivar resultados de operações e posições, mantendo saldo e patrimônio separados dos indicadores de rendimento, conforme as regras financeiras existentes.

## Risks / Trade-offs

- **[Risco]** Leituras concorrentes com uma operação podem observar dados em momentos diferentes. → Usar uma transação de leitura consistente e projetar os dados necessários na mesma consulta lógica.
- **[Risco]** Cotações antigas tornam os indicadores menos atuais. → Retornar o último valor válido com instante e aviso, sem ocultar a inconsistência.
- **[Risco]** Consultas agregadas podem ficar pesadas com histórico grande. → Não carregar histórico completo; usar posições, movimentações necessárias ao realizado e projeções/consultas filtradas pela conta.
- **[Risco]** Alterações futuras no núcleo financeiro podem divergir do dashboard. → Cobrir exemplos numéricos do T26 no serviço e manter dependência explícita das mesmas abstrações de cálculo.

## Migration Plan

Não há migração de banco. Implementar o endpoint e seus testes, mantendo a publicação compatível com as rotas existentes; em rollback, remover o controller/serviço do dashboard sem alterar dados persistidos nem operações financeiras.
