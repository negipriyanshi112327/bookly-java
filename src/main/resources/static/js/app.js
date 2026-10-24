/*
 * Bookly - small progressive enhancements.
 * Every feature also works without JavaScript; all rules are enforced on the server.
 */
(function () {
    'use strict';

    function ready(fn) {
        if (document.readyState !== 'loading') fn();
        else document.addEventListener('DOMContentLoaded', fn);
    }

    ready(function () {

        /* ---------- mobile navigation ---------- */
        document.querySelectorAll('[data-menu-toggle]').forEach(function (button) {
            var header = button.closest('.site-header');
            button.addEventListener('click', function () {
                var open = button.getAttribute('aria-expanded') === 'true';
                button.setAttribute('aria-expanded', String(!open));
                header.classList.toggle('menu-open', !open);
            });
        });

        /* ---------- dismissible alerts ---------- */
        document.querySelectorAll('[data-dismiss]').forEach(function (button) {
            button.addEventListener('click', function () {
                var alert = button.closest('.alert');
                if (alert) alert.remove();
            });
        });

        /* ---------- show / hide password ---------- */
        document.querySelectorAll('[data-toggle-password]').forEach(function (button) {
            var input = document.getElementById(button.getAttribute('data-toggle-password'));
            if (!input) return;
            button.addEventListener('click', function () {
                var show = input.type === 'password';
                input.type = show ? 'text' : 'password';
                button.textContent = show ? 'Hide' : 'Show';
                button.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
            });
        });

        /* ---------- confirmation before destructive actions ---------- */
        document.addEventListener('submit', function (event) {
            var form = event.target;
            var message = form.getAttribute('data-confirm');
            var submitter = event.submitter;
            if (!message && submitter) message = submitter.getAttribute('data-confirm');
            if (message && !window.confirm(message)) {
                event.preventDefault();
                event.stopImmediatePropagation();
            }
        }, true);

        /* ---------- client-side validation messages (server validates again) ---------- */
        document.querySelectorAll('form[data-validate]').forEach(function (form) {
            form.addEventListener('submit', function (event) {
                var firstInvalid = null;
                form.querySelectorAll('[data-match]').forEach(function (input) {
                    var other = document.getElementById(input.getAttribute('data-match'));
                    input.setCustomValidity(other && other.value !== input.value ? 'Passwords do not match.' : '');
                });
                form.querySelectorAll('input, select, textarea').forEach(function (field) {
                    var wrapper = field.closest('.field');
                    if (!wrapper || field.type === 'hidden') return;
                    var message = wrapper.querySelector('.field-error.js-error');
                    if (field.checkValidity()) {
                        if (message && !wrapper.querySelector('.field-error:not(.js-error)')) wrapper.classList.remove('has-error');
                        if (message) message.remove();
                        return;
                    }
                    if (!firstInvalid) firstInvalid = field;
                    wrapper.classList.add('has-error');
                    if (!message) {
                        message = document.createElement('p');
                        message.className = 'field-error js-error';
                        wrapper.appendChild(message);
                    }
                    message.textContent = field.validationMessage;
                });
                if (firstInvalid) {
                    event.preventDefault();
                    event.stopImmediatePropagation();
                    firstInvalid.focus();
                }
            });
        });

        /* ---------- loading state + no double submit ---------- */
        document.querySelectorAll('form').forEach(function (form) {
            form.addEventListener('submit', function (event) {
                if (event.defaultPrevented) return;
                var once = form.hasAttribute('data-once');
                if (once && form.dataset.submitted === 'true') {
                    event.preventDefault();
                    return;
                }
                form.dataset.submitted = 'true';
                var button = event.submitter || form.querySelector('[type="submit"]');
                if (button && button.hasAttribute('data-loading-text')) {
                    button.dataset.originalText = button.textContent;
                    button.textContent = button.getAttribute('data-loading-text');
                    button.classList.add('is-loading');
                    if (once) button.disabled = true;
                }
                if (form.hasAttribute('data-loading-form')) form.classList.add('is-loading');
            });
        });
        // When the page is restored from the back/forward cache, re-enable buttons.
        window.addEventListener('pageshow', function () {
            document.querySelectorAll('.is-loading').forEach(function (el) {
                el.classList.remove('is-loading');
                if (el.dataset.originalText) el.textContent = el.dataset.originalText;
                if (el.tagName === 'BUTTON') el.disabled = false;
            });
            document.querySelectorAll('form').forEach(function (form) { form.dataset.submitted = ''; });
        });

        /* ---------- cart quantity box submits on change ---------- */
        document.querySelectorAll('[data-autosubmit]').forEach(function (input) {
            input.addEventListener('change', function () {
                if (input.checkValidity()) input.form.requestSubmit ? input.form.requestSubmit() : input.form.submit();
                else input.reportValidity();
            });
        });

        /* ---------- character counters ---------- */
        document.querySelectorAll('textarea[data-counter][maxlength]').forEach(function (area) {
            var counter = document.createElement('p');
            counter.className = 'hint counter';
            counter.setAttribute('aria-live', 'polite');
            area.insertAdjacentElement('afterend', counter);
            function update() { counter.textContent = area.value.length + ' / ' + area.maxLength; }
            area.addEventListener('input', update);
            update();
        });

        /* ---------- admin: live cover preview ---------- */
        var coverInput = document.querySelector('[data-cover-input]');
        var coverPreview = document.querySelector('[data-cover-preview]');
        if (coverInput && coverPreview) {
            coverInput.addEventListener('input', function () {
                var url = coverInput.value.trim();
                if (/^(https?:\/\/|\/)/.test(url)) {
                    coverPreview.hidden = false;
                    coverPreview.src = url;
                } else {
                    coverPreview.hidden = true;
                }
            });
        }

        /* ---------- gentle reveal animation on the home page ---------- */
        if ('IntersectionObserver' in window && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            var observer = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('is-visible');
                        observer.unobserve(entry.target);
                    }
                });
            }, { threshold: 0.08 });
            document.querySelectorAll('.book-card, .quote-section, .story-section').forEach(function (el) {
                el.classList.add('reveal');
                observer.observe(el);
            });
        }

        /* ---------- auto-hide success messages ---------- */
        document.querySelectorAll('.flash-area .alert-success').forEach(function (alert) {
            setTimeout(function () { alert.classList.add('fade-out'); }, 6000);
        });
    });
})();
