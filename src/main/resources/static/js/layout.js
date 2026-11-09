document.addEventListener('DOMContentLoaded', () => {
    // ১. ট্রি মেনু ড্রপডাউন (Event Delegation ব্যবহার করে, যাতে HTMX রিলোডেও কাজ করে)
    document.body.addEventListener('click', function(e) {
        const toggle = e.target.closest('.toggle-submenu');
        if (toggle) {
            e.preventDefault();
            const parent = toggle.parentElement;
            const submenu = parent.querySelector('.submenu');
            if (submenu) {
                parent.classList.toggle('open');
                submenu.classList.toggle('active'); // classList.active দিয়ে টগল হবে
            }
        }
    });

    // ২. লগআউট
    document.body.addEventListener('click', async function(e) {
        const logoutBtn = e.target.closest('#logoutBtn');
        if (logoutBtn) {
            e.preventDefault();
            try { await fetch('/logout', { method: 'POST', credentials: 'same-origin' }); } catch (err) {}
            window.location.href = '/login';
        }
    });

    // ৩. মডাল ক্লোজ
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

    if (evt.detail.target.id === 'modal-container') {
        modal.style.display = 'flex';
    }
    if (evt.detail.target.id === 'company-list' || evt.detail.target.id === 'master-company-list' || evt.detail.target.id === 'user-list' || evt.detail.target.id === 'role-list' || evt.detail.target.id === "permission-list") {
        modal.style.display = 'none';
        modal.innerHTML = '';
    }
});

// HTMX Error Handling
document.body.addEventListener('htmx:responseError', function(evt) {
    if (evt.detail.xhr.status === 400) {
        alert("Cannot delete: " + evt.detail.xhr.responseText);
    } else {
        alert("Server Error: " + evt.detail.xhr.status);
    }
});
// Close modal via HX-Trigger
document.body.addEventListener('closeModal', function() {
    const modal = document.getElementById('modal-container');
    if (modal) {
        modal.style.display = 'none';
        modal.innerHTML = '';
    }
});