package lk.catchbus.api.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;
    public UserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Map<String,Object>> findByEmail(String email) {
        return jdbc.query("SELECT TOP 1 user_id, full_name, email, phone_number, password_hash, role FROM users WHERE LOWER(email)=LOWER(?)",
                rs -> rs.next() ? Optional.of(Map.of(
                        "user_id", rs.getLong("user_id"),
                        "full_name", rs.getString("full_name"),
                        "email", rs.getString("email"),
                        "phone_number", rs.getString("phone_number"),
                        "password_hash", rs.getString("password_hash"),
                        "role", rs.getString("role"))) : Optional.empty(), email);
    }

    public long insertPassenger(String fullName, String email, String phone, String hash) {
        return jdbc.queryForObject("""
            INSERT INTO users(full_name,email,phone_number,password_hash,role,created_at)
            OUTPUT INSERTED.user_id
            VALUES(?,?,?,?, 'PASSENGER', SYSUTCDATETIME())
            """, Long.class, fullName, email, phone, hash);
    }
}
