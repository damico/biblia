(function () {
    var data = document.body.dataset;
    var editionCode = data.edition;
    var bookCode = data.book;
    var chapterNumber = parseInt(data.chapter, 10);
    var api = '/api/editions/' + encodeURIComponent(editionCode);

    var noteCount = 0;
    var hasAdded = false;
    var hasTokens = false;
    var hasAlignment = false;
    var isTranslation = false;
    var translationEditionName = '';
    var chapterBookName = '';

    function noteLabel(n) {
        // 1 -> a, 26 -> z, 27 -> aa
        var label = '';
        while (n > 0) {
            n--;
            label = String.fromCharCode(97 + (n % 26)) + label;
            n = Math.floor(n / 26);
        }
        return label;
    }

    function addNote(container, passage, annotation) {
        noteCount++;
        var label = noteLabel(noteCount);

        var sup = Biblia.element('sup');
        var ref = Biblia.element('a', 'note-ref', label);
        ref.href = '#note-' + noteCount;
        ref.id = 'ref-' + noteCount;
        ref.setAttribute('aria-label', 'Nota ' + label);
        sup.appendChild(ref);
        container.appendChild(sup);

        var item = Biblia.element('li');
        item.id = 'note-' + noteCount;
        item.appendChild(Biblia.element('span', 'note-label', label + ' '));
        item.appendChild(Biblia.element('span', 'note-verse', 'v. ' + passage.verse + ' '));
        if (annotation.kind === 'reference') {
            item.appendChild(Biblia.element('span', 'note-kind', 'Citação: '));
        } else if (annotation.kind === 'crossref') {
            item.appendChild(Biblia.element('span', 'note-kind', 'Ver: '));
        }
        if (annotation.keyText) {
            item.appendChild(Biblia.element('em', null, annotation.keyText + ': '));
        }
        item.appendChild(document.createTextNode(annotation.body + ' '));
        var back = Biblia.element('a', 'note-back', 'voltar ao texto');
        back.href = '#ref-' + noteCount;
        item.appendChild(back);
        document.getElementById('notes-list').appendChild(item);
    }

    // Offsets are code points, so the content is walked as an array of code points.
    function renderContent(paragraph, passage) {
        var chars = Array.from(passage.content);
        var container = paragraph;
        var buffer = '';

        function flush() {
            if (buffer) {
                container.appendChild(document.createTextNode(buffer));
                buffer = '';
            }
        }

        for (var i = 0; i <= chars.length; i++) {
            passage.annotations.forEach(function (a) {
                if (a.kind === 'added' && a.endOffset === i && container !== paragraph) {
                    flush();
                    container = paragraph;
                }
            });
            passage.annotations.forEach(function (a) {
                if (a.kind !== 'added' && a.startOffset === i) {
                    flush();
                    addNote(container, passage, a);
                }
            });
            passage.annotations.forEach(function (a) {
                if (a.kind === 'added' && a.startOffset === i && a.endOffset > i) {
                    flush();
                    var em = Biblia.element('em', 'added');
                    paragraph.appendChild(em);
                    container = em;
                    hasAdded = true;
                }
            });
            if (i < chars.length) {
                buffer += chars[i];
            }
        }
        flush();
    }

    // Original language text: every word is a button that opens the word panel.
    function renderTokens(paragraph, passage) {
        passage.tokens.forEach(function (token) {
            // Keeps the word and its punctuation on the same line.
            var group = Biblia.element('span', 'word-group');
            var word = Biblia.element('button', 'word', token.surface);
            word.type = 'button';
            word.dataset.tokenId = String(token.id);
            group.appendChild(word);
            var punctuation = token.trailing.trim();
            if (punctuation) {
                group.appendChild(document.createTextNode(punctuation));
            }
            paragraph.appendChild(group);
            // A Hebrew maqaf joins the next word without a space.
            if (!/\u05BE$/.test(punctuation)) {
                paragraph.appendChild(document.createTextNode(' '));
            }
        });
        hasTokens = true;
    }

    // Translation with a word alignment: aligned words open the original word in the panel.
    function renderAlignedTokens(paragraph, passage) {
        passage.tokens.forEach(function (token) {
            var node;
            if (token.alignedTo.length > 0) {
                node = Biblia.element('button', 'word pt-aligned', token.surface);
                node.type = 'button';
                node.dataset.tokenId = String(token.alignedTo[0]);
                node.dataset.translationWord = token.surface;
                hasAlignment = true;
            } else {
                node = document.createTextNode(token.surface);
            }
            paragraph.appendChild(node);
            if (token.trailing) {
                paragraph.appendChild(document.createTextNode(token.trailing));
            }
        });
    }

    // Interlinear line for the panel: each original word over its English gloss. The source writes
    // multiword glosses with dots ("he.created"), shown here with spaces.
    function renderInterlinear(container, passage) {
        passage.tokens.forEach(function (token) {
            var cell = Biblia.element('span', 'interlinear__cell');
            var word = Biblia.element('button', 'word', token.surface);
            word.type = 'button';
            word.dataset.tokenId = String(token.id);
            cell.appendChild(word);
            var gloss = Biblia.element('span', 'interlinear__gloss',
                token.gloss ? token.gloss.replace(/\./g, ' ') : '-');
            gloss.lang = 'en';
            gloss.dir = 'ltr';
            cell.appendChild(gloss);
            container.appendChild(cell);
            container.appendChild(document.createTextNode(' '));
        });
    }

    var selectedWord = null;
    var panel = document.getElementById('word-panel');
    var panelBody = document.getElementById('word-panel-body');

    function closePanel() {
        panel.hidden = true;
        document.body.classList.remove('has-word-panel');
        if (selectedWord) {
            selectedWord.classList.remove('is-selected');
            selectedWord.removeAttribute('aria-pressed');
            if (selectedWord.tagName === 'BUTTON') {
                selectedWord.focus();
            }
            selectedWord = null;
        }
    }

    function detailRow(list, term, value, lang) {
        if (!value) {
            return;
        }
        list.appendChild(Biblia.element('dt', null, term));
        var dd = Biblia.element('dd', null, value);
        if (lang) {
            dd.lang = lang;
        }
        list.appendChild(dd);
    }

    function countsText(counts) {
        return Biblia.plural(counts.occurrences, 'ocorrência', 'ocorrências') + ' em '
            + Biblia.plural(counts.verses, 'versículo', 'versículos') + ' de '
            + Biblia.plural(counts.books, 'livro', 'livros');
    }

    // backToVerse: shows a button that returns to the original verse the word was picked from.
    function renderWord(detail, translationWord, backToVerse) {
        var lang = Biblia.languageTag(detail.languageCode);
        panelBody.textContent = '';

        if (backToVerse) {
            var back = Biblia.element('button', 'link-button', 'Voltar ao versículo original');
            back.type = 'button';
            back.addEventListener('click', function () {
                showOriginalVerse(verseContext);
            });
            panelBody.appendChild(back);
        }
        if (translationWord) {
            panelBody.appendChild(Biblia.element('p', 'word-panel__alignment',
                'Em ' + translationEditionName + ', "' + translationWord + '" está alinhada a esta palavra de '
                + detail.editionName + '. O alinhamento foi transferido automaticamente do espanhol (RVR09) '
                + 'e pode conter erros.'));
        }

        var surface = Biblia.element('p', 'word-panel__surface', detail.surface);
        surface.lang = lang;
        panelBody.appendChild(surface);
        panelBody.appendChild(Biblia.element('p', 'word-panel__ref',
            detail.bookName + ' ' + detail.chapter + ':' + detail.verse));

        var segmented = detail.segments.length > 1;
        if (segmented) {
            panelBody.appendChild(Biblia.element('p', 'word-panel__note',
                'Esta palavra escrita tem ' + detail.segments.length + ' partes analisadas separadamente '
                + '(prefixos, raiz e sufixos). Cada parte conta como uma ocorrência do seu verbete.'));
        }
        detail.segments.forEach(function (segment) {
            panelBody.appendChild(renderSegment(detail, segment, segmented));
        });

        var formSection = Biblia.element('section', 'word-panel__counts');
        formSection.appendChild(Biblia.element('h3', null, 'Ocorrências desta forma escrita'));
        formSection.appendChild(Biblia.element('p', null, countsText(detail.formCounts)));
        var formLink = Biblia.element('a', null, 'Ver ocorrências da forma ');
        var formText = Biblia.element('span', null, detail.normalized);
        formText.lang = lang;
        formLink.appendChild(formText);
        formLink.href = Biblia.concordanceUrl(detail.editionCode, {form: detail.normalized});
        formSection.appendChild(formLink);
        panelBody.appendChild(formSection);

        panelBody.appendChild(Biblia.element('p', 'word-panel__note',
            'Definições de dicionário ainda não estão disponíveis. A tradução em inglês é um auxílio de '
            + 'leitura da fonte, não o significado completo da palavra.'));
    }

    function renderSegment(detail, segment, segmented) {
        var lang = Biblia.languageTag(segment.languageCode);
        var section = Biblia.element('section', 'word-panel__segment');
        if (segmented) {
            var title = Biblia.element('h3', 'word-panel__segment-title');
            var text = Biblia.element('span', null, segment.segmentText || '(implícito)');
            text.lang = lang;
            title.appendChild(text);
            title.appendChild(document.createTextNode(' - ' + Biblia.label('partOfSpeech', segment.partOfSpeech)));
            section.appendChild(title);
        }

        var list = Biblia.element('dl', 'word-panel__list');
        list.appendChild(Biblia.element('dt', null, 'Verbete'));
        var verbete = Biblia.element('dd');
        verbete.appendChild(Biblia.verbete(segment.lemma, lang, segment.lemmaGlosses));
        list.appendChild(verbete);
        detailRow(list, 'Transliteração', segment.transliteration);
        if (segment.languageCode !== detail.languageCode) {
            detailRow(list, 'Idioma', Biblia.languageName(segment.languageCode));
        }
        if (!segmented) {
            detailRow(list, 'Classe gramatical', Biblia.label('partOfSpeech', segment.partOfSpeech));
        }
        var morphology = ['stem', 'verbForm', 'person', 'tense', 'voice', 'mood', 'grammaticalCase', 'gender',
            'number', 'state', 'degree']
            .map(function (attribute) { return Biblia.label(attribute, segment[attribute]); })
            .filter(Boolean)
            .join(', ');
        detailRow(list, 'Morfologia desta ocorrência', morphology);
        detailRow(list, 'Código morfológico', segment.morphCode);
        detailRow(list, 'Strong (referência auxiliar)', Biblia.strongLabel(segment.strong, segment.languageCode));
        detailRow(list, 'Em inglês, nesta ocorrência', segment.gloss ? Biblia.englishText([segment.gloss]) : null);
        section.appendChild(list);

        var counts = Biblia.element('div', 'word-panel__counts');
        counts.appendChild(Biblia.element('h3', null, 'Ocorrências do verbete nesta edição'));
        counts.appendChild(Biblia.element('p', null, countsText(segment.lemmaCounts)));
        var lemmaLink = Biblia.element('a', null, 'Ver todas as ocorrências de ');
        var lemmaText = Biblia.element('span', null, segment.lemma);
        lemmaText.lang = lang;
        lemmaLink.appendChild(lemmaText);
        // from: the clicked token, origin of the link map in the concordance
        lemmaLink.href = Biblia.concordanceUrl(detail.editionCode, {lexeme: segment.lexemeId, from: detail.id});
        counts.appendChild(lemmaLink);
        section.appendChild(counts);
        return section;
    }

    function selectInText(element) {
        if (selectedWord) {
            selectedWord.classList.remove('is-selected');
            selectedWord.removeAttribute('aria-pressed');
        }
        selectedWord = element;
        element.classList.add('is-selected');
        if (element.tagName === 'BUTTON') {
            element.setAttribute('aria-pressed', 'true');
        }
    }

    function showPanel(titleText, titleLang) {
        var title = document.getElementById('word-panel-title');
        title.textContent = titleText;
        title.lang = titleLang;
        panelBody.textContent = 'Carregando...';
        panel.hidden = false;
        document.body.classList.add('has-word-panel');
    }

    // inPanel: the word was picked from the original verse shown in the panel.
    function openWord(button, inPanel) {
        if (!inPanel) {
            selectInText(button);
        }
        showPanel(button.textContent, button.closest('[lang]').lang);

        Biblia.getJson('/api/tokens/' + button.dataset.tokenId)
            .then(function (detail) {
                renderWord(detail, button.dataset.translationWord, inPanel);
            })
            .catch(function (error) {
                panelBody.textContent = 'Não foi possível carregar a palavra (' + error.message + ').';
            });
    }

    document.getElementById('chapter-text').addEventListener('click', function (event) {
        var button = event.target.closest('button.word');
        if (button) {
            openWord(button, false);
        }
    });
    panelBody.addEventListener('click', function (event) {
        var button = event.target.closest('button.word');
        if (button) {
            openWord(button, true);
        }
    });
    document.getElementById('word-panel-close').addEventListener('click', closePanel);
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !panel.hidden) {
            closePanel();
        }
    });

    function renderEditions(editions) {
        var select = document.getElementById('edition-select');
        Biblia.sortEditions(editions).forEach(function (edition) {
            var option = Biblia.element('option', null, edition.name);
            option.value = edition.code;
            option.selected = edition.code === editionCode;
            select.appendChild(option);
        });
        select.addEventListener('change', function () {
            Biblia.setReadingEdition(select.value);
            location.href = Biblia.readingUrl(select.value, bookCode, chapterNumber);
        });
    }

    var editionsRequest = Biblia.getJson('/api/editions');

    // Translation text: each word leads to the same verse in an original language edition.
    // There is no word alignment for the translation, so the link is verse level only.
    // preferredEditionCode: the original the translation is aligned to, if any.
    function linkToOriginal(chapter, preferredEditionCode) {
        editionsRequest.then(function (editions) {
            var originals = editions.filter(function (e) {
                return e.languageCode !== chapter.edition.languageCode;
            });
            originals.sort(function (a, b) {
                return (b.code === preferredEditionCode) - (a.code === preferredEditionCode);
            });
            return findEditionWithBook(originals, 0);
        }).then(function (original) {
            if (original) {
                applyOriginalLinks(chapter, original, Boolean(preferredEditionCode));
            }
        }).catch(function () {
            // links to the original are optional
        });
    }

    function findEditionWithBook(editions, index) {
        if (index >= editions.length) {
            return null;
        }
        var edition = editions[index];
        return Biblia.getJson('/api/editions/' + encodeURIComponent(edition.code) + '/books').then(function (books) {
            var hasBook = books.some(function (b) { return b.code === bookCode; });
            return hasBook ? edition : findEditionWithBook(editions, index + 1);
        });
    }

    var originalEdition = null;
    var verseContext = null;

    // Translation words and verse numbers open the same verse of the original in the panel; the page
    // stays in the edition the reader chose. Without a word alignment the reader picks the original word.
    function applyOriginalLinks(chapter, original, aligned) {
        originalEdition = original;
        var wordPattern = /[\p{L}\p{M}]+(?:-[\p{L}\p{M}]+)*/gu;
        document.querySelectorAll('#chapter-text .verse').forEach(function (paragraph) {
            var verse = paragraph.id.substring(1);
            var title = 'Ver ' + chapter.book.name + ' ' + chapterNumber + ':' + verse + ' em ' + original.name;

            // The verse number is the keyboard accessible control; words are a pointer shortcut.
            var number = paragraph.querySelector('.verse-num');
            var button = Biblia.element('button', 'verse-num verse-link', number.textContent);
            button.type = 'button';
            button.title = title;
            button.setAttribute('aria-label', title);
            number.replaceWith(button);
            if (aligned) {
                return;
            }

            var walker = document.createTreeWalker(paragraph, NodeFilter.SHOW_TEXT, {
                acceptNode: function (node) {
                    return node.parentElement.closest('a, sup, button') ? NodeFilter.FILTER_REJECT
                        : NodeFilter.FILTER_ACCEPT;
                }
            });
            var textNodes = [];
            while (walker.nextNode()) {
                textNodes.push(walker.currentNode);
            }
            textNodes.forEach(function (node) {
                var fragment = document.createDocumentFragment();
                var last = 0;
                node.textContent.replace(wordPattern, function (word, index) {
                    fragment.appendChild(document.createTextNode(node.textContent.substring(last, index)));
                    fragment.appendChild(Biblia.element('span', 'pt-word', word));
                    last = index + word.length;
                    return word;
                });
                fragment.appendChild(document.createTextNode(node.textContent.substring(last)));
                node.replaceWith(fragment);
            });
        });

        // Only the aligned translation needs a hint: its alignment was transferred automatically.
        if (aligned) {
            var hint = document.getElementById('original-hint');
            hint.textContent = 'Palavras com sublinhado tracejado estão alinhadas a uma palavra de '
                + original.name + ': clique para ver, ao lado, verbete, morfologia e ocorrências. O alinhamento foi '
                + 'transferido automaticamente do espanhol (RVR09) e pode conter erros. O número do versículo '
                + 'mostra o versículo inteiro no original.';
            hint.hidden = false;
        }
    }

    function openOriginalVerse(trigger) {
        var verse = parseInt(trigger.closest('.verse').id.substring(1), 10);
        var word = trigger.classList.contains('pt-word') ? trigger.textContent : null;
        selectInText(trigger);
        showPanel(word || chapterBookName + ' ' + chapterNumber + ':' + verse, 'pt-BR');
        verseContext = {verse: verse, word: word};
        showOriginalVerse(verseContext);
    }

    function showOriginalVerse(context) {
        panelBody.textContent = 'Carregando...';
        Biblia.getJson(Biblia.correspondenceUrl(editionCode, bookCode, chapterNumber, context.verse,
            originalEdition.code)).then(function (result) {
            panelBody.textContent = '';
            var reference = chapterBookName + ' ' + chapterNumber + ':' + context.verse;
            panelBody.appendChild(Biblia.element('p', 'word-panel__alignment', context.word
                ? 'Em ' + translationEditionName + ', ' + reference + ': "' + context.word + '".'
                : translationEditionName + ', ' + reference + '.'));

            if (result.passages.length === 0) {
                panelBody.appendChild(Biblia.element('p', null, result.method === 'standard'
                    ? 'Este versículo não tem correspondente em ' + result.targetEdition.name
                        + ': o texto não existe nessa edição.'
                    : result.targetEdition.name + ' não tem versículo com a referência ' + reference + '.'));
                return;
            }
            panelBody.appendChild(Biblia.element('p', 'word-panel__note',
                correspondenceText(result, reference, context.verse)));
            result.passages.forEach(function (passage) {
                panelBody.appendChild(Biblia.element('h3', 'word-panel__segment-title',
                    result.targetEdition.name + ', ' + chapterBookName + ' ' + passage.chapter + ':' + passage.verse));
                var text = Biblia.element('div', 'interlinear');
                text.lang = Biblia.languageTag(result.targetEdition.languageCode);
                text.dir = result.targetEdition.scriptDirection;
                renderInterlinear(text, passage);
                panelBody.appendChild(text);
            });
            panelBody.appendChild(Biblia.element('p', 'word-panel__note',
                'Embaixo de cada palavra, a tradução em inglês da fonte: um auxílio de leitura, não a tradução '
                + 'dessa palavra no versículo. Não há alinhamento palavra a palavra para ' + translationEditionName
                + ': escolha acima a palavra original correspondente para ver verbete, morfologia e ocorrências.'));
        }).catch(function (error) {
            panelBody.textContent = 'Não foi possível carregar o original (' + error.message + ').';
        });
    }

    function correspondenceText(result, reference, verseNumber) {
        if (result.method !== 'standard') {
            return 'Versículo com a mesma referência (a tabela de correspondência de versículos não foi '
                + 'calculada para esta edição; a divisão pode diferir).';
        }
        var targets = result.passages.map(function (p) { return p.chapter + ':' + p.verse; }).join(' e ');
        if (result.exact) {
            var same = result.passages[0].chapter === chapterNumber && result.passages[0].verse === verseNumber;
            return same ? 'Correspondência exata de versículo.'
                : 'Correspondência exata de versículo: em ' + result.targetEdition.name + ' este texto é o '
                    + targets + ' (numeração diferente).';
        }
        return 'Correspondência parcial: a divisão de versículos é diferente. O texto de ' + reference
            + ' está distribuído em ' + targets + ' de ' + result.targetEdition.name
            + ', que podem conter também texto de versículos vizinhos.';
    }

    document.getElementById('chapter-text').addEventListener('click', function (event) {
        var trigger = event.target.closest('.pt-word, .verse-link');
        if (trigger) {
            openOriginalVerse(trigger);
        }
    });

    // Sharing: the reader picks verses of the chapter and gets an image of them to copy or save.

    var SHARE_MAX_VERSES = 20;
    var SHARE_WIDTH = 1080;
    var SHARE_PADDING = 96;
    var shareChapter = null;
    var shareTexts = {};
    var sharing = false;
    var sharedVerses = [];
    var lastPickedVerse = null;
    var shareBlob = null;
    var shareFileName = '';
    var shareBar = document.getElementById('share-bar');
    var shareDialog = document.getElementById('share-dialog');
    var shareMessage = document.getElementById('share-message');

    // Plain text of a verse, spaced the way renderTokens and renderAlignedTokens show it.
    function passageText(passage) {
        if (passage.tokens.length > 0 && isTranslation) {
            return passage.tokens.map(function (t) { return t.surface + t.trailing; }).join('').trim();
        }
        if (passage.tokens.length > 0) {
            return passage.tokens.map(function (t) {
                var punctuation = t.trailing.trim();
                return t.surface + punctuation + (/\u05BE$/.test(punctuation) ? '' : ' ');
            }).join('').trim();
        }
        return passage.content;
    }

    // "16-18, 20": consecutive verses become ranges.
    function verseRanges(verses) {
        var parts = [];
        verses.forEach(function (verse, i) {
            if (i > 0 && verse === verses[i - 1] + 1) {
                parts[parts.length - 1].end = verse;
            } else {
                parts.push({start: verse, end: verse});
            }
        });
        return parts.map(function (p) { return p.start === p.end ? String(p.start) : p.start + '-' + p.end; })
            .join(', ');
    }

    function shareReference() {
        return chapterBookName + ' ' + chapterNumber + ':' + verseRanges(sharedVerses);
    }

    function updateShareBar() {
        var status = document.getElementById('share-status');
        var create = document.getElementById('share-create');
        if (sharedVerses.length === 0) {
            status.textContent = 'Toque nos versículos que deseja compartilhar. Com Shift, o clique seleciona '
                + 'um intervalo.';
        } else if (sharedVerses.length > SHARE_MAX_VERSES) {
            status.textContent = shareReference() + ': selecione no máximo ' + SHARE_MAX_VERSES
                + ' versículos para a imagem.';
        } else {
            status.textContent = shareReference() + ' ('
                + Biblia.plural(sharedVerses.length, 'versículo', 'versículos') + ')';
        }
        create.disabled = sharedVerses.length === 0 || sharedVerses.length > SHARE_MAX_VERSES;
    }

    function setVersePicked(paragraph, picked) {
        paragraph.classList.toggle('is-shared', picked);
        paragraph.setAttribute('aria-checked', String(picked));
    }

    function pickVerse(paragraph, range) {
        var verse = parseInt(paragraph.id.substring(1), 10);
        var picked = sharedVerses.indexOf(verse) < 0;
        var verses = [verse];
        if (range && lastPickedVerse !== null) {
            verses = [];
            for (var v = Math.min(lastPickedVerse, verse); v <= Math.max(lastPickedVerse, verse); v++) {
                verses.push(v);
            }
            picked = true;
        }
        verses.forEach(function (v) {
            var target = document.getElementById('v' + v);
            if (!target) {
                return;
            }
            var index = sharedVerses.indexOf(v);
            if (picked && index < 0) {
                sharedVerses.push(v);
            } else if (!picked && index >= 0) {
                sharedVerses.splice(index, 1);
            }
            setVersePicked(target, picked);
        });
        sharedVerses.sort(function (a, b) { return a - b; });
        lastPickedVerse = verse;
        updateShareBar();
    }

    function startSharing() {
        if (!panel.hidden) {
            closePanel();
        }
        stopListening();
        sharing = true;
        document.body.classList.add('is-sharing');
        document.querySelectorAll('#chapter-text .verse').forEach(function (paragraph) {
            paragraph.setAttribute('role', 'checkbox');
            paragraph.setAttribute('aria-checked', 'false');
            paragraph.tabIndex = 0;
        });
        // A verse the reader arrived at (#v16) starts selected.
        var target = document.querySelector('#chapter-text .verse.is-target');
        if (target) {
            pickVerse(target, false);
        }
        updateShareBar();
        shareBar.hidden = false;
        var first = target || document.querySelector('#chapter-text .verse');
        if (first) {
            first.focus({preventScroll: Boolean(target)});
        }
    }

    function stopSharing() {
        sharing = false;
        document.body.classList.remove('is-sharing');
        document.querySelectorAll('#chapter-text .verse').forEach(function (paragraph) {
            paragraph.classList.remove('is-shared');
            paragraph.removeAttribute('role');
            paragraph.removeAttribute('aria-checked');
            paragraph.removeAttribute('tabindex');
        });
        sharedVerses = [];
        lastPickedVerse = null;
        shareBar.hidden = true;
        document.getElementById('share-start').focus();
    }

    // While sharing, a click anywhere in a verse picks it instead of opening words or notes.
    document.getElementById('chapter-text').addEventListener('click', function (event) {
        if (!sharing) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        var paragraph = event.target.closest('.verse');
        if (paragraph) {
            pickVerse(paragraph, event.shiftKey);
        }
    }, true);
    document.getElementById('chapter-text').addEventListener('keydown', function (event) {
        if (sharing && (event.key === ' ' || event.key === 'Enter') && event.target.classList.contains('verse')) {
            event.preventDefault();
            pickVerse(event.target, event.shiftKey);
        }
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && sharing && !shareDialog.open) {
            stopSharing();
        }
    });
    document.getElementById('share-start').addEventListener('click', function () {
        if (sharing) {
            stopSharing();
        } else {
            startSharing();
        }
    });
    document.getElementById('share-cancel').addEventListener('click', stopSharing);
    document.getElementById('share-create').addEventListener('click', createShareImage);
    document.getElementById('share-close').addEventListener('click', function () {
        shareDialog.close();
    });

    function cssValue(name) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    // Lines of runs ({text, number}) that fit the text width at the given font size.
    function layoutShareText(ctx, fonts, size, width) {
        var lines = [];
        var line = [];
        var lineWidth = 0;
        ctx.font = size + 'px ' + fonts.text;
        var space = ctx.measureText(' ').width;
        sharedVerses.forEach(function (verse) {
            var words = shareTexts[verse].split(/\s+/).filter(Boolean);
            if (sharedVerses.length > 1) {
                words.unshift({number: String(verse)});
            }
            words.forEach(function (word) {
                var run = typeof word === 'string' ? {text: word, number: false} : {text: word.number, number: true};
                ctx.font = run.number ? '600 ' + Math.round(size * 0.6) + 'px ' + fonts.ui : size + 'px ' + fonts.text;
                run.width = ctx.measureText(run.text).width;
                if (line.length > 0 && lineWidth + space + run.width > width) {
                    // A verse number moves to the next line with the first word of its verse.
                    var carried = line[line.length - 1].number && line.length > 1 ? line.pop() : null;
                    lines.push(line);
                    line = [];
                    lineWidth = 0;
                    if (carried) {
                        carried.gap = 0;
                        lineWidth = carried.width;
                        line.push(carried);
                    }
                }
                run.gap = line.length === 0 ? 0 : (line[line.length - 1].number ? space * 0.5 : space);
                lineWidth += run.gap + run.width;
                line.push(run);
            });
        });
        if (line.length > 0) {
            lines.push(line);
        }
        return lines;
    }

    function drawShareImage(fonts) {
        var colors = {
            bg: cssValue('--bg'), text: cssValue('--text'), accent: cssValue('--link')
        };
        var rtl = shareChapter.edition.scriptDirection === 'rtl';
        var canvas = document.createElement('canvas');
        var ctx = canvas.getContext('2d');
        var textWidth = SHARE_WIDTH - 2 * SHARE_PADDING;
        var footerHeight = 150;
        var minHeight = SHARE_WIDTH;
        var maxHeight = Math.round(SHARE_WIDTH * 1.25);

        // The largest font size whose text fits a 4:5 image; long passages get a taller image.
        var size = 72;
        var lines = layoutShareText(ctx, fonts, size, textWidth);
        var leading = rtl ? 1.8 : 1.5;
        while (size > 30 && 2 * SHARE_PADDING + lines.length * size * leading + footerHeight > maxHeight) {
            size -= 2;
            lines = layoutShareText(ctx, fonts, size, textWidth);
        }
        var lineHeight = size * leading;
        var textHeight = lines.length * lineHeight;
        var height = Math.max(minHeight, Math.ceil(2 * SHARE_PADDING + textHeight + footerHeight));

        canvas.width = SHARE_WIDTH;
        canvas.height = height;
        ctx.fillStyle = colors.bg;
        ctx.fillRect(0, 0, SHARE_WIDTH, height);

        // Text centered in the space above the footer.
        var top = SHARE_PADDING + Math.max(0, (height - 2 * SHARE_PADDING - footerHeight - textHeight) / 2);
        ctx.textBaseline = 'alphabetic';
        ctx.direction = rtl ? 'rtl' : 'ltr';
        ctx.textAlign = rtl ? 'right' : 'left';
        lines.forEach(function (line, n) {
            var y = top + n * lineHeight + size * 1.1;
            var x = rtl ? SHARE_WIDTH - SHARE_PADDING : SHARE_PADDING;
            line.forEach(function (run) {
                x += rtl ? -run.gap : run.gap;
                if (run.number) {
                    ctx.font = '600 ' + Math.round(size * 0.6) + 'px ' + fonts.ui;
                    ctx.fillStyle = colors.accent;
                    ctx.fillText(run.text, x, y - size * 0.3);
                } else {
                    ctx.font = size + 'px ' + fonts.text;
                    ctx.fillStyle = colors.text;
                    ctx.fillText(run.text, x, y);
                }
                x += rtl ? -run.width : run.width;
            });
        });

        // Footer: rule and reference, always left to right.
        var footerTop = height - SHARE_PADDING - footerHeight + 56;
        ctx.direction = 'ltr';
        ctx.textAlign = 'left';
        ctx.fillStyle = colors.accent;
        ctx.fillRect(SHARE_PADDING, footerTop, 96, 5);
        ctx.fillStyle = colors.text;
        ctx.font = '700 44px ' + fonts.ui;
        ctx.fillText(shareReference(), SHARE_PADDING, footerTop + 76);
        return canvas;
    }

    function createShareImage() {
        var sample = document.getElementById('v' + sharedVerses[0]);
        var fonts = {text: getComputedStyle(sample).fontFamily, ui: cssValue('--font-ui')};
        var sampleText = shareTexts[sharedVerses[0]].substring(0, 40);
        // Web fonts load on demand; the canvas only draws with fonts that are already loaded.
        Promise.all([
            document.fonts.load('48px ' + fonts.text, sampleText),
            document.fonts.load('700 44px ' + fonts.ui, 'Abc')
        ]).catch(function () {
            // fallback fonts are fine
        }).then(function () {
            drawShareImage(fonts).toBlob(showShareDialog, 'image/png');
        });
    }

    function showShareDialog(blob) {
        var reference = shareReference();
        shareBlob = blob;
        shareFileName = reference.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
            .replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') + '.png';
        var preview = document.getElementById('share-preview');
        if (preview.src) {
            URL.revokeObjectURL(preview.src);
        }
        preview.src = URL.createObjectURL(blob);
        preview.alt = 'Imagem de ' + reference + ', ' + shareChapter.edition.name;
        var download = document.getElementById('share-download');
        download.href = preview.src;
        download.download = shareFileName;

        var copy = document.getElementById('share-copy');
        var canCopy = Boolean(navigator.clipboard && navigator.clipboard.write && window.ClipboardItem);
        copy.disabled = !canCopy;
        shareMessage.textContent = canCopy ? ''
            : 'Este navegador só permite copiar imagens em conexão segura (HTTPS). Use "Baixar imagem".';

        var file = new File([blob], shareFileName, {type: 'image/png'});
        document.getElementById('share-send').hidden =
            !(navigator.canShare && navigator.canShare({files: [file]}));
        shareDialog.showModal();
        if (canCopy) {
            copy.focus();
        }
    }

    document.getElementById('share-copy').addEventListener('click', function () {
        // The blob is ready before the click, so the write happens inside the user gesture (Safari).
        navigator.clipboard.write([new ClipboardItem({'image/png': shareBlob})]).then(function () {
            shareMessage.textContent = 'Imagem copiada. Cole no aplicativo em que vai compartilhar.';
        }).catch(function () {
            shareMessage.textContent = 'Não foi possível copiar a imagem. Use "Baixar imagem".';
        });
    });
    document.getElementById('share-send').addEventListener('click', function () {
        var file = new File([shareBlob], shareFileName, {type: 'image/png'});
        navigator.share({files: [file], title: shareReference()}).catch(function () {
            // the reader closed the share sheet
        });
    });

    // Reading aloud: each chapter of the Portuguese editions was recorded beforehand (scripts/generate-audio.sh),
    // with the start and end of each verse, so the verse being read is marked while the audio plays.

    var listenBar = document.getElementById('listen-bar');
    var listenStatus = document.getElementById('listen-status');
    var listenPause = document.getElementById('listen-pause');
    var recording = null;
    var listenAudio = null;
    var listenVerse = null;

    function recordingUrl(extension) {
        return Biblia.url('/audio/') + encodeURIComponent(editionCode) + '/' + encodeURIComponent(bookCode) + '/'
            + chapterNumber + '.' + extension;
    }

    // The button shows only when the chapter was recorded.
    function findRecording() {
        fetch(recordingUrl('json')).then(function (response) {
            return response.ok ? response.json() : null;
        }).then(function (timings) {
            recording = timings;
            document.getElementById('listen-start').hidden = !recording;
        }).catch(function () {
            // reading aloud is optional
        });
    }

    function markSpeaking(verse) {
        var previous = document.querySelector('#chapter-text .verse.is-speaking');
        var paragraph = verse ? document.getElementById('v' + verse) : null;
        if (previous && previous !== paragraph) {
            previous.classList.remove('is-speaking');
        }
        if (!paragraph || paragraph === previous) {
            return;
        }
        paragraph.classList.add('is-speaking');
        var box = paragraph.getBoundingClientRect();
        if (box.top < 0 || box.bottom > window.innerHeight - listenBar.offsetHeight) {
            paragraph.scrollIntoView({block: 'center', behavior: 'smooth'});
        }
    }

    function verseAt(time) {
        var found = null;
        recording.verses.forEach(function (v) {
            if (v.start <= time) {
                found = v;
            }
        });
        return found;
    }

    function showListenStatus() {
        var reference = chapterBookName + ' ' + chapterNumber;
        listenStatus.textContent = listenAudio.paused
            ? 'Pausado: ' + reference + (listenVerse ? ':' + listenVerse : '')
            : 'Lendo ' + reference + (listenVerse ? ':' + listenVerse : '');
        listenPause.textContent = listenAudio.paused ? 'Continuar' : 'Pausar';
    }

    function startListening() {
        if (sharing) {
            stopSharing();
        }
        if (!listenAudio) {
            listenAudio = new Audio(recordingUrl('m4a'));
            listenAudio.preload = 'auto';
            listenAudio.addEventListener('timeupdate', function () {
                var v = verseAt(listenAudio.currentTime);
                var verse = v ? v.verse : null;
                if (verse !== listenVerse) {
                    listenVerse = verse;
                    markSpeaking(verse);
                    showListenStatus();
                }
            });
            listenAudio.addEventListener('play', showListenStatus);
            listenAudio.addEventListener('pause', showListenStatus);
            listenAudio.addEventListener('ended', function () {
                listenVerse = null;
                markSpeaking(null);
                listenStatus.textContent = 'Fim do capítulo.';
                listenPause.textContent = 'Ouvir de novo';
            });
            listenAudio.addEventListener('error', function () {
                listenStatus.textContent = 'Não foi possível carregar o áudio do capítulo.';
            });
        }
        // A verse the reader arrived at (#v16) is where the reading starts.
        var target = document.querySelector('#chapter-text .verse.is-target');
        var start = target ? recording.verses.filter(function (v) { return 'v' + v.verse === target.id; })[0] : null;
        listenAudio.currentTime = start ? start.start : 0;
        listenStatus.textContent = 'Carregando o áudio...';
        listenBar.hidden = false;
        document.body.classList.add('is-listening');
        listenAudio.play().catch(function () {
            listenStatus.textContent = 'O navegador não permitiu tocar o áudio.';
        });
    }

    function stopListening() {
        if (!listenAudio || listenBar.hidden) {
            return;
        }
        listenAudio.pause();
        listenVerse = null;
        markSpeaking(null);
        listenBar.hidden = true;
        document.body.classList.remove('is-listening');
        document.getElementById('listen-start').focus();
    }

    document.getElementById('listen-start').addEventListener('click', function () {
        if (!listenBar.hidden) {
            stopListening();
        } else {
            startListening();
        }
    });
    document.getElementById('listen-stop').addEventListener('click', stopListening);
    listenPause.addEventListener('click', function () {
        if (listenAudio.paused) {
            listenAudio.play();
        } else {
            listenAudio.pause();
        }
    });

    function renderChapter(chapter) {
        var title = chapter.book.name + ' ' + chapter.chapter;
        document.getElementById('chapter-title').textContent = title;
        document.title = title + ' - ' + chapter.edition.name;
        Biblia.renderEdition(chapter.edition);

        isTranslation = chapter.edition.languageCode === 'por';
        chapterBookName = chapter.book.name;
        translationEditionName = chapter.edition.name;
        var text = document.getElementById('chapter-text');
        text.dir = chapter.edition.scriptDirection;
        text.lang = Biblia.languageTag(chapter.edition.languageCode);
        shareChapter = chapter;
        chapter.passages.forEach(function (passage) {
            shareTexts[passage.verse] = passageText(passage);
            if (passage.heading) {
                text.appendChild(Biblia.element('p', 'verse-heading', passage.heading));
            }
            var paragraph = Biblia.element('p', 'verse');
            paragraph.id = 'v' + passage.verse;
            paragraph.appendChild(Biblia.element('span', 'verse-num', String(passage.verse)));
            paragraph.appendChild(document.createTextNode(' '));
            if (passage.tokens.length > 0 && isTranslation) {
                renderAlignedTokens(paragraph, passage);
            } else if (passage.tokens.length > 0) {
                renderTokens(paragraph, passage);
            } else {
                renderContent(paragraph, passage);
            }
            text.appendChild(paragraph);
        });

        document.getElementById('notes-section').hidden = noteCount === 0;
        document.getElementById('added-legend').hidden = !hasAdded;
        document.getElementById('word-hint').hidden = !hasTokens;
        document.getElementById('share-start').hidden = chapter.passages.length === 0;
        findRecording();
        if (isTranslation && hasAlignment) {
            var firstAligned = document.querySelector('#chapter-text .pt-aligned');
            Biblia.getJson('/api/tokens/' + firstAligned.dataset.tokenId).then(function (detail) {
                linkToOriginal(chapter, detail.editionCode);
            });
        } else if (isTranslation) {
            linkToOriginal(chapter, null);
        }
        if (location.hash) {
            var target = document.getElementById(location.hash.substring(1));
            if (target) {
                // :target does not apply to elements rendered after load
                target.classList.add('is-target');
                target.scrollIntoView({block: 'center'});
            }
        }
    }

    function renderNavigation(books) {
        var index = books.findIndex(function (b) { return b.code === bookCode; });
        if (index < 0) {
            return;
        }
        var book = books[index];

        var select = document.getElementById('chapter-select');
        for (var n = 1; n <= book.chapterCount; n++) {
            var option = Biblia.element('option', null, String(n));
            option.value = String(n);
            option.selected = n === chapterNumber;
            select.appendChild(option);
        }
        select.addEventListener('change', function () {
            location.href = Biblia.readingUrl(editionCode, bookCode, select.value);
        });

        var prev = chapterNumber > 1
            ? {book: book, chapter: chapterNumber - 1}
            : (index > 0 ? {book: books[index - 1], chapter: books[index - 1].chapterCount} : null);
        var next = chapterNumber < book.chapterCount
            ? {book: book, chapter: chapterNumber + 1}
            : (index < books.length - 1 ? {book: books[index + 1], chapter: 1} : null);

        [['prev-chapter', prev], ['next-chapter', next]].forEach(function (pair) {
            var link = document.getElementById(pair[0]);
            if (pair[1]) {
                link.href = Biblia.readingUrl(editionCode, pair[1].book.code, pair[1].chapter);
                link.textContent = (pair[0] === 'prev-chapter' ? 'Anterior: ' : 'Próximo: ')
                    + pair[1].book.name + ' ' + pair[1].chapter;
                link.hidden = false;
            }
        });
    }

    Biblia.getJson(api + '/books/' + encodeURIComponent(bookCode) + '/chapters/' + chapterNumber)
        .then(renderChapter)
        .catch(function (error) {
            document.getElementById('chapter-title').textContent = 'Capítulo não encontrado';
            Biblia.showError('Não foi possível carregar o capítulo (' + error.message + ').');
        });
    editionsRequest.then(renderEditions).catch(function () {
        // edition switch is optional
    });
    Biblia.getJson(api + '/books').then(renderNavigation).catch(function () {
        // navigation is optional, the chapter text still loads
    });
})();
