## Context

O backend já centraliza saldo, histórico, patrimônio, cotações e regras financeiras, e o frontend já possui shell privado responsivo e contratos de dashboard. O change atravessa esses contratos, persistência e o sistema visual; ver `proposal.md` e as delta specs para o comportamento completo.

## Goals / Non-Goals

**Goals:**
- Introduzir retirada como movimentação financeira atômica e suportar seus timestamps.
- Separar preço inicial da cotação de preço confirmado editável, mantendo validação no backend.
- Reorganizar a experiência privada e aplicar um sistema visual azul-marinho responsivo.
- Usar componentes de gráfico acessíveis, com valores e estados textuais equivalentes.

**Non-Goals:**
- Não integrar banco, gateway de pagamento ou retirada real.
- Não copiar código, marca ou conteúdo dos sites de referência.
- Não permitir que o frontend seja fonte de verdade para saldo, patrimônio ou resultados.

## Decisions

- **Retirada como evento financeiro existente:** usar o mesmo caminho transacional de aporte/operação, adicionando tipo e validações; isso preserva atomicidade, histórico imutável e ponto patrimonial. Uma migração deverá ajustar enumerações/constraints sem reescrever dados.
- **Preço e data como entrada validada:** o formulário inicia com cotação e relógio de Brasília, mas envia os valores editados; o backend valida moeda, positividade, formato e futuro e grava os valores efetivos. Alternativa rejeitada: manter preço somente leitura, pois contradiz a solicitação.
- **Cotação antiga como fechamento:** o estado de desatualização continua permitindo fallback, mas a camada de apresentação usará o rótulo “preço de fechamento” para evitar sugerir que o valor é intradiário atual.
- **Gráficos progressivos:** preferir componentes existentes ou uma dependência pequena e acessível; cada gráfico terá tabela/resumo textual ou valores legíveis como fallback. Não serão criadas séries sintéticas.
- **Shell e CSS tokens:** centralizar cores, superfícies, espaçamento, overflow e breakpoints em tokens compartilhados; corrigir causa de largura excedente em vez de ocultar o problema com rolagem horizontal global.
- **Corretora revalidada no comando:** a associação deve consultar a situação cadastral ativa no momento da confirmação; indisponibilidade externa impede conclusão, não equivale a inatividade.

## Risks / Trade-offs

- [Alteração de preço pode divergir da cotação] → validar positividade/moeda no backend, registrar preço efetivo e exibir confirmação clara.
- [Data retroativa pode afetar ordenação e série patrimonial] → usar o mesmo instante efetivo em histórico/ponto e rejeitar datas futuras.
- [Migração de tipos de movimentação pode quebrar dados antigos] → manter valores legados, migrar apenas schema/enum e cobrir leitura regressiva.
- [Reformulação visual pode causar regressões de overflow] → validar 320 px, tablet e desktop com testes de layout e arraste horizontal.
- [Gráficos podem perder informação em celular ou acessibilidade] → manter rótulos, valores textuais, foco e estados vazios fora do canvas.

## Migration Plan

1. Adicionar migração compatível para retirada, timestamps e eventuais constraints.
2. Publicar backend aceitando os novos campos sem remover os contratos existentes; validar operações e rollback.
3. Publicar frontend com telas de retirada, negociação, dashboard e shell reformulados.
4. Em rollback, reverter frontend para os fluxos anteriores e preservar registros novos; só reverter schema se nenhum registro da nova versão existir.
