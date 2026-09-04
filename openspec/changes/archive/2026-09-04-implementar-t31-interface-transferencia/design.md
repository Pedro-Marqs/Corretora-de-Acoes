## Context

A T30 fornece o caso de uso autenticado e transacional de transferência; a T31 deve apenas compor esse contrato no frontend existente. A interface já possui cliente HTTP, proteção de rotas, estados compartilhados e padrões de confirmação usados em operações financeiras.

## Goals / Non-Goals

**Goals:**

- Reutilizar a leitura autenticada de posições e corretoras para montar a seleção de transferência.
- Manter a seleção derivada da carteira, validar apenas restrições de apresentação e enviar o payload mínimo aceito pelo backend.
- Integrar confirmação, bloqueio de submissão, tratamento de 401 e atualização pós-sucesso aos padrões existentes.
- Cobrir o fluxo com testes de componentes e serviço, incluindo viewport estreito e falhas recuperáveis.

**Non-Goals:**

- Alterar o endpoint, o domínio financeiro, o saldo, a persistência ou o contrato de erros do backend.
- Recalcular custo, preço médio, patrimônio, disponibilidade ou resultado no frontend.
- Criar resumo separado, estorno, edição de transferência ou suporte a corretoras de outra conta.

## Decisions

- **Composição no fluxo privado existente:** a transferência será uma rota/componente privado integrado ao layout de carteira, em vez de uma tela pública ou fluxo independente. Isso preserva proteção de sessão e navegação já estabelecidas; uma implementação paralela duplicaria cliente HTTP e estados.
- **Fonte de verdade no snapshot e na resposta:** a origem, a quantidade disponível e o saldo serão derivados da leitura autenticada, enquanto o aceite final continuará no backend. Após sucesso, a tela substituirá o snapshot por resposta ou nova leitura; não haverá atualização otimista, pois ela poderia mascarar concorrência.
- **Payload mínimo:** enviar somente identificadores opacos de origem, destino e ativo e a quantidade. Preço, custo, saldo e conta serão omitidos, evitando que dados exibidos sejam tratados como autoridade financeira.
- **Validação em camadas:** bloquear imediatamente destino igual à origem e quantidade inválida no formulário, mas preservar os erros funcionais do backend para alterações concorrentes ou estado desatualizado. A validação local não substituirá a confirmação do servidor.
- **Confirmação inline reutilizável:** usar o mesmo padrão de confirmação simples das operações existentes, mantendo foco, cancelamento sem perda de contexto e sem página de resumo. Um modal separado apenas será necessário se o componente compartilhado não suportar acessibilidade equivalente.
- **Tratamento de sessão uniforme:** resposta 401 removerá dados privados e seguirá o redirecionamento de autenticação existente; erros de rede e 5xx manterão contexto e oferecerão nova tentativa.

## Risks / Trade-offs

- **[Snapshot desatualizado]** a posição pode mudar antes do envio → o backend permanece autoridade; exibir sua mensagem funcional e manter o formulário recuperável.
- **[Duplicidade de requisições]** cliques repetidos podem gerar tentativas concorrentes → desabilitar controles durante leitura, confirmação e envio e cobrir essa transição em testes.
- **[Layout estreito]** seletores e mensagens podem exceder 320 px → usar o sistema responsivo existente e verificar especificamente 320 px, tablet e desktop.
- **[Inconsistência visual entre carteiras e transferência]** componentes duplicados podem divergir → reutilizar formatadores, estados e controles já existentes, adicionando apenas o comportamento específico.

## Migration Plan

Não há migração de banco nem alteração de API. Implementar o componente e sua rota/navegação no frontend, habilitar os testes focados e validar o build. O rollback consiste em remover a rota e os componentes novos; nenhum dado persistido exige conversão.
