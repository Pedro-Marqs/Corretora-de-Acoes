
# Gestão de Ações e Corretoras

Projeto acadêmico para simulação de investimentos em ações brasileiras e norte-americanas.

## Estrutura inicial

- Backend: Java 17 e Spring Boot 3.4.0.
- Frontend: React com JavaScript e Vite; Node.js 20.19 ou 22.12+ e npm 10+.
- Organização própria em camadas, conforme `docs/04-arquitetura.md`.

O projeto é implementado incrementalmente, uma tarefa por vez. O repositório externo citado na documentação serve somente como referência estrutural; nenhum código foi copiado ou incorporado.

## Backend

Copie `.env.example` para `.env` e substitua os valores de exemplo. O arquivo `.env` é local e ignorado pelo Git.

Inicie o PostgreSQL:

```powershell
docker compose up -d postgres
```

Carregue as variáveis do `.env` no terminal e inicie o backend com o perfil local:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^[^#][^=]*=') {
        $name, $value = $_ -split '=', 2
        Set-Item -Path "Env:$name" -Value $value
    }
}
.\mvnw.cmd spring-boot:run
```

Execute os testes rápidos com H2:

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
