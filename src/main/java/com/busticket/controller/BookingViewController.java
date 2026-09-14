package com.busticket.controller;

import com.busticket.model.Booking;
import com.busticket.model.Trip;
import com.busticket.repository.BookingRepository;
import com.busticket.repository.TripRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class BookingViewController {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    public BookingViewController(TripRepository tripRepository, BookingRepository bookingRepository) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Trip> trips = tripRepository.findAll();
        model.addAttribute("trips", trips);
        return "index";
    }

    @GetMapping("/booking/{tripId}")
    public String bookingPage(@PathVariable Long tripId, Model model) {
        Optional<Trip> tripOpt = tripRepository.findById(tripId);
        if (tripOpt.isEmpty()) {
            return "redirect:/";
        }
        model.addAttribute("trip", tripOpt.get());
        return "booking";
    }

    @GetMapping("/booking/confirmation")
    public String confirmationPage(@RequestParam String pnr, Model model) {
        Optional<Booking> bookingOpt = bookingRepository.findByPnr(pnr);
        if (bookingOpt.isEmpty()) {
            return "redirect:/";
        }
        model.addAttribute("booking", bookingOpt.get());
        return "confirmation";
    }
}
