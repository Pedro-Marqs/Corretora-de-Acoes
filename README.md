
# Gestão de Ações e Corretoras

Projeto acadêmico para simulação de investimentos em ações brasileiras e norte-americanas.

## Estrutura inicial

- Backend: Java 17 e Spring Boot 3.4.0.
- Frontend: React com JavaScript e Vite; Node.js 20.19 ou 22.12+ e npm 10+.
- Organização própria em camadas, conforme `docs/04-arquitetura.md`.

O projeto é implementado incrementalmente, uma tarefa por vez. O repositório externo citado na documentação serve somente como referência estrutural; nenhum código foi copiado ou incorporado.

## Backend

```powershell
.\mvnw.cmd clean verify
```

## Frontend

```powershell
Set-Location src/main/front
npm ci
npm run lint
npm test
npm run build
```
