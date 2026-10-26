(function () {
    var editionCode = document.body.dataset.edition;
    var params = new URLSearchParams(location.search);
    var FILTERS = ['person', 'number', 'gender', 'grammaticalCase', 'tense', 'voice', 'mood', 'stem', 'verbForm',
        'state'];

    var criteria = {lexeme: params.get('lexeme'), form: params.get('form'), book: params.get('book'),
        from: params.get('from')};
    FILTERS.forEach(function (name) {
        criteria[name] = params.get(name);
    });

    var offset = 0;
    var currentBook = null;
    var language = 'grc';
    var direction = 'ltr';

    function apiUrl(pageOffset) {
        var query = new URLSearchParams();
        Object.keys(criteria).forEach(function (key) {
            if (criteria[key]) {
                query.set(key, criteria[key]);
            }
        });
        query.set('offset', String(pageOffset));
        return '/api/editions/' + encodeURIComponent(editionCode) + '/concordance?' + query.toString();
    }

    function pageUrl(changes) {
        var next = {};
        Object.keys(criteria).forEach(function (key) {
            next[key] = criteria[key];
        });
        Object.keys(changes).forEach(function (key) {
            next[key] = changes[key];
        });
        return Biblia.concordanceUrl(editionCode, next);
    }

    function dlRow(list, term, value, lang) {
        list.appendChild(Biblia.element('dt', null, term));
        var dd = Biblia.element('dd', null, value);
        if (lang) {
            dd.lang = lang;
        }
        list.appendChild(dd);
    }

    function renderHeader(result) {
        language = Biblia.languageTag(result.edition.languageCode);
        direction = result.edition.scriptDirection;
        Biblia.renderEdition(result.edition);

        var title = document.getElementById('concordance-title');
        title.textContent = result.lemma || result.form;
        title.lang = language;
        document.title = (result.lemma || result.form) + ' - Concordância';
        document.getElementById('concordance-dek').textContent = result.lemma
            ? 'Todas as ocorrências do verbete, incluindo suas flexões.'
            : 'Ocorrências da forma escrita exata, sem distinguir maiúsculas e minúsculas.';

        var scope = document.getElementById('scope-list');
        dlRow(scope, 'Edição', result.edition.name + ' (' + result.edition.version + ')');
        dlRow(scope, 'Idioma', result.edition.languageName);
        if (result.lemma) {
            scope.appendChild(Biblia.element('dt', null, 'Verbete'));
            var verbete = Biblia.element('dd');
            verbete.appendChild(Biblia.verbete(result.lemma, language, result.lemmaGlosses));
            scope.appendChild(verbete);
        } else {
            dlRow(scope, 'Forma', result.form, language);
        }
        var bookName = result.bookCode
            ? (result.byBook[0] ? result.byBook[0].bookName : result.bookCode)
            : 'todos os livros da edição';
        dlRow(scope, 'Livros', bookName);
        var filters = Object.keys(result.filters).map(function (name) {
            return Biblia.filterNames[name] + ': ' + Biblia.label(name, result.filters[name]);
        });
        dlRow(scope, 'Filtros morfológicos', filters.length ? filters.join('; ') : 'nenhum');
        dlRow(scope, 'Análise', 'Verbetes e morfologia conforme a fonte da edição');

        var totals = document.getElementById('totals');
        dlRow(totals, 'Ocorrências', result.counts.occurrences.toLocaleString('pt-BR'));
        dlRow(totals, 'Versículos distintos', result.counts.verses.toLocaleString('pt-BR'));
        dlRow(totals, 'Livros distintos', result.counts.books.toLocaleString('pt-BR'));
    }

    function renderFilters(result) {
        var grid = document.getElementById('filters-grid');

        var bookLabel = Biblia.element('label');
        bookLabel.appendChild(Biblia.element('span', null, 'Livro'));
        var bookSelect = Biblia.element('select');
        bookSelect.name = 'book';
        bookSelect.appendChild(new Option('Todos', ''));
        Biblia.getJson('/api/editions/' + encodeURIComponent(editionCode) + '/books').then(function (books) {
            books.forEach(function (book) {
                bookSelect.appendChild(new Option(book.name, book.code, false, book.code === criteria.book));
            });
        });
        bookLabel.appendChild(bookSelect);
        grid.appendChild(bookLabel);

        // Only the attributes present in this edition are offered, with the values found in its data.
        Biblia.getJson('/api/editions/' + encodeURIComponent(editionCode) + '/filter-values').then(function (values) {
            FILTERS.filter(function (name) { return values[name]; }).forEach(function (name) {
                var field = Biblia.element('label');
                field.appendChild(Biblia.element('span', null, Biblia.filterNames[name]));
                var select = Biblia.element('select');
                select.name = name;
                select.appendChild(new Option('Qualquer', ''));
                values[name].forEach(function (value) {
                    select.appendChild(new Option(Biblia.label(name, value), value, false, value === criteria[name]));
                });
                field.appendChild(select);
                grid.appendChild(field);
            });
        });

        document.getElementById('filters').addEventListener('submit', function (event) {
            event.preventDefault();
            var changes = {book: bookSelect.value};
            FILTERS.forEach(function (name) {
                var select = grid.querySelector('select[name="' + name + '"]');
                changes[name] = select ? select.value : '';
            });
            location.href = pageUrl(changes);
        });
        var clear = {book: ''};
        FILTERS.forEach(function (name) {
            clear[name] = '';
        });
        document.getElementById('clear-filters').href = pageUrl(clear);
    }

    function renderByBook(result) {
        var body = document.getElementById('by-book');
        result.byBook.forEach(function (row) {
            var tr = Biblia.element('tr');
            var nameCell = Biblia.element('td');
            var link = Biblia.element('a', null, row.bookName);
            link.href = pageUrl({book: row.bookCode});
            nameCell.appendChild(link);
            tr.appendChild(nameCell);
            tr.appendChild(Biblia.element('td', 'num', row.occurrences.toLocaleString('pt-BR')));
            tr.appendChild(Biblia.element('td', 'num', row.verses.toLocaleString('pt-BR')));
            body.appendChild(tr);
        });
    }

    function renderHits(result) {
        var list = document.getElementById('hits');
        result.hits.forEach(function (hit) {
            if (hit.bookCode !== currentBook) {
                currentBook = hit.bookCode;
                var heading = Biblia.element('li', 'hits__book');
                heading.appendChild(Biblia.element('h3', null, hit.bookName));
                list.appendChild(heading);
            }
            var item = Biblia.element('li', 'hit');
            var ref = Biblia.element('a', 'hit__ref', hit.bookName + ' ' + hit.chapter + ':' + hit.verse);
            // The link opens the corresponding verse in the reader's edition (official one by default).
            var firstParallel = hit.parallel[0];
            ref.href = Biblia.readingUrl(hit.parallelEditionCode, hit.bookCode,
                firstParallel ? firstParallel.chapter : hit.chapter) + (firstParallel ? '#v' + firstParallel.verse : '');
            ref.addEventListener('click', function (event) {
                event.preventDefault();
                Biblia.openVerse(editionCode, hit.bookCode, hit.chapter, hit.verse);
            });
            item.appendChild(ref);

            var original = Biblia.element('p', 'hit__original');
            original.lang = language;
            original.dir = direction;
            hit.tokens.forEach(function (token) {
                var word = hit.matchedPositions.indexOf(token.position) >= 0
                    ? Biblia.element('mark', null, token.surface)
                    : document.createTextNode(token.surface);
                original.appendChild(word);
                original.appendChild(document.createTextNode(token.trailing));
                if (!/[\s\u05BE]$/.test(token.trailing)) {
                    original.appendChild(document.createTextNode(' '));
                }
            });
            item.appendChild(original);

            var translation = Biblia.element('p', 'hit__translation');
            translation.lang = 'pt-BR';
            if (hit.parallel.length === 0) {
                translation.textContent = 'Este versículo não tem correspondente na Bíblia Livre.';
            }
            var renumbered = hit.parallel.length > 1 || hit.parallel.some(function (p) {
                return p.chapter !== hit.chapter || p.verse !== hit.verse;
            });
            hit.parallel.forEach(function (p, i) {
                if (i > 0) {
                    translation.appendChild(document.createTextNode(' '));
                }
                if (renumbered) {
                    translation.appendChild(Biblia.element('span', 'hit__parallel-ref',
                        (p.chapter !== hit.chapter ? p.chapter + ':' : '') + p.verse + ' '));
                }
                translation.appendChild(document.createTextNode(p.content));
            });
            item.appendChild(translation);
            list.appendChild(item);
        });

        offset += result.hits.length;
        var more = document.getElementById('load-more');
        more.hidden = offset >= result.counts.verses;
    }

    if (!criteria.lexeme && !criteria.form) {
        Biblia.showError('Informe um verbete ou uma forma. Comece selecionando uma palavra na leitura.');
        return;
    }

    Biblia.getJson(apiUrl(0))
        .then(function (result) {
            renderHeader(result);
            renderFilters(result);
            renderByBook(result);
            renderHits(result);
        })
        .catch(function (error) {
            Biblia.showError('Não foi possível carregar a concordância (' + error.message + ').');
        });

    document.getElementById('load-more').addEventListener('click', function () {
        var button = this;
        button.disabled = true;
        Biblia.getJson(apiUrl(offset))
            .then(renderHits)
            .catch(function (error) {
                Biblia.showError('Não foi possível carregar mais resultados (' + error.message + ').');
            })
            .finally(function () {
                button.disabled = false;
            });
    });
})();
