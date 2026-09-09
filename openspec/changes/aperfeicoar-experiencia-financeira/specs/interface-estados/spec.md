## MODIFIED Requirements

### Requirement: Aplicar direção visual azul global

A interface SHALL usar fundo azul-marinho e uma direção visual azul consistente em páginas públicas e privadas, incluindo fundos, superfícies, navegação, campos, botões, cartões, gráficos e modais. O layout SHALL poder usar referências visuais de Status Invest e Investidor10 apenas como inspiração de hierarquia, densidade e organização, sem copiar identidade, textos, imagens ou elementos proprietários. A mudança SHALL preservar contraste legível, responsividade sem rolagem horizontal, estados de carregamento/vazio/erro/alerta/sucesso e as cores semânticas necessárias para ganho, perda e ações destrutivas.

#### Scenario: Reformulação visual
- **WHEN** o investidor navegar entre login, início, Investimentos, Banco e Bolsa
- **THEN** a interface SHALL apresentar o novo layout azul-marinho e a hierarquia visual reformulada sem permanecer com a aparência anterior

#### Scenario: Rotas públicas e privadas com tema azul
- **WHEN** o investidor navegar entre cadastro, login, conta, carteira, ativos e operações
- **THEN** os elementos recorrentes SHALL apresentar a paleta azul definida para a aplicação, sem permanecerem verdes por herança de estilos anteriores

#### Scenario: Estados semânticos preservados
- **WHEN** a interface exibir erro, alerta, sucesso, ganho, perda ou ação destrutiva sob o tema azul
- **THEN** o estado SHALL continuar distinguível por cor, texto e/ou estrutura, com contraste e foco visível adequados

#### Scenario: Tema responsivo
- **WHEN** qualquer rota for visualizada em 320 px, tablet ou desktop
- **THEN** o tema SHALL permanecer utilizável, sem sobreposição ou rolagem horizontal e sem quebrar os fluxos existentes

### Requirement: Impedir área vazia por arraste horizontal

Todas as páginas e componentes SHALL limitar o conteúdo ao viewport e SHALL impedir que largura mínima, transformações, tabelas, gráficos ou cartões criem área vazia acessível por arraste horizontal. Conteúdos extensos SHALL adaptar, quebrar, empilhar ou oferecer rolagem interna localizada sem rolagem horizontal da página.

#### Scenario: Arraste em viewport estreito
- **WHEN** o investidor tentar arrastar horizontalmente uma página a partir de 320 px
- **THEN** a página SHALL permanecer limitada ao viewport, sem faixa vazia ou deslocamento lateral do layout

#### Scenario: Conteúdo amplo
- **WHEN** um gráfico, lista ou cartão não couber na largura disponível
- **THEN** o componente SHALL adaptar seu layout ou conter a rolagem internamente sem ampliar a página inteira
