import React from 'react'

/** Original CatchBusLK wordmark + bus-window mark. Pure SVG, no external assets. */
export default function Logo({ dark = false, className = '' }) {
  const textColor = dark ? '#f5f2ea' : '#0f1712'
  return (
    <div className={`flex items-center gap-2.5 ${className}`}>
      <svg width="34" height="34" viewBox="0 0 34 34" fill="none" aria-hidden="true">
        <rect x="1" y="1" width="32" height="32" rx="9" fill="#1fb85a" />
        <rect x="6" y="9" width="22" height="13" rx="3.2" fill="#0f1712" />
        <rect x="8.4" y="11.4" width="6.4" height="5.2" rx="1.2" fill="#78e39d" />
        <rect x="16.6" y="11.4" width="6.4" height="5.2" rx="1.2" fill="#78e39d" />
        <circle cx="11.5" cy="24" r="2.4" fill="#f5f2ea" stroke="#0f1712" strokeWidth="1.4" />
        <circle cx="22.5" cy="24" r="2.4" fill="#f5f2ea" stroke="#0f1712" strokeWidth="1.4" />
        <rect x="6" y="18.6" width="22" height="2.2" fill="#f5f2ea" opacity="0.9" />
      </svg>
      <span className="font-display text-xl font-bold tracking-tight" style={{ color: textColor }}>
        Catch<span className="text-brand-500">Bus</span>
        <span className="text-brand-700">LK</span>
      </span>
    </div>
  )
}
