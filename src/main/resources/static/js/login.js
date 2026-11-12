document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('loginForm');
    if (!form) return;

    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const errorBox      = document.getElementById('error-msg');
    const errorText     = document.getElementById('error-text');
    const submitBtn     = form.querySelector('.login-btn');
    const capsWarning   = document.getElementById('caps-warning');

    document.querySelectorAll('[data-toggle-password]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const target = document.getElementById(btn.getAttribute('data-toggle-password'));
            if (!target) return;
            const showing = target.type === 'text';
            target.type = showing ? 'password' : 'text';
            btn.setAttribute('aria-pressed', String(!showing));
            btn.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
            const eye    = btn.querySelector('.icon-eye');
            const eyeOff = btn.querySelector('.icon-eye-off');
            if (eye && eyeOff) {
                eye.style.display    = showing ? '' : 'none';
                eyeOff.style.display = showing ? 'none' : '';
            }
            target.focus();
        });
    });

    function updateCaps(e) {
        if (!capsWarning) return;
        const on = e && typeof e.getModifierState === 'function' && e.getModifierState('CapsLock');
        capsWarning.style.display = on ? 'flex' : 'none';
    }
    if (passwordInput) {
        passwordInput.addEventListener('keydown', updateCaps);
        passwordInput.addEventListener('keyup',   updateCaps);
        passwordInput.addEventListener('blur', function () {
            if (capsWarning) capsWarning.style.display = 'none';
        });
    }

    function showError(message) {
        if (!errorBox || !errorText) return;
        errorText.textContent = message;
        errorBox.style.display = 'flex';
    }
    function clearError() { if (errorBox) errorBox.style.display = 'none'; }
    function setLoading(loading) {
        if (!submitBtn) return;
        submitBtn.classList.toggle('loading', loading);
        submitBtn.disabled = loading;
        if (loading) submitBtn.setAttribute('aria-busy', 'true');
        else         submitBtn.removeAttribute('aria-busy');
    }

    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        clearError();

        const honeypot = document.getElementById('website');
        if (honeypot && honeypot.value) return;   /* bot filled honeypot */

        const username = usernameInput ? usernameInput.value.trim() : '';
        const password = passwordInput ? passwordInput.value : '';
        if (!username || !password) {
            showError('Please enter your username and password.');
            return;
        }

        setLoading(true);

        const csrfTokenEl  = document.querySelector('meta[name="_csrf"]');
        const csrfHeaderEl = document.querySelector('meta[name="_csrf_header"]');
        const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
        if (csrfTokenEl && csrfHeaderEl) {
            headers[csrfHeaderEl.getAttribute('content')] = csrfTokenEl.getAttribute('content');
        }

        try {
            const res = await fetch('/login', {
                method: 'POST',
                headers: headers,
                credentials: 'same-origin',
                body: JSON.stringify({ username: username, password: password })
            });

            if (res.ok) { window.location.href = '/dashboard'; return; }
            if (res.status === 429) { showError('Too many attempts. Please try again later.'); return; }
            if (res.status === 403) { showError('Session expired. Please refresh the page.'); return; }
            showError('Invalid username or password.');
        } catch (err) {
            showError('Network error. Please check your connection and try again.');
        } finally {
            setLoading(false);
        }
    });
});