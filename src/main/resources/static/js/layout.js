/* ============================================================
   ErpCore — layout.js
   Handles: submenu toggle, profile dropdown, modal, logout,
            CSRF injection for HTMX, error toasts, 401 redirect.
   ============================================================ */
document.addEventListener('DOMContentLoaded', function () {

    /* ---------- Submenu toggle (button + aria-expanded) ---------- */
    document.body.addEventListener('click', function (e) {
        const toggle = e.target.closest('.toggle-submenu');
        if (!toggle) return;
        e.preventDefault();
        const parent  = toggle.parentElement;
        const submenu = parent.querySelector('.submenu');
        if (!submenu) return;
        const open = parent.classList.toggle('open');
        submenu.classList.toggle('active', open);
        toggle.setAttribute('aria-expanded', String(open));
    });

    /* ---------- Profile dropdown: hover + keyboard + click ---------- */
    document.body.addEventListener('click', function (e) {
        const profileBtn = e.target.closest('button.user-profile');
        const dropdown   = document.querySelector('.dropdown');
        if (profileBtn) {
            const open = dropdown.classList.toggle('open');
            profileBtn.setAttribute('aria-expanded', String(open));
            return;
        }
        /* Click outside closes it */
        if (dropdown && !e.target.closest('.dropdown')) {
            dropdown.classList.remove('open');
            const btn = dropdown.querySelector('button.user-profile');
            if (btn) btn.setAttribute('aria-expanded', 'false');
        }
    });

    /* Escape closes dropdown + modal */
    document.addEventListener('keydown', function (e) {
        if (e.key !== 'Escape') return;
        const dropdown = document.querySelector('.dropdown.open');
        if (dropdown) {
            dropdown.classList.remove('open');
            const btn = dropdown.querySelector('button.user-profile');
            if (btn) btn.setAttribute('aria-expanded', 'false');
        }
        closeModal();
    });

    /* ---------- Modal ---------- */
    function closeModal() {
        const modal = document.getElementById('modal-container');
        if (!modal) return;
        modal.style.display = 'none';
        modal.innerHTML = '';
    }
    window.closeModal = closeModal;

    document.body.addEventListener('click', function (evt) {
        const modal = document.getElementById('modal-container');
        if (!modal) return;
        if (evt.target.id === 'modal-container' || evt.target.classList.contains('close-modal')) {
            closeModal();
        }
    });

    /* ---------- Logout (CSRF-protected) ---------- */
    document.body.addEventListener('click', async function (e) {
        const logoutBtn = e.target.closest('#logoutBtn');
        if (!logoutBtn) return;
        e.preventDefault();
        try {
            const headers = {};
            const token  = document.querySelector('meta[name="_csrf"]');
            const header = document.querySelector('meta[name="_csrf_header"]');
            if (token && header) headers[header.getAttribute('content')] = token.getAttribute('content');
            await fetch('/logout', { method: 'POST', credentials: 'same-origin', headers: headers });
        } catch (err) {
            console.warn('Logout request failed:', err);
        } finally {
            window.location.href = '/login';
        }
    });

    /* ---------- HTMX: inject CSRF on every request ---------- */
    document.body.addEventListener('htmx:configRequest', function (evt) {
        const token  = document.querySelector('meta[name="_csrf"]');
        const header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header) {
            evt.detail.headers[header.getAttribute('content')] = token.getAttribute('content');
        }
    });

    /* ---------- HTMX: modal auto-open after swap ---------- */
    document.body.addEventListener('htmx:afterSwap', function (evt) {
        const modal = document.getElementById('modal-container');
        if (!modal) return;
        if (evt.detail.target.id === 'modal-container') modal.style.display = 'flex';

        const listIds = ['company-list','master-company-list','user-list',
                         'role-list','permission-list','branch-list',
                         'code-list','tenant-list'];
        if (listIds.includes(evt.detail.target.id)) closeModal();
    });

    /* ---------- HTMX: error handling ---------- */
    document.body.addEventListener('htmx:responseError', function (evt) {
        const status = evt.detail.xhr && evt.detail.xhr.status;
        const body   = evt.detail.xhr && evt.detail.xhr.responseText;

        /* Session expired → force re-login */
        if (status === 401) {
            window.location.href = '/login?expired=1';
            return;
        }
        if (status === 403) {
            showToast('You do not have permission for this action.', 'error');
            return;
        }
        if (status === 400) {
            showToast(body || 'Request could not be processed.', 'error');
            return;
        }
        showToast('Server error (' + status + '). Please try again.', 'error');
    });

    document.body.addEventListener('htmx:sendError', function () {
        showToast('Network error. Check your connection.', 'error');
    });

    /* ---------- Lightweight accessible toast (replaces alert) ---------- */
    function showToast(message, kind) {
        let host = document.getElementById('toast-host');
        if (!host) {
            host = document.createElement('div');
            host.id = 'toast-host';
            host.setAttribute('role', 'region');
            host.setAttribute('aria-live', 'polite');
            host.setAttribute('aria-atomic', 'true');
            host.style.cssText = 'position:fixed;top:16px;right:16px;z-index:2000;display:flex;flex-direction:column;gap:8px;max-width:360px;';
            document.body.appendChild(host);
        }
        const toast = document.createElement('div');
        toast.textContent = message;
        toast.setAttribute('role', 'status');
        toast.style.cssText =
            'background:' + (kind === 'error' ? '#fef2f2' : '#ecfdf5') + ';' +
            'color:'      + (kind === 'error' ? '#991b1b' : '#065f46') + ';' +
            'border:1px solid ' + (kind === 'error' ? '#fecaca' : '#a7f3d0') + ';' +
            'padding:12px 16px;border-radius:8px;font-size:13px;' +
            'box-shadow:0 4px 12px rgba(0,0,0,0.08);';
        host.appendChild(toast);
        setTimeout(function () { toast.remove(); }, 6000);
    }

    /* ---------- closeModal via HX-Trigger ---------- */
    document.body.addEventListener('closeModal', closeModal);
});

/* ---------- Tenant switcher (CSRF-protected) ---------- */
async function switchTenant(tenantId) {
    const token  = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    const headers = {};
    if (token && header) headers[header.getAttribute('content')] = token.getAttribute('content');

    try {
        const res = await fetch(`/auth/tenant/switch/${tenantId}`, {
            method: 'POST', credentials: 'same-origin', headers: headers
        });
        if (res.ok) { window.location.reload(); return; }
        console.warn('Tenant switch failed:', res.status);
    } catch (err) {
        console.warn('Tenant switch error:', err);
    }
    window.location.reload();
}

/* ---------- Division switcher (CSRF-protected) ---------- */
async function switchDivision(divisionId) {
    const token  = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    const headers = {};
    if (token && header) headers[header.getAttribute('content')] = token.getAttribute('content');

    const url = divisionId ? `/admin/division/switch/${divisionId}` : `/admin/division/clear`;
    try {
        const res = await fetch(url, { method: 'POST', credentials: 'same-origin', headers: headers });
        if (res.ok) { window.location.reload(); return; }
        console.warn('Division switch failed:', res.status);
    } catch (err) {
        console.warn('Division switch error:', err);
    }
}