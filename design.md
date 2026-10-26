# DESIGN.md — Diretrizes de design para leitura de artigos e textos longos

> **Versão:** 1.0  
> **Data:** 25 de outubro de 2026  
> **Status:** guia conceitual e especificação inicial de UX/UI  
> **Escopo:** sites de artigos, ensaios, relatórios técnicos e textos analíticos publicados na web.  
> **Independência tecnológica:** este documento não pressupõe framework, CMS ou plataforma.

## 1. Visão

Criar uma experiência de leitura digital que combine:

1. **A atmosfera editorial da [Quanta Magazine](https://www.quantamagazine.org/):** protagonismo do texto, tipografia com personalidade, hierarquia elegante, respiro visual e integração de ilustrações, gráficos e referências à narrativa.
2. **A clareza funcional do [GOV.UK](https://www.gov.uk/) e de seu [Design System](https://design-system.service.gov.uk/):** informação imediatamente compreensível, organização previsível, alto contraste, navegação simples, controles acessíveis e ausência de ruído visual.
3. **A profundidade de uma publicação técnico-científica:** figuras, tabelas, equações, citações e fontes podem ser exploradas sem prejudicar o fluxo de leitura.

**Diretriz principal:** adotar **serifa no corpo do artigo** e **sem serifa na interface**, com elementos visuais subordinados ao conteúdo.

A página deve parecer, ao mesmo tempo, uma **revista de ensaios de alta qualidade** e um **serviço digital extremamente claro**. Evitar o aspecto de dashboard corporativo e, no extremo oposto, a ornamentação que atrapalha a leitura.

### 1.1 O que se pretende alcançar

- Favorecer leitura contínua de 10–40 minutos ou mais.
- Permitir leitura exploratória: localizar capítulos, voltar a uma seção, seguir referências e examinar dados.
- Preservar a compreensão quando o usuário altera zoom, tamanho da letra, espaçamento ou tema.
- Tornar textos técnicos densos acessíveis sem simplificar indevidamente seu conteúdo.
- Funcionar com qualidade em desktop, tablet, celular e impressão.

### 1.2 O que não fazer

- Não imitar literalmente a identidade visual, a marca ou os ativos de Quanta/GOV.UK.
- Não assumir que toda preferência editorial tem validação científica: certas escolhas são **hipóteses de design** a testar.
- Não preencher as margens com painéis, botões ou widgets permanentes.
- Não usar imagens, gráficos, cards ou animações como enfeite sem função informacional.
- Não justificar parágrafos (`text-align: justify`) por padrão.
- Não transformar artigos em uma sucessão excessiva de cards; o fluxo textual deve dominar.

---

## 2. Referências e o que aprender de cada uma

| Referência | Principal contribuição | Como aplicar | Natureza da evidência |
|---|---|---|---|
| [Quanta Magazine](https://www.quantamagazine.org/) | Linguagem editorial de artigos científicos e ensaios | Tipografia, ritmo de seções, integração texto–imagem, aberturas de artigo | **Inspiração visual/editorial**, não alegação de superioridade mensurada |
| [GOV.UK Design System](https://design-system.service.gov.uk/) | Clareza, padronização, navegação e acessibilidade | Hierarquia, links claros, componentes previsíveis, linguagem direta | **Sistema documentado e desenvolvido com pesquisa de usuários** |
| [GOV.UK — Layout](https://design-system.service.gov.uk/styles/layout/) | Controle de largura do conteúdo e adaptação responsiva | Coluna de leitura limitada e layout mobile-first | Diretriz de design documentada |
| [US Web Design System — Typography](https://designsystem.digital.gov/components/typography/) | Comprimento de linha (*measure*) | Tomar aproximadamente 66 caracteres por linha como ponto de partida | Diretriz pública, ajustável a testes |
| [Quarto Websites](https://quarto.org/docs/websites/) | Publicação de conteúdo técnico | Notas, referências, fórmulas, capítulos e modo de leitura | Referência funcional/template |
| [Our World in Data](https://ourworldindata.org/) | Integração entre análise, fontes e visualização | Gráficos contextualizados e metodologia acessível | Referência editorial/funcional |
| [Readwise Reader](https://readwise.io/read) | Leitura controlada pelo usuário | Controles de tema, tipografia, marcações | Referência funcional |
| [Astro Starlight](https://starlight.astro.build/) | Documentação estruturada | Índice, busca e navegação por seções | Template de documentação, não padrão visual principal |

**Síntese:** Quanta inspira **a voz visual da leitura**; GOV.UK orienta **a disciplina de interação**. Quarto/Our World in Data contribuem para **conteúdo técnico e verificável**.

---

## 3. Princípios de design

1. **O artigo é a interface principal.** O leitor deve perceber primeiro título, introdução e texto; a navegação fica em segundo plano.
2. **Escaneabilidade sem fragmentação.** Títulos, subtítulos e resumos ajudam quem explora, mas não quebram desnecessariamente o encadeamento do argumento.
3. **Contraste alto, ruído baixo.** Acessibilidade deve determinar a cor e o destaque, não o contrário.
4. **Uma largura confortável de leitura.** Telas grandes não justificam linhas intermináveis.
5. **Hierarquia sem dependência de cor.** Tamanho, peso, posição, espaçamento e marcação semântica distinguem níveis de conteúdo.
6. **Controles descobríveis, mas discretos.** O leitor encontra índice, tema, ajuste de fonte e compartilhamento sem que eles concorram com o texto.
7. **Referências verificáveis.** Citações e dados permitem consultar a origem, identificar data, autoria e metodologia.
8. **Progressive disclosure.** Oferecer explicações adicionais quando necessárias, sem sobrecarregar a leitura inicial.
9. **Acessibilidade desde o início.** Teclado, foco, leitores de tela, zoom e contrastes integram o desenho.
10. **Evidência acima de ornamento.** Mapas, tabelas, imagens e gráficos devem esclarecer um argumento ou documentar uma observação.

---

## 4. Tipografia

### 4.1 Escolha principal

| Uso | Família recomendada | Racional |
|---|---|---|
| **Corpo do artigo** | **Source Serif 4** | Serifa contemporânea, apropriada a texto longo, livre e com tamanhos ópticos |
| **Títulos editoriais** | **Source Serif 4**, semibold | Continuidade visual entre a abertura e o corpo |
| Navegação, menus, controles | **Source Sans 3** | Complemento sem serifa, funcional e discreto |
| Legendas, autor, datas, índice | **Source Sans 3** | Ajuda a distinguir metainformações da narrativa |
| Código, identificadores, coordenadas | Fonte monoespaçada acessível | Diferencia conteúdos literais e facilita inspeção |
| Tabelas numéricas | Source Sans 3, algarismos tabulares | Melhora alinhamento e comparabilidade |

**Alternativas que merecem teste com leitores:** [Literata](https://github.com/googlefonts/literata) (leitura digital imersiva) e [Alegreya](https://github.com/huertatipografica/Alegreya) (caráter humanista/literário).

**Importante:** fontes serifadas **não são comprovadamente superiores em qualquer tela e para qualquer leitor**. A escolha aqui responde ao caráter editorial desejado. A experiência deve oferecer opção sem serifa e testes reais de compreensão e conforto.

Não copiar GDS Transport do GOV.UK: sua licença é restritiva em usos fora dos domínios GOV.UK. A dupla Source Serif 4 + Source Sans 3 fornece identidade própria e está disponível com licenças abertas, sujeitas à verificação dos termos na implantação.

Fontes oficiais: [Source Serif](https://adobe-fonts.github.io/source-serif/), [repositório Source Serif](https://github.com/adobe-fonts/source-serif), [repositório Source Sans](https://github.com/adobe-fonts/source-sans).

### 4.2 Parâmetros iniciais de leitura

Os valores a seguir são **pontos de partida para prototipação**, não leis universais de legibilidade.

| Propriedade | Desktop | Mobile | Observação |
|---|---:|---:|---|
| Fonte do corpo | 19–20 px | 17–19 px | Respeitar escala de texto do sistema |
| Entrelinha | 1,55–1,65 | 1,55–1,65 | Preferir valores relativos |
| Comprimento de linha | Alvo 60–75 caracteres | Definido pela largura disponível | Verificar contagem real com a fonte aplicada |
| Largura máxima do corpo | ~`68–72ch` | 100% da área útil | `ch` é aproximação, não contagem exata |
| Espaço entre parágrafos | ~`0.9–1.2em` | ~`0.9–1.2em` | Ajustar conforme ritmo de leitura |
| Alinhamento | À esquerda | À esquerda | Não justificar automaticamente |
| Recuo da primeira linha | Nenhum | Nenhum | Usar separação entre parágrafos |
| Peso do corpo | 400 | 400 | Evitar fontes excessivamente leves |
| Itálico | Trechos curtos | Trechos curtos | Não usar para blocos extensos |

A referência do USWDS sugere que a maioria das linhas tenha cerca de 45–90 caracteres e propõe **66 caracteres** como bom alvo para textos longos. O GOV.UK também limita a extensão das linhas em desktop, frequentemente a aproximadamente 75 caracteres.

### 4.3 Escala tipográfica sugerida

| Elemento | Desktop | Mobile | Tratamento |
|---|---:|---:|---|
| H1 / título do artigo | `clamp(2.35rem, 4vw, 3.7rem)` | Fluido | Source Serif 4, peso 600, entrelinha 1.1–1.17 |
| Resumo/abertura (*dek*) | 1.25–1.4rem | 1.1–1.25rem | Peso regular, entrelinha 1.45 |
| H2 | 2.0–2.25rem | 1.65–1.9rem | Serifa, espaço superior generoso |
| H3 | 1.45–1.7rem | 1.3–1.5rem | Serifa ou sem serifa, conforme teste |
| H4 | 1.12–1.25rem | 1.05–1.18rem | Hierarquia claramente distinta |
| Corpo | 1.1875–1.25rem | 1.0625–1.1875rem | Source Serif 4 |
| Legenda | 0.875–0.95rem | 0.875–0.95rem | Source Sans 3 |
| Metadados | 0.8125–0.9rem | 0.8125–0.9rem | Source Sans 3 |

Evitar comprimir títulos em excesso. Não usar tudo em maiúsculas em títulos extensos. Empregar níveis `h1`–`h4` de maneira semântica, não apenas visual.

### 4.4 Variações de leitura

Oferecer ao leitor, de preferência em um controle discreto:

- **Fonte:** editorial (serifada, padrão) / simples (sem serifa).
- **Tamanho:** menor / padrão / maior, sempre preservando controles nativos do navegador.
- **Tema:** claro (padrão) / escuro / sépia.
- **Modo de leitura:** esconder navegação secundária e ampliar foco no texto, sem desabilitar recursos de acessibilidade.

As preferências podem ser mantidas no próprio navegador, respeitando preferências do sistema. A ausência dessa persistência não deve bloquear a leitura.

---

## 5. Layout e composição de página

### 5.1 Estrutura recomendada no desktop

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ Marca discreta                          Busca · Artigos · Preferências   │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│       Categoria / tema                                                   │
│       TÍTULO DO ARTIGO                                                   │
│       Subtítulo editorial curto e informativo                            │
│       Autor · Data · Tempo estimado de leitura                           │
│                                                                          │
│  Índice       ┌────────────────────────────────────┐   Referências/notas  │
│  do artigo    │ Texto principal serifado          │   contextuais*       │
│  (discreto)   │ largura ~68–72ch                   │                      │
│               │                                    │                      │
│               │ Parágrafos, subtítulos, citações  │                      │
│               │ e ilustrações                     │                      │
│               └────────────────────────────────────┘                      │
│                                                                          │
│                  Figuras largas podem exceder a coluna                    │
│                                                                          │
│                   Bibliografia · Artigos relacionados                     │
└──────────────────────────────────────────────────────────────────────────┘
*Notas laterais apenas quando acrescentarem valor e houver espaço real.
```

A coluna de leitura é a protagonista. Índice e notas laterais não precisam estar presentes permanentemente: podem aparecer somente em telas largas, ou ser recolhíveis.

### 5.2 Responsividade

- **Mobile primeiro:** uma única coluna, margens laterais generosas sem desperdiçar área útil (começar com 16–20 px).
- **Tablet:** coluna central; índice acessível por botão ou painel recolhível.
- **Desktop:** possibilidade de índice lateral *sticky*, desde que não reduza a coluna do artigo.
- **Telas muito largas:** preservar o limite de largura do texto; permitir que certas visualizações usem uma faixa maior, com alinhamento visual consistente.
- **Zoom de 200% e reflow em 320 CSS px de largura equivalente:** texto, controles e tabelas devem continuar utilizáveis; excetuar, quando permitido, conteúdo que exige representação bidimensional.

Os breakpoints devem nascer de testes de conteúdo (quando o layout deixa de funcionar), não de modelos específicos de aparelhos.

### 5.3 Espaçamento e ritmo vertical

- Dar mais espaço **antes** de uma nova seção do que entre o título e seu primeiro parágrafo.
- Preservar agrupamento entre título e conteúdo subsequente.
- Evitar sucessões de parágrafos muito curtos apenas para criar aparência de interface.
- Equilibrar textos, imagens, notas e tabelas para não formar uma parede ininterrupta de conteúdo.
- Usar divisores horizontalmente apenas para mudanças reais de contexto ou seções principais.

### 5.4 Cabeçalho e rodapé

**Cabeçalho:** baixo, estável, com marca, navegação mínima e busca quando relevante. Cabeçalho fixo é opcional; se existir, não deve ocupar uma fração excessiva da tela móvel.

**Rodapé:** referências institucionais, licença, informações editoriais, privacidade, contato e navegação relacionada. Não repetir painéis promocionais ao fim de cada capítulo.

---

## 6. Cores e identidade visual

A identidade deve ser **neutra, tipográfica e de alto contraste**, com poucos acentos. O papel da cor é comunicar seleção, interação ou significado — não decorar o texto.

### 6.1 Paleta inicial (hipótese visual)

| Token | Tema claro | Finalidade |
|---|---|---|
| `--bg` | `#FCFBF8` | Fundo próximo ao papel branco |
| `--surface` | `#FFFFFF` | Planos ocasionais, quando necessários |
| `--text` | `#202322` | Corpo e títulos |
| `--muted` | `#555D64` | Metadados e legendas |
| `--link` | `#17518A` | Links e ações de navegação |
| `--rule` | `#D9DCD9` | Linhas divisórias |
| `--focus` | `#17518A` | Contorno de foco com contraste verificável |

| Token | Tema escuro | Finalidade |
|---|---|---|
| `--bg` | `#16191E` | Fundo |
| `--surface` | `#20262E` | Planos ocasionais |
| `--text` | `#E8E9E8` | Corpo e títulos |
| `--muted` | `#A6ADBA` | Metadados e legendas |
| `--link` | `#9CCAFF` | Links |
| `--rule` | `#464D57` | Divisores |

Tema sépia opcional: fundo `#F7F3EB`, texto `#21262D`, links `#1B4C75`. Todos os pares texto/fundo sugeridos acima devem ser retestados no contexto dos componentes e estados reais.

### 6.2 Regras de cor

- Links **visivelmente sublinhados** dentro do corpo do artigo, não identificados apenas pela cor.
- Diferenciar estados (selecionado, ativo, aviso, erro) por mais de uma característica visual.
- Não utilizar fundo colorido como recurso recorrente para destacar conclusões.
- Para artigos científicos, reservar cores mais vivas aos gráficos e mapas, preferencialmente com legendas acessíveis.
- Fotografias e ilustrações devem ter tratamento consistente, mas sem filtros decorativos que prejudicam evidências.

---

## 7. Componentes editoriais e de navegação

### 7.1 Abertura do artigo

Ordem recomendada:

1. Categoria ou seção temática discreta.
2. Título principal legível, informativo e não truncado.
3. *Dek* (subtítulo/resumo de 1–3 frases), quando adequado.
4. Autoria, data de publicação, data de atualização (se houver) e tempo estimado de leitura.
5. Imagem de abertura somente se acrescentar contexto ou valor explicativo.
6. Introdução textual sem interferências promocionais.

A data de atualização não substitui a data original de publicação. Quando houver revisão substancial, deixar isso explícito.

### 7.2 Índice do artigo

- Gerado a partir dos títulos semânticos `h2`/`h3`.
- Oportunamente lateral em telas largas; recolhível em telas menores.
- Mostrar seção ativa de forma discreta e acessível.
- Permitir copiar o link direto de uma seção (`#ancora`).
- O índice não deve cobrir texto, bloquear rolagem nem capturar o foco sem ação do usuário.

### 7.3 Links e referências

- Exibir links reconhecíveis e com descrição útil, não apenas “clique aqui”.
- Citações numéricas podem abrir nota contextual por clique ou foco; o destino definitivo deve ser acessível via link.
- Referências completas ao final, com autor/instituição, título, ano, URL/DOI e data de acesso quando relevante.
- Notas devem ter possibilidade de retorno ao ponto original de leitura.
- Não depender de `hover`: telas sensíveis ao toque e navegação por teclado precisam do mesmo acesso.
- Links externos não precisam abrir nova aba compulsoriamente.

### 7.4 Destaques, notas e citações

Criar poucos padrões consistentes:

- **Citação longa:** recuo e tratamento tipográfico próprios; autoria/fonte obrigatória quando aplicável.
- **Nota explicativa:** texto opcional com contexto adicional.
- **Alerta metodológico:** aviso para limitações de interpretação ou qualidade de dados.
- **Definição:** termo e explicação objetiva, com link para glossário quando necessário.

Não usar caixas chamativas para cada conclusão ou parágrafo importante. Dar preferência a subtítulos, linguagem precisa e espaço em branco.

### 7.5 Tabelas

- Cabeçalhos descritivos, unidades explícitas e alinhamento à direita para números.
- Algarismos tabulares quando houver comparação numérica.
- `caption` e relacionamento semântico com cabeçalhos (`th`, `scope`).
- Não encolher fontes a ponto de prejudicar leitura; em mobile, aceitar rolagem horizontal **somente no contêiner da tabela**, com indicação visível.
- Quando a tabela for realmente grande, oferecer visualização ampliada ou arquivo de dados acessível.
- Notas sobre fonte e método imediatamente próximas à tabela.

### 7.6 Gráficos, figuras e mapas

- Manter uma pergunta clara por figura e título interpretável.
- Oferecer legenda, unidades, período de referência e fonte.
- Disponibilizar texto alternativo; figuras complexas podem exigir descrição longa ou tabela equivalente.
- Não codificar séries apenas por cor; apoiar-se também em rótulos, forma, padrão ou textura.
- Permitir largura maior que a coluna do artigo quando necessário, sem quebrar a ordem de leitura.
- Evitar que mapas interativos capturem rolagem da página inesperadamente.
- Indicar explicitamente se os números são observados, estimados, modelados ou ilustrativos.

### 7.7 Equações e código

- Fórmulas em notação correta e com identificação quando referenciadas no texto.
- Em telas pequenas, permitir deslocamento **local** para expressões matemáticas longas, sem rolagem horizontal da página inteira.
- Código com fonte monoespaçada, contraste correto, opção de copiar e quebra/rolagem de linhas cuidadosamente testada.
- Explicar siglas e símbolos na primeira ocorrência relevante.

### 7.8 Elementos opcionais

- Barra de progresso de leitura **discreta**, nunca obrigatória.
- Tempo estimado de leitura como metadado secundário.
- Compartilhar/copiar link, sem barra social permanente cobrindo o texto.
- “Continuar lendo” ou salvamento como melhoria posterior, se houver necessidade demonstrada.
- Busca global útil em acervos grandes; desnecessária para um site com poucos artigos.

---

## 8. Acessibilidade como critério de aceitação

Meta inicial: **WCAG 2.2 nível AA**. Alguns critérios AAA podem orientar melhorias sem serem confundidos com obrigação AA.

### 8.1 Requisitos mínimos

- Contraste mínimo de **4,5:1** para texto comum e **3:1** para texto grande, conforme as exceções normativas.
- Elementos não textuais de interface com contraste adequado, quando exigível.
- Links distinguíveis sem depender somente da cor.
- Navegação integral por teclado, foco claramente visível e ordem de foco lógica.
- Link “Pular para o conteúdo”.
- Regiões semânticas (`header`, `nav`, `main`, `aside`, `footer`) e hierarquia de títulos coerente.
- Zoom de texto até **200%** sem perda de conteúdo ou função.
- Reflow e espaçamentos personalizados sem corte de texto ou controles.
- Alvos interativos adequados para toque, estados acessíveis e mensagens compreensíveis.
- Modo escuro e animações respeitando preferências do sistema, inclusive `prefers-reduced-motion`.

**Precisão normativa:** WCAG 2.2 **1.4.12 (AA)** não obriga usar sempre um determinado espaçamento; exige que o conteúdo não se perca quando o usuário aplica os espaçamentos definidos no critério. O critério **1.4.8 (AAA)** traz orientações adicionais sobre apresentação visual, como limite de caracteres e ajustes disponíveis ao leitor.

Referências oficiais: [WCAG 2.2 em português](https://www.w3c.br/traducoes/wcag/wcag22-pt-BR/), [WCAG 2.2 W3C](https://www.w3.org/TR/WCAG22/), [GOV.UK Design System](https://design-system.service.gov.uk/).

### 8.2 Testes indispensáveis

- Teclado: Tab, Shift+Tab, Enter, Escape e foco visível.
- Leitores de tela: pelo menos um fluxo completo de leitura e de navegação por títulos.
- Zoom de 200% e reflow em viewport estreita.
- Texto ampliado e espaçamento sobrescrito pelo usuário.
- Diferentes condições de luminosidade, monitores e celulares.
- Acesso ao índice, notas e referências sem mouse.
- Impressão/PDF pelo navegador sem elementos de interface desnecessários.

---

## 9. Exemplo inicial de design tokens e CSS

Este CSS é uma **base de protótipo**, não um tema completo nem uma implementação obrigatória. Pressupõe o carregamento legítimo das famílias tipográficas escolhidas.

```css
:root {
  color-scheme: light;
  --font-reading: "Source Serif 4", Georgia, serif;
  --font-ui: "Source Sans 3", system-ui, sans-serif;
  --font-code: ui-monospace, "SFMono-Regular", Consolas, monospace;

  --bg: #fcfbf8;
  --surface: #ffffff;
  --text: #202322;
  --muted: #555d64;
  --link: #17518a;
  --rule: #d9dcd9;
  --focus: #17518a;

  --reading-measure: 70ch;
  --article-font-size: 1.25rem;
  --article-leading: 1.6;
  --gutter: clamp(1rem, 3vw, 2rem);
}

* { box-sizing: border-box; }
html { scroll-behavior: smooth; }
body {
  margin: 0;
  color: var(--text);
  background: var(--bg);
  font-family: var(--font-ui);
}

.article {
  width: min(calc(100% - 2 * var(--gutter)), var(--reading-measure));
  margin-inline: auto;
}

.article__body {
  font-family: var(--font-reading);
  font-optical-sizing: auto;
  font-size: var(--article-font-size);
  font-weight: 400;
  line-height: var(--article-leading);
  text-align: left;
  overflow-wrap: break-word;
}
.article__body p { margin: 0 0 1em; }
.article__body h2,
.article__body h3 {
  font-family: var(--font-reading);
  font-optical-sizing: auto;
  line-height: 1.2;
  scroll-margin-top: 5rem;
}
.article__body h2 { margin: 2.1em 0 0.55em; }
.article__body h3 { margin: 1.65em 0 0.5em; }
.article__body a { color: var(--link); text-underline-offset: 0.14em; }
.article__body a:hover { text-decoration-thickness: 0.12em; }

.article__meta,
.article__caption,
.article__toc { font-family: var(--font-ui); }
.article__caption { color: var(--muted); font-size: 0.9rem; }

:focus-visible {
  outline: 3px solid var(--focus);
  outline-offset: 3px;
}

.article__table-wrap {
  max-width: 100%;
  overflow-x: auto;
}

@media (max-width: 48rem) {
  :root {
    --article-font-size: 1.125rem;
    --gutter: 1rem;
  }
}

/* Tema escolhido explicitamente pelo leitor. */
[data-theme="dark"] {
  color-scheme: dark;
  --bg: #16191e;
  --surface: #20262e;
  --text: #e8e9e8;
  --muted: #a6adba;
  --link: #9ccaff;
  --rule: #464d57;
  --focus: #9ccaff;
}

[data-theme="sepia"] {
  color-scheme: light;
  --bg: #f7f3eb;
  --surface: #fcfaf5;
  --text: #21262d;
  --muted: #555d64;
  --link: #1b4c75;
  --rule: #d7d2c7;
  --focus: #1b4c75;
}

[data-reading-font="sans"] {
  --font-reading: "Source Sans 3", system-ui, sans-serif;
}

@media (prefers-reduced-motion: reduce) {
  html { scroll-behavior: auto; }
}

@media print {
  :root { --bg: #fff; --text: #111; }
  .site-header, .article__toc, .reading-controls { display: none !important; }
  .article { width: 100%; max-width: none; }
  a { color: inherit; }
}
```

**Ajustes obrigatórios antes de produção:** definir carregamento de fontes com fontes variáveis e `font-display` apropriado; implementar escala completa de títulos; testar tokens com contrast checker; testar tabelas, fórmulas, gráficos e componentes interativos; verificar comportamento com fontes ausentes; preferir unidades relativas em layout tipográfico.

---

## 10. Modelo mínimo de estrutura de um artigo

```text
Artigo
├── Identificação
│   ├── Categoria
│   ├── Título (H1)
│   ├── Resumo editorial / dek
│   ├── Autor(es)
│   └── Datas e metadados
├── Navegação contextual (índice)
├── Corpo
│   ├── Introdução
│   ├── Seções (H2) e subseções (H3)
│   ├── Parágrafos, listas e citações
│   ├── Figuras, gráficos, mapas e tabelas
│   └── Notas metodológicas e explicativas
├── Conclusão
├── Fontes e bibliografia
├── Informações sobre atualização / metodologia
└── Artigos relacionados (opcional)
```

O documento original pode estar em Markdown, HTML semântico ou outro formato estruturado. **O design não deve depender do formato de armazenamento**, mas exige a capacidade de representar corretamente títulos, notas, tabelas, figuras, fontes e referências cruzadas.

---

## 11. Estratégia de prototipação e validação

### Protótipo A — Editorial

- Source Serif 4 no corpo e nos títulos.
- Coluna limitada e fundo claro próximo ao papel.
- Cabeçalho discreto, índice lateral opcional e figuras bem integradas.
- Referência dominante: Quanta Magazine.

### Protótipo B — Editorial + funcional (preferido)

- Mesma base editorial, mas com navegação, links, estados e componentes inspirados na disciplina do GOV.UK.
- Índice acessível, referências navegáveis, personalização de leitura e layouts previsíveis.
- **Este é o conceito recomendado para a primeira implementação.**

### Protótipo C — Leitor imersivo

- Variante do B com interface minimizada, ajustes tipográficos e modo de concentração.
- Útil para testar leitura extensa e retorno ao ponto anterior.

### Critérios a observar com usuários reais

1. **Encontrabilidade:** o leitor localiza uma informação específica sem esforço excessivo?
2. **Compreensão:** consegue explicar corretamente o argumento principal e localizar a evidência correspondente?
3. **Orientação:** sabe em que seção está e consegue voltar após abrir referências?
4. **Conforto:** relata leitura confortável após 10–20 minutos em tela de desktop e celular?
5. **Acessibilidade:** navega com teclado, zoom e leitor de tela sem perder recursos?
6. **Densidade de informação:** a presença de gráficos e tabelas ajuda em vez de interromper?
7. **Preferência:** quais fontes, larguras e temas funcionam melhor para diferentes leitores?

Medir tarefas e dificuldades, não apenas votos sobre qual tela parece mais bonita. A serifa deve ser mantida como padrão editorial **se sua aceitação for boa**, sem impedir alternativas individuais.

### Critérios iniciais de aceite da UI

- [ ] Corpo editorial em Source Serif 4; interface em Source Sans 3.
- [ ] Comprimento real de linha testado, aproximando-se de 60–75 caracteres no desktop.
- [ ] Conteúdo principal permanece legível em mobile, zoom elevado e orientação alternativa.
- [ ] Contrastes e interações atendem à meta WCAG 2.2 AA.
- [ ] Índice e âncoras funcionam sem mouse.
- [ ] Notas, citações e fontes podem ser abertas e fechadas sem perder o contexto.
- [ ] Tabelas e figuras largas não estouram o layout.
- [ ] Temas claro/escuro, preferências tipográficas e movimento reduzido são testados.
- [ ] Impressão apresenta o conteúdo sem controles redundantes.
- [ ] Pelo menos dois artigos reais (um narrativo e um técnico com tabelas/gráficos) são usados na validação.

---

## 12. Decisões tomadas e questões em aberto

### Decisões adotadas

1. **Direção visual:** Quanta Magazine no conteúdo + GOV.UK na clareza e nos padrões de uso.
2. **Tipografia padrão:** serifada no texto longo, sem serifa na navegação e na interface.
3. **Fonte inicial preferida:** Source Serif 4, complementada por Source Sans 3.
4. **Largura da leitura:** aproximadamente 60–75 caracteres por linha em desktop, com calibração visual.
5. **Hierarquia:** editorial, discreta, sem excesso de cards, badges ou efeitos visuais.
6. **Conteúdo técnico:** gráficos, tabelas, fórmulas, mapas e referências como parte da leitura, não como dashboard autônomo.
7. **Acessibilidade:** meta WCAG 2.2 AA desde os protótipos.
8. **Personalização:** oferecer alternativas de fonte, tamanho e tema sem impor uma única configuração.

### Questões a decidir após protótipo

- O índice ficará sempre visível em desktop ou somente sob demanda?
- Quais configurações tipográficas terão persistência local?
- Citações terão notas laterais, *popover* acessível, notas de rodapé ou combinação?
- Qual será o tratamento de elementos muito largos em telas pequenas?
- Como serão exibidas atualizações editoriais e correções de um artigo já publicado?
- Qual identidade cromática secundária é apropriada sem prejudicar a neutralidade editorial?

---

## 13. Fontes e materiais de consulta

### Identidade editorial e exemplos reais

- Quanta Magazine: https://www.quantamagazine.org/
- Our World in Data: https://ourworldindata.org/
- Readwise Reader: https://readwise.io/read

### Design systems e leitura

- GOV.UK Design System: https://design-system.service.gov.uk/
- GOV.UK — Layout: https://design-system.service.gov.uk/styles/layout/
- GOV.UK — Type scale: https://design-system.service.gov.uk/styles/type-scale/
- GOV.UK — Home Office typography/accessibility: https://design.homeoffice.gov.uk/accessibility/page-structure/layout-typography
- U.S. Web Design System — Typography: https://designsystem.digital.gov/components/typography/
- W3C — WCAG 2.2: https://www.w3.org/TR/WCAG22/
- W3C Brasil — tradução das WCAG 2.2: https://www.w3c.br/traducoes/wcag/wcag22-pt-BR/

### Fontes tipográficas

- Source Serif 4 — oficial: https://adobe-fonts.github.io/source-serif/
- Source Serif — código e licença: https://github.com/adobe-fonts/source-serif
- Source Sans — código e licença: https://github.com/adobe-fonts/source-sans
- Literata: https://github.com/googlefonts/literata
- Alegreya: https://github.com/huertatipografica/Alegreya

### Templates e sistemas de publicação

- Quarto: https://quarto.org/docs/websites/
- Astro Starlight: https://starlight.astro.build/

---

## 14. Resumo executivo

> **Design editorial por natureza, acessível por construção.**
>
> Artigos devem ter a expressividade tipográfica e a serenidade de uma revista científica, com texto serifado como protagonista; a interface deve ter a disciplina, a previsibilidade e a acessibilidade de um serviço público bem desenhado. A tecnologia, os painéis e os controles nunca devem disputar a atenção com o conteúdo.

**Recomendação final:** começar pelo **Protótipo B — Editorial + funcional**, usando **Source Serif 4 + Source Sans 3**, uma coluna de aproximadamente **70ch**, fundo claro neutro, links evidentes, navegação contextual simples e componentes de dados tratados como conteúdo editorial. Validar o conjunto em artigos reais antes de sofisticar a interface.
