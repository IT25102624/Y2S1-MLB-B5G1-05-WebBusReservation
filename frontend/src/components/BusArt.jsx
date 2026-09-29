import React from 'react'

/** Original hero illustration: a side-on coach bus on a dashed road, hand-built
 * in SVG so the site has real "bus" imagery without hotlinking stock photos. */
export function HeroBus({ className = '' }) {
  return (
    <svg viewBox="0 0 640 320" className={className} xmlns="http://www.w3.org/2000/svg">
      <ellipse cx="330" cy="278" rx="260" ry="18" fill="#104d2c" opacity="0.12" />

      <g className="animate-drive">
        {/* body */}
        <rect x="70" y="90" width="430" height="130" rx="26" fill="#1fb85a" />
        <rect x="70" y="90" width="430" height="130" rx="26" fill="url(#busShade)" />
        {/* roof accent */}
        <rect x="70" y="86" width="430" height="18" rx="9" fill="#149548" />
        {/* nose */}
        <path d="M470 96 h30 a26 26 0 0 1 26 26 v78 a26 26 0 0 1 -26 26 h-30 Z" fill="#12773c" />
        {/* windshield */}
        <path d="M478 108 h16 a14 14 0 0 1 14 14 v18 h-30 Z" fill="#dff8e7" />
        {/* windows */}
        {[0,1,2,3,4].map((i) => (
          <rect key={i} x={100 + i * 66} y="108" width="50" height="42" rx="8" fill="#dff8e7" />
        ))}
        {/* stripe */}
        <rect x="70" y="168" width="430" height="10" fill="#0f1712" opacity="0.85" />
        <rect x="70" y="168" width="430" height="10" fill="#78e39d" opacity="0.5" />
        {/* door */}
        <rect x="420" y="150" width="34" height="66" rx="4" fill="#0f1712" opacity="0.7" />
        {/* headlight */}
        <circle cx="494" cy="196" r="7" fill="#ffe89c" />
        {/* wheels */}
        <g className="animate-wheel">
          <circle cx="150" cy="222" r="26" fill="#0f1712" />
          <circle cx="150" cy="222" r="10" fill="#dff8e7" />
        </g>
        <g className="animate-wheel">
          <circle cx="420" cy="222" r="26" fill="#0f1712" />
          <circle cx="420" cy="222" r="10" fill="#dff8e7" />
        </g>
      </g>

      <defs>
        <linearGradient id="busShade" x1="70" y1="90" x2="70" y2="220" gradientUnits="userSpaceOnUse">
          <stop stopColor="white" stopOpacity="0.18" />
          <stop offset="1" stopColor="black" stopOpacity="0.06" />
        </linearGradient>
      </defs>

      <line x1="0" y1="260" x2="640" y2="260" stroke="#0f1712" strokeOpacity="0.15" strokeWidth="3" strokeDasharray="16 14" />
    </svg>
  )
}

/** Small bus glyph used inside route cards. */
export function RouteBusIcon({ className = '' }) {
  return (
    <svg viewBox="0 0 48 32" className={className} xmlns="http://www.w3.org/2000/svg">
      <rect x="2" y="6" width="38" height="18" rx="6" fill="currentColor" />
      <rect x="6" y="10" width="8" height="7" rx="1.6" fill="white" fillOpacity="0.85" />
      <rect x="17" y="10" width="8" height="7" rx="1.6" fill="white" fillOpacity="0.85" />
      <rect x="28" y="10" width="8" height="7" rx="1.6" fill="white" fillOpacity="0.85" />
      <circle cx="12" cy="26" r="4.4" fill="#0f1712" />
      <circle cx="32" cy="26" r="4.4" fill="#0f1712" />
    </svg>
  )
}
