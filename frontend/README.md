# CatchBusLK — Identity Frontend

React + Vite + Tailwind frontend for the **Identity, Role & Conductor Boarding
Management** module of the Web-based Bus Ticket Reservation System, rebranded
as **CatchBusLK** with a FlixBus-inspired layout: a hero search widget
overlapping a bus illustration, a popular-destinations grid, and a distinct
dark "operations" portal for staff and admins.

## What changed from the first draft
- Rebranded to **CatchBusLK** with an original SVG logo mark and hero bus
  illustration (no hotlinked stock photography, so nothing is broken or
  copyrighted).
- Redesigned the public site around FlixBus's layout pattern: hero +
  overlapping search card, "Popular destinations" route-card grid, a features
  band, and a footer — in CatchBusLK's own green/white palette, not FlixBus's
  actual assets or copy.
- **Merged the Admin login into the Staff portal** (`/staff/login`): one page,
  three role tabs — Conductor, Driver, Administrator. Conductor/Driver share
  `/api/auth/staff/login`; Administrator calls `/api/auth/admin/login`. Old
  `/admin/login` route has been removed.
- Added a `src/data/destinations.js` placeholder dataset for the destinations
  grid — swap this for a live call to the Route & Schedule module's API
  when it's ready.

## Requirements covered
1. **Guest booking** — `Home.jsx` never requires a session to search/book.
2. **Optional passenger login** — `/login` and `/register`, purely opt-in.
3. **Distinct staff login, role-aware** — `/staff/login`, dark "operations"
   theme separate from the public site, explicit Conductor/Driver/Admin tabs.
4. **Modern, eye-catching UI** — bespoke bus illustrations, ticket-stub cards,
   overlapping hero search widget, staggered entrance animations.
5. **Backend/frontend separation** — talks to the Spring Boot API only via
   `src/api/client.js` (proxied to `http://localhost:8081` in dev).
6. **Admin panel access control** — gated client-side by `ProtectedRoute` and,
   more importantly, server-side by Spring Security's `hasRole('ADMIN')`.

## Running locally

```bash
npm install
npm run dev
```

Proxies `/api/*` to `http://localhost:8081` (see `vite.config.js`) — run the
backend first for anything beyond the guest homepage. Build for production
with `npm run build`.

## Seat Selection & Ticket Booking flow

The frontend includes a complete guest-booking flow:

`Home → Book a Trip → View Times → View seats → Seat Selection → Passenger details → Provisional booking confirmation`

- `/seat-selection` is a dedicated seat-selection page.
- Seats are laid out as a 2+2 bus (supporting both Non A/C Normal 54-seat and A/C Luxury 49-seat layouts) with driver and rear markers.
- Selected seats are held in `localStorage` for 10 minutes with a live countdown timer.
- Unavailable seats cannot be selected, and users can select up to 6 seats per booking.
- Passenger details and boarding point are collected before confirmation.
- `/booking-confirmation` displays booking reference, summary, and cancellation flow that releases the held seats.
- `/direct-booking` provides an expedited WhatsApp direct reservation flow.
