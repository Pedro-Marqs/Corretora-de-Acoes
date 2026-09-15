## Context

Conforme a `historico-registro-patrimonial`, as movimentações já são eventos imutáveis vinculados à conta e produzidos pelas operações financeiras. A T32 adiciona somente a leitura autenticada desses eventos; a consulta deve respeitar o isolamento da sessão, o limite funcional de 20 itens e os filtros previstos na T32.

## Goals / Non-Goals

**Goals:**

- Definir um contrato estável de consulta paginada para `GET /api/history`.
- Executar filtros e ordenação no backend antes de montar a página.
- Retornar projeções de leitura e metadados sem permitir alteração dos eventos.
- Manter o contrato uniforme de autenticação e validação já existente.

**Non-Goals:**

- Criar tela frontend ou exportação de histórico.
- Alterar, excluir, corrigir ou recalcular movimentações persistidas.
- Criar pontos patrimoniais ou qualquer nova movimentação durante a consulta.
- Introduzir filtros ou tipos de movimento não previstos na especificação.

## Decisions

- **Endpoint e paginação:** usar `GET /api/history`, com `page` baseado em zero e padrão `0`. O tamanho não será controlável pelo cliente: cada página terá no máximo 20 itens. A resposta conterá `content`, `page`, `size`, `totalElements` e `totalPages`. Isso evita que um consumidor contorne o limite; aceitar `size` livre foi considerado e rejeitado.
- **Filtros:** aceitar `from` inclusivo e `to` exclusivo como instantes ISO-8601, tipo de movimentação conforme os tipos persistidos, ticker com comparação normalizada, identificador da corretora e mercado aceito. Os filtros serão combinados por AND; ausência de filtro significa não restringir aquela dimensão. Um intervalo invertido ou parâmetro malformado será erro de validação, não consulta vazia.
- **Ordenação determinística:** ordenar por instante decrescente e usar o identificador persistido como desempate decrescente. Assim, registros com o mesmo instante não mudam de página entre consultas equivalentes.
- **Leitura e isolamento:** a consulta será derivada exclusivamente da conta autenticada e retornará uma projeção/DTO de leitura, sem expor entidade de persistência nem credenciais. O identificador de corretora usado no filtro será validado como parâmetro de consulta, mas nunca ampliará o escopo além da conta da sessão.
- **Metadados e vazio:** uma página válida sem resultados retorna HTTP 200 com `content` vazio e totais zero ou consistentes com a consulta. Página negativa e filtros inválidos usam a categoria de validação do contrato existente.
- **Persistência:** reutilizar a tabela e os registros imutáveis existentes, sem migração destrutiva. A consulta deve filtrar por conta no próprio acesso ao repositório; índices adicionais somente serão criados de forma aditiva se a implementação comprovar que os índices atuais não suportam a combinação conta/instante.

## Risks / Trade-offs

- **[Risco]** Muitos filtros combinados podem gerar consultas mais custosas. → Aplicar conta e intervalo/ordenação na consulta persistente, limitar sempre a página a 20 e validar a consulta com volume superior a uma página.
- **[Risco]** Registros com o mesmo instante podem causar duplicidade ou lacunas entre páginas. → Usar o identificador persistido como segundo critério determinístico de ordenação.
- **[Risco]** Um filtro de corretora de outra conta pode revelar existência por diferenças de resposta. → Restringir a consulta primeiro à conta autenticada e retornar o mesmo formato de lista vazia quando não houver correspondência.
- **[Risco]** Expor campos financeiros inadequados pode vazar detalhes internos. → Definir a projeção apenas com os campos funcionais do histórico e revisar respostas e logs contra o contrato de erro e as regras de privacidade existentes.

## Migration Plan

Não há migração de dados nem mudança destrutiva de esquema. Publicar o endpoint somente após os testes de contrato e isolamento; em caso de rollback, remover/desabilitar a rota sem alterar as movimentações já persistidas.
