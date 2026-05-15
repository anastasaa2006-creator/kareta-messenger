package ru.gr0946x.server.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gr0946x.server.db.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNickIgnoreCase(String nick);
    boolean existsByNickIgnoreCase(String nick);
}