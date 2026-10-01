function getCurrentUser() {
    let user = null;
    try {
        user = JSON.parse(localStorage.getItem('bus_user') || localStorage.getItem('currentUser') || 'null');
    } catch (e) {}

    if (!user) {
        // Fallback default test passenger matching seed data (Dohan Imanjith, user_id = 2)
        user = {
            id: 2,
            userId: 2,
            fullName: 'Dohan Imanjith',
            firstName: 'Dohan',
            lastName: 'Imanjith',
            email: 'dohan@busreservation.lk',
            role: 'PASSENGER'
        };
        localStorage.setItem('bus_user', JSON.stringify(user));
        localStorage.setItem('currentUser', JSON.stringify(user));
    }
    if (!user.id && user.userId) user.id = user.userId;
    if (!user.userId && user.id) user.userId = user.id;
    return user;
}

function setCurrentUser(user) {
    if (user) {
        if (!user.id && user.userId) user.id = user.userId;
        if (!user.userId && user.id) user.userId = user.id;
    }
    localStorage.setItem('bus_user', JSON.stringify(user));
    localStorage.setItem('currentUser', JSON.stringify(user));
}

function clearCurrentUser() {
    localStorage.removeItem('bus_user');
    localStorage.removeItem('currentUser');
    localStorage.removeItem('bus_jwt_token');
}

function requireUser() {
    return getCurrentUser();
}

function setupNav(activePage) {
    const user = getCurrentUser();

    // Standardize navbar brand
    const brand = document.querySelector('.brand');
    if (brand) {
        brand.innerHTML = '<span>🚌</span> Catch<span>Bus</span>LK';
        brand.href = 'index.html';
    }

    document.querySelectorAll('.nav-links a').forEach(link => {
        if (link.getAttribute('href') === activePage) link.classList.add('active');
    });

    const loginLink = document.getElementById('loginLink');
    const logoutBtn = document.getElementById('logoutBtn');
    if (loginLink) {
        if (user && user.fullName) {
            loginLink.textContent = `👤 ${user.fullName.split(' ')[0]}`;
            loginLink.href = 'admin.html?tab=profile';
            loginLink.classList.remove('hidden');
        } else {
            loginLink.textContent = 'Sign In';
            loginLink.href = 'admin.html?tab=signin';
        }
    }

    if (user) setupNotificationBell(user);

    if (logoutBtn) {
        logoutBtn.classList.toggle('hidden', !user);
        logoutBtn.addEventListener('click', async () => {
            await fetch('/api/v1/auth/logout', { method: 'POST' }).catch(() => {});
            clearCurrentUser();
            window.location.href = 'index.html';
        });
    }
}

function setupNotificationBell(user) {
    const navLinks = document.querySelector('.nav-links');
    if (!navLinks || document.getElementById('notificationBell')) return;

    const area = document.createElement('div');
    area.className = 'notification-area';
    area.innerHTML = `
        <button id="notificationBell" class="icon-button" type="button" title="Notifications" aria-label="Notifications">
            <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"></path>
                <path d="M13.7 21a2 2 0 0 1-3.4 0" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"></path>
            </svg>
            <span id="notificationBadge" class="notification-badge hidden">0</span>
        </button>
        <div id="notificationMenu" class="notification-menu hidden">
            <h3>
                <span>🔔 Recent Alerts</span>
                <a href="notifications.html" style="font-size:12px; color:var(--primary); text-decoration:none;">View all</a>
            </h3>
            <div id="notificationItems"><p style="font-size:13px; color:#64748b;">Loading...</p></div>
            <button id="markAllReadBtn" class="button secondary" style="width:100%; margin-top:10px; font-size:12px; padding:6px 10px;" type="button">Mark All Read</button>
        </div>
    `;

    const loginLink = document.getElementById('loginLink');
    navLinks.insertBefore(area, loginLink || navLinks.firstChild);

    const uid = user.id || user.userId || 2;

    document.getElementById('notificationBell').addEventListener('click', (e) => {
        e.stopPropagation();
        document.getElementById('notificationMenu').classList.toggle('hidden');
        loadNotificationBellItems(uid);
    });

    document.getElementById('markAllReadBtn').addEventListener('click', async () => {
        await fetch(`/api/v1/notifications/user/${uid}/read-all`, { method: 'PUT' });
        await loadNotificationBellItems(uid);
    });

    document.addEventListener('click', event => {
        if (!area.contains(event.target)) {
            const menu = document.getElementById('notificationMenu');
            if (menu) menu.classList.add('hidden');
        }
    });

    loadNotificationBellItems(uid);
}

async function loadNotificationBellItems(userId) {
    try {
        const response = await fetch(`/api/v1/notifications/user/${userId}`);
        const notifications = await response.json();
        const unread = Array.isArray(notifications) ? notifications.filter(item => !item.isRead).length : 0;
        const badge = document.getElementById('notificationBadge');
        if (badge) {
            badge.textContent = unread > 9 ? '9+' : unread;
            badge.classList.toggle('hidden', unread === 0);
        }

        const items = document.getElementById('notificationItems');
        if (items) {
            items.innerHTML = (Array.isArray(notifications) && notifications.length) ? notifications.slice(0, 5).map(item => `
                <article class="item" style="padding:10px; margin-bottom:8px;">
                    <span class="tag" style="font-size:10px; padding:2px 6px;">${item.isRead ? 'READ' : 'NEW'}</span>
                    <p style="font-size:12px; font-weight:600; color:#0f172a; margin:4px 0;">${item.message}</p>
                    <small style="font-size:11px; color:#94a3b8;">${formatDateTime(item.createdAt)}</small>
                </article>
            `).join('') : '<p style="font-size:13px; color:#64748b; padding:10px 0;">No new alerts.</p>';
        }
    } catch (e) {
        const items = document.getElementById('notificationItems');
        if (items) items.innerHTML = '<p style="font-size:12px; color:#94a3b8;">No notifications available.</p>';
    }
}

function showMessage(text, type = 'success') {
    const message = document.getElementById('message');
    if (!message) return;
    message.textContent = text;
    message.className = `message ${type}`;
    setTimeout(() => {
        message.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 50);
}

function formatDateTime(value) {
    return value ? new Date(value).toLocaleString() : 'Recently';
}
