package lk.catchbus.api.controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    public Map<String,Object> health() { return Map.of("status","UP","service","catchbus-backend"); }
}
