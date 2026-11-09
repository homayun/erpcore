document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const errorBox = document.getElementById('error-msg');
    const errorText = document.getElementById('error-text');
    const submitBtn = document.querySelector('.login-btn');

    errorBox.style.display = 'none';
    const originalBtnHtml = submitBtn.innerHTML;
    submitBtn.innerHTML = 'Signing in...';
    submitBtn.style.opacity = '0.7';
    submitBtn.disabled = true;

    // CSRF token required for POST /login (Login CSRF protection)
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = { 'Content-Type': 'application/json' };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    try {
        const res = await fetch('/login', {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin',
            body: JSON.stringify({
                username: document.getElementById('username').value,
                password: document.getElementById('password').value
            })
        });

        if (res.ok) {
            window.location.href = '/dashboard';
        } else if (res.status === 403) {
            errorText.textContent = 'Session expired. Please refresh the page.';
            errorBox.style.display = 'flex';
            submitBtn.innerHTML = originalBtnHtml;
            submitBtn.style.opacity = '1';
            submitBtn.disabled = false;
        } else {
            errorText.textContent = 'Invalid username or password';
            errorBox.style.display = 'flex';
            submitBtn.innerHTML = originalBtnHtml;
            submitBtn.style.opacity = '1';
            submitBtn.disabled = false;
        }
    } catch (err) {
        errorText.textContent = 'Network error: ' + err.message;
        errorBox.style.display = 'flex';
        submitBtn.innerHTML = originalBtnHtml;
        submitBtn.style.opacity = '1';
        submitBtn.disabled = false;
    }
});