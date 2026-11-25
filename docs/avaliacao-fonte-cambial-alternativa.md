# Avaliação documental de fonte cambial alternativa

Data da avaliação: 2026-11-24

Esta avaliação é somente documental. Nenhuma fonte alternativa foi implementada, configurada ou usada para substituir a AwesomeAPI.

## Candidata

A candidata é o endpoint de câmbio da Twelve Data, que já é um provedor configurado para cotações norte-americanas:

`GET https://api.twelvedata.com/exchange_rate?symbol=USD/BRL&apikey=<chave>`

Documentação oficial:

- [Autenticação e API keys](https://twelvedata.com/docs/introduction/libraries)
- [Endpoint exchange_rate](https://twelvedata.com/docs/ai/ai-data-assistant)
- [Créditos e limites](https://support.twelvedata.com/en/articles/5615854-credits)

## Comparação

| Critério | AwesomeAPI atual | Twelve Data como alternativa | Decisão neste change |
| --- | --- | --- | --- |
| Autenticação | `x-api-key`, conforme evidência de produção e documentação do provedor | Query `apikey` ou header `Authorization: apikey`, conforme documentação oficial | Manter AwesomeAPI; não ativar alternativa |
| Par USD/BRL | Endpoint já usado pelo sistema | `/exchange_rate` aceita pares com barra, incluindo o formato documentado | Requer teste de contrato e adaptação de payload antes de qualquer adoção |
| Timestamp | Payload atual fornece timestamp Unix | Endpoint documenta timestamp Unix | Compatível em princípio, sujeito a validação de semântica e timezone |
| Limites | Devem ser observados pelo contrato da conta AwesomeAPI | Twelve Data usa créditos por minuto; o plano Basic também possui limite diário de 800 créditos e HTTP 429 ao exceder quota | Não duplicar chamadas nem trocar provedor para contornar quota |
| Custo/licença | Contrato atual da conta deve permanecer como fonte de verdade | Cobertura e limites variam por plano; contratação/licença não foi avaliada | Nenhuma mudança comercial ou de licença neste change |
| Falhas | O problema observado na VPS é 429 com Bearer e 200 com `x-api-key` | API padroniza erros de autenticação e quota, mas ainda exige observabilidade e cache | Não usar fallback alternativo automaticamente |

## Conclusão

Twelve Data é tecnicamente plausível como fallback futuro porque já possui credencial e documentação para câmbio, mas a troca não é segura como correção automática: consumiria quota adicional, mudaria a fonte oficial e exigiria definir precedência, qualidade, timestamps, arredondamento, observabilidade e aprovação de produto. A AwesomeAPI permanece a fonte oficial deste change; qualquer adoção deverá ser um novo change OpenSpec com testes de contrato e decisão explícita.
