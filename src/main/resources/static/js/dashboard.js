document.addEventListener('DOMContentLoaded', function () {
    const btn = document.getElementById('logoutBtn');
    if (!btn) return;

    btn.addEventListener('click', async function (e) {
        e.preventDefault();
        try {
            const token  = document.querySelector('meta[name="_csrf"]');
            const header = document.querySelector('meta[name="_csrf_header"]');
            const headers = {};
            if (token && header) headers[header.getAttribute('content')] = token.getAttribute('content');
            await fetch('/logout', { method: 'POST', credentials: 'same-origin', headers: headers });
        } catch (err) {
            console.warn('Logout request failed:', err);
        } finally {
            window.location.href = '/login';
        }
    });
});