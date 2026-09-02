## 1. Agendamentos

- [x] 1.1 Criar os pontos de entrada agendados para cotacoes brasileiras a cada cinco minutos e para cotacoes norte-americanas e USD/BRL as 10h de Brasilia; verificar as expressoes com testes usando relogio controlado
- [x] 1.2 Implementar a selecao de ativos ativos que possuam posicao e a atualizacao delegada aos servicos de cotacao/cambio; verificar que ativos sem posicao nao sao consultados
- [x] 1.3 Impedir sobreposicao do mesmo ciclo e execucao diaria duplicada; verificar concorrencia e no maximo uma execucao por dia em testes

## 2. Falhas E Historico

- [x] 2.1 Preservar o cache valido e sinalizar dado desatualizado quando a fonte externa falhar; verificar o valor e o estado anteriores em testes
- [x] 2.2 Garantir que atualizacoes automaticas nao criem movimentacoes nem pontos patrimoniais; verificar que as contagens e dados historicos permanecem inalterados
- [x] 2.3 Executar os testes focados de scheduler e mercado, compilacao com `mvnw -q -Dmaven.compiler.useIncrementalCompilation=false -DskipTests compile` e revisar o diff antes de marcar o change concluido; verificacao comprovada: compilacao, suite backend completa, testes focados e `git diff --check` passaram. A revisao externa posterior aprovou a implementacao sem achados criticos, altos ou medios.

## 3. Consolidacao documental da prova T21

- [x] 3.1 Consolidar em `docs/` a matriz tecnica de Brapi, Twelve Data e AwesomeAPI com endpoints, autenticacao, campos, timestamps, mercados, limites, data da verificacao, consumo projetado, falhas, fixtures e conclusao de prontidao, usando somente as evidencias produzidas pela T21 e T22; registrar que a frequencia brasileira futura deve ser controlada dinamicamente por quota e recencia, manter smoke tests externos desabilitados por padrao e verificar que a suite normal executa sem internet ou credenciais, sem implementar codigo de aplicacao e sem marcar tarefas de codigo como concluidas
