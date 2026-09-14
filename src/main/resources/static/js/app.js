function getCurrentUser() {
    let user = JSON.parse(localStorage.getItem('currentUser') || 'null');
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
    localStorage.setItem('currentUser', JSON.stringify(user));
}

function clearCurrentUser() {
    localStorage.removeItem('currentUser');
}

function requireUser() {
    return getCurrentUser();
}

function setupNav(activePage) {
    const user = getCurrentUser();
    document.querySelectorAll('.nav-links a').forEach(link => {
        if (link.getAttribute('href') === activePage) link.classList.add('active');
    });
    const notificationsLink = document.querySelector('.nav-links a[href="notifications.html"]');
    if (notificationsLink) {
        notificationsLink.remove();
    }
    const loginLink = document.getElementById('loginLink');
    const logoutBtn = document.getElementById('logoutBtn');
    if (loginLink) loginLink.classList.toggle('hidden', !!user);
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
            <h3>Notifications</h3>
            <div id="notificationItems"><p>Loading...</p></div>
            <button id="markAllReadBtn" class="button secondary" type="button">Mark All Read</button>
        </div>
    `;
    const loginLink = document.getElementById('loginLink');
    navLinks.insertBefore(area, loginLink || navLinks.firstChild);

    const uid = user.id || user.userId || 2;

    document.getElementById('notificationBell').addEventListener('click', () => {
        document.getElementById('notificationMenu').classList.toggle('hidden');
        loadNotificationBellItems(uid);
    });
    document.getElementById('markAllReadBtn').addEventListener('click', async () => {
        await fetch(`/api/v1/notifications/user/${uid}/read-all`, { method: 'PUT' });
        await loadNotificationBellItems(uid);
    });
    document.addEventListener('click', event => {
        if (!area.contains(event.target)) document.getElementById('notificationMenu').classList.add('hidden');
    });
    loadNotificationBellItems(uid);
}

async function loadNotificationBellItems(userId) {
    const response = await fetch(`/api/v1/notifications/user/${userId}`);
    const notifications = await response.json();
    const unread = notifications.filter(item => !item.isRead).length;
    const badge = document.getElementById('notificationBadge');
    if (badge) {
        badge.textContent = unread > 9 ? '9+' : unread;
        badge.classList.toggle('hidden', unread === 0);
    }

    const items = document.getElementById('notificationItems');
    if (items) {
        items.innerHTML = notifications.length ? notifications.slice(0, 5).map(item => `
            <article class="item">
                <span class="tag">${item.isRead ? 'READ' : 'NEW'}</span>
                <p>${item.message}</p>
                <small>${formatDateTime(item.createdAt)}</small>
            </article>
        `).join('') : '<p>No notifications yet.</p>';
    }
}

function showMessage(text, type = 'success') {
    const message = document.getElementById('message');
    if (!message) return;
    message.textContent = text;
    message.className = `message ${type}`;
}

function formatDateTime(value) {
    return value ? new Date(value).toLocaleString() : 'Recently';
}
