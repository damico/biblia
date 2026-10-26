// Shared helpers: JSON fetch, preferences panel and edition credits.
var Biblia = (function () {
    var root = document.documentElement;

    // Context path of the application ("/biblia"), rendered by the server; every root-relative
    // path (pages and API) goes through url().
    var BASE = document.querySelector('meta[name="app-base"]').content.replace(/\/$/, '');

    function url(path) {
        return BASE + path;
    }

    function getJson(path) {
        return fetch(url(path)).then(function (response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        });
    }

    function showError(message) {
        var box = document.getElementById('load-error');
        box.textContent = message;
        box.hidden = false;
    }

    function element(tag, className, text) {
        var el = document.createElement(tag);
        if (className) {
            el.className = className;
        }
        if (text !== undefined && text !== null) {
            el.textContent = text;
        }
        return el;
    }

    function readingUrl(editionCode, bookCode, chapter) {
        return url('/read/') + encodeURIComponent(editionCode) + '/' + encodeURIComponent(bookCode) + '/' + chapter;
    }

    // The Biblia Livre is the official edition. The reading edition changes only when the reader picks
    // another one in the edition selector; links between pages always lead to it.
    var OFFICIAL_EDITION = 'blivre-n4';
    var readingBooksRequest = null;

    function readingEdition() {
        try {
            return localStorage.getItem('reading-edition') || OFFICIAL_EDITION;
        } catch (e) {
            return OFFICIAL_EDITION;
        }
    }

    function setReadingEdition(code) {
        try {
            localStorage.setItem('reading-edition', code);
        } catch (e) {
            // storage unavailable, the official edition stays the default
        }
    }

    function correspondenceUrl(editionCode, bookCode, chapter, verse, target) {
        return '/api/editions/' + encodeURIComponent(editionCode) + '/books/' + encodeURIComponent(bookCode)
            + '/chapters/' + chapter + '/verses/' + verse + '/correspondence?target=' + encodeURIComponent(target);
    }

    /** Opens a verse of any edition in the reading edition (the official one when the reading edition
     *  lacks the book), going through the verse correspondence; never changes the reading edition. */
    function openVerse(editionCode, bookCode, chapter, verse) {
        var edition = readingEdition();
        if (!readingBooksRequest) {
            readingBooksRequest = getJson('/api/editions/' + encodeURIComponent(edition) + '/books')
                .catch(function () { return []; });
        }
        readingBooksRequest.then(function (books) {
            var target = books.some(function (b) { return b.code === bookCode; }) ? edition : OFFICIAL_EDITION;
            if (target === editionCode || !verse) {
                location.href = readingUrl(target, bookCode, chapter) + (verse ? '#v' + verse : '');
                return;
            }
            getJson(correspondenceUrl(editionCode, bookCode, chapter, verse, target))
                .then(function (result) {
                    var first = result.passages[0];
                    location.href = first
                        ? readingUrl(target, bookCode, first.chapter) + '#v' + first.verse
                        : readingUrl(target, bookCode, chapter);
                })
                .catch(function () {
                    location.href = readingUrl(target, bookCode, chapter);
                });
        });
    }

    /** Official edition first, then the others in the order received. */
    function sortEditions(editions) {
        return editions.slice().sort(function (a, b) {
            return (b.code === OFFICIAL_EDITION) - (a.code === OFFICIAL_EDITION);
        });
    }

    function concordanceUrl(editionCode, params) {
        var query = Object.keys(params)
            .filter(function (key) { return params[key] !== null && params[key] !== undefined && params[key] !== ''; })
            .map(function (key) { return encodeURIComponent(key) + '=' + encodeURIComponent(params[key]); })
            .join('&');
        return url('/concordance/') + encodeURIComponent(editionCode) + (query ? '?' + query : '');
    }

    // BCP 47 tag for an ISO 639-3 language code of the database.
    var LANGUAGE_TAGS = {por: 'pt-BR', grc: 'grc', hbo: 'hbo', arc: 'arc', syc: 'syc'};

    function languageTag(code) {
        return LANGUAGE_TAGS[code] || code;
    }

    // Portuguese labels for the morphology values of the sources (MACULA Greek).
    var LABELS = {
        partOfSpeech: {
            noun: 'substantivo', verb: 'verbo', det: 'artigo', conj: 'conjunção', pron: 'pronome',
            prep: 'preposição', adj: 'adjetivo', adv: 'advérbio', ptcl: 'partícula', num: 'numeral',
            intj: 'interjeição', preposition: 'preposição', particle: 'partícula', conjunction: 'conjunção',
            adjective: 'adjetivo', pronoun: 'pronome', adverb: 'advérbio', suffix: 'sufixo'
        },
        person: {first: '1ª pessoa', second: '2ª pessoa', third: '3ª pessoa'},
        number: {singular: 'singular', plural: 'plural', dual: 'dual'},
        gender: {masculine: 'masculino', feminine: 'feminino', neuter: 'neutro', common: 'comum', both: 'ambos'},
        grammaticalCase: {
            nominative: 'nominativo', genitive: 'genitivo', dative: 'dativo', accusative: 'acusativo',
            vocative: 'vocativo'
        },
        tense: {
            present: 'presente', imperfect: 'imperfeito', future: 'futuro', aorist: 'aoristo',
            perfect: 'perfeito', pluperfect: 'mais-que-perfeito'
        },
        voice: {active: 'ativa', middle: 'média', passive: 'passiva', middlepassive: 'média ou passiva'},
        mood: {
            indicative: 'indicativo', subjunctive: 'subjuntivo', optative: 'optativo', imperative: 'imperativo',
            infinitive: 'infinitivo', participle: 'particípio'
        },
        degree: {comparative: 'comparativo', superlative: 'superlativo'},
        // Hebrew and Aramaic stems (binyanim) keep their technical names.
        stem: {},
        verbForm: {
            'participle active': 'particípio ativo', 'participle passive': 'particípio passivo',
            'infinitive construct': 'infinitivo construto', 'infinitive absolute': 'infinitivo absoluto',
            imperative: 'imperativo', jussive: 'jussivo', cohortative: 'coortativo'
        },
        state: {absolute: 'absoluto', construct: 'construto', determined: 'enfático'}
    };

    var FILTER_NAMES = {
        person: 'Pessoa', number: 'Número', gender: 'Gênero', grammaticalCase: 'Caso', tense: 'Tempo',
        voice: 'Voz', mood: 'Modo', stem: 'Tronco (binyan)', verbForm: 'Forma verbal', state: 'Estado'
    };

    var LANGUAGE_NAMES = {grc: 'grego', hbo: 'hebraico', arc: 'aramaico', por: 'português'};

    function languageName(code) {
        return LANGUAGE_NAMES[code] || code;
    }

    // Strong numbers are stored without prefix; Hebrew ones may have leading zeros and a letter (0871a).
    function strongLabel(strong, languageCode) {
        if (!strong) {
            return null;
        }
        return (languageCode === 'grc' ? 'G' : 'H') + strong.replace(/^0+/, '');
    }

    function label(attribute, value) {
        if (!value) {
            return '';
        }
        return (LABELS[attribute] && LABELS[attribute][value]) || value;
    }

    function plural(count, singular, pluralForm) {
        return count.toLocaleString('pt-BR') + ' ' + (count === 1 ? singular : pluralForm);
    }

    function savePrefs() {
        var prefs = {
            theme: root.getAttribute('data-theme'),
            font: root.getAttribute('data-reading-font'),
            size: root.getAttribute('data-reading-size')
        };
        try {
            localStorage.setItem('reading-prefs', JSON.stringify(prefs));
        } catch (e) {
            // storage unavailable, preferences last only for this page
        }
    }

    function initPreferences() {
        var toggle = document.querySelector('.pref-toggle');
        var panel = document.getElementById('reading-prefs');
        var attributes = {theme: 'data-theme', font: 'data-reading-font', size: 'data-reading-size'};

        toggle.addEventListener('click', function () {
            var open = panel.hidden;
            panel.hidden = !open;
            toggle.setAttribute('aria-expanded', String(open));
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !panel.hidden) {
                panel.hidden = true;
                toggle.setAttribute('aria-expanded', 'false');
                toggle.focus();
            }
        });

        Object.keys(attributes).forEach(function (name) {
            var current = root.getAttribute(attributes[name]);
            panel.querySelectorAll('input[name="' + name + '"]').forEach(function (input) {
                input.checked = input.value === current;
                input.addEventListener('change', function () {
                    root.setAttribute(attributes[name], input.value);
                    if (name === 'theme') {
                        root.setAttribute('data-bs-theme', input.value === 'dark' ? 'dark' : 'light');
                    }
                    savePrefs();
                });
            });
        });
    }

    function renderEdition(edition) {
        document.getElementById('edition-name').textContent = edition.name + ' - versão ' + edition.version;
    }

    initPreferences();

    // A verbete (lemma) with its most common English glosses beside it. The source writes multiword glosses
    // with dots ("he.created"), shown here with spaces.
    function verbete(lemma, lang, glosses) {
        var box = element('span', 'verbete');
        var text = element('span', 'verbete__lemma', lemma);
        text.lang = lang;
        box.appendChild(text);
        if (glosses && glosses.length > 0) {
            var english = element('span', 'verbete__english', 'Em inglês: ');
            var words = element('span', null, englishText(glosses));
            words.lang = 'en';
            english.appendChild(words);
            box.appendChild(english);
        }
        return box;
    }

    function englishText(glosses) {
        return glosses.map(function (g) { return g.replace(/\./g, ' '); }).join(', ');
    }

    return {
        url: url,
        getJson: getJson,
        showError: showError,
        element: element,
        readingUrl: readingUrl,
        concordanceUrl: concordanceUrl,
        officialEdition: OFFICIAL_EDITION,
        readingEdition: readingEdition,
        setReadingEdition: setReadingEdition,
        correspondenceUrl: correspondenceUrl,
        openVerse: openVerse,
        sortEditions: sortEditions,
        languageTag: languageTag,
        languageName: languageName,
        strongLabel: strongLabel,
        labels: LABELS,
        filterNames: FILTER_NAMES,
        label: label,
        plural: plural,
        renderEdition: renderEdition,
        verbete: verbete,
        englishText: englishText
    };
})();
