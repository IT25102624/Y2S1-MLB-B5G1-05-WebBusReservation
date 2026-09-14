package com.busticket.config;

import com.busticket.model.*;
import com.busticket.model.enums.DeckType;
import com.busticket.model.enums.SeatType;
import com.busticket.repository.BusRepository;
import com.busticket.repository.SeatRepository;
import com.busticket.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final TripRepository tripRepository;

    public DataInitializer(BusRepository busRepository, SeatRepository seatRepository, TripRepository tripRepository) {
        this.busRepository = busRepository;
        this.seatRepository = seatRepository;
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (busRepository.count() > 0) {
            return;
        }

        log.info("Seeding Sri Lanka bus route data...");

        // ─── Bus 1: AC Seater — Colombo to Kandy (2+2 Layout) ───────────────────
        Bus bus1 = new Bus("NC-8888", "Sri Lanka Transport Board (SLTB)", "Luxury AC Seater (2+2)", 32, false);
        busRepository.save(bus1);

        // 8 rows × 4 seats = 32 seats
        for (int r = 1; r <= 8; r++) {
            seatRepository.save(new Seat(bus1, "S" + r + "A", DeckType.LOWER, r, 1, SeatType.SEATER_WINDOW));
            seatRepository.save(new Seat(bus1, "S" + r + "B", DeckType.LOWER, r, 2, SeatType.SEATER_REGULAR));
            seatRepository.save(new Seat(bus1, "S" + r + "C", DeckType.LOWER, r, 3, SeatType.SEATER_REGULAR));
            seatRepository.save(new Seat(bus1, "S" + r + "D", DeckType.LOWER, r, 4, SeatType.SEATER_WINDOW));
        }

        // Trip 1: Colombo → Kandy
        tripRepository.save(new Trip(
                bus1,
                "Colombo",
                "Kandy",
                LocalDateTime.now().plusDays(1).withHour(6).withMinute(0),
                LocalDateTime.now().plusDays(1).withHour(9).withMinute(30),
                420.00
        ));

        // ─── Bus 2: Semi-Luxury Seater — Colombo to Galle (2+2 Layout) ──────────
        Bus bus2 = new Bus("SP-5511", "Perera & Sons Travels", "Semi-Luxury AC Seater (2+2)", 28, false);
        busRepository.save(bus2);

        // 7 rows × 4 seats = 28 seats
        for (int r = 1; r <= 7; r++) {
            seatRepository.save(new Seat(bus2, "A" + r + "A", DeckType.LOWER, r, 1, SeatType.SEATER_WINDOW));
            seatRepository.save(new Seat(bus2, "A" + r + "B", DeckType.LOWER, r, 2, SeatType.SEATER_REGULAR));
            seatRepository.save(new Seat(bus2, "A" + r + "C", DeckType.LOWER, r, 3, SeatType.SEATER_REGULAR));
            seatRepository.save(new Seat(bus2, "A" + r + "D", DeckType.LOWER, r, 4, SeatType.SEATER_WINDOW));
        }

        // Trip 2: Colombo → Galle
        tripRepository.save(new Trip(
                bus2,
                "Colombo",
                "Galle",
                LocalDateTime.now().plusDays(1).withHour(8).withMinute(30),
                LocalDateTime.now().plusDays(1).withHour(11).withMinute(0),
                350.00
        ));

        // ─── Bus 3: Express AC Seater — Kandy to Jaffna (2+2 Layout) ────────────
        Bus bus3 = new Bus("NW-3344", "Rajawasa Express", "Express AC Seater (2+2)", 32, false);
        busRepository.save(bus3);

        // 8 rows × 4 seats = 32 seats
        for (int r = 1; r <= 8; r++) {
            seatRepository.save(new Seat(bus3, "K" + r + "A", DeckType.LOWER, r, 1, SeatType.SEATER_WINDOW));
            seatRepository.save(new Seat(bus3, "K" + r + "B", DeckType.LOWER, r, 2, SeatType.SEATER_REGULAR));
            seatRepository.save(new Seat(bus3, "K" + r + "C", DeckType.LOWER, r, 3, SeatType.SEATER_REGULAR));
            seatRepository.save(new Seat(bus3, "K" + r + "D", DeckType.LOWER, r, 4, SeatType.SEATER_WINDOW));
        }

        // Trip 3: Kandy → Jaffna
        tripRepository.save(new Trip(
                bus3,
                "Kandy",
                "Jaffna",
                LocalDateTime.now().plusDays(2).withHour(5).withMinute(0),
                LocalDateTime.now().plusDays(2).withHour(13).withMinute(30),
                950.00
        ));

        // ─── Bus 4: AC Seater — Colombo to Trincomalee (2+2 Layout) ─────────────
        Bus bus4 = new Bus("EP-7722", "National Transport Commission", "Super Luxury AC Seater (2+2)", 36, false);
        busRepository.save(bus4);

        // 9 rows × 4 seats = 36 seats
        for (int r = 1; r <= 9; r++) {
            seatRepository.save(new Seat(bus4, "T" + r + "A", DeckType.LOWER, r, 1, SeatType.SEATER_WINDOW));
            seatRepository.save(new Seat(bus4, "T" + r + "B", DeckType.LOWER, r, 2, SeatType.SEATER_REGULAR));
            seatRepository.save(new Seat(bus4, "T" + r + "C", DeckType.LOWER, r, 3, SeatType.SEATER_REGULAR));
            seatRepository.save(new Seat(bus4, "T" + r + "D", DeckType.LOWER, r, 4, SeatType.SEATER_WINDOW));
        }

        // Trip 4: Colombo → Trincomalee
        tripRepository.save(new Trip(
                bus4,
                "Colombo",
                "Trincomalee",
                LocalDateTime.now().plusDays(1).withHour(7).withMinute(0),
                LocalDateTime.now().plusDays(1).withHour(15).withMinute(0),
                800.00
        ));

        // ─── Bus 5: AC Seater — Colombo to Matara (2+2 Layout) ──────────────────
        Bus bus5 = new Bus("SB-1199", "Malkey Travels", "AC Seater (2+2)", 28, false);
        busRepository.save(bus5);

        // 7 rows × 4 seats = 28 seats
        for (int r = 1; r <= 7; r++) {
            seatRepository.save(new Seat(bus5, "M" + r + "A", DeckType.LOWER, r, 1, SeatType.SEATER_WINDOW));
            seatRepository.save(new Seat(bus5, "M" + r + "B", DeckType.LOWER, r, 2, SeatType.SEATER_REGULAR));
            seatRepository.save(new Seat(bus5, "M" + r + "C", DeckType.LOWER, r, 3, SeatType.SEATER_REGULAR));
            seatRepository.save(new Seat(bus5, "M" + r + "D", DeckType.LOWER, r, 4, SeatType.SEATER_WINDOW));
        }

        // Trip 5: Colombo → Matara
        tripRepository.save(new Trip(
                bus5,
                "Colombo",
                "Matara",
                LocalDateTime.now().plusDays(1).withHour(9).withMinute(0),
                LocalDateTime.now().plusDays(1).withHour(12).withMinute(30),
                380.00
        ));

        log.info("Seeding complete. {} buses, {} seats, {} trips.",
                busRepository.count(), seatRepository.count(), tripRepository.count());
    }
}
