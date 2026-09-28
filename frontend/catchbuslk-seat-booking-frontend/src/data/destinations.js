/** Sri Lankan intercity routes for the "Popular destinations" grid and the
 * full "/destinations" listing — placeholder data until the Route & Schedule
 * module exposes a live /api/routes endpoint to replace it.
 *
 * Network shape: Colombo is the main hub. Every route below starts or ends
 * at Colombo, connecting the capital to every district capital/major town
 * in the country — mirroring how the national SLTB/private inter-provincial
 * network actually radiates out from Colombo. */
const districtRoutes = [
  { to: 'Matara', duration: '3h 40m', price: 'LKR 780', frequency: '28 trips/day' },
  { to: 'Galle', duration: '2h 30m', price: 'LKR 650', frequency: '40 trips/day' },
  { to: 'Hambantota', duration: '4h 45m', price: 'LKR 980', frequency: '12 trips/day' },
  { to: 'Ampara', duration: '7h 10m', price: 'LKR 1,750', frequency: '6 trips/day' },
  { to: 'Jaffna', duration: '7h 45m', price: 'LKR 2,100', frequency: '10 trips/day' },
  { to: 'Monaragala', duration: '6h 20m', price: 'LKR 1,300', frequency: '7 trips/day' },
  { to: 'Gampaha', duration: '0h 50m', price: 'LKR 160', frequency: '60 trips/day' },
  { to: 'Kalutara', duration: '1h 20m', price: 'LKR 260', frequency: '45 trips/day' },
  { to: 'Mahanuwara', duration: '3h 15m', price: 'LKR 850', frequency: '32 trips/day' },
  { to: 'Matale', duration: '3h 45m', price: 'LKR 900', frequency: '16 trips/day' },
  { to: 'Nuwara Eliya', duration: '4h 30m', price: 'LKR 1,050', frequency: '14 trips/day' },
  { to: 'Kilinochchi', duration: '8h 15m', price: 'LKR 2,250', frequency: '5 trips/day' },
  { to: 'Mannar', duration: '7h 30m', price: 'LKR 2,050', frequency: '4 trips/day' },
  { to: 'Mullaitivu', duration: '8h 40m', price: 'LKR 2,300', frequency: '3 trips/day' },
  { to: 'Vavuniya', duration: '6h 15m', price: 'LKR 1,700', frequency: '8 trips/day' },
  { to: 'Batticaloa', duration: '7h 20m', price: 'LKR 1,900', frequency: '7 trips/day' },
  { to: 'Trincomalee', duration: '6h 30m', price: 'LKR 1,600', frequency: '9 trips/day' },
  { to: 'Kurunegala', duration: '2h 00m', price: 'LKR 480', frequency: '38 trips/day' },
  { to: 'Puttalam', duration: '3h 30m', price: 'LKR 800', frequency: '18 trips/day' },
  { to: 'Anuradhapura', duration: '4h 50m', price: 'LKR 1,050', frequency: '14 trips/day' },
  { to: 'Polonnaruwa', duration: '5h 40m', price: 'LKR 1,200', frequency: '10 trips/day' },
  { to: 'Badulla', duration: '6h 50m', price: 'LKR 1,350', frequency: '9 trips/day' },
  { to: 'Ratnapura', duration: '2h 50m', price: 'LKR 600', frequency: '25 trips/day' },
  { to: 'Kegalle', duration: '1h 45m', price: 'LKR 350', frequency: '30 trips/day' }
]

// Every district route, from = Colombo, to = district — the full network.
export const allRoutes = districtRoutes.map((r) => ({ from: 'Colombo', ...r }))

// Homepage's highlighted grid: a subset of the full network.
export const popularRoutes = allRoutes.filter((r) =>
  ['Mahanuwara', 'Galle', 'Jaffna', 'Nuwara Eliya', 'Trincomalee', 'Anuradhapura', 'Batticaloa', 'Matara'].includes(r.to)
)

// Kept for compatibility with anything still importing moreRoutes directly.
export const moreRoutes = allRoutes.filter((r) => !popularRoutes.includes(r))

export const cities = ['Colombo', ...districtRoutes.map((r) => r.to)]

/** Parses "3h 40m" (or "8h 20min") into total minutes. */
function parseDurationToMinutes(duration) {
  const match = String(duration || '').match(/(\d+)\s*h\s*(\d+)\s*m/i)
  if (!match) return 180
  return Number(match[1]) * 60 + Number(match[2])
}

/** Formats total minutes back into "Xh Ymin", matching the site's duration style. */
function formatMinutes(totalMinutes) {
  const clamped = Math.max(20, Math.round(totalMinutes / 5) * 5)
  const h = Math.floor(clamped / 60)
  const m = clamped % 60
  return `${h}h ${String(m).padStart(2, '0')}min`
}

/** Parses "LKR 780" into 780. */
function parsePriceToNumber(price) {
  const n = Number(String(price || '').replace(/[^0-9.]/g, ''))
  return Number.isFinite(n) && n > 0 ? n : 800
}

/** Formats a number back into "LKR 780" style, rounded to a tidy step. */
function formatPrice(value, step) {
  const rounded = Math.max(step, Math.round(value / step) * step)
  return `LKR ${rounded.toLocaleString('en-LK')}`
}

/**
 * Deterministically generates a mock list of departure times for a route so
 * the Schedule page has something believable to render. Swap this out for a
 * real call to the Route & Schedule module's /api/schedules endpoint once
 * it exists.
 *
 * Each trip (a departure time + operator) now carries a list of bus-type
 * `options` — e.g. a slower, cheaper "Non A/C Normal Bus" and a faster,
 * pricier "A/C Luxury Bus" — each with its own duration, fare and seats
 * left, in that display order. The Schedule page renders one row per
 * option, and "View seats" passes that option's own duration/busType/price
 * through to the seat-selection page.
 */
export function generateMockSchedule(from, to) {
  const route = allRoutes.find(
    (r) => r.from.toLowerCase() === from?.toLowerCase() && r.to.toLowerCase() === to?.toLowerCase()
  ) || { from: from || 'Colombo', to: to || 'Kandy', duration: '3h 00m', price: 'LKR 800' }

  const operators = ['SLTB Express', 'Comfort Line', 'RideLanka', 'Highland Coach']
  const startHours = [5, 6, 7, 8, 9, 11, 13, 15, 17, 19, 21]

  const baseMinutes = parseDurationToMinutes(route.duration)
  const basePrice = parsePriceToNumber(route.price)

  const trips = startHours.map((hour, i) => {
    const seed = hour * 7 + (from?.length || 0) + (to?.length || 0)
    const departTime = `${String(hour).padStart(2, '0')}:${seed % 2 === 0 ? '00' : '30'}`
    const operator = operators[seed % operators.length]

    // Non A/C normal bus: stops more often along the way — slower, cheaper.
    const normalMinutes = baseMinutes * 1.6
    const normalPrice = basePrice * 0.65
    const normalSeats = 5 + ((seed * 3) % 40)

    // A/C luxury bus: fewer stops / expressway — faster, pricier.
    const luxuryMinutes = baseMinutes * 0.8
    const luxuryPrice = basePrice * 1.75
    const luxurySeats = 5 + ((seed * 5) % 35)

    return {
      id: `${route.from}-${route.to}-${i}`,
      departTime,
      operator,
      options: [
        {
          key: 'non-ac-normal',
          busType: 'Non A/C Normal Bus',
          duration: formatMinutes(normalMinutes),
          price: formatPrice(normalPrice, 10),
          seatsLeft: normalSeats
        },
        {
          key: 'ac-luxury',
          busType: 'A/C Luxury Bus',
          duration: formatMinutes(luxuryMinutes),
          price: formatPrice(luxuryPrice, 50),
          seatsLeft: luxurySeats
        }
      ]
    }
  })

  return { route, trips }
}
