package com.system.busreservation.analytics.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class AnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // ============================================================
    // DASHBOARD SUMMARY
    // ============================================================

    public Object[] getDashboardSummary() {

        String sql = """
            SELECT
                (SELECT COUNT(*)
                 FROM bookings) AS totalBookings,

                (SELECT ISNULL(SUM(total_amount), 0)
                 FROM bookings
                 WHERE booking_status = 'CONFIRMED') AS totalRevenue,

                (SELECT COUNT(*)
                 FROM trips) AS totalTrips,

                (SELECT COUNT(*)
                 FROM buses) AS totalBuses,

                (SELECT COUNT(*)
                 FROM bookings
                 WHERE CAST(booking_time AS DATE)
                       = CAST(GETDATE() AS DATE)) AS todayBookings,

                (SELECT ISNULL(SUM(total_amount), 0)
                 FROM bookings
                 WHERE booking_status = 'CONFIRMED'
                   AND CAST(booking_time AS DATE)
                       = CAST(GETDATE() AS DATE)) AS todayRevenue
            """;

        Query query = entityManager.createNativeQuery(sql);

        return (Object[]) query.getSingleResult();
    }

    // ============================================================
    // AVERAGE OCCUPANCY
    // ============================================================

    public double getAverageOccupancy() {

        /*
         * First calculate occupancy for each trip.
         * Then calculate AVG() outside the trip calculation.
         *
         * This avoids SQL Server error:
         * "Cannot perform an aggregate function on an expression
         * containing an aggregate or a subquery."
         */

        String sql = """
            SELECT ISNULL(
                AVG(
                    CASE
                        WHEN x.total_seats = 0 THEN 0
                        ELSE
                            (CAST(x.booked_seats AS FLOAT) * 100.0)
                            / x.total_seats
                    END
                ),
                0
            )
            FROM
            (
                SELECT
                    t.trip_id,
                    bus.total_seats,
                    COUNT(bs.booking_seat_id) AS booked_seats

                FROM trips t

                INNER JOIN buses bus
                    ON bus.bus_id = t.bus_id

                LEFT JOIN bookings b
                    ON b.trip_id = t.trip_id
                    AND b.booking_status = 'CONFIRMED'

                LEFT JOIN booking_seats bs
                    ON bs.booking_id = b.booking_id

                GROUP BY
                    t.trip_id,
                    bus.total_seats
            ) x
            """;

        Query query = entityManager.createNativeQuery(sql);

        Object result = query.getSingleResult();

        if (result == null) {
            return 0.0;
        }

        return ((Number) result).doubleValue();
    }

    // ============================================================
    // REVENUE BY DATE
    // ============================================================

    public List<Object[]> getRevenueByDateRange(
            LocalDate startDate,
            LocalDate endDate) {

        String sql = """
            SELECT
                CAST(b.booking_time AS DATE) AS date,

                ISNULL(
                    SUM(b.total_amount),
                    0
                ) AS revenue,

                COUNT(*) AS bookingCount

            FROM bookings b

            WHERE b.booking_status = 'CONFIRMED'

              AND CAST(b.booking_time AS DATE)
                  BETWEEN :startDate AND :endDate

            GROUP BY
                CAST(b.booking_time AS DATE)

            ORDER BY date
            """;

        Query query = entityManager.createNativeQuery(sql);

        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }

    // ============================================================
    // ROUTE ANALYTICS
    // ============================================================

    public List<Object[]> getRouteAnalytics() {

        String sql = """
            SELECT
                r.route_id AS routeId,

                CONCAT(
                    r.source_city,
                    ' → ',
                    r.destination_city
                ) AS routeName,

                r.source_city AS origin,

                r.destination_city AS destination,

                COUNT(DISTINCT t.trip_id) AS totalTrips,

                COUNT(DISTINCT b.booking_id) AS totalBookings,

                ISNULL(
                    SUM(
                        CASE
                            WHEN b.booking_status = 'CONFIRMED'
                            THEN b.total_amount
                            ELSE 0
                        END
                    ),
                    0
                ) AS totalRevenue

            FROM routes r

            LEFT JOIN trips t
                ON t.route_id = r.route_id

            LEFT JOIN bookings b
                ON b.trip_id = t.trip_id

            GROUP BY
                r.route_id,
                r.source_city,
                r.destination_city

            ORDER BY totalBookings DESC
            """;

        Query query = entityManager.createNativeQuery(sql);

        return query.getResultList();
    }

    // ============================================================
    // ROUTE OCCUPANCY
    // ============================================================

    public double getRouteOccupancy(long routeId) {

        String sql = """
            SELECT ISNULL(
                AVG(
                    CASE
                        WHEN x.total_seats = 0 THEN 0
                        ELSE
                            (CAST(x.booked_seats AS FLOAT) * 100.0)
                            / x.total_seats
                    END
                ),
                0
            )
            FROM
            (
                SELECT
                    t.trip_id,
                    bus.total_seats,
                    COUNT(bs.booking_seat_id) AS booked_seats

                FROM trips t

                INNER JOIN buses bus
                    ON bus.bus_id = t.bus_id

                LEFT JOIN bookings b
                    ON b.trip_id = t.trip_id
                    AND b.booking_status = 'CONFIRMED'

                LEFT JOIN booking_seats bs
                    ON bs.booking_id = b.booking_id

                WHERE t.route_id = :routeId

                GROUP BY
                    t.trip_id,
                    bus.total_seats
            ) x
            """;

        Query query = entityManager.createNativeQuery(sql);

        query.setParameter("routeId", routeId);

        Object result = query.getSingleResult();

        if (result == null) {
            return 0.0;
        }

        return ((Number) result).doubleValue();
    }

    // ============================================================
    // OCCUPANCY BY TRIP
    // ============================================================

    public List<Object[]> getOccupancy(
            LocalDate startDate,
            LocalDate endDate) {

        String sql = """
            SELECT
                t.trip_id AS tripId,

                bus.bus_number AS busNumber,

                CONCAT(
                    r.source_city,
                    ' → ',
                    r.destination_city
                ) AS routeName,

                CONVERT(
                    VARCHAR,
                    t.departure_time,
                    120
                ) AS departureTime,

                bus.total_seats,

                COUNT(bs.booking_seat_id) AS bookedSeats

            FROM trips t

            INNER JOIN buses bus
                ON bus.bus_id = t.bus_id

            INNER JOIN routes r
                ON r.route_id = t.route_id

            LEFT JOIN bookings b
                ON b.trip_id = t.trip_id
                AND b.booking_status = 'CONFIRMED'

            LEFT JOIN booking_seats bs
                ON bs.booking_id = b.booking_id

            WHERE CAST(t.departure_time AS DATE)
                  BETWEEN :startDate AND :endDate

            GROUP BY
                t.trip_id,
                bus.bus_number,
                r.source_city,
                r.destination_city,
                t.departure_time,
                bus.total_seats

            ORDER BY
                t.departure_time
            """;

        Query query = entityManager.createNativeQuery(sql);

        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }

    // ============================================================
    // POPULAR ROUTES
    // ============================================================

    public List<Object[]> getPopularRoutes() {

        String sql = """
            SELECT TOP 10
                CONCAT(
                    r.source_city,
                    ' → ',
                    r.destination_city
                ) AS routeName,

                r.source_city AS origin,

                r.destination_city AS destination,

                COUNT(DISTINCT b.booking_id) AS bookingCount,

                ISNULL(
                    SUM(b.total_amount),
                    0
                ) AS revenue

            FROM routes r

            INNER JOIN trips t
                ON t.route_id = r.route_id

            INNER JOIN bookings b
                ON b.trip_id = t.trip_id

            WHERE b.booking_status = 'CONFIRMED'

            GROUP BY
                r.source_city,
                r.destination_city

            ORDER BY
                bookingCount DESC
            """;

        Query query = entityManager.createNativeQuery(sql);

        return query.getResultList();
    }
}