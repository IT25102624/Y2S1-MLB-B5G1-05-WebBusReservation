import React, { useState, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import notificationsApi from '../api/notifications.js'

export default function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState(0)
  const [recentNotifications, setRecentNotifications] = useState([])
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef(null)

  const fetchNotificationData = async () => {
    try {
      const list = await notificationsApi.getAll()
      if (Array.isArray(list)) {
        const unread = list.filter(n => !n.isRead).length
        setUnreadCount(unread)
        setRecentNotifications(list.slice(0, 6))
      }
    } catch (err) {
      // fallback
    }
  }

  useEffect(() => {
    fetchNotificationData()
    const interval = setInterval(fetchNotificationData, 5000)

    // Listen for real-time luggage & booking notifications
    const handleCustomAlert = () => {
      fetchNotificationData()
    }
    window.addEventListener('notification-alert', handleCustomAlert)

    return () => {
      clearInterval(interval)
      window.removeEventListener('notification-alert', handleCustomAlert)
    }
  }, [])

  useEffect(() => {
    function handleClickOutside(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleMarkAllRead = async () => {
    try {
      await notificationsApi.markAllAsRead()
      setUnreadCount(0)
      setRecentNotifications(prev => prev.map(n => ({ ...n, isRead: true })))
    } catch (err) {
      console.error(err)
    }
  }

  const handleMarkSingle = async (id, e) => {
    e.stopPropagation()
    try {
      await notificationsApi.markAsRead(id)
      setRecentNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n))
      setUnreadCount(prev => Math.max(0, prev - 1))
    } catch (err) {
      console.error(err)
    }
  }

  const formatTime = (dateStr) => {
    if (!dateStr) return 'Just now'
    try {
      return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    } catch {
      return 'Just now'
    }
  }

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Notifications"
        className="relative flex h-10 w-10 items-center justify-center rounded-full border border-ink-900/10 bg-white text-ink-800 transition hover:border-brand-500/50 hover:bg-brand-50/50 shadow-sm"
      >
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-signal-rose text-[10px] font-bold text-white shadow-sm ring-2 ring-white animate-pulse">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-3 w-80 sm:w-96 rounded-3xl border border-ink-900/10 bg-white p-4 shadow-card-lg z-50 animate-rise">
          <div className="flex items-center justify-between border-b border-ink-900/8 pb-3">
            <div className="flex items-center gap-2">
              <span className="font-display font-bold text-ink-900 text-sm">Notifications</span>
              {unreadCount > 0 && (
                <span className="rounded-full bg-brand-500/10 px-2 py-0.5 text-[10px] font-bold text-brand-700">
                  {unreadCount} new
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                className="text-xs font-semibold text-brand-600 hover:text-brand-700 hover:underline"
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="mt-3 divide-y divide-ink-900/5 max-h-80 overflow-y-auto">
            {recentNotifications.length === 0 ? (
              <div className="py-8 text-center text-xs text-ink-700/60">
                <span className="text-2xl block mb-2">🔔</span>
                No notifications at this moment. You will receive live alerts for your bookings and luggage updates.
              </div>
            ) : (
              recentNotifications.map((n) => (
                <div
                  key={n.id}
                  className={`py-3 px-1 transition rounded-xl ${n.isRead ? 'opacity-70' : 'bg-brand-50/30'}`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-brand-600">
                      {(n.type || 'ALERT').replace(/_/g, ' ')}
                    </span>
                    <div className="flex items-center gap-2">
                      <span className="text-[10px] text-ink-700/50">
                        {formatTime(n.createdAt)}
                      </span>
                      {!n.isRead && (
                        <button
                          onClick={(e) => handleMarkSingle(n.id, e)}
                          title="Mark as read"
                          className="text-[10px] text-brand-600 hover:underline font-bold"
                        >
                          ✓
                        </button>
                      )}
                    </div>
                  </div>
                  <p className="mt-1 text-xs text-ink-900 leading-snug font-medium">{n.message}</p>
                  <div className="mt-1 flex items-center gap-2 text-[9px] text-emerald-700 font-semibold">
                    <span className="inline-block h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                    <span>Live Notification Delivered</span>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="mt-3 border-t border-ink-900/8 pt-3 text-center">
            <Link
              to="/notifications"
              onClick={() => setIsOpen(false)}
              className="text-xs font-bold text-brand-600 hover:underline block"
            >
              Open Full Notifications Feed →
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
