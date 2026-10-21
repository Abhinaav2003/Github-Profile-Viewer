(function () {
    var KEY = 'theme';
    var root = document.documentElement;
    var media = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

    function stored() {
        try { return localStorage.getItem(KEY); } catch (e) { return null; }
    }

    function preferred() {
        return stored() || (media && media.matches ? 'dark' : 'light');
    }

    function apply(theme) {
        root.setAttribute('data-bs-theme', theme);
        var button = document.getElementById('themeToggle');
        if (button) {
            var label = theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme';
            button.setAttribute('aria-label', label);
            button.setAttribute('title', label);
        }
    }

    // Runs in <head>, so the right theme is set before the page paints (no flash).
    apply(preferred());

    document.addEventListener('DOMContentLoaded', function () {
        apply(root.getAttribute('data-bs-theme'));
        var button = document.getElementById('themeToggle');
        if (!button) return;
        button.addEventListener('click', function () {
            var next = root.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
            try { localStorage.setItem(KEY, next); } catch (e) { /* storage unavailable */ }
            apply(next);
        });
    });

    // Follow the OS setting until the user picks a theme manually.
    if (media && media.addEventListener) {
        media.addEventListener('change', function (e) {
            if (!stored()) apply(e.matches ? 'dark' : 'light');
        });
    }
})();