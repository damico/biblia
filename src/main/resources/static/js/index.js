(function () {
    var editionCode = document.body.dataset.edition;
    // Without an explicit edition in the address, show the reader's edition (the official one by default).
    if (!new URLSearchParams(location.search).has('edition') && Biblia.readingEdition() !== editionCode) {
        location.replace(Biblia.url('/?edition=') + encodeURIComponent(Biblia.readingEdition()));
        return;
    }
    var api = '/api/editions/' + encodeURIComponent(editionCode);

    function renderBooks(books) {
        var lists = {OT: document.getElementById('books-ot'), NT: document.getElementById('books-nt')};
        books.forEach(function (book) {
            var item = Biblia.element('li');
            var details = Biblia.element('details');
            details.id = 'book-' + book.code;
            details.appendChild(Biblia.element('summary', null, book.name));

            var chapters = Biblia.element('ol', 'chapter-links');
            chapters.setAttribute('aria-label', 'Capítulos de ' + book.name);
            for (var n = 1; n <= book.chapterCount; n++) {
                var link = Biblia.element('a', null, String(n));
                link.href = Biblia.readingUrl(editionCode, book.code, n);
                link.setAttribute('aria-label', book.name + ' ' + n);
                var chapterItem = Biblia.element('li');
                chapterItem.appendChild(link);
                chapters.appendChild(chapterItem);
            }
            details.appendChild(chapters);
            item.appendChild(details);
            lists[book.testament].appendChild(item);
        });
        Object.keys(lists).forEach(function (testament) {
            lists[testament].closest('section').hidden = lists[testament].children.length === 0;
        });
        // Coming from the book name of a chapter: that book is open, with its chapters in view.
        var target = location.hash ? document.getElementById(decodeURIComponent(location.hash.slice(1))) : null;
        if (target && target.tagName === 'DETAILS') {
            target.open = true;
            target.scrollIntoView({block: 'start'});
        }
    }

    function renderEditions(editions) {
        var select = document.getElementById('edition-select');
        Biblia.sortEditions(editions).forEach(function (edition) {
            select.appendChild(new Option(edition.name, edition.code, false, edition.code === editionCode));
        });
        select.addEventListener('change', function () {
            Biblia.setReadingEdition(select.value);
            location.href = Biblia.url('/?edition=') + encodeURIComponent(select.value);
        });
    }

    Biblia.getJson('/api/editions').then(renderEditions).catch(function () {
        // edition switch is optional
    });
    Biblia.getJson(api).then(Biblia.renderEdition).catch(function () {
        Biblia.showError('Não foi possível carregar a edição.');
    });
    Biblia.getJson(api + '/books').then(renderBooks).catch(function (error) {
        Biblia.showError('Erro ao carregar os livros: ' + error.message);
    });
})();
