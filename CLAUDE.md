# CLAUDE.md

## Regras inegociaveis

### Idioma do codigo-fonte

- TODO o codigo-fonte deve ser escrito em INGLES, incluindo comentarios, nomes de variaveis, metodos, classes, mensagens de log e identificadores.
- Textos de interface (labels visiveis ao usuario) em Portugues Brasileiro, com excecao a palavras e jargoes tecnicos naturalmente em ingles.
- Esta documentacao (CLAUDE.md) e as conversas podem ser em portugues.

### Autoria de commits e comentarios

- NUNCA faca commit nem assine comentarios como "Claude" ou similar.
- Use sempre a identidade do usuario do git configurada na maquina:
  - Nome: Jose Damico
  - Email: jose.damico@scicrop.com
- Nada de linhas "Co-Authored-By: Claude" ou "Generated with Claude".

### Caracteres proibidos (front-end e codigo-fonte)

Jamais use estes caracteres no codigo, nos templates ou em textos de UI. Use somente ASCII simples no lugar deles. A coluna "Glifo" existe apenas como referencia visual nesta tabela (unica excecao permitida).

| Nome | Code point | Decimal | HTML entity | Glifo | Use no lugar |
|---|---|---|---|---|---|
| Travessao (em dash) | U+2014 | 8212 | `&mdash;` | — | virgula, parenteses, - |
| Meia-risca (en dash) | U+2013 | 8211 | `&ndash;` | – | hifen comum (-) |
| Aspas duplas abre | U+201C | 8220 | `&ldquo;` | “ | aspas retas (") |
| Aspas duplas fecha | U+201D | 8221 | `&rdquo;` | ” | aspas retas (") |
| Aspas simples abre | U+2018 | 8216 | `&lsquo;` | ‘ | aspa reta (') |
| Aspas simples fecha | U+2019 | 8217 | `&rsquo;` | ’ | aspa reta (') |
| Seta direita | U+2192 | 8594 | `&rarr;` | → | palavra "para" |
| Seta esquerda | U+2190 | 8592 | `&larr;` | ← | palavra "voltar" |
| Bullet | U+2022 | 8226 | `&bull;` | • | hifen (-) em listas |
| Ponto medio | U+00B7 | 183 | `&middot;` | · | hifen (-) |
| Aproximadamente | U+2248 | 8776 | `&asymp;` | ≈ | palavra "aproximado" |
| Infinito | U+221E | 8734 | `&infin;` | ∞ | palavra "infinito" |

**Observacao 1:** nenhum desses caracteres (nem o code point, mas o GLIFO) pode aparecer no codigo autoral. Acima eles aparecem so na tabela de referencia.

**Observacao 2:** arquivos de terceiros vendorizados (ex.: bootstrap.min.css) podem conter esses caracteres internamente; isso e aceitavel porque nao e codigo que nos escrevemos. A regra vale para o codigo autoral.

## Requisitos da aplicacao web (modulo Java)

- Spring Boot + Thymeleaf + SpringBootAI.
- Bootstrap 5 LOCAL, sem CDN. Os arquivos CSS e JS ficam servidos de `/static` no proprio projeto.
- Banco: PostgreSQL (inclusive no desenvolvimento local). Nao usar H2.

### Estrutura de programacao Java

| Camada | Responsabilidade |
|---|---|
| Controller | Recebe a requisicao HTTP e retorna JSON |
| Service | Executa regras de negocio e controla transacoes |
| Repository | Executa SQL diretamente no PostgreSQL |
| Model | Representa os dados, sem `@Entity` |
| DTO | Define os campos aceitos pelo endpoint |

## Persistencia e SGBD

- Por padrao, PostgreSQL na mesma VM do Spring Boot.
- Jamais usar JPA, Hibernate ou qualquer ORM. As consultas devem ser construidas em SQL.
- Jamais usar ferramentas de migracao de schema de terceiros.
  - Mudancas de schema sao scripts SQL numerados em `db/migrations/` (ex.: `003_clientes.sql`).
  - Os scripts sao aplicados pelo `db/migrate.sh` com `psql`, que registra o que ja rodou na tabela `schema_migration`.
  - Script ja aplicado nunca e editado: correcoes vao num script novo.

## Ambiente de testes

- Maquina de testes (aplicacao e banco): Ubuntu 24.04, `192.168.68.137`, usuario sudoer `jdamico` (acesso SSH por chave).
- Spring Boot roda na porta `8089`.
- A aplicacao roda sob o context path `/biblia` (`server.servlet.context-path`), o mesmo caminho publico de `https://josedamico.com.br/biblia`: o nginx de producao repassa `/biblia/` sem reescrever o caminho, pela VPN, para esta maquina (`10.8.0.11:8089`).
  - Templates montam URLs com `@{...}`, que ja incluem o prefixo. No JS, toda URL raiz (paginas e API) passa por `Biblia.url()` (`common.js`, le o prefixo da meta `app-base` do layout); `Biblia.getJson()` ja aplica o prefixo. Nunca escreva `'/api/...'` ou `location.href = '/...'` direto.
- PostgreSQL 16 na mesma maquina, ouvindo apenas em `localhost:5432`.
  - Banco `biblia`, dono/usuario `biblia`.
  - A senha do usuario `biblia` fica em `~/.pgpass` do usuario `jdamico` na maquina de testes. Nunca grave senhas no codigo nem no repositorio.
  - Para acessar a partir da maquina de desenvolvimento, use tunel SSH: `ssh -L 5432:localhost:5432 jdamico@192.168.68.137`.
- Build local exige JDK 21 (o Maven da maquina de desenvolvimento usa o 17 por padrao):
  `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -DskipTests package`.
- Deploy na maquina de testes: `./deploy.sh` (build, copia `biblia.jar` e `db/` para `~/biblia/`, roda `db/migrate.sh` e reinicia o servico).
  - A aplicacao roda como servico systemd de usuario (`systemctl --user status biblia`, logs em `journalctl --user -u biblia`), com linger habilitado para subir no boot.
  - A aplicacao tambem le a senha do banco do `~/.pgpass` (driver JDBC), sem senha em `application.properties`.
- Textos biblicos sao dados, nao migracoes: a edicao (fonte, licenca, versao) e cadastrada por migracao e os versiculos sao carregados por importador.
  - Biblia Livre n4 2025.1.0 (commit `a315a15e9f`): `scripts/import-blivre.sh` na maquina de testes (baixa a fonte para `~/biblia/sources/` e carrega a edicao `blivre-n4`, que precisa estar vazia).
  - Nestle 1904 e SBLGNT com morfologia MACULA Greek (commit `8423afe`): `scripts/import-macula.sh n1904` e `scripts/import-macula.sh sblgnt` (tokens, lemas e analises).
  - Almeida 1911, so Novo Testamento, com alinhamento palavra a palavra ao SBLGNT (Clear Bible Alignments, commit `c99bd0a`): `scripts/import-jfa11.sh`, depois do SBLGNT. O alinhamento foi transferido automaticamente do espanhol (RVR09); a interface sempre avisa isso.
  - Codice de Leningrado (WLC) com morfologia MACULA Hebrew/OSHB (commit `47db250`): `scripts/import-macula.sh wlc` (leva cerca de 6 minutos). Cada palavra grafica e um token; prefixos, raiz e sufixos sao segmentos em `token_analyses`, com idioma proprio (hebraico ou aramaico). Contagem por lema conta segmentos. O MACULA usa o Qere onde ha Ketiv/Qere, e a numeracao de palavras tem lacunas nesses pontos.
  - Para recarregar uma edicao MACULA ja importada: `scripts/import-macula.sh <edicao> --replace` (apaga os versiculos da edicao em cascata, inclusive alinhamentos que apontam para ela). Todo importador roda `ANALYZE` no fim; sem isso as primeiras consultas apos uma importacao podem levar minutos.
  - Correspondencia de versiculos (STEPBible TVTMS, commit `be4191f`, mais `scripts/data/versification-corrections.tsv`): `scripts/import-versification.sh`, a ser rodado de novo apos importar ou recarregar qualquer edicao. Grava em `passage_standard_refs` a referencia padrao de cada versiculo; e so nivel de versiculo, nunca de palavra. Defeitos de arquivo de uma edicao entram como correcao manual, verificada no texto.
  - Versificacao difere entre edicoes (ex.: At 24:2-3, 2Co 13:12-14, Ap 12:18; MACULA usa Mc 16:99 para o final curto). O paralelo em portugues da concordancia e por referencia igual, nunca apresentado como alinhamento.
- Antes de aplicar uma migracao nova no banco `biblia`, valide-a num banco descartavel (ex.: `biblia_tmp`), pois script aplicado nao pode ser editado.

## Front-end, GUI e UX

- Todo o front-end segue o que esta definido em `design.md`, na raiz do projeto.
- A Biblia Livre (`blivre-n4`) e a edicao oficial e padrao. A edicao de leitura so muda quando o usuario escolhe outra no seletor de edicao; cliques em palavras, referencias da concordancia e linhas do mapa nunca trocam a edicao: o original aparece no painel lateral.

## Principios de design

- KISS (Keep It Simple, Stupid): prefira a solucao mais simples que resolve. Nada de camadas ou abstracoes sem necessidade real.
- Padroes de projeto (GoF) somente quando agregam reuso real.
- Pense em reutilizacao de codigo antes de duplicar.

## Fluxo de desenvolvimento (bottom-up)

Todo desenvolvimento deve ser bottom-up, nesta ordem:

1. Persistencia: tabelas/DDL no PostgreSQL e o SQL das consultas.
2. Model e Repository.
3. Service.
4. DTO e Controller, ate o endpoint.
