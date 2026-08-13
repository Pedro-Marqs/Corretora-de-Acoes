# Continuidade do projeto

## Finalidade

Este arquivo registra o ponto atual do trabalho para permitir sua continuação em outra máquina. Os documentos de referência são, nesta ordem:

1. `docs/01-visao.md` — escopo e decisões de produto confirmadas;
2. `docs/02-pesquisa.md` — pesquisa e recomendações técnicas;
3. enunciado acadêmico — prevalece quando houver requisito obrigatório.

Em caso de divergência, a decisão mais recente registrada nesta seção deve ser usada e o documento divergente deve ser corrigido antes de avançar.

## Estado atual

- A entrevista de visão foi encerrada.
- A visão consolidada está em `docs/01-visao.md`.
- A pesquisa técnica está em `docs/02-pesquisa.md`.
- A pesquisa foi revisada uma última vez e alinhada às decisões mais recentes.
- Nenhum código da nova versão foi implementado nesta etapa.
- Próxima etapa: consolidar `docs/03-requisitos.md`, sem reabrir perguntas não críticas.

## Decisões funcionais confirmadas

- O sistema é um simulador acadêmico para pessoa física, com um único perfil de usuário.
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
- Ativos internacionais serão exibidos em dólar e em real, com conversão direta e sem taxa cambial.
- A cotação USD/BRL será atualizada diariamente; falhas usam o último valor. Após uma semana, será exibido aviso sem bloquear operações.
- As cotações dos ativos em carteira serão atualizadas automaticamente a cada cinco minutos enquanto o backend estiver ativo e houver internet.
- Pesquisas e confirmações de compra ou venda tentarão obter cotação atual antes de recorrer ao cache.
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
- Banco principal: PostgreSQL.
- Banco para testes automatizados rápidos: H2.
- MySQL não será suportado na primeira versão.
- Persistência: Spring Data JPA/Hibernate; valores exatos com `BigDecimal` e `NUMERIC/DECIMAL`.
- Migrações: Flyway antes da consolidação do esquema.
- Autenticação: Spring Security, sessão opaca em cookie seguro e senhas com bcrypt.
- Cliente HTTP: Spring Cloud OpenFeign.
- Cotações brasileiras: Brapi.
- Cotações norte-americanas: Twelve Data, sujeita a prova técnica de cobertura, latência e limites.
- Cache de cotações: PostgreSQL, sem Redis.
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

## Suposições reversíveis e validações futuras

- A regra de preço médio após venda ainda deverá ser confirmada com o professor; até lá, será mantido o preço médio anterior.
- A fonte específica de USD/BRL continuará atrás de um adapter configurável.
- Twelve Data somente será consolidada após prova técnica com os campos mínimos, dados em tempo real ou quase em tempo real e consumo compatível com consultas agrupadas.
- A obtenção de dados oficiais da CVM deverá evitar automação de páginas protegidas; preferir dados abertos processados pelo backend.
- A compatibilidade de Spring Boot 3.4.0 com as dependências mantidas será validada pelo build e pelos testes antes do desenvolvimento funcional.
- Para dúvidas não críticas futuras, será adotada a alternativa mais simples e registrada como suposição reversível.

## Próximo passo

Atualizar `docs/03-requisitos.md` a partir da visão aprovada e da pesquisa técnica, produzindo requisitos identificáveis, regras de negócio e critérios de aceitação verificáveis. Só fazer nova pergunta se surgir um bloqueio que torne a implementação tecnicamente impossível.
