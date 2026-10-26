(function () {
    var query = new URLSearchParams(location.search).get('q') || '';
    var input = document.getElementById('site-search-input');
    input.value = query;

    // Strength of a co-occurrence (Dice coefficient), with the number shown next to it.
    function strength(dice) {
        if (dice >= 0.5) {
            return 'forte';
        }
        return dice >= 0.25 ? 'média' : 'fraca';
    }

    function withoutAccents(text) {
        return text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
    }

    function lemmaCell(lemma, languageCode) {
        var cell = Biblia.element('td');
        var text = Biblia.element('span', 'search-lemma', lemma);
        text.lang = Biblia.languageTag(languageCode);
        cell.appendChild(text);
        cell.appendChild(Biblia.element('span', 'search-language', ' ' + Biblia.languageName(languageCode)));
        return cell;
    }

    function concordanceCell(editionCode, lexemeId) {
        var cell = Biblia.element('td');
        var link = Biblia.element('a', null, 'Ocorrências');
        link.href = Biblia.concordanceUrl(editionCode, {lexeme: lexemeId});
        cell.appendChild(link);
        return cell;
    }

    function table(caption, headers) {
        var wrap = Biblia.element('div', 'table-wrap');
        var tableEl = Biblia.element('table', 'data-table');
        tableEl.appendChild(Biblia.element('caption', 'search-caption', caption));
        var head = Biblia.element('thead');
        var row = Biblia.element('tr');
        headers.forEach(function (h) {
            var th = Biblia.element('th', h.num ? 'num' : null, h.label);
            th.scope = 'col';
            row.appendChild(th);
        });
        head.appendChild(row);
        tableEl.appendChild(head);
        var body = Biblia.element('tbody');
        tableEl.appendChild(body);
        wrap.appendChild(tableEl);
        return {wrap: wrap, body: body};
    }

    function renderGlosses(result) {
        var box = document.getElementById('gloss-results');
        [[result.oldTestament, result.oldTestamentGlosses, 'Antigo Testamento'],
            [result.newTestament, result.newTestamentGlosses, 'Novo Testamento']].forEach(function (group) {
            var edition = group[0];
            var matches = group[1];
            if (matches.length === 0) {
                box.appendChild(Biblia.element('p', 'search-empty',
                    group[2] + ': nenhuma tradução em inglês contém "' + result.query + '".'));
                return;
            }
            var t = table(group[2] + ' (' + edition.name + ')', [
                {label: 'Verbete'}, {label: 'Em inglês'}, {label: 'Ocorrências com essas traduções', num: true},
                {label: 'Versículos com o verbete', num: true}, {label: ''}]);
            matches.forEach(function (m) {
                var row = Biblia.element('tr');
                row.appendChild(lemmaCell(m.lemma, m.languageCode));
                var glosses = Biblia.element('td', null, Biblia.englishText(m.matchedGlosses.slice(0, 6))
                    + (m.matchedGlosses.length > 6 ? ', ...' : ''));
                glosses.lang = 'en';
                row.appendChild(glosses);
                row.appendChild(Biblia.element('td', 'num', m.matchedOccurrences.toLocaleString('pt-BR')));
                row.appendChild(Biblia.element('td', 'num', m.lemmaVerses.toLocaleString('pt-BR')));
                row.appendChild(concordanceCell(edition.code, m.lexemeId));
                t.body.appendChild(row);
            });
            box.appendChild(t.wrap);
        });
        document.getElementById('gloss-section').hidden = false;
    }

    function renderAssociations(result) {
        var box = document.getElementById('association-results');
        [[result.oldTestament, result.oldTestamentAssociations, 'Antigo Testamento'],
            [result.newTestament, result.newTestamentAssociations, 'Novo Testamento']].forEach(function (group) {
            var edition = group[0];
            var rows = group[1];
            if (rows.length === 0) {
                box.appendChild(Biblia.element('p', 'search-empty',
                    group[2] + ': a palavra não aparece na Bíblia Livre ou aparece em poucos versículos.'));
                return;
            }
            var t = table(group[2] + ' (' + edition.name + '), em ' + rows[0].wordVerses.toLocaleString('pt-BR')
                + ' versículos correspondentes', [
                {label: 'Verbete'}, {label: 'Em inglês'}, {label: 'Versículos em comum', num: true},
                {label: 'Associação'}, {label: ''}]);
            rows.forEach(function (a) {
                var row = Biblia.element('tr');
                row.appendChild(lemmaCell(a.lemma, a.languageCode));
                var glosses = Biblia.element('td', null, Biblia.englishText(a.glosses));
                glosses.lang = 'en';
                row.appendChild(glosses);
                row.appendChild(Biblia.element('td', 'num', a.together.toLocaleString('pt-BR') + ' de '
                    + a.lemmaVerses.toLocaleString('pt-BR')));
                row.appendChild(Biblia.element('td', 'search-strength search-strength--' + strength(a.dice)
                        .replace('é', 'e'),
                    strength(a.dice) + ' (' + a.dice.toFixed(2).replace('.', ',') + ')'));
                row.appendChild(concordanceCell(edition.code, a.lexemeId));
                t.body.appendChild(row);
            });
            box.appendChild(t.wrap);
        });
        box.appendChild(Biblia.element('p', 'article__caption',
            '"Versículos em comum": em quantos dos versículos originais com o verbete a Bíblia Livre usa a palavra '
            + 'no versículo correspondente. Associação: coeficiente de Dice entre os dois conjuntos de '
            + 'versículos (1 = sempre juntos).'));
        document.getElementById('association-section').hidden = false;
    }

    function renderVerses(result) {
        var list = document.getElementById('verse-results');
        var target = withoutAccents(result.query);
        var single = result.query.indexOf(' ') < 0;
        document.getElementById('verses-summary').textContent = result.verseCount === 0
            ? 'A palavra não aparece na Bíblia Livre.'
            : Biblia.plural(result.verseCount, 'versículo', 'versículos') + ' com a palavra (sem diferenciar acentos '
                + 'e maiúsculas, incluindo plural em s ou es)'
                + (result.verseCount > result.verses.length ? '; mostrando os ' + result.verses.length + ' primeiros.'
                    : '.');
        result.verses.forEach(function (v) {
            var item = Biblia.element('li', 'hit');
            var ref = Biblia.element('a', 'hit__ref', v.bookName + ' ' + v.chapter + ':' + v.verse);
            ref.href = Biblia.readingUrl(result.translation.code, v.bookCode, v.chapter) + '#v' + v.verse;
            ref.addEventListener('click', function (event) {
                event.preventDefault();
                Biblia.openVerse(result.translation.code, v.bookCode, v.chapter, v.verse);
            });
            item.appendChild(ref);
            var text = Biblia.element('p', 'hit__translation');
            text.lang = 'pt-BR';
            v.content.split(/(\s+)/).forEach(function (piece) {
                var bare = withoutAccents(piece.replace(/[^\p{L}\p{M}'-]/gu, ''));
                var match = single && (bare === target || bare === target + 's' || bare === target + 'es');
                text.appendChild(match ? Biblia.element('mark', null, piece) : document.createTextNode(piece));
            });
            item.appendChild(text);
            list.appendChild(item);
        });
        document.getElementById('verses-section').hidden = false;
    }

    if (!query.trim()) {
        Biblia.showError('Digite uma palavra no campo de busca.');
        return;
    }
    document.getElementById('search-title').textContent = 'Busca: ' + query;
    document.title = query + ' - Busca';
    Biblia.getJson('/api/search?q=' + encodeURIComponent(query))
        .then(function (result) {
            renderGlosses(result);
            renderAssociations(result);
            renderVerses(result);
        })
        .catch(function (error) {
            Biblia.showError(error.message === 'HTTP 400'
                ? 'Use uma palavra ou expressão curta, só com letras (até 40).'
                : 'Não foi possível buscar (' + error.message + ').');
        });
})();
