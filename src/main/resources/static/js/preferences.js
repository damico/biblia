// Applies saved reading preferences before first paint.
// Theme defaults to the system preference, light when none is set.
(function () {
    var root = document.documentElement;
    var prefs = {};
    try {
        prefs = JSON.parse(localStorage.getItem('reading-prefs') || '{}');
    } catch (e) {
        // storage unavailable or invalid, use defaults
    }
    var systemDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    var theme = prefs.theme || (systemDark ? 'dark' : 'light');

    root.setAttribute('data-theme', theme);
    root.setAttribute('data-bs-theme', theme === 'dark' ? 'dark' : 'light');
    root.setAttribute('data-reading-font', prefs.font || 'serif');
    root.setAttribute('data-reading-size', prefs.size || 'normal');
})();
