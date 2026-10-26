// Link map of the concordance (D3 for axes, zoom and labels; canvas for the lines).
// The server sends one point per verse on each axis; the lines are built here for the visible part of
// the axes only. When the visible part holds too many pairs, lines are grouped by chapter (or book);
// zooming in turns them into verse to verse lines. The verses of both sides in view are listed below
// the map ("Versículos na área visível") and, when few, written beside the axes.
(function () {
    var HEIGHT = 760;
    var MARGIN_TOP = 36;
    var MARGIN_BOTTOM = 16;
    var BOOK_GAP = 6;
    var MIN_WIDTH = 720;
    var TRANSLATION = Biblia.officialEdition;

    var MAX_VERSE_LINES = 8000;    // above this, lines are grouped by chapter
    var MAX_GROUP_LINES = 6000;    // above this, chapter groups become book groups
    var CARD_LIMIT = 8;            // verse texts beside an axis when at most this many verses are in view
    var REF_LIMIT = 80;            // verse references beside an axis when at most this many verses are in view
    var READING_GROUPS = 12;       // verses listed below the map
    var READING_LINKED = 6;        // linked verses shown under each of them

    var EXPLANATIONS = {
        fan: 'Cada linha sai do versículo em que você clicou e vai até outra ocorrência do mesmo verbete.',
        pairs: 'Cada linha liga uma ocorrência do verbete no livro da esquerda a uma ocorrência no eixo da direita.',
        lxx: 'Cada linha liga uma ocorrência do verbete a um versículo do outro testamento onde a palavra tem o mesmo '
            + 'número Strong grego: no Antigo Testamento, o da palavra que a Septuaginta usou para traduzir o '
            + 'hebraico ou aramaico (equivalência registrada pelo MACULA Hebrew); no Novo, o da própria palavra grega.'
    };

    var editionCode = document.body.dataset.edition;
    var params = new URLSearchParams(location.search);
    var lexeme = params.get('lexeme');
    var from = params.get('from');
    if (!lexeme || typeof d3 === 'undefined') {
        return;
    }

    var section = document.getElementById('links-section');
    var form = document.getElementById('links-form');
    var leftSelect = document.getElementById('links-left');
    var rightSelect = document.getElementById('links-right');
    var canvas = document.getElementById('links-canvas');
    var summary = document.getElementById('links-summary');
    var detail = document.getElementById('links-detail');
    var readingStatus = document.getElementById('links-reading-status');
    var readingBody = document.getElementById('links-reading-body');
    var state = {mode: from ? 'fan' : 'pairs', left: '', right: ''};
    var view = null;
    var textCache = {};

    section.hidden = false;
    form.querySelectorAll('input[name="mode"]').forEach(function (input) {
        if (input.value === 'fan' && !from) {
            input.disabled = true;
            input.parentElement.title = 'Abra a concordância a partir de uma palavra do texto para usar este modo.';
        }
        input.checked = input.value === state.mode;
        input.addEventListener('change', function () {
            state = {mode: input.value, left: '', right: ''};
            load();
        });
    });
    leftSelect.addEventListener('change', function () {
        state.left = leftSelect.value;
        load();
    });
    rightSelect.addEventListener('change', function () {
        state.right = rightSelect.value;
        load();
    });
    document.querySelectorAll('[data-zoom]').forEach(function (button) {
        button.addEventListener('click', function () {
            if (!view) {
                return;
            }
            if (button.dataset.zoom === 'reset') {
                view.reset();
            } else {
                view.zoomBy(button.dataset.zoom, parseFloat(button.dataset.factor));
            }
        });
    });

    function load() {
        document.getElementById('links-explanation').textContent = EXPLANATIONS[state.mode];
        summary.textContent = 'Carregando...';
        detail.hidden = true;
        var query = new URLSearchParams({mode: state.mode, lexeme: lexeme});
        if (from) {
            query.set('from', from);
        }
        if (state.left) {
            query.set('left', state.left);
        }
        if (state.right) {
            query.set('right', state.right);
        }
        Biblia.getJson('/api/editions/' + encodeURIComponent(editionCode) + '/concordance/links?' + query.toString())
            .then(render)
            .catch(function (error) {
                canvas.textContent = '';
                view = null;
                summary.textContent = 'Não foi possível montar o mapa (' + error.message + ').';
            });
    }

    function fillSelects(graph) {
        leftSelect.textContent = '';
        graph.leftBooks.filter(function (b) { return b.occurrences > 0; }).forEach(function (book) {
            leftSelect.appendChild(new Option(book.bookName + ' (' + book.occurrences + ')', book.bookCode,
                false, book.bookCode === graph.leftRange));
        });
        leftSelect.disabled = graph.mode === 'fan';

        rightSelect.textContent = '';
        var testaments = graph.rightBooks.map(function (b) { return b.testament; });
        rightSelect.appendChild(new Option('Toda a edição', 'ALL', false, graph.rightRange === 'ALL'));
        if (testaments.indexOf('OT') >= 0 && testaments.indexOf('NT') >= 0) {
            rightSelect.appendChild(new Option('Antigo Testamento', 'OT', false, graph.rightRange === 'OT'));
            rightSelect.appendChild(new Option('Novo Testamento', 'NT', false, graph.rightRange === 'NT'));
        }
        graph.rightBooks.forEach(function (book) {
            var label = book.bookName + (book.occurrences > 0 ? ' (' + book.occurrences + ')' : '');
            rightSelect.appendChild(new Option(label, book.bookCode, false, book.bookCode === graph.rightRange));
        });
    }

    // Position of every chapter on an axis, in verse units, with a gap between books.
    function axisLayout(chapters) {
        var offsets = {};
        var books = [];
        var total = 0;
        var previous = null;
        chapters.forEach(function (c) {
            if (c.bookCode !== previous) {
                if (previous !== null) {
                    books[books.length - 1].end = total;
                    total += BOOK_GAP;
                }
                books.push({bookCode: c.bookCode, bookName: c.bookName, start: total});
                previous = c.bookCode;
            }
            offsets[c.bookCode + ' ' + c.chapter] = {start: total, verses: c.verses};
            total += c.verses;
        });
        if (books.length > 0) {
            books[books.length - 1].end = total;
        }
        return {
            chapters: chapters,
            books: books,
            total: Math.max(total, 1),
            unit: function (p) {
                var chapter = offsets[p.bookCode + ' ' + p.chapter];
                return chapter ? chapter.start + p.verse - 0.5 : null;
            },
            chapterStart: function (c) {
                return offsets[c.bookCode + ' ' + c.chapter].start;
            },
            chapterMiddle: function (bookCode, chapter) {
                var c = offsets[bookCode + ' ' + chapter];
                return c.start + c.verses / 2;
            },
            bookMiddle: function (bookCode) {
                var b = books.filter(function (x) { return x.bookCode === bookCode; })[0];
                return (b.start + b.end) / 2;
            }
        };
    }

    function textKey(sideEdition, p) {
        return sideEdition + ' ' + p.bookCode + '.' + p.chapter + '.' + p.verse;
    }

    function reference(names, p) {
        return (names[p.bookCode] || p.bookCode) + ' ' + p.chapter + ':' + p.verse;
    }

    // Loads (and caches) the texts of verses not yet known, in requests of at most 200 verses.
    function loadTexts(sideEdition, points) {
        var missing = points.filter(function (p) { return !(textKey(sideEdition, p) in textCache); });
        if (missing.length === 0) {
            return Promise.resolve(false);
        }
        var batches = [];
        for (var i = 0; i < missing.length; i += 200) {
            batches.push(missing.slice(i, i + 200));
        }
        return Promise.all(batches.map(function (batch) {
            batch.forEach(function (p) {
                textCache[textKey(sideEdition, p)] = null;
            });
            var refs = batch.map(function (p) { return p.bookCode + '.' + p.chapter + '.' + p.verse; }).join(',');
            return Biblia.getJson('/api/editions/' + encodeURIComponent(sideEdition) + '/verse-texts?refs='
                + encodeURIComponent(refs) + '&translation=' + encodeURIComponent(TRANSLATION))
                .then(function (texts) {
                    texts.forEach(function (t) {
                        textCache[textKey(sideEdition, t)] = t;
                    });
                })
                .catch(function () {
                    batch.forEach(function (p) {
                        delete textCache[textKey(sideEdition, p)];
                    });
                });
        })).then(function () { return true; });
    }

    // Portuguese of the official edition (by verse correspondence), with its own number when it differs.
    function translationText(text) {
        if (text.translation.length === 0) {
            return 'Sem versículo correspondente na Bíblia Livre.';
        }
        return text.translation.map(function (t) {
            return (t.chapter !== text.chapter || t.verse !== text.verse ? '(' + t.chapter + ':' + t.verse + ') ' : '')
                + t.content;
        }).join(' ');
    }

    function originalLang(sideEdition) {
        return sideEdition === 'wlc' ? {lang: 'hbo', dir: 'rtl'} : {lang: 'grc', dir: 'ltr'};
    }

    // A verse text block: reference, Portuguese and original.
    function verseText(sideEdition, names, p, compact) {
        var block = Biblia.element('div', compact ? 'links-verse links-verse--compact' : 'links-verse');
        block.appendChild(Biblia.element('strong', 'links-verse__ref', reference(names, p)));
        var text = textCache[textKey(sideEdition, p)];
        var pt = Biblia.element('span', 'links-verse__pt', text ? ' ' + translationText(text) : ' Carregando...');
        pt.lang = 'pt-BR';
        block.appendChild(pt);
        if (text) {
            var original = Biblia.element('span', 'links-verse__original', text.content);
            var o = originalLang(sideEdition);
            original.lang = o.lang;
            original.dir = o.dir;
            block.appendChild(original);
        }
        return block;
    }

    function render(graph) {
        fillSelects(graph);
        canvas.textContent = '';
        detail.hidden = true;

        var width = Math.max(canvas.clientWidth, MIN_WIDTH);
        var leftX = Math.round(width * 0.36);
        var rightX = Math.round(width * 0.64);
        var midX = (leftX + rightX) / 2;
        var names = {};
        graph.leftAxis.concat(graph.rightAxis).forEach(function (c) {
            names[c.bookCode] = c.bookName;
        });

        var sides = {
            left: {x: leftX, edition: graph.leftEditionCode, name: graph.leftEditionName, layout: axisLayout(graph.leftAxis),
                points: graph.leftPoints},
            right: {x: rightX, edition: graph.rightEditionCode, name: graph.rightEditionName,
                layout: axisLayout(graph.rightAxis), points: graph.rightPoints}
        };
        Object.keys(sides).forEach(function (key) {
            var side = sides[key];
            side.units = side.points.map(function (p) { return side.layout.unit(p); });
            side.base = d3.scaleLinear().domain([0, side.layout.total]).range([MARGIN_TOP, HEIGHT - MARGIN_BOTTOM]);
            side.transform = d3.zoomIdentity;
            side.y = side.base;
        });

        // Two points are linked: always (fan), unless they are the same verse (pairs), or by Strong number (lxx).
        function linked(a, b) {
            if (graph.mode === 'lxx') {
                return a.key === b.key;
            }
            if (graph.mode === 'pairs') {
                return !(a.bookCode === b.bookCode && a.chapter === b.chapter && a.verse === b.verse);
            }
            return true;
        }

        // Stage: canvas for the lines, SVG on top for zoom areas, axes, labels, points and verse cards.
        var stage = d3.select(canvas).append('div').attr('class', 'links-stage')
            .style('width', width + 'px').style('height', HEIGHT + 'px');
        var ratio = window.devicePixelRatio || 1;
        var lineCanvas = stage.append('canvas').attr('class', 'links-lines-canvas')
            .attr('width', width * ratio).attr('height', HEIGHT * ratio)
            .style('width', width + 'px').style('height', HEIGHT + 'px');
        var ctx = lineCanvas.node().getContext('2d');
        ctx.scale(ratio, ratio);
        var lineColor = getComputedStyle(document.documentElement).getPropertyValue('--link').trim() || '#17518a';
        var tooltip = stage.append('div').attr('class', 'links-tooltip').property('hidden', true);

        var svg = stage.append('svg').attr('class', 'links-svg')
            .attr('width', width).attr('height', HEIGHT).attr('viewBox', '0 0 ' + width + ' ' + HEIGHT)
            .attr('role', 'img').attr('aria-labelledby', 'links-summary');
        svg.append('clipPath').attr('id', 'links-clip').append('rect')
            .attr('x', 0).attr('y', MARGIN_TOP - 4).attr('width', width)
            .attr('height', HEIGHT - MARGIN_TOP - MARGIN_BOTTOM + 8);
        var zoomAreas = {
            left: svg.append('rect').attr('class', 'links-zoom-area')
                .attr('x', 0).attr('y', 0).attr('width', leftX + 12).attr('height', HEIGHT),
            right: svg.append('rect').attr('class', 'links-zoom-area')
                .attr('x', rightX - 12).attr('y', 0).attr('width', width - rightX + 12).attr('height', HEIGHT)
        };
        var clickArea = svg.append('rect').attr('class', 'links-click-area')
            .attr('x', leftX + 12).attr('y', 0).attr('width', Math.max(rightX - leftX - 24, 0)).attr('height', HEIGHT);
        var plot = svg.append('g').attr('clip-path', 'url(#links-clip)');
        var axesLayer = plot.append('g');
        var labelsLayer = plot.append('g').attr('class', 'links-labels');
        var pointsLayer = plot.append('g');
        svg.append('text').attr('class', 'links-axis-title').attr('x', leftX).attr('y', 18)
            .attr('text-anchor', 'middle').text(graph.leftEditionName);
        svg.append('text').attr('class', 'links-axis-title').attr('x', rightX).attr('y', 18)
            .attr('text-anchor', 'middle').text(graph.rightEditionName);

        var selection = null;   // {left: index, right: index} or {side: key, index: index}
        var current = {level: 'verse', lines: [], visible: {left: [], right: []}};

        function inView(y) {
            return y >= MARGIN_TOP - 1 && y <= HEIGHT - MARGIN_BOTTOM + 1;
        }

        function pixelsPerVerse(side) {
            return (HEIGHT - MARGIN_TOP - MARGIN_BOTTOM) / side.layout.total * side.transform.k;
        }

        function visibleIndexes(key) {
            var side = sides[key];
            var result = [];
            side.points.forEach(function (p, i) {
                if (side.units[i] !== null && inView(side.y(side.units[i]))) {
                    result.push(i);
                }
            });
            return result;
        }

        // Lines among the visible points: verse to verse when few enough, else grouped by chapter or book.
        function buildLines() {
            var visL = visibleIndexes('left');
            var visR = visibleIndexes('right');
            var L = sides.left.points;
            var R = sides.right.points;
            var rightByKey = d3.group(visR, function (j) { return graph.mode === 'lxx' ? R[j].key : ''; });
            var estimate = 0;
            d3.rollup(visL, function (v) { return v.length; }, function (i) { return graph.mode === 'lxx' ? L[i].key : ''; })
                .forEach(function (count, k) {
                    estimate += count * ((rightByKey.get(k) || []).length);
                });

            if (estimate <= MAX_VERSE_LINES) {
                var lines = [];
                visL.forEach(function (i) {
                    (rightByKey.get(graph.mode === 'lxx' ? L[i].key : '') || []).forEach(function (j) {
                        if (linked(L[i], R[j])) {
                            lines.push({a: sides.left.units[i], b: sides.right.units[j], left: i, right: j,
                                weight: L[i].count * R[j].count});
                        }
                    });
                });
                return {level: 'verse', lines: lines, visible: {left: visL, right: visR}, pairs: lines.length};
            }
            var grouped = groupLines(visL, visR, 'chapter');
            if (grouped.lines.length > MAX_GROUP_LINES) {
                grouped = groupLines(visL, visR, 'book');
            }
            grouped.visible = {left: visL, right: visR};
            grouped.pairs = estimate;
            return grouped;
        }

        // Group lines: weight is the number of occurrence pairs between the two groups (same Strong in lxx).
        function groupLines(visL, visR, level) {
            function groups(key, indexes) {
                var side = sides[key];
                var map = new Map();
                indexes.forEach(function (i) {
                    var p = side.points[i];
                    var id = level === 'book' ? p.bookCode : p.bookCode + ' ' + p.chapter;
                    var g = map.get(id);
                    if (!g) {
                        g = {id: id, bookCode: p.bookCode, chapter: level === 'book' ? null : p.chapter,
                            unit: level === 'book' ? side.layout.bookMiddle(p.bookCode)
                                : side.layout.chapterMiddle(p.bookCode, p.chapter),
                            byKey: new Map(), total: 0};
                        map.set(id, g);
                    }
                    var k = graph.mode === 'lxx' ? p.key : '';
                    g.byKey.set(k, (g.byKey.get(k) || 0) + p.count);
                    g.total += p.count;
                });
                return Array.from(map.values());
            }
            var gl = groups('left', visL);
            var gr = groups('right', visR);
            var lines = [];
            gl.forEach(function (a) {
                gr.forEach(function (b) {
                    var weight = 0;
                    a.byKey.forEach(function (count, k) {
                        weight += count * (b.byKey.get(k) || 0);
                    });
                    if (weight > 0) {
                        lines.push({a: a.unit, b: b.unit, group: true, leftGroup: a, rightGroup: b, weight: weight});
                    }
                });
            });
            return {level: level, lines: lines};
        }

        function ends(line) {
            return [sides.left.y(line.a), sides.right.y(line.b)];
        }

        function isSelected(line) {
            if (!selection || line.group) {
                return false;
            }
            if (selection.side) {
                return selection.side === 'left' ? line.left === selection.index : line.right === selection.index;
            }
            return line.left === selection.left && line.right === selection.right;
        }

        function strokeCurve(e) {
            ctx.beginPath();
            ctx.moveTo(leftX, e[0]);
            ctx.bezierCurveTo(midX, e[0], midX, e[1], rightX, e[1]);
            ctx.stroke();
        }

        function drawLines() {
            ctx.clearRect(0, 0, width, HEIGHT);
            ctx.save();
            ctx.beginPath();
            ctx.rect(0, MARGIN_TOP - 4, width, HEIGHT - MARGIN_TOP - MARGIN_BOTTOM + 8);
            ctx.clip();
            ctx.strokeStyle = lineColor;
            var lines = current.lines;
            var maxWeight = d3.max(lines, function (l) { return l.weight; }) || 1;
            var opacity = Math.min(0.7, Math.max(0.06, 7 / Math.sqrt(Math.max(lines.length, 1))));
            var selected = [];
            lines.forEach(function (line) {
                if (isSelected(line)) {
                    selected.push(ends(line));
                    return;
                }
                ctx.globalAlpha = selection ? Math.min(opacity, 0.08) : opacity;
                ctx.lineWidth = line.group ? 1 + 5 * Math.sqrt(line.weight / maxWeight) : 1.2;
                strokeCurve(ends(line));
            });
            ctx.globalAlpha = 1;
            ctx.lineWidth = 2.5;
            selected.forEach(strokeCurve);
            ctx.restore();
        }

        function drawAxis(key) {
            var side = sides[key];
            var y = side.y;
            var ppv = pixelsPerVerse(side);
            var axis = axesLayer.selectAll('g.axis-' + key).data([key]).join('g').attr('class', 'axis-' + key);
            var shown = side.layout.chapters.filter(function (c) {
                var top = y(side.layout.chapterStart(c));
                var bottom = y(side.layout.chapterStart(c) + c.verses);
                return bottom > MARGIN_TOP + 2 && top < HEIGHT - MARGIN_BOTTOM - 2;
            });
            var visibleBooks = d3.group(shown, function (c) { return c.bookCode; }).size;

            axis.selectAll('rect.links-chapter-hit').data(shown, function (c) { return c.bookCode + ' ' + c.chapter; })
                .join('rect')
                .attr('class', 'links-chapter-hit')
                .attr('x', side.x - 11).attr('width', 22)
                .attr('y', function (c) { return y(side.layout.chapterStart(c)); })
                .attr('height', function (c) { return Math.max(c.verses * ppv, 1); })
                .on('click', function (event, c) {
                    if (visibleBooks > 3) {
                        focusBook(key, c.bookCode);
                    } else {
                        focusChapter(key, c);
                    }
                })
                .on('wheel', function (event) {
                    if (zooms[key]) {
                        event.preventDefault();
                        zoomAreas[key].node().dispatchEvent(new WheelEvent('wheel', event));
                    }
                })
                .selectAll('title').data(function (c) { return [c]; }).join('title')
                .text(function (c) {
                    return visibleBooks > 3 ? 'Ampliar ' + c.bookName : 'Ampliar ' + c.bookName + ' ' + c.chapter;
                });
            axis.selectAll('rect.links-chapter').data(shown, function (c) { return c.bookCode + ' ' + c.chapter; })
                .join('rect')
                .attr('class', function (c) {
                    return c.chapter % 2 === 0 ? 'links-chapter links-chapter--alt' : 'links-chapter';
                })
                .attr('x', side.x - 2).attr('width', 4)
                .attr('y', function (c) { return y(side.layout.chapterStart(c)); })
                .attr('height', function (c) { return Math.max(c.verses * ppv, 0.5); });

            // Book names when many books are in view, chapter numbers otherwise. Close up, chapter labels
            // move to the inner side of the axis, leaving the outer side to the verses.
            var labels = [];
            var inner = current.visible[key].length <= REF_LIMIT;
            var anchor = (key === 'left') !== inner ? 'end' : 'start';
            var labelX = (key === 'left') !== inner ? side.x - 8 : side.x + 8;
            if (visibleBooks > 3) {
                side.layout.books.forEach(function (b) {
                    labels.push({y: y(b.start) + 9, text: b.bookName, book: true});
                });
            } else {
                shown.forEach(function (c, i) {
                    var named = i === 0 || c.chapter === 1;
                    labels.push({y: Math.max(y(side.layout.chapterStart(c)), MARGIN_TOP) + 9,
                        text: (named ? c.bookName + ' ' : '') + c.chapter, book: named});
                });
            }
            var last = -100;
            labels = labels.filter(function (l) {
                if (l.y - last < 12 || l.y < MARGIN_TOP || l.y > HEIGHT - MARGIN_BOTTOM) {
                    return false;
                }
                last = l.y;
                return true;
            });
            labelsLayer.selectAll('text.label-' + key).data(labels).join('text')
                .attr('class', function (l) {
                    return 'label-' + key + ' links-label' + (l.book ? ' links-label--book' : '');
                })
                .attr('x', labelX).attr('y', function (l) { return l.y; })
                .attr('text-anchor', anchor)
                .text(function (l) { return l.text; });
        }

        function drawPoints(key) {
            var side = sides[key];
            var visible = current.visible[key];
            var items = visible.map(function (i) {
                return {index: i, point: side.points[i], y: side.y(side.units[i])};
            });
            var few = items.length <= REF_LIMIT;

            pointsLayer.selectAll('circle.point-' + key).data(items, function (d) { return d.index; }).join('circle')
                .attr('class', function (d) {
                    var selected = selection && selection.side === key && selection.index === d.index;
                    return 'point-' + key + ' links-point' + (selected ? ' is-selected' : '');
                })
                .attr('cx', side.x).attr('cy', function (d) { return d.y; })
                .attr('r', few ? 4 : 2.5)
                .on('click', function (event, d) {
                    select({side: key, index: d.index});
                })
                .selectAll('title').data(function (d) { return [d]; }).join('title')
                .text(function (d) { return reference(names, d.point); });

            var withCards = items.length > 0 && items.length <= CARD_LIMIT;
            var anchor = key === 'left' ? 'end' : 'start';
            var labelX = key === 'left' ? side.x - 10 : side.x + 10;
            var last = -100;
            var refItems = (few && !withCards ? items : []).filter(function (d) {
                if (d.y - last < 12) {
                    return false;
                }
                last = d.y;
                return true;
            });
            labelsLayer.selectAll('text.ref-' + key).data(refItems, function (d) { return d.index; }).join('text')
                .attr('class', 'ref-' + key + ' links-label')
                .attr('x', labelX).attr('y', function (d) { return d.y + 4; })
                .attr('text-anchor', anchor)
                .text(function (d) { return reference(names, d.point); });
            drawCards(key, withCards ? items : []);
        }

        // Verse texts beside an axis, pushed down so they do not overlap, each linked to its point.
        function drawCards(key, items) {
            var side = sides[key];
            var cardWidth = key === 'left' ? side.x - 26 : width - side.x - 26;
            var cardX = key === 'left' ? 8 : side.x + 18;
            var cardHeight = 76;
            var room = (HEIGHT - MARGIN_TOP - MARGIN_BOTTOM) / Math.max(items.length, 1) - 6;
            cardHeight = Math.max(40, Math.min(cardHeight, room));
            var bottom = MARGIN_TOP - 6;
            var cards = items.map(function (d) {
                var top = Math.max(d.y - cardHeight / 2, bottom + 6);
                bottom = top + cardHeight;
                return {item: d, top: top};
            });
            var groups = labelsLayer.selectAll('g.card-' + key).data(cards, function (c) { return c.item.index; })
                .join(function (enter) {
                    var g = enter.append('g').attr('class', 'card-' + key);
                    g.append('line').attr('class', 'links-card-connector');
                    g.append('foreignObject').append('xhtml:div').attr('class', 'links-card');
                    return g;
                });
            groups.select('line')
                .attr('x1', key === 'left' ? cardX + cardWidth : cardX).attr('x2', side.x)
                .attr('y1', function (c) { return c.top + 12; })
                .attr('y2', function (c) { return c.item.y; });
            groups.select('foreignObject')
                .attr('x', cardX).attr('y', function (c) { return c.top; })
                .attr('width', cardWidth).attr('height', cardHeight);
            groups.select('div.links-card').style('height', cardHeight + 'px').each(function (c) {
                this.textContent = '';
                this.appendChild(verseText(side.edition, names, c.item.point, true));
            });
            if (items.length > 0) {
                loadTexts(side.edition, items.map(function (d) { return d.point; })).then(function (changed) {
                    if (changed) {
                        scheduleRedraw();
                    }
                });
            }
        }

        function redraw() {
            current = buildLines();
            drawLines();
            drawAxis('left');
            drawAxis('right');
            drawPoints('left');
            drawPoints('right');
            scheduleReading();
        }

        var pending = false;
        function scheduleRedraw() {
            if (!pending) {
                pending = true;
                requestAnimationFrame(function () {
                    pending = false;
                    redraw();
                });
            }
        }

        // Reading list below the map: for each verse on the side with fewer verses in view, its text and
        // the texts of the verses it is linked to on the other side. Refreshed when the view settles.
        var readingTimer = null;
        function scheduleReading() {
            clearTimeout(readingTimer);
            readingTimer = setTimeout(renderReading, 300);
        }

        function renderReading() {
            readingBody.textContent = '';
            if (current.level !== 'verse') {
                readingStatus.textContent = Biblia.plural(current.pairs, 'par', 'pares') + ' de ocorrências na área '
                    + 'visível, por isso as linhas estão agrupadas por ' + (current.level === 'book' ? 'livro' : 'capítulo')
                    + '. Amplie um dos eixos (clique num livro ou capítulo) para ver versículo a versículo.';
                return;
            }
            if (current.lines.length === 0) {
                readingStatus.textContent = 'Nenhuma ligação entre os versículos visíveis dos dois eixos.';
                return;
            }
            var byLeft = d3.group(current.lines, function (l) { return l.left; });
            var byRight = d3.group(current.lines, function (l) { return l.right; });
            var anchorLeft = byLeft.size <= byRight.size;
            var anchors = Array.from((anchorLeft ? byLeft : byRight).entries())
                .sort(function (x, y) { return x[0] - y[0]; });
            var anchorSide = anchorLeft ? sides.left : sides.right;
            var otherSide = anchorLeft ? sides.right : sides.left;
            readingStatus.textContent = Biblia.plural(current.lines.length, 'ligação', 'ligações') + ' entre '
                + Biblia.plural(byLeft.size, 'versículo', 'versículos') + ' à esquerda e '
                + Biblia.plural(byRight.size, 'versículo', 'versículos') + ' à direita'
                + (anchors.length > READING_GROUPS ? '; mostrando os ' + READING_GROUPS + ' primeiros de '
                    + anchors.length + ' (amplie para reduzir)' : '') + '.';

            var wanted = {anchor: [], other: []};
            anchors.slice(0, READING_GROUPS).forEach(function (entry) {
                wanted.anchor.push(anchorSide.points[entry[0]]);
                entry[1].slice(0, READING_LINKED).forEach(function (l) {
                    wanted.other.push(otherSide.points[anchorLeft ? l.right : l.left]);
                });
            });
            Promise.all([loadTexts(anchorSide.edition, wanted.anchor), loadTexts(otherSide.edition, wanted.other)])
                .then(function () {
                    readingBody.textContent = '';
                    anchors.slice(0, READING_GROUPS).forEach(function (entry) {
                        var anchorIndex = entry[0];
                        var lines = entry[1];
                        var group = Biblia.element('article', 'links-reading-group');
                        var head = verseText(anchorSide.edition, names, anchorSide.points[anchorIndex], false);
                        head.classList.add('links-reading-group__anchor');
                        head.addEventListener('click', function () {
                            select({side: anchorLeft ? 'left' : 'right', index: anchorIndex});
                        });
                        group.appendChild(head);
                        var list = Biblia.element('ul', 'links-reading-group__linked');
                        lines.slice(0, READING_LINKED).forEach(function (l) {
                            var otherIndex = anchorLeft ? l.right : l.left;
                            var item = Biblia.element('li');
                            item.appendChild(verseText(otherSide.edition, names, otherSide.points[otherIndex], true));
                            item.addEventListener('click', function () {
                                select({left: l.left, right: l.right});
                            });
                            list.appendChild(item);
                        });
                        group.appendChild(list);
                        if (lines.length > READING_LINKED) {
                            group.appendChild(Biblia.element('p', 'links__help', 'e mais '
                                + Biblia.plural(lines.length - READING_LINKED, 'versículo ligado', 'versículos ligados')
                                + ' (amplie o outro eixo para ver só os que interessam).'));
                        }
                        readingBody.appendChild(group);
                    });
                });
        }

        // Nearest drawn line to a point of the map (within 6 pixels), sampling each curve.
        function nearestLine(mx, my) {
            var best = null;
            var bestDistance = 36;
            current.lines.forEach(function (line) {
                var e = ends(line);
                for (var i = 0; i <= 24; i++) {
                    var t = i / 24;
                    var u = 1 - t;
                    var x = u * u * u * leftX + 3 * u * u * t * midX + 3 * u * t * t * midX + t * t * t * rightX;
                    var yy = (u * u * u + 3 * u * u * t) * e[0] + (3 * u * t * t + t * t * t) * e[1];
                    var d = (x - mx) * (x - mx) + (yy - my) * (yy - my);
                    if (d < bestDistance) {
                        bestDistance = d;
                        best = line;
                    }
                }
            });
            return best;
        }

        function lineLabel(line) {
            if (line.group) {
                var a = line.leftGroup;
                var b = line.rightGroup;
                return (names[a.bookCode] + (a.chapter ? ' ' + a.chapter : '')) + ' e '
                    + (names[b.bookCode] + (b.chapter ? ' ' + b.chapter : '')) + ': '
                    + Biblia.plural(line.weight, 'par', 'pares') + ' (clique para ampliar)';
            }
            return reference(names, sides.left.points[line.left]) + ' e '
                + reference(names, sides.right.points[line.right]);
        }

        var hoverPending = false;
        clickArea
            .on('click', function (event) {
                var at = d3.pointer(event);
                var line = nearestLine(at[0], at[1]);
                if (!line) {
                    return;
                }
                if (line.group) {
                    focusGroup('left', line.leftGroup);
                    focusGroup('right', line.rightGroup);
                } else {
                    select({left: line.left, right: line.right});
                }
            })
            .on('mousemove', function (event) {
                var at = d3.pointer(event);
                if (hoverPending) {
                    return;
                }
                hoverPending = true;
                requestAnimationFrame(function () {
                    hoverPending = false;
                    var line = nearestLine(at[0], at[1]);
                    tooltip.property('hidden', !line);
                    clickArea.classed('is-over-line', Boolean(line));
                    if (line) {
                        tooltip.text(lineLabel(line))
                            .style('left', Math.min(at[0] + 12, width - 260) + 'px').style('top', (at[1] + 14) + 'px');
                    }
                });
            })
            .on('mouseleave', function () {
                tooltip.property('hidden', true);
            });

        // One vertical zoom per axis (the left axis of the fan mode holds only the origin verse).
        var zooms = {};
        Object.keys(sides).filter(function (key) {
            return !(key === 'left' && graph.mode === 'fan');
        }).forEach(function (key) {
            var side = sides[key];
            zooms[key] = d3.zoom()
                .scaleExtent([1, 4000])
                .extent([[0, MARGIN_TOP], [width, HEIGHT - MARGIN_BOTTOM]])
                .translateExtent([[0, MARGIN_TOP], [width, HEIGHT - MARGIN_BOTTOM]])
                .on('zoom', function (event) {
                    side.transform = event.transform;
                    side.y = event.transform.rescaleY(side.base);
                    scheduleRedraw();
                });
            zoomAreas[key].call(zooms[key]);
        });

        function focusRange(key, from, to) {
            if (!zooms[key]) {
                return;
            }
            var side = sides[key];
            var pad = 0.5;
            var k = (HEIGHT - MARGIN_TOP - MARGIN_BOTTOM) / (side.base(to + pad) - side.base(from - pad));
            var transform = d3.zoomIdentity.translate(0, MARGIN_TOP - k * side.base(from - pad)).scale(Math.max(k, 1));
            zoomAreas[key].transition().duration(450).call(zooms[key].transform, transform);
        }

        function focusChapter(key, c) {
            var from = sides[key].layout.chapterStart(c);
            focusRange(key, from, from + c.verses);
        }

        function focusBook(key, bookCode) {
            var b = sides[key].layout.books.filter(function (x) { return x.bookCode === bookCode; })[0];
            focusRange(key, b.start, b.end);
        }

        function focusGroup(key, group) {
            if (group.chapter) {
                var c = sides[key].layout.chapters.filter(function (x) {
                    return x.bookCode === group.bookCode && x.chapter === group.chapter;
                })[0];
                focusChapter(key, c);
            } else {
                focusBook(key, group.bookCode);
            }
        }

        function select(target) {
            selection = target;
            drawLines();
            drawPoints('left');
            drawPoints('right');
            showDetail(target);
        }

        function showDetail(target) {
            detail.textContent = '';
            detail.hidden = false;
            var heading = Biblia.element('h3', 'links__detail-title');
            detail.appendChild(heading);
            var points = [];
            if (target.side) {
                var side = sides[target.side];
                var point = side.points[target.index];
                var count = current.lines.filter(isSelected).length;
                heading.textContent = reference(names, point) + ': '
                    + Biblia.plural(count, 'ligação visível', 'ligações visíveis');
                points.push([side, point]);
            } else {
                var a = sides.left.points[target.left];
                var b = sides.right.points[target.right];
                heading.textContent = reference(names, a) + ' e ' + reference(names, b);
                points.push([sides.left, a], [sides.right, b]);
            }
            var columns = Biblia.element('div', 'links__detail-columns');
            detail.appendChild(columns);
            Promise.all(points.map(function (sp) { return loadTexts(sp[0].edition, [sp[1]]); })).then(function () {
                points.forEach(function (sp) {
                    var block = Biblia.element('div', 'links__detail-verse');
                    block.appendChild(Biblia.element('h4', null, sp[0].name));
                    block.appendChild(verseText(sp[0].edition, names, sp[1], false));
                    var open = Biblia.element('button', 'link-button', 'Abrir na leitura');
                    open.type = 'button';
                    open.addEventListener('click', function () {
                        Biblia.openVerse(sp[0].edition, sp[1].bookCode, sp[1].chapter, sp[1].verse);
                    });
                    block.appendChild(open);
                    columns.appendChild(block);
                });
            });
        }

        view = {
            reset: function () {
                Object.keys(zooms).forEach(function (key) {
                    zoomAreas[key].transition().duration(300).call(zooms[key].transform, d3.zoomIdentity);
                });
            },
            zoomBy: function (key, factor) {
                if (zooms[key]) {
                    zoomAreas[key].transition().duration(300).call(zooms[key].scaleBy, factor,
                        [sides[key].x, (MARGIN_TOP + HEIGHT - MARGIN_BOTTOM) / 2]);
                }
            }
        };

        redraw();

        var text = Biblia.plural(graph.pairCount, 'par', 'pares') + ' de ocorrências entre '
            + Biblia.plural(graph.leftPoints.length, 'versículo', 'versículos') + ' à esquerda e '
            + Biblia.plural(graph.rightPoints.length, 'versículo', 'versículos') + ' à direita';
        if (graph.mode === 'lxx') {
            text += '. Strong grego: ' + graph.strongs.map(function (s) { return 'G' + s; }).join(', ');
        }
        summary.textContent = graph.pairCount === 0 ? 'Nenhuma ligação para esta combinação de eixos.' : text + '.';
    }

    load();
})();
