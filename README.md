# Bíblia Interlinear e Concordância Lexical

> Especificação conceitual inicial de uma aplicação web para leitura e investigação lexical das Escrituras, com textos em idiomas originais, traduções em português e concordância de palavras.
>
> **Estado:** proposta inicial — sem implementação definida.
>
> **Decisão tecnológica deste documento:** PostgreSQL como base de dados. Nenhuma outra tecnologia de implementação é prescrita.

## 1. Visão geral

O projeto pretende disponibilizar uma **Bíblia completa, navegável e pesquisável**, acompanhada de recursos interlineares e de um sistema de **concordância lexical**. O usuário poderá selecionar uma palavra de um texto bíblico e imediatamente:

1. ver sua escrita original, transliteração e pronúncia aproximada, quando houver dados confiáveis;
2. identificar seu **lema** (forma de dicionário) e a análise gramatical da ocorrência;
3. consultar os significados registrados em fontes lexicográficas, distinguindo definições gerais da tradução naquela passagem;
4. contar suas ocorrências em uma **edição textual determinada**;
5. abrir a lista completa de referências e os versículos correspondentes, com destaque da palavra;
6. filtrar por livro, conjunto de livros, forma escrita e, conforme os dados disponíveis, características morfológicas;
7. navegar da ocorrência encontrada para o respectivo contexto de leitura.

A aplicação deverá ser útil tanto a leitores sem conhecimentos de idiomas bíblicos quanto a estudantes interessados no texto original, sem exigir inteligência artificial. O projeto não é uma tradução automática nem uma ferramenta destinada a oferecer uma única interpretação teológica.

### Objetivo central

**Clicar em uma palavra → compreender seus significados possíveis → saber quantas vezes seu lema aparece → consultar cada ocorrência no contexto bíblico.**

## 2. Alcance dos textos e idiomas

| Conjunto textual | Idioma | Tratamento proposto |
| --- | --- | --- |
| Bíblia Hebraica / maior parte do Antigo Testamento | Hebraico bíblico | Corpus original prioritário |
| Passagens de Daniel, Esdras e outros trechos tradicionalmente reconhecidos | Aramaico bíblico | Integrar ao mesmo corpus textual do Antigo Testamento, indicando o idioma de cada unidade |
| Novo Testamento | Grego koiné | Corpus original prioritário |
| Septuaginta | Grego | Corpus adicional de tradução antiga, identificado separadamente do hebraico |
| Peshitta | Siríaco, variedade histórica do aramaico | Corpus adicional, condicionado à disponibilidade e aos direitos de cada fonte |
| Tradução(ões) em português | Português | Leitura e comparação, com identificação explícita da tradução/edição |

**Princípios históricos importantes:**

- Os Evangelhos preservados foram redigidos em grego; sua redação grega não deve ser identificada automaticamente com a forma aramaica exata das falas históricas de Jesus.
- A Septuaginta e a Peshitta são tradições textuais de grande relevância, mas **não substituem indistintamente** o hebraico/aramaico ou o grego dos corpora originais escolhidos.
- Divergências entre edições, tradições e manuscritos não devem ser apagadas para formar um único texto artificial.
- Uma edição é sempre identificada por sua fonte, versão, idioma, abrangência e critérios editoriais.

### Cânones bíblicos

O projeto deverá permitir, em sua modelagem, mais de uma organização canônica: tradição de 66 livros, tradição católica de 73 livros e conjuntos presentes nas tradições ortodoxas. **A primeira entrega pode ser restrita aos 66 livros com fontes abertas já validadas**, sem comprometer a evolução posterior. Inclusão de livros, adições e diferenças de numeração requer fontes textuais e traduções devidamente licenciadas.

A navegação por capítulo e versículo não deve presumir que todos os corpora usem numeração idêntica. Deve existir um mecanismo explícito de correspondência entre referências, incluindo lacunas e correspondências de um-para-muitos ou muitos-para-um.

## 3. Experiência do usuário

### 3.1 Leitura bíblica

A leitura começa pela escolha de um livro, capítulo, versículo, idioma e edição textual. A interface deve oferecer:

- texto contínuo, com pontuação e apresentação legíveis;
- visualização paralela entre texto original e tradução;
- visualização interlinear por palavra ou segmento;
- opção de exibir ou ocultar transliteração, glosa e morfologia;
- seleção direta de qualquer palavra do original;
- destaque de palavras pesquisadas e acesso ao contexto dos versículos.

A leitura dos textos hebraicos e aramaicos deve respeitar a direção da escrita da direita para a esquerda. A apresentação não deve perder vogais, sinais diacríticos ou outros elementos relevantes presentes na edição utilizada.

### 3.2 Painel lexical ao selecionar uma palavra

Exemplo conceitual: seleção de **λόγος** em João 1:1.

O painel exibiria:

| Campo | Exemplo |
| --- | --- |
| Forma selecionada | λόγος |
| Lema | λόγος |
| Transliteração | lógos |
| Identificador lexical | Strong G3056, quando a fonte fornecer associação validada |
| Classe gramatical | Substantivo |
| Morfologia da ocorrência | Nominativo, masculino, singular |
| Possíveis significados | palavra, discurso, mensagem, relato, razão, argumento |
| Tradução naquela passagem | Conforme a edição portuguesa escolhida |
| Contagem | Calculada sobre o corpus e a edição selecionados; nunca fixada manualmente |

As definições devem indicar sua **fonte lexicográfica**. Uma glosa curta não é uma explicação completa do significado nem garante que todas as ocorrências tenham o mesmo sentido.

### 3.3 Concordância e ocorrências

O painel oferece, preferencialmente, três buscas distintas:

1. **Por lema (padrão):** agrupa as diferentes flexões da mesma entrada lexical, sem confundir palavras não relacionadas.
2. **Por forma exata:** encontra apenas ocorrências da forma escrita selecionada, de acordo com regras explícitas de normalização.
3. **Por atributos morfológicos:** restringe ocorrências do mesmo lema, quando os dados disponíveis permitirem, por classe, tempo, caso, número, gênero ou outras categorias pertinentes ao idioma.

A pesquisa por **raiz** pode ser acrescentada posteriormente, mas não deve ser tratada como sinônimo de lema. A pesquisa por **sentido específico** só pode gerar contagens quando houver anotações semânticas confiáveis para cada ocorrência; não basta compartilhar o mesmo verbete no dicionário.

Os resultados devem informar separadamente:

- **número de ocorrências:** quantas unidades lexicais foram encontradas;
- **número de versículos distintos:** em quantos versículos as ocorrências aparecem;
- **número de livros distintos:** em quantos livros o lema ocorre.

Uma mesma palavra repetida três vezes em um versículo conta como **três ocorrências e um versículo**. Os resultados indicam sempre idioma, edição, filtros e critérios usados.

### 3.4 Lista das ocorrências

Cada resultado deve mostrar:

- livro, capítulo e versículo na edição consultada;
- trecho original com a palavra destacada;
- trecho correspondente em português, quando disponível;
- forma escrita da ocorrência e seus dados gramaticais;
- acesso ao versículo completo e ao contexto anterior/posterior.

A lista poderá ser agrupada por livro e ordenada pela sequência canônica escolhida. A totalização por livro é parte do requisito, e não apenas uma apresentação visual.

### 3.5 Seleção a partir da tradução portuguesa

Como evolução desejável, a seleção de uma palavra ou expressão em português deve apontar para a(s) palavra(s) correspondente(s) do original **quando houver alinhamento explícito e validado**.

A correspondência não é necessariamente uma-para-uma: uma palavra pode corresponder a várias; algumas unidades não terão equivalente direto; e traduções interpretativas podem reestruturar frases inteiras. **Não presumir alinhamento automático perfeito.** Sem evidência de correspondência lexical, mostrar a tradução apenas em nível de versículo.

## 4. Conceitos linguísticos e critérios de rigor

### Palavra, ocorrência e lema

- **Ocorrência (token):** posição concreta de uma palavra ou unidade textual numa edição.
- **Forma escrita:** representação efetivamente encontrada no texto.
- **Lema (lexema):** forma de referência do vocabulário usada para agrupar flexões.
- **Morfologia:** descrição gramatical daquela ocorrência.
- **Raiz:** elemento de formação lexical, quando linguisticamente pertinente e identificado por fonte adequada.
- **Glosa:** equivalência breve usada como auxílio à leitura.
- **Sentido lexical:** acepção de um lema; pode mudar conforme o contexto.
- **Alinhamento:** relação documentada entre unidades de corpora ou traduções diferentes.

### Cuidados obrigatórios

1. **Identificadores de Strong são referências auxiliares**, não substitutos universais de lemas, sentidos, raízes ou análises modernas. Diferentes conjuntos de dados podem empregar identificadores estendidos ou desambiguações próprias.
2. Formas com acentos, vogais, sinais de cantilação e variantes ortográficas precisam de regras de comparação documentadas. A forma original deve ser preservada, mesmo quando existirem formas normalizadas para pesquisa.
3. No hebraico e no aramaico, prefixos e sufixos podem representar morfemas relevantes. O sistema precisa distinguir token gráfico de unidade lexical analisada para não subcontar ou supercontar resultados.
4. Leituras alternativas, como **ketiv/qere**, devem conservar sua distinção quando presentes na fonte; a política de contagem precisa declarar qual leitura está sendo considerada.
5. Uma forma ambígua pode admitir mais de uma análise: registrar confiança, ambiguidade ou fonte da desambiguação, sem fabricar certeza.
6. As listas devem ser reproduzíveis: a mesma pesquisa, com a mesma edição e filtros, produz os mesmos totais.
7. Textos em idiomas diferentes nunca devem ser somados como se compartilhassem o mesmo vocabulário lexical.

## 5. Fontes candidatas e proveniência

Estas são **fontes a avaliar e importar**, não ferramentas de implementação prescritas.

| Fonte | Possível uso | Condição a observar |
| --- | --- | --- |
| [Open Scriptures Hebrew Bible](https://github.com/openscriptures/morphhb) | Texto hebraico, trechos aramaicos, lemas, morfologia e identificadores de ocorrência | Texto-base em domínio público; anotações sob CC BY 4.0, com atribuição |
| [SBL Greek New Testament](https://github.com/Faithlife/SBLGNT) | Edição grega do Novo Testamento | CC BY 4.0; a edição textual, por si só, não garante todos os metadados lexicais desejados |
| [MACULA Greek](https://github.com/Clear-Bible/macula-greek) | Morfologia, lemas, relações e mapeamentos gregos | Dados centrais declarados CC BY 4.0; conferir separadamente as licenças e condições de materiais incorporados |
| [STEPBible Data](https://github.com/STEPBible/STEPBible-Data) | Léxicos, anotações, identificadores e dados para concordância | Repositório declara CC BY 4.0; preservar créditos e regras aplicáveis a cada conjunto |
| [Bíblia Livre](https://github.com/blivre/BibliaLivre) | Tradução de referência inicial em português | CC BY 3.0 Brasil; manter créditos, identificação da variante e versão |
| [ETCBC Peshitta](https://github.com/ETCBC/peshitta) | Corpus adicional da Peshitta do Antigo Testamento | O **texto dos dados** é indicado como CC BY-NC: não pressupor possibilidade de exploração comercial; diferenciar licença dos dados e do código do repositório |

**Observação:** a licença de cada repositório deve ser revisitada antes da importação pública ou da redistribuição. Se um corpus apresentar restrições incompatíveis, deve ficar fora da distribuição até haver autorização ou substituição válida. O texto de um repositório aberto não significa que seus dicionários, glosas e traduções embutidas tenham necessariamente direitos idênticos.

### Cadastro mínimo de proveniência

Para cada fonte incorporada, registrar:

- nome e responsável;
- endereço oficial;
- edição, versão ou referência de atualização;
- data de obtenção;
- licença e texto de atribuição exigido;
- escopo de cobertura (livros, idiomas, camadas lexicais);
- modificações realizadas durante a normalização;
- limitações conhecidas e vínculo com o dado original.

## 6. Modelo conceitual de dados — PostgreSQL

O **PostgreSQL é a base de dados proposta para armazenar, consultar e relacionar os corpora bíblicos e suas anotações**. O esquema deve privilegiar integridade, proveniência e consultas reprodutíveis, sem fixar prematuramente todos os detalhes físicos.

### 6.1 Entidades principais

| Entidade conceitual | Responsabilidade |
| --- | --- |
| `languages` | Identificar idiomas dos textos |
| `textual_works` | Distinguir obras e tradições textuais |
| `editions` | Identificar edições efetivamente consultáveis e suas versões |
| `canons` | Registrar conjuntos e ordens de livros |
| `books` | Identidade de livros bíblicos, independente de uma edição |
| `canon_books` | Pertencimento, nome e ordem dos livros em cada cânon |
| `passages` | Unidades de referência de uma edição: livro, capítulo, versículo e eventuais subdivisões |
| `passage_correspondences` | Mapear referências não equivalentes entre edições |
| `tokens` | Ocorrências textuais com ordem, grafia e localização |
| `token_segments` | Segmentação morfológica ou lexical de um token, quando aplicável |
| `lexemes` | Entradas lexicais por idioma e sistema de identificação |
| `token_lexemes` | Associação de uma ocorrência ou segmento ao(s) lema(s), com fonte e eventuais ambiguidades |
| `lexical_senses` | Definições e acepções com atribuição de fonte |
| `morphological_analyses` | Análises gramaticais de ocorrências ou segmentos |
| `lexical_identifiers` | Códigos de Strong e de outros sistemas, sem pressupor equivalência perfeita |
| `text_alignments` | Correspondências verificadas entre unidades de edições distintas |
| `source_records` | Registro de origem, versão, licença e transformação dos dados |

Uma ocorrência deve ter identidade estável no contexto de sua edição, e sua ordenação deve ser preservada. Uma mesma passagem pode ter diversas ocorrências do mesmo lema. Um token pode possuir mais de um segmento e, conforme a fonte, múltiplas possibilidades de análise.

### 6.2 Relações fundamentais

- **Edição → passagens → tokens**: permite reconstruir o texto e listar ocorrências em sua posição real.
- **Token/segmento → lema**: sustenta a concordância lexical.
- **Token/segmento → morfologia**: permite filtros gramaticais.
- **Lema → definições/identificadores**: permite consultar significado e referências lexicais.
- **Token/trecho → alinhamento → token/trecho de outra edição**: oferece equivalências sem exigir correspondência individual obrigatória.
- **Edição/fonte → licença/proveniência**: permite auditoria e atribuição adequada.

### 6.3 Integridade e desempenho

O PostgreSQL deverá conter restrições para evitar duplicidades acidentais de posições textuais, referências inválidas e relações entre idiomas não compatíveis. As consultas frequentes deverão dispor de índices apropriados para edição, lema, passagem, ordem dos tokens e filtros morfológicos.

A modelagem deve admitir a preservação do texto original **e** versões normalizadas exclusivamente para pesquisa. A normalização nunca deve modificar silenciosamente a forma editorial exibida.

## 7. Regras de contagem e de pesquisa

### Contagem por lema

Por padrão, a pesquisa informa o total de **ocorrências lexicais elegíveis** daquele lema na edição selecionada. Quando um token tiver mais de um componente lexical, a unidade contada deve ser o segmento corretamente identificado. Quando uma ocorrência tiver análises alternativas, evitar contagem duplicada e adotar uma política explícita de inclusão ou exclusão de casos ambíguos.

### Contagem por forma exata

É independente da contagem por lema. O usuário deve saber se a busca considera diferenciação de sinais diacríticos, pontuação, espaços e variantes ortográficas. A forma exibida no texto permanece inalterada.

### Contagem por sentido

Não é requisito do MVP. A lista de significados disponíveis em um dicionário **não atribui automaticamente um significado a cada ocorrência**. Uma eventual contagem por sentido requer análise contextual previamente anotada e indicação de sua fonte.

### Escopo da busca

Toda busca precisa exibir, pelo menos:

- edição e respectiva versão;
- idioma do corpus;
- lema ou forma pesquisada;
- livros incluídos ou excluídos;
- filtros morfológicos;
- critério de tratamento de leituras variantes e ambiguidades, quando pertinente.

Não somar edições alternativas do mesmo texto para produzir um único número de ocorrências da “Bíblia” sem explicar o que está sendo somado.

## 8. Fluxo de ingestão e validação

1. **Selecionar a fonte:** verificar cobertura textual, qualidade dos metadados e permissões de uso.
2. **Preservar o original:** manter a possibilidade de conferir cada conteúdo importado contra sua fonte.
3. **Registrar proveniência:** identificar edição, versão, licença e data.
4. **Normalizar referências:** conectar livros, capítulos, versículos e sistemas de numeração sem perder diferenças entre edições.
5. **Importar ocorrências:** preservar forma escrita, ordem e vínculo à passagem.
6. **Associar dados linguísticos:** conectar lemas, segmentos, análises e definições, anotando discrepâncias entre fontes.
7. **Construir a concordância:** garantir que cada ocorrência recuperada possa ser rastreada até sua posição original.
8. **Validar por amostragem e por totais:** comparar livros, passagens, tokens e ocorrências com os dados de origem.
9. **Publicar com atribuição:** exibir créditos, fontes e limitações no próprio produto.

### Casos de teste linguísticos

- Verificar que **λόγος**, **λόγου** e **λόγον** podem ser recuperados pelo lema comum, mas diferenciados na busca por forma.
- Verificar que três ocorrências do mesmo lema num versículo geram três ocorrências, mas um único versículo distinto.
- Verificar que prefixos hebraicos segmentados não causam dupla contagem do lema principal.
- Verificar leituras alternativas com critérios de contagem documentados.
- Verificar que edições gregas distintas podem produzir listas e totais diferentes.
- Verificar que seleção em português sem alinhamento disponível não gera associação lexical inventada.
- Verificar direção, pontuação e diacríticos de textos originais.

## 9. Requisitos funcionais priorizados

### Fase 1 — Corpus e leitura

- [ ] Cadastro de obras, idiomas, livros, cânones e edições.
- [ ] Importação e validação de uma edição do Antigo Testamento hebraico/aramaico.
- [ ] Importação e validação de uma edição grega do Novo Testamento.
- [ ] Importação de uma tradução completa em português, observando sua licença.
- [ ] Navegação completa por livro, capítulo e versículo.
- [ ] Exibição paralela entre texto original e português, inicialmente em nível de versículo.
- [ ] Identificação visível da edição em uso.

### Fase 2 — Léxico e concordância (**objetivo central**)

- [ ] Tornar as palavras originais selecionáveis.
- [ ] Exibir lema, transliteração, classe gramatical e dados morfológicos existentes.
- [ ] Exibir definições com fontes e ressalvas de interpretação.
- [ ] Pesquisar todas as ocorrências por lema.
- [ ] Pesquisar por forma escrita.
- [ ] Apresentar contagens de ocorrências, versículos e livros distintos.
- [ ] Listar e agrupar as ocorrências por livro, sempre com destaque no contexto.
- [ ] Filtrar resultados por livro, edição e atributos linguísticos disponíveis.
- [ ] Registrar e exibir proveniência e atribuições.

### Fase 3 — Expansão textual e interlinear

- [ ] Adicionar, conforme viabilidade e licenças, outras traduções em português.
- [ ] Desenvolver alinhamentos verificáveis entre original e português.
- [ ] Permitir seleção de palavra em português para localizar correspondências originais quando comprovadas.
- [ ] Incorporar a Septuaginta como edição/tradição separada.
- [ ] Avaliar a inclusão de corpora siríacos da Peshitta sob condições de uso adequadas.
- [ ] Ampliar cobertura de tradições canônicas e diferentes versificações.
- [ ] Adicionar comparação lexical entre edições, preservando as diferenças textuais.

### Fora do escopo inicial

- Interpretação automática por inteligência artificial.
- Tradução automática ou criação automática de significados.
- Contagem automática de ocorrências por sentido sem anotação específica.
- Reconstrução especulativa do aramaico originalmente falado por Jesus.
- Reconstrução de manuscritos ou aparato crítico completo.
- Recursos pessoais de anotação, contas de usuário e colaboração, até que surja necessidade concreta.

## 10. Critérios de aceitação do MVP

O MVP será considerado funcional quando um leitor puder:

1. percorrer os livros contemplados nos corpora iniciais e saber qual edição está lendo;
2. abrir João 1:1, selecionar **λόγος** e visualizar seu lema e informações lexicais disponíveis;
3. consultar **todas** as ocorrências daquele lema na edição grega selecionada, com contagem reproduzível;
4. alternar para a busca pela forma exata e obter um conjunto de resultados potencialmente diferente;
5. visualizar totais separados de ocorrências, versículos e livros;
6. filtrar a concordância por livro e abrir cada resultado no contexto de leitura;
7. repetir o fluxo para um lema hebraico ou aramaico presente no corpus do Antigo Testamento;
8. consultar créditos e dados de origem da edição e do léxico apresentados.

Esses testes **não** dependerão de um número de ocorrências codificado no produto: os valores deverão derivar da edição importada e passar por conferência contra a fonte.

## 11. Decisões em aberto

As decisões abaixo podem ser tomadas após a validação inicial dos corpora:

- Quais edições específicas do hebraico/aramaico e do grego serão adotadas como padrão?
- Qual tradução portuguesa será exibida inicialmente e qual variante/versionamento será fixado?
- Qual sistema de lema será principal quando as fontes divergirem?
- Como apresentar ao usuário divergências entre diferentes léxicos, identificadores e análises?
- Como declarar no produto a política de contagem para morfemas, casos ambíguos e leituras variantes?
- Em qual etapa entram deuterocanônicos, Septuaginta e Peshitta?
- Até que nível a relação palavra-a-palavra com o português pode ser garantida por dados existentes?

## 12. Princípios permanentes

1. **Texto original primeiro:** manter vínculo verificável entre a interface e o corpus de origem.
2. **Transparência editorial:** toda ocorrência pertence a uma edição definida.
3. **Significado contextualizado:** distinguir dicionário, glosa, tradução e sentido em uso.
4. **Concordância auditável:** cada total deve poder ser explicado e reproduzido.
5. **Nenhuma equivalência inventada:** não pressupor alinhamento entre idiomas sem dados adequados.
6. **Rigor de atribuição:** licenças e autores dos corpora fazem parte da documentação do produto.
7. **Evolução gradual:** começar pela leitura e concordância lexical, sem adicionar funcionalidades não essenciais.

---

**Resumo:** uma Bíblia web com leitura em português e nas línguas bíblicas originais, capaz de mostrar o significado documentado de uma palavra e encontrar, contar e explorar todas as suas ocorrências — principalmente por **lema**, com respeito às diferenças entre idiomas, edições e tradições textuais. O armazenamento e as consultas serão organizados no **PostgreSQL**.
