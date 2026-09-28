/** Representative Sri Lankan intercity routes for the "Popular destinations"
 * grid and the full "/destinations" listing — placeholder data until the
 * Route & Schedule module exposes a live /api/routes endpoint to replace it. */
export const popularRoutes = [
  { from: 'Colombo', to: 'Kandy', duration: '3h 15m', price: 'LKR 850', frequency: '32 trips/day' },
  { from: 'Colombo', to: 'Galle', duration: '2h 30m', price: 'LKR 650', frequency: '40 trips/day' },
  { from: 'Colombo', to: 'Jaffna', duration: '7h 45m', price: 'LKR 2,100', frequency: '10 trips/day' },
  { from: 'Colombo', to: 'Ella', duration: '6h 10m', price: 'LKR 1,450', frequency: '8 trips/day' },
  { from: 'Kandy', to: 'Nuwara Eliya', duration: '2h 45m', price: 'LKR 480', frequency: '18 trips/day' },
  { from: 'Colombo', to: 'Trincomalee', duration: '6h 30m', price: 'LKR 1,600', frequency: '9 trips/day' },
  { from: 'Colombo', to: 'Anuradhapura', duration: '4h 50m', price: 'LKR 1,050', frequency: '14 trips/day' },
  { from: 'Negombo', to: 'Colombo', duration: '1h 05m', price: 'LKR 220', frequency: '55 trips/day' }
]

// Extra pairs so the full "/destinations" page has more than the homepage's
// 8 highlighted cards — combine with popularRoutes there.
export const moreRoutes = [
  { from: 'Colombo', to: 'Matara', duration: '3h 40m', price: 'LKR 780', frequency: '28 trips/day' },
  { from: 'Colombo', to: 'Batticaloa', duration: '7h 20m', price: 'LKR 1,900', frequency: '7 trips/day' },
  { from: 'Colombo', to: 'Badulla', duration: '6h 50m', price: 'LKR 1,350', frequency: '9 trips/day' },
  { from: 'Kandy', to: 'Jaffna', duration: '6h 30m', price: 'LKR 1,800', frequency: '5 trips/day' },
  { from: 'Galle', to: 'Matara', duration: '1h 10m', price: 'LKR 180', frequency: '35 trips/day' },
  { from: 'Kandy', to: 'Ella', duration: '4h 20m', price: 'LKR 950', frequency: '11 trips/day' },
  { from: 'Negombo', to: 'Kandy', duration: '3h 50m', price: 'LKR 900', frequency: '15 trips/day' },
  { from: 'Anuradhapura', to: 'Jaffna', duration: '3h 15m', price: 'LKR 850', frequency: '10 trips/day' }
]

export const allRoutes = [...popularRoutes, ...moreRoutes]

export const cities = [
  'Colombo', 'Kandy', 'Galle', 'Jaffna', 'Negombo', 'Ella',
  'Anuradhapura', 'Trincomalee', 'Matara', 'Nuwara Eliya', 'Batticaloa', 'Badulla'
]

/**
 * Deterministically generates a mock list of departure times for a route so
 * the Schedule page has something believable to render. Swap this out for a
 * real call to the Route & Schedule module's /api/schedules endpoint once
 * it exists — the shape (id, departTime, arriveTime, operator, busType,
 * seatsLeft, price) is a reasonable contract to ask that module to match.
 */
export function generateMockSchedule(from, to) {
  const route = allRoutes.find(
    (r) => r.from.toLowerCase() === from?.toLowerCase() && r.to.toLowerCase() === to?.toLowerCase()
  ) || { from: from || 'Colombo', to: to || 'Kandy', duration: '3h 00m', price: 'LKR 800' }

  const operators = ['SLTB Express', 'Comfort Line', 'RideLanka', 'Highland Coach']
  const busTypes = ['Semi-Luxury A/C', 'Luxury A/C', 'Standard']
  const startHours = [5, 6, 7, 8, 9, 11, 13, 15, 17, 19, 21]

  const trips = startHours.map((hour, i) => {
    const seed = hour * 7 + (from?.length || 0) + (to?.length || 0)
    const seatsLeft = 5 + (seed % 40)
    const departTime = `${String(hour).padStart(2, '0')}:${seed % 2 === 0 ? '00' : '30'}`
    return {
      id: `${route.from}-${route.to}-${i}`,
      departTime,
      duration: route.duration,
      operator: operators[seed % operators.length],
      busType: busTypes[seed % busTypes.length],
      seatsLeft,
      price: route.price
    }
  })

  return { route, trips }
}
