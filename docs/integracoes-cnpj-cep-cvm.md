# Prova técnica — fontes de CNPJ, CEP e CVM

## Escopo e data

Verificação executada em **27 de agosto de 2026** para orientar a T18. O código da prova está somente em `src/test/java/com/projeto/gestao/infra/client/probe`; não é adapter de produção, não persiste dados e não participa da API da aplicação.

As consultas ao vivo são opt-in. A suíte normal utiliza fixtures mínimas em `src/test/resources/t17` e não depende da internet.

## BrasilAPI — cadastro empresarial

- Finalidade: consulta pontual de dados cadastrais pelo CNPJ.
- Contrato oficial: `https://brasilapi.com.br/docs`.
- Endpoint observado: `GET https://brasilapi.com.br/api/cnpj/v1/{cnpj}`.
- Formato: JSON UTF-8.
- Campos consumidos: `cnpj`, `razao_social`, `nome_fantasia`, `descricao_situacao_cadastral` e `cep`.
- Campos de endereço disponíveis para comparação: `logradouro`, `numero`, `complemento`, `bairro`, `municipio` e `uf`.

Comportamentos:

- HTTP 200: resposta cadastral; os campos obrigatórios ainda precisam ser verificados.
- HTTP 400: CNPJ inválido ou malformado.
- HTTP 404: CNPJ não encontrado.
- Outros erros HTTP, timeout ou falha de conexão: indisponibilidade, diferente de ausência cadastral.
- JSON inválido ou campo obrigatório vazio: resposta incompleta, não sucesso.

A BrasilAPI aceita CNPJ com ou sem máscara. O cruzamento usa uma representação canônica de 14 caracteres, sem pontuação e em maiúsculas para acomodar o contrato alfanumérico documentado. A API de corretoras da BrasilAPI não é usada como autoridade regulatória.

## ViaCEP — endereço estruturado

- Contrato oficial: `https://viacep.com.br/`.
- Endpoint: `GET https://viacep.com.br/ws/{cep}/json/`.
- Entrada: exatamente oito dígitos após normalização.
- Campos consumidos: `cep`, `logradouro`, `complemento`, `bairro`, `localidade` e `uf`.

Comportamentos documentados e reproduzidos:

- CEP válido e encontrado: HTTP 200 com endereço.
- Formato inválido: HTTP 400.
- CEP bem formado mas inexistente: HTTP 200 com `erro: true`.
- Timeout, falha de conexão ou outro erro HTTP: indisponibilidade.
- Campo estrutural obrigatório ausente: resposta incompleta.

O ViaCEP alerta que consultas massivas podem causar bloqueio. A aplicação deve fazer apenas consultas pontuais; não se deve usá-lo para validar uma base inteira.

## CVM — autoridade regulatória

- Conjunto oficial: `https://dados.cvm.gov.br/dataset/intermed-cad`.
- Dados: `https://dados.cvm.gov.br/dados/INTERMED/CAD/DADOS/cad_intermed.zip`.
- Dicionário: `https://dados.cvm.gov.br/dados/INTERMED/CAD/META/meta_cad_intermed.zip`.
- Periodicidade publicada: diária, com informações referentes ao último dia útil.
- Licença publicada: ODbL.

Observação de 27/08/2026:

- `cad_intermed.zip`: 337.249 bytes no download da prova.
- Entradas: `cad_intermed.csv` (cadastro básico) e `cad_intermed_resp.csv` (responsáveis).
- `meta_cad_intermed.zip`: `meta_cad_intermed.txt` e `meta_cad_intermed_resp.txt`.
- Formato cadastral: CSV separado por `;`, texto Windows-1252.
- Colunas mínimas: `TP_PARTIC`, `CNPJ`, `DENOM_SOCIAL`, `DENOM_COMERC`, `SIT`, `CD_CVM` e `CEP`.
- O dicionário define `TP_PARTIC` como “Tipo de participante”, `CNPJ` como cadastro da pessoa jurídica e `SIT` como situação.

Regra comprovada para a prova:

1. normalizar o CNPJ;
2. localizar todas as linhas do CNPJ em `cad_intermed.csv`;
3. exigir uma linha com `TP_PARTIC = CORRETORAS`;
4. exigir nessa linha `SIT = EM FUNCIONAMENTO NORMAL`.

O texto `CTVM` aparece na denominação de várias instituições, mas a decisão não depende do nome empresarial: usa a categoria oficial `CORRETORAS` e a situação oficial. Um mesmo CNPJ pode possuir várias linhas e categorias, portanto a busca não pode assumir unicidade por CNPJ.

## Casos auditáveis

### Positivo — XP Investimentos

- CNPJ normalizado: `02332886000104`.
- BrasilAPI: empresa `ATIVA`; razão social observada `XP INVESTIMENTOS CORRETORA DE CAMBIO, TITULOS E VALORES MOBILIARIOS S/A`; CEP `22250911`.
- ViaCEP: `Praia Botafogo`, `Botafogo`, `Rio de Janeiro/RJ`.
- CVM: três registros para o CNPJ no snapshot observado; um deles tem `TP_PARTIC = CORRETORAS`, `SIT = EM FUNCIONAMENTO NORMAL` e `CD_CVM = 3247`.
- Resultado: satisfaz o critério CTVM da prova.

### Negativo — Magazine Luiza

- CNPJ normalizado: `47960950000121`.
- BrasilAPI: empresa `ATIVA`; razão social `MAGAZINE LUIZA S/A`; CEP `14400490`.
- ViaCEP: `Rua Voluntários da Franca`, `Centro`, `Franca/SP`.
- CVM: nenhuma linha para o CNPJ no snapshot observado.
- Resultado: cadastro empresarial válido e ativo não é suficiente; o critério regulatório rejeita o caso.

## Execução reproduzível

Suíte offline focada:

```powershell
.\mvnw.cmd -Dtest=CnpjCepProbeTests,CvmDatasetProbeTests,RegistryCrossCheckProbeTests test
```

Smoke test ao vivo, executado apenas por opção explícita:

```powershell
$env:T17_EXTERNAL_SMOKE = 'true'
.\mvnw.cmd -Dtest=T17ExternalSmokeTests test
Remove-Item Env:T17_EXTERNAL_SMOKE
```

Sem `T17_EXTERNAL_SMOKE=true`, o teste externo é ignorado antes de criar o cliente HTTP. Mudanças futuras dos casos, endpoints ou arquivos não tornam a suíte normal instável; exigem nova execução documentada da prova.

## Limitações e decisões deixadas para a T18

- Os serviços não oferecem SLA assumido pelo projeto; timeout, retry e circuit breaker ainda serão definidos.
- A T17 não escolhe cache, download por consulta ou atualização agendada do dataset CVM.
- O parser é propositalmente mínimo e experimental. A T18 deve definir DTOs, portas, validação e observabilidade definitivos.
- CEPs no CSV da CVM podem perder zero à esquerda; a prova preenche até oito dígitos somente para normalização.
- Nome fantasia e complemento podem estar vazios; razão social, situação, CNPJ e campos estruturais usados pelo fluxo devem ser validados explicitamente.
- Divergência de endereço entre fontes deve ser observável; esta prova não define precedência de persistência.
- A CVM é a autoridade para categoria/situação regulatória; BrasilAPI é fonte cadastral e ViaCEP é fonte de endereço.
