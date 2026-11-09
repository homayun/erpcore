document.addEventListener('DOMContentLoaded', () => {
    // ট্রি মেনু ড্রপডাউন
    document.querySelectorAll('.toggle-submenu').forEach(toggle => {
        toggle.addEventListener('click', (e) => {
            e.preventDefault();
            const parent = toggle.parentElement;
            const submenu = parent.querySelector('.submenu');
            parent.classList.toggle('open');
            submenu.style.display = (submenu.style.display === 'block') ? 'none' : 'block';
        });
    });

    // লগআউট
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            try { await fetch('/logout', { method: 'POST', credentials: 'same-origin' }); } catch (e) {}
            window.location.href = '/login';
        });
    }

    // মডাল ক্লোজ (বাইরে ক্লিক করলে বা Cancel বাটনে)
    document.body.addEventListener('click', function(evt) {
        const modal = document.getElementById('modal-container');
        if (!modal) return;

        if (evt.target.id === 'modal-container') {
            modal.style.display = 'none';
            modal.innerHTML = '';
        }
        if (evt.target.classList.contains('close-modal')) {
            modal.style.display = 'none';
            modal.innerHTML = '';
        }
    });
});

// HTMX CSRF হেডার
document.body.addEventListener('htmx:configRequest', function(evt) {
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header) {
        evt.detail.headers[header.content] = token.content;
    }
});

// HTMX Swap ইভেন্ট (মডাল ওপেন/ক্লোজ অটোমেশন)
document.body.addEventListener('htmx:afterSwap', function(evt) {
    const modal = document.getElementById('modal-container');
    if (!modal) return;

    // ফর্ম লোড হলে মডাল ওপেন হবে
    if (evt.detail.target.id === 'modal-container') {
        modal.style.display = 'flex';
    }
    // নতুন রো যোগ হলে মডাল বন্ধ হবে
    if (evt.detail.target.id === 'company-list') {
        modal.style.display = 'none';
        modal.innerHTML = '';
    }
});