// CatchBusLK - Core Shared Application JavaScript

function getCurrentUser() {
    let user = null;
    try {
        const stored = localStorage.getItem('user_info') || localStorage.getItem('bus_user') || localStorage.getItem('currentUser');
        if (stored) user = JSON.parse(stored);
    } catch (e) {}

    if (user) {
        if (!user.id && user.userId) user.id = user.userId;
        if (!user.userId && user.id) user.userId = user.id;
    }
    return user;
}

function setCurrentUser(user) {
    if (user) {
        if (!user.id && user.userId) user.id = user.userId;
        if (!user.userId && user.id) user.userId = user.id;
        const serialized = JSON.stringify(user);
        localStorage.setItem('user_info', serialized);
        localStorage.setItem('bus_user', serialized);
        localStorage.setItem('currentUser', serialized);
    }
}

function clearCurrentUser() {
    localStorage.removeItem('user_info');
    localStorage.removeItem('bus_user');
    localStorage.removeItem('currentUser');
    localStorage.removeItem('auth_token');
    localStorage.removeItem('bus_jwt_token');
}

function handleLogout() {
    clearCurrentUser();
    window.location.reload();
}

function requireUser(redirectUrl = 'admin.html?tab=signin') {
    const user = getCurrentUser();
    return user;
}

/**
 * Standardize navigation across all views:
 * Standard 4 links: Book a Trip | Timetable | Feedback | Customer Care
 * Right actions: Notification Bell (with badge & dropdown) + Auth status
 */
function setupNav(activePage) {
    const isSubdir = window.location.pathname.includes('/analytics/') || window.location.pathname.includes('/api/');
    const pfx = isSubdir ? '../' : '';

    // 1. Standardize brand logo
    document.querySelectorAll('.logo, .brand, .brand-logo').forEach(brand => {
        brand.innerHTML = 'Catch<span style="color:var(--primary,#10b981);">Bus</span>LK';
        brand.href = pfx + 'index.html';
        brand.style.display = 'inline-flex';
        brand.style.alignItems = 'center';
        brand.style.gap = '4px';
        brand.style.textDecoration = 'none';
    });

    // 2. Locate or create standard container (.nav-wrap or .nav-inner)
    const header = document.querySelector('header, .top-nav');
    if (!header) return;

    let wrap = header.querySelector('.nav-wrap, .nav-inner');
    if (!wrap) {
        wrap = document.createElement('div');
        wrap.className = 'nav-wrap';
        while (header.firstChild) {
            wrap.appendChild(header.firstChild);
        }
        header.appendChild(wrap);
    }

    // 3. Detect active page from window location if not passed
    const currentPath = window.location.pathname.toLowerCase();
    if (!activePage) {
        if (currentPath.includes('search')) activePage = 'search.html';
        else if (currentPath.includes('schedule')) activePage = 'schedule.html';
        else if (currentPath.includes('feedback')) activePage = 'feedback.html';
        else if (currentPath.includes('customer-care')) activePage = 'customer-care.html';
        else if (currentPath.includes('notification')) activePage = 'notifications.html';
        else if (currentPath.includes('index') || currentPath.endsWith('/') || currentPath === '') activePage = 'index.html';
    }

    const user = getCurrentUser();
    const token = localStorage.getItem('auth_token') || localStorage.getItem('bus_jwt_token');
    const isAuthenticated = Boolean(token || user);
    const isAdmin = Boolean(user && String(user.role).toUpperCase() === 'ADMIN');

    // 4. Standardize nav-links (Customer Care restricted to Admins)
    let navLinks = wrap.querySelector('.nav-links');
    if (!navLinks) {
        navLinks = document.createElement('nav');
        navLinks.className = 'nav-links';
        wrap.appendChild(navLinks);
    }

    navLinks.innerHTML = `
        <a href="${pfx}search.html" id="navLinkSearch" class="${activePage === 'search.html' ? 'active' : ''}">Book a Trip</a>
        <a href="${pfx}schedule.html" id="navLinkSchedule" class="${activePage === 'schedule.html' ? 'active' : ''}">Timetable</a>
        <a href="${pfx}feedback.html" id="navLinkFeedback" class="${activePage === 'feedback.html' ? 'active' : ''}">Feedback</a>
        ${isAdmin ? `<a href="${pfx}customer-care.html" id="navLinkCare" class="${activePage === 'customer-care.html' ? 'active' : ''}">Customer Care</a>` : ''}
    `;

    // 5. Standardize right actions (Bell + Auth)
    let navActions = wrap.querySelector('.nav-actions');
    if (!navActions) {
        navActions = document.createElement('div');
        navActions.className = 'nav-actions';
        wrap.appendChild(navActions);
    }

    let userInitials = 'U';
    let userName = 'My Account';
    if (user) {
        const name = user.fullName || user.name || user.email || 'My Account';
        userName = name.split(' ')[0];
        const parts = name.trim().split(' ');
        userInitials = parts.length > 1 ? (parts[0][0] + parts[1][0]).toUpperCase() : parts[0].substring(0, 2).toUpperCase();
    }

    navActions.innerHTML = `
        <div class="notification-area" id="navNotificationArea">
            <button id="notificationBell" class="nav-bell-btn" type="button" title="View Alerts" aria-label="Notifications">
                <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"></path>
                    <path d="M13.7 21a2 2 0 0 1-3.4 0"></path>
                </svg>
                <span id="notificationBadge" class="notification-badge hidden">0</span>
            </button>
            <div id="notificationMenu" class="notification-menu hidden">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; border-bottom:1px solid #f1f5f9; padding-bottom:8px;">
                    <strong style="font-size:14px; font-weight:700; color:#0f172a;">Platform Alerts</strong>
                    <a href="${pfx}notifications.html" style="font-size:12px; color:var(--primary,#10b981); font-weight:700; text-decoration:none;">Alerts Hub &rarr;</a>
                </div>
                <div id="notificationItems" style="max-height:260px; overflow-y:auto; margin-bottom:12px;">
                    <p style="font-size:12px; color:#64748b; padding:8px 0;">Loading alerts...</p>
                </div>
                <div style="display:flex; gap:8px;">
                    <button id="markAllReadBtn" type="button" style="flex:1; padding:6px 10px; font-size:12px; font-weight:600; border:1px solid #e2e8f0; background:#f8fafc; border-radius:6px; cursor:pointer;">Mark All Read</button>
                    <a href="${pfx}notifications.html" style="flex:1; text-align:center; padding:6px 10px; font-size:12px; font-weight:700; background:var(--primary,#10b981); color:#fff; text-decoration:none; border-radius:6px;">View Feed</a>
                </div>
            </div>
        </div>

        ${isAuthenticated ? `
            ${isAdmin ? `<a href="${pfx}admin.html?tab=portal" class="nav-admin-link">Admin Portal</a>` : ''}
            <a href="${pfx}admin.html?tab=profile" class="user-chip">
                <span class="user-avatar-dot">${userInitials}</span>
                <span>${userName}</span>
            </a>
            <button onclick="handleLogout()" class="nav-signout-btn" type="button">Sign Out</button>
        ` : `
            <a href="${pfx}admin.html?tab=signin" class="nav-signin">Sign In</a>
            <a href="${pfx}admin.html?tab=register" class="nav-signup-btn">Sign Up</a>
        `}
    `;

    // 6. Bind Notification Bell events
    wireNotificationBell();
}

/**
 * Connect click handlers and fetch count for the notification bell
 */
function wireNotificationBell() {
    const bell = document.getElementById('notificationBell');
    const menu = document.getElementById('notificationMenu');
    const markBtn = document.getElementById('markAllReadBtn');
    const area = document.getElementById('navNotificationArea');

    if (!bell || !menu) return;

    bell.onclick = (e) => {
        e.stopPropagation();
        menu.classList.toggle('hidden');
        if (!menu.classList.contains('hidden')) {
            loadNotificationBellItems();
        }
    };

    if (markBtn) {
        markBtn.onclick = async (e) => {
            e.stopPropagation();
            const user = getCurrentUser();
            const url = user?.id ? `/api/v1/notifications/user/${user.id}/read-all` : '/api/v1/notifications/read-all';
            try {
                await fetch(url, { method: 'PUT' });
            } catch (err) {}
            await loadNotificationBellItems();
        };
    }

    document.addEventListener('click', (event) => {
        if (area && !area.contains(event.target)) {
            menu.classList.add('hidden');
        }
    });

    loadNotificationBellItems();
}

/**
 * Fetch and render unread notifications
 */
async function loadNotificationBellItems() {
    const badge = document.getElementById('notificationBadge');
    const items = document.getElementById('notificationItems');
    const user = getCurrentUser();

    let url = '/api/v1/notifications';
    if (user && user.id) {
        url = `/api/v1/notifications/user/${user.id}`;
    }

    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error('Failed to load');
        const notifications = await res.json();
        let list = Array.isArray(notifications) ? notifications : [];

        // If user-specific notifications are empty, fallback to platform-wide alerts
        if (list.length === 0 && url !== '/api/v1/notifications') {
            try {
                const platformRes = await fetch('/api/v1/notifications');
                if (platformRes.ok) {
                    const platformData = await platformRes.json();
                    if (Array.isArray(platformData) && platformData.length > 0) {
                        list = platformData;
                    }
                }
            } catch (err) {}
        }

        const unread = list.filter(n => !n.isRead).length;

        if (badge) {
            if (unread > 0) {
                badge.textContent = unread > 9 ? '9+' : unread;
                badge.classList.remove('hidden');
                badge.style.display = 'flex';
            } else {
                badge.textContent = '0';
                badge.classList.add('hidden');
                badge.style.display = 'none';
            }
        }

        if (items) {
            if (!user) {
                items.innerHTML = `
                    <div style="padding:14px 10px; text-align:center;">
                        <span style="font-size:10px; font-weight:800; text-transform:uppercase; color:var(--primary-dark,#059669); letter-spacing:0.5px; display:block; margin-bottom:4px;">PASSENGER ALERTS</span>
                        <p style="font-size:12px; font-weight:600; color:#334155; margin:0 0 10px; line-height:1.4;">
                            Live trip updates and baggage alerts are customized for registered passengers.
                        </p>
                        <div style="display:flex; flex-direction:column; gap:6px;">
                            <a href="admin.html?tab=signin" style="display:block; text-align:center; padding:7px 12px; font-size:11px; font-weight:700; background:var(--primary,#10b981); color:#fff; text-decoration:none; border-radius:6px;">Sign In to View Alerts</a>
                            <a href="notifications.html" style="display:block; text-align:center; font-size:11px; font-weight:600; color:var(--primary-dark,#059669); text-decoration:none;">How Alerts Work &rarr;</a>
                        </div>
                    </div>
                `;
                if (badge) {
                    badge.classList.add('hidden');
                    badge.style.display = 'none';
                }
                return;
            }

            if (list.length > 0) {
                items.innerHTML = list.slice(0, 5).map(item => `
                    <div style="padding:8px 10px; margin-bottom:6px; background:#f8fafc; border-radius:8px; border-left:3px solid ${item.isRead ? '#cbd5e1' : 'var(--primary,#10b981)'};">
                        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:2px;">
                            <span style="font-size:10px; font-weight:800; text-transform:uppercase; color:${item.isRead ? '#64748b' : 'var(--primary-dark,#059669)'}; letter-spacing:0.5px;">${item.type ? item.type.replace(/_/g, ' ') : (item.isRead ? 'READ' : 'NEW ALERT')}</span>
                            <span style="font-size:10px; color:#94a3b8;">${formatDateTime(item.createdAt)}</span>
                        </div>
                        <p style="font-size:12px; font-weight:600; color:#0f172a; margin:0; line-height:1.4;">${escapeHtml(item.message || '')}</p>
                    </div>
                `).join('');
            } else {
                items.innerHTML = '<p style="font-size:12px; color:#64748b; padding:10px 0; text-align:center;">No alerts at this moment.</p>';
            }
        }
    } catch (e) {
        if (items) items.innerHTML = '<p style="font-size:12px; color:#94a3b8; padding:8px 0; text-align:center;">Alerts temporarily unavailable.</p>';
        if (badge) {
            badge.classList.add('hidden');
            badge.style.display = 'none';
        }
    }
}

/**
 * Modern dark toast popup (NO EMOJIS)
 */
function showLiveNotificationToast(title = 'SYSTEM ALERT', message = 'Action recorded successfully.', options = {}) {
    let container = document.getElementById('liveToastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'liveToastContainer';
        container.className = 'live-toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'live-toast';

    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', second: '2-digit', hour12: true });

    toast.innerHTML = `
        <div class="toast-icon-circle">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"></path>
                <path d="M13.7 21a2 2 0 0 1-3.4 0"></path>
            </svg>
        </div>
        <div class="toast-content">
            <div class="toast-header">
                <span class="toast-title">${escapeHtml(title)}</span>
                <div class="toast-meta">
                    <span class="toast-time">${timeStr}</span>
                    <button class="toast-close" type="button" aria-label="Close">&times;</button>
                </div>
            </div>
            <div class="toast-body">${escapeHtml(message)}</div>
            <div class="toast-footer">
                <span class="toast-pulse-dot"></span>
                <span>Active Network Notice</span>
            </div>
        </div>
    `;

    container.appendChild(toast);

    toast.querySelector('.toast-close').addEventListener('click', () => {
        dismissToast(toast);
    });

    const duration = options.duration || 6500;
    setTimeout(() => {
        dismissToast(toast);
    }, duration);

    // Refresh bell count
    loadNotificationBellItems();
}

function dismissToast(toast) {
    if (!toast || toast.dataset.dismissed) return;
    toast.dataset.dismissed = 'true';
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(15px) scale(0.95)';
    setTimeout(() => {
        if (toast.parentNode) toast.parentNode.removeChild(toast);
    }, 300);
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

function escapeHtml(str) {
    return String(str ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}

// Auto-run on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    setupNav();
});
