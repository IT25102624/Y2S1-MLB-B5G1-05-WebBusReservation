package lk.catchbus.api.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
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
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO users(full_name,email,phone_number,password_hash,role,created_at)
                VALUES(?,?,?,?, 'PASSENGER', CURRENT_TIMESTAMP)
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setString(4, hash);
            return statement;
        }, keys);
        Number id = null;
        if (!keys.getKeyList().isEmpty()) {
            Map<String, Object> generated = keys.getKeyList().getFirst();
            Object namedId = generated.get("user_id");
            if (namedId instanceof Number number) {
                id = number;
            } else {
                id = generated.values().stream()
                        .filter(Number.class::isInstance)
                        .map(Number.class::cast)
                        .findFirst()
                        .orElse(null);
            }
        }
        if (id == null) throw new IllegalStateException("Database did not return the new user ID.");
        return id.longValue();
    }
}
