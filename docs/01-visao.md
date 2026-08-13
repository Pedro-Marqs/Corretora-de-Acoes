# Visão do projeto

## 1. Visão geral

O projeto será uma plataforma web de simulação de investimentos para pessoas físicas. Cada usuário poderá administrar um saldo fictício, cadastrar corretoras validadas, simular operações com ativos dos mercados brasileiro e norte-americano e acompanhar a evolução da própria carteira.

As operações serão apenas simulações. O sistema não enviará ordens às bolsas, não movimentará dinheiro real e não terá vínculo com bancos ou contas reais de corretoras.

## 2. Problema

Investidores que utilizam mais de uma corretora precisam reunir posições, movimentações e resultados para obter uma visão consolidada de seus investimentos.

O sistema pretende representar esse cenário em ambiente acadêmico, centralizando:

- Corretoras associadas ao usuário;
- Saldo simulado;
- Posições separadas por corretora;
- Compras, vendas, aportes e transferências;
- Cotações de ativos brasileiros e norte-americanos;
- Histórico e indicadores da carteira.

## 3. Público-alvo

Pessoas físicas interessadas em simular investimentos nos mercados brasileiro e norte-americano.

Haverá apenas um tipo de usuário: o investidor. Não existirão perfis administrativos ou diferenças de permissão entre investidores.

## 4. Objetivo

Permitir que cada usuário simule e acompanhe seus investimentos em diferentes corretoras por meio de uma conta individual, mantendo saldo, posições, movimentações, resultados e dashboards consolidados.

## 5. Informações confirmadas

### 5.1. Conta e acesso

- O usuário deverá criar uma conta antes de utilizar o sistema.
- O cadastro exigirá nome, CPF, e-mail e senha.
- O CPF será validado pelo formato e pelos dígitos verificadores.
- O e-mail será validado pelo formato, mas não precisará ser confirmado para liberar o acesso.
- O login será realizado com e-mail e senha.
- A senha deverá ter no mínimo oito caracteres, incluindo letra minúscula, letra maiúscula, número e caractere especial.
- As senhas serão armazenadas de forma protegida e nunca poderão ser recuperadas ou exibidas em texto legível.
- A recuperação de senha não fará parte da primeira versão.
- O usuário poderá alterar e-mail e senha após informar a senha atual.
- Nome e CPF não poderão ser alterados.
- Alterações de e-mail ou senha encerrarão todas as sessões abertas.
- O usuário poderá realizar logout; não haverá expiração automática por inatividade na primeira versão.
- CPF e e-mail serão parcialmente ocultados nas telas.
- Cada conta será isolada. Um usuário nunca poderá acessar dados pertencentes a outro, mesmo conhecendo os identificadores dos registros.

### 5.2. Exclusão e reativação da conta

- A exclusão será lógica e manterá os dados no banco.
- Para excluir a conta, o usuário deverá confirmar e-mail e senha e escrever a palavra `Excluir`.
- Uma conta inativa poderá ser reativada, restaurando saldo, carteira, corretoras e histórico.
- Se o CPF estiver associado a uma conta inativa, o usuário poderá reativá-la ou criar uma nova conta.
- Ao criar uma nova conta, a anterior será marcada como excluída e permanecerá inacessível, embora seus dados sejam preservados.

### 5.3. Saldo

- Cada nova conta começará com R$ 10.000,00.
- O saldo pertencerá ao usuário e será compartilhado por todas as corretoras cadastradas na conta.
- O usuário poderá adicionar saldo por meio de aportes fictícios.
- Cada aporte terá valor mínimo de R$ 10,00 e não terá valor máximo.
- O aporte aumentará o saldo e o patrimônio, mas não será considerado lucro ou valorização.

### 5.4. Corretoras

- A busca de corretoras será realizada somente por CNPJ.
- Uma corretora somente poderá ser cadastrada se possuir CNPJ ativo e constar na CVM com a categoria `CTVM` (Corretora de Títulos e Valores Mobiliários).
- O sistema consultará dados cadastrais, endereço e registro na CVM em fontes externas.
- Serão armazenados CNPJ, razão social, nome fantasia, situação cadastral, autorização e endereço estruturado.
- E-mail e telefone da corretora não serão armazenados.
- A mesma corretora não poderá ser cadastrada duas vezes na mesma conta.
- Uma corretora somente poderá ser removida se não possuir posições vinculadas.
- A remoção será lógica e preservará seu histórico.
- Uma corretora removida poderá ser cadastrada novamente.

### 5.5. Ativos e posições

- Serão aceitos ativos fornecidos pelas APIs adotadas que pertençam aos mercados brasileiro ou norte-americano.
- Criptomoedas, moedas e ativos de outros mercados serão rejeitados.
- O ativo deverá possuir nome, ticker, cotação, moeda e mercado.
- A interface exibirá ticker, nome e mercado, sem precisar classificar o tipo do ativo.
- Todas as operações utilizarão quantidades inteiras.
- As posições serão vinculadas diretamente às corretoras.
- O mesmo ativo poderá existir em mais de uma corretora, mantendo posições independentes.
- Uma posição zerada deixará de aparecer na carteira, mas continuará no histórico.
- Se uma posição zerada for comprada novamente, seu preço médio começará novamente do zero.

### 5.6. Compra e venda

- Compras e vendas serão simulações instantâneas a preço de mercado.
- As operações poderão ocorrer em qualquer dia e horário.
- Não haverá ordens pendentes, corretagem, impostos ou outras taxas.
- Não será possível comprar acima do saldo disponível.
- Não será possível vender mais unidades do que a quantidade disponível na corretora escolhida.
- A primeira versão não terá uma etapa de resumo prévio da operação.
- O valor recebido em uma venda será acrescentado imediatamente ao saldo.
- O lucro ou prejuízo realizado será calculado pela diferença entre o preço de venda e o preço médio, multiplicada pela quantidade vendida.
- Compras adicionais do mesmo ativo na mesma corretora recalcularão o preço médio por média ponderada.
- Uma venda parcial manterá o preço médio unitário da posição restante e reduzirá proporcionalmente seu custo total.
- Uma operação concluída não poderá ser editada, cancelada ou estornada.

### 5.7. Ações norte-americanas e câmbio

- Ativos norte-americanos serão apresentados em dólares e em reais.
- A compra será debitada diretamente do saldo em reais, usando conversão USD/BRL sem taxa cambial.
- A cotação cambial USD/BRL será obtida da AwesomeAPI por HTTP REST e atualizada diariamente.
- Se a atualização falhar, será utilizada a última cotação cambial armazenada.
- Câmbio armazenado há mais de uma semana exibirá um aviso, sem bloquear a operação.
- A cotação cambial utilizada não precisará constar no histórico.

### 5.8. Cotações

- As cotações dos ativos brasileiros presentes nas carteiras serão atualizadas automaticamente a cada cinco minutos enquanto o backend estiver em execução, atendendo ao requisito acadêmico de atualização em tempo real ou quase em tempo real.
- As cotações dos ativos norte-americanos presentes nas carteiras serão atualizadas uma vez ao dia pela Twelve Data.
- A pesquisa e a confirmação de compra ou venda de ativo brasileiro sempre tentarão obter uma cotação atualizada. Para ativo norte-americano, será usada a cotação obtida no ciclo diário ou a última cotação armazenada.
- O sistema deverá agrupar consultas e respeitar os limites dos provedores externos; o horário da cotação será exibido ao usuário.
- Se a API falhar ou atingir o limite de requisições, será utilizada a última cotação armazenada.
- Cotações de ativos com mais de um dia exibirão um aviso, mas continuarão disponíveis para operações.
- Se ainda não existir uma cotação armazenada, a operação dependente dela será bloqueada.
- O usuário não poderá solicitar uma atualização manual.
- Se um ativo da carteira deixar de ser retornado pela API, ele ainda poderá ser vendido pela última cotação armazenada.

### 5.9. Persistência

- O PostgreSQL será o banco de dados principal da aplicação.
- O H2 será usado somente em testes automatizados rápidos.
- O MySQL não será suportado na primeira versão.

### 5.10. Transferências

- O usuário poderá transferir total ou parcialmente uma posição entre suas corretoras.
- Não será possível transferir mais unidades do que a quantidade disponível na origem.
- Não haverá taxa, prazo de liquidação ou restrição de horário.
- A transferência não alterará o saldo.
- Quantidade, custo e histórico serão preservados.
- Se o destino já possuir o mesmo ativo, seu preço médio será recalculado por média ponderada.
- O histórico identificará as corretoras de origem e destino.

### 5.11. Histórico

- O histórico registrará somente movimentações concluídas com sucesso.
- O usuário não poderá editar ou excluir registros históricos.
- Serão registrados saldo inicial, aporte, compra, venda e transferência.
- Cada registro conterá os dados aplicáveis, incluindo tipo, ticker, cotação, quantidade, valor total, moeda, corretora, origem e destino, data/hora e saldo restante.
- O histórico exibirá apenas o ticker, sem necessidade de mostrar o nome do ativo.
- Haverá filtros por período, tipo de movimentação, ativo, corretora e mercado.
- As listagens serão paginadas e ordenadas pelos registros mais recentes por padrão.

### 5.12. Dashboards

- Haverá um dashboard geral e um dashboard específico para a corretora selecionada.
- Serão apresentados saldo, patrimônio, posição, preço médio, lucro/prejuízo realizado, valorização não realizada e resultado total.
- Haverá distribuições por ativo, corretora e mercado.
- O gráfico histórico mostrará a evolução do patrimônio.
- Os períodos disponíveis serão: quatro semanas, três meses, seis meses, um ano, cinco anos e máximo.
- O período máximo abrangerá todo o histórico desde a criação da conta.
- Quando não houver dados para todo o período selecionado, será exibido apenas o intervalo que possuir dados.

### 5.13. Interface, integridade e erros

- A interface será desenvolvida em React e será responsiva.
- A primeira versão será executada somente no ambiente local, sem implantação em serviços de hospedagem.
- Apesar da execução local, será obrigatória uma conexão com a internet para consultar CNPJ, CEP, CVM, câmbio e cotações.
- A organização visual será inspirada em plataformas como Investidor10 e Rico, sem copiá-las.
- Valores monetários serão calculados e exibidos com duas casas decimais.
- Datas e horários usarão o horário de Brasília.
- A primeira versão executará aporte, compra, venda e transferência sem uma etapa separada de resumo prévio.
- Cada movimentação será atômica: se qualquer etapa falhar, saldo, posições e histórico permanecerão como estavam antes da tentativa.
- O usuário receberá uma mensagem explicativa, sem códigos técnicos.
- Erros de saldo ou quantidade informarão também os valores disponíveis.

## 6. Decisões ainda pendentes

Não há decisões funcionais pendentes neste momento. A fonte de dados abertos da CVM e a cobertura das APIs deverão ser validadas tecnicamente antes da implementação das respectivas integrações.

## 7. Suposições reversíveis

Para as dúvidas não críticas, serão adotadas inicialmente as alternativas mais simples:

- A integração de ações norte-americanas utilizará Twelve Data, com atualização uma vez ao dia e provedor isolado para permitir substituição futura.
- A integração USD/BRL utilizará a AwesomeAPI por HTTP REST e permanecerá isolada para permitir substituição futura.
- Dados válidos já armazenados não serão apagados quando uma API retornar uma resposta incompleta.
- Nome e endereço de uma corretora serão atualizados quando ela for consultada novamente, sem uma verificação periódica de todas as corretoras.
- Valores externos com mais de duas casas decimais serão arredondados para duas casas pelo modo `HALF_UP`: terceira casa de 0 a 4 arredonda para baixo e de 5 a 9 arredonda para cima.

## 8. Funcionalidades fora do escopo

- Negociação real de ativos;
- Vínculo com bancos ou contas reais de corretoras;
- Aplicativo mobile nativo;
- Perfis administrativos ou outros tipos de usuário;
- Ordens limitadas, ordens pendentes e livro de ofertas;
- Compra ou venda de quantidades fracionárias;
- Corretagem, impostos e outras taxas;
- Dividendos, juros sobre capital próprio e outros proventos;
- Desdobramentos, agrupamentos e outros eventos corporativos;
- Criptomoedas, moedas e mercados diferentes de Brasil/B3 e Estados Unidos;
- Exportação de relatórios em Excel;
- Notificações ou e-mails sobre movimentações;
- Recuperação de senha e qualquer envio de e-mail;
- Atualização manual de cotações;
- Edição, exclusão ou estorno de movimentações concluídas;
- Expiração automática da sessão por inatividade;
- Bloqueio temporário por tentativas incorretas de login;
- Tratamento específico de operações simultâneas da mesma conta em dispositivos diferentes na primeira versão.
- Implantação em nuvem ou publicação na internet na primeira versão.

## 9. Restrições acadêmicas

O projeto deverá respeitar o enunciado registrado em `docs/01,1-trabalho.md`, incluindo:

- Backend em Java com Spring Boot;
- API REST com respostas JSON;
- Arquitetura em camadas;
- Persistência em banco de dados relacional;
- Tratamento centralizado de erros;
- Integração com pelo menos três serviços externos reais;
- Validação de CNPJ, CEP, autorização da corretora e existência dos ativos;
- Documentação das APIs utilizadas e de suas limitações;
- Documentação dos endpoints;
- Testes e demonstração prática do sistema.
