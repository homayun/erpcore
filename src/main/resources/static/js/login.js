document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const errEl = document.getElementById('error');
    errEl.style.display = 'none';
    try {
        const res = await fetch('/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({
                username: document.getElementById('username').value,
                password: document.getElementById('password').value
            })
        });
        if (res.ok) {
            window.location.href = '/dashboard';
        } else {
            errEl.textContent = 'Login failed (HTTP ' + res.status + ')';
            errEl.style.display = 'block';
        }
    } catch (err) {
        errEl.textContent = 'Login error: ' + err.message;
        errEl.style.display = 'block';
    }
});