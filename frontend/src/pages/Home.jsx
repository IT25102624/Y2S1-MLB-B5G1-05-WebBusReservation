import React from 'react'
import { Link } from 'react-router-dom'

export default function Home() {
  return (
    <div className="min-h-screen bg-sand-50 py-10 px-6">
      <div className="mx-auto max-w-6xl">
        {/* HERO SECTION */}
        <section className="mt-2 rounded-3xl bg-gradient-to-br from-brand-600 via-brand-500 to-brand-700 p-8 sm:p-12 text-white shadow-card-lg relative overflow-hidden">
          <div className="relative z-10 max-w-3xl">
            <span className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1 text-xs font-bold uppercase tracking-wider backdrop-blur-md">
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
              Express Passenger &amp; Cargo Network
            </span>

            <h1 className="mt-4 font-display text-3xl font-black sm:text-5xl leading-tight">
              Smart Baggage, Real-Time Alerts &amp; Passenger Care
            </h1>

            <p className="mt-4 text-white/90 text-sm sm:text-base leading-relaxed max-w-2xl">
              Experience modern intercity travel across Sri Lanka with automated SMS &amp; Email alerts, anti-loss QR luggage tagging, and seamless post-journey reviews.
            </p>

            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to="/luggage"
                className="rounded-2xl bg-white px-6 py-3 text-sm font-extrabold text-brand-700 shadow-card hover:bg-sand-50 transition"
              >
                🏷️ Tag Your Luggage
              </Link>
              <Link
                to="/notifications"
                className="rounded-2xl bg-white/20 border border-white/30 backdrop-blur-md px-6 py-3 text-sm font-extrabold text-white hover:bg-white/30 transition"
              >
                🔔 Alerts &amp; Notifications
              </Link>
              <Link
                to="/feedback"
                className="rounded-2xl bg-white/20 border border-white/30 backdrop-blur-md px-6 py-3 text-sm font-extrabold text-white hover:bg-white/30 transition"
              >
                ⭐ Travel Reviews &amp; Ratings
              </Link>
            </div>
          </div>

          <div className="absolute -right-12 -bottom-12 opacity-10 text-[260px] select-none pointer-events-none">
            🧳
          </div>
        </section>

        {/* 3 CORE SERVICES */}
        <div className="mt-10 grid gap-8 md:grid-cols-3">
          {/* Card 1: Luggage */}
          <div className="rounded-3xl border border-ink-900/10 bg-white p-7 shadow-card flex flex-col justify-between hover:border-brand-500/40 transition">
            <div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-purple-50 text-2xl">
                🏷️
              </div>
              <span className="mt-4 block text-[11px] font-bold uppercase tracking-wider text-purple-700">
                Anti-Loss Baggage Protection
              </span>
              <h2 className="mt-1 font-display text-xl font-bold text-ink-950">
                Luggage Tagging &amp; Tracking
              </h2>
              <p className="mt-2 text-xs text-ink-700/75 leading-relaxed">
                Declare baggage count linked to your confirmed E-ticket. Generates a unique Luggage ID, printable digital tag card with scannable QR code, and real-time cargo progression tracker.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-ink-900/5 flex items-center justify-between">
              <Link to="/luggage" className="text-xs font-extrabold text-brand-600 hover:underline">
                Tag Luggage →
              </Link>
              <Link to="/luggage" className="text-xs font-semibold text-ink-700/60 hover:text-ink-900">
                Live Cargo Tracker
              </Link>
            </div>
          </div>

          {/* Card 2: Notifications */}
          <div className="rounded-3xl border border-ink-900/10 bg-white p-7 shadow-card flex flex-col justify-between hover:border-brand-500/40 transition">
            <div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-50 text-2xl">
                🔔
              </div>
              <span className="mt-4 block text-[11px] font-bold uppercase tracking-wider text-blue-700">
                Real-Time Passenger Updates
              </span>
              <h2 className="mt-1 font-display text-xl font-bold text-ink-950">
                Live Notifications &amp; Alerts
              </h2>
              <p className="mt-2 text-xs text-ink-700/75 leading-relaxed">
                Receive instant real-time notifications for booking confirmations, luggage status changes, and journey departure updates directly on the portal.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-ink-900/5 flex items-center justify-between">
              <Link to="/notifications" className="text-xs font-extrabold text-brand-600 hover:underline">
                View Alerts Hub →
              </Link>
              <span className="text-[11px] text-emerald-600 font-bold">✓ Real-time Feed</span>
            </div>
          </div>

          {/* Card 3: Feedback */}
          <div className="rounded-3xl border border-ink-900/10 bg-white p-7 shadow-card flex flex-col justify-between hover:border-brand-500/40 transition">
            <div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-50 text-2xl">
                ⭐
              </div>
              <span className="mt-4 block text-[11px] font-bold uppercase tracking-wider text-amber-700">
                Post-Journey Experience
              </span>
              <h2 className="mt-1 font-display text-xl font-bold text-ink-950">
                Customer Reviews &amp; Care
              </h2>
              <p className="mt-2 text-xs text-ink-700/75 leading-relaxed">
                Submit 5-star ratings and written reviews after your journey. Explore verified traveler feedback and official Customer Service resolutions in our public review database.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-ink-900/5 flex items-center justify-between">
              <Link to="/feedback" className="text-xs font-extrabold text-brand-600 hover:underline">
                Reviews Database →
              </Link>
              <Link to="/admin/feedback" className="text-xs font-semibold text-ink-700/60 hover:text-ink-900">
                Support Console
              </Link>
            </div>
          </div>
        </div>

        {/* TRUST & SERVICE HIGHLIGHTS */}
        <div className="mt-10 rounded-3xl border border-ink-900/10 bg-white p-8 shadow-card">
          <div className="grid gap-6 sm:grid-cols-3 text-center">
            <div className="p-4">
              <div className="text-3xl mb-2">🛡️</div>
              <h4 className="font-bold text-ink-950 text-sm">Anti-Loss Cargo Guarantee</h4>
              <p className="text-xs text-ink-700/70 mt-1">Unique QR identifiers track every baggage piece from departure to collection.</p>
            </div>
            <div className="p-4 border-t sm:border-t-0 sm:border-l border-ink-900/10">
              <div className="text-3xl mb-2">📱</div>
              <h4 className="font-bold text-ink-950 text-sm">Real-Time SMS Dispatches</h4>
              <p className="text-xs text-ink-700/70 mt-1">Instant mobile updates for journey schedules, bus boarding, and baggage alerts.</p>
            </div>
            <div className="p-4 border-t sm:border-t-0 sm:border-l border-ink-900/10">
              <div className="text-3xl mb-2">🏆</div>
              <h4 className="font-bold text-ink-950 text-sm">24/7 Verified Customer Care</h4>
              <p className="text-xs text-ink-700/70 mt-1">Dedicated customer service team resolving passenger feedback and complaints promptly.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
