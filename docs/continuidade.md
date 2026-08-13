# Continuidade do projeto

## Finalidade

Este arquivo registra o ponto atual do trabalho para permitir sua continuação em outra máquina. Os documentos de referência são, nesta ordem:

1. `docs/01-visao.md` — escopo e decisões de produto confirmadas;
2. `docs/02-pesquisa.md` — pesquisa e recomendações técnicas;
3. enunciado acadêmico — prevalece quando houver requisito obrigatório.

Em caso de divergência, a decisão mais recente registrada nesta seção deve ser usada e o documento divergente deve ser corrigido antes de avançar.

Este arquivo deve ser atualizado sempre que houver uma decisão relevante, alteração de escopo, conclusão de etapa, criação de documento ou mudança no próximo passo do projeto.

## Estado atual

- A entrevista de visão foi encerrada.
- A visão consolidada está em `docs/01-visao.md`.
- A pesquisa técnica está em `docs/02-pesquisa.md`.
- A pesquisa foi revisada uma última vez e alinhada às decisões mais recentes.
- Nenhum código da nova versão foi implementado nesta etapa.
- Os requisitos foram consolidados em `docs/03-requisitos.md`, e as cinco dúvidas levantadas nessa etapa foram resolvidas.
- Dez especificações funcionais foram criadas em `docs/spec/`, uma por conjunto de funcionalidades.
- O único tipo de usuário é o `Investidor`; os documentos distinguem somente seu estado autenticado ou não autenticado quando o fluxo exigir.
- A arquitetura foi proposta em `docs/04-arquitetura.md`, sem implementação da aplicação.
- Próxima etapa: revisar/aprovar requisitos, especificações e arquitetura; depois produzir `docs/05-specs.md` ou avançar para o detalhamento das tarefas, conforme a organização documental escolhida.

## Decisões funcionais confirmadas

- O sistema é um simulador acadêmico para pessoa física, com um único tipo de usuário: `Investidor`. Cadastro, login e reativação são realizados pelo investidor ainda não autenticado; as demais funções exigem autenticação.
- Cada usuário terá conta individual, sem vínculo ou compartilhamento com outras contas.
- Cadastro: nome, CPF, e-mail e senha. Não haverá confirmação de e-mail na primeira versão.
- O saldo inicial será de R$ 10.000,00 e pertence à conta, sendo compartilhado por todas as corretoras cadastradas.
- As posições em ativos pertencem a uma corretora específica.
- A corretora será pesquisada somente por CNPJ e aceita apenas se constar na CVM como `CTVM`.
- O mesmo CNPJ não poderá ser cadastrado duas vezes na mesma conta.
- Compra e venda serão simulações instantâneas a preço de mercado, sem livro de ordens ou preço definido pelo usuário.
- Entradas inválidas, saldo insuficiente, venda ou transferência acima da posição serão rejeitados sem alteração parcial dos dados.
- Transferências entre corretoras preservam quantidade, custo e histórico; não alteram saldo e não têm taxa ou liquidação.
- O histórico é imutável e registra somente movimentações concluídas com sucesso.
- O histórico terá 20 registros por página.
- Pontos patrimoniais serão registrados somente após saldo inicial, aporte, compra, venda e transferência; atualizações isoladas de cotação não criarão pontos.
- Ativos internacionais serão exibidos em dólar e em real, com conversão direta e sem taxa cambial.
- A cotação USD/BRL será consultada diariamente na AwesomeAPI por HTTP REST; falhas usam o último valor. Após uma semana, será exibido aviso sem bloquear operações.
- As cotações brasileiras em carteira serão atualizadas a cada cinco minutos; as norte-americanas, uma vez ao dia pela Twelve Data, enquanto backend e internet estiverem disponíveis.
- Pesquisas e confirmações de compra ou venda brasileiras tentarão obter cotação atual antes do cache; operações norte-americanas usarão a cotação diária armazenada.
- Compras adicionais recalculam o preço médio por média ponderada; vendas parciais mantêm o preço médio unitário restante.
- Valores externos serão arredondados para duas casas por `HALF_UP`.
- Falhas de API usam a última cotação armazenada; sem cotação armazenada, a operação será bloqueada.
- O dashboard terá saldo, posição, preço médio, lucro/prejuízo, gráficos históricos, distribuições por ação, corretora e mercado, e filtros de 4 semanas, 3 meses, 6 meses, 1 ano, 5 anos e máximo.
- A interface React será responsiva e inspirada visualmente em Investidor10 e Rico, sem copiar identidade visual.
- Datas e horas usarão o horário de Brasília; valores monetários terão duas casas decimais.
- A primeira versão rodará somente localmente, mas precisará de internet para consultar serviços externos.

## Decisões técnicas confirmadas

- Base do projeto: `https://github.com/Os-Tops/Corretora-Acoes-Apiv2`, branch `dev` considerada na pesquisa.
- Frontend: React com JavaScript e Vite.
- Backend: Java 17, Spring Boot 3.4.0 como ponto de partida e Maven Wrapper.
- Arquitetura: monólito em camadas; portas/adapters apenas nas integrações externas.
- Estrutura interna semelhante ao repositório-base, usando os pacotes `api`, `config`, `domain`, `infra`, `repository`, `service` e `scheduler`; o React permanece diretamente em `src/main/front` com `components`, `pages`, `services` e `styles`.
- Um único processo Spring Boot e um único PostgreSQL; sem microsserviços, Redis, mensageria ou gateway.
- Banco principal: PostgreSQL.
- Banco para testes automatizados rápidos: H2.
- MySQL não será suportado na primeira versão.
- Persistência: Spring Data JPA/Hibernate; valores exatos com `BigDecimal` e `NUMERIC/DECIMAL`.
- Migrações: Flyway antes da consolidação do esquema.
- Autenticação: Spring Security, sessão opaca em cookie seguro e senhas com bcrypt.
- Cliente HTTP: Spring Cloud OpenFeign.
- Cotações brasileiras: Brapi.
- Cotações norte-americanas: Twelve Data, uma vez ao dia, sujeita a prova técnica de cobertura e campos.
- USD/BRL: AwesomeAPI por HTTP REST, uma vez ao dia.
- As atualizações diárias de ativos norte-americanos e USD/BRL ocorrerão às 10h no horário de Brasília.
- Cache de cotações: PostgreSQL, sem Redis.
- Movimentações financeiras serão transações curtas e atômicas; chamadas externas ocorrerão antes da transação, seguidas de revalidação do estado.
- Controllers tratarão apenas HTTP e delegarão regras aos casos de uso e ao domínio.
- O frontend não será fonte de verdade para preços, saldo, preço médio ou resultados.
- Dependências herdadas sem uso serão removidas, em especial Thymeleaf e Alpha Vantage.
- Testes: JUnit/Mockito, Spring Boot Test, H2 para testes rápidos, PostgreSQL/Testcontainers nos fluxos críticos e mocks para APIs externas.

## Fora do escopo da primeira versão

- Hospedagem ou implantação em nuvem.
- Recuperação de senha e qualquer envio de e-mail.
- Verificação de e-mail no cadastro.
- Importação de carteira real, Open Finance, integração com B3 ou execução de ordens reais.
- Livro de ofertas, ordens limitadas, taxas, impostos, dividendos, desdobramentos e grupamentos.
- Relatório ou exportação para Excel.
- Resumo prévio da operação; haverá apenas confirmação simples.
- Pesquisa de corretora por nome e validação adicional pelo Banco Central.
- Revalidação periódica da situação da corretora após o cadastro.
- Identificação do tipo do ativo; bastam ticker, nome, mercado, moeda e cotação.
- Atualização manual solicitada pelo usuário.
- Comprovação de identidade adicional para reativação de conta; na primeira versão, a reativação não a exigirá.

## Validações técnicas futuras

- Validar na Twelve Data os campos mínimos e a cobertura necessária antes da integração completa.
- Priorizar dados abertos da CVM processáveis pelo backend e validar a fonte/formato antes do cadastro completo de corretora, evitando automação de páginas ou CAPTCHA.
- A compatibilidade de Spring Boot 3.4.0 com as dependências mantidas será validada pelo build e pelos testes antes do desenvolvimento funcional.
- Para dúvidas não críticas futuras, será adotada a alternativa mais simples e registrada como suposição reversível.

## Próximo passo

Revisar e aprovar `docs/03-requisitos.md`, os arquivos de `docs/spec/` e `docs/04-arquitetura.md`. Depois, consolidar o índice de especificações em `docs/05-specs.md` ou avançar para `docs/06-tarefas.md`, sem iniciar a aplicação antes da aprovação. Só fazer nova pergunta se surgir um bloqueio que torne a implementação tecnicamente impossível.
