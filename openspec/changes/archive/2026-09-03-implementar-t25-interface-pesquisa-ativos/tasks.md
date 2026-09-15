## 1. Integrar o contrato de mercado

- [x] 1.1 Criar o serviço frontend de mercado sobre o cliente HTTP comum, confirmar no contrato da T23 a rota e os campos de resposta, encaminhar o ticker e normalizar somente erros de transporte/API; verificar com testes que a requisição usa a sessão existente e não envia preço, câmbio ou comando de atualização manual.

## 2. Criar a tela privada de pesquisa

- [x] 2.1 Adicionar a rota privada e a entrada correspondente na navegação do layout, verificando que usuário sem sessão segue o fluxo existente de autenticação e que nenhuma tela pública renderiza dados de ativos.
- [x] 2.2 Implementar o formulário de pesquisa exclusiva por ticker e o resultado com ticker, nome, mercado, moeda, cotação e horário; verificar resultados brasileiros e o bloqueio de reenvio enquanto a consulta estiver em andamento.
- [x] 2.3 Implementar a apresentação de ativos norte-americanos com valores em USD e BRL, exibindo os instantes retornados pelo backend e verificando que a interface não recalcula nem altera os valores oficiais.

## 3. Tratar estados e desatualização

- [x] 3.1 Integrar os estados compartilhados de carregamento, vazio, sucesso, erro funcional e indisponibilidade, preservando o ticker para retry e distinguindo ausência de resultado de falha; verificar ativo inválido, mercado rejeitado, resposta incompleta, ausência de cache e falha de conexão sem exposição de detalhes técnicos.
- [x] 3.2 Renderizar avisos independentes para cotação acima de 24 horas e USD/BRL acima de sete dias, mantendo os valores e horários originais; verificar os limites de idade e a ausência de qualquer controle de atualização manual.

## 4. Validar interface e regressões

- [x] 4.1 Criar testes de componentes/integração simulada cobrindo resultados BR e US, conversão apresentada, estados de carregamento e vazio, erros, avisos de desatualização, sessão inválida e proteção contra submissão duplicada.
- [x] 4.2 Ajustar a composição responsiva e acessível da tela para desktop, tablet e celular, verificando valores monetários com duas casas, ausência de rolagem horizontal e controles utilizáveis em viewport de 320 px; executar a suíte frontend, lint e build Vite sem regressões nas telas existentes.
