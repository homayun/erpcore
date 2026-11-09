document.addEventListener('DOMContentLoaded', () => {
    // ১. ট্রি মেনু ড্রপডাউন লজিক
    const toggles = document.querySelectorAll('.toggle-submenu');
    toggles.forEach(toggle => {
        toggle.addEventListener('click', (e) => {
            e.preventDefault();
            const parent = toggle.parentElement;
            const submenu = parent.querySelector('.submenu');
            
            parent.classList.toggle('open');
            if (submenu.style.display === 'block') {
                submenu.style.display = 'none';
            } else {
                submenu.style.display = 'block';
            }
        });
    });

    // ২. লগআউট লজিক
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            try {
                await fetch('/logout', { method: 'POST', credentials: 'same-origin' });
            } catch (e) { }
            window.location.href = '/login';
        });
    }
});