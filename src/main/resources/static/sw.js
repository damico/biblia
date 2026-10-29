// Service worker that makes the site installable as an app. Nothing is cached: the text and the audio
// always come from the server; without a connection, pages show a short notice instead of the browser error.
var OFFLINE_PAGE = '<!DOCTYPE html><html lang="pt-BR"><head><meta charset="UTF-8">'
    + '<meta name="viewport" content="width=device-width, initial-scale=1"><title>Sem conexão</title></head>'
    + '<body style="font-family: system-ui, sans-serif; padding: 2rem; color: #202322; background: #fcfbf8">'
    + '<h1>Sem conexão</h1><p>A Bíblia Interlinear precisa de internet. Tente de novo quando a conexão voltar.</p>'
    + '</body></html>';

self.addEventListener('install', function () {
    self.skipWaiting();
});

self.addEventListener('activate', function (event) {
    event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', function (event) {
    if (event.request.mode !== 'navigate') {
        return;
    }
    event.respondWith(fetch(event.request).catch(function () {
        return new Response(OFFLINE_PAGE, {headers: {'Content-Type': 'text/html; charset=UTF-8'}});
    }));
});
