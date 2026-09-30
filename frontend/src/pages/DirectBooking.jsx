import React, { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

// The dispatch desk's real WhatsApp/phone number, in international format for
// wa.me links (94 + the number without its leading 0).
const WHATSAPP_DISPLAY = '070 191 9700'
const WHATSAPP_INTL = '94701919700'
const SUPPORT_EMAIL = 'gamanasl@gmail.com'

/**
 * "Skip the seat map" flow for passengers in a hurry: pulls in the trip they
 * tapped "Direct Booking" on from the query string, lets them add their name
 * and a free-text request, then opens WhatsApp with everything pre-filled so
 * the message lands straight in the dispatch desk's chat.
 */
export default function DirectBooking() {
  const [params] = useSearchParams()
  const from = params.get('from') || ''
  const to = params.get('to') || ''
  const date = params.get('date') || ''
  const time = params.get('time') || ''
  const operator = params.get('operator') || ''

  const routeLabel = from && to ? `${from} ⇄ ${to}` : 'Not specified'

  const [name, setName] = useState('')
  const [message, setMessage] = useState('Can I reserve seats together for this trip?')

  const buildWhatsAppLink = () => {
    const lines = [
      'Hi CatchBusLK, I\'d like to make a direct booking:',
      '',
      `Route: ${routeLabel}`,
      date ? `Date: ${date}` : null,
      time ? `Departure: ${time}${operator ? ` (${operator})` : ''}` : null,
      '',
      message ? `Request: ${message}` : null,
      name ? `Name: ${name}` : null
    ].filter(Boolean)

    return `https://wa.me/${WHATSAPP_INTL}?text=${encodeURIComponent(lines.join('\n'))}`
  }

  const handleSend = (e) => {
    e.preventDefault()
    window.open(buildWhatsAppLink(), '_blank', 'noopener,noreferrer')
  }

  return (
    <div className="bg-ops-950">
      <div className="mx-auto grid max-w-6xl gap-0 lg:grid-cols-2">
        {/* Left: contact info */}
        <div className="px-6 py-16 sm:px-10 lg:py-24">
          <Link to="/schedule" className="text-sm font-semibold text-slate-400 hover:text-white">
            ← Back to times
          </Link>

          <span className="mt-6 inline-block rounded-md bg-signal-amber px-3 py-1 text-xs font-extrabold uppercase tracking-widest text-ink-950">
            Islandwide 24h assistance
          </span>
          <h1 className="mt-4 font-display text-4xl font-extrabold text-white sm:text-5xl">
            Contact &amp; WhatsApp Support
          </h1>
          <p className="mt-4 max-w-md text-slate-400">
            Got a question before booking? Want to confirm seat availability or need help with a
            route? Reach our Colombo team around the clock.
          </p>

          <div className="mt-8 space-y-4">
            <a
              href={`https://wa.me/${WHATSAPP_INTL}`}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-4 rounded-2xl bg-white p-4 shadow-card transition hover:-translate-y-0.5"
            >
              <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-brand-500 text-white">
                <svg viewBox="0 0 24 24" fill="currentColor" className="h-6 w-6">
                  <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91c0 1.75.46 3.45 1.32 4.95L2 22l5.29-1.39a9.9 9.9 0 0 0 4.75 1.21h.01c5.46 0 9.91-4.45 9.91-9.91 0-2.65-1.03-5.14-2.9-7.01A9.82 9.82 0 0 0 12.04 2zm5.8 14.14c-.24.68-1.4 1.3-1.93 1.38-.5.08-1.12.11-1.81-.11-.42-.13-.95-.31-1.64-.6-2.88-1.24-4.76-4.14-4.9-4.33-.14-.19-1.17-1.56-1.17-2.98 0-1.42.74-2.11 1-2.4.26-.29.57-.36.76-.36.19 0 .38 0 .55.01.18.01.41-.07.64.49.24.58.81 2 .88 2.15.07.15.12.32.02.51-.1.19-.15.31-.29.48-.15.17-.31.38-.44.51-.15.15-.3.31-.13.6.17.29.76 1.25 1.63 2.02 1.12 1 2.06 1.31 2.35 1.46.29.15.46.13.63-.08.17-.21.72-.84.91-1.13.19-.29.38-.24.64-.14.26.1 1.64.77 1.92.91.28.14.47.21.54.33.07.12.07.68-.17 1.36z" />
                </svg>
              </span>
              <div>
                <p className="text-xs font-bold uppercase tracking-wide text-ink-700/60">WhatsApp us (24 hours)</p>
                <p className="font-display text-lg font-extrabold text-ink-950">{WHATSAPP_DISPLAY}</p>
                <p className="text-xs font-semibold text-brand-600">Fastest response — click to chat</p>
              </div>
            </a>

            <a href={`tel:+94${WHATSAPP_INTL.slice(2)}`} className="flex items-center gap-4 rounded-2xl bg-white p-4 shadow-card transition hover:-translate-y-0.5">
              <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-blue-600 text-white">
                <svg viewBox="0 0 24 24" fill="currentColor" className="h-6 w-6">
                  <path d="M6.62 10.79a15.05 15.05 0 006.59 6.59l2.2-2.2a1 1 0 011.02-.24 11.36 11.36 0 003.57.57 1 1 0 011 1V20a1 1 0 01-1 1C10.61 21 3 13.39 3 4a1 1 0 011-1h3.5a1 1 0 011 1 11.36 11.36 0 00.57 3.57 1 1 0 01-.25 1.02l-2.2 2.2z" />
                </svg>
              </span>
              <div>
                <p className="text-xs font-bold uppercase tracking-wide text-ink-700/60">Direct phone support</p>
                <p className="font-display text-lg font-extrabold text-ink-950">{WHATSAPP_DISPLAY}</p>
                <p className="text-xs font-semibold text-ink-700/50">24h daily phone line</p>
              </div>
            </a>

            <a href={`mailto:${SUPPORT_EMAIL}`} className="flex items-center gap-4 rounded-2xl bg-white p-4 shadow-card transition hover:-translate-y-0.5">
              <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-signal-amber text-ink-950">
                <svg viewBox="0 0 24 24" fill="currentColor" className="h-6 w-6">
                  <path d="M2 6a2 2 0 012-2h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6zm2 0l8 6 8-6H4zm16 2.24l-7.4 5.55a1 1 0 01-1.2 0L4 8.24V18h16V8.24z" />
                </svg>
              </span>
              <div>
                <p className="text-xs font-bold uppercase tracking-wide text-ink-700/60">Email inquiries</p>
                <p className="font-display text-lg font-extrabold uppercase text-ink-950">{SUPPORT_EMAIL}</p>
                <p className="text-xs font-semibold text-ink-700/50">For official inquiries &amp; receipts</p>
              </div>
            </a>
          </div>
        </div>

        {/* Right: quick booking form */}
        <div className="flex items-center px-6 py-16 sm:px-10 lg:py-24">
          <form onSubmit={handleSend} className="w-full rounded-3xl bg-white p-8 shadow-card-lg sm:p-10">
            <h2 className="font-display text-2xl font-extrabold text-ink-950">Quick Booking Assistance</h2>
            <p className="mt-2 text-sm text-ink-700/60">
              Send a quick inquiry straight to our dispatch support desk on WhatsApp.
            </p>

            <div className="mt-6 rounded-xl border border-brand-500/20 bg-brand-50 px-4 py-3 text-sm font-semibold text-ink-900">
              {routeLabel}
              {(date || time) && (
                <span className="mt-0.5 block text-xs font-medium text-ink-700/60">
                  {date && <>{date} </>}
                  {time && <>· {time}{operator ? ` · ${operator}` : ''}</>}
                </span>
              )}
            </div>

            <label className="mt-5 block">
              <span className="text-xs font-bold uppercase tracking-wide text-ink-700/60">Your name</span>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Kasun Perera"
                className="mt-1.5 w-full rounded-xl border border-ink-900/15 px-4 py-2.5 text-ink-950 outline-none focus:border-brand-500/60"
              />
            </label>

            <label className="mt-5 block">
              <span className="text-xs font-bold uppercase tracking-wide text-ink-700/60">Your question or travel request</span>
              <textarea
                required
                rows={4}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="e.g. Can I reserve 3 A/C seats together for this Friday morning?"
                className="mt-1.5 w-full resize-y rounded-xl border border-ink-900/15 px-4 py-2.5 text-ink-950 outline-none focus:border-brand-500/60"
              />
            </label>

            <button
              type="submit"
              className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-brand-500 py-3.5 font-bold text-white shadow-card transition hover:bg-brand-600"
            >
              <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5">
                <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91c0 1.75.46 3.45 1.32 4.95L2 22l5.29-1.39a9.9 9.9 0 0 0 4.75 1.21h.01c5.46 0 9.91-4.45 9.91-9.91 0-2.65-1.03-5.14-2.9-7.01A9.82 9.82 0 0 0 12.04 2z" />
              </svg>
              Send Message to WhatsApp Desk
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
