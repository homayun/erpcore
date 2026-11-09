document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('logoutBtn');
    if (btn) {
        btn.addEventListener('click', async () => {
            try {
                await fetch('/logout', { method: 'POST', credentials: 'same-origin' });
            } catch (e) { /* ignore */ }
            window.location.href = '/login';
        });
    }
});