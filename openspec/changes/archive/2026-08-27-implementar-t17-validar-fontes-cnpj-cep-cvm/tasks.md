## 1. Validar BrasilAPI e ViaCEP

- [x] 1.1 Criar a prova técnica de consulta individual de CNPJ pela BrasilAPI e de endereço pelo ViaCEP, identificando e normalizando os campos necessários para CNPJ, razão social, nome fantasia, situação cadastral, CEP e endereço estruturado; verificar com consultas controladas e testes locais que respostas válidas, entradas inválidas, dados ausentes e indisponibilidade podem ser distinguidos sem tornar a suíte normal dependente da internet.

## 2. Validar a fonte oficial da CVM

- [x] 2.1 Criar a prova técnica para obter e processar o dataset oficial `Participantes Intermediários: Informação Cadastral` e seu dicionário, identificando formato, arquivos internos, coluna de CNPJ e informação necessária para determinar a categoria `CTVM`; verificar que o protótipo consegue localizar programaticamente um participante pelo CNPJ sem scraping, navegador ou CAPTCHA.

## 3. Validar cruzamento das fontes

- [x] 3.1 Executar e documentar pelo menos um caso positivo de CNPJ que satisfaça o critério `CTVM` e um caso negativo de CNPJ válido que não o satisfaça, normalizando o CNPJ entre as fontes e criando fixtures mínimas para reprodução offline; verificar que o caso positivo pode ser confirmado pela CVM e que o caso negativo é rejeitado pelo critério regulatório mesmo quando possui cadastro empresarial válido.

## 4. Consolidar evidências e validar a T17

- [x] 4.1 Documentar em `docs/` os endpoints e recursos utilizados, campos consumidos, formatos, casos de erro, limitações, data das verificações e conclusões que orientarão a T18, mantendo código experimental separado da implementação de produção; verificar que a suíte normal roda sem acesso obrigatório à internet, que os smoke tests externos possuem execução explícita e finalizar com `openspec validate implementar-t17-validar-fontes-cnpj-cep-cvm --type change`.
